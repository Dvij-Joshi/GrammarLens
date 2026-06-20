package com.example.grammarlens.ui.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.grammarlens.network.GrammarCheckResult
import com.example.grammarlens.ui.components.PastelColors
import com.example.grammarlens.overlay.OverlayState
import com.example.grammarlens.network.GroqMessage

@Composable
fun OverlayScreen(
    state: OverlayState,
    chatHistory: List<GroqMessage> = emptyList(),
    isLoadingAction: Boolean = false,
    actionResult: String? = null,
    pauseDurationMins: Int = 15,
    onApplyFix: (String) -> Unit = {},
    onAction: (String) -> Unit = {},
    onExplain: () -> Unit = {},
    onPause: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onExpand: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onRequestKeyboardFocus: () -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> },  // dx, dy in px — bubble drag
    onDismiss: () -> Unit
) {
    if (state is OverlayState.Hidden) return

    val context = LocalContext.current

    if (state is OverlayState.IdleBubble) {
        val hasError = state.hasError
        // Outer Box: handles drag gesture and routes dx/dy to window-level repositioning
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            // Inner circle: handles tap-to-expand
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (hasError) Color(0xFFFF6B6B) else PastelColors.CardBlue)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onExpand() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "GrammarLens",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(PastelColors.CardPurple)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Show back arrow when there's a logical "parent" screen to return to
                            val showBack = state is OverlayState.Chat ||
                                    state is OverlayState.RewritePreview
                            if (showBack) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PastelColors.TextMain, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PastelColors.TextMain, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = when (state) {
                                    is OverlayState.Chat -> "Assistant"
                                    is OverlayState.RewritePreview -> "Rewrite Preview"
                                    else -> "GrammarLens"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = PastelColors.TextMain
                            )
                        }
                        Row {
                            // Show chat icon only on GrammarSuggestion screen
                            if (state is OverlayState.GrammarSuggestion) {
                                IconButton(
                                    onClick = onOpenChat,
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = "Chat", tint = PastelColors.TextMain, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = PastelColors.TextMain, modifier = Modifier.size(16.dp))
                            }
                        }
                    }


                    Spacer(Modifier.height(20.dp))

                    when (state) {
                        is OverlayState.GrammarSuggestion -> {
                            val result = state.result

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    result.mistakes.firstOrNull()?.category ?: "Suggestion",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PastelColors.TextMain
                                )
                                Spacer(Modifier.width(8.dp))
                                // Explain is a user-triggered button, not auto shown
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PastelColors.CardPink)
                                        .clickable { onExplain() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Explain", fontSize = 11.sp, color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Corrected text box with Copy button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Correction:", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha = 0.6f))
                                            Spacer(Modifier.height(4.dp))
                                            Text(result.correctedText, fontSize = 15.sp, color = PastelColors.TextMain, fontWeight = FontWeight.Medium)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        // Copy button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PastelColors.CardBlue)
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("corrected", result.correctedText))
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Copy", fontSize = 11.sp, color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Show explanation only when the user asked for it
                            if (actionResult != null) {
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.6f))
                                        .padding(12.dp)
                                ) {
                                    Text(actionResult, fontSize = 13.sp, color = PastelColors.TextMain, lineHeight = 18.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Tone Toggles
                            Text("Rewrite Tone:", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha = 0.6f))
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Improve Vocabulary", "Make Formal", "Make Crisp").forEach { action ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PastelColors.CardBlue)
                                            .clickable { onAction(action) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(action, fontSize = 13.sp, color = PastelColors.TextMain, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            if (isLoadingAction) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = PastelColors.SuccessGreen,
                                    trackColor = Color.White.copy(alpha = 0.5f)
                                )
                            } else {
                                Button(
                                    onClick = { onApplyFix(result.correctedText) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelColors.SuccessGreen),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Apply Correction", color = PastelColors.TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) {
                                    Text("Pause for ${pauseDurationMins}m", color = PastelColors.TextMain.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        is OverlayState.RewritePreview -> {
                            Text("Tone Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PastelColors.TextMain)
                            Spacer(Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            state.newText,
                                            fontSize = 15.sp,
                                            color = PastelColors.TextMain,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        // Copy button for rewrite too
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PastelColors.CardBlue)
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("rewritten", state.newText))
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Copy", fontSize = 11.sp, color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { onAction("Retry") },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelColors.ButtonPink),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = PastelColors.TextMain, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry", color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onApplyFix(state.newText) },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelColors.SuccessGreen),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Apply", color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is OverlayState.Chat -> {
                            var textInput by remember { mutableStateOf("") }
                            val focusRequester = remember { FocusRequester() }
                            val view = LocalView.current
                            var windowFocused by remember { mutableStateOf(false) }

                            // After making window focusable, wait briefly then request focus + show keyboard
                            LaunchedEffect(windowFocused) {
                                if (windowFocused) {
                                    kotlinx.coroutines.delay(80) // Let FLAG_NOT_FOCUSABLE removal take effect
                                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                                    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.showSoftInput(view, 0)
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // Chat History
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(scrollState)
                                        .fillMaxWidth()
                                ) {
                                    if (chatHistory.none { it.role != "system" }) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Text("Ask me anything!", fontSize = 13.sp, color = PastelColors.TextMain.copy(alpha = 0.4f))
                                        }
                                    }
                                    chatHistory.forEach { msg ->
                                        if (msg.role != "system") {
                                            val isUser = msg.role == "user"
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isUser) PastelColors.CardBlue else PastelColors.CardPink)
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Text(msg.content, color = PastelColors.TextMain, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                    if (isLoadingAction) {
                                        Text("typing...", fontSize = 11.sp, color = PastelColors.TextMain.copy(alpha = 0.5f), modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Input row — invisible tap-catcher overlay sits on top of TextField until
                            // window is made focusable; after first tap it disappears and TextField is live
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TextField(
                                        value = textInput,
                                        onValueChange = { textInput = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .focusRequester(focusRequester),
                                        placeholder = {
                                            Text(
                                                if (windowFocused) "Ask something..." else "Tap to type...",
                                                fontSize = 13.sp
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(25.dp),
                                        maxLines = 1
                                    )
                                    // Transparent overlay — intercepts the FIRST tap to activate the window.
                                    // Once windowFocused=true this overlay is removed and TextField works normally.
                                    if (!windowFocused) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable {
                                                    windowFocused = true
                                                    onRequestKeyboardFocus() // Remove FLAG_NOT_FOCUSABLE
                                                }
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(if (!isLoadingAction && textInput.isNotBlank()) PastelColors.SuccessGreen else Color.LightGray)
                                        .clickable(enabled = !isLoadingAction && textInput.isNotBlank()) {
                                            onSendMessage(textInput)
                                            textInput = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("→", color = PastelColors.TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }


                        else -> {}
                    }
                }
            }
        }
    }
}
