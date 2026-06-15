package com.example.grammarlens.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grammarlens.data.database.GrammarDatabase
import com.example.grammarlens.data.database.MistakeEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GrammarDatabase.getDatabase(application)
    private val mistakeDao = db.mistakeDao()

    val totalMistakesCount: StateFlow<Int> = mistakeDao.getTotalMistakesCount()
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
