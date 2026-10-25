package com.lumen.gallery.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Accent color plus the hue that tints the neutral "paper" backgrounds. */
enum class Palette(
    val label: String,
    val hue: Float,
    val saturation: Float,
    val accentLight: Long,
    val accentDark: Long,
) {
    CORAL("Coral", 40f, 0.26f, 0xFFE5532D, 0xFFFF7246),
    SAGE("Sage", 100f, 0.16f, 0xFF4E7A57, 0xFF93C9A0),
    OCEAN("Ocean", 215f, 0.20f, 0xFF2D5BD2, 0xFF8DB0FF),
    AMBER("Amber", 44f, 0.36f, 0xFFB5761A, 0xFFF3B95C),
    ROSE("Rose", 340f, 0.18f, 0xFFC23F6A, 0xFFFF8FB3),
    INK("Ink", 0f, 0f, 0xFF1A1A1A, 0xFFF2F2F2),
}

enum class FontPairing(val label: String, val description: String) {
    EDITORIAL("Editorial", "Instrument Serif & Manrope"),
    CLASSIC("Classic", "Playfair Display & Manrope"),
    MODERN("Modern", "Space Grotesk"),
    ROUNDED("Rounded", "Nunito"),
    SYSTEM("System", "Your phone's font"),
}

enum class SortOrder(val label: String, val comparator: Comparator<MediaItem>) {
    DATE_DESC("Newest first", compareByDescending<MediaItem> { it.dateTaken }.thenByDescending { it.id }),
    DATE_ASC("Oldest first", compareBy<MediaItem> { it.dateTaken }.thenBy { it.id }),
    NAME("Name", compareBy<MediaItem, String>(String.CASE_INSENSITIVE_ORDER) { it.name }),
    SIZE("Largest first", compareByDescending<MediaItem> { it.size });

    val isByDate: Boolean get() = this == DATE_DESC || this == DATE_ASC
}

/** An album the user made in Lumen. Photos stay where they are; the album only references them. */
data class UserAlbum(
    val id: String,
    val name: String,
    val itemIds: List<Long>,
    val coverId: Long?,
    val hidden: Boolean,
    val createdAt: Long,
)

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val palette: Palette = Palette.CORAL,
    val fonts: FontPairing = FontPairing.EDITORIAL,
    val dynamicColor: Boolean = false,
    val pureBlack: Boolean = false,
    val gridColumns: Int = DEFAULT_COLUMNS,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val showMemories: Boolean = true,
    val favorites: Set<Long> = emptySet(),
    val hiddenAlbums: Set<Long> = emptySet(),
    /** Cover photo chosen for a device folder, keyed by bucket id. */
    val folderCovers: Map<Long, Long> = emptyMap(),
    val userAlbums: List<UserAlbum> = emptyList(),
) {
    companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 6
        const val DEFAULT_COLUMNS = 4
    }
}

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            Settings(
                theme = p[THEME].toEnum(ThemeMode.SYSTEM),
                palette = p[PALETTE].toEnum(Palette.CORAL),
                fonts = p[FONTS].toEnum(FontPairing.EDITORIAL),
                dynamicColor = p[DYNAMIC_COLOR] ?: false,
                pureBlack = p[PURE_BLACK] ?: false,
                gridColumns = (p[GRID_COLUMNS] ?: Settings.DEFAULT_COLUMNS)
                    .coerceIn(Settings.MIN_COLUMNS, Settings.MAX_COLUMNS),
                sortOrder = p[SORT].toEnum(SortOrder.DATE_DESC),
                showMemories = p[MEMORIES] ?: true,
                favorites = p[FAVORITES].toIds(),
                hiddenAlbums = p[HIDDEN_ALBUMS].toIds(),
                folderCovers = p[FOLDER_COVERS].orEmpty().mapNotNull { entry ->
                    val (bucket, media) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                    (bucket.toLongOrNull() ?: return@mapNotNull null) to (media.toLongOrNull() ?: return@mapNotNull null)
                }.toMap(),
                userAlbums = decodeAlbums(p[USER_ALBUMS]),
            )
        }

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    suspend fun setPalette(palette: Palette) {
        store.edit {
            it[PALETTE] = palette.name
            it[DYNAMIC_COLOR] = false
        }
    }

    suspend fun setFonts(fonts: FontPairing) {
        store.edit { it[FONTS] = fonts.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setPureBlack(enabled: Boolean) {
        store.edit { it[PURE_BLACK] = enabled }
    }

    suspend fun setGridColumns(columns: Int) {
        store.edit { it[GRID_COLUMNS] = columns.coerceIn(Settings.MIN_COLUMNS, Settings.MAX_COLUMNS) }
    }

    suspend fun setSortOrder(order: SortOrder) {
        store.edit { it[SORT] = order.name }
    }

    suspend fun setShowMemories(enabled: Boolean) {
        store.edit { it[MEMORIES] = enabled }
    }

    /** Adds all [ids] to favorites, or removes them if every one is already a favorite. */
    suspend fun toggleFavorites(ids: Collection<Long>) {
        store.edit { p ->
            val current = p[FAVORITES].orEmpty()
            val keys = ids.mapTo(HashSet()) { it.toString() }
            p[FAVORITES] = if (current.containsAll(keys)) current - keys else current + keys
        }
    }

    suspend fun setAlbumHidden(id: Long, hidden: Boolean) {
        store.edit { p ->
            val current = p[HIDDEN_ALBUMS].orEmpty()
            p[HIDDEN_ALBUMS] = if (hidden) current + id.toString() else current - id.toString()
        }
    }

    suspend fun setFolderCover(bucketId: Long, mediaId: Long?) {
        store.edit { p ->
            val others = p[FOLDER_COVERS].orEmpty().filterNot { it.startsWith("$bucketId:") }.toSet()
            p[FOLDER_COVERS] = if (mediaId == null) others else others + "$bucketId:$mediaId"
        }
    }

    // User albums

    suspend fun createAlbum(name: String, hidden: Boolean, itemIds: List<Long>): String {
        val id = UUID.randomUUID().toString()
        editAlbums { it + UserAlbum(id, name, itemIds.distinct(), null, hidden, System.currentTimeMillis()) }
        return id
    }

    suspend fun addToAlbum(albumId: String, itemIds: Collection<Long>) = editAlbum(albumId) { album ->
        album.copy(itemIds = (album.itemIds + itemIds).distinct())
    }

    suspend fun removeFromAlbum(albumId: String, itemIds: Collection<Long>) = editAlbum(albumId) { album ->
        val remove = itemIds.toSet()
        album.copy(
            itemIds = album.itemIds.filterNot { it in remove },
            coverId = album.coverId?.takeUnless { it in remove },
        )
    }

    suspend fun renameAlbum(albumId: String, name: String) = editAlbum(albumId) { it.copy(name = name) }

    suspend fun setAlbumCover(albumId: String, mediaId: Long?) = editAlbum(albumId) { it.copy(coverId = mediaId) }

    suspend fun setUserAlbumHidden(albumId: String, hidden: Boolean) = editAlbum(albumId) { it.copy(hidden = hidden) }

    suspend fun deleteAlbum(albumId: String) = editAlbums { list -> list.filterNot { it.id == albumId } }

    private suspend fun editAlbum(albumId: String, transform: (UserAlbum) -> UserAlbum) =
        editAlbums { list -> list.map { if (it.id == albumId) transform(it) else it } }

    private suspend fun editAlbums(transform: (List<UserAlbum>) -> List<UserAlbum>) {
        store.edit { p: MutablePreferences ->
            p[USER_ALBUMS] = encodeAlbums(transform(decodeAlbums(p[USER_ALBUMS])))
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
        val FONTS = stringPreferencesKey("fonts")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SORT = stringPreferencesKey("sort_order")
        val MEMORIES = booleanPreferencesKey("show_memories")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val HIDDEN_ALBUMS = stringSetPreferencesKey("hidden_albums")
        val FOLDER_COVERS = stringSetPreferencesKey("folder_covers")
        val USER_ALBUMS = stringPreferencesKey("user_albums")
    }
}

private fun encodeAlbums(albums: List<UserAlbum>): String = JSONArray().apply {
    albums.forEach { album ->
        put(
            JSONObject()
                .put("id", album.id)
                .put("name", album.name)
                .put("cover", album.coverId ?: -1L)
                .put("hidden", album.hidden)
                .put("created", album.createdAt)
                .put("items", JSONArray(album.itemIds)),
        )
    }
}.toString()

private fun decodeAlbums(json: String?): List<UserAlbum> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        List(array.length()) { index ->
            val o = array.getJSONObject(index)
            val items = o.optJSONArray("items") ?: JSONArray()
            UserAlbum(
                id = o.getString("id"),
                name = o.optString("name", "Album"),
                itemIds = List(items.length()) { items.getLong(it) },
                coverId = o.optLong("cover", -1L).takeIf { it >= 0 },
                hidden = o.optBoolean("hidden", false),
                createdAt = o.optLong("created", 0L),
            )
        }
    }.getOrDefault(emptyList())
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

private fun Set<String>?.toIds(): Set<Long> =
    this?.mapNotNullTo(HashSet()) { it.toLongOrNull() } ?: emptySet()
