package com.example.grammarlens.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grammarlens.ui.components.PastelColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // --- Animation states ---
    var logoVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    var dotsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        logoVisible = true
        delay(350)
        titleVisible = true
        delay(200)
        taglineVisible = true
        delay(300)
        dotsVisible = true
        delay(1400)  // Let loading dots play for a bit
        onFinished()
    }

    // Logo scale + fade
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ), label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(400), label = "logoAlpha"
    )

    // Title slide-up + fade
    val titleOffsetY by animateFloatAsState(
        targetValue = if (titleVisible) 0f else 30f,
        animationSpec = tween(500, easing = EaseOutCubic), label = "titleOffset"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(500), label = "titleAlpha"
    )

    // Tagline slide-up + fade
    val taglineOffsetY by animateFloatAsState(
        targetValue = if (taglineVisible) 0f else 20f,
        animationSpec = tween(400, easing = EaseOutCubic), label = "taglineOffset"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(400), label = "taglineAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFBF5),  // warm cream top
                        Color(0xFFFFF0FA),  // soft pink tint bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // --- Animated Logo Icon ---
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(100.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PastelColors.CardBlue,
                                PastelColors.CardPurple
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "GrammarLens Logo",
                    tint = PastelColors.TextMain,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- App Name ---
            Text(
                text = "GrammarLens",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PastelColors.TextMain,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffsetY.dp)
            )

            Spacer(Modifier.height(8.dp))

            // --- Tagline ---
            Text(
                text = "Write beautifully, every time",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = PastelColors.TextMain.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(taglineAlpha)
                    .offset(y = taglineOffsetY.dp)
            )

            Spacer(Modifier.height(56.dp))

            // --- Animated Loading Dots ---
            AnimatedVisibility(
                visible = dotsVisible,
                enter = fadeIn(tween(300))
            ) {
                LoadingDots()
            }
        }
    }
}

@Composable
fun LoadingDots() {
    val dotCount = 3
    val dotColors = listOf(PastelColors.CardBlue, PastelColors.CardPurple, PastelColors.ButtonPink)

    // Infinite repeating animation
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotColors.forEachIndexed { index, color ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 420,
                        easing = EaseInOutSine,
                        delayMillis = 0
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 140)
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
