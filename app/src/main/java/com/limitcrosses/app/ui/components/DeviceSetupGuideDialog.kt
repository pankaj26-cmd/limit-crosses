package com.limitcrosses.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.limitcrosses.app.ui.theme.*

enum class AndroidBrand(
    val brandId: String,
    val displayName: String,
    val osName: String,
    val badge: String,
    val steps: List<String>,
    val tips: List<String>
) {
    XIAOMI(
        brandId = "xiaomi",
        displayName = "Xiaomi / Redmi / POCO",
        osName = "MIUI 14 & Xiaomi HyperOS",
        badge = "HyperOS / MIUI",
        steps = listOf(
            "Tap 'Open App Info' below to open Limit Crosses system settings.",
            "If settings are greyed out, tap the top-right 3-dots (⋮) or (!) icon and select 'Allow restricted settings' (enter device lock screen PIN/password).",
            "Under App Info, toggle 'Autostart' to ON (crucial for HyperOS/MIUI background screen time tracking).",
            "Under 'Other permissions', grant 'Display pop-up windows while running in background' and 'Show on Lock screen'.",
            "Scroll to 'Battery saver' and select 'No restrictions'.",
            "Return to Limit Crosses, tap 'Open Usage Settings' -> find 'Limit Crosses' -> toggle 'Permit usage access' ON (accept the 10-second warning countdown by ticking the checkbox and tapping OK)."
        ),
        tips = listOf(
            "MIUI & HyperOS will kill background limit services unless 'Autostart' is enabled and Battery Saver is set to 'No restrictions'.",
            "In Recent Apps overview, press and hold the Limit Crosses card and tap the Padlock icon to lock it in RAM."
        )
    ),

    VIVO(
        brandId = "vivo",
        displayName = "Vivo / iQOO",
        osName = "Funtouch OS & OriginOS",
        badge = "Funtouch OS",
        steps = listOf(
            "Tap 'Open App Info' below.",
            "Tap the top-right 3-dots (⋮) or Gear icon -> select 'Allow restricted settings' -> verify your lock screen PIN or fingerprint.",
            "Under App Info, tap 'Permissions' -> enable 'Floating window' / 'Display over other apps'.",
            "Go to Settings -> Battery -> Background power consumption management -> select Limit Crosses -> choose 'High background power usage' (or 'Don't restrict').",
            "Return to Limit Crosses -> tap 'Open Usage Settings' -> select 'Limit Crosses' -> toggle 'Permit usage access' ON."
        ),
        tips = listOf(
            "Selecting 'High background power usage' prevents Vivo's aggressive task killer from terminating the social limit blocker."
        )
    ),

    OPPO_REALME(
        brandId = "oppo_realme",
        displayName = "OPPO / Realme",
        osName = "ColorOS & Realme UI",
        badge = "ColorOS / Realme UI",
        steps = listOf(
            "Tap 'Open App Info' below.",
            "Tap the top-right three dots (⋮) -> tap 'Allow restricted settings' -> authenticate with your device PIN or fingerprint.",
            "Under App Info -> tap 'Battery usage' -> toggle ON both 'Allow background activity' and 'Allow auto-launch'.",
            "Tap 'Open Overlay Settings' in Limit Crosses -> locate Limit Crosses -> toggle 'Allow display over other apps' to ON.",
            "Tap 'Open Usage Settings' -> select 'Limit Crosses' -> toggle 'Permit usage access' ON."
        ),
        tips = listOf(
            "ColorOS requires both 'Allow background activity' and 'Allow auto-launch' enabled so the daily limit enforcer can run seamlessly."
        )
    ),

    ONEPLUS(
        brandId = "oneplus",
        displayName = "OnePlus",
        osName = "OxygenOS 13 / 14 / 15",
        badge = "OxygenOS",
        steps = listOf(
            "Tap 'Open App Info' below.",
            "Tap the top-right 3-dots (⋮) menu -> select 'Allow restricted settings' -> authenticate with lock screen PIN or fingerprint.",
            "Go to Battery -> App battery management -> turn ON 'Allow background activity' and 'Allow auto-launch'.",
            "Return to Limit Crosses -> tap 'Open Usage Settings' -> select Limit Crosses -> toggle 'Permit usage access' ON.",
            "Tap 'Open Overlay Settings' -> turn ON 'Display over other apps'."
        ),
        tips = listOf(
            "In Recent Apps, swipe down or tap the 3 dots on the Limit Crosses card and choose 'Lock' to keep it active in RAM."
        )
    ),

    MOTOROLA(
        brandId = "motorola",
        displayName = "Motorola",
        osName = "Hello UI / MyUX / Moto Android",
        badge = "Hello UI / MyUX",
        steps = listOf(
            "Tap 'Open App Info' below.",
            "Tap the 3-dots menu (⋮) in the top-right corner -> select 'Allow restricted settings' -> confirm screen lock PIN or biometric.",
            "Tap 'App battery usage' -> select 'Unrestricted'.",
            "Return to Limit Crosses -> tap 'Open Usage Settings' -> locate Limit Crosses -> turn ON 'Permit usage access'.",
            "Tap 'Open Overlay Settings' -> toggle 'Allow display over other apps' to ON."
        ),
        tips = listOf(
            "Motorola's clean system unlocks all restricted settings as soon as 'Allow restricted settings' is authenticated."
        )
    ),

    NOTHING(
        brandId = "nothing",
        displayName = "Nothing Phone",
        osName = "Nothing OS 2.0 / 2.5 / 3.0",
        badge = "Nothing OS",
        steps = listOf(
            "Tap 'Open App Info' below.",
            "Tap the 3 dots (⋮) in the top-right corner -> select 'Allow restricted settings' -> enter your device PIN or fingerprint.",
            "Return to Limit Crosses -> tap 'Open Usage Settings' -> select Limit Crosses -> turn ON 'Permit usage access'.",
            "Tap 'Open Overlay Settings' -> turn ON 'Allow display over other apps'.",
            "Under App Info -> Battery -> select 'Unrestricted' for continuous background screen time enforcement."
        ),
        tips = listOf(
            "Nothing OS shows a brief toast confirming 'Restricted settings allowed for Limit Crosses'."
        )
    ),

    SAMSUNG(
        brandId = "samsung",
        displayName = "Samsung Galaxy",
        osName = "One UI 5 / 6 / 7",
        badge = "One UI",
        steps = listOf(
            "If settings are greyed out: tap 'Open App Info' -> tap the 3 dots (⋮) at top-right -> tap 'Allow restricted settings'.",
            "Under App Info -> tap 'Appear on top' -> toggle to 'Allowed'.",
            "Under App Info -> tap 'Battery' -> change from 'Optimized' to 'Unrestricted'.",
            "Go to Settings -> Battery -> Background usage limits -> check 'Never sleeping apps' and add Limit Crosses to the list.",
            "Return to Limit Crosses -> tap 'Open Usage Settings' -> turn ON 'Permit usage access'."
        ),
        tips = listOf(
            "Adding Limit Crosses to Samsung's 'Never sleeping apps' prevents One UI from freezing the background daily limit watcher."
        )
    ),

    PIXEL(
        brandId = "pixel",
        displayName = "Google Pixel / Stock",
        osName = "Stock Android 13, 14 & 15",
        badge = "Stock Android",
        steps = listOf(
            "When Android shows 'Restricted setting: For your security, this setting is currently unavailable', tap OK.",
            "Tap 'Open App Info' below.",
            "Tap the 3 dots (⋮) in the top-right corner of App Info.",
            "Tap 'Allow restricted settings' and verify your PIN or fingerprint.",
            "Return to Limit Crosses -> tap 'Open Usage Settings' -> toggle ON 'Permit usage access'.",
            "Tap 'Open Overlay Settings' -> toggle ON 'Allow display over other apps'."
        ),
        tips = listOf(
            "On stock Android, sideloaded APKs always require unlocking restricted settings before Usage Access is allowed."
        )
    );

    companion object {
        fun detectCurrentDevice(): AndroidBrand {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val brand = Build.BRAND.lowercase()
            return when {
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") || brand.contains("poco") -> XIAOMI
                manufacturer.contains("vivo") || manufacturer.contains("iqoo") || brand.contains("iqoo") -> VIVO
                manufacturer.contains("oppo") || manufacturer.contains("realme") || brand.contains("realme") -> OPPO_REALME
                manufacturer.contains("oneplus") -> ONEPLUS
                manufacturer.contains("motorola") || manufacturer.contains("moto") -> MOTOROLA
                manufacturer.contains("nothing") -> NOTHING
                manufacturer.contains("samsung") -> SAMSUNG
                manufacturer.contains("google") -> PIXEL
                else -> PIXEL
            }
        }
    }
}

@Composable
fun DeviceSetupGuideDialog(
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    val detectedBrand = remember { AndroidBrand.detectCurrentDevice() }
    var selectedBrand by remember { mutableStateOf(detectedBrand) }

    val currentManufacturer = remember { Build.MANUFACTURER.replaceFirstChar { it.uppercase() } }
    val currentModel = remember { Build.MODEL }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = CosmicCard,
            border = BorderStroke(1.dp, CosmicCardBorder),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CosmicWarning.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CosmicWarning, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Device Setup Guide",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                            Text(
                                text = "Restricted Settings & Permissions",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CosmicTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detected Device Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicSurface,
                    border = BorderStroke(1.dp, CosmicSecondary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = CosmicSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detected: $currentManufacturer $currentModel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmicSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedBrand == detectedBrand) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CosmicSuccess.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "Auto-Selected",
                                    color = CosmicSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brand Selector Horizontal Chips
                Text(
                    text = "SELECT YOUR PHONE BRAND:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AndroidBrand.entries.forEach { brand ->
                        val isSelected = (brand == selectedBrand)
                        val isDetected = (brand == detectedBrand)

                        Surface(
                            onClick = { selectedBrand = brand },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CosmicPrimary else CosmicSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CosmicPrimary else if (isDetected) CosmicSecondary else CosmicCardBorder
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isDetected) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else CosmicSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = brand.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else CosmicTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Instructions Content Area (Scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Brand Badge & OS Header
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CosmicSurface,
                        border = BorderStroke(1.dp, CosmicCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedBrand.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = CosmicTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CosmicPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = selectedBrand.badge,
                                        color = CosmicPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "System: ${selectedBrand.osName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "STEP-BY-STEP SETUP INSTRUCTIONS:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    selectedBrand.steps.forEachIndexed { index, step ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CosmicSurface,
                            border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CosmicSecondary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CosmicSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextPrimary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (selectedBrand.tips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "BRAND-SPECIFIC RELIABILITY TIPS:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicWarning
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        selectedBrand.tips.forEach { tip ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CosmicWarning.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, CosmicWarning.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = CosmicWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmicTextPrimary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val secondaryBtnContentColor = if (MaterialTheme.colorScheme.background.red > 0.5f) Color.White else Color.Black
                    Button(
                        onClick = onOpenAppInfo,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicSecondary,
                            contentColor = secondaryBtnContentColor
                        )
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = secondaryBtnContentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open App Info", color = secondaryBtnContentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onOpenUsageSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Usage Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

