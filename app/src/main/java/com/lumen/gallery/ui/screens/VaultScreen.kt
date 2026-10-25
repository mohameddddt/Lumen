package com.lumen.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.Sources
import com.lumen.gallery.ui.components.FloatingTopBar
import com.lumen.gallery.ui.components.RoundIconButton
import com.lumen.gallery.ui.components.ScreenHeader
import com.lumen.gallery.ui.components.TopScrim
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.components.topBarClearance
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import dev.chrisbanes.haze.rememberHazeState

/** The locked list of hidden folders. */
@Composable
fun VaultScreen(vm: GalleryViewModel, onBack: () -> Unit, onOpenCollection: (String) -> Unit) {
    LockedGate(vm) {
        val albums by vm.userAlbums.collectAsStateWithLifecycle()
        val hidden = albums.filter { it.hidden }
        val gridState = rememberLazyGridState()
        val haze = rememberHazeState()
        val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
        var creating by remember { mutableStateOf(false) }

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topBarClearance(), bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(haze),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                    ScreenHeader(
                        "Hidden",
                        eyebrow = "Locked",
                        subtitle = Format.plural(hidden.size, "folder") + " · ${Format.plural(hidden.sumOf { it.items.size }, "item")}",
                        topInset = false,
                        startPadding = 4.dp,
                        endPadding = 0.dp,
                    )
                }
                item(key = "new") {
                    NewAlbumCard("New hidden folder", LumenIcons.FolderLock) { creating = true }
                }
                items(hidden, key = { it.id }) { album ->
                    UserAlbumCard(album) { onOpenCollection(Sources.userAlbum(album.id)) }
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "note") {
                    Text(
                        "Hidden folders stay out of Photos, Albums, Search and Memories, and lock again whenever you leave Lumen. " +
                            "The files remain on your phone, so other gallery apps can still see them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    )
                }
            }

            TopScrim(Modifier.align(Alignment.TopCenter), extra = 72.dp)
            FloatingTopBar(
                onBack = onBack,
                haze = haze,
                title = "Hidden",
                showTitle = scrolled,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                RoundIconButton(
                    LumenIcons.Lock,
                    "Lock now",
                    {
                        vm.lockVault()
                        onBack()
                    },
                    haze = haze,
                )
            }
        }

        if (creating) {
            AlbumNameDialog(
                title = "New hidden folder",
                confirmLabel = "Create",
                initialHidden = true,
                showHiddenToggle = false,
                onDismiss = { creating = false },
                onConfirm = { name, _ ->
                    creating = false
                    vm.createAlbum(name, hidden = true, items = emptyList()) { id ->
                        onOpenCollection(Sources.userAlbum(id))
                    }
                },
            )
        }
    }
}
