package com.lumen.gallery.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lumen.gallery.data.MediaItem
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private const val MAX_SCALE = 6f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * A full-screen photo with pinch-to-zoom, pan and double-tap zoom. While not zoomed,
 * single-finger drags are left unconsumed so the surrounding pager can swipe.
 */
@Composable
fun ZoomableImage(
    item: MediaItem,
    active: Boolean,
    onTap: () -> Unit,
    onZoomChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var fullLoaded by remember(item.id) { mutableStateOf(false) }
    val tap by rememberUpdatedState(onTap)
    val zoomChanged by rememberUpdatedState(onZoomChange)

    val zoomed = scale > 1.01f
    LaunchedEffect(zoomed) { zoomChanged(zoomed) }
    LaunchedEffect(active) {
        if (!active) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    fun clamp(target: Offset, s: Float): Offset {
        val bw = boxSize.width.toFloat()
        val bh = boxSize.height.toFloat()
        val fit = if (item.width > 0 && item.height > 0) min(bw / item.width, bh / item.height) else 1f
        val cw = if (item.width > 0) item.width * fit else bw
        val ch = if (item.height > 0) item.height * fit else bh
        val maxX = max(0f, (cw * s - bw) / 2f)
        val maxY = max(0f, (ch * s - bh) / 2f)
        return Offset(target.x.coerceIn(-maxX, maxX), target.y.coerceIn(-maxY, maxY))
    }

    fun animateTo(targetScale: Float, targetOffset: Offset) {
        val startScale = scale
        val startOffset = offset
        scope.launch {
            animate(0f, 1f, animationSpec = tween(260)) { v, _ ->
                scale = lerp(startScale, targetScale, v)
                offset = lerp(startOffset, targetOffset, v)
            }
        }
    }

    val fullRequest = remember(item.id, item.dateModified) {
        ImageRequest.Builder(context).data(item.uri).size(2560, 2560).crossfade(200).build()
    }
    val previewRequest = remember(item.id, item.dateModified) {
        ImageRequest.Builder(context).data(item.thumb).size(512, 512).build()
    }

    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tap() },
                    onDoubleTap = { position ->
                        if (scale > 1.01f) {
                            animateTo(1f, Offset.Zero)
                        } else {
                            val center = Offset(boxSize.width / 2f, boxSize.height / 2f)
                            animateTo(DOUBLE_TAP_SCALE, clamp((center - position) * (DOUBLE_TAP_SCALE - 1f), DOUBLE_TAP_SCALE))
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2 || scale > 1.01f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                            val center = Offset(boxSize.width / 2f, boxSize.height / 2f)
                            val anchor = if (centroid.isSpecified) centroid - center else Offset.Zero
                            val newOffset = (offset - anchor) * (newScale / scale) + anchor + pan
                            scale = newScale
                            offset = clamp(newOffset, newScale)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale < 1.02f) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            if (!fullLoaded) {
                AsyncImage(
                    model = previewRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AsyncImage(
                model = fullRequest,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                onSuccess = { fullLoaded = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
