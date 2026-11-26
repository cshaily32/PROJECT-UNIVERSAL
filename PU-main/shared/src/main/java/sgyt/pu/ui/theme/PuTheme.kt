package sgyt.pu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PuPalette {
    val Background = Color(0xFFECF2ED)
    val BackgroundLight = Color(0xFFF7F6EF)
    val Foreground = Color(0xFF11281F)
    val Card = Color(0xFFFCFCF9)
    val Primary = Color(0xFF113C2C)
    val OnPrimary = Color(0xFFF6F4EB)
    val Secondary = Color(0xFFDEEAE0)
    val OnSecondary = Color(0xFF103627)
    val Muted = Color(0xFFE4ECE5)
    val OnMuted = Color(0xFF57685E)
    val Accent = Color(0xFFB9E4C6)
    val OnAccent = Color(0xFF0A3123)
    val Destructive = Color(0xFFBA3728)
    val Honey = Color(0xFFE5C379)
    val Outline = Color(0x1F113C2C)
    val OutlineStrong = Color(0x33113C2C)
}

private val PuLightColors = lightColorScheme(
    primary = PuPalette.Primary,
    onPrimary = PuPalette.OnPrimary,
    primaryContainer = PuPalette.Secondary,
    onPrimaryContainer = PuPalette.OnSecondary,
    secondary = PuPalette.Secondary,
    onSecondary = PuPalette.OnSecondary,
    tertiary = PuPalette.Honey,
    onTertiary = PuPalette.Foreground,
    background = PuPalette.Background,
    onBackground = PuPalette.Foreground,
    surface = PuPalette.Card,
    onSurface = PuPalette.Foreground,
    surfaceVariant = PuPalette.Muted,
    onSurfaceVariant = PuPalette.OnMuted,
    outline = PuPalette.OutlineStrong,
    outlineVariant = PuPalette.Outline,
    error = PuPalette.Destructive,
    onError = Color.White,
)

private val PuTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun PuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PuLightColors,
        typography = PuTypography,
        content = content,
    )
}
