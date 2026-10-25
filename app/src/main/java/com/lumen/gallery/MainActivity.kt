package com.lumen.gallery

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumen.gallery.ui.AppRoot
import com.lumen.gallery.ui.GalleryViewModel
import com.lumen.gallery.ui.theme.LumenTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GalleryViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            LumenTheme(settings) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(vm)
                }
            }
        }
    }
}
