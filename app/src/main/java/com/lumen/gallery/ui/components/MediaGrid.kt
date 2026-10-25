package com.lumen.gallery.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.data.Settings
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import java.time.LocalDate
import java.time.YearMonth
import java.util.TimeZone

enum class Grouping { NONE, DAY, MONTH }

fun groupingFor(settings: Settings): Grouping = when {
    !settings.sortOrder.isByDate -> Grouping.NONE
    settings.gridColumns >= 5 -> Grouping.MONTH
    else -> Grouping.DAY
}

private sealed interface GridEntry {
    data class Header(val key: String, val title: String, val subtitle: String, val ids: List<Long>) : GridEntry
    data class Cell(val item: MediaItem) : GridEntry
}

private const val DAY_MILLIS = 86_400_000L

private fun buildEntries(items: List<MediaItem>, grouping: Grouping): List<GridEntry> {
    if (grouping == Grouping.NONE) return items.map { GridEntry.Cell(it) }
    val today = LocalDate.now()
    val out = ArrayList<GridEntry>(items.size + 64)
    var currentKey: Any? = null
    var headerIndex = -1
    var ids = ArrayList<Long>()

    fun closeGroup() {
        if (headerIndex >= 0) out[headerIndex] = (out[headerIndex] as GridEntry.Header).copy(ids = ids)
    }

    // Turning every timestamp into a LocalDate is slow for big libraries, so bucket by epoch day
    // with plain arithmetic and only build date objects when the day actually changes.
    val zone = TimeZone.getDefault()
    var lastDay = Long.MIN_VALUE
    var key: Any? = null
    for (item in items) {
        val day = Math.floorDiv(item.dateTaken + zone.getOffset(item.dateTaken), DAY_MILLIS)
        if (day != lastDay) {
            lastDay = day
            val date = LocalDate.ofEpochDay(day)
            key = if (grouping == Grouping.DAY) date else YearMonth.from(date)
        }
        if (key != currentKey) {
            closeGroup()
            ids = ArrayList()
            currentKey = key
            headerIndex = out.size
            out += if (key is LocalDate) {
                GridEntry.Header(key.toString(), Format.dayTitle(key, today), Format.daySubtitle(key, today), emptyList())
            } else {
                val month = key as YearMonth
                GridEntry.Header(key.toString(), Format.monthName(month), month.year.toString(), emptyList())
            }
        }
        ids += item.id
        out += GridEntry.Cell(item)
    }
    closeGroup()
    return out
}

/**
 * The photo grid shared by every collection screen: serif date headers, tap to open,
 * long-press to multi-select, and pinch to change the number of columns.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    columns: Int,
    grouping: Grouping,
    selection: Set<Long>,
    favorites: Set<Long>,
    onOpen: (MediaItem) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectGroup: (ids: List<Long>, selected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    onColumnsChange: ((Int) -> Unit)? = null,
    alwaysSelect: Boolean = false,
    header: LazyGridScope.() -> Unit = {},
) {
    val entries = remember(items, grouping) { buildEntries(items, grouping) }
    val selectionMode = alwaysSelect || selection.isNotEmpty()
    val haptics = LocalHapticFeedback.current
    val currentColumns by rememberUpdatedState(columns)
    val changeColumns by rememberUpdatedState(onColumnsChange)
    val corner = when {
        columns <= 2 -> 14.dp
        columns == 3 -> 10.dp
        else -> 6.dp
    }

    val pinch = if (onColumnsChange == null) Modifier else Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var zoom = 1f
            var handled = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    if (!handled) {
                        zoom *= event.calculateZoom()
                        val next = when {
                            zoom > 1.25f -> currentColumns - 1
                            zoom < 0.8f -> currentColumns + 1
                            else -> currentColumns
                        }.coerceIn(Settings.MIN_COLUMNS, Settings.MAX_COLUMNS)
                        if (next != currentColumns) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            changeColumns?.invoke(next)
                            handled = true
                        }
                    }
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier.fillMaxSize().then(pinch),
    ) {
        header()
        items(
            items = entries,
            key = { entry ->
                when (entry) {
                    is GridEntry.Header -> "h_${entry.key}"
                    is GridEntry.Cell -> entry.item.id
                }
            },
            span = { entry -> if (entry is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            contentType = { entry -> if (entry is GridEntry.Header) 0 else 1 },
        ) { entry ->
            when (entry) {
                is GridEntry.Header -> {
                    val allSelected = selectionMode && entry.ids.all { it in selection }
                    GroupHeader(
                        header = entry,
                        selectionMode = selectionMode,
                        allSelected = allSelected,
                        onToggle = { onSelectGroup(entry.ids, !allSelected) },
                    )
                }
                is GridEntry.Cell -> {
                    val item = entry.item
                    MediaThumbnail(
                        item = item,
                        selected = item.id in selection,
                        selectionMode = selectionMode,
                        favorite = item.id in favorites,
                        corner = corner,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { if (selectionMode) onToggleSelect(item.id) else onOpen(item) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleSelect(item.id)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(header: GridEntry.Header, selectionMode: Boolean, allSelected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 6.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(header.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(10.dp))
        Text(
            header.subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 4.dp),
            maxLines = 1,
        )
        if (selectionMode) {
            Box(
                Modifier
                    .padding(bottom = 2.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .then(
                        if (allSelected) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier.border(1.5.dp, Lumen.extras.hairline, CircleShape),
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                if (allSelected) {
                    Icon(LumenIcons.Check, contentDescription = "Deselect group", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            Text(
                header.ids.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}
