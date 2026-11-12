# Backend & Cost Architecture: Limit Crosses

Official Pricing Checked Date: **November 12, 2026**

## 1. Provider Comparison & Official Offerings

| Provider | Free Tier Limits | Overages / Paid Plans | Inactivity Policy | Client Credential Risk |
| :--- | :--- | :--- | :--- | :--- |
| **Google Firebase** | **Spark Plan ($0/mo)**:<br>• 50,000 Firestore reads/day<br>• 20,000 Firestore writes/day<br>• 1 GB Firestore storage<br>• 10 GB/month egress<br>• 50,000 monthly active users (Email/OAuth auth) | **Blaze Plan (Pay-as-you-go)**:<br>• Retains free quotas<br>• Reads: ~$0.03–$0.06 / 100k<br>• Writes: ~$0.09–$0.18 / 100k<br>• Storage: ~$0.15 / GiB-mo<br>• Functions: $0.40 / 1M calls | No inactivity pause on Spark, but hard cutoffs trigger once daily caps are exceeded. | Public Web/Android `google-services.json` API keys identify project only; server rules must guard data. Service accounts must never be bundled. |
| **Supabase** | **Free Tier ($0/mo)**:<br>• 2 active projects<br>• 500 MB Postgres database<br>• 1 GB file storage<br>• 5 GB/month bandwidth<br>• 50,000 MAUs<br>• 500,000 Edge Function calls/month | **Pro Plan ($25/mo)**:<br>• 8 GB Postgres database<br>• 100 GB storage<br>• 250 GB bandwidth<br>• Daily automated backups<br>• No project pausing | **7-day inactivity pause**: Free tier projects with zero requests for 7 consecutive days are paused until unpaused via dashboard. | Uses `anon` public key with Row Level Security (RLS). `service_role` master secret must never be bundled in client APK. |
| **Appwrite** | **Cloud Free Tier ($0/mo)**:<br>• 2 projects per org<br>• 75,000 MAUs<br>• 2 GB storage<br>• 5 GB bandwidth<br>• 750,000 function calls<br>• 250,000 DB reads/writes | **Pro Plan ($25/mo)**:<br>• Unlimited projects<br>• Scaled quotas<br>• **Self-Hosting (100% Free)**:<br>• Docker/VPS deployment with no user or storage caps | **7-day inactivity pause**: Free projects pause after 7 days without API requests. Deleted if untouched for 90 days. | Client-side API keys with document-level permissions. Master API keys strictly server-side. |

---

## 2. Selected Architecture: Dual Mode (Local/Demo & Cloud Sync)

To ensure **zero vendor lock-in**, **100% privacy resilience**, and **zero cost for self-discipline users**, Limit Crosses implements a dual-mode integration layer:

1. **Local / Demo Mode (Default, Zero Cost, 100% Offline)**
   - All user records, invite hashes, locked limits, audit logs, and usage histories are stored on-device using Android SharedPreferences and encrypted state tables.
   - Enforces all authoritative server-side rules in `LocalMockBackendEngine`:
     * Public registration as Administrator is strictly rejected.
     * Admin bootstrap requires the deployment master key (`LIMIT-CROSS-ADMIN-BOOTSTRAP-2026`).
     * Users cannot modify daily limits once locked.
     * Puzzles must be solved consecutively before extra time can be requested.
     * Admins can approve, deny, or adjust minutes with audit reasons.
   - **Cost**: \$0.00 forever. Zero external server dependencies.

2. **Cloud Sync Mode (Enterprise / Multi-Device)**
   - Configurable at runtime in **Settings → Backend Connectivity**.
   - Accepts a secure REST endpoint (Supabase, Firebase, or custom Appwrite server).
   - Operates strictly with unprivileged client tokens.
   - **Never embeds service accounts, master API keys, or database administrative passwords into the APK**.

---

## 3. Projected Future Costs at Scale

| Active Users | Estimated Monthly Cost (Firebase Blaze) | Estimated Monthly Cost (Supabase Pro) | Estimated Monthly Cost (Self-Hosted Appwrite) |
| :--- | :--- | :--- | :--- |
| **1 – 1,000** | **\$0.00** (Within free daily tiers) | **\$0.00** (With weekly ping) or \$25 | **\$5.00** (Single VPS) |
| **10,000** | **\$12 – \$25 / month** (Firestore reads/writes) | **\$25.00 / month** (Pro plan base) | **\$10 – \$20 / month** (VPS) |
| **50,000+** | **\$60 – \$140 / month** | **\$25 + Compute Addons (~$40)** | **\$40 / month** (Dedicated VM) |

Sources Verified:
- Google Firebase Pricing & Limits Documentation (2026)
- Supabase Pricing Matrix & Inactivity Terms (2026)
- Appwrite Cloud & Open Source Documentation (2026)

