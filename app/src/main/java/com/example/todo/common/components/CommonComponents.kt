package com.example.todo.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.Priority
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskStatus
import com.example.todo.features.tasks.presentation.CompletionEvent
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.ui.input.pointer.pointerInput

// ─── Glassmorphic Card ──────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val cardModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MustDoColors.GlassWhite,
                    MustDoColors.GlassOverlay
                )
            ),
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    MustDoColors.GlassBorder,
                    Color.Transparent
                )
            ),
            shape = shape
        )
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )
        .padding(16.dp)

    Column(modifier = cardModifier, content = content)
}

// ─── Priority Indicator ─────────────────────────────────────────────────────

@Composable
fun PriorityIndicator(
    priority: String,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    val color = when (priority) {
        Priority.LOW.name -> MustDoColors.PriorityLow
        Priority.MEDIUM.name -> MustDoColors.PriorityMedium
        Priority.HIGH.name -> MustDoColors.PriorityHigh
        Priority.URGENT.name -> MustDoColors.PriorityUrgent
        else -> MustDoColors.PriorityMedium
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun PriorityChip(
    priority: String,
    modifier: Modifier = Modifier
) {
    val color = when (priority) {
        Priority.LOW.name -> MustDoColors.PriorityLow
        Priority.MEDIUM.name -> MustDoColors.PriorityMedium
        Priority.HIGH.name -> MustDoColors.PriorityHigh
        Priority.URGENT.name -> MustDoColors.PriorityUrgent
        else -> MustDoColors.PriorityMedium
    }

    val label = priority.lowercase().replaceFirstChar { it.uppercase() }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── Task Card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: TaskEntity,
    onTaskClick: (String) -> Unit,
    onCompleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    categoryColor: String? = null,
    onLongClick: ((String) -> Unit)? = null,
    hasResources: Boolean = false,
    hasNoteBlocks: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
    checklistChecked: Int = 0,
    checklistTotal: Int = 0,
    completionEventFlow: SharedFlow<CompletionEvent>? = null
) {
    val isCompleted = task.status == TaskStatus.COMPLETED.name
    val isOverdue = task.dueDateMillis != null &&
            task.dueDateMillis < System.currentTimeMillis() &&
            !isCompleted

    val animationsEnabled = areAnimationsEnabled()
    val animController = rememberCompletionAnimationController()

    // Local XP float animation state
    var localXpAmount by remember { mutableIntStateOf(0) }
    var showLocalXp by remember { mutableStateOf(false) }

    LaunchedEffect(completionEventFlow) {
        completionEventFlow?.collect { event ->
            if (event.taskId == task.id && event.xpAwarded > 0) {
                localXpAmount = event.xpAwarded
                showLocalXp = true
            }
        }
    }

    // ── Phase 3: Card success animation ─────────────────────────────────
    val cardScale by animateFloatAsState(
        targetValue = if (animController.isAnimating && animController.phase == 3) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val successTint by animateColorAsState(
        targetValue = if (animController.isAnimating && animController.phase == 3)
            MustDoColors.Success.copy(alpha = 0.06f)
        else Color.Transparent,
        animationSpec = tween(150),
        label = "successTint"
    )

    val cardElevation by animateFloatAsState(
        targetValue = if (animController.isAnimating && animController.phase == 3) 6f else 0f,
        animationSpec = tween(150),
        label = "cardElevation"
    )

    // ── Phase 4: Text completion animation ──────────────────────────────
    val isInPhase4OrLater = animController.isAnimating && animController.phase >= 4
    val showCompletedVisuals = isCompleted && (!animController.isAnimating || animController.phase >= 4)

    val titleColor by animateColorAsState(
        targetValue = if (showCompletedVisuals)
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(250),
        label = "titleColor"
    )

    val descriptionAlpha by animateFloatAsState(
        targetValue = if (showCompletedVisuals) 0.4f else 0.7f,
        animationSpec = tween(250),
        label = "descAlpha"
    )

    val chipAlpha by animateFloatAsState(
        targetValue = if (showCompletedVisuals) 0.5f else 1f,
        animationSpec = tween(250),
        label = "chipAlpha"
    )

    // Animated strikethrough progress (0→1 draws left to right)
    val strikethroughProgress by animateFloatAsState(
        targetValue = if (showCompletedVisuals) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(300, easing = FastOutSlowInEasing)
                        else snap(),
        label = "strikethrough"
    )

    val animatedBorderAlpha by animateFloatAsState(
        targetValue = if (isOverdue) 0.6f else 0f,
        animationSpec = tween(300),
        label = "border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                shadowElevation = cardElevation
            }
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = { onTaskClick(task.id) },
                onLongClick = { onLongClick?.invoke(task.id) }
            ),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isOverdue) 1.5.dp else 1.dp,
            color = if (isOverdue) MustDoColors.Accent.copy(alpha = animatedBorderAlpha) else MustDoColors.DarkBorder
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // Success tint overlay
            if (successTint != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(successTint, RoundedCornerShape(16.dp))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated completion checkbox
                Box(
                    modifier = Modifier.size(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedCompletionCheckbox(
                        isCompleted = isCompleted,
                        priority = task.priority,
                        onToggle = { onCompleteClick(task.id) },
                        animationController = animController
                    )

                    // Local XP Reward Chip anchored exactly above the checkbox/title area
//                    Box(
//                        modifier = Modifier
//                            .offset(y = 0.dp)
//                            .align(Alignment.TopCenter)
//                    ) {
//                        XpRewardChip(
//                            xpAmount = localXpAmount,
//                            visible = showLocalXp,
//                            onDismiss = { showLocalXp = false }
//                        )
//                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Task content
                Column(modifier = Modifier.weight(1f)) {
                    // Title with animated strikethrough
                    Box {
                        var titleWidthPx by remember { mutableIntStateOf(0) }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = TextDecoration.None,
                            color = titleColor,
                            modifier = Modifier.onSizeChanged { titleWidthPx = it.width }
                        )

                        // Draw animated strikethrough line
                        if (strikethroughProgress > 0f) {
                            val density = LocalDensity.current
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                            ) {
                                val lineY = size.height / 2f
                                val lineWidth = titleWidthPx * strikethroughProgress
                                drawLine(
                                    color = titleColor,
                                    start = Offset(0f, lineY),
                                    end = Offset(lineWidth, lineY),
                                    strokeWidth = with(density) { 1.2.dp.toPx() }
                                )
                            }
                        }
                    }

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = descriptionAlpha
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.graphicsLayer { alpha = chipAlpha }
                    ) {
                        PriorityChip(priority = task.priority)

                        if (task.dueDateMillis != null) {
                            Text(
                                text = DateUtils.formatRelativeDate(task.dueDateMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) MustDoColors.Accent
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (categoryName != null) {
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(categoryColor ?: "#6C63FF"))
                            } catch (e: Exception) {
                                MustDoColors.Primary
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = catColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    

                    if (task.estimatedMinutes != null) {
                            Text(
                                text = "${task.estimatedMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (checklistTotal > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = checklistChecked.toFloat() / checklistTotal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MustDoColors.Primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "$checklistChecked/$checklistTotal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (task.notes.isNotBlank() || hasResources || hasNoteBlocks) {
                    IconButton(
                        onClick = { onInfoClick?.invoke() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Show details"
                        )
                    }
                }
            }
        }
    }
}

// ─── Completion Checkbox ────────────────────────────────────────────────────

@Composable
fun CompletionCheckbox(
    isCompleted: Boolean,
    priority: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (priority) {
        Priority.LOW.name -> MustDoColors.PriorityLow
        Priority.MEDIUM.name -> MustDoColors.PriorityMedium
        Priority.HIGH.name -> MustDoColors.PriorityHigh
        Priority.URGENT.name -> MustDoColors.PriorityUrgent
        else -> MustDoColors.Primary
    }

    val alpha by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(200),
        label = "checkbox_alpha"
    )

    val gradient = Brush.linearGradient(
        colors = listOf(
            MustDoColors.Primary,
            MustDoColors.PrimaryLight
        )
    )

    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (isCompleted) Modifier.background(gradient, alpha = alpha)
                else Modifier.background(Color.Transparent)
            )
            .border(
                width = if (isCompleted) 0.dp else 2.dp,
                color = if (isCompleted) Color.Transparent else priorityColor.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Circular Progress ──────────────────────────────────────────────────────

@Composable
fun CircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    progressColor: Color = MustDoColors.Primary,
    trackColor: Color = MustDoColors.DarkSurfaceElevated,
    content: @Composable () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val sizeValue = size.minDimension - strokeWidthPx
            
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx),
                size = androidx.compose.ui.geometry.Size(sizeValue, sizeValue),
                topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            )
            
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(sizeValue, sizeValue),
                topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            )
        }
        content()
    }
}

// ─── Stat Card ──────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.graphicsLayer {
            shadowElevation = 2f
            shape = RoundedCornerShape(18.dp)
            clip = true
        },
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MustDoColors.DarkBorder),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Empty State ────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}

// ─── Section Header ─────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
fun GradientFab(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "fabScale")

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = 8f
                shape = RoundedCornerShape(20.dp)
                clip = false
                ambientShadowColor = MustDoColors.Primary
                spotShadowColor = MustDoColors.PrimaryLight
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(MustDoColors.Primary, MustDoColors.PrimaryLight)
                )
            )
            .clickable { onClick() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            androidx.compose.ui.input.pointer.PointerEventType.Press -> isPressed = true
                            androidx.compose.ui.input.pointer.PointerEventType.Release -> isPressed = false
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "FAB Icon",
            tint = Color.White
        )
    }
}
@Composable
fun PremiumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "btnScale")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (enabled) 6f else 0f
                shape = RoundedCornerShape(14.dp)
                clip = false
                ambientShadowColor = MustDoColors.Primary
                spotShadowColor = MustDoColors.PrimaryLight
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = if (enabled) Brush.linearGradient(
                    colors = listOf(MustDoColors.Primary, MustDoColors.PrimaryLight)
                ) else Brush.linearGradient(
                    colors = listOf(MustDoColors.DarkDisabled, MustDoColors.DarkDisabled)
                )
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .pointerInput(Unit) {
                if (enabled) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press -> isPressed = true
                                androidx.compose.ui.input.pointer.PointerEventType.Release -> isPressed = false
                            }
                        }
                    }
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun PremiumSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "btnScale")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = 1.dp,
                color = if (enabled) MustDoColors.DarkBorder else MustDoColors.DarkDisabled,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .pointerInput(Unit) {
                if (enabled) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press -> isPressed = true
                                androidx.compose.ui.input.pointer.PointerEventType.Release -> isPressed = false
                            }
                        }
                    }
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

fun Modifier.softGlow(
    color: Color,
    radius: Dp = 8.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
): Modifier = this.graphicsLayer {
    shadowElevation = radius.value
    this.shape = shape
    clip = false
    ambientShadowColor = color
    spotShadowColor = color
}

@Composable
fun CommonComponentsPreview() {
    MustDoTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = "SECTION HEADER EXAMPLE")
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("This is a Glass Card", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("It uses glassmorphism borders and shadows.", color = Color.White.copy(alpha = 0.7f))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressRing(
                    progress = 0.75f,
                    modifier = Modifier.size(60.dp),
                    strokeWidth = 6.dp,
                    progressColor = MustDoColors.Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text("75%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }

                StatCard(
                    label = "Tasks Done",
                    value = "12",
                    icon = Icons.Default.CheckCircle,
                    color = MustDoColors.Success,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

