package com.example.todo.common.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.todo.common.theme.MustDoColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    trackHeight: Dp = 14.dp,
    thumbRadius: Dp = 9.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidSliderWave")
    
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (-2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase2"
    )

    val coercedValue = value.coerceIn(valueRange)
    val rangeLength = valueRange.endInclusive - valueRange.start
    val fraction = if (rangeLength > 0) (coercedValue - valueRange.start) / rangeLength else 0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val maxPx = constraints.maxWidth.toFloat()
        val thumbRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { thumbRadius.toPx() }
        val trackHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { trackHeight.toPx() }
        
        val startPadding = thumbRadiusPx
        val endPadding = thumbRadiusPx
        val usableWidth = maxPx - startPadding - endPadding

        val thumbCenterRawX = startPadding + fraction * usableWidth

        fun updateValueForX(x: Float) {
            val relativeX = (x - startPadding).coerceIn(0f, usableWidth)
            val newFraction = if (usableWidth > 0) relativeX / usableWidth else 0f
            val newValue = valueRange.start + newFraction * rangeLength
            onValueChange(newValue)
        }

        val primaryColor = MustDoColors.Primary
        val primaryLight = MustDoColors.PrimaryLight.copy(alpha = 0.35f)
        val inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        updateValueForX(offset.x)
                    }
                }
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        updateValueForX(change.position.x)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f
            
            val trackLeft = startPadding
            val trackRight = canvasWidth - endPadding
            val trackTop = centerY - (trackHeightPx / 2f)

            val bgPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = trackLeft,
                        top = trackTop,
                        right = trackRight,
                        bottom = trackTop + trackHeightPx,
                        cornerRadius = CornerRadius(trackHeightPx / 2f)
                    )
                )
            }
            
            drawPath(bgPath, color = inactiveColor)

            if (thumbCenterRawX > trackLeft) {
                val activePath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = trackLeft,
                            top = trackTop,
                            right = thumbCenterRawX,
                            bottom = trackTop + trackHeightPx,
                            topLeftCornerRadius = CornerRadius(trackHeightPx / 2f),
                            bottomLeftCornerRadius = CornerRadius(trackHeightPx / 2f),
                            topRightCornerRadius = CornerRadius(0f),
                            bottomRightCornerRadius = CornerRadius(0f)
                        )
                    )
                }

                clipPath(activePath) {
                    val liquidBrush = Brush.linearGradient(
                        colors = listOf(primaryColor, MustDoColors.PrimaryLight),
                        start = Offset(trackLeft, centerY),
                        end = Offset(thumbCenterRawX, centerY)
                    )
                    
                    val wavePath1 = Path().apply {
                        val waveAmplitude = trackHeightPx * 0.15f
                        val waveFrequency = (2 * PI / (usableWidth * 0.4f)).toFloat()
                        
                        moveTo(trackLeft, trackTop + trackHeightPx)
                        lineTo(trackLeft, trackTop)
                        
                        var x = trackLeft
                        while (x <= thumbCenterRawX) {
                            val relativeX = x - trackLeft
                            val y = trackTop + (trackHeightPx * 0.2f) + sin(relativeX * waveFrequency + wavePhase1) * waveAmplitude
                            lineTo(x, y)
                            x += 4f
                        }
                        
                        lineTo(thumbCenterRawX, trackTop + trackHeightPx)
                        close()
                    }
                    drawPath(wavePath1, color = primaryLight)

                    val wavePath2 = Path().apply {
                        val waveAmplitude = trackHeightPx * 0.20f
                        val waveFrequency = (2 * PI / (usableWidth * 0.3f)).toFloat()
                        
                        moveTo(trackLeft, trackTop + trackHeightPx)
                        lineTo(trackLeft, trackTop)
                        
                        var x = trackLeft
                        while (x <= thumbCenterRawX) {
                            val relativeX = x - trackLeft
                            val y = trackTop + (trackHeightPx * 0.35f) + sin(relativeX * waveFrequency + wavePhase2) * waveAmplitude
                            lineTo(x, y)
                            x += 4f
                        }
                        
                        lineTo(thumbCenterRawX, trackTop + trackHeightPx)
                        close()
                    }
                    drawPath(wavePath2, brush = liquidBrush)
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(thumbCenterRawX, centerY),
                    radius = thumbRadiusPx * 2.8f
                ),
                radius = thumbRadiusPx * 2.8f,
                center = Offset(thumbCenterRawX, centerY)
            )

            drawCircle(
                color = primaryColor,
                radius = thumbRadiusPx,
                center = Offset(thumbCenterRawX, centerY)
            )

            drawCircle(
                color = Color.White,
                radius = thumbRadiusPx * 0.65f,
                center = Offset(thumbCenterRawX, centerY)
            )
            
            drawCircle(
                color = primaryColor,
                radius = thumbRadiusPx * 0.35f,
                center = Offset(thumbCenterRawX, centerY)
            )
        }
    }
}
