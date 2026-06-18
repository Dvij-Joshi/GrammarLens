package com.example.grammarlens.ui.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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

@Composable
fun OverlayScreen(
    result: GrammarCheckResult?,
    isSuccess: Boolean = false,
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
    if (isSuccess) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(PastelColors.SuccessGreen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PastelColors.TextMain, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Looks good!", color = PastelColors.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        return
    }

    if (result == null) return

    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(result, isExpanded) {
        if (!isExpanded) {
            kotlinx.coroutines.delay(3000)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier.padding(end = 16.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PastelColors.ButtonPink)
                        .clickable {
                            isExpanded = true
                            onExpand()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Grammar mistake",
                        tint = PastelColors.TextMain,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${result.mistakes.size}",
                        fontSize = 10.sp,
                        color = PastelColors.ButtonPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PastelColors.CardBlue)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    result.mistakes.firstOrNull()?.category ?: "Grammar Suggestion",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = PastelColors.TextMain,
                                    lineHeight = 24.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha=0.3f))
                                        .clickable { onExplain() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Explain", fontSize = 10.sp, color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = PastelColors.TextMain.copy(alpha=0.6f), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Change to:",
                        fontSize = 12.sp,
                        color = PastelColors.TextMain.copy(alpha=0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        result.correctedText,
                        fontSize = 14.sp,
                        color = PastelColors.TextMain,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(16.dp))
                    
                    if (actionResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha=0.8f))
                                .padding(12.dp)
                        ) {
                            Text(actionResult, fontSize = 13.sp, color = PastelColors.TextMain, lineHeight = 18.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    // Tone Adjustment Actions
                    Text("Rewrite Tone:", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Improve Vocabulary", "Make Formal", "Make Crisp & Educated").forEach { action ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PastelColors.CardBlue.copy(alpha=0.2f))
                                    .clickable { onAction(action) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(action, fontSize = 12.sp, color = PastelColors.CardBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    if (isLoadingAction) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PastelColors.SuccessGreen, trackColor = Color.White.copy(alpha = 0.5f))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PastelColors.SuccessGreen)
                                .clickable { onApplyFix(result.correctedText) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Apply Correction ->", color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPause() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Pause for ${pauseDurationMins}m", color = PastelColors.TextMain.copy(alpha=0.6f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
