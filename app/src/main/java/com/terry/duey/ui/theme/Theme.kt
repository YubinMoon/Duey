package com.terry.duey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.color.ColorProvider as GlanceDayNightColorProvider
import androidx.glance.color.colorProviders

private val DarkColorScheme = darkColorScheme(
    primary = SkyLinkBlue,
    secondary = PearlButton,
    tertiary = ActionBlue,
    background = PureBlack,
    surface = NearBlackTile1,
    onPrimary = PaperWhite,
    onSecondary = NearBlackInk,
    onBackground = PaperWhite,
    onSurface = PaperWhite,
    onSurfaceVariant = SoftChipGray,
    surfaceVariant = NearBlackTile2,
    outline = SoftChipGray,
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
    onSurfaceVariant = MutedInk48,
    surfaceVariant = Parchment,
    outline = DividerGray,
    primaryContainer = ActionBlue.copy(alpha = 0.1f),
    onPrimaryContainer = ActionBlue,
)

val DueyGlanceColorScheme = colorProviders(
    primary = GlanceDayNightColorProvider(ActionBlue, SkyLinkBlue),
    onPrimary = GlanceDayNightColorProvider(PureWhite, PaperWhite),
    primaryContainer = GlanceDayNightColorProvider(ActionBlue.copy(alpha = 0.1f), NearBlackTile2),
    onPrimaryContainer = GlanceDayNightColorProvider(ActionBlue, SkyLinkBlue),
    secondary = GlanceDayNightColorProvider(PearlButton, PearlButton),
    onSecondary = GlanceDayNightColorProvider(NearBlackInk, NearBlackInk),
    secondaryContainer = GlanceDayNightColorProvider(Parchment, NearBlackTile2),
    onSecondaryContainer = GlanceDayNightColorProvider(NearBlackInk, PaperWhite),
    tertiary = GlanceDayNightColorProvider(FocusBlue, ActionBlue),
    onTertiary = GlanceDayNightColorProvider(PureWhite, PaperWhite),
    tertiaryContainer = GlanceDayNightColorProvider(Parchment, NearBlackTile2),
    onTertiaryContainer = GlanceDayNightColorProvider(NearBlackInk, PaperWhite),
    error = GlanceDayNightColorProvider(SundayRed, SundayRed),
    errorContainer = GlanceDayNightColorProvider(Parchment, NearBlackTile2),
    onError = GlanceDayNightColorProvider(PureWhite, PaperWhite),
    onErrorContainer = GlanceDayNightColorProvider(SundayRed, SundayRed),
    background = GlanceDayNightColorProvider(Parchment, PureBlack),
    onBackground = GlanceDayNightColorProvider(NearBlackInk, PaperWhite),
    surface = GlanceDayNightColorProvider(PureWhite, NearBlackTile1),
    onSurface = GlanceDayNightColorProvider(NearBlackInk, PaperWhite),
    surfaceVariant = GlanceDayNightColorProvider(Parchment, NearBlackTile2),
    onSurfaceVariant = GlanceDayNightColorProvider(MutedInk48, SoftChipGray),
    outline = GlanceDayNightColorProvider(DividerGray, SoftChipGray),
    inverseOnSurface = GlanceDayNightColorProvider(PaperWhite, NearBlackInk),
    inverseSurface = GlanceDayNightColorProvider(NearBlackTile1, PaperWhite),
    inversePrimary = GlanceDayNightColorProvider(SkyLinkBlue, ActionBlue),
    widgetBackground = GlanceDayNightColorProvider(PureWhite, NearBlackTile1),
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
