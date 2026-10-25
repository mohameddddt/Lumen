package com.lumen.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

data class MenuEntry(
    val label: String,
    val icon: ImageVector? = null,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

data class BarAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

// Glass

@Composable
fun glassStyle(): HazeStyle {
    val extras = Lumen.extras
    return HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tints = listOf(HazeTint(extras.glassTint)),
        blurRadius = 20.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(extras.glassFallback),
    )
}

val DarkGlass = HazeStyle(
    backgroundColor = Color.Black,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.38f))),
    blurRadius = 20.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(Color.Black.copy(alpha = 0.6f)),
)

/** Marks content that floating glass bars blur. */
fun Modifier.glassSource(state: HazeState?): Modifier = if (state == null) this else hazeSource(state)

/** Frosted glass background: blurs whatever [state]'s source draws behind this element. */
fun Modifier.glass(state: HazeState?, style: HazeStyle, shape: Shape, border: Color): Modifier =
    this
        .clip(shape)
        .then(
            if (state != null) {
                Modifier.hazeEffect(state, style) { inputScale = HazeInputScale.Auto }
            } else {
                Modifier.background(style.fallbackTint.color)
            },
        )
        .border(1.dp, border, shape)

/** Space to leave under scrolling content so the floating dock never covers it. */
@Composable
fun dockClearance(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp

/** Space to leave above content for a floating top bar. */
@Composable
fun topBarClearance(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

// Typography helpers

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 4),
        color = color,
        modifier = modifier,
    )
}

/** Large editorial page title, used as the first item of a scrolling screen. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    topInset: Boolean = true,
    startPadding: Dp = 14.dp,
    endPadding: Dp = 10.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (topInset) Modifier.statusBarsPadding() else Modifier)
            .padding(start = startPadding, end = endPadding, top = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Eyebrow(eyebrow, Modifier.padding(bottom = 6.dp))
            }
            Text(title, style = MaterialTheme.typography.displayMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** A soft fade under the status bar so content scrolling beneath it stays legible. */
@Composable
fun TopScrim(modifier: Modifier = Modifier, extra: Dp = 20.dp) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + extra)
            .background(Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.85f), bg.copy(alpha = 0f)))),
    )
}

/** Floating back button + actions for detail screens, with a title that fades in once scrolled. */
@Composable
fun FloatingTopBar(
    onBack: () -> Unit,
    haze: HazeState?,
    modifier: Modifier = Modifier,
    title: String? = null,
    showTitle: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(LumenIcons.ArrowLeft, "Back", onBack, haze = haze)
        val titleAlpha by animateFloatAsState(if (showTitle && title != null) 1f else 0f, label = "barTitle")
        Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            if (title != null && titleAlpha > 0f) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .graphicsLayer { alpha = titleAlpha }
                        .glass(haze, glassStyle(), CircleShape, Lumen.extras.glassBorder)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

// Buttons

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    haze: HazeState? = null,
    style: HazeStyle? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val background = if (haze != null || style != null) {
        Modifier.glass(haze, style ?: glassStyle(), CircleShape, Lumen.extras.glassBorder)
    } else {
        Modifier.clip(CircleShape).background(container)
    }
    Box(
        modifier
            .size(size)
            .then(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    height: Dp = 54.dp,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .then(
                if (filled) Modifier.background(if (enabled) colors.inverseSurface else colors.surfaceContainerHigh)
                else Modifier.border(1.dp, Lumen.extras.hairline, CircleShape),
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = when {
                !enabled -> colors.onSurfaceVariant
                filled -> colors.inverseOnSurface
                else -> colors.onSurface
            },
        )
    }
}

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) colors.inverseSurface else Color.Transparent, label = "chipBg")
    val fg by animateColorAsState(if (selected) colors.inverseOnSurface else colors.onSurface, label = "chipFg")
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else Lumen.extras.hairline, CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

// Controls

/** Purely visual; the row that contains it handles the toggle. */
@Composable
fun LumenSwitch(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val track by animateColorAsState(
        when {
            !enabled -> colors.surfaceContainerHigh
            checked -> colors.primary
            else -> colors.surfaceContainerHighest
        },
        label = "switchTrack",
    )
    val offset by animateDpAsState(
        if (checked) 20.dp else 0.dp,
        spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "switchThumb",
    )
    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(track)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(22.dp)
                .shadow(2.dp, CircleShape)
                .background(if (enabled) Color.White else colors.surfaceContainerHighest, CircleShape),
        )
    }
}

@Composable
fun <T> SegmentedPills(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val x by animateDpAsState(segment * index, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "segment")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .shadow(2.dp, CircleShape)
                .background(Lumen.extras.raised, CircleShape),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { option ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (option == selected) colors.onSurface else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A slim seek bar: a hairline track with a small round thumb. */
@Composable
fun Scrubber(
    fraction: Float,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    val seek by rememberUpdatedState(onSeek)
    val end by rememberUpdatedState(onSeekEnd)
    var dragging by remember { mutableStateOf(false) }
    val thumb by animateDpAsState(if (dragging) 16.dp else 12.dp, label = "thumb")
    BoxWithConstraints(
        modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    seek((position.x / size.width).coerceIn(0f, 1f))
                    end()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        end()
                    },
                    onDragCancel = {
                        dragging = false
                        end()
                    },
                ) { change, _ ->
                    change.consume()
                    seek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val f = fraction.coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(color.copy(alpha = 0.25f)))
        Box(Modifier.fillMaxWidth(f).height(3.dp).clip(CircleShape).background(color))
        Box(
            Modifier
                .offset(x = (maxWidth - thumb) * f)
                .size(thumb)
                .background(color, CircleShape),
        )
    }
}

// Menus

@Composable
fun MenuPopup(expanded: Boolean, onDismiss: () -> Unit, entries: List<MenuEntry>) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 16.dp,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Lumen.extras.hairline),
    ) {
        entries.forEach { entry ->
            DropdownMenuItem(
                text = { Text(entry.label, style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = entry.icon?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) } },
                trailingIcon = if (entry.selected) {
                    { Icon(LumenIcons.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                } else null,
                onClick = {
                    onDismiss()
                    entry.onClick()
                },
                contentPadding = PaddingValues(horizontal = 18.dp),
            )
        }
    }
}

@Composable
fun OverflowMenu(
    entries: List<MenuEntry>,
    haze: HazeState? = null,
    style: HazeStyle? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(LumenIcons.More, "More options", { open = true }, haze = haze, style = style, tint = tint)
        MenuPopup(expanded = open, onDismiss = { open = false }, entries = entries)
    }
}

// Docks

data class DockItem(val label: String, val icon: ImageVector)

/** The floating glass navigation capsule. The selected tab expands to show its label. */
@Composable
fun FloatingDock(
    items: List<DockItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    haze: HazeState?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .shadow(24.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .glass(haze, glassStyle(), CircleShape, Lumen.extras.glassBorder)
            .height(64.dp)
            .padding(6.dp)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(if (selected) colors.inverseSurface else Color.Transparent, label = "dockBg")
            val fg by animateColorAsState(if (selected) colors.inverseOnSurface else colors.onSurface, label = "dockFg")
            Row(
                Modifier
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .semantics { contentDescription = item.label }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = fg, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Replaces the dock while items are selected. */
@Composable
fun SelectionDock(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    actions: List<BarAction>,
    haze: HazeState?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            .fillMaxWidth()
            .shadow(24.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .glass(haze, glassStyle(), CircleShape, Lumen.extras.glassBorder)
            .height(64.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(LumenIcons.Close, "Clear selection", onClose, container = Color.Transparent)
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onSelectAll)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text("$count selected", style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text("Select all", style = MaterialTheme.typography.labelSmall, color = colors.primary)
        }
        actions.forEach { action ->
            RoundIconButton(
                icon = action.icon,
                contentDescription = action.label,
                onClick = action.onClick,
                container = Color.Transparent,
                tint = action.tint ?: colors.onSurface,
            )
        }
    }
}

// States

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .border(1.dp, Lumen.extras.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun Loader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onSurface,
            trackColor = Lumen.extras.hairline,
            strokeWidth = 2.dp,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(text, Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
