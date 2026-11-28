package sgyt.pu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The light PU palette: pale sage backdrop, deep forest primary, mint and honey signals. */
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
    val OutlineStrong = Color(0x40113C2C)
}

/** The dark PU palette: the same world read at night, not an inverted light theme. */
object PuDarkPalette {
    val Background = Color(0xFF0E1512)
    val BackgroundLight = Color(0xFF0B110E)
    val Foreground = Color(0xFFE3EAE5)
    val Card = Color(0xFF161E1A)
    val Primary = Color(0xFF86D3A8)
    val OnPrimary = Color(0xFF06291B)
    val PrimaryContainer = Color(0xFF1D3A2C)
    val OnPrimaryContainer = Color(0xFFCFE8D8)
    val Secondary = Color(0xFF1F2A25)
    val OnSecondary = Color(0xFFD7E7DC)
    val Muted = Color(0xFF24322B)
    val OnMuted = Color(0xFFB3C1B8)
    val Accent = Color(0xFF23402F)
    val OnAccent = Color(0xFFB9E4C6)
    val Destructive = Color(0xFFFFB4AB)
    val Honey = Color(0xFFE5C379)
    val Outline = Color(0xFF3A4741)
    val OutlineStrong = Color(0xFF7A8A81)
}

private val PuLightColors = lightColorScheme(
    primary = PuPalette.Primary,
    onPrimary = PuPalette.OnPrimary,
    primaryContainer = PuPalette.Secondary,
    onPrimaryContainer = PuPalette.OnSecondary,
    secondary = PuPalette.Secondary,
    onSecondary = PuPalette.OnSecondary,
    secondaryContainer = PuPalette.Muted,
    onSecondaryContainer = PuPalette.OnMuted,
    tertiary = PuPalette.Honey,
    onTertiary = PuPalette.Foreground,
    tertiaryContainer = Color(0xFFF6E9C6),
    onTertiaryContainer = Color(0xFF5C4310),
    background = PuPalette.Background,
    onBackground = PuPalette.Foreground,
    surface = PuPalette.Card,
    onSurface = PuPalette.Foreground,
    surfaceVariant = PuPalette.Muted,
    onSurfaceVariant = PuPalette.OnMuted,
    surfaceContainerLowest = PuPalette.BackgroundLight,
    surfaceContainerLow = PuPalette.Card,
    surfaceContainer = PuPalette.Card,
    surfaceContainerHigh = PuPalette.Muted,
    surfaceContainerHighest = Color(0xFFD7E2DA),
    outline = PuPalette.OutlineStrong,
    outlineVariant = PuPalette.Outline,
    error = PuPalette.Destructive,
    onError = Color.White,
    errorContainer = Color(0xFFF6DEDB),
    onErrorContainer = Color(0xFF8E2418),
)

private val PuDarkColors = darkColorScheme(
    primary = PuDarkPalette.Primary,
    onPrimary = PuDarkPalette.OnPrimary,
    primaryContainer = PuDarkPalette.PrimaryContainer,
    onPrimaryContainer = PuDarkPalette.OnPrimaryContainer,
    secondary = PuDarkPalette.Secondary,
    onSecondary = PuDarkPalette.OnSecondary,
    secondaryContainer = PuDarkPalette.Muted,
    onSecondaryContainer = PuDarkPalette.OnMuted,
    tertiary = PuDarkPalette.Honey,
    onTertiary = PuPalette.Foreground,
    tertiaryContainer = Color(0xFF4A3D18),
    onTertiaryContainer = Color(0xFFF2DFA8),
    background = PuDarkPalette.Background,
    onBackground = PuDarkPalette.Foreground,
    surface = PuDarkPalette.Card,
    onSurface = PuDarkPalette.Foreground,
    surfaceVariant = PuDarkPalette.Muted,
    onSurfaceVariant = PuDarkPalette.OnMuted,
    surfaceContainerLowest = PuDarkPalette.BackgroundLight,
    surfaceContainerLow = Color(0xFF131A17),
    surfaceContainer = PuDarkPalette.Card,
    surfaceContainerHigh = PuDarkPalette.Muted,
    surfaceContainerHighest = Color(0xFF2B3830),
    outline = PuDarkPalette.OutlineStrong,
    outlineVariant = PuDarkPalette.Outline,
    error = PuDarkPalette.Destructive,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF4A1A14),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val PuSignalLight = PuSignalColors(
    readyContainer = Color(0xFFB9E4C6),
    onReadyContainer = Color(0xFF0A3123),
    warningContainer = Color(0xFFE5C379),
    onWarningContainer = Color(0xFF11281F),
    dangerContainer = Color(0xFFE8B8B1),
    onDangerContainer = Color(0xFF7A1B10),
)

private val PuSignalDark = PuSignalColors(
    readyContainer = Color(0xFF23402F),
    onReadyContainer = Color(0xFFB9E4C6),
    warningContainer = Color(0xFF4A3D18),
    onWarningContainer = Color(0xFFF2DFA8),
    dangerContainer = Color(0xFF4A1A14),
    onDangerContainer = Color(0xFFFFB4AB),
)

/** Light progress pair: deep amber fill on the pale sage track, honey on the forest card. */
private val PuProgressLight = PuProgressColors(
    fill = Color(0xFF94690C),
    track = Color(0xFFE4ECE5),
    onPrimaryFill = Color(0xFFE5C379),
    onPrimaryTrack = Color(0xFFF6F4EB).copy(alpha = 0.20f),
)

/** Dark progress pair: honey reads at full strength, and the forest card takes the deep amber. */
private val PuProgressDark = PuProgressColors(
    fill = Color(0xFFE5C379),
    track = Color(0xFF24322B),
    onPrimaryFill = Color(0xFF5C4310),
    onPrimaryTrack = Color(0xFF06291B).copy(alpha = 0.18f),
)

/**
 * Shape scale: covers stay tight, controls and cards stay soft, and anything large is a sheet.
 * Pills are reserved for small controls — chips, badges and segmented segments.
 */
private val PuShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
)

private val PuTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.1).sp,
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
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
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
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
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
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/** Spacing scale access, mirroring the MaterialTheme accessors. */
val MaterialTheme.spacing: PuSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalPuSpacing.current

/** Status colours access, mirroring the MaterialTheme accessors. */
val MaterialTheme.signalColors: PuSignalColors
    @Composable
    @ReadOnlyComposable
    get() = LocalPuSignalColors.current

/** Progress indicator colours access, mirroring the MaterialTheme accessors. */
val MaterialTheme.progressColors: PuProgressColors
    @Composable
    @ReadOnlyComposable
    get() = LocalPuProgressColors.current

@Composable
fun PuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPuSpacing provides PuSpacing(),
        LocalPuSignalColors provides if (darkTheme) PuSignalDark else PuSignalLight,
        LocalPuProgressColors provides if (darkTheme) PuProgressDark else PuProgressLight,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) PuDarkColors else PuLightColors,
            typography = PuTypography,
            shapes = PuShapes,
            content = content,
        )
    }
}
