package com.lumen.gallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    /** Capture time in epoch millis (falls back to added/modified time). */
    val dateTaken: Long,
    /** Last modification in epoch seconds, used to invalidate cached thumbnails. */
    val dateModified: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val bucketId: Long,
    val bucketName: String,
    val relativePath: String,
    val isVideo: Boolean,
    /** Epoch seconds when a trashed item is purged; 0 for live items. */
    val expiresAt: Long = 0L,
) {
    val isGif: Boolean get() = mimeType == "image/gif"
    val isScreenshot: Boolean
        get() = bucketName.equals("Screenshots", ignoreCase = true) ||
            relativePath.contains("Screenshots", ignoreCase = true)
    val thumb: MediaThumb get() = MediaThumb(uri, dateModified)
}

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val cover: MediaItem,
    val count: Int,
    val path: String,
) {
    val isCamera: Boolean get() = path.startsWith("DCIM/Camera", ignoreCase = true)
}

/** A [UserAlbum] resolved against the library: only items that still exist, in sort order. */
@Immutable
data class UserAlbumView(
    val album: UserAlbum,
    val items: List<MediaItem>,
    val cover: MediaItem?,
) {
    val id: String get() = album.id
    val name: String get() = album.name
    val hidden: Boolean get() = album.hidden
}

@Immutable
data class Memory(val year: Int, val yearsAgo: Int, val items: List<MediaItem>)

enum class MediaFilter(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    FAVORITES("Favorites"),
    GIFS("GIFs"),
    SCREENSHOTS("Screenshots");

    fun matches(item: MediaItem, favorites: Set<Long>): Boolean = when (this) {
        ALL -> true
        PHOTOS -> !item.isVideo
        VIDEOS -> item.isVideo
        FAVORITES -> item.id in favorites
        GIFS -> item.isGif
        SCREENSHOTS -> item.isScreenshot
    }
}
