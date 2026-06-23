package com.example.grammarlens.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
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
    var onDragPause: (() -> Unit)? = null  // Drag-to-pause: always 5 mins, regardless of settings
    var onSendMessage: ((String) -> Unit)? = null
    var onBack: (() -> Unit)? = null
    var lastGrammarResult: com.example.grammarlens.network.GrammarCheckResult? = null
    var currentImeHeight: Int = 0  // Updated by the service when keyboard shows/hides
    var pauseDurationMins: Int = 15
    /** Called from the chat TextField when user taps it — makes window keyboard-focusable. */
    var onRequestKeyboardFocus: (() -> Unit)? = null

    private var bubbleOffsetX = 0
    private var bubbleOffsetY = 0

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun showIdleBubble(hasError: Boolean = false) {
        val actualHasError = hasError || (lastGrammarResult?.isGrammarCorrect == false)
        if (overlayState.value is OverlayState.Hidden ||
            overlayState.value is OverlayState.IdleBubble) {
            updateState(OverlayState.IdleBubble(actualHasError))
        }
    }

    fun showGrammarSuggestion(result: GrammarCheckResult, pauseMins: Int) {
        pauseDurationMins = pauseMins
        actionResult.value = null
        lastGrammarResult = result
        
        val isUnusedChat = overlayState.value is OverlayState.Chat &&
                (composeView?.layoutParams as? WindowManager.LayoutParams)?.flags?.and(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0

        // Turn bubble RED to alert user — they click to open correction popup
        when (overlayState.value) {
            is OverlayState.Hidden, is OverlayState.IdleBubble ->
                updateState(OverlayState.IdleBubble(hasError = true))
            else -> {
                // If the user accidentally opened Chat before the grammar check finished, override it
                if (isUnusedChat) {
                    updateState(OverlayState.IdleBubble(hasError = true))
                }
            }
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

    /** Navigate back: RewritePreview → GrammarSuggestion, Chat → GrammarSuggestion, or → IdleBubble */
    fun goBack() {
        val lastResult = lastGrammarResult
        if (lastResult != null) {
            actionResult.value = null
            updateState(OverlayState.GrammarSuggestion(lastResult))
        } else {
            updateState(OverlayState.IdleBubble(hasError = false))
        }
    }

    fun hideOverlay() {
        // Do NOT clear lastGrammarResult here, so the user can re-open their last error.
        // Restore non-focusable flag before destroying view (clean state for next show)
        composeView?.let { view ->
            val lp = view.layoutParams as? WindowManager.LayoutParams
            if (lp != null && (lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
            }
        }
        // Always reset bubble position so it reappears at the default corner next time
        bubbleOffsetX = 0
        bubbleOffsetY = 0
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
                setOnTouchListener { _, ev ->
                    if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                        val state = overlayState.value
                        if (state !is OverlayState.IdleBubble && state !is OverlayState.Hidden) {
                            // Close popup. Keep bubble red if there is an active uncorrected error.
                            val hasErr = lastGrammarResult?.isGrammarCorrect == false
                            updateState(OverlayState.IdleBubble(hasError = hasErr))
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
                setViewTreeLifecycleOwner(this@FloatingOverlayManager)
                setViewTreeSavedStateRegistryOwner(this@FloatingOverlayManager)
                setContent {
                    val currentState by overlayState.collectAsState()
                    val loading by isLoadingAction.collectAsState()
                    val actResult by actionResult.collectAsState()
                    val chatList by chatHistory.collectAsState()
                    val inPauseZone by isDragInPauseZone.collectAsState()

                    OverlayScreen(
                        state = currentState,
                        chatHistory = chatList,
                        isLoadingAction = loading,
                        actionResult = actResult,
                        isDragInPauseZone = inPauseZone,
                        pauseDurationMins = pauseDurationMins,
                        onApplyFix = { onApplyFix?.invoke(it) },
                        onAction = { 
                            if (it == "ClearAction") {
                                setActionResult(null)
                            } else {
                                onAction?.invoke(it)
                            }
                        },
                        onExplain = { onExplain?.invoke() },
                        onPause = { onPause?.invoke() },
                        onSendMessage = { onSendMessage?.invoke(it) },
                        onBack = { goBack() },
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
                        onDrag = { dx, dy -> handleDrag(dx, dy) },
                        onDragEnd = { handleDragEnd() }
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
                (if (newState is OverlayState.Chat) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) or
                        (if (newState is OverlayState.Chat) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = if (newState is OverlayState.IdleBubble)
                    Gravity.BOTTOM or Gravity.END   // Bubble: anchor bottom-right
                else
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL  // Card: full-width centered
                
                if (newState is OverlayState.IdleBubble) {
                    x = bubbleOffsetX
                    y = currentImeHeight + bubbleOffsetY
                } else {
                    x = 0
                    y = currentImeHeight
                }
            }

            windowManager.addView(composeView, layoutParams)
            // If opening directly into Chat, show keyboard immediately
            if (newState is OverlayState.Chat) {
                composeView?.post {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    composeView?.let { imm?.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
                }
            }
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
                
                val wasFocusable = (layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0
                val shouldBeFocusable = (newState is OverlayState.Chat)
                val focusChanged = wasFocusable != shouldBeFocusable
                
                if (widthChanged || gravityChanged || focusChanged) {
                    layoutParams.width = newWidth
                    layoutParams.gravity = newGravity
                    
                    if (focusChanged) {
                        if (shouldBeFocusable) {
                            layoutParams.flags = layoutParams.flags and
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() and
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
                        } else {
                            layoutParams.flags = layoutParams.flags or
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        }
                    }
                    
                    // Apply offsets if returning to IdleBubble
                    if (newState is OverlayState.IdleBubble) {
                        layoutParams.x = bubbleOffsetX
                        layoutParams.y = currentImeHeight + bubbleOffsetY
                    } else {
                        layoutParams.x = 0
                        layoutParams.y = currentImeHeight
                    }

                    try { windowManager.updateViewLayout(composeView, layoutParams) } catch (e: Exception) {}
                    
                    // After making focusable for Chat, show the keyboard
                    if (shouldBeFocusable) {
                        composeView?.post {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            composeView?.let { imm?.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
                        }
                    }
                }
            }
        }
    }

    /** Removes FLAG_NOT_FOCUSABLE so the chat TextField can receive keyboard input.
     *  Deprecated: Focus is now managed automatically in updateState based on OverlayState.Chat */

    /** Called by the service whenever IME (keyboard) height changes. Repositions overlay above the keyboard. */
    fun updateBottomOffset(offsetPx: Int) {
        currentImeHeight = offsetPx
        val view = composeView ?: return
        if (!view.isAttachedToWindow) return
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return
        
        if (overlayState.value is OverlayState.IdleBubble) {
            lp.y = offsetPx + bubbleOffsetY
        } else {
            lp.y = offsetPx
        }
        
        try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
    }

    val isDragInPauseZone = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun handleDrag(dx: Float, dy: Float) {
        // Gravity is BOTTOM|END, so:
        // dx > 0 (drag right) means distance from END edge decreases -> subtract dx
        // dy > 0 (drag down) means distance from BOTTOM edge decreases -> subtract dy
        bubbleOffsetX -= dx.toInt()
        bubbleOffsetY -= dy.toInt()
        
        isDragInPauseZone.value = bubbleOffsetY < -150
        
        val view = composeView ?: return
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (overlayState.value is OverlayState.IdleBubble) {
            lp.x = bubbleOffsetX
            lp.y = currentImeHeight + bubbleOffsetY
            try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
        }
    }

    fun handleDragEnd() {
        if (isDragInPauseZone.value) {
            onDragPause?.invoke()   // Always 5-minute pause when dragged to dismiss
            isDragInPauseZone.value = false
            bubbleOffsetY = 0
            bubbleOffsetX = 0
        }
    }
}
