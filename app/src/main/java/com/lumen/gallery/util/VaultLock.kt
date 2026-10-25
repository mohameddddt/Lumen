package com.lumen.gallery.util

import android.view.WindowManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/**
 * Asks for fingerprint, face or the screen lock. Phones without any screen lock can't be
 * protected, so they are let through with a reminder to set one.
 */
fun FragmentActivity.authenticateForVault(onSuccess: () -> Unit) {
    val status = BiometricManager.from(this).canAuthenticate(AUTHENTICATORS)
    if (status != BiometricManager.BIOMETRIC_SUCCESS) {
        Toast.makeText(this, "Set a screen lock to protect hidden folders", Toast.LENGTH_LONG).show()
        onSuccess()
        return
    }
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock hidden folders")
            .setSubtitle("Confirm it's you to see your hidden photos")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build(),
    )
}

/** Returns a function that shows the unlock prompt and calls [onUnlocked] when it succeeds. */
@Composable
fun rememberVaultUnlocker(onUnlocked: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onUnlocked)
    return remember(context) {
        {
            val activity = context.findActivity() as? FragmentActivity
            if (activity == null) callback() else activity.authenticateForVault { callback() }
        }
    }
}

private object SecureWindow {
    var holders = 0
}

/** Blocks screenshots and hides the recents preview while hidden photos are on screen. */
@Composable
fun SecureContent() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        SecureWindow.holders++
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            SecureWindow.holders--
            if (SecureWindow.holders <= 0) {
                SecureWindow.holders = 0
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
