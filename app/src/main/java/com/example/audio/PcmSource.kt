package com.example.audio

/**
 * Seekable PCM frame source for offline bounce renders.
 * Abstracts the decoder so the bounce pipeline is unit-testable with a
 * synthetic source (android.media is stubbed in JVM unit tests).
 */
interface PcmSource {
    /** Source sample rate in Hz (resampled to [AudioEngine.SAMPLE_RATE] where needed). */
    val sampleRate: Int

    /** Total stereo frames, or -1 if unknown (progress becomes indeterminate). */
    val totalFrames: Long

    /**
     * Reads up to [frameCount] stereo frames as interleaved L/R floats into
     * [interleaved] at [offset]. Returns frames actually read; 0 means the
     * source is drained ([isDrained] is then true).
     */
    fun readFrames(interleaved: FloatArray, offset: Int, frameCount: Int): Int

    /** True when every frame has been delivered. */
    fun isDrained(): Boolean

    /** Rewinds to the first frame for a second pass. */
    fun reset()

    fun release()
}

/**
 * [PcmSource] backed by [Mp3AudioDecoder] (MP3/WAV/FLAC/M4A/AAC/OGG/Opus via
 * MediaExtractor/MediaCodec, resampled to 44.1kHz mono-or-stereo PCM).
 */
class DecoderPcmSource(filePath: String) : PcmSource {

    private var decoder: Mp3AudioDecoder? = Mp3AudioDecoder(filePath)

    override val sampleRate: Int get() = AudioEngine.globalSampleRate

    override val totalFrames: Long
        get() {
            val durMs = decoder?.durationMs ?: 0L
            if (durMs <= 0L) return -1L
            return (durMs * AudioEngine.globalSampleRate) / 1000L
        }

    override fun readFrames(interleaved: FloatArray, offset: Int, frameCount: Int): Int {
        return decoder?.readStereoSamples(interleaved, offset, frameCount * 2) ?: 0
    }

    override fun isDrained(): Boolean {
        return decoder?.isDrained() ?: true
    }

    override fun reset() {
        decoder?.seekTo(0L)
    }

    override fun release() {
        decoder?.release()
        decoder = null
    }
}
