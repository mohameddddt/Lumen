package com.lumen.gallery.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format

private val BadgeShadow = Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 6f)

@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    favorite: Boolean = false,
    corner: Dp = 6.dp,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    // Animations only exist while selecting; ordinary scrolling composes plain, static tiles.
    val scale: Float
    val radius: Dp
    if (selectionMode) {
        scale = animateFloatAsState(
            if (selected) 0.86f else 1f,
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
            label = "thumbScale",
        ).value
        radius = animateDpAsState(if (selected) 18.dp else corner, label = "thumbCorner").value
    } else {
        scale = 1f
        radius = corner
    }
    val hasBadge = item.isVideo || item.isGif || favorite
    Box(modifier.aspectRatio(1f)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shape = RoundedCornerShape(radius)
                    clip = true
                }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = item.thumb,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (hasBadge) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f))),
                )
            }
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (favorite) {
                    Icon(LumenIcons.HeartFilled, contentDescription = "Favorite", tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    item.isVideo -> {
                        Icon(LumenIcons.PlayFilled, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        if (item.durationMs > 0) BadgeText(Format.duration(item.durationMs))
                    }
                    item.isGif -> BadgeText("GIF")
                }
            }
            overlay()
        }
        if (selectionMode) {
            SelectionIndicator(
                selected = selected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun BadgeText(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall.copy(shadow = BadgeShadow),
    )
}

@Composable
fun SelectionIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Box(
            modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LumenIcons.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    } else {
        Box(
            modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.18f))
                .border(1.5.dp, Color.White.copy(alpha = 0.95f), CircleShape),
        )
    }
}
