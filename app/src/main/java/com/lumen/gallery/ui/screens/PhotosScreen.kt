package com.lumen.gallery.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lumen.gallery.data.Memory
import com.lumen.gallery.data.SortOrder
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.Sources
import com.lumen.gallery.ui.components.EmptyState
import com.lumen.gallery.ui.components.Eyebrow
import com.lumen.gallery.ui.components.Loader
import com.lumen.gallery.ui.components.MediaGrid
import com.lumen.gallery.ui.components.MenuEntry
import com.lumen.gallery.ui.components.MenuPopup
import com.lumen.gallery.ui.components.OverflowMenu
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.components.RoundIconButton
import com.lumen.gallery.ui.components.ScreenHeader
import com.lumen.gallery.ui.components.TopScrim
import com.lumen.gallery.ui.components.dockClearance
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.components.groupingFor
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import com.lumen.gallery.util.rememberMediaActions
import dev.chrisbanes.haze.HazeState

@Composable
fun PhotosScreen(
    vm: GalleryViewModel,
    gridState: LazyGridState,
    haze: HazeState,
    partialAccess: Boolean,
    onManageAccess: () -> Unit,
    onOpenViewer: (source: String, id: Long) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val timeline by vm.timeline.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    val actions = rememberMediaActions()
    val items = timeline.orEmpty()
    val selected = remember(selection, items) { items.filter { it.id in selection } }
    var addTargets by remember { mutableStateOf<List<com.lumen.gallery.data.MediaItem>?>(null) }

    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }

    val headerActions: @Composable () -> Unit = {
        SortButton(settings.sortOrder, vm::setSortOrder)
        OverflowMenu(
            listOf(
                MenuEntry("Recently deleted", LumenIcons.Trash, onClick = onOpenTrash),
                MenuEntry("Settings", LumenIcons.Settings, onClick = onOpenSettings),
            ),
        )
    }

    Box(Modifier.fillMaxSize()) {
        when {
            timeline == null -> Loader()
            items.isEmpty() -> Column(Modifier.fillMaxSize()) {
                ScreenHeader("Photos") { headerActions() }
                EmptyState(
                    icon = LumenIcons.Image,
                    title = "Nothing here yet",
                    message = if (partialAccess) "Lumen can only see the photos you chose. Pick more to see them here."
                    else "Photos and videos you take or save will appear here.",
                    modifier = Modifier.weight(1f),
                    action = if (partialAccess) ({ PillButton("Choose photos", onManageAccess) }) else null,
                )
            }
            else -> MediaGrid(
                items = items,
                columns = settings.gridColumns,
                grouping = groupingFor(settings),
                selection = selection,
                favorites = settings.favorites,
                onOpen = { onOpenViewer(Sources.ALL, it.id) },
                onToggleSelect = vm::toggleSelection,
                onSelectGroup = vm::setSelected,
                state = gridState,
                modifier = Modifier.glassSource(haze),
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = dockClearance()),
                onColumnsChange = vm::setGridColumns,
                header = {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        ScreenHeader("Photos", subtitle = Format.counts(items)) { headerActions() }
                    }
                    if (partialAccess) {
                        item(key = "partial", span = { GridItemSpan(maxLineSpan) }, contentType = "banner") {
                            PartialAccessBanner(onManageAccess)
                        }
                    }
                    if (settings.showMemories && memories.isNotEmpty()) {
                        item(key = "memories", span = { GridItemSpan(maxLineSpan) }, contentType = "memories") {
                            MemoriesRow(memories) { memory ->
                                onOpenViewer(Sources.memory(memory.year), memory.items.first().id)
                            }
                        }
                    }
                },
            )
        }
        TopScrim(Modifier.align(Alignment.TopCenter))
        SelectionOverlay(
            count = selection.size,
            onClose = vm::clearSelection,
            onSelectAll = { vm.selectAll(items.map { it.id }) },
            actions = selectionActions(selected, settings.favorites, vm, actions, onAddToAlbum = { addTargets = selected }),
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
}

@Composable
private fun SortButton(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(LumenIcons.Sort, "Sort", { open = true })
        MenuPopup(
            expanded = open,
            onDismiss = { open = false },
            entries = SortOrder.entries.map { order ->
                MenuEntry(order.label, selected = order == current) { onSelect(order) }
            },
        )
    }
}

@Composable
private fun PartialAccessBanner(onManage: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, Lumen.extras.hairline, RoundedCornerShape(22.dp))
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LumenIcons.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            "Showing only the photos you chose",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        PillButton("Manage", onManage, height = 38.dp)
    }
}

@Composable
private fun MemoriesRow(memories: List<Memory>, onOpen: (Memory) -> Unit) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Eyebrow("On this day", Modifier.padding(start = 10.dp, top = 4.dp, bottom = 12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(memories, key = { it.year }) { memory ->
                MemoryCard(memory, onClick = { onOpen(memory) })
            }
        }
    }
}

@Composable
private fun MemoryCard(memory: Memory, onClick: () -> Unit) {
    val cover = memory.items.first()
    Box(
        Modifier
            .size(width = 156.dp, height = 214.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = cover.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f))),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                if (memory.yearsAgo == 1) "A year ago" else "${memory.yearsAgo} years ago",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                "${Format.dayMonth(cover.dateTaken)} ${memory.year} · ${memory.items.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
