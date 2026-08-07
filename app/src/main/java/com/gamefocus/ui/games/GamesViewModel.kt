package com.gamefocus.ui.games

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.GameDao
import com.gamefocus.data.models.GameApp
import com.gamefocus.data.repository.GamesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<Application>()
    private val db = AppDatabase.getInstance(app)
    private val repo = GamesRepository(app.applicationContext, db.gameDao())

    enum class SortOption { NAME, LAST_PLAYED, RECENTLY_INSTALLED, PLAY_TIME, FAVORITES }

    enum class FilterOption { ALL, FAVORITES, RECENTLY_PLAYED, NEVER_PLAYED }

    private val _allGames = MutableStateFlow<List<GameApp>>(emptyList())
    private val _games = MutableStateFlow<List<GameApp>>(emptyList())
    val games: StateFlow<List<GameApp>> = _games.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _gameCount = MutableStateFlow(0)
    val gameCount: StateFlow<Int> = _gameCount.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentSort: SortOption = SortOption.NAME
    private var currentFilter: FilterOption = FilterOption.ALL

    init {
        observeGames()
        observeCount()
    }

    private fun observeGames() {
        viewModelScope.launch {
            repo.observeSavedGamesWithLastSession().collectLatest { list ->
                if (_isLoading.value) _isLoading.value = false
                val mapped = withContext(Dispatchers.IO) {
                    val pm = app.packageManager
                    list.map { entity ->
                        val icon = try {
                            pm.getApplicationIcon(entity.packageName)
                        } catch (_: Exception) {
                            null
                        }
                        GameApp(
                            packageName = entity.packageName,
                            appName = entity.appName,
                            icon = icon,
                            category = "GAME",
                            installDate = entity.installDate,
                            updateDate = entity.updateDate,
                            lastPlayed = entity.lastPlayed,
                            lastSessionDate = entity.lastSessionDate,
                            lastSessionDuration = entity.lastSessionDuration,
                            isFavorite = entity.isFavorite
                        )
                    }
                }
                _allGames.value = mapped
                applySortAndFilter()
            }
        }
    }

    private fun observeCount() {
        viewModelScope.launch {
            repo.getGamesCount().collectLatest { count ->
                _gameCount.value = count
            }
        }
    }

    fun setSortOption(sort: SortOption) {
        currentSort = sort
        applySortAndFilter()
    }

    fun setFilterOption(filter: FilterOption) {
        currentFilter = filter
        applySortAndFilter()
    }

    private fun applySortAndFilter() {
        var list = _allGames.value
        val q = _query.value.trim()
        if (q.isNotBlank()) {
            list = list.filter {
                it.appName.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
            }
        }
        list = when (currentFilter) {
            FilterOption.ALL -> list
            FilterOption.FAVORITES -> list.filter { it.isFavorite }
            FilterOption.RECENTLY_PLAYED -> list.filter { it.lastPlayed > 0 }
            FilterOption.NEVER_PLAYED -> list.filter { it.lastPlayed == 0L }
        }
        list = when (currentSort) {
            SortOption.NAME -> list.sortedBy { it.appName.lowercase() }
            SortOption.LAST_PLAYED -> list.sortedByDescending { it.lastPlayed }
            SortOption.RECENTLY_INSTALLED -> list.sortedByDescending { it.installDate }
            SortOption.PLAY_TIME -> list.sortedByDescending { it.playTimeMillis }
            SortOption.FAVORITES -> list.sortedBy { !it.isFavorite }
        }
        _games.value = list
    }

    fun refreshGames() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        _error.value = null
        viewModelScope.launch {
            val result = repo.scanAndSaveGames()
            result.onSuccess { stats ->
                _error.value = "Scanned ${stats.totalScanned} apps, found ${stats.totalFound} games"
            }.onFailure { e ->
                _error.value = "Scan failed: ${e.message}"
            }
            _isRefreshing.value = false
        }
    }

    fun searchGames(q: String) {
        _query.value = q
        applySortAndFilter()
    }

    fun toggleFavorite(game: GameApp) {
        viewModelScope.launch {
            repo.toggleFavorite(game.packageName)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
