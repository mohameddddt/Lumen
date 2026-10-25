package com.lumen.gallery.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumen.gallery.data.Album
import com.lumen.gallery.data.FontPairing
import com.lumen.gallery.data.Palette
import com.lumen.gallery.data.UserAlbumView
import com.lumen.gallery.data.ExifDetails
import com.lumen.gallery.data.MediaFilter
import com.lumen.gallery.data.MediaItem
import com.lumen.gallery.data.MediaRepository
import com.lumen.gallery.data.Memory
import com.lumen.gallery.data.Settings
import com.lumen.gallery.data.SettingsRepository
import com.lumen.gallery.data.SortOrder
import com.lumen.gallery.data.ThemeMode
import com.lumen.gallery.util.Format
import com.lumen.gallery.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class SearchResults(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val items: List<MediaItem> = emptyList(),
)

/** Screens address a list of media by a source key so the viewer can page through the same list. */
object Sources {
    const val ALL = "all"
    const val FAVORITES = "fav"
    const val VIDEOS = "videos"
    const val SEARCH = "search"
    fun album(id: Long) = "album_$id"
    fun memory(year: Int) = "mem_$year"
    fun userAlbum(id: String) = "ualbum_$id"
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepository(app)
    private val prefs = SettingsRepository(app)

    val settings: StateFlow<Settings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val started = MutableStateFlow(false)
    private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val updates: Flow<Unit> = merge(flowOf(Unit), merge(repo.changes(), refreshes).debounce(400))

    private val media: StateFlow<List<MediaItem>?> = started
        .flatMapLatest { on -> if (on) updates.mapLatest { repo.loadMedia() } else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val sortOrder = settings.map { it.sortOrder }.distinctUntilChanged()

    /** Ids of everything inside hidden folders; these never appear outside the vault. */
    val hiddenIds: StateFlow<Set<Long>> = settings
        .map { s -> s.userAlbums.filter { it.hidden }.flatMapTo(HashSet()) { it.itemIds } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** The whole library including hidden items, in sort order. Only used to resolve user albums. */
    private val allSorted: StateFlow<List<MediaItem>> = combine(media, sortOrder) { list, order ->
        list.orEmpty().sortedWith(order.comparator)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The library minus hidden-folder items: what every public screen is built from. */
    private val visible: StateFlow<List<MediaItem>?> = combine(media, hiddenIds) { list, hidden ->
        if (list == null || hidden.isEmpty()) list else list.filter { it.id !in hidden }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val sortedOrNull: StateFlow<List<MediaItem>?> =
        combine(visible, sortOrder) { list, order ->
            list?.sortedWith(order.comparator)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Every visible item in the library, in the user's sort order. */
    val sorted: StateFlow<List<MediaItem>> = sortedOrNull.map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The main Photos timeline; `null` while the library is still loading. Excludes hidden albums. */
    val timeline: StateFlow<List<MediaItem>?> =
        combine(sortedOrNull, settings.map { it.hiddenAlbums }.distinctUntilChanged()) { list, hidden ->
            if (list == null || hidden.isEmpty()) list else list.filter { it.bucketId !in hidden }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Device folders, with any cover the user picked. */
    val albums: StateFlow<List<Album>> =
        combine(visible, settings.map { it.folderCovers }.distinctUntilChanged()) { list, covers ->
            buildAlbums(list.orEmpty(), covers)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Albums the user created, newest first; includes hidden folders (screens filter them). */
    val userAlbums: StateFlow<List<UserAlbumView>> =
        combine(settings.map { it.userAlbums }.distinctUntilChanged(), allSorted, hiddenIds) { albums, all, hidden ->
            val byId = all.associateBy { it.id }
            albums.sortedByDescending { it.createdAt }.map { album ->
                val ids = album.itemIds.toHashSet()
                // A photo that is also in a hidden folder stays out of ordinary albums.
                val items = all.filter { it.id in ids && (album.hidden || it.id !in hidden) }
                val cover = album.coverId?.let(byId::get)?.takeIf { it in items } ?: items.firstOrNull()
                UserAlbumView(album, items, cover)
            }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Hidden folders lock

    private val _vaultUnlocked = MutableStateFlow(false)
    val vaultUnlocked: StateFlow<Boolean> = _vaultUnlocked.asStateFlow()
    fun unlockVault() { _vaultUnlocked.value = true }
    fun lockVault() { _vaultUnlocked.value = false }

    val memories: StateFlow<List<Memory>> = timeline.map { buildMemories(it.orEmpty()) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Most recent months that have media, with item counts, for search suggestions. */
    val months: StateFlow<List<Pair<YearMonth, Int>>> = visible.map { list ->
        list.orEmpty()
            .groupingBy { YearMonth.from(it.dateTaken.toLocalDate()) }
            .eachCount()
            .toList()
            .sortedByDescending { it.first }
            .take(12)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Search

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _filter = MutableStateFlow(MediaFilter.ALL)
    val filter: StateFlow<MediaFilter> = _filter.asStateFlow()

    private val searchKeys: StateFlow<Map<Long, String>> = visible.map { list ->
        list.orEmpty().associate { it.id to searchKey(it) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val searchResults: StateFlow<SearchResults> = combine(
        _query.debounce(150), _filter, sorted, searchKeys, settings.map { it.favorites }.distinctUntilChanged(),
    ) { q, f, list, keys, favorites ->
        val query = q.trim()
        if (query.isEmpty() && f == MediaFilter.ALL) return@combine SearchResults(query, f)
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val items = list.filter { item ->
            f.matches(item, favorites) && terms.all { term -> keys[item.id]?.contains(term) == true }
        }
        SearchResults(query, f, items)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, SearchResults())

    // Recently deleted (only queried while the bin screen is open)

    val trash: StateFlow<List<MediaItem>?> = started
        .flatMapLatest { on ->
            if (on) updates.mapLatest { repo.loadMedia(trashed = true).sortedByDescending { it.expiresAt } }
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Selection

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    fun toggleSelection(id: Long) = _selection.update { if (id in it) it - id else it + id }
    fun setSelected(ids: Collection<Long>, selected: Boolean) =
        _selection.update { if (selected) it + ids else it - ids.toSet() }
    fun selectAll(ids: Collection<Long>) { _selection.value = ids.toSet() }
    fun clearSelection() { _selection.value = emptySet() }

    // Sources

    fun itemsFor(source: String): Flow<List<MediaItem>> =
        combine(sorted, timeline, settings, searchResults, allSorted) { all, timeline, settings, search, everything ->
            pick(source, all, timeline.orEmpty(), settings, search.items, everything)
        }.distinctUntilChanged()

    fun itemsNow(source: String): List<MediaItem> =
        pick(source, sorted.value, timeline.value.orEmpty(), settings.value, searchResults.value.items, allSorted.value)

    /** Whether [source] is a hidden folder, which must stay behind the lock. */
    fun isHiddenSource(source: String): Boolean =
        source.startsWith("ualbum_") &&
            settings.value.userAlbums.any { it.id == source.removePrefix("ualbum_") && it.hidden }

    private fun pick(
        source: String,
        all: List<MediaItem>,
        timeline: List<MediaItem>,
        settings: Settings,
        search: List<MediaItem>,
        everything: List<MediaItem>,
    ): List<MediaItem> = when {
        source.startsWith("ualbum_") -> settings.userAlbums.firstOrNull { it.id == source.removePrefix("ualbum_") }
            ?.let { album ->
                val ids = album.itemIds.toHashSet()
                (if (album.hidden) everything else all).filter { it.id in ids }
            }.orEmpty()
        source == Sources.ALL -> timeline
        source == Sources.FAVORITES -> all.filter { it.id in settings.favorites }
        source == Sources.VIDEOS -> all.filter { it.isVideo }
        source == Sources.SEARCH -> search
        source.startsWith("album_") -> source.removePrefix("album_").toLongOrNull()
            ?.let { id -> all.filter { it.bucketId == id } }.orEmpty()
        source.startsWith("mem_") -> source.removePrefix("mem_").toIntOrNull()?.let { year ->
            val today = LocalDate.now()
            timeline.filter {
                val d = it.dateTaken.toLocalDate()
                d.year == year && d.monthValue == today.monthValue && d.dayOfMonth == today.dayOfMonth
            }
        }.orEmpty()
        else -> emptyList()
    }

    // Commands

    fun start() { started.value = true }
    fun refresh() { refreshes.tryEmit(Unit) }
    fun setQuery(query: String) { _query.value = query }
    fun setFilter(filter: MediaFilter) { _filter.value = filter }

    fun toggleFavorites(ids: Collection<Long>) { viewModelScope.launch { prefs.toggleFavorites(ids) } }
    fun setSortOrder(order: SortOrder) { viewModelScope.launch { prefs.setSortOrder(order) } }
    fun setGridColumns(columns: Int) { viewModelScope.launch { prefs.setGridColumns(columns) } }
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { prefs.setTheme(mode) } }
    fun setDynamicColor(enabled: Boolean) { viewModelScope.launch { prefs.setDynamicColor(enabled) } }
    fun setPureBlack(enabled: Boolean) { viewModelScope.launch { prefs.setPureBlack(enabled) } }
    fun setShowMemories(enabled: Boolean) { viewModelScope.launch { prefs.setShowMemories(enabled) } }
    fun setAlbumHidden(id: Long, hidden: Boolean) { viewModelScope.launch { prefs.setAlbumHidden(id, hidden) } }
    fun setPalette(palette: Palette) { viewModelScope.launch { prefs.setPalette(palette) } }
    fun setFonts(fonts: FontPairing) { viewModelScope.launch { prefs.setFonts(fonts) } }
    fun setFolderCover(bucketId: Long, mediaId: Long?) { viewModelScope.launch { prefs.setFolderCover(bucketId, mediaId) } }

    fun createAlbum(name: String, hidden: Boolean, items: List<MediaItem>, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = prefs.createAlbum(name.trim(), hidden, items.map { it.id })
            if (items.isNotEmpty()) toast("Added ${Format.plural(items.size, "item")} to ${name.trim()}")
            onCreated(id)
        }
    }

    fun addToAlbum(album: UserAlbumView, items: List<MediaItem>) {
        viewModelScope.launch {
            prefs.addToAlbum(album.id, items.map { it.id })
            val count = Format.plural(items.size, "item")
            toast(if (album.hidden) "Hid $count in ${album.name}" else "Added $count to ${album.name}")
        }
    }

    fun removeFromAlbum(albumId: String, items: List<MediaItem>) {
        viewModelScope.launch { prefs.removeFromAlbum(albumId, items.map { it.id }) }
    }

    fun renameAlbum(albumId: String, name: String) { viewModelScope.launch { prefs.renameAlbum(albumId, name.trim()) } }
    fun setAlbumCover(albumId: String, mediaId: Long?) { viewModelScope.launch { prefs.setAlbumCover(albumId, mediaId) } }
    fun setUserAlbumHidden(albumId: String, hidden: Boolean) { viewModelScope.launch { prefs.setUserAlbumHidden(albumId, hidden) } }
    fun deleteAlbum(albumId: String) { viewModelScope.launch { prefs.deleteAlbum(albumId) } }

    private fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    fun rename(item: MediaItem, newName: String) {
        viewModelScope.launch {
            if (!repo.rename(item, newName)) {
                Toast.makeText(getApplication(), "Couldn't rename ${item.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun loadExif(item: MediaItem): ExifDetails? = repo.loadExif(item)

    private fun buildAlbums(list: List<MediaItem>, covers: Map<Long, Long>): List<Album> =
        list.groupBy { it.bucketId }.map { (id, items) ->
            val chosen = covers[id]?.let { coverId -> items.firstOrNull { it.id == coverId } }
            val cover = chosen ?: items.maxBy { it.dateTaken }
            Album(id, cover.bucketName.ifBlank { "Internal storage" }, cover, items.size, cover.relativePath)
        }.sortedWith(compareByDescending<Album> { it.isCamera }.thenByDescending { it.cover.dateTaken })

    private fun buildMemories(list: List<MediaItem>): List<Memory> {
        val today = LocalDate.now()
        return list.asSequence()
            .map { it to it.dateTaken.toLocalDate() }
            .filter { (_, d) -> d.monthValue == today.monthValue && d.dayOfMonth == today.dayOfMonth && d.year < today.year }
            .groupBy({ it.second.year }, { it.first })
            .map { (year, items) -> Memory(year, today.year - year, items) }
            .sortedBy { it.yearsAgo }
    }

    private fun searchKey(item: MediaItem): String =
        "${item.name} ${item.bucketName} ${Format.longDate(item.dateTaken)}".lowercase()
}
