package com.lumen.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.components.EmptyState
import com.lumen.gallery.ui.components.FloatingTopBar
import com.lumen.gallery.ui.components.MediaGrid
import com.lumen.gallery.ui.components.ScreenHeader
import com.lumen.gallery.ui.components.TopScrim
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.components.groupingFor
import com.lumen.gallery.ui.components.topBarClearance
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import dev.chrisbanes.haze.rememberHazeState

/** Full-screen multi-select of the library, for adding photos to a user album. */
@Composable
fun PickerScreen(vm: GalleryViewModel, albumId: String, onBack: () -> Unit) {
    val albums by vm.userAlbums.collectAsStateWithLifecycle()
    val album = albums.firstOrNull { it.id == albumId }
    if (album == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    if (album.hidden) {
        LockedGate(vm) { PickerContent(vm, albumId, onBack) }
    } else {
        PickerContent(vm, albumId, onBack)
    }
}

@Composable
private fun PickerContent(vm: GalleryViewModel, albumId: String, onBack: () -> Unit) {
    val albums by vm.userAlbums.collectAsStateWithLifecycle()
    val album = albums.firstOrNull { it.id == albumId } ?: return
    val timeline by vm.timeline.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val haze = rememberHazeState()
    val existing = remember(album.items) { album.items.mapTo(HashSet()) { it.id } }
    val candidates = remember(timeline, existing) { timeline.orEmpty().filter { it.id !in existing } }
    var picked by remember { mutableStateOf(emptySet<Long>()) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (candidates.isEmpty()) {
            EmptyState(
                icon = LumenIcons.Images,
                title = "Nothing left to add",
                message = "Every photo in your library is already in this album.",
            )
        } else {
            MediaGrid(
                items = candidates,
                columns = settings.gridColumns,
                grouping = groupingFor(settings),
                selection = picked,
                favorites = settings.favorites,
                onOpen = {},
                onToggleSelect = { id -> picked = if (id in picked) picked - id else picked + id },
                onSelectGroup = { ids, select -> picked = if (select) picked + ids else picked - ids.toSet() },
                alwaysSelect = true,
                modifier = Modifier.glassSource(haze),
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = topBarClearance(), bottom = 140.dp),
                onColumnsChange = vm::setGridColumns,
                header = {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                        ScreenHeader(
                            "Add photos",
                            eyebrow = album.name,
                            subtitle = if (picked.isEmpty()) "Tap photos to choose them" else Format.plural(picked.size, "photo") + " chosen",
                            topInset = false,
                        )
                    }
                },
            )
        }

        TopScrim(Modifier.align(Alignment.TopCenter), extra = 72.dp)
        FloatingTopBar(onBack = onBack, haze = haze, modifier = Modifier.align(Alignment.TopCenter))

        PillButton(
            text = if (picked.isEmpty()) "Choose photos" else "Add ${Format.plural(picked.size, "photo")}",
            onClick = {
                vm.addToAlbum(album, candidates.filter { it.id in picked })
                onBack()
            },
            enabled = picked.isNotEmpty(),
            height = 58.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
                .shadow(16.dp, androidx.compose.foundation.shape.CircleShape),
        )
    }
}
