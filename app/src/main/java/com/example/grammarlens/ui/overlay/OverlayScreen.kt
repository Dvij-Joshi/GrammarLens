package com.example.grammarlens.ui.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grammarlens.network.GrammarCheckResult
import com.example.grammarlens.ui.components.PastelColors
import com.example.grammarlens.overlay.OverlayState

@Composable
fun OverlayScreen(
    state: OverlayState,
    isLoadingAction: Boolean = false,
    actionResult: String? = null,
    pauseDurationMins: Int = 15,
    onApplyFix: (String) -> Unit = {},
    onAction: (String) -> Unit = {},
    onExplain: () -> Unit = {},
    onPause: () -> Unit = {},
    onExpand: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (state is OverlayState.Hidden) return

    if (state is OverlayState.IdleBubble) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PastelColors.CardBlue)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onExpand() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = "GrammarLens", tint = PastelColors.TextMain, modifier = Modifier.size(28.dp))
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
                    .border(1.dp, Color.White.copy(alpha=0.5f), RoundedCornerShape(32.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PastelColors.TextMain, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "GrammarLens",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = PastelColors.TextMain
                            )
                        }
                        Row {
                            IconButton(onClick = { /* Go to Chat - Phase 3 */ }, modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha=0.3f))) {
                                Icon(Icons.Default.Face, contentDescription = "Chat", tint = PastelColors.TextMain, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha=0.3f))) {
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
                            
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp)) {
                                Column {
                                    Text("Correction:", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
                                    Spacer(Modifier.height(4.dp))
                                    Text(result.correctedText, fontSize = 15.sp, color = PastelColors.TextMain, fontWeight = FontWeight.Medium)
                                }
                            }

                            if (actionResult != null) {
                                Spacer(Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha=0.6f)).padding(12.dp)) {
                                    Text(actionResult, fontSize = 13.sp, color = PastelColors.TextMain, lineHeight = 18.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            
                            // Tone Toggles
                            Text("Rewrite Tone:", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
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
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PastelColors.SuccessGreen, trackColor = Color.White.copy(alpha = 0.5f))
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
                                    Text("Pause for ${pauseDurationMins}m", color = PastelColors.TextMain.copy(alpha=0.6f), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        is OverlayState.RewritePreview -> {
                            Text("Tone Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PastelColors.TextMain)
                            Spacer(Modifier.height(12.dp))
                            
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp)) {
                                Text(state.newText, fontSize = 15.sp, color = PastelColors.TextMain, fontWeight = FontWeight.Medium)
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
                        
                        else -> {
                            // Chat mode will go here
                        }
                    }
                }
            }
        }
    }
}
