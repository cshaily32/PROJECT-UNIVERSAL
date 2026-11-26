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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sgyt.pu.model.Compatibility
import sgyt.pu.model.PuArtStyle
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.ui.theme.PuPalette

private val ScreenGradient = Brush.verticalGradient(
    colors = listOf(PuPalette.BackgroundLight, PuPalette.Background),
)

@Composable
fun PuScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 112.dp),
    ) {
        PuTopBar(title = title, onBack = onBack, trailing = trailing)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        }
        content()
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
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
                modifier = Modifier.size(42.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Go back")
                }
            }
            Spacer(Modifier.width(12.dp))
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

@Composable
fun PuGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
        shadowElevation = 2.dp,
    ) {
        Column { content() }
    }
}

@Composable
fun PuPrimaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
    ) {
        Column { content() }
    }
}

@Composable
fun PuMetric(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    color: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp),
            color = color,
        )
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelLarge,
                color = color.copy(alpha = 0.72f),
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
        }
    }
}

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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelected(index) },
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun PuGameCover(
    art: PuArtStyle,
    modifier: Modifier = Modifier,
) {
    val colors = when (art) {
        PuArtStyle.LANTERN -> listOf(Color(0xFFF2D998), Color(0xFF173A38), Color(0xFF6E9B75))
        PuArtStyle.HOLLOW_BELL -> listOf(Color(0xFFD4B875), Color(0xFF312347), Color(0xFF654C82))
        PuArtStyle.SALTWIND -> listOf(Color(0xFFC4E6D9), Color(0xFF164653), Color(0xFF287381))
        PuArtStyle.NINTH_FERRY -> listOf(Color(0xFFF0EBDA), Color(0xFF2D1B24), Color(0xFF5C3844))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors)),
    )
}

@Composable
fun PuCompatibilityChip(compatibility: Compatibility) {
    val (background, foreground) = when (compatibility) {
        Compatibility.SMOOTH -> PuPalette.Accent to PuPalette.OnAccent
        Compatibility.LAG -> PuPalette.Honey.copy(alpha = 0.42f) to PuPalette.Foreground
        Compatibility.BLOCKED -> PuPalette.Destructive.copy(alpha = 0.12f) to PuPalette.Destructive
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
                    .background(if (compatibility == Compatibility.BLOCKED) PuPalette.Destructive else PuPalette.Primary),
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
    val icon = when (tone) {
        NoticeTone.INFO -> Icons.Outlined.Info
        NoticeTone.WARNING -> Icons.Outlined.WarningAmber
        NoticeTone.SUCCESS -> Icons.Outlined.Check
    }
    val tint = when (tone) {
        NoticeTone.INFO -> MaterialTheme.colorScheme.primary
        NoticeTone.WARNING -> PuPalette.Honey
        NoticeTone.SUCCESS -> PuPalette.Accent
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.52f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
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
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            .height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
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
            .height(50.dp),
        shape = CircleShape,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PuProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        color = PuPalette.Honey,
        trackColor = Color.White.copy(alpha = 0.16f),
    )
}

enum class PuMainTab(val route: String, val label: String, val icon: ImageVector) {
    LIBRARY("library", "Library", Icons.Outlined.LibraryBooks),
    RUNTIMES("runtimes", "Runtimes", Icons.Outlined.Layers),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings),
}

@Composable
fun PuBottomBar(
    selected: PuMainTab,
    onSelected: (PuMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuMainTab.entries.forEach { tab ->
                val active = tab == selected
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelected(tab) },
                    shape = CircleShape,
                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .then(if (active) Modifier.padding(horizontal = 14.dp) else Modifier)
                            .height(44.dp)
                            .padding(horizontal = if (active) 0.dp else 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
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

@Composable
fun PuLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun PuBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun PuTruncatedTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
