package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = BrightCyan,
        onPrimary = DarkBg,
        primaryContainer = SecondaryTeal,
        onPrimaryContainer = Color.White,
        secondary = AccentCyan,
        onSecondary = DarkBg,
        tertiary = AlertAmber,
        onTertiary = Color.White,
        error = AlertRed,
        onError = Color.White,
        background = DarkBg,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = Color(0xFFB0C4DE),
        outline = DarkOutline
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryNavy,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E4F0),
        onPrimaryContainer = PrimaryNavy,
        secondary = AccentCyan,
        onSecondary = Color.White,
        tertiary = AlertAmber,
        onTertiary = Color.White,
        error = AlertRed,
        onError = Color.White,
        background = LightBg,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = Color(0xFF475569),
        outline = LightOutline
    )

@Composable
fun EcoSismoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

