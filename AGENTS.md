# AGENTS.md — AI Recorder Android App

This file is read by Codex (and other coding agents) on every turn. Keep it
short, opinionated, and current.

## Project

**AI Recorder** for Moto-style Android. System-level voice recording with
an AI summary loop. The 6/7 SLT demo is the immediate target — see
`spec.md` for the full UX, `screens/` for reference renderings.

## Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Audio capture**: `MediaRecorder` in a foreground `Service`
- **Audio playback**: `androidx.media3:media3-exoplayer`
- **Notifications**: `MediaStyle` for the ongoing card
- **`minSdk` 28, `targetSdk` 34**

No RxJava. No Koin. No View-system XML for new screens.

## Build & deploy commands

```bash
# Build
./gradlew :app:assembleDebug

# Deploy to running emulator
android run --apks=app/build/outputs/apk/debug/app-debug.apk

# Capture current screen for visual diff
android screen capture --output=current.png

# Inspect live UI tree (helpful when Compose hierarchy is unclear)
android layout --pretty --output=hierarchy.json
```

The emulator is named `medium_phone` and is expected to be running. If it
isn't, run `android emulator start medium_phone`.

## Codebase conventions

- Package root: `com.moto.airecorder`
- One file per Compose screen, named `XxxScreen.kt`
- State holders: `XxxViewModel` (Hilt-injected)
- Domain models live in `com.moto.airecorder.domain`
- Demo-mode fallbacks live in `com.moto.airecorder.demo` and are wired in
  via a debug-flavor Hilt module so real implementations can replace them
  without touching the UI

## Hard product rules (do not deviate)

1. **Ringing ≠ paused.** A ringing call does *not* interrupt recording.
   The status capsule reads `REC · Recording continues / Audio still saving`
   until the user answers. See `spec.md` STEP-06.
2. **Answered = paused for privacy. Call audio is never recorded.** Capsule
   switches to a muted state: `REC paused · Call not recorded / Resumes
   after call`. After the call, recording auto-resumes. See STEP-07, STEP-07b.
3. **AOD shows live audio confidence, not controls.** REC + big timer +
   static waveform + "Audio saved". No buttons. See STEP-05.
4. **AI output is always traceable.** Every summary line and action item
   carries a `timestamp` anchor. Tap → transcript scrolls + audio scrubs.
   See STEP-11, STEP-13.
5. **AI progress is plain English.** Never expose model names. The six
   visible steps are: Enhancing audio · Transcribing · Separating speakers
   · Correcting names and terms · Writing summary · Extracting actions.
6. **Created tasks carry provenance.** Task metadata includes `source:
   AI Recording at MM:SS`. See STEP-12.
7. **Minimize ≠ stop. Exit Focus ≠ stop.** Only an explicit Stop ends a
   recording session. Focus Mode confirms with a bottom sheet. See F-D.

## Copy is final

Every string in `spec.md` is final copy. Don't rewrite it. If you think a
string is wrong, surface a note in your turn summary — don't change it
yourself.

## Visuals

- The only red is `#E5484D`, reserved for active REC state.
- Saved / success uses `#1F8A5B`.
- Paused-during-call uses a muted gold `#B08A3C`.
- Don't add gradients, glows, emoji, or marketing flourish. Internal-review
  fidelity.

## Open questions (humans answer, do not invent)

These are duplicated at the top of `spec.md`. **If any block your phase, stop
and ask.**

- **ASR / transcription provider** — Google on-device? Whisper? Vendor SDK?
- **AI Key hardware intent** — Moto's stable intent action, or do we use
  an `AccessibilityService` shortcut?
- **AOD layer** — Moto AOD device APIs vs AndroidX `AmbientCallback`. AndroidX
  alone won't render the recording state shown in STEP-05.
- **Summary backend** — local on-device model, or server call?
- **Task target** — Google Tasks, system Calendar, or an in-app store?

## Demo-mode fallbacks (build these first)

- **Transcript stream**: read `assets/demo/transcript.jsonl` line-by-line on
  a coroutine timer, emit through a `Flow<TranscriptLine>`.
- **Summary**: read `assets/demo/summary.json`, simulate the 6-step pipeline
  with delays (~5s total in demo mode).
- **AI Key**: a long-press of `VOLUME_UP` triggers the same intent during
  development.
- **Task creation**: write to a local Room table; surface "Open Tasks"
  navigates to a simple list screen.

## When uncertain about Android APIs

```bash
android docs search "<question>"
android docs fetch kb://<url>
```

Cite the `kb://` URL in your reasoning. Don't guess.

## What to avoid

- Adding dependencies without asking.
- Changing the package structure without asking.
- Implementing real ASR / real AOD before the demo-mode fallback works.
- Building UI that doesn't match `screens/STEP-XX.png` layout & copy.
- Touching `spec.md`, `screens/`, or this file unless explicitly asked.
