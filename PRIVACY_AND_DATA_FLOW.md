# Privacy, Data Flow & Retention Policy: Limit Crosses

Last Updated: **November 12, 2026**

## 1. Executive Summary & Data Dignity
Limit Crosses is architected around **Data Dignity** and **Zero-Surveillance Accountability**. Unlike traditional parental control or tracking tools that spy on communications or harvest browsing habits, Limit Crosses exists solely to measure **aggregate foreground time** on 3 specific platforms (Facebook, Instagram, Snapchat) to empower mindful digital boundaries.

---

## 2. What Data Is Collected and Why

| Data Category | Specific Fields | Legal / Technical Basis | Purpose |
| :--- | :--- | :--- | :--- |
| **Monitored App Durations** | Seconds in foreground for `com.facebook.katana`, `com.instagram.android`, `com.snapchat.android`. | Android `UsageStatsManager` (Local user grant) | Essential for calculating daily combined allowance against the user-confirmed limit. |
| **Account Identification** | Full name, email address, salted SHA-256 password hash. | Contractual / User consent | Managing authentication, invitation associations, and multi-device cloud sync. |
| **Daily Limit Configuration** | Daily limit minutes (0–45), locked status timestamp. | User policy commitment | Enforcing the daily ceiling and preventing impulsive self-modifications. |
| **Extension Requests** | Requested minutes (1–30), constructive reason text, admin decision status. | User submission | Allowing structured time extensions reviewed by designated Administrators. |
| **Audit Log Entries** | Timestamp, action type, actor ID, brief descriptive text. | System integrity & accountability | Providing a transparent, immutable record of limit modifications and admin actions. |
| **Optional Social Handles** | Display usernames (e.g. `@username`) for Facebook, Instagram, Snapchat. | Optional user input | Visual profile recognition only. **Zero credentials or API access tokens are ever requested or stored.** |

---

## 3. What We NEVER Collect (Strict Negative Scope)

Limit Crosses technically and architecturally isolates itself from sensitive user data:
- ❌ **No Private Messages / Chats**: No access to WhatsApp, Messenger, DMs, or SMS.
- ❌ **No Social Media Passwords or Credentials**: Users never enter third-party passwords.
- ❌ **No Camera, Microphone, or Screen Capture**: No keystroke logging or screenshotting.
- ❌ **No Browsing History or Full URLs**: The local VPN service performs local DNS resolution checks only; it does not decrypt HTTPS or store visited websites.
- ❌ **No Contacts, Call Logs, or Phonebooks**.
- ❌ **No Precise GPS Location Data**.
- ❌ **No Unrelated Application Usage**: Banking, productivity, messaging, and system app durations are explicitly ignored and filtered out.

---

## 4. Administrator Visibility Matrix

Administrators hold accountability roles, but their visibility is strictly circumscribed:

| Data Element | Can Admin View? | Purpose & Safeguard |
| :--- | :--- | :--- |
| User Name & Email | **YES** | Identifying participants in the accountability network. |
| Assigned Daily Limit | **YES** | Reviewing and approving policy caps. |
| Total Combined Minutes Used Today | **YES** | Monitoring whether the user is within allowance. |
| Extension Requests & Reasons | **YES** | Evaluating temporary time requests. |
| Individual Chats / Posts / Photos | **NO (Technically Impossible)** | App has zero permissions or APIs to view content. |
| Full Web Browsing History | **NO (Technically Impossible)** | VPN operates locally without logging URLs or HTTPS payloads. |
| User Plaintext Passwords | **NO** | Passwords are salted and hashed with SHA-256 before storage. |

---

## 5. Retention Schedule & Permanent Deletion

1. **Daily Usage Records**:
   - Aggregated daily runtime totals are retained for **30 rolling days** for historical trend visualization.
   - On day 31, records are automatically and permanently purged.
2. **Account Deletion Guarantee**:
   - Users can invoke **Settings → Request Instant Account Deletion** at any time.
   - Deletion takes effect immediately:
     * User profile and credentials are permanently erased.
     * Locked limit settings and active extension requests are scrubbed.
     * In-memory and persisted storage keys are wiped.
     * An immutable audit log records the deletion event (`ACCOUNT_DELETED`) without retaining any personally identifiable data.

