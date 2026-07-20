package com.example.todo.features.settings.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.todo.common.components.SectionHeader
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) {
        null
    }
    val versionName = packageInfo?.versionName ?: "1.0"

    SettingsSubPageScaffold(
        title = "About",
        onNavigateBack = onNavigateBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // App branding
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MustDo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Turn Chaos Into Progress",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MustDoColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // App Info
            item {
                SectionHeader(
                    title = "APP INFO",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                SharedSettingsCard {
                    SharedSettingsInfo(label = "Version", value = versionName)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsInfo(label = "Platform", value = "Android")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsInfo(label = "Developer", value = "MustDo Team")
                }
            }

            // Actions
            item {
                SectionHeader(
                    title = "SUPPORT & FEEDBACK",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                SharedSettingsCard {
                    SharedSettingsButton(
                        label = "Rate the App",
                        icon = Icons.Outlined.Star,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsButton(
                        label = "Share with Friends",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "MustDo — Productivity App")
                                putExtra(Intent.EXTRA_TEXT, "Check out MustDo, a smart productivity app! https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsButton(
                        label = "Send Feedback",
                        icon = Icons.Outlined.Email,
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@mustdo.app"))
                                putExtra(Intent.EXTRA_SUBJECT, "MustDo App Feedback (v$versionName)")
                            }
                            try {
                                context.startActivity(emailIntent)
                            } catch (_: Exception) { }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SharedSettingsButton(
                        label = "Report a Bug",
                        icon = Icons.Outlined.BugReport,
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("bugs@mustdo.app"))
                                putExtra(Intent.EXTRA_SUBJECT, "MustDo Bug Report (v$versionName)")
                            }
                            try {
                                context.startActivity(emailIntent)
                            } catch (_: Exception) { }
                        }
                    )
                }
            }

            // Footer
            item {
                Text(
                    text = "Made with ❤️ for productivity lovers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun AboutScreenPreview() {
    MustDoTheme(darkTheme = true) {
        AboutScreen(onNavigateBack = {})
    }
}
