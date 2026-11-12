# Limit Crosses

> **Mindful Digital Boundaries — Transparent, Unified Social Usage Control**

Limit Crosses is a modern native Android application built from scratch to help individuals and accountability partners establish firm, respectful boundaries around social media usage.

---

## 🌟 Key Architecture & Features

- **Unified Combined Daily Allowance**:
  - Enforces a single combined daily limit (0–45 minutes total) across **Facebook**, **Instagram**, and **Snapchat** (NOT 45 minutes each).
- **Daily Limit Locking**:
  - Once confirmed by the user, the daily limit is locked to encourage genuine commitment. Modifications require Administrator approval and generate an immutable audit log entry.
- **5-Level Deterministic Cognitive Verification**:
  - To request additional time, users must solve 5 consecutive deterministic logic/pattern puzzles. Any failure immediately resets the progress back to Level 1.
  - Accessibility alternatives with textual reasoning hints are supported.
- **Admin & Invite Hierarchy**:
  - **Admin Creation**: Public users cannot self-register as Admin. Admin creation is strictly guarded by the deployment Master Bootstrap Key (`LIMIT-CROSS-ADMIN-BOOTSTRAP-2026`).
  - **User Onboarding**: Users join solely via cryptographically generated Admin invitation codes or deep links (`limitcrosses://invite?code=...`).
- **Data Dignity & Transparent Privacy**:
  - Measures only foreground runtime of the 3 specified packages via `UsageStatsManager`.
  - **Zero collection** of passwords, chats, camera, contacts, or browsing history.
- **Learn Cosmos**:
  - Independent educational portal featuring verified astronomy from **NASA** and **ESA** (Universe, Galaxies, Stars, Black Holes, Planets, Missions).
- **Dual Mode (Demo / Local & Cloud Sync)**:
  - 100% functional offline out-of-the-box with `LocalMockBackendEngine`.
  - Connects to external cloud REST APIs with zero bundled secrets.
- **Bilingual Localization**:
  - Complete native support for **English** and **Hindi (हिंदी)** across all UI strings.
- **Configurable App Name**:
  - Managed from a single location (`res/values/strings.xml` and `BuildConfig.APP_NAME`).

---

## 🛠️ Technology Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose & Material 3
- **Navigation**: Jetpack Navigation Compose
- **Architecture**: Clean Architecture + MVVM + Repository Pattern
- **Dependency Injection**: Koin 3.5.6 (Kotlin-native, zero-annotation overhead)
- **Asynchronous Execution**: Kotlin Coroutines & StateFlow
- **Platform APIs**:
  - `android.app.usage.UsageStatsManager` (Foreground app time)
  - `android.net.VpnService` (Local DNS resolution filtering)
  - `androidx.browser.customtabs` (NASA & ESA secure external viewer)
  - `android.app.admin.DeviceAdminReceiver` (Optional enterprise mode)

---

## 🚀 Build Instructions

### Prerequisites
1. **JDK 21 LTS** (e.g. Eclipse Temurin 21)
2. **Android SDK** (API 26 to 35 / 37)
3. Windows PowerShell or Unix Shell

### Environment Configuration
Set your `JAVA_HOME` and Android SDK path:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
```

### Clean Build & Unit Tests
```powershell
# Perform a clean build
.\gradlew.bat clean

# Compile debug APK
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat test
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Emulator Testing Instructions

### Recommended Emulator Target
- **Device**: Medium Phone / Pixel 8
- **API Level**: Android 14 (API 34) or Android 15/16 (API 35/37)
- **Architecture**: x86_64

### Launching the Emulator & Installing
```powershell
# 1. Start Emulator
& "C:\Users\<user>\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_37.0

# 2. Wait for device boot
& "C:\Users\<user>\AppData\Local\Android\Sdk\platform-tools\adb.exe" wait-for-device

# 3. Install APK
& "C:\Users\<user>\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"

# 4. Launch Limit Crosses
& "C:\Users\<user>\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.limitcrosses.app/.MainActivity
```

---

## 📱 Emulator Testing & Empirical Validation Results

Tested and verified on live Android Emulator (**Android 16 Baklava Preview / API 37**, `sdk_gphone64_x86_64`):

| Test Scenario | Action Performed | Result | Status |
|---|---|---|---|
| **Clean Gradle Build** | `.\gradlew.bat clean` & `assembleDebug` | Output `app-debug.apk` (19.4 MB) | **PASSED** |
| **Unit Test Suite** | `.\gradlew.bat test` (49 Gradle tasks) | 12/12 test cases passed (`testDebugUnitTest`, `testReleaseUnitTest`) | **PASSED** |
| **Admin Bootstrap** | Input master key `LIMIT-CROSS-ADMIN-BOOTSTRAP-2026` | Admin account activated; navigated to Administrator Console | **PASSED** |
| **Invite Generation** | Generated invite `LC-APA8-U4P8` with 30m default cap | Cryptographic invite code created and persisted | **PASSED** |
| **User Onboarding** | Used code `LC-APA8-U4P8` to register `Alice User` | Invite verified, account created, linked optional handles | **PASSED** |
| **Permission Checklist** | Real-time `UsageAccess` & `Notifications` status | Live detection updated from `Missing` to `Granted` upon granting | **PASSED** |
| **Limit Locking** | Confirmed 30 min daily allowance | Allowance locked; user cannot change without Admin approval | **PASSED** |
| **Main Dashboard** | Rendered allowance countdown, meters, best-effort badge | Accurately displayed 30 min remaining and FB/IG/Snap targets | **PASSED** |
| **Cognitive Verification** | Solved 5-level deterministic logic sequence puzzle | Radio selections, deterministic evaluations, reset-on-failure | **PASSED** |
| **Learn Cosmos** | Filtered categories (Universe, Galaxies, Stars) | Custom Tabs launched NASA/ESA verified research URLs | **PASSED** |
| **Orientation Adaptation** | Rotated device to Landscape (`user_rotation 1`) | UI adapted smoothly across orientation change with zero crashes | **PASSED** |
| **Process Recreation** | Forced app termination (`am force-stop`) & relaunch | App restored user session, locked limit, and usage records | **PASSED** |
| **Localization Switch** | Toggled language to Hindi (हिंदी) in Settings | Dynamic UI localization applied cleanly without process restart | **PASSED** |

### Verified Screen Artifacts
The following screen captures were pulled directly from the running emulator:
- `welcome_screen.png`: Welcome landing screen with role-aware onboarding
- `admin_dashboard.png`: Administrator console with invite generation and tabs
- `permissions_screen.png`: Real-time permission health checklist
- `limit_setup_screen.png`: Daily limit setup slider with 0–45m enforcement
- `dashboard_screen.png`: Main mindful usage dashboard with per-app tracking
- `puzzle_screen.png`: 5-level deterministic cognitive challenge
- `cosmos_screen.png`: Learn Cosmos educational explorer
- `landscape_screen.png`: Responsive UI adaptation under landscape rotation
- `settings_screen.png`: Profile, privacy, and sign-out settings
- `hindi_screen.png`: Bilingual Hindi localization (हिंदी)

---

## 🔐 Admin Bootstrap Process

1. Launch **Limit Crosses**.
2. On the **Welcome Screen**, tap **"Initial Admin Bootstrap"**.
3. Fill in your Administrator details:
   - Full Name: `System Administrator`
   - Email: `admin@limitcrosses.org`
   - Password: `<secure-password>`
   - Master Bootstrap Secret: `LIMIT-CROSS-ADMIN-BOOTSTRAP-2026`
4. Tap **"Activate Admin Profile"**.
5. Once inside the **Administrator Console**, you can:
   - Generate user invitation codes (assigning default limits and expiry).
   - Review and adjust user limits with required audit reasons.
   - Approve, deny, or modify pending extra-time extension requests.
   - Inspect the immutable system audit log.

---

## 📄 Documentation Index

- [Support Matrix & OS Compatibility](SUPPORT_MATRIX.md)
- [Privacy, Data Flow & Retention](PRIVACY_AND_DATA_FLOW.md)
- [Backend Pricing & Quotas](COST_AND_BACKEND.md)
- [Threat Model & Limitations](THREAT_MODEL_AND_LIMITATIONS.md)

