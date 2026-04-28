package com.example.mytodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SkyLinkBlue,
    secondary = PearlButton,
    tertiary = ActionBlue,
    background = PureBlack,
    surface = NearBlackTile1,
    onPrimary = PureWhite,
    onSecondary = NearBlackInk,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = NearBlackTile2,
)

private val LightColorScheme = lightColorScheme(
    primary = ActionBlue,
    secondary = PearlButton,
    tertiary = FocusBlue,
    background = Parchment,
    surface = PureWhite,
    onPrimary = PureWhite,
    onSecondary = NearBlackInk,
    onBackground = NearBlackInk,
    onSurface = NearBlackInk,
    surfaceVariant = Parchment,
    primaryContainer = ActionBlue.copy(alpha = 0.1f),
    onPrimaryContainer = ActionBlue,
)

@Composable
fun MyTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Enforce our design system
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
