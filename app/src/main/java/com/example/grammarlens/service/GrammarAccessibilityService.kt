package com.example.grammarlens.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.grammarlens.data.database.GrammarDatabase
import com.example.grammarlens.data.database.MistakeEntity
import com.example.grammarlens.network.GrammarChecker
import com.example.grammarlens.overlay.FloatingOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GrammarAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null
    
    private lateinit var grammarChecker: GrammarChecker
    private lateinit var overlayManager: FloatingOverlayManager
    private lateinit var database: GrammarDatabase

    override fun onServiceConnected() {
        super.onServiceConnected()
        grammarChecker = GrammarChecker(this)
        overlayManager = FloatingOverlayManager(this)
        database = GrammarDatabase.getDatabase(this)
        Log.d("GrammarLens", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        if (type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        // Try source node text first, fall back to event text
        val nodeText = event.source?.text?.toString()
        val eventText = event.text?.joinToString("") { it }
        val text = (nodeText ?: eventText)?.trim() ?: return

        Log.d("GrammarLens", "Text event: '${text.takeLast(50)}'")

        // Trigger when the text contains a sentence-ending punctuation anywhere
        val sentenceEndRegex = Regex("(?<=[.!?])\\s*$")
        if (!sentenceEndRegex.containsMatchIn(text) && text.last() !in listOf('.', '?', '!')) return

        debounceJob?.cancel()

        // Split by sentence boundaries and get the last non-empty sentence
        val parts = text.split(Regex("(?<=[.!?])\\s+"))
        // The last part might be a new fragment or a complete sentence
        val candidate = parts.lastOrNull { it.trim().length > 3 }?.trim() ?: return

        Log.d("GrammarLens", "Candidate sentence to check: '$candidate'")

        debounceJob = serviceScope.launch {
            delay(800)
            processSentence(candidate)
        }
    }

    private suspend fun processSentence(sentence: String) {
        Log.d("GrammarLens", "Processing sentence: $sentence")
        val result = grammarChecker.analyzeSentence(sentence)
        if (result != null) {
            if (!result.isGrammarCorrect) {
                val entity = MistakeEntity(
                    originalText = sentence,
                    correctedText = result.correctedText,
                    mistakeTypes = result.mistakes.map { it.category }.distinct(),
                    isCorrect = false
                )
                database.mistakeDao().insertMistake(entity)

                withContext(Dispatchers.Main) {
                    overlayManager.showOverlay(result, onAction = { actionType ->
                        serviceScope.launch {
                            withContext(Dispatchers.Main) { overlayManager.setActionLoading(true) }
                            val newText = grammarChecker.applyActionToSentence(sentence, actionType)
                            withContext(Dispatchers.Main) { overlayManager.setActionResult(newText) }
                        }
                    })
                }
            } else {
                // Log correct sentences for tracking checks/streak
                val entity = MistakeEntity(
                    originalText = sentence,
                    correctedText = sentence,
                    mistakeTypes = emptyList(),
                    isCorrect = true
                )
                database.mistakeDao().insertMistake(entity)
                
                withContext(Dispatchers.Main) {
                    overlayManager.showSuccessOverlay()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("GrammarLens", "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        debounceJob?.cancel()
        overlayManager.hideOverlay()
    }
}
