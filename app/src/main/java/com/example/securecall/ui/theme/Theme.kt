package com.example.securecall.ui.theme

import android.app.Activity
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


private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,

    secondary = PrimaryBlueDark,
    onSecondary = White,

    background = White,
    onBackground = Color(0xFF1C1B1F),

    surface = White,
    onSurface = Color(0xFF1C1B1F),

    error = ErrorRed,
    onError = White,

    outline = LightGray

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlueLight,

    secondary = PrimaryBlueLight,
    onSecondary = DarkBackground,
    secondaryContainer = PrimaryBlueDark,
    onSecondaryContainer = PrimaryBlueLight,

    background = DarkBackground,
    onBackground = White,

    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = LightGray,

    error = ErrorRed,
    onError = White,

    outline = Color(0xFF938F99)
)


@Composable
fun SecureCallTheme(
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