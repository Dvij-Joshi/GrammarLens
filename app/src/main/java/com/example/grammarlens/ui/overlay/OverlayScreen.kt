package com.example.grammarlens.ui.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.grammarlens.ui.components.NeuColors
import com.example.grammarlens.ui.components.NeumorphicCard
import com.example.grammarlens.ui.components.neumorphic

@Composable
fun OverlayScreen(
    result: GrammarCheckResult?,
    isSuccess: Boolean = false,
    isLoadingAction: Boolean = false,
    actionResult: String? = null,
    onApplyFix: (String) -> Unit = {},
    onAction: (String) -> Unit = {},
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
                    .neumorphic(cornerRadius = 24.dp, elevation = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF38A169))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Looks good!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        return
    }

    if (result == null) return

    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
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
                        .neumorphic(cornerRadius = 26.dp, elevation = 6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53E3E))
                        .clickable {
                            isExpanded = true
                            onExpand()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Grammar mistake",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .neumorphic(cornerRadius = 9.dp, elevation = 2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDD6B20)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${result.mistakes.size}",
                        fontSize = 10.sp,
                        color = Color.White,
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
                    .neumorphic(cornerRadius = 20.dp, elevation = 16.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(NeuColors.Background)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE53E3E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${result.mistakes.size} mistake${if (result.mistakes.size > 1) "s" else ""} found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFE53E3E)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = NeuColors.TextMain, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.mistakes.map { it.category }.distinct().take(3).forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeuColors.Primary.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    cat, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp, color = NeuColors.Primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text("Fix:", fontSize = 12.sp, color = NeuColors.TextMain.copy(alpha = 0.7f))
                    Text(
                        result.correctedText,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuColors.TextMain,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    if (isLoadingAction) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeuColors.Primary, trackColor = NeuColors.DarkShadow.copy(alpha = 0.2f))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NeuButton(
                                text = "Vocab",
                                onClick = { onAction("Improve Vocabulary") },
                                modifier = Modifier.weight(1f)
                            )

                            NeuButton(
                                text = "Formal",
                                onClick = { onAction("Make Formal") },
                                modifier = Modifier.weight(1f)
                            )

                            NeuButton(
                                text = "Crisp",
                                onClick = { onAction("Make Crisp & Educated") },
                                modifier = Modifier.weight(1f)
                            )

                            NeuButton(
                                text = "✓ Apply",
                                onClick = { onApplyFix(result.correctedText) },
                                modifier = Modifier.weight(1f),
                                isPrimary = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeuButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isPrimary: Boolean = false) {
    Box(
        modifier = modifier
            .neumorphic(cornerRadius = 8.dp, elevation = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPrimary) NeuColors.Primary else NeuColors.Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            maxLines = 1,
            color = if (isPrimary) Color.White else NeuColors.TextMain,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium
        )
    }
}
