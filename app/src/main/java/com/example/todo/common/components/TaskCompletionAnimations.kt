package com.example.todo.common.components

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.todo.core.database.entity.AchievementEntity
import com.example.todo.core.database.entity.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─── Accessibility Utility ──────────────────────────────────────────────────

@Composable
fun areAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        } catch (e: Exception) {
            true
        }
    }
}

// ─── Completion Animation State ─────────────────────────────────────────────

/**
 * Holds the animation phase state for a single task's completion animation.
 * Passed from TaskCard down to its children so the whole card can react.
 */
class CompletionAnimationController {
    var isAnimating by mutableStateOf(false)
        private set
    var phase by mutableStateOf(0)
        private set

    // Phase 1: checkbox spring
    // Phase 2: checkbox morph (fill + checkmark)
    // Phase 3: card success (elevation + tint + scale)
    // Phase 4: text strikethrough + fade
    // Phase 5: success pulse

    fun startAnimation() {
        isAnimating = true
        phase = 1
    }

    fun advancePhase(next: Int) {
        phase = next
    }

    fun complete() {
        isAnimating = false
        phase = 0
    }
}

@Composable
fun rememberCompletionAnimationController(): CompletionAnimationController {
    return remember { CompletionAnimationController() }
}

// ─── Animated Completion Checkbox ───────────────────────────────────────────

@Composable
fun AnimatedCompletionCheckbox(
    isCompleted: Boolean,
    priority: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    animationController: CompletionAnimationController? = null
) {
    val haptic = LocalHapticFeedback.current
    val animationsEnabled = areAnimationsEnabled()
    val coroutineScope = rememberCoroutineScope()

    val priorityColor = when (priority) {
        Priority.LOW.name -> MustDoColors.PriorityLow
        Priority.MEDIUM.name -> MustDoColors.PriorityMedium
        Priority.HIGH.name -> MustDoColors.PriorityHigh
        Priority.URGENT.name -> MustDoColors.PriorityUrgent
        else -> MustDoColors.Primary
    }

    // ── Phase 1: Spring scale ───────────────────────────────────────────
    val scaleAnim = remember { Animatable(1f) }

    // ── Phase 2: Checkbox morph ─────────────────────────────────────────
    val fillSweep = remember { Animatable(0f) }        // 0→360 clockwise fill
    val checkmarkProgress = remember { Animatable(0f) } // 0→1 checkmark draw
    val glowAlpha = remember { Animatable(0f) }         // brief glow

    // ── Phase 5: Success pulse ──────────────────────────────────────────
    val pulseScale = remember { Animatable(1f) }
    val pulseAlpha = remember { Animatable(0f) }

    // Whether the checkbox visually shows as "filled" (either completed or animating completion)
    val showFilled = isCompleted && (animationController == null || animationController.phase >= 2 || !animationController.isAnimating)

    // When already completed (no animation running), show full state instantly
    val staticFillSweep = if (showFilled && !animationController?.isAnimating.let { it == true }) 360f else fillSweep.value
    val staticCheckmark = if (showFilled && !animationController?.isAnimating.let { it == true }) 1f else checkmarkProgress.value

    fun triggerCompletionAnimation() {
        if (!animationsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
            return
        }

        coroutineScope.launch {
            // Phase 1: Spring scale + haptic
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            animationController?.startAnimation()

            scaleAnim.animateTo(
                targetValue = 0.9f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh)
            )
            scaleAnim.animateTo(
                targetValue = 1.05f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
            )

            // Trigger the actual state change
            onToggle()

            // Phase 2: Checkbox morph
            animationController?.advancePhase(2)
            launch {
                fillSweep.snapTo(0f)
                fillSweep.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
            }
            delay(100)
            launch {
                checkmarkProgress.snapTo(0f)
                checkmarkProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                )
            }
            delay(50)
            // Glow
            launch {
                glowAlpha.snapTo(0f)
                glowAlpha.animateTo(0.4f, animationSpec = tween(80))
                glowAlpha.animateTo(0f, animationSpec = tween(150))
            }

            // Phase 3: Card success (handled by TaskCard reading animationController.phase)
            delay(100)
            animationController?.advancePhase(3)

            // Phase 4: Text animation
            delay(150)
            animationController?.advancePhase(4)

            // Phase 5: Success pulse
            launch {
                animationController?.advancePhase(5)
                pulseAlpha.snapTo(0.20f)
                pulseScale.snapTo(1f)
                launch {
                    pulseScale.animateTo(
                        targetValue = 1.8f,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    pulseAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(350, easing = LinearEasing)
                    )
                }
            }

            delay(400)
            animationController?.complete()
        }
    }

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Success pulse circle (behind everything) ────────────────────
        if (pulseAlpha.value > 0f) {
            Canvas(
                modifier = Modifier
                    .size(26.dp)
                    .scale(pulseScale.value)
            ) {
                drawCircle(
                    color = priorityColor.copy(alpha = pulseAlpha.value),
                    radius = size.minDimension / 2f
                )
            }
        }

        // ── Glow (behind checkbox) ──────────────────────────────────────
        if (glowAlpha.value > 0f) {
            Canvas(modifier = Modifier.size(34.dp)) {
                drawCircle(
                    color = priorityColor.copy(alpha = glowAlpha.value),
                    radius = size.minDimension / 2f
                )
            }
        }

        // ── Checkbox ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(26.dp)
                .scale(scaleAnim.value)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!isCompleted) {
                            triggerCompletionAnimation()
                        } else {
                            onToggle()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 2.dp.toPx()
                val radius = size.minDimension / 2f

                if (isCompleted && !animationController?.isAnimating.let { it == true }) {
                    // Already completed — draw filled circle + checkmark
                    drawCircle(color = priorityColor, radius = radius)
                    drawCheckmark(priorityColor = Color.White, progress = 1f, size = size)
                } else if (animationController?.isAnimating == true && animationController.phase >= 2) {
                    // Animating — draw partial fill arc + partial checkmark
                    // Border
                    drawCircle(
                        color = priorityColor.copy(alpha = 0.3f),
                        radius = radius - strokeW / 2f,
                        style = Stroke(width = strokeW)
                    )
                    // Clockwise fill
                    val currentSweep = fillSweep.value
                    if (currentSweep > 0f) {
                        drawArc(
                            color = priorityColor,
                            startAngle = -90f,
                            sweepAngle = currentSweep,
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = size
                        )
                    }
                    // Checkmark
                    val currentCheck = checkmarkProgress.value
                    if (currentCheck > 0f) {
                        drawCheckmark(
                            priorityColor = Color.White,
                            progress = currentCheck,
                            size = size
                        )
                    }
                } else {
                    // Uncompleted — empty circle with border
                    drawCircle(
                        color = priorityColor.copy(alpha = 0.5f),
                        radius = radius - strokeW / 2f,
                        style = Stroke(width = strokeW)
                    )
                }
            }
        }
    }
}

/**
 * Draws a checkmark path with animated progress (0–1).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheckmark(
    priorityColor: Color,
    progress: Float,
    size: Size
) {
    val strokeWidth = 2.dp.toPx()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val scale = size.width / 26f // normalized to 26dp checkbox

    // Checkmark points (relative to center)
    val p1x = cx - 5f * scale
    val p1y = cy + 0.5f * scale
    val p2x = cx - 1.5f * scale
    val p2y = cy + 4f * scale
    val p3x = cx + 5.5f * scale
    val p3y = cy - 3.5f * scale

    // Total path length (approximate)
    val seg1Len = kotlin.math.sqrt(((p2x - p1x) * (p2x - p1x) + (p2y - p1y) * (p2y - p1y)).toDouble()).toFloat()
    val seg2Len = kotlin.math.sqrt(((p3x - p2x) * (p3x - p2x) + (p3y - p2y) * (p3y - p2y)).toDouble()).toFloat()
    val totalLen = seg1Len + seg2Len

    val drawnLen = progress * totalLen

    if (drawnLen <= 0f) return

    val path = androidx.compose.ui.graphics.Path()
    path.moveTo(p1x, p1y)

    if (drawnLen <= seg1Len) {
        // Partial first segment
        val t = drawnLen / seg1Len
        path.lineTo(p1x + (p2x - p1x) * t, p1y + (p2y - p1y) * t)
    } else {
        // Full first segment + partial second
        path.lineTo(p2x, p2y)
        val remaining = drawnLen - seg1Len
        val t = (remaining / seg2Len).coerceAtMost(1f)
        path.lineTo(p2x + (p3x - p2x) * t, p2y + (p3y - p2y) * t)
    }

    drawPath(
        path = path,
        color = priorityColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

// ─── XP Reward Chip ─────────────────────────────────────────────────────────

@Composable
fun XpRewardChip(
    xpAmount: Int,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = areAnimationsEnabled()
    
    val offsetAnim = remember { Animatable(12f) } // start 12dp down
    val alphaAnim = remember { Animatable(0f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            if (animationsEnabled) {
                // Quick spring slide up + fade in
                launch {
                    offsetAnim.animateTo(
                        targetValue = -8f, // slide up past target to -8dp
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
                    )
                }
                launch {
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(150, easing = LinearOutSlowInEasing)
                    )
                }
                
                delay(600) // float duration
                
                // Slow fade out + slide up further
                launch {
                    offsetAnim.animateTo(
                        targetValue = -24f,
                        animationSpec = tween(350, easing = FastOutLinearInEasing)
                    )
                }
                launch {
                    alphaAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    )
                }
            } else {
                alphaAnim.snapTo(1f)
                offsetAnim.snapTo(-8f)
                delay(500)
                alphaAnim.snapTo(0f)
            }
            onDismiss()
        } else {
            alphaAnim.snapTo(0f)
            offsetAnim.snapTo(12f)
        }
    }

    if (alphaAnim.value > 0f) {
        Box(
            modifier = modifier
                .offset(y = offsetAnim.value.dp)
                .graphicsLayer { alpha = alphaAnim.value }
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MustDoColors.Primary.copy(alpha = 0.18f),
                contentColor = MustDoColors.Primary,
                tonalElevation = 3.dp,
                shadowElevation = 1.dp
            ) {
                Text(
                    text = "+$xpAmount XP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── Streak Banner ──────────────────────────────────────────────────────────

@Composable
fun StreakBanner(
    streakCount: Int,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(2000L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MustDoColors.Warning.copy(alpha = 0.15f),
            contentColor = MustDoColors.Warning,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🔥", fontSize = 18.sp)
                Text(
                    text = "$streakCount Day Streak",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Achievement Unlock Bottom Sheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementUnlockSheet(
    achievement: AchievementEntity?,
    onDismiss: () -> Unit,
    xpReward: Int = 0
) {
    if (achievement == null) return

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(achievement) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Achievement icon with glow
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft glow behind
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = MustDoColors.Primary.copy(alpha = 0.12f),
                        radius = size.minDimension / 2f
                    )
                }
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MustDoColors.Primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            // "Achievement Unlocked" label
            Text(
                text = "Achievement Unlocked!",
                style = MaterialTheme.typography.labelMedium,
                color = MustDoColors.Primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            // Achievement name
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Description
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // XP reward chip
            if (xpReward > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MustDoColors.Success.copy(alpha = 0.12f),
                    contentColor = MustDoColors.Success
                ) {
                    Text(
                        text = "+$xpReward XP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dismiss button
            FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MustDoColors.Primary.copy(alpha = 0.12f),
                    contentColor = MustDoColors.Primary
                )
            ) {
                Text("Continue", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun TaskCompletionAnimationsPreview() {
    MustDoTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakBanner(
                streakCount = 5,
                visible = true,
                onDismiss = {}
            )
        }
    }
}

