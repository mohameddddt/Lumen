package com.lumen.gallery.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons used across Lumen, adapted from Lucide (https://lucide.dev, ISC license).
 * Drawn as 24×24 strokes so they tint like any other icon.
 */
object LumenIcons {
    val Trash: ImageVector by lazy { lineIcon("Trash", "M10 11v6", "M14 11v6", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M3 6h18", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2") }
    val Image: ImageVector by lazy { lineIcon("Image", "M5.0,3.0h14.0a2.0,2.0 0 0 1 2.0,2.0v14.0a2.0,2.0 0 0 1 -2.0,2.0h-14.0a2.0,2.0 0 0 1 -2.0,-2.0v-14.0a2.0,2.0 0 0 1 2.0,-2.0Z", "M7.0,9.0a2.0,2.0 0 1,0 4.0,0a2.0,2.0 0 1,0 -4.0,0", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21") }
    val Images: ImageVector by lazy { lineIcon("Images", "m22 11-1.296-1.296a2.4 2.4 0 0 0-3.408 0L11 16", "M4 8a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2", "M12.0,7.0a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0", "M10.0,2.0h10.0a2.0,2.0 0 0 1 2.0,2.0v10.0a2.0,2.0 0 0 1 -2.0,2.0h-10.0a2.0,2.0 0 0 1 -2.0,-2.0v-10.0a2.0,2.0 0 0 1 2.0,-2.0Z") }
    val Search: ImageVector by lazy { lineIcon("Search", "m21 21-4.34-4.34", "M3.0,11.0a8.0,8.0 0 1,0 16.0,0a8.0,8.0 0 1,0 -16.0,0") }
    val Heart: ImageVector by lazy { lineIcon("Heart", "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5") }
    val Share: ImageVector by lazy { lineIcon("Share", "M12 2v13", "m16 6-4-4-4 4", "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8") }
    val Info: ImageVector by lazy { lineIcon("Info", "M2.0,12.0a10.0,10.0 0 1,0 20.0,0a10.0,10.0 0 1,0 -20.0,0", "M12 16v-4", "M12 8h.01") }
    val Pencil: ImageVector by lazy { lineIcon("Pencil", "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z", "m15 5 4 4") }
    val Close: ImageVector by lazy { lineIcon("Close", "M18 6 6 18", "m6 6 12 12") }
    val Check: ImageVector by lazy { lineIcon("Check", "M20 6 9 17l-5-5") }
    val ArrowLeft: ImageVector by lazy { lineIcon("ArrowLeft", "m12 19-7-7 7-7", "M19 12H5") }
    val More: ImageVector by lazy { lineIcon("More", "M11.0,12.0a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0", "M18.0,12.0a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0", "M4.0,12.0a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0") }
    val Sort: ImageVector by lazy { lineIcon("Sort", "m21 16-4 4-4-4", "M17 20V4", "m3 8 4-4 4 4", "M7 4v16") }
    val Settings: ImageVector by lazy { lineIcon("Settings", "M14 17H5", "M19 7h-9", "M14.0,17.0a3.0,3.0 0 1,0 6.0,0a3.0,3.0 0 1,0 -6.0,0", "M4.0,7.0a3.0,3.0 0 1,0 6.0,0a3.0,3.0 0 1,0 -6.0,0") }
    val Play: ImageVector by lazy { lineIcon("Play", "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z") }
    val Pause: ImageVector by lazy { lineIcon("Pause", "M15.0,3.0h3.0a1.0,1.0 0 0 1 1.0,1.0v16.0a1.0,1.0 0 0 1 -1.0,1.0h-3.0a1.0,1.0 0 0 1 -1.0,-1.0v-16.0a1.0,1.0 0 0 1 1.0,-1.0Z", "M6.0,3.0h3.0a1.0,1.0 0 0 1 1.0,1.0v16.0a1.0,1.0 0 0 1 -1.0,1.0h-3.0a1.0,1.0 0 0 1 -1.0,-1.0v-16.0a1.0,1.0 0 0 1 1.0,-1.0Z") }
    val Volume: ImageVector by lazy { lineIcon("Volume", "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z", "M16 9a5 5 0 0 1 0 6", "M19.364 18.364a9 9 0 0 0 0-12.728") }
    val Mute: ImageVector by lazy { lineIcon("Mute", "M11 4.702a.7.7 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.7.7 0 0 0 11 19.298z", "m16.5 14.5 5-5", "m16.5 9.5 5 5") }
    val Film: ImageVector by lazy { lineIcon("Film", "M5.0,3.0h14.0a2.0,2.0 0 0 1 2.0,2.0v14.0a2.0,2.0 0 0 1 -2.0,2.0h-14.0a2.0,2.0 0 0 1 -2.0,-2.0v-14.0a2.0,2.0 0 0 1 2.0,-2.0Z", "M7 3v18", "M3 7.5h4", "M3 12h18", "M3 16.5h4", "M17 3v18", "M17 7.5h4", "M17 16.5h4") }
    val Calendar: ImageVector by lazy { lineIcon("Calendar", "M8 2v3", "M16 2v3", "M5.0,3.0h14.0a2.0,2.0 0 0 1 2.0,2.0v14.0a2.0,2.0 0 0 1 -2.0,2.0h-14.0a2.0,2.0 0 0 1 -2.0,-2.0v-14.0a2.0,2.0 0 0 1 2.0,-2.0Z", "M3 9h18") }
    val Folder: ImageVector by lazy { lineIcon("Folder", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z") }
    val Camera: ImageVector by lazy { lineIcon("Camera", "M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z", "M9.0,13.0a3.0,3.0 0 1,0 6.0,0a3.0,3.0 0 1,0 -6.0,0") }
    val Aperture: ImageVector by lazy { lineIcon("Aperture", "M2.0,12.0a10.0,10.0 0 1,0 20.0,0a10.0,10.0 0 1,0 -20.0,0", "m14.31 8 5.74 9.94", "M9.69 8h11.48", "m7.38 12 5.74-9.94", "M9.69 16 3.95 6.06", "M14.31 16H2.83", "m16.62 12-5.74 9.94") }
    val Eye: ImageVector by lazy { lineIcon("Eye", "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0", "M9.0,12.0a3.0,3.0 0 1,0 6.0,0a3.0,3.0 0 1,0 -6.0,0") }
    val EyeOff: ImageVector by lazy { lineIcon("EyeOff", "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49", "M14.084 14.158a3 3 0 0 1-4.242-4.242", "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143", "m2 2 20 20") }
    val Slideshow: ImageVector by lazy { lineIcon("Slideshow", "M2 7v10", "M6 5v14", "M12.0,3.0h8.0a2.0,2.0 0 0 1 2.0,2.0v14.0a2.0,2.0 0 0 1 -2.0,2.0h-8.0a2.0,2.0 0 0 1 -2.0,-2.0v-14.0a2.0,2.0 0 0 1 2.0,-2.0Z") }
    val Wallpaper: ImageVector by lazy { lineIcon("Wallpaper", "M12 17v4", "M8 21h8", "m9 17 6.1-6.1a2 2 0 0 1 2.81.01L22 15", "M6.0,9.0a2.0,2.0 0 1,0 4.0,0a2.0,2.0 0 1,0 -4.0,0", "M4.0,3.0h16.0a2.0,2.0 0 0 1 2.0,2.0v10.0a2.0,2.0 0 0 1 -2.0,2.0h-16.0a2.0,2.0 0 0 1 -2.0,-2.0v-10.0a2.0,2.0 0 0 1 2.0,-2.0Z") }
    val External: ImageVector by lazy { lineIcon("External", "M15 3h6v6", "M10 14 21 3", "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6") }
    val Rename: ImageVector by lazy { lineIcon("Rename", "M12 20h-1a2 2 0 0 1-2-2 2 2 0 0 1-2 2H6", "M13 8h7a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-7", "M5 16H4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h1", "M6 4h1a2 2 0 0 1 2 2 2 2 0 0 1 2-2h1", "M9 6v12") }
    val Restore: ImageVector by lazy { lineIcon("Restore", "M3.0,3.0h18.0a1.0,1.0 0 0 1 1.0,1.0v3.0a1.0,1.0 0 0 1 -1.0,1.0h-18.0a1.0,1.0 0 0 1 -1.0,-1.0v-3.0a1.0,1.0 0 0 1 1.0,-1.0Z", "M4 8v11a2 2 0 0 0 2 2h2", "M20 8v11a2 2 0 0 1-2 2h-2", "m9 15 3-3 3 3", "M12 12v9") }
    val DeleteForever: ImageVector by lazy { lineIcon("DeleteForever", "M2.0,12.0a10.0,10.0 0 1,0 20.0,0a10.0,10.0 0 1,0 -20.0,0", "m15 9-6 6", "m9 9 6 6") }
    val Sparkles: ImageVector by lazy { lineIcon("Sparkles", "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z", "M20 2v4", "M22 4h-4", "M2.0,20.0a2.0,2.0 0 1,0 4.0,0a2.0,2.0 0 1,0 -4.0,0") }
    val Palette: ImageVector by lazy { lineIcon("Palette", "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z", "M13.0,6.5a0.5,0.5 0 1,0 1.0,0a0.5,0.5 0 1,0 -1.0,0", "M17.0,10.5a0.5,0.5 0 1,0 1.0,0a0.5,0.5 0 1,0 -1.0,0", "M6.0,12.5a0.5,0.5 0 1,0 1.0,0a0.5,0.5 0 1,0 -1.0,0", "M8.0,7.5a0.5,0.5 0 1,0 1.0,0a0.5,0.5 0 1,0 -1.0,0") }
    val Moon: ImageVector by lazy { lineIcon("Moon", "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401") }
    val Contrast: ImageVector by lazy { lineIcon("Contrast", "M12 2v2", "M14.837 16.385a6 6 0 1 1-7.223-7.222c.624-.147.97.66.715 1.248a4 4 0 0 0 5.26 5.259c.589-.255 1.396.09 1.248.715", "M16 12a4 4 0 0 0-4-4", "m19 5-1.256 1.256", "M20 12h2") }
    val Grid: ImageVector by lazy { lineIcon("Grid", "M4.0,3.0h5.0a1.0,1.0 0 0 1 1.0,1.0v5.0a1.0,1.0 0 0 1 -1.0,1.0h-5.0a1.0,1.0 0 0 1 -1.0,-1.0v-5.0a1.0,1.0 0 0 1 1.0,-1.0Z", "M15.0,3.0h5.0a1.0,1.0 0 0 1 1.0,1.0v5.0a1.0,1.0 0 0 1 -1.0,1.0h-5.0a1.0,1.0 0 0 1 -1.0,-1.0v-5.0a1.0,1.0 0 0 1 1.0,-1.0Z", "M15.0,14.0h5.0a1.0,1.0 0 0 1 1.0,1.0v5.0a1.0,1.0 0 0 1 -1.0,1.0h-5.0a1.0,1.0 0 0 1 -1.0,-1.0v-5.0a1.0,1.0 0 0 1 1.0,-1.0Z", "M4.0,14.0h5.0a1.0,1.0 0 0 1 1.0,1.0v5.0a1.0,1.0 0 0 1 -1.0,1.0h-5.0a1.0,1.0 0 0 1 -1.0,-1.0v-5.0a1.0,1.0 0 0 1 1.0,-1.0Z") }
    val SearchX: ImageVector by lazy { lineIcon("SearchX", "m13.5 8.5-5 5", "m8.5 8.5 5 5", "M3.0,11.0a8.0,8.0 0 1,0 16.0,0a8.0,8.0 0 1,0 -16.0,0", "m21 21-4.3-4.3") }
    val SelectAll: ImageVector by lazy { lineIcon("SelectAll", "M18 6 7 17l-5-5", "m22 10-7.5 7.5L13 16") }
    val ChevronRight: ImageVector by lazy { lineIcon("ChevronRight", "m9 18 6-6-6-6") }
    val Lock: ImageVector by lazy { lineIcon("Lock", "M5.0,11.0h14.0a2.0,2.0 0 0 1 2.0,2.0v7.0a2.0,2.0 0 0 1 -2.0,2.0h-14.0a2.0,2.0 0 0 1 -2.0,-2.0v-7.0a2.0,2.0 0 0 1 2.0,-2.0Z", "M7 11V7a5 5 0 0 1 10 0v4") }
    val Sun: ImageVector by lazy { lineIcon("Sun", "M8.0,12.0a4.0,4.0 0 1,0 8.0,0a4.0,4.0 0 1,0 -8.0,0", "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41") }
    val Smartphone: ImageVector by lazy { lineIcon("Smartphone", "M7.0,2.0h10.0a2.0,2.0 0 0 1 2.0,2.0v16.0a2.0,2.0 0 0 1 -2.0,2.0h-10.0a2.0,2.0 0 0 1 -2.0,-2.0v-16.0a2.0,2.0 0 0 1 2.0,-2.0Z", "M12 18h.01") }
    val Layers: ImageVector by lazy { lineIcon("Layers", "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z", "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12", "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17") }
    val Clapper: ImageVector by lazy { lineIcon("Clapper", "m12.296 3.464 3.02 3.956", "M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3z", "M3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", "m6.18 5.276 3.1 3.899") }
    val PlayFilled: ImageVector by lazy { lineIcon("PlayFilled", "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z", filled = true) }
    val PauseFilled: ImageVector by lazy { lineIcon("PauseFilled", "M15.0,3.0h3.0a1.0,1.0 0 0 1 1.0,1.0v16.0a1.0,1.0 0 0 1 -1.0,1.0h-3.0a1.0,1.0 0 0 1 -1.0,-1.0v-16.0a1.0,1.0 0 0 1 1.0,-1.0Z", "M6.0,3.0h3.0a1.0,1.0 0 0 1 1.0,1.0v16.0a1.0,1.0 0 0 1 -1.0,1.0h-3.0a1.0,1.0 0 0 1 -1.0,-1.0v-16.0a1.0,1.0 0 0 1 1.0,-1.0Z", filled = true) }
    val Plus: ImageVector by lazy { lineIcon("Plus", "M5 12h14", "M12 5v14") }
    val ImagePlus: ImageVector by lazy { lineIcon("ImagePlus", "M16 5h6", "M19 2v6", "M21 11.5V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7.5", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21", "M7.0,9.0a2.0,2.0 0 1,0 4.0,0a2.0,2.0 0 1,0 -4.0,0") }
    val ImageMinus: ImageVector by lazy { lineIcon("ImageMinus", "M21 9v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7", "M16,5L22,5", "M7.0,9.0a2.0,2.0 0 1,0 4.0,0a2.0,2.0 0 1,0 -4.0,0", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21") }
    val FolderLock: ImageVector by lazy { lineIcon("FolderLock", "M15.0,17.0h6.0a1.0,1.0 0 0 1 1.0,1.0v3.0a1.0,1.0 0 0 1 -1.0,1.0h-6.0a1.0,1.0 0 0 1 -1.0,-1.0v-3.0a1.0,1.0 0 0 1 1.0,-1.0Z", "M10 20H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H20a2 2 0 0 1 2 2v2.5", "M20 17v-2a2 2 0 1 0-4 0v2") }
    val Unlock: ImageVector by lazy { lineIcon("Unlock", "M5.0,11.0h14.0a2.0,2.0 0 0 1 2.0,2.0v7.0a2.0,2.0 0 0 1 -2.0,2.0h-14.0a2.0,2.0 0 0 1 -2.0,-2.0v-7.0a2.0,2.0 0 0 1 2.0,-2.0Z", "M7 11V7a5 5 0 0 1 9.9-1") }
    val FolderPlus: ImageVector by lazy { lineIcon("FolderPlus", "M12 10v6", "M9 13h6", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z") }
    val Type: ImageVector by lazy { lineIcon("Type", "M12 4v16", "M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2", "M9 20h6") }
    val HeartFilled: ImageVector by lazy { lineIcon("HeartFilled", "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5", filled = true) }
}

private fun lineIcon(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
