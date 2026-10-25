package com.lumen.gallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.lumen.gallery.R
import com.lumen.gallery.data.FontPairing
import com.lumen.gallery.data.Palette
import com.lumen.gallery.data.Settings
import com.lumen.gallery.data.ThemeMode
import com.lumen.gallery.util.findActivity

/** Hearts use their own warm red so favorites read the same in every theme. */
val FavoriteRed = Color(0xFFFF4D5E)

// Type

private val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

@OptIn(ExperimentalTextApi::class)
private fun variable(normal: Int, italic: Int? = null): FontFamily = FontFamily(
    listOf(400, 500, 600, 700, 800).flatMap { weight ->
        val settings = FontVariation.Settings(FontVariation.weight(weight))
        listOfNotNull(
            Font(normal, weight = FontWeight(weight), variationSettings = settings),
            italic?.let { Font(it, weight = FontWeight(weight), style = FontStyle.Italic, variationSettings = settings) },
        )
    },
)

private val Manrope = variable(R.font.manrope)
private val Playfair = variable(R.font.playfair_display, R.font.playfair_display_italic)
private val SpaceGrotesk = variable(R.font.space_grotesk)
private val Nunito = variable(R.font.nunito)

/** How a font pairing is set: display face and weight, body face, and size scale for headings. */
private data class TypeSpec(
    val display: FontFamily,
    val displayWeight: FontWeight,
    val displayScale: Float,
    val displayTracking: Float,
    val body: FontFamily,
)

private fun specFor(fonts: FontPairing) = when (fonts) {
    FontPairing.EDITORIAL -> TypeSpec(InstrumentSerif, FontWeight.Normal, 1f, -0.4f, Manrope)
    FontPairing.CLASSIC -> TypeSpec(Playfair, FontWeight.Medium, 0.82f, -0.6f, Manrope)
    FontPairing.MODERN -> TypeSpec(SpaceGrotesk, FontWeight.Medium, 0.8f, -1.4f, SpaceGrotesk)
    FontPairing.ROUNDED -> TypeSpec(Nunito, FontWeight.ExtraBold, 0.78f, -0.6f, Nunito)
    FontPairing.SYSTEM -> TypeSpec(FontFamily.Default, FontWeight.SemiBold, 0.78f, -0.8f, FontFamily.Default)
}

/** The display face of a pairing, used for previews in Settings. */
fun displayFamilyFor(fonts: FontPairing): FontFamily = specFor(fonts).display
fun displayWeightFor(fonts: FontPairing): FontWeight = specFor(fonts).displayWeight

fun typographyFor(fonts: FontPairing): Typography {
    val spec = specFor(fonts)
    fun display(size: Int, line: Int) = TextStyle(
        fontFamily = spec.display,
        fontWeight = spec.displayWeight,
        fontSize = (size * spec.displayScale).sp,
        lineHeight = (line * spec.displayScale).sp,
        letterSpacing = spec.displayTracking.sp * (size / 44f),
    )
    fun body(size: Double, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
        fontFamily = spec.body,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )
    return Typography(
        displayLarge = display(52, 56),
        displayMedium = display(44, 50),
        displaySmall = display(34, 40),
        headlineLarge = display(30, 36),
        headlineMedium = display(26, 32),
        headlineSmall = display(23, 28),
        titleLarge = body(19.0, 26, FontWeight.Bold, -0.2),
        titleMedium = body(16.0, 22, FontWeight.SemiBold, -0.1),
        titleSmall = body(14.0, 20, FontWeight.SemiBold),
        bodyLarge = body(16.0, 24, FontWeight.Medium),
        bodyMedium = body(14.0, 20, FontWeight.Medium),
        bodySmall = body(12.5, 18, FontWeight.Medium),
        labelLarge = body(14.0, 18, FontWeight.SemiBold),
        labelMedium = body(12.0, 16, FontWeight.SemiBold, 0.2),
        labelSmall = body(11.0, 14, FontWeight.Bold, 0.4),
    )
}

private val LumenShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

// Color: neutrals are generated from the palette's hue so every palette gets its own "paper".

private fun hsl(hue: Float, saturation: Float, lightness: Float): Color =
    Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))))

private fun onColor(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF141210) else Color.White

fun schemeFor(palette: Palette, dark: Boolean): ColorScheme {
    val h = palette.hue
    val s = palette.saturation
    return if (!dark) {
        val accent = Color(palette.accentLight)
        val background = hsl(h, s, 0.936f)
        val ink = hsl(h, s * 0.4f, 0.095f)
        lightColorScheme(
            primary = accent,
            onPrimary = onColor(accent),
            primaryContainer = lerp(background, accent, 0.18f),
            onPrimaryContainer = lerp(ink, accent, 0.35f),
            secondary = hsl(h, s * 0.3f, 0.4f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h, s * 0.8f, 0.89f),
            onSecondaryContainer = ink,
            tertiary = hsl(h, s * 0.6f, 0.38f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(h, s, 0.86f),
            onTertiaryContainer = ink,
            background = background,
            onBackground = ink,
            surface = background,
            onSurface = ink,
            surfaceVariant = hsl(h, s * 0.8f, 0.89f),
            onSurfaceVariant = hsl(h, s * 0.35f, 0.44f),
            surfaceTint = Color.Transparent,
            inverseSurface = ink,
            inverseOnSurface = background,
            inversePrimary = Color(palette.accentDark),
            outline = hsl(h, s * 0.5f, 0.66f),
            outlineVariant = hsl(h, s * 0.7f, 0.84f),
            error = Color(0xFFC8341F),
            onError = Color.White,
            scrim = Color.Black,
            surfaceBright = hsl(h, s, 0.985f),
            surfaceDim = hsl(h, s, 0.88f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hsl(h, s, 0.972f),
            surfaceContainer = hsl(h, s * 0.9f, 0.918f),
            surfaceContainerHigh = hsl(h, s * 0.85f, 0.889f),
            surfaceContainerHighest = hsl(h, s * 0.8f, 0.857f),
        )
    } else {
        val accent = Color(palette.accentDark)
        val background = hsl(h, s * 0.45f, 0.055f)
        val paper = hsl(h, s, 0.93f)
        darkColorScheme(
            primary = accent,
            onPrimary = onColor(accent),
            primaryContainer = lerp(background, accent, 0.25f),
            onPrimaryContainer = lerp(paper, accent, 0.3f),
            secondary = hsl(h, s * 0.4f, 0.7f),
            onSecondary = background,
            secondaryContainer = hsl(h, s * 0.4f, 0.155f),
            onSecondaryContainer = paper,
            tertiary = hsl(h, s, 0.72f),
            onTertiary = background,
            tertiaryContainer = hsl(h, s * 0.5f, 0.22f),
            onTertiaryContainer = paper,
            background = background,
            onBackground = paper,
            surface = background,
            onSurface = paper,
            surfaceVariant = hsl(h, s * 0.4f, 0.155f),
            onSurfaceVariant = hsl(h, s * 0.3f, 0.55f),
            surfaceTint = Color.Transparent,
            inverseSurface = paper,
            inverseOnSurface = background,
            inversePrimary = Color(palette.accentLight),
            outline = hsl(h, s * 0.3f, 0.34f),
            outlineVariant = hsl(h, s * 0.4f, 0.165f),
            error = Color(0xFFFF7A66),
            onError = Color(0xFF3A0700),
            scrim = Color.Black,
            surfaceBright = hsl(h, s * 0.4f, 0.17f),
            surfaceDim = background,
            surfaceContainerLowest = hsl(h, s * 0.45f, 0.035f),
            surfaceContainerLow = hsl(h, s * 0.4f, 0.087f),
            surfaceContainer = hsl(h, s * 0.4f, 0.107f),
            surfaceContainerHigh = hsl(h, s * 0.4f, 0.135f),
            surfaceContainerHighest = hsl(h, s * 0.4f, 0.165f),
        )
    }
}

/** Tokens Material's color scheme has no slot for. */
@Immutable
data class LumenExtras(
    val isDark: Boolean,
    /** Thin dividers and outlines. */
    val hairline: Color,
    /** The raised thumb of segmented controls. */
    val raised: Color,
    /** Tint laid over blurred content for floating "glass" bars. */
    val glassTint: Color,
    /** Used instead of blur on devices that can't blur. */
    val glassFallback: Color,
    val glassBorder: Color,
)

private fun extrasFor(scheme: ColorScheme, dark: Boolean) = LumenExtras(
    isDark = dark,
    hairline = scheme.outlineVariant,
    raised = if (dark) scheme.surfaceContainerHighest.copy(alpha = 1f).let { lerp(it, Color.White, 0.08f) } else Color.White,
    glassTint = scheme.surfaceContainerLow.copy(alpha = if (dark) 0.62f else 0.68f),
    glassFallback = scheme.surfaceContainerLow.copy(alpha = 0.96f),
    glassBorder = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
)

private val LocalLumenExtras = staticCompositionLocalOf { extrasFor(schemeFor(Palette.CORAL, false), dark = false) }

object Lumen {
    val extras: LumenExtras
        @Composable @ReadOnlyComposable get() = LocalLumenExtras.current
}

@Composable
fun LumenTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    var scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> remember(settings.palette, dark) { schemeFor(settings.palette, dark) }
    }
    if (dark && settings.pureBlack) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = lerp(Color.Black, scheme.surfaceContainerLow, 0.7f),
            surfaceContainer = lerp(Color.Black, scheme.surfaceContainer, 0.75f),
            surfaceContainerHigh = lerp(Color.Black, scheme.surfaceContainerHigh, 0.8f),
            surfaceContainerHighest = lerp(Color.Black, scheme.surfaceContainerHighest, 0.85f),
        )
    }
    val typography = remember(settings.fonts) { typographyFor(settings.fonts) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalLumenExtras provides extrasFor(scheme, dark)) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = LumenShapes,
            content = content,
        )
    }
}
