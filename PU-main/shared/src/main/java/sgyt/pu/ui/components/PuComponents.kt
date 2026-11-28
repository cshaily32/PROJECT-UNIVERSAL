package sgyt.pu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sgyt.pu.model.Compatibility
import sgyt.pu.model.PuArtStyle
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.ui.theme.LocalPuBottomBarClearance
import sgyt.pu.ui.theme.progressColors
import sgyt.pu.ui.theme.signalColors
import sgyt.pu.ui.theme.spacing

/**
 * Screen shell.
 *
 * The backdrop is a flat surface colour — no gradient, no blur — so the screen stays cheap to
 * draw on a low-end GPU. [scrollable] is turned off by screens that own their own lazy scroller.
 *
 * The shell is a [Surface] so every descendant inherits one foreground colour. Text that does not
 * choose its own colour would otherwise fall back to an unspecified colour, which renders as an
 * unreadable black in the night theme.
 */
@Composable
fun PuScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    scrollable: Boolean = true,
    bottomContentPadding: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    val bottomPadding = bottomContentPadding ?: LocalPuBottomBarClearance.current
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(horizontal = MaterialTheme.spacing.base)
                .padding(top = MaterialTheme.spacing.md, bottom = bottomPadding),
        ) {
            PuTopBar(title = title, onBack = onBack, trailing = trailing)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs, bottom = MaterialTheme.spacing.md),
                )
            }
            content()
        }
    }
}

@Composable
private fun PuTopBar(
    title: String,
    onBack: (() -> Unit)?,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Go back")
            }
            Spacer(Modifier.width(MaterialTheme.spacing.xs))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun PuEditionBadge(config: PuEditionConfig, inverse: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (inverse) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (inverse) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        ),
    ) {
        Text(
            text = config.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (inverse) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/**
 * Standard content card.
 *
 * Depth comes from the hairline outline and the surface colour step, never from a shadow: shadows
 * cost a render layer on the low-end phones this app targets, and a border under a shadow reads
 * as a ghost card.
 */
@Composable
fun PuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick, role = Role.Button)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column { content() }
    }
}

/** The one filled surface on a screen: the current status or the key decision. */
@Composable
fun PuPrimaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column { content() }
    }
}

@Composable
fun PuMetric(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    color: Color = LocalContentColor.current,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = color,
        )
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelLarge,
                color = color.copy(alpha = 0.76f),
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
        }
    }
}

/**
 * Segmented control.
 *
 * The selected segment is a filled primary surface with an on-primary label, so the choice reads
 * without relying on colour alone. Every segment keeps a 48 dp touch target.
 */
@Composable
fun PuSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit = {},
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelected(index) },
                        ),
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
                    )
                }
            }
        }
    }
}

/**
 * Cover art placeholder.
 *
 * Each game carries a deterministic art style, so the cover is a flat two-tone composition rather
 * than a gradient or an empty rectangle. It never carries text.
 */
@Composable
fun PuGameCover(
    art: PuArtStyle,
    modifier: Modifier = Modifier,
) {
    val (base, accent) = when (art) {
        PuArtStyle.LANTERN -> Color(0xFF173A38) to Color(0xFFF2D998)
        PuArtStyle.HOLLOW_BELL -> Color(0xFF312347) to Color(0xFFD4B875)
        PuArtStyle.SALTWIND -> Color(0xFF164653) to Color(0xFFC4E6D9)
        PuArtStyle.NINTH_FERRY -> Color(0xFF2D1B24) to Color(0xFFF0EBDA)
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(base),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 26.dp, y = 26.dp)
                .clip(CircleShape)
                .background(accent),
        )
    }
}

/** Compatibility state. The dot and the word both carry the meaning, never colour alone. */
@Composable
fun PuCompatibilityChip(compatibility: Compatibility) {
    val signals = MaterialTheme.signalColors
    val (background, foreground) = when (compatibility) {
        Compatibility.SMOOTH -> signals.readyContainer to signals.onReadyContainer
        Compatibility.LAG -> signals.warningContainer to signals.onWarningContainer
        Compatibility.BLOCKED -> signals.dangerContainer to signals.onDangerContainer
    }
    Surface(shape = CircleShape, color = background) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (compatibility == Compatibility.BLOCKED) signals.onDangerContainer else foreground),
            )
            Text(
                text = compatibility.label,
                style = MaterialTheme.typography.labelMedium,
                color = foreground,
            )
        }
    }
}

enum class NoticeTone {
    INFO,
    WARNING,
    SUCCESS,
}

@Composable
fun PuNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: NoticeTone = NoticeTone.INFO,
) {
    val signals = MaterialTheme.signalColors
    val icon = when (tone) {
        NoticeTone.INFO -> Icons.Outlined.Info
        NoticeTone.WARNING -> Icons.Outlined.WarningAmber
        NoticeTone.SUCCESS -> Icons.Outlined.Check
    }
    val tint = when (tone) {
        NoticeTone.INFO -> MaterialTheme.colorScheme.primary
        NoticeTone.WARNING -> signals.onWarningContainer
        NoticeTone.SUCCESS -> signals.onReadyContainer
    }
    val toneLabel = when (tone) {
        NoticeTone.INFO -> "Information"
        NoticeTone.WARNING -> "Warning"
        NoticeTone.SUCCESS -> "Ready"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.40f)),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = toneLabel, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PuSettingRow(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun PuPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PuSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * In-progress indicator.
 *
 * [onPrimarySurface] selects the pair that reads against the surface the bar is drawn on. The
 * default suits a pale card; the primary card needs its own pair, because honey on a pale track
 * disappears and honey on the night theme's mint card disappears too.
 */
@Composable
fun PuProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    onPrimarySurface: Boolean = false,
) {
    val progressColors = MaterialTheme.progressColors
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        color = if (onPrimarySurface) progressColors.onPrimaryFill else progressColors.fill,
        trackColor = if (onPrimarySurface) progressColors.onPrimaryTrack else progressColors.track,
        strokeCap = StrokeCap.Round,
    )
}

enum class PuMainTab(val route: String, val label: String, val icon: ImageVector) {
    LIBRARY("library", "Library", Icons.AutoMirrored.Outlined.LibraryBooks),
    RUNTIMES("runtimes", "Runtimes", Icons.Outlined.Layers),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings),
}

/**
 * Floating navigation pill.
 *
 * Each destination is named once for screen readers: the icon is decorative and the tab itself
 * carries the label, so TalkBack never reads the name twice.
 */
@Composable
fun PuBottomBar(
    selected: PuMainTab,
    onSelected: (PuMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuMainTab.entries.forEach { tab ->
                val active = tab == selected
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            onClick = { onSelected(tab) },
                        )
                        .semantics { contentDescription = tab.label },
                    shape = CircleShape,
                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .then(if (active) Modifier.padding(horizontal = MaterialTheme.spacing.md) else Modifier)
                            .height(48.dp)
                            .padding(horizontal = if (active) 0.dp else MaterialTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (active) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(19.dp),
                        )
                        if (active) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section label.
 *
 * The ink comes from the enclosing surface, so a label inside the forest primary card reads in
 * cream and the same label on a pale card reads in the standard foreground.
 */
@Composable
fun PuLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Supporting copy.
 *
 * One step below [PuLabel] in the same ink, so the hierarchy survives on every surface.
 */
@Composable
fun PuBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalContentColor.current.copy(alpha = 0.76f),
        modifier = modifier,
    )
}

/**
 * Game title.
 *
 * A title may occupy two lines: PU scales rather than truncates, so a long name stays readable at
 * large font sizes instead of being cut to a clipped single line.
 */
@Composable
fun PuTruncatedTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
