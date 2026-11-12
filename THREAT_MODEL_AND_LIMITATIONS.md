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

## 3. Best-Effort Enforcement Mechanism

When the daily combined limit reaches zero:
1. **Immediate Overlay & Foreground Prompt**:
   - `LimitReachedScreen` takes focus.
   - Shows remaining time = 0 and countdown to local midnight.
2. **Cognitive Gate**:
   - Instead of automatic frustration, users are prompted to solve 5 consecutive deterministic reasoning puzzles if they wish to request additional time from their Administrator.
   - Educational divergence is offered via **Learn Cosmos**, redirecting dopamine-seeking impulses toward astronomical science.
3. **Accountability Model**:
   - Limit Crosses acts as a mirror and commitment contract. Bypassing it by deleting the app or revoking permissions is recorded in the immutable audit log visible to the Administrator.

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

