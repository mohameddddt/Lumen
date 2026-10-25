package com.lumen.gallery.data

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse

/** Coil model for a grid thumbnail, served from the system's MediaStore thumbnail cache. */
data class MediaThumb(val uri: Uri, val version: Long)

class MediaThumbFetcher(
    private val data: MediaThumb,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val width = options.size.width.pxOrElse { DEFAULT_SIZE }.coerceIn(MIN_SIZE, MAX_SIZE)
        val height = options.size.height.pxOrElse { DEFAULT_SIZE }.coerceIn(MIN_SIZE, MAX_SIZE)
        val decoded = options.context.contentResolver.loadThumbnail(data.uri, Size(width, height), null)
        // Upload to the GPU here, on Coil's background thread, so the render thread doesn't
        // have to upload every new tile mid-scroll. Hardware bitmaps also use far less heap.
        val bitmap = decoded.copy(Bitmap.Config.HARDWARE, false)?.also { decoded.recycle() } ?: decoded
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<MediaThumb> {
        override fun create(data: MediaThumb, options: Options, imageLoader: ImageLoader): Fetcher =
            MediaThumbFetcher(data, options)
    }

    private companion object {
        const val DEFAULT_SIZE = 384
        const val MIN_SIZE = 96
        const val MAX_SIZE = 1024
    }
}

class MediaThumbKeyer : Keyer<MediaThumb> {
    override fun key(data: MediaThumb, options: Options): String = "thumb:${data.uri}:${data.version}"
}
