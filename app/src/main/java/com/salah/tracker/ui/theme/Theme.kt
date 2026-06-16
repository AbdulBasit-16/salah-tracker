package com.salah.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

// 1. Forest Green (Default)
private val ForestLight = lightColorScheme(
    primary = ForestPrimaryLight,
    secondary = ForestSecondaryLight,
    tertiary = ForestTertiaryLight,
    background = ForestBackgroundLight,
    surface = ForestSurfaceLight,
    onPrimary = ForestBackgroundLight,
    onBackground = ForestTextLight,
    onSurface = ForestTextLight
)
private val ForestDark = darkColorScheme(
    primary = ForestPrimaryDark,
    secondary = ForestSecondaryDark,
    tertiary = ForestTertiaryDark,
    background = ForestBackgroundDark,
    surface = ForestSurfaceDark,
    onPrimary = ForestBackgroundDark,
    onBackground = ForestTextDark,
    onSurface = ForestTextDark
)

// 2. Deep Blue
private val BlueLight = lightColorScheme(
    primary = BluePrimaryLight,
    secondary = BlueSecondaryLight,
    tertiary = BlueTertiaryLight,
    background = BlueBackgroundLight,
    surface = BlueSurfaceLight,
    onPrimary = BlueBackgroundLight,
    onBackground = BlueTextLight,
    onSurface = BlueTextLight
)
private val BlueDark = darkColorScheme(
    primary = BluePrimaryDark,
    secondary = BlueSecondaryDark,
    tertiary = BlueTertiaryDark,
    background = BlueBackgroundDark,
    surface = BlueSurfaceDark,
    onPrimary = BlueBackgroundDark,
    onBackground = BlueTextDark,
    onSurface = BlueTextDark
)

// 3. Olive Gold
private val OliveLight = lightColorScheme(
    primary = OlivePrimaryLight,
    secondary = OliveSecondaryLight,
    tertiary = OliveTertiaryLight,
    background = OliveBackgroundLight,
    surface = OliveSurfaceLight,
    onPrimary = OliveBackgroundLight,
    onBackground = OliveTextLight,
    onSurface = OliveTextLight
)
private val OliveDark = darkColorScheme(
    primary = OlivePrimaryDark,
    secondary = OliveSecondaryDark,
    tertiary = OliveTertiaryDark,
    background = OliveBackgroundDark,
    surface = OliveSurfaceDark,
    onPrimary = OliveBackgroundDark,
    onBackground = OliveTextDark,
    onSurface = OliveTextDark
)

// 4. Royal Purple
private val PurpleLight = lightColorScheme(
    primary = PurplePrimaryLight,
    secondary = PurpleSecondaryLight,
    tertiary = PurpleTertiaryLight,
    background = PurpleBackgroundLight,
    surface = PurpleSurfaceLight,
    onPrimary = PurpleBackgroundLight,
    onBackground = PurpleTextLight,
    onSurface = PurpleTextLight
)
private val PurpleDark = darkColorScheme(
    primary = PurplePrimaryDark,
    secondary = PurpleSecondaryDark,
    tertiary = PurpleTertiaryDark,
    background = PurpleBackgroundDark,
    surface = PurpleSurfaceDark,
    onPrimary = PurpleBackgroundDark,
    onBackground = PurpleTextDark,
    onSurface = PurpleTextDark
)

// 5. Slate Rose
private val RoseLight = lightColorScheme(
    primary = RosePrimaryLight,
    secondary = RoseSecondaryLight,
    tertiary = RoseTertiaryLight,
    background = RoseBackgroundLight,
    surface = RoseSurfaceLight,
    onPrimary = RoseBackgroundLight,
    onBackground = RoseTextLight,
    onSurface = RoseTextLight
)
private val RoseDark = darkColorScheme(
    primary = RosePrimaryDark,
    secondary = RoseSecondaryDark,
    tertiary = RoseTertiaryDark,
    background = RoseBackgroundDark,
    surface = RoseSurfaceDark,
    onPrimary = RoseBackgroundDark,
    onBackground = RoseTextDark,
    onSurface = RoseTextDark
)

@Composable
fun SalahTrackerTheme(
    themeName: String = "FOREST_GREEN",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "DEEP_BLUE" -> if (darkTheme) BlueDark else BlueLight
        "OLIVE_GOLD" -> if (darkTheme) OliveDark else OliveLight
        "ROYAL_PURPLE" -> if (darkTheme) PurpleDark else PurpleLight
        "SLATE_ROSE" -> if (darkTheme) RoseDark else RoseLight
        else -> if (darkTheme) ForestDark else ForestLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Force fixed font scale by overriding LocalDensity
    val currentDensity = LocalDensity.current
    val customDensity = Density(
        density = currentDensity.density,
        fontScale = 1.0f // Disable system font size changes!
    )

    CompositionLocalProvider(LocalDensity provides customDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
