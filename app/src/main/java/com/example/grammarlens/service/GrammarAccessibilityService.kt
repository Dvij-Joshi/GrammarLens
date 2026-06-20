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
import com.example.grammarlens.overlay.OverlayState
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
        
        // Setup Overlay Callbacks
        overlayManager.onApplyFix = { fixedText ->
            replaceTextInFocusedField(fixedText)
            overlayManager.hideOverlay()
        }
        
        var lastToneAction = "Make Formal" // Track for Retry
        
        overlayManager.onAction = { actionType ->
            serviceScope.launch {
                withContext(Dispatchers.Main) { overlayManager.setActionLoading(true) }
                
                val actionToPerform = if (actionType == "Retry") lastToneAction else actionType
                if (actionType != "Retry") lastToneAction = actionType

                val original = lastEditableNode?.text?.toString() ?: ""
                val newText = grammarChecker.applyActionToSentence(original, actionToPerform)
                
                withContext(Dispatchers.Main) { 
                    overlayManager.setActionLoading(false)
                    if (newText != null) {
                        overlayManager.showRewritePreview(original, newText)
                    } else {
                        overlayManager.setActionResult("Failed to rewrite. Try again.")
                    }
                }
            }
        }

        overlayManager.onSendMessage = { userMessage ->
            serviceScope.launch {
                withContext(Dispatchers.Main) {
                    val currentHistory = overlayManager.chatHistory.value.toMutableList()
                    currentHistory.add(com.example.grammarlens.network.GroqMessage(role = "user", content = userMessage))
                    overlayManager.chatHistory.value = currentHistory
                    overlayManager.setActionLoading(true)
                }

                val reply = grammarChecker.chatWithAssistant(overlayManager.chatHistory.value)

                withContext(Dispatchers.Main) {
                    overlayManager.setActionLoading(false)
                    if (reply != null) {
                        val currentHistory = overlayManager.chatHistory.value.toMutableList()
                        currentHistory.add(com.example.grammarlens.network.GroqMessage(role = "assistant", content = reply))
                        overlayManager.chatHistory.value = currentHistory
                    } else {
                        overlayManager.setActionResult("Network error. Could not reach assistant.")
                    }
                }
            }
        }
        
        overlayManager.onExplain = {
            // Need result mistakes, but for now we'll just show a generic explanation if triggered
        }
        
        overlayManager.onPause = {
            val prefs = getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)
            val pauseMins = prefs.getInt("pause_duration_mins", 15)
            val now = System.currentTimeMillis()
            prefs.edit().putLong("pause_until", now + (pauseMins * 60 * 1000L)).apply()
            overlayManager.hideOverlay()
        }

        Log.d("GrammarLens", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val sharedPrefs = getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)
        if (!sharedPrefs.getBoolean("service_enabled", true)) return
        if (System.currentTimeMillis() < sharedPrefs.getLong("pause_until", 0L)) return

        val packageName = event?.packageName?.toString() ?: ""
        if (packageName == this.packageName) return // Ignore events from our own app (Issue 3 fix)
        
        val blacklistedApps = sharedPrefs.getStringSet("blacklisted_apps", emptySet()) ?: emptySet()
        if (blacklistedApps.contains(packageName.lowercase())) return

        val type = event?.eventType ?: return

        // Check if keyboard is visible
        val isKeyboardVisible = try {
            windows?.any { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD } ?: false
        } catch (e: Exception) { false }

        if (!isKeyboardVisible && overlayManager.overlayState.value is OverlayState.IdleBubble) {
            CoroutineScope(Dispatchers.Main).launch {
                overlayManager.hideOverlay()
            }
        }

        // Check if an editable field gained or lost focus
        if (type == AccessibilityEvent.TYPE_VIEW_FOCUSED || type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val node = event.source
            if (node?.isEditable == true) {
                lastEditableNode = node
                // Show bubble if focus gained and keyboard is visible
                if (isKeyboardVisible) {
                    CoroutineScope(Dispatchers.Main).launch {
                        overlayManager.showIdleBubble()
                    }
                }
            } else if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (!isKeyboardVisible && overlayManager.overlayState.value is OverlayState.IdleBubble) {
                    CoroutineScope(Dispatchers.Main).launch {
                        overlayManager.hideOverlay()
                    }
                }
            }
        }

        if (type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        // Save the editable node for later text replacement
        event.source?.let { node ->
            if (node.isEditable) {
                lastEditableNode = node
                CoroutineScope(Dispatchers.Main).launch {
                    overlayManager.showIdleBubble()
                }
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
                    val prefs = getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)
                    val pauseMins = prefs.getInt("pause_duration_mins", 15)
                    
                    // Wire up explain properly using the current result
                    overlayManager.onExplain = {
                        serviceScope.launch {
                            withContext(Dispatchers.Main) { overlayManager.setActionLoading(true) }
                            val explanation = grammarChecker.explainMistake(sentence, result.mistakes)
                            withContext(Dispatchers.Main) {
                                overlayManager.setActionLoading(false)
                                overlayManager.setActionResult(explanation ?: "Sorry, I couldn't explain this mistake right now.")
                            }
                        }
                    }

                    overlayManager.showGrammarSuggestion(result, pauseMins)
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

