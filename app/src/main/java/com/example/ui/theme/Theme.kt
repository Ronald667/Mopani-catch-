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
    primary = ForestGreen,
    secondary = AmberGold,
    tertiary = Terracotta,
    background = Color(0xFF1F1D1A),
    surface = Color(0xFF2D2A26),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFFDE68A),
    onSurface = Color(0xFFFDE68A)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForestGreenDark,
    secondary = AmberGold,
    tertiary = Terracotta,
    background = WarmCream,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight
  )

@Composable
fun MopaniCatchTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // prefer our custom nature theme by default
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

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MopaniCatchTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
