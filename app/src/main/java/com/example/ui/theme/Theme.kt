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

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0E3A4B),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = PurpleSubscription,
    background = VaultDarkBg,
    onBackground = TextPrimaryDark,
    surface = VaultDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = VaultDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = VaultDarkBorder,
    error = RoseExpense,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = PurpleSubscription,
    background = VaultLightBg,
    onBackground = TextPrimaryLight,
    surface = VaultLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = VaultLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = VaultLightBorder,
    error = RoseExpense,
    onError = Color.White
)

@Composable
fun PocketVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent sleek financial branding
    content: @Composable () -> Unit,
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = PocketVaultTheme(darkTheme, dynamicColor, content)
