package com.example.grammarlens.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
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
import kotlinx.coroutines.launch

class FloatingOverlayManager(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun showOverlay(result: GrammarCheckResult) {
        if (!Settings.canDrawOverlays(context)) return

        hideOverlay() // remove existing

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayManager)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayManager)
            setContent {
                OverlayScreen(result = result, onDismiss = { hideOverlay() })
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        windowManager.addView(composeView, layoutParams)
    }

    fun showSuccessOverlay() {
        if (!Settings.canDrawOverlays(context)) return
        hideOverlay()

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayManager)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayManager)
            setContent {
                OverlayScreen(result = null, isSuccess = true, onDismiss = { hideOverlay() })
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        windowManager.addView(composeView, layoutParams)

        // Auto dismiss after 2s
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            hideOverlay()
        }
    }

    fun hideOverlay() {
        composeView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            composeView = null
        }
    }
}
