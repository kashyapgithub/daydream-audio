package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.LocalTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages local MP3 audio files stored strictly inside the application's
 * private sandbox directory (context.filesDir / "local_audio").
 *
 * Files stored here are isolated to Daydream Audio only and inaccessible
 * by other apps on the device, ensuring privacy and local isolation.
 */
class LocalTrackManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalTrackManager"
        private const val PREFS_NAME = "daydream_local_tracks_prefs"
        private const val KEY_TRACKS_JSON = "saved_local_tracks_json"
        private const val DIR_NAME = "local_audio"
    }

    private val storageDir: File by lazy {
        File(context.filesDir, DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Retrieves all saved local tracks stored inside the app.
     */
    fun getStoredTracks(): List<LocalTrack> {
        val jsonStr = prefs.getString(KEY_TRACKS_JSON, null) ?: return emptyList()
        val list = mutableListOf<LocalTrack>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val filePath = obj.optString("filePath", "")
                val file = File(filePath)
                // Only return tracks whose files actually exist in internal storage
                if (file.exists() && file.length() > 0) {
                    list.add(
                        LocalTrack(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.optString("artist", "Unknown Artist"),
                            album = obj.optString("album", ""),
                            durationMs = obj.optLong("durationMs", 0L),
                            filePath = filePath,
                            fileName = obj.optString("fileName", file.name),
                            fileSize = obj.optLong("fileSize", file.length()),
                            dateAdded = obj.optLong("dateAdded", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse stored tracks JSON", e)
        }
        return list
    }

    fun getAllTracks(): List<LocalTrack> = getStoredTracks()

    suspend fun importTrackFromUri(uri: Uri): LocalTrack? = importMp3FromUri(uri).getOrNull()

    /**
     * Imports an MP3 file selected via system document/file picker, copying it
     * into the app's internal private directory so it resides inside this app only.
     */
    suspend fun importMp3FromUri(uri: Uri): Result<LocalTrack> = withContext(Dispatchers.IO) {
        try {
            var originalName = "track_${System.currentTimeMillis()}.mp3"
            var reportedSize = 0L

            // 1. Query display name and size from content resolver
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) originalName = cursor.getString(nameIdx) ?: originalName
                        if (sizeIdx != -1) reportedSize = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not query URI metadata, using fallback name", e)
            }

            // Sanitize file name
            val safeName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(storageDir, "${UUID.randomUUID()}_$safeName")

            // 2. Stream-copy the file into internal private storage
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not open input stream for URI: $uri"))

            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.delete()
                return@withContext Result.failure(Exception("Copied file is empty or missing"))
            }

            val actualSize = targetFile.length()

            // 3. Extract ID3 metadata using MediaMetadataRetriever
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var durationMs: Long = 0L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(targetFile.absolutePath)
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                Log.w(TAG, "MediaMetadataRetriever failed to read ID3 tags", e)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }

            // Fallback for title: filename without .mp3 extension
            val finalTitle = title?.takeIf { it.isNotBlank() } ?: originalName.substringBeforeLast(".")
            val finalArtist = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
            val finalAlbum = album ?: ""

            val newTrack = LocalTrack(
                id = UUID.randomUUID().toString(),
                title = finalTitle,
                artist = finalArtist,
                album = finalAlbum,
                durationMs = durationMs,
                filePath = targetFile.absolutePath,
                fileName = originalName,
                fileSize = actualSize,
                dateAdded = System.currentTimeMillis()
            )

            // 4. Save to persisted library
            val currentTracks = getStoredTracks().toMutableList()
            currentTracks.add(0, newTrack)
            saveTracks(currentTracks)

            Log.i(TAG, "Successfully imported local track: ${newTrack.title} (${newTrack.formattedSize})")
            Result.success(newTrack)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing MP3 from URI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a stored track from the internal app directory and updates the library.
     */
    fun deleteTrack(trackId: String): Boolean {
        val tracks = getStoredTracks().toMutableList()
        val trackToDelete = tracks.find { it.id == trackId } ?: return false

        try {
            val file = File(trackToDelete.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete file for track $trackId", e)
        }

        tracks.removeAll { it.id == trackId }
        saveTracks(tracks)
        return true
    }

    fun getTrackById(id: String): LocalTrack? {
        return getStoredTracks().find { it.id == id }
    }

    private fun saveTracks(tracks: List<LocalTrack>) {
        val jsonArray = JSONArray()
        tracks.forEach { track ->
            val obj = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("durationMs", track.durationMs)
                put("filePath", track.filePath)
                put("fileName", track.fileName)
                put("fileSize", track.fileSize)
                put("dateAdded", track.dateAdded)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_TRACKS_JSON, jsonArray.toString()).apply()
    }
}
