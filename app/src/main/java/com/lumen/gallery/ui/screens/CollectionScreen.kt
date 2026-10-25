package com.lumen.gallery.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.Sources
import com.lumen.gallery.ui.components.EmptyState
import com.lumen.gallery.ui.components.FloatingTopBar
import com.lumen.gallery.ui.components.MediaGrid
import com.lumen.gallery.ui.components.MenuEntry
import com.lumen.gallery.ui.components.OverflowMenu
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.components.RoundIconButton
import com.lumen.gallery.ui.components.ScreenHeader
import com.lumen.gallery.ui.components.TopScrim
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.components.groupingFor
import com.lumen.gallery.ui.components.topBarClearance
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import com.lumen.gallery.util.rememberMediaActions
import dev.chrisbanes.haze.rememberHazeState

/** A device folder, one of the user's own albums, or the Favorites / Videos collections. */
@Composable
fun CollectionScreen(
    vm: GalleryViewModel,
    source: String,
    onBack: () -> Unit,
    onOpenViewer: (source: String, id: Long, slideshow: Boolean) -> Unit,
    onAddPhotos: (albumId: String) -> Unit,
) {
    val userAlbumId = if (source.startsWith("ualbum_")) source.removePrefix("ualbum_") else null
    val userAlbums by vm.userAlbums.collectAsStateWithLifecycle()
    val userAlbum = userAlbums.firstOrNull { it.id == userAlbumId }

    if (userAlbumId != null && userAlbum == null) {
        // The album was deleted (or never existed).
        LaunchedEffect(Unit) { onBack() }
        return
    }
    if (userAlbum?.hidden == true) {
        LockedGate(vm) { CollectionContent(vm, source, onBack, onOpenViewer, onAddPhotos) }
    } else {
        CollectionContent(vm, source, onBack, onOpenViewer, onAddPhotos)
    }
}

@Composable
private fun CollectionContent(
    vm: GalleryViewModel,
    source: String,
    onBack: () -> Unit,
    onOpenViewer: (source: String, id: Long, slideshow: Boolean) -> Unit,
    onAddPhotos: (albumId: String) -> Unit,
) {
    val items by remember(source) { vm.itemsFor(source) }
        .collectAsStateWithLifecycle(remember(source) { vm.itemsNow(source) })
    val albums by vm.albums.collectAsStateWithLifecycle()
    val userAlbums by vm.userAlbums.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val actions = rememberMediaActions()
    val selected = remember(selection, items) { items.filter { it.id in selection } }
    val gridState = rememberLazyGridState()
    val haze = rememberHazeState()
    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    val folderId = if (source.startsWith("album_")) source.removePrefix("album_").toLongOrNull() else null
    val folder = albums.firstOrNull { it.id == folderId }
    val userAlbum = userAlbums.firstOrNull { source == Sources.userAlbum(it.id) }
    val title = when {
        source == Sources.FAVORITES -> "Favorites"
        source == Sources.VIDEOS -> "Videos"
        userAlbum != null -> userAlbum.name
        else -> folder?.name ?: "Album"
    }
    val eyebrow = when {
        userAlbum?.hidden == true -> "Hidden folder"
        userAlbum != null -> "Your album"
        folderId != null -> "Folder"
        else -> "Collection"
    }
    val hiddenFromTimeline = folderId != null && folderId in settings.hiddenAlbums

    var addTargets by remember { mutableStateOf<List<MediaItem>?>(null) }
    var choosingCover by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { vm.clearSelection() } }
    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }

    val menu = buildList {
        if (userAlbum != null) {
            add(MenuEntry("Add photos", LumenIcons.ImagePlus) { onAddPhotos(userAlbum.id) })
            if (items.isNotEmpty()) add(MenuEntry("Change cover", LumenIcons.Image) { choosingCover = true })
            add(MenuEntry("Rename album", LumenIcons.Rename) { renaming = true })
            add(
                if (userAlbum.hidden) {
                    MenuEntry("Move out of hidden", LumenIcons.Unlock) { vm.setUserAlbumHidden(userAlbum.id, false) }
                } else {
                    MenuEntry("Move to hidden", LumenIcons.FolderLock) { vm.setUserAlbumHidden(userAlbum.id, true) }
                },
            )
            add(MenuEntry("Delete album", LumenIcons.Trash) { confirmDelete = true })
        }
        if (folderId != null) {
            if (items.isNotEmpty()) add(MenuEntry("Change cover", LumenIcons.Image) { choosingCover = true })
            add(
                MenuEntry(
                    label = if (hiddenFromTimeline) "Show in timeline" else "Hide from timeline",
                    icon = if (hiddenFromTimeline) LumenIcons.Eye else LumenIcons.EyeOff,
                ) { vm.setAlbumHidden(folderId, !hiddenFromTimeline) },
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(top = topBarClearance())) {
                ScreenHeader(title, eyebrow = eyebrow, topInset = false, startPadding = 20.dp)
                EmptyState(
                    icon = when {
                        source == Sources.FAVORITES -> LumenIcons.Heart
                        source == Sources.VIDEOS -> LumenIcons.Film
                        userAlbum?.hidden == true -> LumenIcons.FolderLock
                        else -> LumenIcons.Images
                    },
                    title = when {
                        source == Sources.FAVORITES -> "No favorites yet"
                        source == Sources.VIDEOS -> "No videos"
                        userAlbum != null -> "Fill this album"
                        else -> "Empty album"
                    },
                    message = when {
                        source == Sources.FAVORITES -> "Tap the heart on any photo to keep it close."
                        source == Sources.VIDEOS -> "Videos you record or save will appear here."
                        userAlbum?.hidden == true -> "Photos you add here disappear from the rest of Lumen."
                        userAlbum != null -> "Pick photos from your library — they stay where they are, nothing is copied."
                        else -> "Nothing to show here right now."
                    },
                    modifier = Modifier.weight(1f),
                    action = userAlbum?.let { album -> { PillButton("Add photos", { onAddPhotos(album.id) }) } },
                )
            }
        } else {
            MediaGrid(
                items = items,
                columns = settings.gridColumns,
                grouping = groupingFor(settings),
                selection = selection,
                favorites = settings.favorites,
                onOpen = { onOpenViewer(source, it.id, false) },
                onToggleSelect = vm::toggleSelection,
                onSelectGroup = vm::setSelected,
                state = gridState,
                modifier = Modifier.glassSource(haze),
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = topBarClearance(), bottom = 120.dp),
                onColumnsChange = vm::setGridColumns,
                header = {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                        ScreenHeader(title, subtitle = Format.counts(items), eyebrow = eyebrow, topInset = false)
                    }
                },
            )
        }

        TopScrim(Modifier.align(Alignment.TopCenter), extra = 72.dp)
        FloatingTopBar(
            onBack = onBack,
            haze = haze,
            title = title,
            showTitle = scrolled,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            if (userAlbum != null) {
                RoundIconButton(LumenIcons.Plus, "Add photos", { onAddPhotos(userAlbum.id) }, haze = haze)
            }
            if (items.isNotEmpty()) {
                RoundIconButton(LumenIcons.Slideshow, "Slideshow", { onOpenViewer(source, items.first().id, true) }, haze = haze)
            }
            if (menu.isNotEmpty()) OverflowMenu(menu, haze = haze)
        }

        SelectionOverlay(
            count = selection.size,
            onClose = vm::clearSelection,
            onSelectAll = { vm.selectAll(items.map { it.id }) },
            actions = selectionActions(
                selected = selected,
                favorites = settings.favorites,
                vm = vm,
                actions = actions,
                onAddToAlbum = { addTargets = selected },
                onRemoveFromAlbum = userAlbum?.let { album ->
                    {
                        vm.removeFromAlbum(album.id, selected)
                        vm.clearSelection()
                    }
                },
            ),
            haze = haze,
        )
    }

    addTargets?.let { targets ->
        AddToAlbumSheet(
            vm = vm,
            items = targets,
            onDismiss = { addTargets = null },
            onDone = {
                addTargets = null
                vm.clearSelection()
            },
        )
    }

    if (choosingCover) {
        val currentCover = userAlbum?.cover?.id ?: folder?.cover?.id
        CoverPickerSheet(
            items = items,
            currentCoverId = currentCover,
            onDismiss = { choosingCover = false },
            onPick = { item ->
                choosingCover = false
                when {
                    userAlbum != null -> vm.setAlbumCover(userAlbum.id, item.id)
                    folderId != null -> vm.setFolderCover(folderId, item.id)
                }
            },
        )
    }

    if (renaming && userAlbum != null) {
        AlbumNameDialog(
            title = "Rename album",
            confirmLabel = "Save",
            initialName = userAlbum.name,
            showHiddenToggle = false,
            onDismiss = { renaming = false },
            onConfirm = { name, _ ->
                renaming = false
                vm.renameAlbum(userAlbum.id, name)
            },
        )
    }

    if (confirmDelete && userAlbum != null) {
        ConfirmDialog(
            title = "Delete “${userAlbum.name}”?",
            message = if (userAlbum.hidden) {
                "The folder is removed and its photos return to your library. No photos are deleted."
            } else {
                "Only the album is removed. The photos stay in your library."
            },
            confirmLabel = "Delete album",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.deleteAlbum(userAlbum.id)
            },
        )
    }
}
