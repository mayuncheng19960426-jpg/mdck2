package com.sillyandroid.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ColorPalette.PrimaryLight,
    secondary = ColorPalette.SecondaryLight,
    tertiary = ColorPalette.Accent,
    background = ColorPalette.BackgroundDark,
    surface = ColorPalette.SurfaceDark,
    surfaceVariant = ColorPalette.SurfaceVariantDark,
    onPrimary = ColorPalette.OnPrimary,
    onSecondary = ColorPalette.OnPrimary,
    onTertiary = ColorPalette.OnPrimary,
    onBackground = ColorPalette.TextPrimaryDark,
    onSurface = ColorPalette.TextPrimaryDark,
    onSurfaceVariant = ColorPalette.TextSecondaryDark,
    outline = ColorPalette.DividerDark
)

private val LightColorScheme = lightColorScheme(
    primary = ColorPalette.PrimaryDark,
    secondary = ColorPalette.SecondaryDark,
    tertiary = ColorPalette.Accent,
    background = ColorPalette.BackgroundLight,
    surface = ColorPalette.SurfaceLight,
    surfaceVariant = ColorPalette.SurfaceVariantLight,
    onPrimary = ColorPalette.OnPrimary,
    onSecondary = ColorPalette.OnPrimary,
    onTertiary = ColorPalette.OnPrimary,
    onBackground = ColorPalette.TextPrimaryLight,
    onSurface = ColorPalette.TextPrimaryLight,
    onSurfaceVariant = ColorPalette.TextSecondaryLight,
    outline = ColorPalette.DividerLight
)

@Composable
fun SillyAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SillyTypography,
        content = content
    )
}
