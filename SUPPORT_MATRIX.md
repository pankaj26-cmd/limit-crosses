# Feature Support & Android OS Compatibility Matrix

## 1. Operating System Compatibility

| Android Version | API Level | Native Usage Tracking (`UsageStatsManager`) | Notification Alerts | Local DNS Inspection (`VpnService`) | Status | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Android 8.0 - 12L** | 26 - 32 | Supported | Supported | Supported | Fully Supported | Core functionality supported. Baseline minimum SDK is API 26 (Android 8.0 Oreo). |
| **Android 13** | 33 | Supported | Supported (Runtime permission) | Supported | Fully Supported | Requires runtime `android.permission.POST_NOTIFICATIONS` permission check. |
| **Android 14** | 34 | Supported | Supported | Supported | Fully Supported | Foreground service type `specialUse` declared for local DNS inspection. |
| **Android 15** | 35 | Supported | Supported | Supported | Fully Supported | 16 KB page alignment and updated edge-to-edge support verified. |
| **Android 16 (Preview / Baklava)** | 36 / 37 | Supported | Supported | Supported | Fully Supported | Target SDK compatibility verified. |

---

## 2. Target Application Support Matrix

| Application | Package Identifier | Supported Trackers | Minimum Interval | Limitations |
| :--- | :--- | :--- | :--- | :--- |
| **Facebook** | `com.facebook.katana` | Foreground App & Local Web (`facebook.com`) | 1 second / 1 minute increments | Background service activity without foreground window is excluded from usage limit. |
| **Instagram** | `com.instagram.android` | Foreground App & Local Web (`instagram.com`) | 1 second / 1 minute increments | In-app browser views count toward Instagram foreground duration. |
| **Snapchat** | `com.snapchat.android` | Foreground App & Local Web (`snapchat.com`) | 1 second / 1 minute increments | Camera preview states count as active foreground usage. |

---

## 3. Enforcement Mode Comparison

| Mode | Target Audience | Circumvention Resistance | Technical Mechanism | Root / MDM Required? |
| :--- | :--- | :--- | :--- | :--- |
| **Consumer Self-Discipline Mode (Default)** | Individuals, Families, Accountability Partners | **Best-Effort**<br>(Respectful barrier; user can theoretically revoke permissions or uninstall) | • UsageStatsManager foreground polling<br>• High-priority limit alerts<br>• Immediate overlay & foreground redirect<br>• Local DNS resolution filter | **No** (Standard Android APK) |
| **Device Owner / Enterprise Mode (Documented)** | Managed Corporate Devices, School Tablets | **Tamper-Resistant**<br>(Cannot uninstall, cannot revoke permissions, cannot clear data) | • Android Enterprise Device Policy Manager (`DevicePolicyManager`)<br>• Policy package suspension (`setPackagesSuspended`)<br>• Disallow uninstall (`DISALLOW_UNINSTALL`) | **Yes** (Requires factory-reset QR code or ADB provisioning: `dpm set-device-owner`) |

---

## 4. First Release Rule Compliance

- **Native App Tracking**: Primary, production-grade tracking via Android `UsageStatsManager`.
- **Local DNS Inspection**: Implemented via local `VpnService` with zero content decryption and zero remote upload. Transparently marked as "Best Effort Local DNS Monitoring".
- **Browser Protection Notice**: Users are explicitly notified that web browser tracking is best-effort and does not inspect HTTPS encrypted payloads.

