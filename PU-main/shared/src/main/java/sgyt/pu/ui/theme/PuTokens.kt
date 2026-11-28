package sgyt.pu.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale for the PU native UI system.
 *
 * Structural gaps — screen margins, card padding, section separation — come from this scale so
 * rhythm stays identical on every screen. Hairline nudges that align text inside one block stay
 * local to that block; they are not part of the scale.
 */
@Immutable
data class PuSpacing(
    /** 4 dp — icon-to-label micro gaps. */
    val xs: Dp = 4.dp,
    /** 8 dp — tight internal gaps. */
    val sm: Dp = 8.dp,
    /** 12 dp — compact card and list gaps. */
    val md: Dp = 12.dp,
    /** 16 dp — screen margin and default control padding. */
    val base: Dp = 16.dp,
    /** 24 dp — section and card separation. */
    val lg: Dp = 24.dp,
    /** 32 dp — major group separation. */
    val xl: Dp = 32.dp,
    /** Room the floating bottom bar needs so it never covers content. */
    val bottomBarClearance: Dp = 88.dp,
)

val LocalPuSpacing = staticCompositionLocalOf { PuSpacing() }

/**
 * Room the floating bottom bar occupies at the bottom of the screen.
 *
 * The app shell publishes this so every screen clears the bar by the same amount, and screens
 * without a bar fall back to a normal bottom margin.
 */
val LocalPuBottomBarClearance = compositionLocalOf { 24.dp }

/**
 * Status colours.
 *
 * Material 3 has no success/warning pair, but PU needs three signal meanings that keep the same
 * reading in both themes: ready, warning and danger. Each pair is a container plus the text
 * colour that is legible on it.
 */
@Immutable
data class PuSignalColors(
    val readyContainer: Color,
    val onReadyContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

val LocalPuSignalColors = staticCompositionLocalOf {
    PuSignalColors(
        readyContainer = Color(0xFFB9E4C6),
        onReadyContainer = Color(0xFF0A3123),
        warningContainer = Color(0xFFE5C379),
        onWarningContainer = Color(0xFF11281F),
        dangerContainer = Color(0xFFE8B8B1),
        onDangerContainer = Color(0xFF7A1B10),
    )
}

/**
 * Colours for an in-progress indicator.
 *
 * Honey is the accent for progress, but the pale honey reads as a fill — it is too light to be a
 * thin bar against a pale track. The progress fill is therefore the deeper tone of the same amber
 * family, which keeps the accent's identity while making the bar legible against its track.
 *
 * The bar is drawn on two different surfaces, and an indicator has to read against whichever one
 * it sits on. The standard pair suits a pale card; the primary pair suits the deep forest card
 * that carries a download or an optimization.
 */
@Immutable
data class PuProgressColors(
    /** The moving part of an indicator on a standard surface. */
    val fill: Color,
    /** The empty part behind it on a standard surface. */
    val track: Color,
    /** The moving part of an indicator on the primary card. */
    val onPrimaryFill: Color,
    /** The empty part behind it on the primary card. */
    val onPrimaryTrack: Color,
)

val LocalPuProgressColors = staticCompositionLocalOf {
    PuProgressColors(
        fill = Color(0xFF94690C),
        track = Color(0xFFE4ECE5),
        onPrimaryFill = Color(0xFFE5C379),
        onPrimaryTrack = Color(0xFFF6F4EB).copy(alpha = 0.20f),
    )
}
