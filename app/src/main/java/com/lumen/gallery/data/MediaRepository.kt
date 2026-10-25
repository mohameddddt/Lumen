package com.lumen.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class ExifDetails(val camera: String?, val settings: List<String>)

class MediaRepository(context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val filesUri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    /** Emits whenever anything in the device's media library changes. */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        resolver.registerContentObserver(filesUri, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    suspend fun loadMedia(trashed: Boolean = false): List<MediaItem> = withContext(Dispatchers.IO) {
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${FileColumns.MEDIA_TYPE} IN (?, ?)")
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(FileColumns.MEDIA_TYPE_IMAGE.toString(), FileColumns.MEDIA_TYPE_VIDEO.toString()),
            )
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaColumns.DATE_MODIFIED} DESC")
            if (trashed) putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }
        val result = ArrayList<MediaItem>()
        runCatching {
            resolver.query(filesUri, PROJECTION, args, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)
                val mimeCol = c.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
                val takenCol = c.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
                val addedCol = c.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
                val modifiedCol = c.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
                val sizeCol = c.getColumnIndexOrThrow(MediaColumns.SIZE)
                val widthCol = c.getColumnIndexOrThrow(MediaColumns.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(MediaColumns.HEIGHT)
                val durationCol = c.getColumnIndexOrThrow(MediaColumns.DURATION)
                val bucketIdCol = c.getColumnIndexOrThrow(MediaColumns.BUCKET_ID)
                val bucketNameCol = c.getColumnIndexOrThrow(MediaColumns.BUCKET_DISPLAY_NAME)
                val pathCol = c.getColumnIndexOrThrow(MediaColumns.RELATIVE_PATH)
                val typeCol = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
                val orientationCol = c.getColumnIndexOrThrow(MediaColumns.ORIENTATION)
                val expiresCol = c.getColumnIndexOrThrow(MediaColumns.DATE_EXPIRES)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val isVideo = c.getInt(typeCol) == FileColumns.MEDIA_TYPE_VIDEO
                    val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val modified = c.getLong(modifiedCol)
                    // Files without capture metadata fall back to their file time rather than the
                    // time they were indexed, so restored backups don't all land on "Today".
                    val taken = c.getLong(takenCol).takeIf { it > 0 }
                        ?: modified.takeIf { it > 0 }?.times(1000)
                        ?: (c.getLong(addedCol) * 1000)
                    val rotated = c.getInt(orientationCol).let { it == 90 || it == 270 }
                    val w = c.getInt(widthCol)
                    val h = c.getInt(heightCol)
                    result += MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id),
                        name = c.getString(nameCol).orEmpty().replace(TRASHED_PREFIX, ""),
                        mimeType = c.getString(mimeCol).orEmpty(),
                        dateTaken = taken,
                        dateModified = modified,
                        size = c.getLong(sizeCol),
                        width = if (rotated) h else w,
                        height = if (rotated) w else h,
                        durationMs = c.getLong(durationCol),
                        bucketId = c.getLong(bucketIdCol),
                        bucketName = c.getString(bucketNameCol).orEmpty(),
                        relativePath = c.getString(pathCol).orEmpty(),
                        isVideo = isVideo,
                        expiresAt = if (trashed) c.getLong(expiresCol) else 0L,
                    )
                }
            }
        }
        result
    }

    /** Requires write access to [item]; callers obtain it via MediaStore.createWriteRequest first. */
    suspend fun rename(item: MediaItem, newName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply { put(MediaColumns.DISPLAY_NAME, newName) }
            resolver.update(item.uri, values, null, null) > 0
        }.getOrDefault(false)
    }

    suspend fun loadExif(item: MediaItem): ExifDetails? = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext null
        runCatching {
            resolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                val camera = when {
                    model.isBlank() -> make
                    make.isBlank() || model.startsWith(make, ignoreCase = true) -> model
                    else -> "$make $model"
                }
                val parts = buildList {
                    exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0 }
                        ?.let { add("f/${it.trimmed()}") }
                    exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0).takeIf { it > 0 }
                        ?.let { add(if (it < 1) "1/${(1 / it).roundToInt()}s" else "${it.trimmed()}s") }
                    exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).takeIf { it > 0 }
                        ?.let { add("ISO $it") }
                    exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 }
                        ?.let { add("${it.trimmed()}mm") }
                }
                ExifDetails(
                    camera = camera.ifBlank { null },
                    settings = parts,
                )
            }
        }.getOrNull()
    }

    private fun Double.trimmed(): String =
        if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)

    private companion object {
        val TRASHED_PREFIX = Regex("^\\.trashed-\\d+-")
        val PROJECTION = arrayOf(
            MediaColumns._ID,
            MediaColumns.DISPLAY_NAME,
            MediaColumns.MIME_TYPE,
            MediaColumns.DATE_TAKEN,
            MediaColumns.DATE_ADDED,
            MediaColumns.DATE_MODIFIED,
            MediaColumns.SIZE,
            MediaColumns.WIDTH,
            MediaColumns.HEIGHT,
            MediaColumns.DURATION,
            MediaColumns.BUCKET_ID,
            MediaColumns.BUCKET_DISPLAY_NAME,
            MediaColumns.RELATIVE_PATH,
            FileColumns.MEDIA_TYPE,
            MediaColumns.ORIENTATION,
            MediaColumns.DATE_EXPIRES,
        )
    }
}
