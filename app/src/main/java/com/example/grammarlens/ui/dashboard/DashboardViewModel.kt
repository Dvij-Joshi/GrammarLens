package com.example.grammarlens.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grammarlens.data.database.GrammarDatabase
import com.example.grammarlens.data.database.MistakeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GrammarDatabase.getDatabase(application)
    private val mistakeDao = db.mistakeDao()
    private val sharedPrefs = application.getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(sharedPrefs.getString("groq_api_key", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _apiUrl = MutableStateFlow(sharedPrefs.getString("groq_api_url", "https://api.groq.com/openai/v1/") ?: "https://api.groq.com/openai/v1/")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    fun saveApiSettings(key: String, url: String) {
        val validUrl = if (url.endsWith("/")) url else "$url/"
        sharedPrefs.edit()
            .putString("groq_api_key", key)
            .putString("groq_api_url", validUrl)
            .apply()
        _apiKey.value = key
        _apiUrl.value = validUrl
    }

    val totalChecksCount: StateFlow<Int> = mistakeDao.getTotalChecksCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val currentStreak: StateFlow<Int> = mistakeDao.getDistinctCheckDates()
        .map { dateStrings ->
            // Dates are returned as "YYYY-MM-DD" in descending order
            if (dateStrings.isEmpty()) return@map 0

            var streak = 0
            val today = java.time.LocalDate.now()
            var currentDate = today

            // If the user hasn't checked today, start checking from yesterday. 
            // If they also didn't check yesterday, the streak is broken (0).
            if (dateStrings.first() == today.toString()) {
                currentDate = today
            } else if (dateStrings.first() == today.minusDays(1).toString()) {
                currentDate = today.minusDays(1)
            } else {
                return@map 0
            }

            for (dateStr in dateStrings) {
                if (dateStr == currentDate.toString()) {
                    streak++
                    currentDate = currentDate.minusDays(1)
                } else {
                    break
                }
            }
            streak
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val recentMistakes: StateFlow<List<MistakeEntity>> = mistakeDao.getRecentMistakes(10)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topMistakeCategories: StateFlow<List<Pair<String, Int>>> = mistakeDao.getAllMistakes()
        .map { mistakes ->
            val categoryCounts = mutableMapOf<String, Int>()
            mistakes.forEach { entity ->
                entity.mistakeTypes.forEach { type ->
                    categoryCounts[type] = categoryCounts.getOrDefault(type, 0) + 1
                }
            }
            categoryCounts.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key to it.value }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
