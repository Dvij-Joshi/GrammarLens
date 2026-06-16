package com.example.grammarlens.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grammarlens.data.database.MistakeEntity
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onOpenSettings: () -> Unit,
    hasPermissions: Boolean
) {
    val totalChecks by viewModel.totalChecksCount.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val trendData by viewModel.trendData.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val hourlyHeatmap by viewModel.hourlyHeatmap.collectAsState()
    val mostRepeatedMistake by viewModel.mostRepeatedMistake.collectAsState()
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentApiUrl by viewModel.apiUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grammar Coach", fontWeight = FontWeight.Bold) },
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
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (!hasPermissions) {
                item { PermissionWarningCard(onOpenSettings) }
            }

            // 1. Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard("Checks Done", totalChecks.toString(), Modifier.weight(1f))
                    StatCard("Current Streak", "$currentStreak days", Modifier.weight(1f))
                }
            }

            // 2. Trend Line Chart (Mistake Rate)
            if (trendData.isNotEmpty()) {
                item { TrendChartCard(trendData) }
            }

            // 3. Most Repeated Mistake
            if (mostRepeatedMistake != null) {
                item { RepeatedMistakeCard(mostRepeatedMistake!!) }
            }

            // 4. Hourly Heatmap
            item { HourlyHeatmapCard(hourlyHeatmap) }

            // 5. Category Breakdown
            item { Text("Mistake Breakdown", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp)) }
            
            if (categoryBreakdown.isEmpty()) {
                item { Text("No mistakes recorded yet. Keep typing!", color = Color.Gray) }
            } else {
                items(categoryBreakdown) { category ->
                    CategoryExpandableCard(category)
                }
            }

            // API Settings at the very bottom
            item { ApiSettingsCard(currentApiKey, currentApiUrl, viewModel) }
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PermissionWarningCard(onOpenSettings: () -> Unit) {
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
            ) { Text("Grant Permissions") }
        }
    }
}

@Composable
fun TrendChartCard(trendData: List<DailyTrend>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mistake Rate Trend (Last 14 Days)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Mistakes ÷ Total Checks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            val maxRate = trendData.maxOfOrNull { it.mistakeRate }?.coerceAtLeast(0.1f) ?: 0.1f
            val primaryColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val width = size.width
                val height = size.height
                val pointWidth = width / max(1, trendData.size - 1)

                val path = Path()
                trendData.forEachIndexed { index, data ->
                    val x = index * pointWidth
                    // Invert Y axis because 0 is at top
                    val y = height - (data.mistakeRate / maxRate * height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 4.dp.toPx())
                )

                trendData.forEachIndexed { index, data ->
                    val x = index * pointWidth
                    val y = height - (data.mistakeRate / maxRate * height)
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }
    }
}

@Composable
fun RepeatedMistakeCard(mistake: RepeatedMistake) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Most Repeated Mistake", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("You've made this exact mistake ${mistake.count} times:", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("« ${mistake.originalText} »", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("→ ${mistake.correctedText}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HourlyHeatmapCard(heatmap: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Time of Day Pattern", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("When do you make the most mistakes?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = heatmap.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val primaryColor = MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                heatmap.forEachIndexed { hour, count ->
                    val heightRatio = count / maxVal
                    val barHeight by animateFloatAsState(targetValue = heightRatio, animationSpec = tween(1000))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .fillMaxHeight(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(primaryColor.copy(alpha = if (count > 0) 1f else 0.1f))
                        )
                        if (hour % 6 == 0) { // Show labels every 6 hours
                            Text(
                                text = "$hour", 
                                fontSize = 8.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryExpandableCard(detail: CategoryDetail) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${String.format("%.1f", detail.percentage)}% of total (${detail.count})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        
                        // Improvement Arrow
                        when (detail.improvement) {
                            ImprovementStatus.IMPROVING -> {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Improving", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Text("Improving", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                            ImprovementStatus.WORSENING -> {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Worsening", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Text("Worsening", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            else -> {}
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Recent Examples:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    
                    detail.recentExamples.forEach { example ->
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text("« ${example.originalText} »", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Text("→ ${example.correctedText}", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

@Composable
fun ApiSettingsCard(currentApiKey: String, currentApiUrl: String, viewModel: DashboardViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            var urlInput by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
            var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }

            Text("API Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("API Endpoint Link", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = urlInput, onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Groq API Key", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = keyInput, onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("gsk_...") }, singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveApiSettings(keyInput, urlInput) },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Save Settings") }
        }
    }
}
