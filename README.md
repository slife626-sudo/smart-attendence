# ওয়াহীদিয়া হাজিরা (Wahidiya Attendance) — Native Android App

Native Kotlin + Jetpack Compose + Material Design 3 app for **ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী** attendance, break tracking, leave management, branch transfer, and admin analytics, backed by **Firebase Authentication (Google Sign-In)** and **Cloud Firestore real-time sync**.

## Cloud Sync & Multi-Device Architecture

- **Google Sign-In (`CredentialManager`)**: Every device authenticates with Google before accessing or syncing workspace data.
- **Real-Time Cloud Firestore Sync**: All staff, branches, shifts, holidays, leave types, leave applications, branch transfers, settings, attendance punches, and break logs sync live across any logged-in Android phone (database ID configured in `firebase_applet_config.xml`).
- **Multi-Device Workspace Sharing**:
  - The primary admin account (`slife626@gmail.com`) defaults to workspace `wahidiya_main`.
  - Other phones sign in with Google, copy their **Firebase User UID** from **অ্যাডমিন প্যানেল → সেটিংস**, and get added via **টিম সদস্য UID যোগ ও শেয়ার করুন**, then switch their Workspace ID to `wahidiya_main` for live multi-device sync.

## Importing Existing Neon PostgreSQL Data

1. Open your **Neon SQL Editor** and run this export query:

```sql
SELECT json_build_object(
  'settings', COALESCE((SELECT json_agg(t) FROM settings t), '[]'::json),
  'branches', COALESCE((SELECT json_agg(t) FROM branches t), '[]'::json),
  'shifts', COALESCE((SELECT json_agg(t) FROM shifts t), '[]'::json),
  'staff', COALESCE((SELECT json_agg(t) FROM staff t), '[]'::json),
  'holidays', COALESCE((SELECT json_agg(t) FROM holidays t), '[]'::json),
  'leave_types', COALESCE((SELECT json_agg(t) FROM leave_types t), '[]'::json),
  'leaves', COALESCE((SELECT json_agg(t) FROM leaves t), '[]'::json),
  'transfers', COALESCE((SELECT json_agg(t) FROM transfers t), '[]'::json),
  'attendance', COALESCE((SELECT json_agg(t) FROM attendance t), '[]'::json),
  'breaks', COALESCE((SELECT json_agg(t) FROM breaks t), '[]'::json)
) AS neon_export;
```

2. Save or copy the resulting JSON object (`neon_export`).
3. In the Android app, go to **অ্যাডমিন প্যানেল → সেটিংস → Neon PostgreSQL ডেটা ইমপোর্ট (.json)**, pick the `.json` file (or paste the JSON text), and tap **Firestore-এ ইমপোর্ট করুন**.

## Local Computer / Attendance Device Sync Bridge

Because all punches live in Cloud Firestore (database `ai-studio-android-attendan-c7921a1d-711f-4edf-b1b9-651fd7c7b48d`), a local PC or hardware attendance device server can push live punches using `firebase-admin` (see `/scripts/pc-device-bridge-example.js`). Every connected phone displays device punches in real time.

## Core Features

- **Staff Punch Kiosk (`হাজিরা`)**: Live Bangla clock, staff & PIN selector, real GPS geofence verification, optional/enforced ML Kit front-camera face verification, break tracking (`ব্রেক শুরু` / `ব্রেক শেষ`), and staff leave portal (`ছুটির আবেদন ও হিসাব`).
- **Admin Panel (`অ্যাডমিন প্যানেল`)**: 17 management & reporting sections including Dashboard KPIs/charts, daily/monthly/yearly reports, Android `PrintManager` PDF export, break analytics, manual punch/break correction, 8-sample ML Kit face enrollment, transfers, leave approvals, and cloud/Neon tools.
- **GitHub Actions APK Build**: Automated Android APK build workflow in `.github/workflows/build-apk.yml`.
