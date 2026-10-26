package com.lumen.gallery.util

import android.app.Activity
import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lumen.gallery.data.MediaItem

/** Holds the callback for the system consent dialog that is currently showing. */
class PendingAction {
    var action: (() -> Unit)? = null
}

/**
 * System-level operations on media: sharing, handing off to other apps, and the
 * MediaStore consent flows (trash, restore, delete, write) introduced in Android 11.
 */
@Stable
class MediaActions internal constructor(
    private val context: Context,
    private val launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    private val pending: PendingAction,
) {
    private val resolver get() = context.contentResolver

    fun share(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val intent = if (items.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = items[0].mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, items[0].uri)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = when {
                    items.all { it.isVideo } -> "video/*"
                    items.none { it.isVideo } -> "image/*"
                    else -> "*/*"
                }
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { it.uri }))
            }
        }
        intent.clipData = ClipData.newRawUri(null, items[0].uri).apply {
            items.drop(1).forEach { addItem(ClipData.Item(it.uri)) }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startChooser(intent, "Share")
    }

    fun moveToTrash(items: List<MediaItem>, onDone: () -> Unit = {}) {
        if (items.isEmpty()) return
        request(MediaStore.createTrashRequest(resolver, items.map { it.uri }, true), onDone)
    }

    fun restore(items: List<MediaItem>, onDone: () -> Unit = {}) {
        if (items.isEmpty()) return
        request(MediaStore.createTrashRequest(resolver, items.map { it.uri }, false), onDone)
    }

    fun deleteForever(items: List<MediaItem>, onDone: () -> Unit = {}) {
        if (items.isEmpty()) return
        request(MediaStore.createDeleteRequest(resolver, items.map { it.uri }), onDone)
    }

    fun requestWrite(items: List<MediaItem>, onGranted: () -> Unit) {
        if (items.isEmpty()) return
        request(MediaStore.createWriteRequest(resolver, items.map { it.uri }), onGranted)
    }

    fun edit(item: MediaItem) {
        // Read access only: we don't hold write access to other apps' media, and asking to pass it
        // on makes Android refuse to start the editor at all. Editors save their result as a copy.
        val intent = Intent(Intent.ACTION_EDIT)
            .setDataAndType(item.uri, item.mimeType.ifBlank { "image/*" })
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startChooser(intent, "Edit with")
    }

    fun setAs(item: MediaItem) {
        val intent = Intent(Intent.ACTION_ATTACH_DATA)
            .setDataAndType(item.uri, item.mimeType)
            .putExtra("mimeType", item.mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startChooser(intent, "Set as")
    }

    fun openWith(item: MediaItem) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(item.uri, item.mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startChooser(intent, "Open with")
    }

    private fun startChooser(intent: Intent, title: String) {
        runCatching { context.startActivity(Intent.createChooser(intent, title)) }
            .onFailure { Toast.makeText(context, "No app can handle this", Toast.LENGTH_SHORT).show() }
    }

    private fun request(intent: PendingIntent, onDone: () -> Unit) {
        pending.action = onDone
        launcher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
    }
}

@Composable
fun rememberMediaActions(): MediaActions {
    val context = LocalContext.current
    val pending = remember { PendingAction() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val action = pending.action
        pending.action = null
        if (result.resultCode == Activity.RESULT_OK) action?.invoke()
    }
    return remember(context, launcher) { MediaActions(context, launcher, pending) }
}
