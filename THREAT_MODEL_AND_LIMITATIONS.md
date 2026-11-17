# Threat Model, Platform Restrictions & Best-Effort Enforcement

## 1. Transparency Principle: No Fake Security Claims

Standard Android sandboxing enforces strict application isolation. Third-party applications running in consumer user-space cannot unconditionally kill, modify, or permanently prevent another application from opening without utilizing privileged system frameworks (e.g., Device Owner / MDM).

Limit Crosses explicitly adheres to engineering honesty:
- **We NEVER claim "unbypassable blocking" in consumer mode.**
- **We NEVER claim "impossible uninstall" without enterprise device enrollment.**
- **All consumer blocking is formally labeled "Best-Effort Enforcement".**

---

## 2. Threat Vector Analysis & Circumvention Realities

| Attack / Bypass Vector | Consumer Mode Resilience | Technical Mitigation in Limit Crosses | Device Owner / Enterprise Mode Resilience |
| :--- | :--- | :--- | :--- |
| **Application Uninstallation** | **Vulnerable**<br>Any user can drag the app icon to "Uninstall" in standard Android. | Upon reinstallation, logging in immediately restores locked limits and audit records from the authoritative backend. | **Immune**<br>`DISALLOW_UNINSTALL` user restriction completely disables the uninstall button in Android Settings. |
| **Revoking Usage Access Permission** | **Vulnerable**<br>User can disable Usage Access in Settings. | • Real-time detection (`hasUsagePermission() == false`).<br>• Prominent warning banner displayed on dashboard.<br>• Never falsely claims "Tracking Active". | **Immune**<br>Device policy controller locks permission grants programmatically (`PERMISSION_GRANT_STATE_GRANTED`). |
| **Disabling Local VPN Service** | **Vulnerable**<br>User can disconnect VPN in Android QS tiles. | State flow transitions to `DISCONNECTED` or `PERMISSION_REVOKED` instantly; native app tracking continues uninterrupted. | **Immune**<br>`setAlwaysOnVpnPackage` locks VPN connection permanently with zero manual toggle. |
| **Modifying System Clock / Timezone** | **Resistant**<br>Attempts to reset timer by changing time. | `MidnightResetReceiver` catches `TIME_SET` and `TIMEZONE_CHANGED` broadcasts to recompute midnight epoch dynamically against UTC timestamps. | **Immune**<br>`DISALLOW_CONFIG_DATE_TIME` prevents user tampering with device time. |
| **Reinstall / Cache Clearing** | **Resistant**<br>Local cache cleared. | Authoritative limit configurations, active extensions, and invites reside on the cloud backend (or persistent cloud sync layer). | **Immune**<br>`DISALLOW_APPS_CONTROL` blocks clearing storage or force-stopping apps. |

---

## 3. Best-Effort Multi-Layer Enforcement Mechanism

Standard Android (consumer mode) blocks apps from killing other apps. To achieve resilient blocking across modern Android versions (Android 10 through Android 16), Limit Crosses implements a 4-layer enforcement stack:

1. **Layer 1: Accessibility Service (`LimitCrossesAccessibilityService`)**:
   - Listens to `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` with 0ms latency.
   - When the user launches `com.instagram.android`, `com.snapchat.android`, or `com.facebook.katana` while remaining allowance is 0:
     - Instantly executes `performGlobalAction(GLOBAL_ACTION_HOME)`, kicking the user out of the app.
     - Triggers the impassable system overlay.
     - Launches `MainActivity` directly into `LimitReachedScreen` (exempt from Background Activity Launch restrictions).

2. **Layer 2: Impassable System Overlay (`AppBlockerOverlayManager`)**:
   - Uses `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY`.
   - Draws a full-screen cosmic card directly over Instagram/Snapchat that intercepts 100% of touches.
   - Social media content underneath is completely inaccessible.

3. **Layer 3: Background Foreground Guard (`AppMonitorBlockerService`)**:
   - Continuous foreground service monitoring package usage.
   - Automatically drops out to home and raises high-priority heads-up alerts if a monitored app enters focus.

4. **Layer 4: Enterprise Device Owner Hardware Lock**:
   - Programmatically invokes `dpm.setPackagesSuspended(admin, arrayOf(pkg), true)`.
   - The OS kernel disables and greys out the icons on the launcher. Tapping them yields an OS-level prompt: *"App disabled by your administrator"*.

---

## 4. Optional Enterprise Device Owner Mode

For environments where strict, tamper-proof enforcement is required (e.g. corporate smartphones, student learning tablets), Limit Crosses includes a pre-configured `DeviceAdminReceiver`:

### Provisioning Procedure
Device Owner must be provisioned during initial device setup (on a factory-reset device or fresh setup wizard) before any user account is added:
```bash
# Provisioning via ADB (for developer / enterprise testing):
adb shell dpm set-device-owner com.limitcrosses.app/.platform.receiver.LimitCrossesDeviceAdminReceiver
```

### Capabilities When Enrolled as Device Owner
- `setPackagesSuspended(admin, arrayOf("com.facebook.katana", "com.instagram.android", "com.snapchat.android"), true)` disables target app launching at OS kernel level.
- `addUserRestriction(admin, UserManager.DISALLOW_UNINSTALL)` blocks app uninstallation.
- `addUserRestriction(admin, UserManager.DISALLOW_APPS_CONTROL)` blocks clearing app data or revoking permissions.

### Deprovisioning & Removal Procedure
```bash
# Removing Device Owner via ADB:
adb shell dpm remove-active-admin com.limitcrosses.app/.platform.receiver.LimitCrossesDeviceAdminReceiver
```
Or initiate a device factory reset via device settings.

