package com.example.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow

/**
 * Pro Studio offline bounce renderer.
 * Pure-Kotlin (no Android APIs except java.io) so it runs in unit tests.
 *
 * - renderOffline(): runs any stereo process fn over full buffers
 * - applyGainDb(): loudness normalize helper
 * - writeWav16Bit()/readWav16BitStereo(): 16-bit PCM WAV I/O for quick bounces
 * - WavStreamWriter: chunked 16/24-bit WAV writer with TPDF dither for
 *   full-track renders that don't fit in memory
 */
object OfflineBounceRenderer {

    data class BounceResult(
        val left: DoubleArray,
        val right: DoubleArray,
        val sampleRate: Int,
        val peakLinear: Double,
        val peakDbtp: Float,
        val appliedGainDb: Float
    )

    fun renderOffline(
        inputL: DoubleArray,
        inputR: DoubleArray,
        process: (Double, Double) -> Pair<Double, Double>
    ): Pair<DoubleArray, DoubleArray> {
        require(inputL.size == inputR.size) { "Stereo input length mismatch" }
        val outL = DoubleArray(inputL.size)
        val outR = DoubleArray(inputR.size)
        for (i in inputL.indices) {
            val (l, r) = process(inputL[i], inputR[i])
            outL[i] = if (l.isNaN() || l.isInfinite()) 0.0 else l.coerceIn(-4.0, 4.0)
            outR[i] = if (r.isNaN() || r.isInfinite()) 0.0 else r.coerceIn(-4.0, 4.0)
        }
        return Pair(outL, outR)
    }

    fun applyGainDb(left: DoubleArray, right: DoubleArray, gainDb: Float) {
        if (gainDb == 0f) return
        val lin = 10.0.pow(gainDb / 20.0)
        for (i in left.indices) {
            left[i] = (left[i] * lin).coerceIn(-4.0, 4.0)
            right[i] = (right[i] * lin).coerceIn(-4.0, 4.0)
        }
    }

    fun peakDbtp(left: DoubleArray, right: DoubleArray): Float {
        var peak = 0.0
        for (i in left.indices) {
            val a = kotlin.math.abs(left[i])
            val b = kotlin.math.abs(right[i])
            if (a > peak) peak = a
            if (b > peak) peak = b
        }
        if (peak <= 1e-9) return -90f
        return (20.0 * kotlin.math.log10(peak)).toFloat().coerceIn(-90f, 12f)
    }

    fun writeWav16Bit(file: File, left: DoubleArray, right: DoubleArray, sampleRate: Int = AudioEngine.SAMPLE_RATE) {
        require(left.size == right.size) { "Stereo length mismatch" }
        val frames = left.size
        val dataBytes = frames * 2 * 2 // stereo 16-bit
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        buf.put("RIFF".toByteArray())
        buf.putInt(36 + dataBytes)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(2) // stereo
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * 2 * 2)
        buf.putShort(4) // block align
        buf.putShort(16) // bits
        buf.put("data".toByteArray())
        buf.putInt(dataBytes)
        for (i in 0 until frames) {
            val sl = (left[i].coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
            val sr = (right[i].coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
            buf.putShort(sl)
            buf.putShort(sr)
        }
        file.parentFile?.mkdirs()
        file.writeBytes(buf.array())
    }

    fun readWav16BitStereo(file: File): Triple<DoubleArray, DoubleArray, Int> {
        val bytes = file.readBytes()
        require(bytes.size > 44) { "File too small for WAV" }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val riff = ByteArray(4); buf.get(riff)
        require(String(riff) == "RIFF") { "Not a RIFF/WAV file" }
        buf.int // chunk size
        val wave = ByteArray(4); buf.get(wave)
        require(String(wave) == "WAVE") { "Not a WAVE file" }
        val fmt = ByteArray(4); buf.get(fmt)
        buf.int // fmt size
        val audioFormat = buf.short
        require(audioFormat.toInt() == 1) { "Only PCM WAV supported" }
        val channels = buf.short.toInt()
        val sampleRate = buf.int
        buf.int // byte rate
        buf.short // block align
        buf.short // bits
        // Skip any extra chunks until "data"
        var dataSize = 0
        while (buf.remaining() >= 8) {
            val id = ByteArray(4); buf.get(id)
            val size = buf.int
            if (String(id) == "data") {
                dataSize = size
                break
            }
            buf.position(buf.position() + size)
        }
        require(dataSize > 0) { "No data chunk found" }
        val frames = dataSize / (channels * 2)
        val left = DoubleArray(frames)
        val right = DoubleArray(frames)
        for (i in 0 until frames) {
            val l = buf.short / 32768.0
            val r = if (channels > 1) buf.short / 32768.0 else l
            // If source has >2 channels, skip extras
            if (channels > 2) repeat(channels - 2) { buf.short }
            left[i] = l
            right[i] = r
        }
        return Triple(left, right, sampleRate)
    }
}

/** Bounce output word length. */
enum class BounceBitDepth(val bits: Int) {
    PCM_16(16),
    PCM_24(24)
}

/** Quantization dither for bounce output. */
enum class BounceDither {
    NONE,
    /** Triangular-probability-density-function dither at ±1 LSB. */
    TPDF
}

/**
 * Chunked stereo WAV writer for full-track bounces.
 * Writes a placeholder header, appends quantized frames, patches sizes on
 * [close]. TPDF dither decorrelates quantization error so fades and reverb
 * tails don't granulate at 16-bit.
 */
class WavStreamWriter(
    file: File,
    private val sampleRate: Int = AudioEngine.SAMPLE_RATE,
    val bitDepth: BounceBitDepth = BounceBitDepth.PCM_16,
    private val dither: BounceDither = BounceDither.TPDF,
    seed: Long = 0xD9EA5EEDL
) {
    private val out = java.io.BufferedOutputStream(java.io.FileOutputStream(file))
    private val targetFile = file
    private val bytesPerSample = bitDepth.bits / 8
    private val random = kotlin.random.Random(seed)
    private var framesWritten = 0L
    private var peakLinear = 0.0

    init {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(0) // patched in close()
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(2) // stereo
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2 * bytesPerSample)
        header.putShort((2 * bytesPerSample).toShort())
        header.putShort(bitDepth.bits.toShort())
        header.put("data".toByteArray())
        header.putInt(0) // patched in close()
        out.write(header.array())
    }

    /** Quantizes and appends [frames] stereo frames from [left]/[right]. */
    fun writeFrames(left: DoubleArray, right: DoubleArray, frames: Int) {
        require(frames <= left.size && frames <= right.size) { "Frame count exceeds buffers" }
        val scale = (1 shl (bitDepth.bits - 1)).toDouble() - 1.0 // 32767 / 8388607
        val maxCode = (1 shl (bitDepth.bits - 1)) - 1
        val minCode = -(1 shl (bitDepth.bits - 1))
        val buf = ByteBuffer.allocate(frames * 2 * bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            for (ch in 0..1) {
                var x = if (ch == 0) left[i] else right[i]
                if (x.isNaN() || x.isInfinite()) x = 0.0
                x = x.coerceIn(-1.0, 1.0)
                val a = abs(x)
                if (a > peakLinear) peakLinear = a
                var code = x * scale
                if (dither == BounceDither.TPDF) {
                    code += random.nextDouble() - random.nextDouble() // ±1 LSB triangular
                } else {
                    code += 0.5 * if (code >= 0) 1.0 else -1.0 // round-half-away on truncate path
                }
                val q = code.toInt().coerceIn(minCode, maxCode)
                when (bitDepth) {
                    BounceBitDepth.PCM_16 -> buf.putShort(q.toShort())
                    BounceBitDepth.PCM_24 -> {
                        buf.put((q and 0xFF).toByte())
                        buf.put(((q shr 8) and 0xFF).toByte())
                        buf.put(((q shr 16) and 0xFF).toByte())
                    }
                }
            }
        }
        out.write(buf.array())
        framesWritten += frames
    }

    data class WavStats(val frames: Long, val peakLinear: Double)

    /** Flushes, patches the RIFF/data sizes, and returns render stats. */
    fun close(): WavStats {
        out.flush()
        out.close()
        val dataBytes = framesWritten * 2 * bytesPerSample
        java.io.RandomAccessFile(targetFile, "rw").use { raf ->
            raf.seek(4)
            val sizes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            sizes.putInt((36 + dataBytes).coerceAtMost(0xFFFFFFFFL.toLong()).toInt())
            raf.write(sizes.array(), 0, 4)
            raf.seek(40)
            val data = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            data.putInt(dataBytes.coerceAtMost(0xFFFFFFFFL.toLong()).toInt())
            raf.write(data.array())
        }
        return WavStats(framesWritten, peakLinear)
    }
}
