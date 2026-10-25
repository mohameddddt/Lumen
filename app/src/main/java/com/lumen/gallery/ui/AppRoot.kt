package com.lumen.gallery.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lumen.gallery.ui.screens.CollectionScreen
import com.lumen.gallery.ui.screens.HomeScreen
import com.lumen.gallery.ui.screens.PickerScreen
import com.lumen.gallery.ui.screens.VaultScreen
import com.lumen.gallery.ui.screens.PermissionScreen
import com.lumen.gallery.ui.screens.SettingsScreen
import com.lumen.gallery.ui.screens.TrashScreen
import com.lumen.gallery.ui.screens.ViewerScreen
import com.lumen.gallery.util.MediaAccess
import com.lumen.gallery.util.findActivity
import com.lumen.gallery.util.mediaAccess
import com.lumen.gallery.util.mediaPermissions
import com.lumen.gallery.util.openAppSettings

@Composable
fun AppRoot(vm: GalleryViewModel) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(context.mediaAccess()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = context.mediaAccess()
        val activity = context.findActivity()
        permanentlyDenied = access == MediaAccess.NONE && activity != null &&
            !activity.shouldShowRequestPermissionRationale(mediaPermissions.first())
        vm.refresh()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = context.mediaAccess()
        if (now != access) {
            access = now
            vm.refresh()
        }
    }
    // Hidden folders lock again whenever Lumen leaves the screen.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.lockVault() }
    LaunchedEffect(access) {
        if (access != MediaAccess.NONE) vm.start()
    }

    if (access == MediaAccess.NONE) {
        PermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onRequest = { launcher.launch(mediaPermissions) },
            onOpenSettings = { context.openAppSettings() },
        )
    } else {
        GalleryNavHost(
            vm = vm,
            partialAccess = access == MediaAccess.PARTIAL,
            onManageAccess = { launcher.launch(mediaPermissions) },
        )
    }
}

private fun NavController.go(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}

@Composable
private fun GalleryNavHost(vm: GalleryViewModel, partialAccess: Boolean, onManageAccess: () -> Unit) {
    val nav = rememberNavController()
    val openViewer: (String, Long, Boolean) -> Unit = { source, id, slideshow ->
        nav.go("viewer/$source/$id?slideshow=$slideshow")
    }

    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.97f) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.97f) },
    ) {
        composable("home") {
            HomeScreen(
                vm = vm,
                partialAccess = partialAccess,
                onManageAccess = onManageAccess,
                onOpenViewer = { source, id -> openViewer(source, id, false) },
                onOpenCollection = { source -> nav.go("collection/$source") },
                onOpenTrash = { nav.go("trash") },
                onOpenVault = { nav.go("vault") },
                onOpenSettings = { nav.go("settings") },
            )
        }
        composable("vault") {
            VaultScreen(
                vm = vm,
                onBack = dropUnlessResumed { nav.popBackStack() },
                onOpenCollection = { source -> nav.go("collection/$source") },
            )
        }
        composable("picker/{albumId}") { entry ->
            PickerScreen(
                vm = vm,
                albumId = entry.arguments?.getString("albumId").orEmpty(),
                onBack = dropUnlessResumed { nav.popBackStack() },
            )
        }
        composable("collection/{source}") { entry ->
            CollectionScreen(
                vm = vm,
                source = entry.arguments?.getString("source").orEmpty(),
                onBack = dropUnlessResumed { nav.popBackStack() },
                onOpenViewer = openViewer,
                onAddPhotos = { albumId -> nav.go("picker/$albumId") },
            )
        }
        composable(
            route = "viewer/{source}/{id}?slideshow={slideshow}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("slideshow") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val args = entry.arguments
            ViewerScreen(
                vm = vm,
                source = args?.getString("source").orEmpty(),
                startId = args?.getLong("id") ?: 0L,
                startSlideshow = args?.getBoolean("slideshow") ?: false,
                onBack = dropUnlessResumed { nav.popBackStack() },
            )
        }
        composable("trash") {
            TrashScreen(vm = vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(vm = vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
    }
}
