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
    object IdleBubble : OverlayState()
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
    var pauseDurationMins: Int = 15

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun showIdleBubble() {
        if (overlayState.value is OverlayState.Hidden || overlayState.value is OverlayState.IdleBubble) {
            updateState(OverlayState.IdleBubble)
        }
    }

    fun showGrammarSuggestion(result: GrammarCheckResult, pauseMins: Int) {
        pauseDurationMins = pauseMins
        updateState(OverlayState.GrammarSuggestion(result))
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
        // Just briefly show an indicator or handle it in UI
        setActionResult("Looks good!")
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            setActionResult(null)
            if (overlayState.value is OverlayState.GrammarSuggestion) {
                updateState(OverlayState.IdleBubble)
            }
        }
    }

    fun hideOverlay() {
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
                        onDismiss = { hideOverlay() },
                        onExpand = { showChat() },
                        onOpenChat = { showChat() }
                    )
                }
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            windowManager.addView(composeView, layoutParams)
        } else {
            // Update flags if entering Chat state
            val layoutParams = composeView?.layoutParams as? WindowManager.LayoutParams
            if (layoutParams != null) {
                if (newState is OverlayState.Chat) {
                    layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                } else {
                    layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }
                windowManager.updateViewLayout(composeView, layoutParams)
            }
        }
    }
}
