package com.orca.kegel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.orca.kegel.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6F62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB9E6D9),
    onPrimaryContainer = Color(0xFF06201A),
    secondary = Color(0xFF4A635D),
    background = Color(0xFFF6FAF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ED3C5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF165047),
    onPrimaryContainer = Color(0xFFB9E6D9),
    secondary = Color(0xFFB1CCC4),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF171D1B),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBFC9C6),
    error = Color(0xFFF2B8B5),
)

/**
 * 主题三态：LIGHT / DARK / SYSTEM（默认 SYSTEM）。
 * 沉浸式训练页同样使用本主题，不擅自强制另一态。
 */
@Composable
fun KegelTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}