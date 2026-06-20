package com.example.grammarlens.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.grammarlens.network.GrammarCheckResult
import com.example.grammarlens.ui.overlay.OverlayScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class OverlayState {
    object Hidden : OverlayState()
    data class IdleBubble(val hasError: Boolean = false) : OverlayState()
    data class GrammarSuggestion(val result: GrammarCheckResult) : OverlayState()
    data class RewritePreview(val originalText: String, val newText: String) : OverlayState()
    object Chat : OverlayState()
}

class FloatingOverlayManager(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    val overlayState = MutableStateFlow<OverlayState>(OverlayState.Hidden)
    val isLoadingAction = MutableStateFlow(false)
    val actionResult = MutableStateFlow<String?>(null)
    
    val chatHistory = MutableStateFlow<List<com.example.grammarlens.network.GroqMessage>>(emptyList())

    // Callbacks provided by the service
    var onApplyFix: ((String) -> Unit)? = null
    var onAction: ((String) -> Unit)? = null
    var onExplain: (() -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onSendMessage: ((String) -> Unit)? = null
    var onBack: (() -> Unit)? = null
    var lastGrammarResult: com.example.grammarlens.network.GrammarCheckResult? = null
    var currentImeHeight: Int = 0  // Updated by the service when keyboard shows/hides
    var pauseDurationMins: Int = 15
    /** Called from the chat TextField when user taps it — makes window keyboard-focusable. */
    var onRequestKeyboardFocus: (() -> Unit)? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun showIdleBubble(hasError: Boolean = false) {
        if (overlayState.value is OverlayState.Hidden ||
            overlayState.value is OverlayState.IdleBubble) {
            updateState(OverlayState.IdleBubble(hasError))
        }
    }

    fun showGrammarSuggestion(result: GrammarCheckResult, pauseMins: Int) {
        pauseDurationMins = pauseMins
        actionResult.value = null
        lastGrammarResult = result
        // Turn bubble RED to alert user — they click to open correction popup
        when (overlayState.value) {
            is OverlayState.Hidden, is OverlayState.IdleBubble ->
                updateState(OverlayState.IdleBubble(hasError = true))
            else -> {} // Don't override an already-open popup
        }
    }

    fun showRewritePreview(originalText: String, newText: String) {
        updateState(OverlayState.RewritePreview(originalText, newText))
    }

    fun showChat() {
        updateState(OverlayState.Chat)
    }

    fun setActionLoading(isLoading: Boolean) {
        isLoadingAction.value = isLoading
    }

    fun setActionResult(resultText: String?) {
        actionResult.value = resultText
        isLoadingAction.value = false
    }

    fun showSuccessOverlay() {
        lastGrammarResult = null  // No errors — clear cached result
        updateState(OverlayState.IdleBubble(hasError = false))
    }

    fun backFromChat() {
        val lastResult = lastGrammarResult
        if (lastResult != null) {
            actionResult.value = null
            updateState(OverlayState.GrammarSuggestion(lastResult))
        } else {
            updateState(OverlayState.IdleBubble(hasError = false))
        }
    }

    fun hideOverlay() {
        lastGrammarResult = null  // Clear cached error on explicit dismiss
        // Restore non-focusable flag before destroying view (clean state for next show)
        composeView?.let { view ->
            val lp = view.layoutParams as? WindowManager.LayoutParams
            if (lp != null && (lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
            }
        }
        updateState(OverlayState.Hidden)
    }

    private fun updateState(newState: OverlayState) {
        if (!Settings.canDrawOverlays(context)) return

        if (newState is OverlayState.Hidden) {
            composeView?.let {
                if (it.isAttachedToWindow) {
                    windowManager.removeView(it)
                }
                composeView = null
            }
            overlayState.value = newState
            return
        }

        val wasHidden = composeView == null
        overlayState.value = newState

        if (wasHidden) {
            composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(this@FloatingOverlayManager)
                setViewTreeSavedStateRegistryOwner(this@FloatingOverlayManager)
                setContent {
                    val currentState by overlayState.collectAsState()
                    val loading by isLoadingAction.collectAsState()
                    val actResult by actionResult.collectAsState()
                    val chatList by chatHistory.collectAsState()

                    OverlayScreen(
                        state = currentState,
                        chatHistory = chatList,
                        isLoadingAction = loading,
                        actionResult = actResult,
                        pauseDurationMins = pauseDurationMins,
                        onApplyFix = { onApplyFix?.invoke(it) },
                        onAction = { onAction?.invoke(it) },
                        onExplain = { onExplain?.invoke() },
                        onPause = { onPause?.invoke() },
                        onSendMessage = { onSendMessage?.invoke(it) },
                        onBack = { backFromChat() },
                        onDismiss = { hideOverlay() },
                        onExpand = {
                            // Red bubble clicked: open the correction popup if available, else chat
                            val result = lastGrammarResult
                            if (result != null) {
                                updateState(OverlayState.GrammarSuggestion(result))
                            } else {
                                showChat()
                            }
                        },
                        onOpenChat = { showChat() },
                        onRequestKeyboardFocus = { makeWindowFocusable() }
                    )
                }
            }

            val layoutParams = WindowManager.LayoutParams(
                if (newState is OverlayState.IdleBubble) WindowManager.LayoutParams.WRAP_CONTENT
                else WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = if (newState is OverlayState.IdleBubble)
                    Gravity.BOTTOM or Gravity.END   // Bubble: anchor bottom-right
                else
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL  // Card: full-width centered
                y = currentImeHeight
            }

            windowManager.addView(composeView, layoutParams)
            // Post a position update after the view is attached to handle delayed IME detection
            composeView?.post {
                if (currentImeHeight > 0) updateBottomOffset(currentImeHeight)
            }
        } else {
            val layoutParams = composeView?.layoutParams as? WindowManager.LayoutParams
            if (layoutParams != null) {
                // Update width and gravity based on state (IdleBubble = compact, others = full-width)
                val newWidth = if (newState is OverlayState.IdleBubble)
                    WindowManager.LayoutParams.WRAP_CONTENT
                else
                    WindowManager.LayoutParams.MATCH_PARENT
                val newGravity = if (newState is OverlayState.IdleBubble)
                    Gravity.BOTTOM or Gravity.END
                else
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

                val widthChanged = layoutParams.width != newWidth
                val gravityChanged = layoutParams.gravity != newGravity
                // NOTE: We no longer toggle FLAG_NOT_FOCUSABLE here.
                // The window stays non-focusable until the user explicitly taps the chat input,
                // at which point makeWindowFocusable() is called — this prevents keyboard dismissal.
                if (widthChanged || gravityChanged) {
                    layoutParams.width = newWidth
                    layoutParams.gravity = newGravity
                    try { windowManager.updateViewLayout(composeView, layoutParams) } catch (e: Exception) {}
                }
            }
        }
    }

    /** Removes FLAG_NOT_FOCUSABLE so the chat TextField can receive keyboard input.
     *  Called only when the user explicitly taps the chat input — avoids unwanted keyboard dismissal. */
    fun makeWindowFocusable() {
        val view = composeView ?: return
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return
        val alreadyFocusable = (lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0
        if (!alreadyFocusable) {
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
        }
    }

    /** Called by the service whenever IME (keyboard) height changes. Repositions overlay above the keyboard. */
    fun updateBottomOffset(offsetPx: Int) {
        currentImeHeight = offsetPx
        val view = composeView ?: return
        if (!view.isAttachedToWindow) {
            // View not yet attached — will be picked up by post() after addView
            return
        }
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (lp.y != offsetPx) {
            lp.y = offsetPx
            try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
        }
    }
}
