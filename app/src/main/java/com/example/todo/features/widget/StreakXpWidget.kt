package com.example.todo.features.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.todo.MainActivity
import com.example.todo.R
import kotlinx.coroutines.flow.first

class StreakXpWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = WidgetDependencies.getUserPreferencesManager(context)
        val streak = prefs.currentStreak.first()
        val totalXp = prefs.totalXp.first()
        val currentLevel = calculateLevel(totalXp)
        val nextLevelXp = calculateNextLevelXp(currentLevel)
        val levelProgress = calculateLevelProgress(totalXp)

        provideContent {
            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Streak Column
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔥",
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$streak Days",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Divider
                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(GlanceTheme.colors.outline)
                    ) {}

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // XP Column
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Level $currentLevel",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "$totalXp / $nextLevelXp XP",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }

    private fun calculateLevel(xp: Int): Int {
        return (xp / 1000) + 1
    }

    private fun calculateNextLevelXp(level: Int): Int {
        return level * 1000
    }

    private fun calculateLevelProgress(xp: Int): Float {
        val levelXp = xp % 1000
        return levelXp.toFloat() / 1000f
    }
}
