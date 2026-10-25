package com.lumen.gallery.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.components.DockItem
import com.lumen.gallery.ui.components.FloatingDock
import com.lumen.gallery.ui.theme.LumenIcons
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

enum class HomeTab(val label: String, val icon: ImageVector) {
    PHOTOS("Photos", LumenIcons.Image),
    ALBUMS("Albums", LumenIcons.Layers),
    SEARCH("Search", LumenIcons.Search),
}

@Composable
fun HomeScreen(
    vm: GalleryViewModel,
    partialAccess: Boolean,
    onManageAccess: () -> Unit,
    onOpenViewer: (source: String, id: Long) -> Unit,
    onOpenCollection: (source: String) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.PHOTOS) }
    val photosGrid = rememberLazyGridState()
    val albumsGrid = rememberLazyGridState()
    val searchGrid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val haze = rememberHazeState()
    val selection by vm.selection.collectAsStateWithLifecycle()

    BackHandler(enabled = tab != HomeTab.PHOTOS) {
        vm.clearSelection()
        tab = HomeTab.PHOTOS
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Crossfade(targetState = tab, animationSpec = tween(200), label = "homeTabs") { current ->
            when (current) {
                HomeTab.PHOTOS -> PhotosScreen(
                    vm = vm,
                    gridState = photosGrid,
                    haze = haze,
                    partialAccess = partialAccess,
                    onManageAccess = onManageAccess,
                    onOpenViewer = onOpenViewer,
                    onOpenTrash = onOpenTrash,
                    onOpenSettings = onOpenSettings,
                )
                HomeTab.ALBUMS -> AlbumsScreen(
                    vm = vm,
                    gridState = albumsGrid,
                    haze = haze,
                    onOpenCollection = onOpenCollection,
                    onOpenTrash = onOpenTrash,
                    onOpenVault = onOpenVault,
                    onOpenSettings = onOpenSettings,
                )
                HomeTab.SEARCH -> SearchScreen(
                    vm = vm,
                    gridState = searchGrid,
                    haze = haze,
                    onOpenViewer = onOpenViewer,
                    onOpenCollection = onOpenCollection,
                )
            }
        }

        AnimatedVisibility(
            visible = selection.isEmpty(),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingDock(
                items = HomeTab.entries.map { DockItem(it.label, it.icon) },
                selectedIndex = tab.ordinal,
                haze = haze,
                onSelect = { index ->
                    val target = HomeTab.entries[index]
                    if (target == tab) {
                        val grid = when (target) {
                            HomeTab.PHOTOS -> photosGrid
                            HomeTab.ALBUMS -> albumsGrid
                            HomeTab.SEARCH -> searchGrid
                        }
                        scope.launch { grid.animateScrollToItem(0) }
                    } else {
                        vm.clearSelection()
                        tab = target
                    }
                },
            )
        }
    }
}
