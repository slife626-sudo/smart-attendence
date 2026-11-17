# ওয়াহীদিয়া হাজিরা (Wahidiya Attendance) — Native Android App

Native Kotlin + Jetpack Compose + Material Design 3 rewrite of the **ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী** attendance, break tracking, leave management, branch transfer, and admin analytics system.

## Core Features Preserved

- **Staff Punch Kiosk (`হাজিরা`)**
  - Live Bangla clock synchronized with configured timezone (`Asia/Dhaka`).
  - Staff selector & saved PIN preference (`DataStore`), with daily status (`IN`, `OUT`, `Break` summary, face enrollment indicator).
  - Real GPS verification (`FusedLocationProviderClient` + `LocationManager` fallback), `MaxAccuracy` check, and Haversine geofence radius verification per branch (transfer-aware).
  - Optional/enforced Front-Camera Face Verification (`CameraX` + `ML Kit Face Detection`) with frontal pose and eyes-open liveness check.
  - Full Break tracking (`ব্রেক শুরু` / `ব্রেক শেষ`) preventing duplicate open breaks or `OUT` while on an active break.
  - Staff Leave Portal (`ছুটির আবেদন ও হিসাব`) with yearly pro-rata quota, probation rules, unpaid estimation, new leave submission, and cancellation of pending applications.

- **Admin Dashboard (`অ্যাডমিন প্যানেল`)**
  - Password-protected login (default password: `admin123`, configurable in Settings).
  - Global Month (`←` / `→`) and Branch (`সব শাখা` or individual branch) filters.
  - 17 management sections:
    1. `ড্যাশবোর্ড`: KPIs, daily attendance bar chart, status breakdown donut chart, branch bars, top 5 late & absent rankings.
    2. `আজকের হাজিরা`: Date-navigable daily status breakdown.
    3. `কর্মীর রিপোর্ট`: Full monthly breakdown per employee, yearly leave balance line, and Android `PrintManager` PDF/print export.
    4. `মাসিক সারসংক্ষেপ`: Branch-filtered monthly employee summary.
    5. `ব্রেক রিপোর্ট`: Date-range break analytics with per-staff expandable daily break logs.
    6. `বাৎসরিক সারসংক্ষেপ`: 12-month attendance matrix per employee.
    7. `ম্যানুয়াল / সংশোধন`: Manual IN/OUT entry, quick-now buttons, deletion, and daily break CRUD.
    8. `মুখ নিবন্ধন`: 8-sample front-camera face enrollment & deletion per employee.
    9. `কর্মী তালিকা`, `শাখা বদলি`, `ছুটির আবেদন`, `ছুটির ব্যালেন্স`, `ছুটির ধরন`, `সরকারি ছুটি`, `শিফট`, `শাখা`, and `সেটিংস`.

## Local Storage & Architecture Notes

- **Database**: Pre-seeded local SQLite database via **Room** (`AppDatabase`) populated on first launch with the branches, shifts, staff (`1234` default PIN), holidays, leave types, and sample records.
- **Offline Operation**: Rewritten from Neon PostgreSQL serverless RPC to on-device Room persistence; multi-device cloud sync is not enabled unless migrated to a remote backend.
- **Face Recognition**: Uses on-device ML Kit facial landmark geometry vectors instead of browser `face-api.js` neural descriptors; previously enrolled browser descriptors should be re-enrolled via `মুখ নিবন্ধন`.
