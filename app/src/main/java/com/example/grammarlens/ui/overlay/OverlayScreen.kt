package com.example.grammarlens.ui.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grammarlens.network.GrammarCheckResult

@Composable
fun OverlayScreen(
    result: GrammarCheckResult?, 
    isSuccess: Boolean = false, 
    isLoadingAction: Boolean = false,
    actionResult: String? = null,
    onAction: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isSuccess) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF4CAF50),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Looks good!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else if (result != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${result.mistakes.size} mistakes found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.mistakes.map { it.category }.distinct().forEach { category ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (expanded) {
                        result.mistakes.forEach { detail ->
                            Text(
                                "Error: ${detail.mistake}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Fix: ${detail.suggestion}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(
                            "Corrected: ${result.correctedText}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions
                        if (isLoadingAction) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (actionResult != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Result:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(actionResult, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { onAction("Improve Vocabulary") }, modifier = Modifier.weight(1f)) {
                                    Text("Improve Vocab", fontSize = 12.sp)
                                }
                                OutlinedButton(onClick = { onAction("Make Formal") }, modifier = Modifier.weight(1f)) {
                                    Text("Make Formal", fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipText = actionResult ?: result.correctedText
                                val clip = ClipData.newPlainText("Corrected Text", clipText)
                                clipboard.setPrimaryClip(clip)
                                onDismiss()
                            }) {
                                Text("Copy Fix")
                            }
                        }
                    } else {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Details & Fix")
                        }
                    }
                }
            }
        }
    }
}
