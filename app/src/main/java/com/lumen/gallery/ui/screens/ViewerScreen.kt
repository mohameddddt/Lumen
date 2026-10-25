package com.lumen.gallery.ui.screens

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.lumen.gallery.data.ExifDetails
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.components.DarkGlass
import com.lumen.gallery.ui.components.MenuEntry
import com.lumen.gallery.ui.components.OverflowMenu
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.components.RoundIconButton
import com.lumen.gallery.ui.components.Scrubber
import com.lumen.gallery.ui.components.ZoomableImage
import com.lumen.gallery.ui.components.glass
import com.lumen.gallery.ui.components.glassSource
import com.lumen.gallery.ui.theme.FavoriteRed
import com.lumen.gallery.ui.theme.Lumen
import com.lumen.gallery.ui.theme.LumenIcons
import com.lumen.gallery.util.Format
import com.lumen.gallery.util.findActivity
import com.lumen.gallery.util.rememberMediaActions
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem as ExoMediaItem

private const val SLIDESHOW_INTERVAL_MS = 3500L
private val GlassBorder = Color.White.copy(alpha = 0.12f)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    vm: GalleryViewModel,
    source: String,
    startId: Long,
    startSlideshow: Boolean,
    onBack: () -> Unit,
) {
    if (remember(source) { vm.isHiddenSource(source) }) {
        LockedGate(vm) { ViewerContent(vm, source, startId, startSlideshow, onBack) }
    } else {
        ViewerContent(vm, source, startId, startSlideshow, onBack)
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerContent(
    vm: GalleryViewModel,
    source: String,
    startId: Long,
    startSlideshow: Boolean,
    onBack: () -> Unit,
) {
    val items by remember(source) { vm.itemsFor(source) }
        .collectAsStateWithLifecycle(remember(source) { vm.itemsNow(source) })
    val settings by vm.settings.collectAsStateWithLifecycle()

    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val context = LocalContext.current
    val actions = rememberMediaActions()
    val haze = rememberHazeState()
    val initialPage = remember { items.indexOfFirst { it.id == startId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }
    val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]
    val settled = items.getOrNull(pagerState.settledPage)

    var showUi by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(startSlideshow) }
    var showInfo by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<MediaItem?>(null) }
    var addTargets by remember { mutableStateOf<List<MediaItem>?>(null) }
    val userAlbumId = source.takeIf { it.startsWith("ualbum_") }?.removePrefix("ualbum_")
    val folderId = source.takeIf { it.startsWith("album_") }?.removePrefix("album_")?.toLongOrNull()

    // One player for the whole viewer, attached to whichever video page has settled. It is only
    // created once a video is reached, so opening a photo doesn't pay for building a player.
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { player?.release() } }
    val playback = rememberPlaybackState(player)
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player?.pause() }
    LaunchedEffect(settled?.id) {
        if (settled?.isVideo == true) {
            val active = player ?: ExoPlayer.Builder(context).build()
                .apply { repeatMode = Player.REPEAT_MODE_ONE }
                .also { player = it }
            playback.firstFrameRendered = false
            active.setMediaItem(ExoMediaItem.fromUri(settled.uri))
            active.prepare()
            active.playWhenReady = !slideshow
        } else {
            player?.stop()
            player?.clearMediaItems()
        }
    }

    LaunchedEffect(pagerState.currentPage) { zoomed = false }
    LaunchedEffect(slideshow) {
        if (!slideshow) return@LaunchedEffect
        showUi = false
        while (true) {
            delay(SLIDESHOW_INTERVAL_MS)
            val next = (pagerState.currentPage + 1) % pagerState.pageCount
            if (next == 0) pagerState.scrollToPage(0) else pagerState.animateScrollToPage(next, animationSpec = tween(650))
        }
    }

    SystemBarsEffect(visible = showUi)

    val toggleUi = {
        if (slideshow) {
            slideshow = false
            showUi = true
        } else {
            showUi = !showUi
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            key = { index -> items.getOrNull(index)?.id ?: index },
            beyondViewportPageCount = 1,
            pageSpacing = 16.dp,
            userScrollEnabled = !zoomed,
            modifier = Modifier
                .fillMaxSize()
                .glassSource(haze),
        ) { page ->
            val item = items.getOrNull(page) ?: return@HorizontalPager
            if (item.isVideo) {
                val attached = page == pagerState.settledPage
                VideoPage(
                    item = item,
                    player = if (attached) player else null,
                    showPoster = !attached || !playback.firstFrameRendered,
                    onTap = toggleUi,
                )
            } else {
                ZoomableImage(
                    item = item,
                    active = page == pagerState.currentPage,
                    onTap = toggleUi,
                    onZoomChange = { if (page == pagerState.currentPage) zoomed = it },
                )
            }
        }

        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ViewerTopBar(
                item = current,
                index = pagerState.currentPage,
                total = items.size,
                haze = haze,
                onBack = onBack,
                menu = buildList {
                    add(MenuEntry("Slideshow", LumenIcons.Slideshow) { slideshow = true })
                    add(MenuEntry("Add to album", LumenIcons.ImagePlus) { addTargets = listOf(current) })
                    if (userAlbumId != null || folderId != null) {
                        add(
                            MenuEntry("Use as album cover", LumenIcons.Image) {
                                if (userAlbumId != null) vm.setAlbumCover(userAlbumId, current.id) else vm.setFolderCover(folderId!!, current.id)
                            },
                        )
                    }
                    if (userAlbumId != null) {
                        add(MenuEntry("Remove from album", LumenIcons.ImageMinus) { vm.removeFromAlbum(userAlbumId, listOf(current)) })
                    }
                    add(MenuEntry("Rename", LumenIcons.Rename) { renameTarget = current })
                    if (!current.isVideo) add(MenuEntry("Set as wallpaper", LumenIcons.Wallpaper) { actions.setAs(current) })
                    add(MenuEntry("Open with", LumenIcons.External) { actions.openWith(current) })
                },
            )
        }

        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val activePlayer = player
                if (current.isVideo && settled?.id == current.id && activePlayer != null) {
                    VideoControls(playback, activePlayer, haze)
                }
                val favorite = current.id in settings.favorites
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glass(haze, DarkGlass, CircleShape, GlassBorder)
                        .height(62.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ViewerAction(LumenIcons.Share, "Share") { actions.share(listOf(current)) }
                    if (!current.isVideo) {
                        ViewerAction(LumenIcons.Pencil, "Edit") { actions.edit(current) }
                    }
                    ViewerAction(
                        icon = if (favorite) LumenIcons.HeartFilled else LumenIcons.Heart,
                        label = if (favorite) "Unfavorite" else "Favorite",
                        tint = if (favorite) FavoriteRed else Color.White,
                    ) { vm.toggleFavorites(listOf(current.id)) }
                    ViewerAction(LumenIcons.Info, "Details") { showInfo = true }
                    ViewerAction(LumenIcons.Trash, "Delete") { actions.moveToTrash(listOf(current)) }
                }
            }
        }
    }

    if (showInfo) {
        ModalBottomSheet(
            onDismissRequest = { showInfo = false },
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
        ) {
            DetailsSheet(current, vm)
        }
    }

    addTargets?.let { targets ->
        AddToAlbumSheet(vm = vm, items = targets, onDismiss = { addTargets = null }, onDone = { addTargets = null })
    }

    renameTarget?.let { target ->
        RenameDialog(
            item = target,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                renameTarget = null
                actions.requestWrite(listOf(target)) { vm.rename(target, newName) }
            },
        )
    }
}

@Composable
private fun SystemBarsEffect(visible: Boolean) {
    val view = LocalView.current
    val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val controller = remember(view) {
        view.context.findActivity()?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(controller) {
        controller?.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.apply {
                show(WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }
    LaunchedEffect(controller, visible) {
        if (visible) controller?.show(WindowInsetsCompat.Type.systemBars())
        else controller?.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun ViewerTopBar(
    item: MediaItem,
    index: Int,
    total: Int,
    haze: HazeState,
    onBack: () -> Unit,
    menu: List<MenuEntry>,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(LumenIcons.ArrowLeft, "Back", onBack, haze = haze, style = DarkGlass, tint = Color.White)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                Format.dayMonth(item.dateTaken),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${Format.year(item.dateTaken)} · ${Format.time(item.dateTaken)} · ${index + 1}/$total",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        OverflowMenu(menu, haze = haze, style = DarkGlass, tint = Color.White)
    }
}

@Composable
private fun ViewerAction(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    RoundIconButton(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        size = 48.dp,
        iconSize = 22.dp,
        container = Color.Transparent,
        tint = tint,
    )
}

// Video

@Stable
private class PlaybackState {
    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var muted by mutableStateOf(false)
    var firstFrameRendered by mutableStateOf(false)
}

@Composable
private fun rememberPlaybackState(player: ExoPlayer?): PlaybackState {
    val state = remember { PlaybackState() }
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                state.duration = player.duration.coerceAtLeast(0L)
                state.position = player.currentPosition
            }

            override fun onRenderedFirstFrame() {
                state.firstFrameRendered = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(state.isPlaying, player) {
        if (player == null) return@LaunchedEffect
        while (state.isPlaying) {
            state.position = player.currentPosition
            state.duration = player.duration.coerceAtLeast(0L)
            delay(200)
        }
    }
    return state
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPage(item: MediaItem, player: Player?, showPoster: Boolean, onTap: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        setKeepContentOnPlayerReset(false)
                    }
                },
                update = { it.player = player },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (showPoster) {
            AsyncImage(
                model = item.thumb,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (player == null) {
            Box(
                Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LumenIcons.PlayFilled, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        )
    }
}

@Composable
private fun VideoControls(state: PlaybackState, player: Player, haze: HazeState) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    val duration = state.duration
    val fraction = if (duration > 0) (state.position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownPosition = scrub?.let { (it * duration).toLong() } ?: state.position

    Row(
        Modifier
            .fillMaxWidth()
            .glass(haze, DarkGlass, CircleShape, GlassBorder)
            .height(56.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = if (state.isPlaying) LumenIcons.PauseFilled else LumenIcons.PlayFilled,
            contentDescription = if (state.isPlaying) "Pause" else "Play",
            onClick = {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                    player.play()
                }
            },
            container = Color.White,
            tint = Color.Black,
            size = 42.dp,
            iconSize = 18.dp,
        )
        Text(
            Format.duration(shownPosition),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(start = 12.dp),
        )
        Scrubber(
            fraction = scrub ?: fraction,
            onSeek = { scrub = it },
            onSeekEnd = {
                scrub?.let { player.seekTo((it * duration).toLong()) }
                scrub = null
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Text(Format.duration(duration), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        RoundIconButton(
            icon = if (state.muted) LumenIcons.Mute else LumenIcons.Volume,
            contentDescription = if (state.muted) "Unmute" else "Mute",
            onClick = {
                state.muted = !state.muted
                player.volume = if (state.muted) 0f else 1f
            },
            container = Color.Transparent,
            tint = Color.White,
            size = 42.dp,
        )
    }
}

// Details & rename

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsSheet(item: MediaItem, vm: GalleryViewModel) {
    val exif by produceState<ExifDetails?>(initialValue = null, item.id) { value = vm.loadExif(item) }
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 30.dp),
    ) {
        Text(Format.fullDate(item.dateTaken), style = MaterialTheme.typography.headlineMedium)
        Text(
            Format.time(item.dateTaken),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 18.dp),
        )
        DetailRow(
            icon = if (item.isVideo) LumenIcons.Film else LumenIcons.Image,
            title = item.name,
            subtitle = listOfNotNull(
                Format.resolution(item.width, item.height),
                if (!item.isVideo) Format.megapixels(item.width, item.height) else null,
                if (item.isVideo && item.durationMs > 0) Format.duration(item.durationMs) else null,
                Format.size(item.size),
            ).joinToString(" · "),
        )
        exif?.let { details ->
            if (details.camera != null || details.settings.isNotEmpty()) {
                DetailRow(LumenIcons.Aperture, details.camera ?: "Camera", null)
                if (details.settings.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 40.dp, bottom = 8.dp),
                    ) {
                        details.settings.forEach { value ->
                            Text(
                                value,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceContainerHigh)
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        }
        DetailRow(LumenIcons.Folder, item.bucketName.ifBlank { "Internal storage" }, "${item.relativePath}${item.name}")
    }
}

@Composable
private fun DetailRow(icon: ImageVector, title: String, subtitle: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RenameDialog(item: MediaItem, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val extension = item.name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
    var text by remember { mutableStateOf(item.name.removeSuffix(extension)) }
    val valid = text.isNotBlank() && text.none { it in "/\\:*?\"<>|" }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        title = { Text("Rename", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceContainerHigh)
                    .border(1.dp, if (valid) Color.Transparent else colors.error, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.weight(1f),
                )
                Text(extension, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        },
        confirmButton = {
            PillButton("Rename", { onConfirm(text.trim() + extension) }, enabled = valid, height = 44.dp)
        },
        dismissButton = {
            PillButton("Cancel", onDismiss, filled = false, height = 44.dp)
        },
    )
}
