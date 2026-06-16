package com.example.grammarlens.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
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
import kotlinx.coroutines.withContext

class GrammarAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null

    private lateinit var grammarChecker: GrammarChecker
    private lateinit var overlayManager: FloatingOverlayManager
    private lateinit var database: GrammarDatabase

    // Track the last focused editable node so we can replace text in it
    private var lastEditableNode: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        grammarChecker = GrammarChecker(this)
        overlayManager = FloatingOverlayManager(this)
        database = GrammarDatabase.getDatabase(this)
        Log.d("GrammarLens", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val sharedPrefs = getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("service_enabled", true)
        if (!isEnabled) return

        val type = event?.eventType ?: return
        if (type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        // Save the editable node for later text replacement
        event.source?.let { node ->
            if (node.isEditable) {
                lastEditableNode = node
            }
        }

        val nodeText = event.source?.text?.toString()
        val eventText = event.text?.joinToString("") { it }
        val text = (nodeText ?: eventText)?.trim() ?: return

        if (text.split(" ").size < 2) return

        Log.d("GrammarLens", "Text event: '${text.takeLast(60)}'")

        debounceJob?.cancel()

        val endsWithPunctuation = text.last() in listOf('.', '?', '!')
        val delayMs = if (endsWithPunctuation) 800L else 2500L

        val candidate = if (endsWithPunctuation) {
            val parts = text.split(Regex("(?<=[.!?])\\s+"))
            parts.lastOrNull { it.trim().length > 3 }?.trim() ?: text
        } else {
            text
        }

        Log.d("GrammarLens", "Scheduling check in ${delayMs}ms for: '${candidate.takeLast(50)}'")

        debounceJob = serviceScope.launch {
            delay(delayMs)
            processSentence(candidate)
        }
    }

    /** Replaces the text inside the currently focused input field */
    fun replaceTextInFocusedField(newText: String) {
        val node = lastEditableNode ?: findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (node == null) {
            Log.w("GrammarLens", "No editable node found for text replacement")
            return
        }
        val bundle = Bundle()
        bundle.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            newText
        )
        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        Log.d("GrammarLens", "Text replacement success: $success → '$newText'")
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
                    overlayManager.showOverlay(
                        result = result,
                        onApplyFix = { fixedText ->
                            replaceTextInFocusedField(fixedText)
                            overlayManager.hideOverlay()
                        },
                        onAction = { actionType ->
                            serviceScope.launch {
                                withContext(Dispatchers.Main) { overlayManager.setActionLoading(true) }
                                val newText = grammarChecker.applyActionToSentence(sentence, actionType)
                                if (newText != null) {
                                    // Auto-apply the result directly into the text field
                                    replaceTextInFocusedField(newText)
                                    withContext(Dispatchers.Main) { overlayManager.hideOverlay() }
                                } else {
                                    withContext(Dispatchers.Main) { overlayManager.setActionResult(null) }
                                }
                            }
                        }
                    )
                }
            } else {
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

