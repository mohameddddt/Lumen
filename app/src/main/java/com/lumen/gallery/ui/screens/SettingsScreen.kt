package com.lumen.gallery.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import com.lumen.gallery.data.FontPairing
import com.lumen.gallery.data.Palette
import com.lumen.gallery.data.Settings
import com.lumen.gallery.ui.theme.displayFamilyFor
import com.lumen.gallery.ui.theme.displayWeightFor
import com.lumen.gallery.ui.theme.schemeFor
import com.lumen.gallery.data.ThemeMode
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.components.FloatingTopBar
import com.lumen.gallery.ui.components.LumenSwitch
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.components.ScreenHeader
import com.lumen.gallery.ui.components.SectionLabel
import com.lumen.gallery.ui.components.SegmentedPills
import com.lumen.gallery.ui.components.TopScrim
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.components.topBarClearance
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import com.lumen.gallery.util.canManageMedia
import com.lumen.gallery.util.manageMediaSupported
import com.lumen.gallery.util.openManageMediaSettings
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun SettingsScreen(vm: GalleryViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val all by vm.sorted.collectAsStateWithLifecycle()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hiddenAlbums = albums.filter { it.id in settings.hiddenAlbums }
    val listState = rememberLazyListState()
    val haze = rememberHazeState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val context = LocalContext.current
    // Granted or revoked on a system screen, so read it again whenever we come back.
    var canManageMedia by remember { mutableStateOf(context.canManageMedia()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canManageMedia = context.canManageMedia() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topBarClearance(), bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .glassSource(haze)
                .navigationBarsPadding(),
        ) {
            item { ScreenHeader("Settings", topInset = false, startPadding = 4.dp) }

            item { SectionLabel("Appearance", Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)) }
            item {
                Card {
                    Column(Modifier.padding(18.dp)) {
                        RowTitle(LumenIcons.Contrast, "Theme")
                        Spacer(Modifier.height(14.dp))
                        SegmentedPills(
                            options = ThemeMode.entries,
                            selected = settings.theme,
                            label = { it.label },
                            onSelect = vm::setTheme,
                        )
                    }
                    Divider()
                    Column(Modifier.padding(vertical = 18.dp)) {
                        RowTitle(LumenIcons.Palette, "Palette", settings.palette.label, Modifier.padding(horizontal = 18.dp))
                        Spacer(Modifier.height(14.dp))
                        PalettePicker(
                            selected = settings.palette.takeUnless { settings.dynamicColor && dynamicSupported },
                            dark = Lumen.extras.isDark,
                            onSelect = vm::setPalette,
                        )
                    }
                    Divider()
                    Column(Modifier.padding(vertical = 18.dp)) {
                        RowTitle(LumenIcons.Type, "Typeface", settings.fonts.description, Modifier.padding(horizontal = 18.dp))
                        Spacer(Modifier.height(14.dp))
                        FontPicker(selected = settings.fonts, onSelect = vm::setFonts)
                    }
                    Divider()
                    ToggleRow(
                        icon = LumenIcons.Sparkles,
                        title = "Wallpaper colors",
                        subtitle = if (dynamicSupported) "Tint Lumen with your wallpaper's palette" else "Needs Android 12 or newer",
                        checked = settings.dynamicColor && dynamicSupported,
                        enabled = dynamicSupported,
                        onChange = vm::setDynamicColor,
                    )
                    Divider()
                    ToggleRow(
                        icon = LumenIcons.Moon,
                        title = "Pure black",
                        subtitle = "True black backgrounds in dark theme",
                        checked = settings.pureBlack,
                        onChange = vm::setPureBlack,
                    )
                }
            }

            item { SectionLabel("Library", Modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp)) }
            item {
                Card {
                    Column(Modifier.padding(18.dp)) {
                        RowTitle(LumenIcons.Grid, "Grid density", "Pinch the grid to change it anytime")
                        Spacer(Modifier.height(14.dp))
                        ColumnPicker(settings.gridColumns, vm::setGridColumns)
                    }
                    Divider()
                    ToggleRow(
                        icon = LumenIcons.Sparkles,
                        title = "On this day",
                        subtitle = "Resurface memories from past years",
                        checked = settings.showMemories,
                        onChange = vm::setShowMemories,
                    )
                    Divider()
                    ToggleRow(
                        icon = LumenIcons.Trash,
                        title = "Delete without asking",
                        subtitle = if (manageMediaSupported) "Skip Android's confirmation when deleting or renaming" else "Needs Android 12 or newer",
                        checked = canManageMedia,
                        enabled = manageMediaSupported,
                        onChange = { context.openManageMediaSettings() },
                    )
                }
            }

            if (hiddenAlbums.isNotEmpty()) {
                item { SectionLabel("Hidden from timeline", Modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp)) }
                item {
                    Card {
                        hiddenAlbums.forEachIndexed { index, album ->
                            if (index > 0) Divider()
                            Row(
                                Modifier.padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(LumenIcons.EyeOff, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(album.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                PillButton("Show", { vm.setAlbumHidden(album.id, false) }, filled = false, height = 36.dp)
                            }
                        }
                    }
                }
            }

            item { SectionLabel("About", Modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp)) }
            item {
                Card {
                    Column(Modifier.padding(22.dp)) {
                        Text(
                            "Lumen",
                            style = MaterialTheme.typography.displaySmall.copy(fontStyle = FontStyle.Italic),
                        )
                        Text(
                            "Version 1.0 · Private by design. Your library never leaves this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${Format.counts(all)} · ${Format.plural(albums.size, "album")}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        TopScrim(Modifier.align(Alignment.TopCenter), extra = 72.dp)
        FloatingTopBar(
            onBack = onBack,
            haze = haze,
            title = "Settings",
            showTitle = scrolled,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, Lumen.extras.hairline, shape),
        content = content,
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Lumen.extras.hairline)
}

@Composable
private fun RowTitle(icon: ImageVector, title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
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
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        LumenSwitch(checked = checked, enabled = enabled)
    }
}

@Composable
private fun ColumnPicker(columns: Int, onChange: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (Settings.MIN_COLUMNS..Settings.MAX_COLUMNS).forEach { count ->
            val selected = count == columns
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) colors.inverseSurface else colors.surfaceContainerHigh)
                    .clickable(role = Role.RadioButton) { onChange(count) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (selected) colors.inverseOnSurface else colors.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(count) {
                            Box(
                                Modifier
                                    .size(3.dp)
                                    .background(
                                        (if (selected) colors.inverseOnSurface else colors.onSurfaceVariant).copy(alpha = 0.7f),
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettePicker(selected: Palette?, dark: Boolean, onSelect: (Palette) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(Palette.entries) { palette ->
            val scheme = remember(palette, dark) { schemeFor(palette, dark) }
            val isSelected = palette == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.RadioButton) { onSelect(palette) }
                    .padding(4.dp),
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Lumen.extras.hairline,
                            shape = CircleShape,
                        )
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(scheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    // Paper background with the accent as a dot and a sliver of the raised surface.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(scheme.surfaceContainerHigh),
                    )
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(scheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(LumenIcons.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(13.dp))
                        }
                    }
                }
                Text(
                    palette.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun FontPicker(selected: FontPairing, onSelect: (FontPairing) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(FontPairing.entries) { fonts ->
            val isSelected = fonts == selected
            Column(
                Modifier
                    .width(104.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) colors.inverseSurface else colors.surfaceContainerHigh)
                    .clickable(role = Role.RadioButton) { onSelect(fonts) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    "Aa",
                    fontFamily = displayFamilyFor(fonts),
                    fontWeight = displayWeightFor(fonts),
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    color = if (isSelected) colors.inverseOnSurface else colors.onSurface,
                )
                Text(
                    fonts.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) colors.inverseOnSurface else colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

