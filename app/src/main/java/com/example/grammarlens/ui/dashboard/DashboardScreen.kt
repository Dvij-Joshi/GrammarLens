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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val pauseUntil by viewModel.pauseUntil.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }
    var showTestingSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PastelColors.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTestingSheet = true },
                containerColor = PastelColors.CardPurple,
                contentColor = PastelColors.TextMain,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Test GrammarLens")
            }
        },
        bottomBar = { BottomNavBar(currentTab = currentTab, onTabSelected = { currentTab = it }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentTab) {
                0 -> DashboardTab(
                    isServiceEnabled = isServiceEnabled,
                    hasPermissions = hasPermissions,
                    trendData = trendData,
                    categoryBreakdown = categoryBreakdown,
                    currentApiKey = currentApiKey,
                    currentApiUrl = currentApiUrl,
                    viewModel = viewModel,
                    onOpenSettings = onOpenSettings,
                    pauseUntil = pauseUntil,
                    onResume = { viewModel.setPauseUntil(0L) }
                )
                1 -> ErrorsTab(
                    trendData = trendData,
                    categoryBreakdown = categoryBreakdown,
                    onDeleteMistake = { viewModel.deleteMistake(it) }
                )
                2 -> SettingsTab(
                    viewModel = viewModel,
                    currentApiKey = currentApiKey,
                    currentApiUrl = currentApiUrl
                )
                // Tab 3 (Profile) can be stubbed or fallback to Dashboard for now
                else -> DashboardTab(
                    isServiceEnabled = isServiceEnabled,
                    hasPermissions = hasPermissions,
                    trendData = trendData,
                    categoryBreakdown = categoryBreakdown,
                    currentApiKey = currentApiKey,
                    currentApiUrl = currentApiUrl,
                    viewModel = viewModel,
                    onOpenSettings = onOpenSettings,
                    pauseUntil = pauseUntil,
                    onResume = { viewModel.setPauseUntil(0L) }
                )
            }
        }
    }

    if (showTestingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTestingSheet = false },
            containerColor = PastelColors.Background
        ) {
            var testingText by remember { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Test GrammarLens",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = PastelColors.TextMain
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Type a sentence with some mistakes below. Our accessibility service will detect it and show the popup automatically!",
                    fontSize = 14.sp,
                    color = PastelColors.TextMain.copy(alpha=0.6f)
                )
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = testingText,
                    onValueChange = { testingText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("e.g. He go to the store yesterday...", color = PastelColors.TextMain.copy(alpha=0.3f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PastelColors.ToggleOn,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = PastelColors.TextMain,
                        unfocusedTextColor = PastelColors.TextMain
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DashboardTab(
    isServiceEnabled: Boolean,
    hasPermissions: Boolean,
    trendData: List<DailyTrend>,
    categoryBreakdown: List<CategoryDetail>,
    currentApiKey: String,
    currentApiUrl: String,
    viewModel: DashboardViewModel,
    onOpenSettings: () -> Unit,
    pauseUntil: Long = 0L,
    onResume: () -> Unit = {}
) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
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

            // Paused banner
            if (pauseUntil > System.currentTimeMillis()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFF0F0))
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("⏸ GrammarLens Paused", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 14.sp)
                                Text("Tap Resume to turn corrections back on", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha = 0.6f))
                            }
                            Button(
                                onClick = onResume,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Resume", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
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

            item { Spacer(Modifier.height(32.dp)) }
        }
}

@Composable
fun SettingsTab(
    viewModel: DashboardViewModel,
    currentApiKey: String,
    currentApiUrl: String
) {
    val pauseDurationMins by viewModel.pauseDurationMins.collectAsState()
    val pauseUntil by viewModel.pauseUntil.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        
        item {
            Text(
                "Settings",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = PastelColors.TextMain
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Configure your GrammarLens experience",
                fontSize = 14.sp,
                color = PastelColors.TextMain.copy(alpha=0.6f)
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            PauseConfigurationCard(
                pauseDurationMins = pauseDurationMins,
                pauseUntil = pauseUntil,
                onDurationChange = { viewModel.setPauseDurationMins(it) },
                onPauseToggle = {
                    val now = System.currentTimeMillis()
                    if (pauseUntil > now) {
                        viewModel.setPauseUntil(0L) // Cancel pause
                    } else {
                        viewModel.setPauseUntil(now + (pauseDurationMins * 60 * 1000L)) // Start pause
                    }
                }
            )
        }

        item { ApiSettingsCard(currentApiKey, currentApiUrl, viewModel) }
        
        item {
            LanguageSelectionCard(
                selectedLanguage = viewModel.selectedLanguage.collectAsState().value,
                onLanguageChange = { viewModel.setSelectedLanguage(it) }
            )
        }
        
        item {
            CustomDictionaryCard(
                ignoredWords = viewModel.ignoredWords.collectAsState().value,
                onAddWord = { viewModel.addIgnoredWord(it) },
                onRemoveWord = { viewModel.removeIgnoredWord(it) }
            )
        }
        
        item {
            AppBlacklistCard(
                blacklistedApps = viewModel.blacklistedApps.collectAsState().value,
                installedApps = viewModel.installedApps.collectAsState().value,
                onAddApp = { viewModel.addBlacklistedApp(it) },
                onRemoveApp = { viewModel.removeBlacklistedApp(it) }
            )
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun PauseConfigurationCard(
    pauseDurationMins: Int,
    pauseUntil: Long,
    onDurationChange: (Int) -> Unit,
    onPauseToggle: () -> Unit
) {
    // Tick every second to update the countdown display
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pauseUntil) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val isPaused = pauseUntil > now
    val remainingMs = if (isPaused) (pauseUntil - now).coerceAtLeast(0L) else 0L
    val remainingMins = (remainingMs / 60000).toInt()
    val remainingSecs = ((remainingMs % 60000) / 1000).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(if (isPaused) Color(0xFFFFF0F0) else Color.White)
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPaused) "⏸ Paused" else "Quick Pause",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isPaused) Color(0xFFD32F2F) else PastelColors.TextMain
                )
            }
            Spacer(Modifier.height(8.dp))

            if (isPaused) {
                // Paused state — show countdown + Resume button prominently
                Text(
                    "GrammarLens corrections are paused.",
                    fontSize = 14.sp,
                    color = PastelColors.TextMain.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))

                // Countdown chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFE0E0))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Resumes in %02d:%02d".format(remainingMins, remainingSecs),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Resume button — primary action
                Button(
                    onClick = onPauseToggle,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Resume Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                // Normal state — show duration picker + Pause button
                Text(
                    "Set how long to pause corrections from the grammar popup.",
                    fontSize = 14.sp,
                    color = PastelColors.TextMain.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(5, 15, 30, 60).forEach { mins ->
                        val selected = pauseDurationMins == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) PastelColors.CardPurple else PastelColors.Background)
                                .clickable { onDurationChange(mins) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${mins}m",
                                color = if (selected) PastelColors.TextMain else PastelColors.TextMain.copy(alpha = 0.5f),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onPauseToggle,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PastelColors.ToggleOn),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Pause for ${pauseDurationMins}m",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorsTab(
    trendData: List<DailyTrend>,
    categoryBreakdown: List<CategoryDetail>,
    onDeleteMistake: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        
        item {
            Text(
                "Your Errors",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = PastelColors.TextMain
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Review your common mistakes by category",
                fontSize = 14.sp,
                color = PastelColors.TextMain.copy(alpha=0.6f)
            )
            Spacer(Modifier.height(16.dp))
        }

        if (categoryBreakdown.isEmpty()) {
            item {
                Text("No mistakes recorded yet. Keep typing!", color = PastelColors.TextMain.copy(alpha = 0.5f))
            }
        } else {
            item {
                val context = LocalContext.current
                Button(
                    onClick = {
                        com.example.grammarlens.util.ReportCardGenerator.generateAndShare(
                            context, trendData, categoryBreakdown
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PastelColors.CardBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Share Progress Report", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            items(categoryBreakdown) { category ->
                CategoryExpandableCard(category, onDelete = onDeleteMistake)
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun CategoryExpandableCard(detail: CategoryDetail, onDelete: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable { expanded = !expanded }
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.category, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PastelColors.TextMain)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${String.format("%.1f", detail.percentage)}% of total (${detail.count})", fontSize = 13.sp, color = PastelColors.TextMain.copy(alpha = 0.7f))
                        Spacer(Modifier.width(8.dp))
                        
                        when (detail.improvement) {
                            ImprovementStatus.IMPROVING -> {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Improving", tint = PastelColors.SuccessGreen, modifier = Modifier.size(16.dp))
                            }
                            ImprovementStatus.WORSENING -> {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Worsening", tint = PastelColors.ButtonPink, modifier = Modifier.size(16.dp))
                            }
                            else -> {}
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = PastelColors.TextMain.copy(alpha=0.5f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = PastelColors.Background, thickness = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    
                    detail.recentExamples.forEach { example ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("« ${example.originalText} »", color = PastelColors.TextMain.copy(alpha=0.8f), fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    Icon(Icons.Default.CheckCircle, contentDescription=null, tint=PastelColors.SuccessGreen, modifier=Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${example.correctedText}", color = PastelColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { onDelete(example.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PastelColors.ButtonPink)
                            }
                        }
                    }
                }
            }
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
            
            val last7 = trendData.takeLast(7)
            val maxRate = last7.maxOfOrNull { it.mistakeRate }?.coerceAtLeast(0.1f) ?: 0.1f
            
            val chartColors = listOf(
                PastelColors.ChartPink,
                PastelColors.ChartPink.copy(alpha=0.8f),
                PastelColors.ChartPurple,
                PastelColors.ChartPurple.copy(alpha=0.8f),
                PastelColors.ChartBlue,
                PastelColors.ChartGreen,
                PastelColors.ToggleOn
            )
            
            val displayData = if (last7.isEmpty()) {
                List(7) { 0f to "-" }
            } else {
                val padding = List(7 - last7.size) { 0f to "-" }
                padding + last7.map { (it.mistakeRate / maxRate) to it.dateString.take(3) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                displayData.forEachIndexed { i, (value, dayLabel) ->
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
                        Text(dayLabel, fontSize = 10.sp, color = PastelColors.TextMain.copy(alpha=0.5f))
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
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PastelColors.Background)
                    .padding(12.dp)
            ) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Column {
                    Text("How to get a free API key:", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.7f), lineHeight = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1. Visit ", fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.7f), lineHeight = 18.sp)
                        Text(
                            "console.groq.com", 
                            fontSize = 12.sp, 
                            color = PastelColors.CardBlue, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.clickable { uriHandler.openUri("https://console.groq.com") }
                        )
                    }
                    Text(
                        "2. Log in or create an account\n" +
                        "3. Go to 'API Keys' in the menu\n" +
                        "4. Create a new key and paste it below", 
                        fontSize = 12.sp, 
                        color = PastelColors.TextMain.copy(alpha=0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
            
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
fun BottomNavBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NavIcon(Icons.Default.Home, "Dashboard", isSelected = currentTab == 0) { onTabSelected(0) }
            NavIcon(Icons.Default.Warning, "Errors", isSelected = currentTab == 1) { onTabSelected(1) }
            NavIcon(Icons.Default.Settings, "Settings", isSelected = currentTab == 2) { onTabSelected(2) }
            NavIcon(Icons.Default.Person, "Profile", isSelected = currentTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomDictionaryCard(
    ignoredWords: List<String>,
    onAddWord: (String) -> Unit,
    onRemoveWord: (String) -> Unit
) {
    var newWord by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text("Custom Dictionary", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PastelColors.TextMain)
            Spacer(Modifier.height(8.dp))
            Text("Add words or slang that should be ignored by the grammar checker.", fontSize = 14.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newWord, onValueChange = { newWord = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Add word...") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = PastelColors.CardBlue
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { 
                    if(newWord.isNotBlank()) { onAddWord(newWord); newWord = "" }
                }, modifier = Modifier.size(48.dp).clip(CircleShape).background(PastelColors.CardBlue)) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = PastelColors.TextMain)
                }
            }
            
            if (ignoredWords.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ignoredWords.forEach { word ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PastelColors.Background).clickable { onRemoveWord(word) }.padding(horizontal=12.dp, vertical=6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(word, fontSize = 13.sp, color = PastelColors.TextMain)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp), tint = PastelColors.ButtonPink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionCard(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Spanish", "French", "German", "Hindi", "Japanese", "Korean", "Mandarin")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text("Typing Language", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PastelColors.TextMain)
            Spacer(Modifier.height(8.dp))
            Text("Select the language you are typing in so the grammar engine can provide accurate feedback.", fontSize = 14.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedLanguage,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = PastelColors.CardBlue
                    ),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language, color = PastelColors.TextMain) },
                            onClick = {
                                onLanguageChange(language)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppBlacklistCard(
    blacklistedApps: List<String>,
    installedApps: List<DashboardViewModel.AppInfo>,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text("App Blacklist", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PastelColors.TextMain)
            Spacer(Modifier.height(8.dp))
            Text("GrammarLens will NOT check your text when typing in these apps.", fontSize = 14.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { showAppPicker = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PastelColors.CardBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Select Apps to Blacklist", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            if (blacklistedApps.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    blacklistedApps.forEach { app ->
                        val appName = installedApps.find { it.packageName == app }?.name ?: app
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PastelColors.Background).clickable { onRemoveApp(app) }.padding(horizontal=12.dp, vertical=6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(appName, fontSize = 13.sp, color = PastelColors.TextMain)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp), tint = PastelColors.ButtonPink)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        Dialog(onDismissRequest = { showAppPicker = false }) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredApps = installedApps.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true)
            }

            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp)) {
                Column {
                    Text("Select App", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PastelColors.TextMain)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = PastelColors.CardBlue
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddApp(app.packageName)
                                        showAppPicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(app.name, fontWeight = FontWeight.Bold, color = PastelColors.TextMain)
                                    Text(app.packageName, fontSize = 12.sp, color = PastelColors.TextMain.copy(alpha=0.6f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showAppPicker = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel", color = PastelColors.CardBlue)
                    }
                }
            }
        }
    }
}

