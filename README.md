# KodaLang Android 📱

Native Android app for **KodaLang** — the AI English tutor for French speakers —
by **Techinfotics**. v1.0.0 MVP.

The app is **linked with [kodalang.com](https://kodalang.com)**: it talks to the
*same* Supabase project, so one account works everywhere and progress, streaks
and goals stay in sync between the website and the app.

## What's inside (MVP)

| Screen | What it does |
|---|---|
| **Splash** | Restores the saved Supabase session → Home, otherwise → Auth |
| **Auth** | Email + password sign-in / sign-up (email confirmation supported, friendly messages — never raw JSON) |
| **Home** | Greeting, CEFR + plan chips, daily-goal progress ring, streak, total XP, feature cards |
| **Practice** | Real AI speaking practice: chat with Koda via `POST /api/tutor-chat` (streamed), 🔊 per-message TTS via `POST /api/tutor-tts`, 402 limit → upgrade prompt, 401 → re-login |
| **Profile** | Email, open kodalang.com, sign out (account deletion stays on the website, as on the web app) |

**Honest deep-links:** *My Goal*, *Memory Health* and *Exam Simulator* open the
real pages on kodalang.com in a Custom Tab in this MVP — they are not fake
native UI. Native versions are on the roadmap below.

## Tech

- **Kotlin + Jetpack Compose** (Material 3), MVVM, Navigation Compose
- **supabase-kt 3.1.4** (`auth-kt`, `postgrest-kt`) — Auth + PostgREST against the shared project
- **OkHttp** for the web app's `/api/tutor-chat` + `/api/tutor-tts` routes (Supabase JWT as `Bearer` token)
- `applicationId = com.techinfotics.kodalang`, minSdk 26, targetSdk 34, version 1.0.0

## Setup

1. **Create `local.properties`** in the project root (it's git-ignored — never commit keys):
   ```bash
   cp local.properties.example local.properties
   ```
2. **Fill in your keys** in `local.properties`:
   ```properties
   SUPABASE_URL=https://tsvfdzkelmznxqlcvrjb.supabase.co
   SUPABASE_ANON_KEY=PASTE_YOUR_ANON_KEY_HERE
   API_BASE=https://kodalang.com
   ```
   Where to find the anon key:
   - your web project's `.env` → `VITE_SUPABASE_PUBLISHABLE_KEY`, **or**
   - Supabase dashboard → your project → *Project Settings → API* → **anon public** key.
3. **Open in Android Studio** (Koala or newer, JDK 17). It syncs Gradle automatically.
4. **Run** on an emulator or USB device, or **Build → Build App Bundle(s) / APK(s)** for a debug APK.

> First build downloads ~1 GB of Gradle/Android dependencies — normal.

## Backend contract (for maintainers)

- Supabase tables read: `profiles` (`display_name, cefr_level, native_language, daily_goal_minutes, total_xp, plan`), `daily_progress` (`day, minutes_practiced, xp_earned, lessons_completed`), `streaks` (`current_streak`), `subscriptions` (`status`). Column names match the web repo's generated `types.ts`.
- `POST {API_BASE}/api/tutor-chat` — header `Authorization: Bearer <supabase JWT>`, body `{ "messages": [{ "id", "role", "parts": [{ "type": "text", "text" }] }], "conversationId": "<uuid>" }`. The app creates the `conversations` row first (`{ user_id, title, scenario: "tutor||text|" }`). The response is an AI-SDK-v5 UI-message SSE stream; the client parses `text-delta` chunks defensively and falls back to raw text.
- `POST {API_BASE}/api/tutor-tts` — body `{ "text", "voiceId": "EXAVITQu4vr4xnSDxMaL" }` → `audio/mpeg` bytes, played with `MediaPlayer`.
- 401 → session expired (back to Auth). 402 → plan's conversation minutes used up (server's upgrade message is shown, with a link to `/pricing`).

## Roadmap

- [ ] Native **Goal** screen (goal-driven curriculum)
- [ ] Native **Memory Health** screen (forgetting-curve dashboard)
- [ ] Native **Exam Simulator** (TOEIC / IELTS)
- [ ] Voice input (mic → Whisper, like the web app)
- [ ] Push notifications for streaks & coaching nudges
- [ ] Release signing + Play Store listing

## License

© 2026 Techinfotics. All rights reserved.
