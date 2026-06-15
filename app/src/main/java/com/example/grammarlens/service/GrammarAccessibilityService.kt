package com.example.grammarlens.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.grammarlens.data.database.GrammarDatabase
import com.example.grammarlens.data.database.MistakeEntity
import com.example.grammarlens.network.GrammarChecker
import com.example.grammarlens.overlay.FloatingOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val nodeInfo: AccessibilityNodeInfo? = event.source
            val text = nodeInfo?.text?.toString() ?: return

            // Check if sentence ends with punctuation
            if (text.isNotEmpty() && text.last() in listOf('.', '?', '!')) {
                // Cancel any existing debounce job
                debounceJob?.cancel()

                // Extract the current sentence. For simplicity, we process the whole text block or last sentence.
                // Let's just process the entire text snippet for grammar checking if it's short, or split by punctuation.
                val sentences = text.split(Regex("(?<=[.!?])\\s+"))
                val lastSentence = sentences.lastOrNull()?.trim() ?: return

                if (lastSentence.isNotEmpty()) {
                    debounceJob = serviceScope.launch {
                        delay(800) // 800ms debounce
                        processSentence(lastSentence)
                    }
                }
            }
        }
    }

    private suspend fun processSentence(sentence: String) {
        Log.d("GrammarLens", "Processing sentence: ${"$"}sentence")
        val result = grammarChecker.analyzeSentence(sentence)

        if (result != null) {
            if (!result.isGrammarCorrect && result.mistakes.isNotEmpty()) {
                // Save to DB
                val entity = MistakeEntity(
                    originalText = result.originalText,
                    correctedText = result.correctedText,
                    mistakeTypes = result.mistakes.map { it.category }
                )
                database.mistakeDao().insertMistake(entity)

                // Show overlay
                launch(Dispatchers.Main) {
                    overlayManager.showOverlay(result)
                }
            } else {
                // Valid sentence, optionally show a tiny green tick and dismiss
                launch(Dispatchers.Main) {
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
