package com.lumen.gallery.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.components.BarAction
import com.lumen.gallery.ui.components.EmptyState
import com.lumen.gallery.ui.components.FloatingTopBar
import com.lumen.gallery.ui.components.Loader
import com.lumen.gallery.ui.components.MediaThumbnail
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.components.ScreenHeader
import com.lumen.gallery.ui.components.TopScrim
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.components.topBarClearance
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.rememberMediaActions
import dev.chrisbanes.haze.rememberHazeState
import java.util.concurrent.TimeUnit

@Composable
fun TrashScreen(vm: GalleryViewModel, onBack: () -> Unit) {
    val trash by vm.trash.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val actions = rememberMediaActions()
    val items = trash.orEmpty()
    val selected = remember(selection, items) { items.filter { it.id in selection } }
    val gridState = rememberLazyGridState()
    val haze = rememberHazeState()
    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    DisposableEffect(Unit) { onDispose { vm.clearSelection() } }
    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            trash == null -> Loader()
            items.isEmpty() -> Column(Modifier.fillMaxSize().padding(top = topBarClearance())) {
                ScreenHeader("Bin", topInset = false, startPadding = 20.dp)
                EmptyState(
                    icon = LumenIcons.Trash,
                    title = "The bin is empty",
                    message = "Deleted photos and videos rest here for 30 days before they're gone for good.",
                    modifier = Modifier.weight(1f),
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = topBarClearance(), bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(haze),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                    Column {
                        ScreenHeader(
                            "Bin",
                            subtitle = "Items are removed after 30 days. Tap to select.",
                            topInset = false,
                        )
                        Row(
                            Modifier.padding(start = 10.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PillButton("Restore all", { actions.restore(items) }, height = 40.dp)
                            PillButton("Empty bin", { actions.deleteForever(items) }, filled = false, height = 40.dp)
                        }
                    }
                }
                items(items, key = { it.id }) { item ->
                    MediaThumbnail(
                        item = item,
                        selected = item.id in selection,
                        selectionMode = true,
                        corner = 10.dp,
                        modifier = Modifier.clickable { vm.toggleSelection(item.id) },
                    ) {
                        Text(
                            daysLeft(item.expiresAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(7.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        TopScrim(Modifier.align(Alignment.TopCenter), extra = 72.dp)
        FloatingTopBar(
            onBack = onBack,
            haze = haze,
            title = "Bin",
            showTitle = scrolled,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        SelectionOverlay(
            count = selection.size,
            onClose = vm::clearSelection,
            onSelectAll = { vm.selectAll(items.map { it.id }) },
            actions = listOf(
                BarAction("Restore", LumenIcons.Restore) { actions.restore(selected) { vm.clearSelection() } },
                BarAction("Delete forever", LumenIcons.DeleteForever, tint = MaterialTheme.colorScheme.error) {
                    actions.deleteForever(selected) { vm.clearSelection() }
                },
            ),
            haze = haze,
        )
    }
}

private fun daysLeft(expiresAtSeconds: Long): String {
    if (expiresAtSeconds <= 0) return "30 days"
    val millis = expiresAtSeconds * 1000 - System.currentTimeMillis()
    val days = TimeUnit.MILLISECONDS.toDays(millis).coerceAtLeast(0)
    return when (days) {
        0L -> "Today"
        1L -> "1 day"
        else -> "$days days"
    }
}
