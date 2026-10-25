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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lumen.gallery.data.Album
import com.lumen.gallery.data.MediaFilter
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.Sources
import com.lumen.gallery.ui.components.EmptyState
import com.lumen.gallery.ui.components.MediaGrid
import com.lumen.gallery.ui.components.PillChip
import com.lumen.gallery.ui.components.SectionLabel
import com.lumen.gallery.ui.components.dockClearance
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.components.groupingFor
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import com.lumen.gallery.util.rememberMediaActions
import dev.chrisbanes.haze.HazeState
import java.time.YearMonth

@Composable
fun SearchScreen(
    vm: GalleryViewModel,
    gridState: LazyGridState,
    haze: HazeState,
    onOpenViewer: (source: String, id: Long) -> Unit,
    onOpenCollection: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(vm.query.value) }
    val filter by vm.filter.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val months by vm.months.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val actions = rememberMediaActions()
    val focus = LocalFocusManager.current
    val selected = remember(selection, results) { results.items.filter { it.id in selection } }
    var addTargets by remember { mutableStateOf<List<com.lumen.gallery.data.MediaItem>?>(null) }

    val active = text.isNotBlank() || filter != MediaFilter.ALL
    val upToDate = results.query == text.trim() && results.filter == filter
    val setText: (String) -> Unit = {
        text = it
        vm.setQuery(it)
    }

    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
            ) {
                Text(
                    "Search",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 14.dp),
                )
                SearchField(
                    value = text,
                    onValueChange = setText,
                    onSearch = { focus.clearFocus() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(MediaFilter.entries.drop(1)) { f ->
                        val isSelected = f == filter
                        PillChip(
                            text = f.label,
                            selected = isSelected,
                            onClick = { vm.setFilter(if (isSelected) MediaFilter.ALL else f) },
                            icon = if (isSelected) LumenIcons.Check else null,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .glassSource(haze),
            ) {
                when {
                    !active -> Suggestions(
                        albums = albums,
                        months = months,
                        onAlbum = { onOpenCollection(Sources.album(it.id)) },
                        onMonth = { setText(Format.monthYear(it)) },
                    )
                    !upToDate && results.items.isEmpty() -> Unit
                    results.items.isEmpty() -> EmptyState(
                        icon = LumenIcons.SearchX,
                        title = "No matches",
                        message = "Try a file name, an album, or a month like “${months.firstOrNull()?.first?.let(Format::monthYear) ?: "June 2024"}”.",
                    )
                    else -> MediaGrid(
                        items = results.items,
                        columns = settings.gridColumns,
                        grouping = groupingFor(settings),
                        selection = selection,
                        favorites = settings.favorites,
                        onOpen = { onOpenViewer(Sources.SEARCH, it.id) },
                        onToggleSelect = vm::toggleSelection,
                        onSelectGroup = vm::setSelected,
                        state = gridState,
                        contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = dockClearance()),
                        onColumnsChange = vm::setGridColumns,
                        header = {
                            item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                                SectionLabel(
                                    Format.plural(results.items.size, "result"),
                                    Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp),
                                )
                            }
                        },
                    )
                }
            }
        }

        SelectionOverlay(
            count = selection.size,
            onClose = vm::clearSelection,
            onSelectAll = { vm.selectAll(results.items.map { it.id }) },
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
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LumenIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text("Names, albums, months…", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(LumenIcons.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Suggestions(
    albums: List<Album>,
    months: List<Pair<YearMonth, Int>>,
    onAlbum: (Album) -> Unit,
    onMonth: (YearMonth) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = dockClearance()),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (albums.isNotEmpty()) {
            item { SectionLabel("Albums", Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(albums.take(16), key = { it.id }) { album ->
                        Column(
                            Modifier
                                .width(78.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .clickable { onAlbum(album) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = album.cover.thumb,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(74.dp)
                                    .border(1.dp, Lumen.extras.hairline, CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            )
                            Text(
                                album.name,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        if (months.isNotEmpty()) {
            item { SectionLabel("Moments", Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 4.dp)) }
            items(months, key = { it.first.toString() }) { (month, count) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onMonth(month) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Format.monthName(month), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        month.year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        LumenIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp).size(18.dp),
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Lumen.extras.hairline)
            }
        }
    }
}
