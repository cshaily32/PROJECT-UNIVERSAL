package sgyt.pu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sgyt.pu.ui.theme.signalColors
import sgyt.pu.ui.theme.spacing
import sgyt.pu.model.PuEditionConfig
import sgyt.pu.model.PuSampleData
import sgyt.pu.ui.components.NoticeTone
import sgyt.pu.ui.components.PuBody
import sgyt.pu.ui.components.PuEditionBadge
import sgyt.pu.ui.components.PuCard
import sgyt.pu.ui.components.PuLabel
import sgyt.pu.ui.components.PuMetric
import sgyt.pu.ui.components.PuNotice
import sgyt.pu.ui.components.PuPrimaryButton
import sgyt.pu.ui.components.PuPrimaryCard
import sgyt.pu.ui.components.PuScreen
import sgyt.pu.ui.components.PuSegmentedControl
import sgyt.pu.ui.components.PuSettingRow
import sgyt.pu.ui.model.PuDeviceFact
import sgyt.pu.ui.model.PuDeviceFactStatus
import sgyt.pu.ui.model.PuDeviceIcon
import sgyt.pu.ui.model.editionLabel
import sgyt.pu.ui.model.label
import sgyt.pu.ui.viewmodels.PuDeviceCheckState
import sgyt.pu.ui.viewmodels.PuDeviceCheckViewModel

@Composable
fun DeviceCheckScreen(
    config: PuEditionConfig,
    viewModel: PuDeviceCheckViewModel,
    onContinue: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PuDeviceCheckScreenContent(
        state = state,
        config = config,
        onContinue = onContinue,
    )
}

@Composable
private fun PuDeviceCheckScreenContent(
    state: PuDeviceCheckState,
    config: PuEditionConfig,
    onContinue: () -> Unit,
) {
    PuScreen(
        title = "PU",
        trailing = { PuEditionBadge(config) },
    ) {
        PuBody("Device check")
        Text(
            text = "Check this phone",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
        )
        PuBody(
            "PU reads the processor, memory, Android version and graphics support before it recommends a profile.",
            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
        )

        when (state.screenState) {
            PuDeviceCheckState.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PuDeviceCheckState.ScreenState.Error -> PuNotice(
                text = state.errorMessage ?: "PU could not read this phone's details.",
                tone = NoticeTone.WARNING,
            )

            PuDeviceCheckState.ScreenState.Success -> {
                val recommendation = state.recommendation
                PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Recommended edition",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = state.snapshot?.tier?.label ?: "—",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                            )
                        }
                        PuMetric(
                            value = recommendation?.edition?.editionLabel ?: "—",
                            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                        )
                        Text(
                            text = recommendation?.reason
                                ?: "PU could not work out a recommendation for this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                        )
                        PuBody(
                            text = "Recommended profile: ${recommendation?.profile?.displayName ?: "—"}",
                            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                        )
                    }
                }

                state.facts.chunked(2).forEach { rowFacts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        rowFacts.forEach { fact ->
                            PuDeviceFact(
                                fact = fact,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                PuNotice(
                    text = deviceNotice(state),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                )
            }
        }

        PuPrimaryButton(
            text = "Continue to library",
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            onClick = onContinue,
        )
    }
}

private fun deviceNotice(state: PuDeviceCheckState): String {
    val warnings = state.facts.filter { it.status == PuDeviceFactStatus.WARNING }
    return if (warnings.isEmpty()) {
        "This phone meets the requirements for Ren'Py and RPG Maker games."
    } else {
        "Check ${warnings.joinToString(", ") { it.label.lowercase() }} before a game needs it."
    }
}

@Composable
private fun PuDeviceFact(
    fact: PuDeviceFact,
    modifier: Modifier = Modifier,
) {
    val passed = fact.status == PuDeviceFactStatus.PASS
    val signals = MaterialTheme.signalColors
    PuCard(modifier = modifier) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                    Icon(
                        imageVector = puDeviceIcon(fact.icon),
                        contentDescription = null,
                        modifier = Modifier.padding(MaterialTheme.spacing.md).size(18.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                // The pill carries the status as a word in [detail] plus this icon; the colour
                // follows the ready/warning pair so the tick stays legible on its own fill.
                Surface(
                    shape = CircleShape,
                    color = if (passed) signals.readyContainer else signals.warningContainer,
                ) {
                    Icon(
                        imageVector = if (passed) Icons.Outlined.Check else Icons.Outlined.WarningAmber,
                        contentDescription = if (passed) "Passed" else "Warning",
                        tint = if (passed) signals.onReadyContainer else signals.onWarningContainer,
                        modifier = Modifier.padding(MaterialTheme.spacing.xs).size(14.dp),
                    )
                }
            }
            PuBody(fact.label, modifier = Modifier.padding(top = MaterialTheme.spacing.md))
            Text(
                text = fact.value,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PuBody(fact.detail, modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
        }
    }
}

private fun puDeviceIcon(icon: PuDeviceIcon): ImageVector = when (icon) {
    PuDeviceIcon.PROCESSOR -> Icons.Outlined.PhoneAndroid
    PuDeviceIcon.ANDROID -> Icons.Outlined.Security
    PuDeviceIcon.MEMORY -> Icons.Outlined.WarningAmber
    PuDeviceIcon.STORAGE -> Icons.Outlined.Storage
    PuDeviceIcon.GRAPHICS -> Icons.Outlined.Build
    PuDeviceIcon.WEBVIEW -> Icons.Outlined.SportsEsports
}

@Composable
fun SavesScreen(
    config: PuEditionConfig,
    onBack: () -> Unit,
) {
    var cloud by remember { mutableStateOf(config.cloudSyncEnabled) }
    var backedUp by remember { mutableStateOf(false) }

    PuScreen(
        title = "Saves and backup",
        onBack = onBack,
    ) {
        PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Backup destination",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                        modifier = Modifier.weight(1f),
                    )
                    PuEditionBadge(config, inverse = true)
                }
                PuMetric(
                    value = if (cloud) "Cloud" else "Local",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                )
                Text(
                    text = if (config.cloudSyncEnabled) {
                        "Cloud sync keeps saves available when you move to another phone."
                    } else {
                        "PU Lite keeps a local backup on this phone."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            }
        }

        if (config.cloudSyncEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PuLabel("Save location")
                    PuBody("Choose where new backups are stored", modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
                }
                PuSegmentedControl(
                    options = listOf("Local", "Cloud"),
                    selectedIndex = if (cloud) 1 else 0,
                    onSelected = { cloud = it == 1 },
                )
            }
        }

        PuLabel("Game saves", modifier = Modifier.padding(top = MaterialTheme.spacing.lg))
        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.sm)) {
            Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                listOf("Quick save", "Slot 1", "Slot 2").forEach { name ->
                    PuSettingRow(
                        icon = Icons.Outlined.Save,
                        title = name,
                        detail = "Current slot",
                    )
                }
            }
        }

        PuNotice(
            text = "Backups use the same save format in PU Lite and PU Plus, so a save can move between editions.",
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
        )

        PuPrimaryButton(
            text = if (backedUp) "Backup complete" else "Back up saves",
            icon = if (backedUp) Icons.Outlined.Check else if (cloud) Icons.Outlined.Cloud else Icons.Outlined.Save,
            enabled = !backedUp,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            onClick = { backedUp = true },
        )
    }
}

@Composable
fun TranslationScreen(
    config: PuEditionConfig,
    onBack: () -> Unit,
) {
    var enabled by remember { mutableStateOf(true) }

    PuScreen(
        title = "Translation overlay",
        onBack = onBack,
    ) {
        PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (config.translationEnabled) "Offline translation" else "PU Plus feature",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                        modifier = Modifier.weight(1f),
                    )
                    PuEditionBadge(config, inverse = true)
                }
                PuMetric(
                    value = if (config.translationEnabled) "OCR" else "Offline translation",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                )
                Text(
                    text = if (config.translationEnabled) {
                        "PU recognizes text on the game screen and translates it without sending an image to a server."
                    } else {
                        "Offline OCR translation is included in PU Plus and is not available in PU Lite."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            }
        }

        if (config.translationEnabled) {
            PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
                Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                    PuSettingRow(
                        icon = Icons.Outlined.Translate,
                        title = "Show translation overlay",
                        detail = "Recognize visible game text while the game runs",
                        trailing = { Switch(checked = enabled, onCheckedChange = { enabled = it }) },
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.Translate,
                        title = "Source language",
                        detail = "Detect the language from the game screen",
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.Translate,
                        title = "Translate into",
                        detail = "Choose the language shown on the overlay",
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.Save,
                        title = "Language pack",
                        detail = "Download the language model for offline use",
                    )
                }
            }
            PuNotice(
                text = "Translation runs on this phone. Captured game images are never uploaded.",
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            )
            PuPrimaryButton(
                text = "Save translation settings",
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            )
        } else {
            PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
                Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                    PuSettingRow(
                        icon = Icons.Outlined.Translate,
                        title = "Translate on-screen text",
                        detail = "Runs offline and keeps game images on this phone",
                        trailing = { PuEditionBadge(config) },
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.Translate,
                        title = "Choose translation language",
                        detail = "Available in PU Plus",
                        trailing = { PuEditionBadge(config) },
                    )
                }
            }
            PuNotice(
                text = "PU Lite keeps the same screen and shows Plus features as quiet rows instead of interrupting play.",
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            )
        }
    }
}

@Composable
fun BackgroundScreen(
    config: PuEditionConfig,
    onBack: () -> Unit,
) {
    var charging by remember { mutableStateOf(config.backgroundWorkEnabled) }
    var lowBattery by remember { mutableStateOf(true) }
    var hot by remember { mutableStateOf(true) }

    PuScreen(
        title = "Background work",
        onBack = onBack,
    ) {
        PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.xs)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Background work",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                        modifier = Modifier.weight(1f),
                    )
                    PuEditionBadge(config, inverse = true)
                }
                PuMetric(
                    value = if (config.backgroundWorkEnabled) "Charging" else "Off while idle",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.md),
                )
                Text(
                    text = if (config.backgroundWorkEnabled) {
                        "PU Plus can optimize new assets while the phone is charging and no game is running."
                    } else {
                        "PU Lite does not run background services while the app is idle."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            }
        }

        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
            Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                if (config.backgroundWorkEnabled) {
                    PuSettingRow(
                        icon = Icons.Outlined.WarningAmber,
                        title = "Optimize while charging",
                        detail = "Continue only while the phone is connected to power",
                        trailing = { Switch(checked = charging, onCheckedChange = { charging = it }) },
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.WarningAmber,
                        title = "Pause on low battery",
                        detail = "Stop background work below the battery level you choose",
                        trailing = { Switch(checked = lowBattery, onCheckedChange = { lowBattery = it }) },
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.Thermostat,
                        title = "Pause when hot",
                        detail = "Protect the phone during long optimization runs",
                        trailing = { Switch(checked = hot, onCheckedChange = { hot = it }) },
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.SportsEsports,
                        title = "Stop while a game runs",
                        detail = "Never optimize assets during active play",
                        trailing = { Icon(Icons.Outlined.Check, contentDescription = null) },
                    )
                } else {
                    PuSettingRow(
                        icon = Icons.Outlined.WarningAmber,
                        title = "Optimize while idle",
                        detail = "Not available in PU Lite",
                        trailing = { PuEditionBadge(config) },
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.Thermostat,
                        title = "Pause when hot",
                        detail = "Applies while optimization is open in the app",
                        trailing = { Switch(checked = true, enabled = false, onCheckedChange = {}) },
                    )
                    PuSettingRow(
                        icon = Icons.Outlined.WarningAmber,
                        title = "Pause on low battery",
                        detail = "Protects the phone during an optimization run",
                        trailing = { Switch(checked = true, enabled = false, onCheckedChange = {}) },
                    )
                }
            }
        }

        PuNotice(
            text = "Background optimization is incremental. New or changed assets are processed without repeating completed work.",
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
        )

        if (config.backgroundWorkEnabled) {
            PuPrimaryButton(
                text = "Save background settings",
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            )
        }
    }
}

@Composable
fun SettingsScreen(
    config: PuEditionConfig,
    onOpenWindows: () -> Unit,
    onOpenTranslation: () -> Unit,
    onOpenBackground: () -> Unit,
    onOpenSaves: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenDeviceCheck: () -> Unit,
) {
    var selectedProfile by remember { mutableStateOf(config.defaultProfile) }
    var pauseWhenHot by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(true) }

    PuScreen(
        title = "Settings",
        trailing = { PuEditionBadge(config) },
    ) {
        PuLabel("Performance profile")
        PuSampleData.profiles.forEach { option ->
            val selected = option.profile == selectedProfile
            PuCard(
                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                onClick = { selectedProfile = option.profile },
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.spacing.base),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(22.dp),
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(MaterialTheme.spacing.xs),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PuLabel(option.profile.displayName, modifier = Modifier.weight(1f))
                            if (option.recommended && !config.backgroundWorkEnabled) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary) {
                                    Text(
                                        text = "Recommended",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
                                    )
                                }
                            }
                        }
                        PuBody(option.detail, modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
                    }
                }
            }
        }

        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
            Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                PuSettingRow(
                    icon = Icons.Outlined.Thermostat,
                    title = "Pause optimizing when hot",
                    detail = "Keep the phone cool and protect the battery",
                    trailing = { Switch(checked = pauseWhenHot, onCheckedChange = { pauseWhenHot = it }) },
                )
                PuSettingRow(
                    icon = Icons.Outlined.Wifi,
                    title = "Download on Wi-Fi only",
                    detail = "Runtime and module downloads stay on Wi-Fi",
                    trailing = { Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it }) },
                )
                PuSettingRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Device check",
                    detail = "Review processor, memory, Android and graphics support",
                    onClick = onOpenDeviceCheck,
                )
                PuSettingRow(
                    icon = Icons.Outlined.SportsEsports,
                    title = "Touch overlay and gamepad",
                    detail = "Map controls for keyboard-only games",
                    onClick = onOpenControls,
                )
                PuSettingRow(
                    icon = Icons.Outlined.Save,
                    title = "Save backup",
                    detail = if (config.cloudSyncEnabled) "Cloud or local sync" else "Local backup",
                    onClick = onOpenSaves,
                )
                PuSettingRow(
                    icon = Icons.Outlined.Tune,
                    title = "Compatibility fixes",
                    detail = "Download profiles and apply them offline",
                )
                PuSettingRow(
                    icon = Icons.Outlined.Cloud,
                    title = "Save sync",
                    detail = "Move saves between devices",
                    onClick = if (config.cloudSyncEnabled) onOpenSaves else null,
                )
                PuSettingRow(
                    icon = Icons.Outlined.Translate,
                    title = "Translation overlay",
                    detail = "Translate on-screen text offline with OCR",
                    onClick = onOpenTranslation,
                )
                PuSettingRow(
                    icon = Icons.Outlined.WarningAmber,
                    title = "Background optimization",
                    detail = if (config.backgroundWorkEnabled) "Optimize while charging and no game is running" else "Not available in PU Lite",
                    onClick = if (config.backgroundWorkEnabled) onOpenBackground else null,
                )
                PuSettingRow(
                    icon = Icons.Outlined.Security,
                    title = "Windows games module",
                    detail = if (config.windowsIncluded) "Included and ready" else "Optional download",
                    onClick = onOpenWindows,
                )
            }
        }

        PuCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        PuLabel("This phone")
                        PuBody("First-launch device check", modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                        Text(
                            text = "PU Lite recommended",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    listOf(
                        "RAM" to "3 GB",
                        "Build" to "arm64",
                        "Android" to "14",
                    ).forEach { (label, value) ->
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = MaterialTheme.spacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                PuBody(label)
                                PuLabel(value, modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
                            }
                        }
                    }
                }
            }
        }

        PuPrimaryCard(modifier = Modifier.padding(top = MaterialTheme.spacing.base)) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.base)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = config.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (config.edition.name == "PLUS") "4 GB+" else "3 GB",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    )
                }
                Text(
                    text = if (config.edition.name == "PLUS") {
                        "Best quality and every Plus feature"
                    } else {
                        "Stable play with the smallest footprint"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
                Text(
                    text = if (config.edition.name == "PLUS") {
                        "Balanced optimization, Windows games included, save sync and offline translation."
                    } else {
                        "Low optimization by default, optional Windows module, local backups, and no background work."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            }
        }
    }
}
