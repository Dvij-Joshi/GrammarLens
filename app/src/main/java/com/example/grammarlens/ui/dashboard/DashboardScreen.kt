package com.example.grammarlens.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grammarlens.data.database.MistakeEntity
import com.example.grammarlens.ui.components.NeuColors
import com.example.grammarlens.ui.components.NeumorphicCard
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
    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grammar Coach", fontWeight = FontWeight.Bold, color = NeuColors.TextMain) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeuColors.Background,
                    titleContentColor = NeuColors.TextMain
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NeuColors.Background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (!hasPermissions) {
                item { PermissionWarningCard(onOpenSettings) }
            }

            // 0. Master Toggle
            item {
                val effectiveActive = isServiceEnabled && hasPermissions
                
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = when {
                                    !hasPermissions -> "System Permission Missing"
                                    isServiceEnabled -> "GrammarLens is Active"
                                    else -> "GrammarLens is Paused"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (effectiveActive) NeuColors.Primary else NeuColors.TextMain
                            )
                            Text(
                                if (!hasPermissions) "Grant permissions to activate" else "Auto-checks grammar while typing",
                                fontSize = 12.sp,
                                color = NeuColors.TextMain.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = effectiveActive,
                            onCheckedChange = { if (hasPermissions) viewModel.toggleServiceEnabled(it) else onOpenSettings() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeuColors.Primary,
                                uncheckedThumbColor = NeuColors.DarkShadow,
                                uncheckedTrackColor = NeuColors.Background
                            )
                        )
                    }
                }
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
            item { Text("Mistake Breakdown", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NeuColors.TextMain, modifier = Modifier.padding(top = 8.dp)) }
            
            if (categoryBreakdown.isEmpty()) {
                item { Text("No mistakes recorded yet. Keep typing!", color = NeuColors.TextMain.copy(alpha = 0.6f)) }
            } else {
                items(categoryBreakdown) { category ->
                    CategoryExpandableCard(category, onDelete = { id -> viewModel.deleteMistake(id) })
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
    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53E3E))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Permissions Required", fontWeight = FontWeight.Bold, color = Color(0xFFE53E3E))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please enable Accessibility and Overlay permissions for GrammarLens to work.", color = NeuColors.TextMain)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
            ) { Text("Grant Permissions") }
        }
    }
}

@Composable
fun TrendChartCard(trendData: List<DailyTrend>) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mistake Rate Trend (Last 14 Days)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeuColors.TextMain)
            Text("Mistakes ÷ Total Checks", fontSize = 12.sp, color = NeuColors.TextMain.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(16.dp))

            val maxRate = trendData.maxOfOrNull { it.mistakeRate }?.coerceAtLeast(0.1f) ?: 0.1f
            val primaryColor = NeuColors.Primary

            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val width = size.width
                val height = size.height
                val pointWidth = width / max(1, trendData.size - 1)

                val path = Path()
                trendData.forEachIndexed { index, data ->
                    val x = index * pointWidth
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
    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFE53E3E))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Most Repeated Mistake", fontWeight = FontWeight.Bold, color = Color(0xFFE53E3E))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("You've made this exact mistake ${mistake.count} times:", fontSize = 14.sp, color = NeuColors.TextMain)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeuColors.Background) // Can add inner shadow here if desired
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("« ${mistake.originalText} »", color = Color(0xFFE53E3E), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("→ ${mistake.correctedText}", color = Color(0xFF38A169), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HourlyHeatmapCard(heatmap: List<Float>) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Time of Day Pattern", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeuColors.TextMain)
            Text("When do you make the most mistakes?", fontSize = 12.sp, color = NeuColors.TextMain.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = heatmap.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val primaryColor = NeuColors.Primary

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
                                color = NeuColors.TextMain.copy(alpha = 0.7f),
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
fun CategoryExpandableCard(detail: CategoryDetail, onDelete: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    NeumorphicCard(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.category, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeuColors.TextMain)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${String.format("%.1f", detail.percentage)}% of total (${detail.count})", fontSize = 13.sp, color = NeuColors.TextMain.copy(alpha = 0.7f))
                        Spacer(Modifier.width(8.dp))
                        
                        // Improvement Arrow
                        when (detail.improvement) {
                            ImprovementStatus.IMPROVING -> {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Improving", tint = Color(0xFF38A169), modifier = Modifier.size(16.dp))
                                Text("Improving", fontSize = 12.sp, color = Color(0xFF38A169), fontWeight = FontWeight.Bold)
                            }
                            ImprovementStatus.WORSENING -> {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Worsening", tint = Color(0xFFE53E3E), modifier = Modifier.size(16.dp))
                                Text("Worsening", fontSize = 12.sp, color = Color(0xFFE53E3E), fontWeight = FontWeight.Bold)
                            }
                            else -> {}
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = NeuColors.TextMain
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = NeuColors.DarkShadow.copy(alpha = 0.2f))
                    Spacer(Modifier.height(12.dp))
                    Text("Recent Examples:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeuColors.Primary)
                    Spacer(Modifier.height(8.dp))
                    
                    detail.recentExamples.forEach { example ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("« ${example.originalText} »", color = NeuColors.TextMain, fontSize = 13.sp)
                                Text("→ ${example.correctedText}", color = Color(0xFF38A169), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            IconButton(onClick = { onDelete(example.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53E3E).copy(alpha = 0.7f))
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
    NeumorphicCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 14.sp, color = NeuColors.TextMain.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeuColors.Primary)
        }
    }
}

@Composable
fun ApiSettingsCard(currentApiKey: String, currentApiUrl: String, viewModel: DashboardViewModel) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            var urlInput by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
            var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }

            Text("API Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeuColors.TextMain)
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("API Endpoint Link", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NeuColors.TextMain)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = urlInput, onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = NeuColors.DarkShadow.copy(alpha = 0.2f),
                    focusedBorderColor = NeuColors.Primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Groq API Key", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NeuColors.TextMain)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = keyInput, onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("gsk_...") }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = NeuColors.DarkShadow.copy(alpha = 0.2f),
                    focusedBorderColor = NeuColors.Primary
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Neumorphic Button
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeuColors.Primary)
                    .clickable { viewModel.saveApiSettings(keyInput, urlInput) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
