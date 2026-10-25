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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.data.UserAlbumView
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.components.Eyebrow
import com.lumen.gallery.ui.components.LumenSwitch
import com.lumen.gallery.ui.components.MediaThumbnail
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import com.lumen.gallery.util.SecureContent
import com.lumen.gallery.util.rememberVaultUnlocker

// Dialogs

/** Name an album; optionally choose whether it is a locked hidden folder. */
@Composable
fun AlbumNameDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, hidden: Boolean) -> Unit,
    initialName: String = "",
    initialHidden: Boolean = false,
    showHiddenToggle: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initialName) }
    var hidden by remember { mutableStateOf(initialHidden) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surfaceContainerHigh)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (name.isEmpty()) {
                        Text("Album name", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
                if (showHiddenToggle) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(role = Role.Switch) { hidden = !hidden }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(LumenIcons.FolderLock, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Hidden folder", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Locked, and kept out of Photos, Albums and Search",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        LumenSwitch(checked = hidden)
                    }
                }
            }
        },
        confirmButton = {
            PillButton(confirmLabel, { onConfirm(name.trim(), hidden) }, enabled = name.isNotBlank(), height = 44.dp)
        },
        dismissButton = { PillButton("Cancel", onDismiss, filled = false, height = 44.dp) },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = { PillButton(confirmLabel, onConfirm, height = 44.dp) },
        dismissButton = { PillButton("Cancel", onDismiss, filled = false, height = 44.dp) },
    )
}

// Sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LumenSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Lumen.extras.hairline, CircleShape),
            )
        },
    ) { content() }
}

/** Pick (or create) an album to add [items] to. Hidden folders only appear after unlocking. */
@Composable
fun AddToAlbumSheet(
    vm: GalleryViewModel,
    items: List<MediaItem>,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val albums by vm.userAlbums.collectAsStateWithLifecycle()
    val unlocked by vm.vaultUnlocked.collectAsStateWithLifecycle()
    val unlock = rememberVaultUnlocker { vm.unlockVault() }
    var creatingHidden by remember { mutableStateOf<Boolean?>(null) }
    val visibleAlbums = albums.filterNot { it.hidden }
    val hiddenAlbums = albums.filter { it.hidden }

    LumenSheet(onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Add to album", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
            Text(
                "${Format.plural(items.size, "item")} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 12.dp),
            )
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item { SheetRow(LumenIcons.Plus, "New album", null) { creatingHidden = false } }
                items(visibleAlbums, key = { it.id }) { album ->
                    AlbumRow(album) {
                        vm.addToAlbum(album, items)
                        onDone()
                    }
                }
                item {
                    HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Lumen.extras.hairline)
                    Eyebrow("Hidden folders", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
                if (!unlocked) {
                    item { SheetRow(LumenIcons.Lock, "Unlock to see hidden folders", "Fingerprint or screen lock", onClick = unlock) }
                } else {
                    item { SheetRow(LumenIcons.FolderLock, "New hidden folder", null) { creatingHidden = true } }
                    items(hiddenAlbums, key = { it.id }) { album ->
                        AlbumRow(album) {
                            vm.addToAlbum(album, items)
                            onDone()
                        }
                    }
                }
            }
        }
    }

    creatingHidden?.let { hidden ->
        AlbumNameDialog(
            title = if (hidden) "New hidden folder" else "New album",
            confirmLabel = "Create",
            initialHidden = hidden,
            showHiddenToggle = false,
            onDismiss = { creatingHidden = null },
            onConfirm = { name, _ ->
                creatingHidden = null
                vm.createAlbum(name, hidden, items)
                onDone()
            },
        )
    }
}

@Composable
private fun SheetRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Lumen.extras.hairline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AlbumRow(album: UserAlbumView, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumCoverImage(album, Modifier.size(52.dp), RoundedCornerShape(16.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(album.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                Format.plural(album.items.size, "item"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (album.hidden) {
            Icon(LumenIcons.Lock, contentDescription = "Hidden", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

/** Grid of an album's photos; tapping one makes it the cover. */
@Composable
fun CoverPickerSheet(
    items: List<MediaItem>,
    currentCoverId: Long?,
    onDismiss: () -> Unit,
    onPick: (MediaItem) -> Unit,
) {
    LumenSheet(onDismiss) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text("Choose a cover", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
            Text(
                "Tap any photo to put it on the front of the album",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 14.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 520.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val current = item.id == currentCoverId
                    MediaThumbnail(
                        item = item,
                        selected = current,
                        selectionMode = current,
                        corner = 12.dp,
                        modifier = Modifier.clickable { onPick(item) },
                    )
                }
            }
        }
    }
}

// Album visuals

@Composable
fun AlbumCoverImage(album: UserAlbumView, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val cover = album.cover
        if (cover != null) {
            AsyncImage(model = cover.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                if (album.hidden) LumenIcons.FolderLock else LumenIcons.Images,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** Album tile used for user albums in grids. */
@Composable
fun UserAlbumCard(album: UserAlbumView, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Box {
            AlbumCoverImage(album, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(22.dp))
            if (album.hidden && album.cover != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LumenIcons.Lock, contentDescription = "Hidden", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
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
            album.items.size.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
        )
    }
}

/** Dashed "new album" tile. */
@Composable
fun NewAlbumCard(label: String, icon: ImageVector, onClick: () -> Unit) {
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
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerHigh),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
        Text(
            "Pick photos & a cover",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
        )
    }
}

// Lock

/**
 * Shows [content] only while hidden folders are unlocked; otherwise a lock screen that
 * prompts for fingerprint or screen lock straight away.
 */
@Composable
fun LockedGate(vm: GalleryViewModel, content: @Composable () -> Unit) {
    val unlocked by vm.vaultUnlocked.collectAsStateWithLifecycle()
    val unlock = rememberVaultUnlocker { vm.unlockVault() }
    if (unlocked) {
        SecureContent()
        content()
    } else {
        LaunchedEffect(Unit) { unlock() }
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .border(1.dp, Lumen.extras.hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LumenIcons.Lock, contentDescription = null, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Hidden folders", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Unlock with your fingerprint, face or screen lock.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PillButton("Unlock", unlock)
        }
    }
}
