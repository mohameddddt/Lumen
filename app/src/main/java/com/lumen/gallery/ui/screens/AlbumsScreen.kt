package com.lumen.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lumen.gallery.data.Album
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.Sources
import com.lumen.gallery.ui.components.EmptyState
import com.lumen.gallery.ui.components.RoundIconButton
import com.lumen.gallery.ui.components.ScreenHeader
import com.lumen.gallery.ui.components.SectionLabel
import com.lumen.gallery.ui.components.TopScrim
import com.lumen.gallery.ui.components.dockClearance
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import dev.chrisbanes.haze.HazeState

@Composable
fun AlbumsScreen(
    vm: GalleryViewModel,
    gridState: LazyGridState,
    haze: HazeState,
    onOpenCollection: (String) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    val userAlbums by vm.userAlbums.collectAsStateWithLifecycle()
    val ownAlbums = remember(userAlbums) { userAlbums.filterNot { it.hidden } }
    var creating by remember { mutableStateOf(false) }
    val all by vm.sorted.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val favorites = remember(all, settings.favorites) { all.filter { it.id in settings.favorites } }
    val videos = remember(all) { all.filter { it.isVideo } }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = dockClearance()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .glassSource(haze),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                ScreenHeader(
                    "Albums",
                    subtitle = Format.plural(albums.size + ownAlbums.size, "album"),
                    startPadding = 4.dp,
                    endPadding = 0.dp,
                ) {
                    RoundIconButton(LumenIcons.Plus, "New album", { creating = true })
                    RoundIconButton(LumenIcons.Settings, "Settings", onOpenSettings)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "collections") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.layout { measurable, constraints ->
                        // Bleed the row to the screen edges so tiles scroll past the grid padding.
                        val bleed = 16.dp.roundToPx()
                        val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + bleed * 2))
                        layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
                    },
                ) {
                    item {
                        CollectionTile("Favorites", favorites.size.toString(), LumenIcons.Heart, favorites.firstOrNull()) {
                            onOpenCollection(Sources.FAVORITES)
                        }
                    }
                    item {
                        CollectionTile("Videos", videos.size.toString(), LumenIcons.Film, videos.firstOrNull()) {
                            onOpenCollection(Sources.VIDEOS)
                        }
                    }
                    item { CollectionTile("Hidden", "Locked", LumenIcons.Lock, null, onClick = onOpenVault) }
                    item { CollectionTile("Bin", "30 days", LumenIcons.Trash, null, onClick = onOpenTrash) }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "ownLabel") {
                SectionLabel("Your albums", Modifier.padding(top = 8.dp), trailing = ownAlbums.size.takeIf { it > 0 }?.toString())
            }
            item(key = "newAlbum") {
                NewAlbumCard("New album", LumenIcons.Plus) { creating = true }
            }
            items(ownAlbums, key = { it.id }) { album ->
                UserAlbumCard(album) { onOpenCollection(Sources.userAlbum(album.id)) }
            }
            if (albums.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    EmptyState(
                        icon = LumenIcons.Folder,
                        title = "No folders yet",
                        message = "Folders with photos or videos will appear here.",
                        modifier = Modifier.height(320.dp),
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }, key = "label") {
                    SectionLabel("On this device", Modifier.padding(top = 8.dp), trailing = albums.size.toString())
                }
                itemsIndexed(
                    albums,
                    key = { _, album -> album.id },
                    span = { index, _ -> if (index == 0) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                ) { index, album ->
                    val hidden = album.id in settings.hiddenAlbums
                    if (index == 0) {
                        FeaturedAlbum(album, hidden) { onOpenCollection(Sources.album(album.id)) }
                    } else {
                        AlbumCard(album, hidden) { onOpenCollection(Sources.album(album.id)) }
                    }
                }
            }
        }
        TopScrim(Modifier.align(Alignment.TopCenter))
    }

    if (creating) {
        AlbumNameDialog(
            title = "New album",
            confirmLabel = "Create",
            onDismiss = { creating = false },
            onConfirm = { name, hidden ->
                creating = false
                vm.createAlbum(name, hidden, emptyList()) { id -> onOpenCollection(Sources.userAlbum(id)) }
            },
        )
    }
}

@Composable
private fun CollectionTile(
    label: String,
    count: String,
    icon: ImageVector,
    cover: MediaItem?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .width(118.dp)
            .aspectRatio(0.82f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, Lumen.extras.hairline, shape)
            .clickable(onClick = onClick),
    ) {
        val onImage = cover != null
        if (cover != null) {
            AsyncImage(model = cover.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.25f to Color.Black.copy(alpha = 0.1f), 1f to Color.Black.copy(alpha = 0.7f))),
            )
        }
        val fg = if (onImage) Color.White else MaterialTheme.colorScheme.onSurface
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.padding(14.dp).size(20.dp))
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = fg, maxLines = 1)
            Text(
                count,
                style = MaterialTheme.typography.labelSmall,
                color = if (onImage) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FeaturedAlbum(album: Album, hidden: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(model = album.cover.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.65f))),
        )
        if (hidden) HiddenBadge(Modifier.align(Alignment.TopEnd))
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Text(album.name, style = MaterialTheme.typography.displaySmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${Format.plural(album.count, "item")} · updated ${Format.dayMonth(album.cover.dateTaken)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun AlbumCard(album: Album, hidden: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(model = album.cover.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (hidden) HiddenBadge(Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            album.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            album.count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun HiddenBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(10.dp)
            .size(30.dp)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(LumenIcons.EyeOff, contentDescription = "Hidden from Photos", tint = Color.White, modifier = Modifier.size(15.dp))
    }
}
