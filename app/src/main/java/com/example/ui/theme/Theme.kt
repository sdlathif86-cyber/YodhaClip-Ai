package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val YodhaDarkColorScheme = darkColorScheme(
    primary = RoyalViolet,
    onPrimary = Color.White,
    primaryContainer = DeepVioletContainer,
    onPrimaryContainer = SoftLavender,
    secondary = ElectricAmethyst,
    onSecondary = Color.White,
    secondaryContainer = HighPlumSurface,
    onSecondaryContainer = RadiantPurple,
    tertiary = CyberMagenta,
    onTertiary = ObsidianBlack,
    tertiaryContainer = Color(0xFF3B154A),
    onTertiaryContainer = Color(0xFFF5D0FE),
    background = ObsidianBlack,
    onBackground = StudioInkPrimary,
    surface = MidnightPlum,
    onSurface = StudioInkPrimary,
    surfaceVariant = ElevatedPlum,
    onSurfaceVariant = StudioInkSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = ObsidianDeep,
    surfaceContainer = MidnightPlum,
    surfaceContainerHigh = ElevatedPlum,
    surfaceContainerHighest = HighPlumSurface,
    outline = VioletOutline,
    outlineVariant = VioletOutlineSubtle,
    error = RoseAlert,
    onError = Color.White,
    errorContainer = RoseAlertContainer,
    onErrorContainer = Color(0xFFFFD9E2)
)

val YodhaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun YodhaClipTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YodhaDarkColorScheme,
        typography = YodhaTypography,
        shapes = YodhaShapes,
        content = content
    )
}
