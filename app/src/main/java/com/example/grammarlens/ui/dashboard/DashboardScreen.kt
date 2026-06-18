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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grammarlens.data.database.MistakeEntity
import com.example.grammarlens.ui.components.PastelColors
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
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentApiUrl by viewModel.apiUrl.collectAsState()
    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()

    Scaffold(
        containerColor = PastelColors.Background,
        bottomBar = { BottomNavBar() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Top Header & Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Hello, Writer",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = PastelColors.TextMain
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Smart Correction", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isServiceEnabled && hasPermissions,
                            onCheckedChange = { if (hasPermissions) viewModel.toggleServiceEnabled(it) else onOpenSettings() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PastelColors.ToggleOn,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            if (!hasPermissions) {
                item { PermissionWarningCard(onOpenSettings) }
            }

            // Mistake Rate Chart
            item {
                PastelChartCard(trendData = trendData)
            }

            // Mode Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModeCard(
                        title = "Formal Mode",
                        icon = Icons.Default.Edit,
                        bgColor = PastelColors.CardBlue,
                        modifier = Modifier.weight(1f)
                    )
                    ModeCard(
                        title = "Creative Mode",
                        icon = Icons.Default.Star, // Brush/Star equivalent
                        bgColor = PastelColors.CardPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Detailed Breakdown
            item { 
                Text("Detailed Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PastelColors.TextMain) 
            }
            
            if (categoryBreakdown.isEmpty()) {
                item { Text("No mistakes recorded yet.", color = PastelColors.TextMain.copy(alpha = 0.5f)) }
            } else {
                item {
                    val colors = listOf(PastelColors.CardPink, PastelColors.CardPurple, PastelColors.CardMint)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        categoryBreakdown.take(3).forEachIndexed { index, cat ->
                            CategoryMiniCard(
                                title = cat.category,
                                count = cat.count,
                                bgColor = colors[index % colors.size],
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // API Settings at the bottom
            item { ApiSettingsCard(currentApiKey, currentApiUrl, viewModel) }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ModeCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PastelColors.TextMain, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PastelColors.TextMain)
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha=0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = PastelColors.TextMain)
                }
            }
        }
    }
}

@Composable
fun CategoryMiniCard(title: String, count: Int, bgColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PastelColors.TextMain, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelColors.TextMain, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text("$count errors", fontSize = 10.sp, color = PastelColors.TextMain.copy(alpha=0.7f))
        }
    }
}

@Composable
fun PastelChartCard(trendData: List<DailyTrend>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mistake Rate", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PastelColors.TextMain)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Past week", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.5f))
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription=null, tint = PastelColors.TextMain.copy(alpha=0.5f), modifier = Modifier.size(14.dp))
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            val fakeData = listOf(0.4f, 0.8f, 0.7f, 0.5f, 0.9f, 0.6f, 0.3f)
            val chartColors = listOf(
                PastelColors.ChartPink,
                PastelColors.ChartPink.copy(alpha=0.8f),
                PastelColors.ChartPurple,
                PastelColors.ChartPurple.copy(alpha=0.8f),
                PastelColors.ChartBlue,
                PastelColors.ChartGreen,
                PastelColors.ToggleOn
            )
            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                fakeData.forEachIndexed { i, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        val barHeight = 80.dp * value
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(14.dp))
                                .background(chartColors[i % chartColors.size])
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(days[i], fontSize = 10.sp, color = PastelColors.TextMain.copy(alpha=0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun ApiSettingsCard(currentApiKey: String, currentApiUrl: String, viewModel: DashboardViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            var urlInput by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
            var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }

            Text("Groq API Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PastelColors.TextMain)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Groq API Key", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = keyInput, onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter your key") }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = PastelColors.CardBlue
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PastelColors.ButtonPink)
                    .clickable { viewModel.saveApiSettings(keyInput, urlInput) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save Key", color = PastelColors.TextMain, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PermissionWarningCard(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFE5E5))
            .padding(16.dp)
    ) {
        Column {
            Text("Permissions Required", fontWeight = FontWeight.Bold, color = Color(0xFFE53E3E))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Please enable Accessibility and Overlay permissions.", color = PastelColors.TextMain, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
            ) { Text("Grant Permissions") }
        }
    }
}

@Composable
fun BottomNavBar() {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NavIcon(Icons.Default.Home, "Dashboard", isSelected = true)
            NavIcon(Icons.Default.Warning, "Errors", isSelected = false)
            NavIcon(Icons.Default.Settings, "Settings", isSelected = false)
            NavIcon(Icons.Default.Person, "Profile", isSelected = false)
        }
    }
}

@Composable
fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) PastelColors.CardPurple else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = label, 
                tint = if (isSelected) PastelColors.TextMain else PastelColors.TextMain.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label, 
            fontSize = 10.sp, 
            color = if (isSelected) PastelColors.TextMain else PastelColors.TextMain.copy(alpha = 0.4f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

