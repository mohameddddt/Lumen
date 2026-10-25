package com.lumen.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumen.gallery.ui.components.PillButton
import com.lumen.gallery.ui.theme.LumenIcons

@Composable
fun PermissionScreen(permanentlyDenied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(colors.primary, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text("Lumen", style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic))
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Collage()
        }

        Text(
            buildAnnotatedString {
                append("Every moment,\n")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = colors.primary)) { append("beautifully") }
                append(" kept.")
            },
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            if (permanentlyDenied) {
                "Photo access is turned off. Enable it in Settings so Lumen can show your library."
            } else {
                "A quiet, fast home for your photos and videos. Lumen works entirely on your device — nothing is uploaded, ever."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        PillButton(
            text = if (permanentlyDenied) "Open settings" else "Allow access to photos",
            onClick = if (permanentlyDenied) onOpenSettings else onRequest,
            modifier = Modifier.fillMaxWidth(),
            height = 58.dp,
        )
    }
}

@Composable
private fun Collage() {
    Box(Modifier.size(width = 300.dp, height = 260.dp), contentAlignment = Alignment.Center) {
        Polaroid(
            brush = Brush.linearGradient(listOf(Color(0xFFF6C9A8), Color(0xFFE5532D))),
            icon = LumenIcons.Sun,
            angle = -11f,
            x = (-74).dp,
            y = 18.dp,
        )
        Polaroid(
            brush = Brush.linearGradient(listOf(Color(0xFF9DB7C4), Color(0xFF2F4858))),
            icon = LumenIcons.Film,
            angle = 9f,
            x = 78.dp,
            y = 26.dp,
        )
        Polaroid(
            brush = Brush.linearGradient(listOf(Color(0xFFE9DCC4), Color(0xFF8A6A3F))),
            icon = LumenIcons.Camera,
            angle = -2f,
            x = 0.dp,
            y = (-10).dp,
        )
    }
}

@Composable
private fun Polaroid(brush: Brush, icon: ImageVector, angle: Float, x: Dp, y: Dp) {
    Box(
        Modifier
            .offset(x, y)
            .rotate(angle)
            .shadow(18.dp, RoundedCornerShape(10.dp))
            .background(Color(0xFFFFFDF8), RoundedCornerShape(10.dp))
            .padding(start = 9.dp, end = 9.dp, top = 9.dp, bottom = 30.dp),
    ) {
        Box(
            Modifier
                .size(width = 122.dp, height = 132.dp)
                .background(brush, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}
