package com.lumen.gallery.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class MediaAccess { NONE, PARTIAL, FULL }

val mediaPermissions: Array<String>
    get() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

fun Context.mediaAccess(): MediaAccess {
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> when {
            granted(Manifest.permission.READ_MEDIA_IMAGES) || granted(Manifest.permission.READ_MEDIA_VIDEO) -> MediaAccess.FULL
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.PARTIAL
            else -> MediaAccess.NONE
        }
        granted(Manifest.permission.READ_EXTERNAL_STORAGE) -> MediaAccess.FULL
        else -> MediaAccess.NONE
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Media management access (Android 12+) skips the system confirmation for trash, delete and rename. */
val manageMediaSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun Context.canManageMedia(): Boolean =
    manageMediaSupported && MediaStore.canManageMedia(this)

fun Context.openManageMediaSettings() {
    if (!manageMediaSupported) return
    runCatching {
        startActivity(
            Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { openAppSettings() }
}

fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
