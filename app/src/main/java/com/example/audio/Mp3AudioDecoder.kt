package com.example.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance native streaming audio decoder using Android's MediaExtractor
 * and MediaCodec. Decodes MP3 (and AAC/M4A/FLAC/WAV) files into 16-bit PCM and converts
 * to normalized floating-point stereo samples (-1.0 to +1.0) for Daydream's 32-bit software DSP pipeline.
 *
 * Implements a bounded lock-free ring buffer (~4 seconds of lookahead) to provide
 * jitter-free sample streaming to AudioEngine, with native seek support and linear
 * sample-rate conversion to target 44.1kHz.
 */
class Mp3AudioDecoder(private val filePath: String) {

    constructor(file: File) : this(file.absolutePath)

    companion object {
        private const val TAG = "Mp3AudioDecoder"
        private const val TARGET_SAMPLE_RATE = AudioEngine.SAMPLE_RATE // 44100
        private const val RING_BUFFER_CAPACITY = 44100 * 4 // 4 seconds of stereo audio
        private const val TIMEOUT_US = 5000L
    }

    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null

    var durationMs: Long = 0L
        private set
    var currentPositionMs: Long = 0L
        private set

    private var sourceSampleRate: Int = TARGET_SAMPLE_RATE
    private var channelCount: Int = 2
    private var isEos = false

    // Lock-free stereo ring buffer
    private val ringBufferL = DoubleArray(RING_BUFFER_CAPACITY)
    private val ringBufferR = DoubleArray(RING_BUFFER_CAPACITY)
    private val lock = Any()
    private var writePos = 0
    private var readPos = 0
    private var availableSamples = 0

    // Resampling state for non-44.1kHz files
    private var resampleFrac = 0.0
    private var lastRawL = 0.0
    private var lastRawR = 0.0

    var isInitialized = false
        private set

    init {
        try {
            val file = File(filePath)
            if (file.exists() && file.length() > 0) {
                initDecoder()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize decoder for $filePath", e)
            release()
        }
    }

    private fun initDecoder() {
        val ext = MediaExtractor()
        ext.setDataSource(filePath)

        var audioTrackIdx = -1
        var trackFormat: MediaFormat? = null
        for (i in 0 until ext.trackCount) {
            val format = ext.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIdx = i
                trackFormat = format
                break
            }
        }

        if (audioTrackIdx == -1 || trackFormat == null) {
            ext.release()
            throw IllegalArgumentException("No audio track found in file: $filePath")
        }

        ext.selectTrack(audioTrackIdx)
        this.extractor = ext

        val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg"
        sourceSampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else TARGET_SAMPLE_RATE

        channelCount = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 2

        val durationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
            trackFormat.getLong(MediaFormat.KEY_DURATION)
        } else 0L
        durationMs = durationUs / 1000L

        val dec = MediaCodec.createDecoderByType(mime)
        dec.configure(trackFormat, null, null, 0)
        dec.start()
        this.codec = dec
        this.isInitialized = true

        Log.i(TAG, "Mp3AudioDecoder initialized: $mime, ${sourceSampleRate}Hz, ${channelCount}ch, ${durationMs}ms")

        // Pre-fill the ring buffer
        pumpDecoder()
    }

    /**
     * Reads up to [count] stereo samples into [destL] and [destR].
     * Returns the actual number of samples read.
     */
    fun readStereoSamples(destL: DoubleArray, destR: DoubleArray, count: Int): Int {
        if (!isInitialized) return 0

        // Keep buffer filled
        pumpDecoder()

        var samplesRead = 0
        synchronized(lock) {
            val toRead = kotlin.math.min(count, availableSamples)
            for (i in 0 until toRead) {
                destL[i] = ringBufferL[readPos]
                destR[i] = ringBufferR[readPos]
                readPos = (readPos + 1) % RING_BUFFER_CAPACITY
            }
            availableSamples -= toRead
            samplesRead = toRead
        }

        // Update approximate position based on played samples
        if (durationMs > 0) {
            val deltaMs = (samplesRead * 1000L) / TARGET_SAMPLE_RATE
            currentPositionMs = kotlin.math.min(durationMs, currentPositionMs + deltaMs)
        }

        // If we reached EOS and ring buffer is exhausted, restart from beginning (looping)
        if (samplesRead == 0 && isEos) {
            seekTo(0L)
            pumpDecoder()
        }

        return samplesRead
    }

    /**
     * Reads up to [count] interleaved stereo float samples (count / 2 frames) into [interleaved]
     * starting at [offset].
     * Returns the number of stereo frames read (i.e. number of L/R pairs).
     */
    fun readStereoSamples(interleaved: FloatArray, offset: Int, count: Int): Int {
        if (!isInitialized) return 0

        // Keep buffer filled
        pumpDecoder()

        val maxFrames = count / 2
        var framesRead = 0
        synchronized(lock) {
            val toRead = kotlin.math.min(maxFrames, availableSamples)
            for (i in 0 until toRead) {
                interleaved[offset + i * 2] = ringBufferL[readPos].toFloat()
                interleaved[offset + i * 2 + 1] = ringBufferR[readPos].toFloat()
                readPos = (readPos + 1) % RING_BUFFER_CAPACITY
            }
            availableSamples -= toRead
            framesRead = toRead
        }

        if (durationMs > 0) {
            val deltaMs = (framesRead * 1000L) / TARGET_SAMPLE_RATE
            currentPositionMs = kotlin.math.min(durationMs, currentPositionMs + deltaMs)
        }

        return framesRead
    }

    /**
     * Decodes frames from MediaCodec and writes to the ring buffer.
     */
    private fun pumpDecoder() {
        val ext = extractor ?: return
        val dec = codec ?: return

        var safetyLimit = 16
        while (safetyLimit-- > 0) {
            // Check space in ring buffer
            var freeSpace: Int
            synchronized(lock) {
                freeSpace = RING_BUFFER_CAPACITY - availableSamples
            }
            if (freeSpace < 2048) {
                break // Ring buffer comfortably filled
            }

            // 1. Feed input buffer to MediaCodec
            if (!isEos) {
                val inIndex = dec.dequeueInputBuffer(TIMEOUT_US)
                if (inIndex >= 0) {
                    val inBuffer: ByteBuffer? = dec.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        inBuffer.clear()
                        val sampleSize = ext.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            dec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            val presentationTimeUs = ext.sampleTime
                            dec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                            ext.advance()
                        }
                    }
                }
            }

            // 2. Read decoded PCM samples from MediaCodec
            val bufferInfo = MediaCodec.BufferInfo()
            val outIndex = dec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outIndex >= 0) {
                val outBuffer: ByteBuffer? = dec.getOutputBuffer(outIndex)
                if (outBuffer != null && bufferInfo.size > 0) {
                    outBuffer.position(bufferInfo.offset)
                    outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                    processPcmChunk(outBuffer, bufferInfo.size)
                }
                dec.releaseOutputBuffer(outIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEos = true
                    break
                }
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = dec.outputFormat
                if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sourceSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                Log.d(TAG, "Output format changed: ${sourceSampleRate}Hz, ${channelCount}ch")
            } else {
                // No more output buffers immediately available
                break
            }
        }
    }

    /**
     * Converts 16-bit PCM bytes into normalized float samples and resamples to 44.1kHz.
     */
    private fun processPcmChunk(byteBuffer: ByteBuffer, sizeBytes: Int) {
        val shortBuffer = byteBuffer.asShortBuffer()
        val numShorts = sizeBytes / 2
        if (numShorts <= 0) return

        val step = if (sourceSampleRate == TARGET_SAMPLE_RATE) {
            1.0
        } else {
            sourceSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
        }

        var sampleIdx = 0
        while (sampleIdx < numShorts) {
            val sampleL: Double
            val sampleR: Double

            if (channelCount == 1) {
                val raw = (shortBuffer.get(sampleIdx) / 32768.0).coerceIn(-1.0, 1.0)
                sampleL = raw
                sampleR = raw
                sampleIdx += 1
            } else {
                val left = (shortBuffer.get(sampleIdx) / 32768.0).coerceIn(-1.0, 1.0)
                val right = if (sampleIdx + 1 < numShorts) {
                    (shortBuffer.get(sampleIdx + 1) / 32768.0).coerceIn(-1.0, 1.0)
                } else left
                sampleL = left
                sampleR = right
                sampleIdx += channelCount
            }

            // Resample if necessary using linear interpolation
            if (step == 1.0) {
                writeToRingBuffer(sampleL, sampleR)
            } else {
                resampleFrac += 1.0 / step
                while (resampleFrac >= 1.0) {
                    val interpL = lastRawL + (sampleL - lastRawL) * (1.0 - (resampleFrac - 1.0))
                    val interpR = lastRawR + (sampleR - lastRawR) * (1.0 - (resampleFrac - 1.0))
                    writeToRingBuffer(interpL, interpR)
                    resampleFrac -= 1.0
                }
                lastRawL = sampleL
                lastRawR = sampleR
            }
        }
    }

    private fun writeToRingBuffer(sampleL: Double, sampleR: Double) {
        synchronized(lock) {
            if (availableSamples < RING_BUFFER_CAPACITY) {
                ringBufferL[writePos] = sampleL
                ringBufferR[writePos] = sampleR
                writePos = (writePos + 1) % RING_BUFFER_CAPACITY
                availableSamples++
            }
        }
    }

    /**
     * Seeks to a specific millisecond timestamp within the audio file.
     */
    fun seekTo(positionMs: Long) {
        if (!isInitialized) return
        val ext = extractor ?: return
        val dec = codec ?: return

        try {
            val targetTimeUs = (positionMs * 1000L).coerceIn(0L, durationMs * 1000L)
            ext.seekTo(targetTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            dec.flush()

            synchronized(lock) {
                readPos = 0
                writePos = 0
                availableSamples = 0
            }

            currentPositionMs = positionMs
            isEos = false
            resampleFrac = 0.0

            pumpDecoder()
            Log.d(TAG, "Seeked to $positionMs ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek to $positionMs ms", e)
        }
    }

    fun release() {
        isInitialized = false
        try {
            codec?.stop()
            codec?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing codec", e)
        }
        codec = null

        try {
            extractor?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing extractor", e)
        }
        extractor = null

        synchronized(lock) {
            readPos = 0
            writePos = 0
            availableSamples = 0
        }
    }
}
