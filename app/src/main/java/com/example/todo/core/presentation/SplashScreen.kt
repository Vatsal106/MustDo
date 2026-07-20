package com.example.todo.core.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation States
    var startAnimation by remember { mutableStateOf(false) }

    val transition = updateTransition(targetState = startAnimation, label = "SplashTransition")

    // Circle Arc draw animation
    val circleProgress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800, easing = LinearOutSlowInEasing) },
        label = "CircleProgress"
    ) { state ->
        if (state) 1f else 0f
    }

    // Checkmark draw animation (starts slightly after circle starts)
    val checkmarkProgress by transition.animateFloat(
        transitionSpec = { 
            keyframes {
                durationMillis = 1200
                0f at 400 // start after 400ms
                1f at 1200 with FastOutSlowInEasing
            }
        },
        label = "CheckmarkProgress"
    ) { state ->
        if (state) 1f else 0f
    }

    // Logo scale and rotation
    val logoScale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "LogoScale"
    ) { state ->
        if (state) 1.1f else 0.5f
    }

    // Text Alpha and slide up
    val textAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800, delayMillis = 600, easing = EaseOutCubic) },
        label = "TextAlpha"
    ) { state ->
        if (state) 1f else 0f
    }

    val textTranslationY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 800, delayMillis = 600, easing = EaseOutCubic) },
        label = "TextTranslationY"
    ) { state ->
        if (state) 0.dp else 40.dp
    }

    // Start animation on mount
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200) // Total splash duration
        onSplashFinished()
    }

    val isDark = MaterialTheme.colorScheme.background == MustDoColors.DarkBackground
    val textPrimaryColor = if (isDark) MustDoColors.DarkTextPrimary else MustDoColors.LightTextPrimary
    val textSecondaryColor = if (isDark) MustDoColors.DarkTextSecondary else MustDoColors.LightTextSecondary

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(MustDoColors.DarkBackground, MustDoColors.DarkSecondaryBackground)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(MustDoColors.LightBackground, MustDoColors.LightSurfaceVariant)
        )
    }

    val glowBrush = if (isDark) {
        Brush.radialGradient(
            colors = listOf(MustDoColors.Primary.copy(alpha = 0.20f), Color.Transparent),
            radius = 700f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(MustDoColors.Primary.copy(alpha = 0.08f), Color.Transparent),
            radius = 700f
        )
    }

    val logoGradient = Brush.linearGradient(
        colors = listOf(
            MustDoColors.Primary,
            MustDoColors.PrimaryLight,
            MustDoColors.AIAccent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = glowBrush)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Custom Canvas Logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer(
                        scaleX = logoScale,
                        scaleY = logoScale
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(MustDoColors.Primary.copy(alpha = 0.12f), MustDoColors.PrimaryLight.copy(alpha = 0.02f))
                            } else {
                                listOf(MustDoColors.Primary.copy(alpha = 0.05f), MustDoColors.PrimaryLight.copy(alpha = 0.01f))
                            }
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            } else {
                                listOf(MustDoColors.Primary.copy(alpha = 0.2f), MustDoColors.PrimaryLight.copy(alpha = 0.05f))
                            }
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(y = (-2).dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw outer animated circle
                    drawArc(
                        brush = logoGradient,
                        startAngle = -90f,
                        sweepAngle = 360f * circleProgress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw animated checkmark
                    if (checkmarkProgress > 0f) {
                        val checkmarkPath = Path().apply {
                            moveTo(width * 0.28f, height * 0.5f)
                            lineTo(width * 0.45f, height * 0.66f)
                            lineTo(width * 0.72f, height * 0.34f)
                        }

                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(checkmarkPath, false)
                        val partialPath = Path()
                        pathMeasure.getSegment(0f, pathMeasure.length * checkmarkProgress, partialPath, true)

                        drawPath(
                            path = partialPath,
                            brush = logoGradient,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Animated Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textTranslationY)
                    .graphicsLayer(alpha = textAlpha)
            ) {
                Text(
                    text = "MustDo",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = textPrimaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Get things done, elegantly.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    color = textSecondaryColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    MustDoTheme(darkTheme = true) {
        SplashScreen(onSplashFinished = {})
    }
}
