package com.example.todo.features.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.SectionHeader
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import com.example.todo.core.database.entity.AchievementEntity

@Composable
fun AchievementsSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AchievementsSettingsContent(
        achievements = state.achievements,
        totalXp = state.totalXp,
        currentLevel = state.currentLevel,
        currentStreak = state.currentStreak,
        longestStreak = state.longestStreak,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun AchievementsSettingsContent(
    achievements: List<AchievementEntity> = emptyList(),
    totalXp: Int = 0,
    currentLevel: Int = 0,
    currentStreak: Int = 0,
    longestStreak: Int = 0,
    onNavigateBack: () -> Unit = {}
) {
    val xpProgress = (totalXp % 100) / 100f
    
    SettingsSubPageScaffold(
        title = "Gamification Hub",
        onNavigateBack = onNavigateBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // GAMIFICATION STATS
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Level and XP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Level $currentLevel",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MustDoColors.Primary
                            )
                            Text(
                                text = "$totalXp Total XP",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MustDoColors.Primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Text(
                            text = "${totalXp % 100} / 100 XP to Level ${currentLevel + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Streaks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🔥 $currentStreak",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current Streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "👑 $longestStreak",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Longest Streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "YOUR BADGES",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            items(achievements, key = { it.id }) { ach ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ach.isUnlocked)
                            MustDoColors.Primary.copy(alpha = 0.08f)
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (ach.isUnlocked) Icons.Default.EmojiEvents else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (ach.isUnlocked) MustDoColors.Warning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = ach.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (ach.isUnlocked) {
                            Text(
                                text = "✓",
                                color = MustDoColors.Success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun AchievementsSettingsPreview() {
    val sampleAchievements = listOf(
        AchievementEntity(id = "1", title = "First Steps", description = "Complete your first task", conditionType = "TASKS_COMPLETED", conditionValue = 1, isUnlocked = true),
        AchievementEntity(id = "2", title = "Streak Master", description = "Maintain a 7-day streak", conditionType = "STREAK", conditionValue = 7, isUnlocked = true),
        AchievementEntity(id = "3", title = "Focus Champion", description = "Complete 10 focus sessions", conditionType = "FOCUS_SESSIONS", conditionValue = 10, isUnlocked = false),
        AchievementEntity(id = "4", title = "Centurion", description = "Complete 100 tasks", conditionType = "TASKS_COMPLETED", conditionValue = 100, isUnlocked = false)
    )
    MustDoTheme(darkTheme = true) {
        AchievementsSettingsContent(achievements = sampleAchievements)
    }
}
