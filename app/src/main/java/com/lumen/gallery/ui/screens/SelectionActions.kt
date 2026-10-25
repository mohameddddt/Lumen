package com.lumen.gallery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.components.BarAction
import com.lumen.gallery.ui.components.SelectionDock
import com.lumen.gallery.ui.theme.FavoriteRed
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.MediaActions
import dev.chrisbanes.haze.HazeState

/**
 * Actions shown while items are selected in any grid: share, favorite, add to (or remove from)
 * an album, and delete.
 */
fun selectionActions(
    selected: List<MediaItem>,
    favorites: Set<Long>,
    vm: GalleryViewModel,
    actions: MediaActions,
    onAddToAlbum: (() -> Unit)? = null,
    onRemoveFromAlbum: (() -> Unit)? = null,
): List<BarAction> {
    val allFavorite = selected.isNotEmpty() && selected.all { it.id in favorites }
    return buildList {
        add(BarAction("Share", LumenIcons.Share) { actions.share(selected) })
        add(
            BarAction(
                label = if (allFavorite) "Unfavorite" else "Favorite",
                icon = if (allFavorite) LumenIcons.HeartFilled else LumenIcons.Heart,
                tint = if (allFavorite) FavoriteRed else null,
            ) {
                vm.toggleFavorites(selected.map { it.id })
                vm.clearSelection()
            },
        )
        when {
            onRemoveFromAlbum != null -> add(BarAction("Remove from album", LumenIcons.ImageMinus, onClick = onRemoveFromAlbum))
            onAddToAlbum != null -> add(BarAction("Add to album", LumenIcons.ImagePlus, onClick = onAddToAlbum))
        }
        add(
            BarAction("Delete", LumenIcons.Trash) {
                actions.moveToTrash(selected) { vm.clearSelection() }
            },
        )
    }
}

/** Slides the selection dock up from the bottom edge while anything is selected. */
@Composable
fun BoxScope.SelectionOverlay(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    actions: List<BarAction>,
    haze: HazeState?,
) {
    AnimatedVisibility(
        visible = count > 0,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        SelectionDock(count = count, onClose = onClose, onSelectAll = onSelectAll, actions = actions, haze = haze)
    }
}
