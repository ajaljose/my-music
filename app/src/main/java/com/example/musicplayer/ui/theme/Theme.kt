package com.example.musicplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color.Black,
    primaryContainer = AccentGold.copy(alpha = 0.2f),
    onPrimaryContainer = AccentGold,
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = AccentTeal.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,
    tertiary = AccentCyan,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    surfaceContainer = CardBackgroundDark,
    surfaceContainerHigh = BadgeBackgroundDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0x14FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentTeal,
    onPrimary = Color.White,
    primaryContainer = AccentTeal.copy(alpha = 0.15f),
    onPrimaryContainer = AccentTeal,
    secondary = AccentGold,
    onSecondary = Color.Black,
    secondaryContainer = AccentGold.copy(alpha = 0.15f),
    onSecondaryContainer = Color.Black,
    tertiary = AccentCyan,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = Color(0xFFE8EAF0),
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0x14000000)
)

@Composable
fun HarmoniaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
