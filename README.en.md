# Pelvic Trainer (盆底肌训练)

[简体中文](./README.md)

A voice-first **Kegel training app for men**: put on headphones and follow spoken cues — "tighten, hold, relax" — with no screen-staring and no mental counting. Train through 6 progressive levels; every session is logged automatically.

| Today | Progress | Settings |
|---|---|---|
| ![Today tab: one-tap session start](./docs/screenshots/home-zh-light.png) | ![Progress tab: endurance and quick-control stats](./docs/screenshots/ability-zh-light.png) | ![Settings tab: theme and language](./docs/screenshots/settings-zh-light.png) |

## The problem it solves

The hard part of Kegel (pelvic floor) training is not the exercise — it's consistency. Typical apps make you watch a countdown and count reps in your head, which is fiddly and distracting. Here the audio carries the whole session: the voice says "tighten", "hold 3, 2, 1", "relax", and you just follow it — sitting or standing, on the commute or between meetings.

## Who it's for

- Men who want to start (or keep) Kegel training without being tied to the screen
- Anyone who prefers explicit level targets instead of guessing seconds and reps
- Privacy-minded users who want their data to stay on the phone

## Core features

- **One-tap daily training**: an endurance + quick-control script generated from your current level, fully voice-guided (Chinese / English audio)
- **Optional ability assessment**: 5 items (movement control, endurance hold, endurance reps, quick reps, relaxation quality), all with built-in timers, countdowns and audio cues — no manual counting; suggests a training level L1–L6
- **Six-level standards**: every level has exact hold / rest / rep values, viewable anytime; upgrades require several dimensions to pass together
- **Full control mid-session**: pause, resume, end early, safety exit (stop immediately on pain); a low-key mode runs silent with subtitles and countdown only
- **Daily budget**: 1/2/3 reminder sessions per day (3 auto-reduces volume), daily contractions capped at 40, plus a half-dose switch
- **Progress tab**: endurance and quick stats kept separate; training days, session count and level history at a glance
- **Light / dark / system theme, zh-CN / English**: unsupported system languages fall back to Chinese; switching never loses an in-progress session

## Quick start (build from source)

Requires JDK 17 and an Android SDK (compileSdk 36 / minSdk 31); Gradle Wrapper is included.

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug          # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 21 domain unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Audio cue assets can be regenerated offline on macOS: `bash tools/make_cues.sh`.

## Health boundary

- This is a **training aid and self-tracking tool**: no medical diagnosis, no promised therapeutic effects (including ED/PE); the in-app assessment is not a clinical strength test.
- If you experience pain, persistent abnormal tension, or notable urinary/bowel issues, use Safety Exit to stop and consult a doctor or pelvic-floor physiotherapist.
- All data stays in the app's private storage on your device. No network, no accounts.

## Project layout

| Path | Contents |
|---|---|
| `app/` | Android app (Kotlin 2.1.20 + Jetpack Compose + Media3; `com.orca.kegel`) |
| `docs/pm/` | Product plan and requirement source of truth (Product Plan V1.1) |
| `docs/design/` | Design contract: interaction spec, freeze notes, clickable prototype (`docs/design/prototype/`) |
| `docs/qa/` | Acceptance traceability matrix (34 ACs) and per-item evidence (`docs/qa/evidence-P045/`) |
| `docs/roles/`, `docs/handoff/` | Development-process governance docs (ORCA role cards and handoff log) |

## Current version

`v1.0` (versionCode 1) · Android 12+ · Simplified Chinese / English.
