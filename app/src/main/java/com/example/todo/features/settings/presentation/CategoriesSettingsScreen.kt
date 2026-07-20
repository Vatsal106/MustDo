package com.example.todo.features.settings.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.SectionHeader
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import com.example.todo.core.database.entity.CategoryEntity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


@Composable
fun CategoriesSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#6C63FF") }

    SettingsSubPageScaffold(
        title = "Categories & Labels",
        onNavigateBack = onNavigateBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                SectionHeader(
                    title = "CATEGORIES",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    trailing = {
                        IconButton(onClick = { showAddCategory = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                )
            }

            items(state.categories, key = { it.id }) { category ->
                val catColor = try {
                    Color(android.graphics.Color.parseColor(category.colorHex))
                } catch (e: Exception) {
                    MustDoColors.Primary
                }


                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(catColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (!category.isSystem) {
                            IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(20.dp),
                                    tint = MustDoColors.Accent
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // Add category dialog
    if (showAddCategory) {
        val colorOptions = listOf(
            "#6C63FF", "#FF6B6B", "#51CF66", "#FFD43B",
            "#22D3EE", "#FF922B", "#A855F7", "#F472B6"
        )

        var hue by remember { mutableStateOf(244f) }
        var saturation by remember { mutableStateOf(0.61f) }
        var value by remember { mutableStateOf(1f) }
        var hexInputText by remember { mutableStateOf(selectedColorHex) }
        var rInputText by remember { mutableStateOf("108") }
        var gInputText by remember { mutableStateOf("99") }
        var bInputText by remember { mutableStateOf("255") }

        LaunchedEffect(Unit) {
            hexToHsv(selectedColorHex)?.let { hsv ->
                hue = hsv[0]
                saturation = hsv[1] / 100f
                value = hsv[2] / 100f
            }
            hexInputText = selectedColorHex
            val colorInt = try {
                android.graphics.Color.parseColor(selectedColorHex)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#6C63FF")
            }
            rInputText = android.graphics.Color.red(colorInt).toString()
            gInputText = android.graphics.Color.green(colorInt).toString()
            bInputText = android.graphics.Color.blue(colorInt).toString()
        }

        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text("New Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Presets", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                            FilterChip(
                                selected = selectedColorHex.uppercase() == hex.uppercase(),
                                onClick = {
                                    selectedColorHex = hex
                                    hexInputText = hex
                                    hexToHsv(hex)?.let { hsv ->
                                        hue = hsv[0]
                                        saturation = hsv[1] / 100f
                                        value = hsv[2] / 100f
                                    }
                                    val colorInt = try {
                                        android.graphics.Color.parseColor(hex)
                                    } catch (e: Exception) {
                                        android.graphics.Color.parseColor("#6C63FF")
                                    }
                                    rInputText = android.graphics.Color.red(colorInt).toString()
                                    gInputText = android.graphics.Color.green(colorInt).toString()
                                    bInputText = android.graphics.Color.blue(colorInt).toString()
                                },
                                label = { },
                                modifier = Modifier.size(32.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color,
                                    containerColor = color.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Color", style = MaterialTheme.typography.labelMedium)

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HsvColorPickerWheelLayout(
                            hue = hue,
                            saturation = saturation,
                            value = value,
                            onColorChange = { h, s, v ->
                                hue = h
                                saturation = s
                                value = v
                                val hex = hsvToHex(h, s, v)
                                selectedColorHex = hex
                                hexInputText = hex
                                val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
                                rInputText = android.graphics.Color.red(colorInt).toString()
                                gInputText = android.graphics.Color.green(colorInt).toString()
                                bInputText = android.graphics.Color.blue(colorInt).toString()
                            },
                            modifier = Modifier.size(150.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val previewColor = try {
                                Color(android.graphics.Color.parseColor(selectedColorHex))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(previewColor)
                            )

                            OutlinedTextField(
                                value = hexInputText,
                                onValueChange = { input ->
                                    hexInputText = input
                                    if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                                        hexToHsv(input)?.let { hsv ->
                                            selectedColorHex = input
                                            hue = hsv[0]
                                            saturation = hsv[1] / 100f
                                            value = hsv[2] / 100f
                                            val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
                                            rInputText = android.graphics.Color.red(colorInt).toString()
                                            gInputText = android.graphics.Color.green(colorInt).toString()
                                            bInputText = android.graphics.Color.blue(colorInt).toString()
                                        }
                                    }
                                },
                                placeholder = { Text("#6C63FF") },
                                singleLine = true,
                                modifier = Modifier.width(90.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Text("+", style = MaterialTheme.typography.titleMedium)
                        }

                        TextButton(
                            onClick = {
                                val defaultHex = "#6C63FF"
                                selectedColorHex = defaultHex
                                hexInputText = defaultHex
                                hexToHsv(defaultHex)?.let { hsv ->
                                    hue = hsv[0]
                                    saturation = hsv[1] / 100f
                                    value = hsv[2] / 100f
                                }
                                val colorInt = android.graphics.Color.parseColor(defaultHex)
                                rInputText = android.graphics.Color.red(colorInt).toString()
                                gInputText = android.graphics.Color.green(colorInt).toString()
                                bInputText = android.graphics.Color.blue(colorInt).toString()
                            }
                        ) {
                            Text("Default color", style = MaterialTheme.typography.bodyMedium, color = MustDoColors.Primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("R", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = rInputText,
                                onValueChange = { input ->
                                    rInputText = input
                                    val rVal = input.toIntOrNull()?.coerceIn(0, 255)
                                    if (rVal != null) {
                                        val gVal = gInputText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                                        val bVal = bInputText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                                        val hsvArray = FloatArray(3)
                                        android.graphics.Color.RGBToHSV(rVal, gVal, bVal, hsvArray)
                                        hue = hsvArray[0]
                                        saturation = hsvArray[1]
                                        value = hsvArray[2]
                                        selectedColorHex = String.format("#%02X%02X%02X", rVal, gVal, bVal)
                                        hexInputText = selectedColorHex
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.width(60.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("G", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = gInputText,
                                onValueChange = { input ->
                                    gInputText = input
                                    val gVal = input.toIntOrNull()?.coerceIn(0, 255)
                                    if (gVal != null) {
                                        val rVal = rInputText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                                        val bVal = bInputText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                                        val hsvArray = FloatArray(3)
                                        android.graphics.Color.RGBToHSV(rVal, gVal, bVal, hsvArray)
                                        hue = hsvArray[0]
                                        saturation = hsvArray[1]
                                        value = hsvArray[2]
                                        selectedColorHex = String.format("#%02X%02X%02X", rVal, gVal, bVal)
                                        hexInputText = selectedColorHex
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.width(60.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("B", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = bInputText,
                                onValueChange = { input ->
                                    bInputText = input
                                    val bVal = input.toIntOrNull()?.coerceIn(0, 255)
                                    if (bVal != null) {
                                        val rVal = rInputText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                                        val gVal = gInputText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                                        val hsvArray = FloatArray(3)
                                        android.graphics.Color.RGBToHSV(rVal, gVal, bVal, hsvArray)
                                        hue = hsvArray[0]
                                        saturation = hsvArray[1]
                                        value = hsvArray[2]
                                        selectedColorHex = String.format("#%02X%02X%02X", rVal, gVal, bVal)
                                        hexInputText = selectedColorHex
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.width(60.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim(), selectedColorHex)
                            newCategoryName = ""
                            showAddCategory = false
                        }
                    }
                ) { Text("Add", color = MustDoColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false }) { Text("Cancel") }
            }
        )
    }


}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun CategoriesSettingsPreview() {
    val sampleCategories = listOf(
        CategoryEntity(id = "1", name = "Work", colorHex = "#6C63FF", isSystem = true),
        CategoryEntity(id = "2", name = "Personal", colorHex = "#51CF66", isSystem = true),
        CategoryEntity(id = "3", name = "Health", colorHex = "#FF6B6B"),
        CategoryEntity(id = "4", name = "Learning", colorHex = "#22D3EE")
    )
    MaterialTheme {
        // Note: Preview shows static layout only (no ViewModel interaction)
        SettingsSubPageScaffold(
            title = "Categories & Labels",
            onNavigateBack = {}
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    SectionHeader(
                        title = "CATEGORIES",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        trailing = {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Add, contentDescription = "Add Category")
                            }
                        }
                    )
                }
                items(sampleCategories, key = { it.id }) { category ->
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (e: Exception) {
                        MustDoColors.Primary
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (!category.isSystem) {
                                IconButton(onClick = {}) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(20.dp),
                                        tint = MustDoColors.Accent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hsvToHex(h: Float, s: Float, v: Float): String {
    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    return String.format("#%06X", 0xFFFFFF and colorInt)
}

private fun hexToHsv(hex: String): FloatArray? {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorInt, hsv)
        hsv[1] = hsv[1] * 100f
        hsv[2] = hsv[2] * 100f
        hsv
    } catch (e: Exception) {
        null
    }
}

@Composable
fun HsvColorPickerWheelLayout(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChange: (h: Float, s: Float, v: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(hue, saturation, value) {
                val thickness = 20.dp.toPx()
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    handleTouch(offset, width, height, thickness, hue, saturation, value, onColorChange)
                }
            }
            .pointerInput(hue, saturation, value) {
                val thickness = 20.dp.toPx()
                detectDragGestures { change, _ ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    handleTouch(change.position, width, height, thickness, hue, saturation, value, onColorChange)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val outerRadius = width / 2
        val thickness = 14.dp.toPx() // Thinner ring looks cleaner
        val innerRadius = outerRadius - thickness

        // 1. Draw Sweep Gradient Hue Ring
        val sweepGradient = Brush.sweepGradient(
            colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            brush = sweepGradient,
            radius = outerRadius - thickness / 2,
            style = Stroke(width = thickness)
        )

        // 2. Draw Saturation-Value Square inside
        val squareSize = innerRadius * 2f / sqrt(2f) * 0.95f
        val left = centerX - squareSize / 2
        val top = centerY - squareSize / 2

        val hueColor = Color.hsv(hue, 1f, 1f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, hueColor),
                startX = left,
                endX = left + squareSize
            ),
            topLeft = Offset(left, top),
            size = Size(squareSize, squareSize)
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = top,
                endY = top + squareSize
            ),
            topLeft = Offset(left, top),
            size = Size(squareSize, squareSize)
        )

        // 3. Draw Hue Thumb Indicator on the circle
        val angleRad = Math.toRadians(hue.toDouble())
        val thumbRadius = outerRadius - thickness / 2
        val thumbX = centerX + thumbRadius * cos(angleRad).toFloat()
        val thumbY = centerY + thumbRadius * sin(angleRad).toFloat()
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(thumbX, thumbY),
            style = Stroke(width = 2.dp.toPx())
        )

        // 4. Draw S-V Thumb Indicator inside the square
        val svThumbX = left + saturation * squareSize
        val svThumbY = top + (1f - value) * squareSize
        drawCircle(
            color = if (value > 0.5f && saturation < 0.4f) Color.Black else Color.White,
            radius = 5.dp.toPx(),
            center = Offset(svThumbX, svThumbY),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun handleTouch(
    offset: Offset,
    width: Float,
    height: Float,
    thickness: Float,
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChange: (h: Float, s: Float, v: Float) -> Unit
) {
    val centerX = width / 2f
    val centerY = height / 2f
    val outerRadius = width / 2f
    val innerRadius = outerRadius - thickness
    val squareSize = innerRadius * 2f / sqrt(2f) * 0.95f
    val left = centerX - squareSize / 2f
    val top = centerY - squareSize / 2f

    val dx = offset.x - centerX
    val dy = offset.y - centerY
    val r = sqrt(dx * dx + dy * dy)

    if (r >= innerRadius - 15f && r <= outerRadius + 15f) {
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val rawHue = (angle + 360f) % 360f
        onColorChange(rawHue, saturation, value)
    } else if (offset.x >= left && offset.x <= left + squareSize && offset.y >= top && offset.y <= top + squareSize) {
        val s = ((offset.x - left) / squareSize).coerceIn(0f, 1f)
        val v = (1f - (offset.y - top) / squareSize).coerceIn(0f, 1f)
        onColorChange(hue, s, v)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun CategoriesSettingsScreenPreview() {
    MustDoTheme(darkTheme = true) {
        SettingsSubPageScaffold(
            title = "Categories & Labels",
            onNavigateBack = {}
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    SectionHeader(
                        title = "YOUR CATEGORIES",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Work", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Personal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Learning", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}



