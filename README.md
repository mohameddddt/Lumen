# Lumen Gallery

A calm, fast, offline gallery app for Android, built with Kotlin and Jetpack Compose.

**Design:** an editorial look of warm paper and ink with a single coral accent. Titles use the Instrument Serif display font and the interface uses Manrope. Icons are thin line icons, navigation is a floating frosted-glass dock, and the chips, switches, segmented controls and video scrubber are custom.

## Features

- **Photos timeline** grouped by day (or by month at 5+ columns), with pinch-to-resize grid (2–6 columns)
- **On this day** memories from previous years
- **Albums** with Favorites, Videos, Hidden and Recently deleted (bin) shortcuts
- **Your own albums**: create albums, add photos from multi-select, the viewer or a picker, and choose any photo as the cover. Device folders can have custom covers too. Photos are only referenced, never copied or moved.
- **Hidden folders**: locked with fingerprint, face or screen lock, and kept out of Photos, Albums, Search and Memories. Screenshots are blocked while they're open, and they re-lock when you leave the app. The files stay on the phone, so other gallery apps can still see them.
- **Palettes**: Coral, Sage, Ocean, Amber, Rose and Ink, each with its own tinted background. Wallpaper colors are optional.
- **Typefaces**: Editorial, Classic, Modern, Rounded or System
- **Search** by file name, album or date ("october 2024", "camera"), plus filters for photos, videos, favorites, GIFs and screenshots
- **Viewer**: swipe between items, pinch / double-tap zoom, video playback with seek and mute, slideshow
- **Details sheet**: date, resolution, size, path and camera EXIF (model, aperture, shutter, ISO, focal length)
- **Multi-select** (long-press, or tap a date header's circle): share, favorite, delete
- **Recently deleted**: restore or permanently delete, with days remaining
- Rename, edit with another app, set as wallpaper or contact photo, open with
- Hide albums from the Photos timeline
- Theme: System / Light / Dark, Material You dynamic color, pure-black dark mode
- Supports Android 14+ "selected photos only" access

## Requirements

- Android 11 (API 30) or newer
- JDK 17 and the Android SDK (API 36) to build

## Build and install

Use the **release** build on a phone. It is optimized with R8 and ships startup profiles, so it is much smoother than the debug build.

```bash
./gradlew installRelease         # optimized build, installs over the debug build and keeps your data
./gradlew assembleRelease        # APK at app/build/outputs/apk/release/app-release.apk
./gradlew installDebug           # debug build for development (slower)
```

The release build is signed with the local debug key so it can be sideloaded. Use a real signing key before publishing to a store.

## Project layout

```
app/src/main/java/com/lumen/gallery/
  data/        MediaStore repository, settings (DataStore), thumbnail fetcher
  ui/          ViewModel, navigation, theme
  ui/components  Grid, thumbnails, zoomable image, shared bars
  ui/screens   Photos, Albums, Search, Collection, Viewer, Bin, Settings, Permission
  util/        Formatting, permissions, system media actions
```

## Credits

- Fonts: Instrument Serif, Manrope, Playfair Display, Space Grotesk and Nunito (Google Fonts), SIL Open Font License
- Icons: adapted from [Lucide](https://lucide.dev), ISC License
- Glass blur: [Haze](https://github.com/chrisbanes/haze), Apache 2.0
