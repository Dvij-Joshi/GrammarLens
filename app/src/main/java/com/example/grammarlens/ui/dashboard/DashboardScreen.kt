package com.example.grammarlens.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onOpenSettings: () -> Unit,
    hasPermissions: Boolean
) {
    val totalChecks by viewModel.totalChecksCount.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val topCategories by viewModel.topMistakeCategories.collectAsState()
    val recentMistakes by viewModel.recentMistakes.collectAsState()
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentApiUrl by viewModel.apiUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GrammarLens", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Warning
            if (!hasPermissions) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Permissions Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please enable Accessibility and Overlay permissions for GrammarLens to work.", color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onOpenSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Grant Permissions")
                            }
                        }
                    }
                }
            }

            // Settings Section
            item {
                Text("API Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var urlInput by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
                        var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }

                        Text("API Endpoint Link", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Groq API Key", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("gsk_...") },
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.saveApiSettings(keyInput, urlInput) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Settings")
                        }
                    }
                }
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Total Checks",
                        value = totalChecks.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Current Streak",
                        value = "$currentStreak days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Top Mistakes Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top 3 Mistake Patterns", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (topCategories.isEmpty()) {
                            Text("No data yet. Keep typing!", color = Color.Gray)
                        } else {
                            val maxCount = topCategories.maxOf { it.second }.coerceAtLeast(1)
                            topCategories.forEach { (category, count) ->
                                var animationPlayed by remember { mutableStateOf(false) }
                                val animatedWidth by animateFloatAsState(
                                    targetValue = if (animationPlayed) count.toFloat() / maxCount else 0f,
                                    animationSpec = tween(1000)
                                )

                                LaunchedEffect(Unit) {
                                    animationPlayed = true
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(category, fontWeight = FontWeight.Medium)
                                        Text("${count}", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedWidth)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Recent Corrections
            item {
                Text("Recent Corrections", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (recentMistakes.isEmpty()) {
                item {
                    Text("No corrections yet.", color = Color.Gray)
                }
            } else {
                items(recentMistakes) { mistake ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Original: ${mistake.originalText}", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fixed: ${mistake.correctedText}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
