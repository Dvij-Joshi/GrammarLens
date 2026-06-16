package com.example.grammarlens.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grammarlens.data.database.GrammarDatabase
import com.example.grammarlens.data.database.MistakeEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DailyTrend(
    val date: LocalDate,
    val dateString: String,
    val mistakes: Int,
    val checks: Int
) {
    val mistakeRate: Float get() = if (checks > 0) mistakes.toFloat() / checks else 0f
}

enum class ImprovementStatus { IMPROVING, WORSENING, SAME, NO_DATA }

data class CategoryDetail(
    val category: String,
    val count: Int,
    val percentage: Float,
    val recentExamples: List<MistakeEntity>,
    val improvement: ImprovementStatus
)

data class RepeatedMistake(
    val originalText: String,
    val correctedText: String,
    val count: Int
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GrammarDatabase.getDatabase(application)
    private val mistakeDao = db.mistakeDao()
    private val sharedPrefs = application.getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(sharedPrefs.getString("groq_api_key", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _apiUrl = MutableStateFlow(sharedPrefs.getString("groq_api_url", "https://api.groq.com/openai/v1/") ?: "https://api.groq.com/openai/v1/")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _isServiceEnabled = MutableStateFlow(sharedPrefs.getBoolean("service_enabled", true))
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    fun toggleServiceEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("service_enabled", enabled).apply()
        _isServiceEnabled.value = enabled
    }

    fun saveApiSettings(key: String, url: String) {
        val validUrl = if (url.endsWith("/")) url else "$url/"
        sharedPrefs.edit()
            .putString("groq_api_key", key)
            .putString("groq_api_url", validUrl)
            .apply()
        _apiKey.value = key
        _apiUrl.value = validUrl
    }

    fun deleteMistake(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mistakeDao.deleteMistake(id)
        }
    }

    val totalChecksCount: StateFlow<Int> = mistakeDao.getTotalChecksCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val currentStreak: StateFlow<Int> = mistakeDao.getDistinctCheckDates()
        .map { dateStrings ->
            if (dateStrings.isEmpty()) return@map 0
            var streak = 0
            val today = LocalDate.now()
            var currentDate = today
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

    private val allMistakesFlow = mistakeDao.getAllMistakes()

    // 1. Trend Line (14 Days)
    val trendData: StateFlow<List<DailyTrend>> = allMistakesFlow.map { entities ->
        val today = LocalDate.now()
        val fourteenDaysAgo = today.minusDays(13)
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

        // Initialize empty map for all 14 days
        val dailyMap = mutableMapOf<LocalDate, Pair<Int, Int>>() // <Mistakes, Checks>
        for (i in 0..13) {
            dailyMap[fourteenDaysAgo.plusDays(i.toLong())] = Pair(0, 0)
        }

        entities.forEach { entity ->
            val date = Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (!date.isBefore(fourteenDaysAgo) && !date.isAfter(today)) {
                val current = dailyMap[date] ?: Pair(0, 0)
                val mistakesDelta = if (!entity.isCorrect) 1 else 0
                dailyMap[date] = Pair(current.first + mistakesDelta, current.second + 1)
            }
        }

        dailyMap.entries.sortedBy { it.key }.map { (date, counts) ->
            DailyTrend(date, date.format(dateFormatter), counts.first, counts.second)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. Mistake Breakdown (Categories, %, Improvement, Examples)
    val categoryBreakdown: StateFlow<List<CategoryDetail>> = allMistakesFlow.map { entities ->
        val justMistakes = entities.filter { !it.isCorrect }
        val totalMistakes = justMistakes.flatMap { it.mistakeTypes }.size.coerceAtLeast(1)
        
        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(7)
        val fourteenDaysAgo = today.minusDays(14)

        val categoriesMap = mutableMapOf<String, MutableList<MistakeEntity>>()
        val last7DaysCount = mutableMapOf<String, Int>()
        val previous7DaysCount = mutableMapOf<String, Int>()

        justMistakes.forEach { entity ->
            val date = Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            entity.mistakeTypes.forEach { type ->
                categoriesMap.getOrPut(type) { mutableListOf() }.add(entity)
                
                if (!date.isBefore(sevenDaysAgo)) {
                    last7DaysCount[type] = last7DaysCount.getOrDefault(type, 0) + 1
                } else if (!date.isBefore(fourteenDaysAgo) && date.isBefore(sevenDaysAgo)) {
                    previous7DaysCount[type] = previous7DaysCount.getOrDefault(type, 0) + 1
                }
            }
        }

        categoriesMap.map { (category, examples) ->
            val currentWeek = last7DaysCount.getOrDefault(category, 0)
            val pastWeek = previous7DaysCount.getOrDefault(category, 0)
            
            val improvement = when {
                pastWeek == 0 && currentWeek == 0 -> ImprovementStatus.NO_DATA
                currentWeek < pastWeek -> ImprovementStatus.IMPROVING
                currentWeek > pastWeek -> ImprovementStatus.WORSENING
                else -> ImprovementStatus.SAME
            }

            CategoryDetail(
                category = category,
                count = examples.size,
                percentage = (examples.size.toFloat() / totalMistakes) * 100f,
                recentExamples = examples.take(5), // Top 5 examples
                improvement = improvement
            )
        }.sortedByDescending { it.count }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. Time-of-day Heatmap
    val hourlyHeatmap: StateFlow<List<Float>> = allMistakesFlow.map { entities ->
        val hourCounts = FloatArray(24) { 0f }
        entities.filter { !it.isCorrect }.forEach { entity ->
            val hour = Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).hour
            hourCounts[hour] += 1f
        }
        hourCounts.toList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, List(24) { 0f })

    // 4. Most Repeated Mistake
    val mostRepeatedMistake: StateFlow<RepeatedMistake?> = allMistakesFlow.map { entities ->
        entities.filter { !it.isCorrect }
            .groupBy { Pair(it.originalText, it.correctedText) }
            .maxByOrNull { it.value.size }
            ?.let { (pair, list) ->
                if (list.size > 1) RepeatedMistake(pair.first, pair.second, list.size) else null
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

}
