# AI Recorder · UX & Engineering Spec

> Source of truth for Codex (and any other coding agent). Cross-references the
> per-step screenshots in `screens/`. Lifted from `AI Recorder SLT Demo UX.html`.

---

## 0. Open Questions — answer before shipping any real implementation

These block the *real* implementation. Demo-mode fallbacks are specified per
step so the full flow can be built today and swapped later.

- **ASR / transcription provider** — Google on-device, Whisper, vendor SDK?
- **AI Key hardware intent** — Moto's stable intent action, or accessibility
  shortcut?
- **AOD layer** — Moto-specific AOD APIs vs AndroidX `AmbientCallback`. AndroidX
  alone cannot deliver STEP-05's render.
- **Summary backend** — local on-device model, or server call?
- **Task target** — Google Tasks, system Calendar, or in-app store?
- **Speaker diarisation** — provider-native, or post-process?
- **Audio format** — `.m4a` (AAC) at what bitrate? sample rate?

---

## 1. Architectural overview

```
┌──────────────────────────────────────────────────────────────────┐
│  UI · Compose (Material 3)                                       │
│  RecorderScreen · TranscriptScreen · DetailScreen · SummaryScreen │
│  TaskCreatedSheet · TracebackOverlay · FocusScreen               │
└──────────────────────────────────────────────────────────────────┘
                ▲                                ▲
                │ StateFlow<RecorderState>       │ events
                │                                │
┌──────────────────────────────────┐  ┌────────────────────────────┐
│  RecorderForegroundService       │  │  GenerateSummaryUseCase    │
│  - MediaRecorder session         │  │  - 6-step pipeline         │
│  - elapsed timer                 │  │  - emits StepEvent stream  │
│  - MediaStyle ongoing notif      │  │  - source-anchor JSON      │
│  - PendingIntent ↔ AI Key        │  └────────────────────────────┘
└──────────────────────────────────┘                ▲
        ▲                ▲                          │
        │                │                          │
┌─────────────────┐  ┌─────────────────┐  ┌────────────────────────┐
│ AsrStream       │  │ TelephonyObserver│  │ TranscriptStore (Room) │
│ (Flow<Line>)    │  │ - RINGING        │  │ - lines + timestamps   │
│ - demo: jsonl   │  │ - OFFHOOK        │  │ - markers              │
│ - real: provider│  │ - IDLE           │  │ - speaker labels       │
└─────────────────┘  └─────────────────┘  └────────────────────────┘
```

### Core state machine (`RecorderState`)

```kotlin
sealed interface RecorderState {
  object Idle : RecorderState
  data class Recording(val elapsedMs: Long, val markers: List<Long>) : RecorderState
  data class PausedForCall(val elapsedMs: Long) : RecorderState
  data class Sealing(val elapsedMs: Long) : RecorderState
  data class Saved(val recordingId: String, val durationMs: Long) : RecorderState
  data class Generating(val recordingId: String, val step: AiStep) : RecorderState
  data class SummaryReady(val recordingId: String) : RecorderState
}

enum class AiStep { ENHANCE, TRANSCRIBE, SEPARATE, CORRECT, SUMMARISE, EXTRACT }
```

### Domain model

```kotlin
data class TranscriptLine(
  val id: String,
  val speaker: String,    // "Speaker 1" etc.
  val startMs: Long,
  val text: String,
  val partial: Boolean = false,
)

data class ActionItem(
  val id: String,
  val text: String,        // "Finalize demo script"
  val owner: String?,      // "PM"
  val due: String?,        // "Fri"
  val anchorMs: Long,      // jump-to position in audio
)

data class Summary(
  val recordingId: String,
  val text: String,
  val decisions: List<Decision>,
  val actions: List<ActionItem>,
)
```

---

## 2. Visual system

| Token | Value | Use |
|---|---|---|
| `--accent` | `#E5484D` | **Only** for active REC state and primary destructive (Stop) |
| `--ok` | `#1F8A5B` | Saved / success / "Generate notes" CTA on saved card |
| `--paused` | `#B08A3C` | Muted gold — call-paused capsule only |
| `--ink` | `#1A1A1A` / `#FFFFFF` on dark | Body text |
| `--ink-faint` | `#9A958A` | Secondary text, timestamps |

Typography: Inter for UI, JetBrains Mono for timestamps & technical labels.
Numbers (timer) are mono everywhere.

Speaker label colors (transcript): `#E9DFC4`, `#9BD1E5`, `#F4A679`, `#C7B7F0`,
`#A0DAB0`, `#F3B5C8` (cycle for >6).

---

## 3. Steps

Each step has: **Trigger** (when it fires), **System state** (machine name in
`snake_case`), **Required** (what must exist), **Fallback** (demo-mode), **Copy**
(every string, final), and **Screen reference** (`screens/STEP-XX.png`).

---

### STEP-00 · Pre-demo · idle → AI Key press

**What the user sees.** Normal home screen. User presses AI Key on the side.
A small system toast (Dynamic-Island-style capsule) appears for ~1.5s saying
"AI Recording started · English detected".

**Trigger.** AI Key short press from any state, including locked.
**System state.** `recording_starting`
**Required.** AI Key system intent · cold-start recorder service · ASR language probe
**Fallback.** Soft-button start inside the recorder app. In dev, long-press
`VOLUME_UP` triggers the same intent.

**Copy.**
- "AI Recording started"
- "English detected"

**Screen reference.** `screens/STEP-00-toast.png`

**Implementation notes.**
- The toast is a custom Compose overlay drawn into a `WindowManager.TYPE_APPLICATION_OVERLAY` window, or a `MediaStyle` notification with a short auto-cancel timer.
- AI Key intent target: `com.moto.airecorder.ACTION_AI_KEY_TOGGLE` — a single intent that toggles between start / stop based on current `RecorderState`.

---

### STEP-01 · Foreground recording

**What the user sees.** Recorder screen. Header reads `AI RECORDING`, with a
language pill on the right (`EN · auto`). Below: a big red dot + `REC` label, then
a `00:12` timer in mono. A live level meter (32 vertical bars). Then "Audio
saved continuously" in a muted gold. A transcript area begins to populate.
Three controls fill the bottom: `⚑ Mark`, `⏸ Pause`, `■ Stop` (Stop is red).
A small "Use Focus Mode" link sits below, low-key.

**Trigger.** Recorder service active, app brought to foreground.
**System state.** `recording_foreground`
**Required.** Recorder session · timer · ASR stream · speaker diarisation · live level meter
**Fallback.** Scripted transcript stream from `assets/demo/transcript.jsonl`.

**Copy.**
- "AI RECORDING" (header)
- "EN · auto" (language pill)
- "REC"
- "Audio saved continuously"
- "⚑ Mark" · "⏸ Pause" · "■ Stop"
- "Use Focus Mode" (secondary link, shield glyph)

**Screen reference.** `screens/STEP-01-foreground.png`

**Implementation notes.**
- Level meter draws from `MediaRecorder.maxAmplitude` polled at 16Hz. In demo mode, synthesise from a seeded waveform.
- Stop button does *not* immediately stop — it goes through STEP-08 sealing.
- Mark is instantaneous — append `markerMs` to `Recording.markers`.

---

### STEP-02 · Real-time transcript with speaker labels

**What the user sees.** The transcript area fills with color-coded speaker
lines. Each line: speaker name (bold, colored), timestamp (mono, faint), the
text. The currently-streaming line has a blinking caret at the end. A small
chip in the list shows `⚑ Marked · 01:12`.

**Trigger.** ASR stream emits new line.
**System state.** `recording_foreground` (same as STEP-01, transcript populated)
**Required.** ASR · diarisation · marker store · auto-scroll on new line
**Fallback.** Pre-scripted JSON with timed reveals (1 line every 2–5s).

**Copy (demo content).**
```
Speaker 1 · 00:08
"Let's align on the launch plan for Chicago."

Speaker 2 · 00:34
"The action item is to finalize the demo script by Friday."

Speaker 3 · 01:02
"We also need a backup plan for network failure."

Speaker 1 · 01:18
"Agreed. Let's keep the rehearsal slot on Thursday."
```

**Screen reference.** `screens/STEP-02-transcript.png`

**Implementation notes.**
- `LazyColumn` with `reverseLayout = false`. Auto-scroll to last item when a new line arrives and the user hasn't scrolled.
- Partial line (`partial = true`) has the blinking caret; on finalisation it replaces in-place.

---

### STEP-03 · App backgrounded · LingLongTai live capsule

**What the user sees.** User taps home. Home screen renders. A
Dynamic-Island-style capsule sits at the top center: red dot, `REC · 01:24`,
sub-line `Recording continues`. Capsule is tappable to return to the recorder.

**Trigger.** App moves to background (home / app switch / new app launched) while recording.
**System state.** `recording_background`
**Required.** System live activity / status capsule · deep link back to recorder
**Fallback.** Persistent ongoing notification with the same content.

**Copy.**
- "REC · 01:24"
- "Recording continues"

**Screen reference.** `screens/STEP-03-background.png`

**Implementation notes.**
- Moto-style Dynamic Island = custom overlay window. AndroidX has no equivalent yet.
- Demo-mode: render an overlay via `WindowManager.TYPE_APPLICATION_OVERLAY` after `SYSTEM_ALERT_WINDOW` is granted. Fall back to standard ongoing notification if permission denied.
- Tap deep-links to `RecorderActivity` via `PendingIntent`.

---

### STEP-04 · Notification shade · expanded card

**What the user sees.** User pulls down the shade. A pinned ongoing card at
the top shows: red dot · `AI Recording active` · timer right-aligned. Below:
`Audio saved continuously · EN`. Then a small level meter. Then `Mark / Pause /
Stop` as horizontal buttons. Below the row: "Tap to open recording".

**Trigger.** Shade swipe-down with recording active.
**System state.** `recording_background`
**Required.** Ongoing notification with `MediaStyle` + custom big content view · action `PendingIntent`s
**Fallback.** Static `NotificationCompat.Builder` card without level meter.

**Copy.**
- "AI Recording active"
- "Audio saved continuously"
- Action labels: "Mark" · "Pause" · "Stop"
- "Tap to open recording"

**Screen reference.** `screens/STEP-04-shade.png`

**Implementation notes.**
- `MediaStyle` for the compact view (system pins it). Custom `RemoteViews` for expanded view — system theming on Android 14+ matters here.
- The level meter inside `RemoteViews` is approximated as a static bar pattern; live animation isn't supported in `RemoteViews`.

---

### STEP-05 · AOD · Live Audio AOD

**What the user sees.** Phone is dozing. Black screen. From top: a tiny dim
clock (9:43). Then `● REC` — a red glowing dot + the word REC in spaced caps.
Then a *huge* 02:43 timer. Then a static 28-bar waveform — center bars red,
outer bars cream. Then "Audio saved" in muted gold. At the very bottom: "Wake
for controls".

**Trigger.** Display enters AOD/doze with recording active.
**System state.** `recording_background_aod`
**Required.** AOD layer override · static waveform asset · breathing red dot in AOD spec
**Fallback.** Static low-power image with REC + timer text. The waveform may
need to be a pre-rendered bitmap rather than live (vendor AOD restrictions).

**Copy.**
- "REC"
- timer (mono, large)
- "Audio saved"
- "Wake for controls" (informational only, not a button)

**Screen reference.** `screens/STEP-05-aod.png`

**Implementation notes.**
- **THIS REQUIRES MOTO AOD COOPERATION.** AndroidX `AmbientCallback` is for
  watches and doesn't apply to phone AOD. Most OEMs lock down AOD content to
  the system's own renderer.
- Demo-mode: emulate by drawing a fullscreen Compose surface with
  `setShowWhenLocked(true)` + `setTurnScreenOn(false)` and dimming the system
  UI. Won't be a real AOD but reads correctly on stage.

---

### STEP-06 · Incoming call · ringing

**What the user sees.** Native incoming-call screen with caller name and
accept/decline buttons. **Above the call UI**, the LingLongTai capsule stays
live: red dot · `REC · 02:10` · sub-line "Recording continues · audio still
saving". The capsule does NOT block the call controls.

**Trigger.** Telephony state RINGING while recording active.
**System state.** `recording_background_ringing`
**Required.** `TelephonyCallback.CallStateListener` · live-capsule state update · **no side-effect on recorder**
**Fallback.** Label-only capsule (no level meter).

**Copy.**
- "REC · 02:10"
- "Recording continues · audio still saving"

**Screen reference.** `screens/STEP-06-ringing.png`

**Implementation notes.**
- **CRITICAL: do not pause the recorder on RINGING.** Some Android telephony
  patterns reflexively duck audio — disable that for this stream.
- If the user rejects or misses, recorder state never changed. The capsule
  just removes its "Recording continues" sub-line and returns to the standard
  recording capsule.

---

### STEP-07 · Call answered · paused for privacy

**What the user sees.** Active call UI. The capsule changes appearance: now
muted gold instead of red, paused glyph (two bars) instead of dot, text "REC
paused · Call not recorded · Resumes after call".

**Trigger.** Telephony state OFFHOOK / ACTIVE_CALL (`TelephonyManager.CALL_STATE_OFFHOOK`).
**System state.** `recording_paused_call`
**Required.** Recorder `pause()` on call accept · capsule paused style · **call audio is never captured**
**Fallback.** Static paused capsule.

**Copy.**
- "REC paused · Call not recorded"
- "Resumes after call"

**Screen reference.** `screens/STEP-07-incall.png`

**Implementation notes.**
- Use `MediaRecorder.pause()` (Android 24+) to preserve the file. **Do not
  stop the session.** A stop would finalise and require a new file.
- Document somewhere user-visible (settings, or an FAQ link from the paused
  capsule) that "we never record call audio for privacy and compliance".

---

### STEP-07b · Call ended · recording resumed

**What the user sees.** Call ends. A short toast capsule appears for ~3s:
"Recording resumed · Audio still saving · 08:51". Then the standard live
capsule returns.

**Trigger.** Telephony state IDLE after OFFHOOK.
**System state.** `recording_background`
**Required.** Recorder `resume()` · toast capsule (~3s) · counter continues
**Fallback.** Silent resume; capsule timer just continues.

**Copy.**
- "Recording resumed"
- "Audio still saving · MM:SS"

**Screen reference.** `screens/STEP-07b-resumed.png`

---

### STEP-08 · AI Key stop → sealed → "Generate notes"

**What the user sees.** Second AI Key press. Live capsule changes to a
spinner with "Sealing audio…". ~1s later, it changes to a green capsule:
"Saved · Generate notes" with sub-line "Tap to open · 12:48". The capsule
itself is the CTA.

**Trigger.** AI Key short press while recording.
**System state.** `recording_saving` → `recording_saved`
**Required.** Recorder stop · file seal · save toast · capsule swap to CTA
**Fallback.** In-app save button on the recorder screen.

**Copy.**
- "Sealing audio…"
- "Saved · Generate notes"
- "Tap to open · MM:SS"

**Screen reference.** `screens/STEP-08-saved.png`

**Implementation notes.**
- Sealing = `MediaRecorder.stop()` + `release()` + persist `Recording` row to Room with duration and file path.
- The "Generate notes" capsule is a `PendingIntent` to `RecordingDetailActivity` with the new `recordingId`.

---

### STEP-09 · Recording detail page

**What the user sees.** Detail page for the just-saved recording. Top:
back arrow + recording name + ⋯. Title: "Chicago launch sync · 12:48 · EN ·
3 speakers · Nov 12 · 9:41". Three tabs: **Transcript** (default), Summary,
Mind map. Inline audio scrubber with play button and current position. The
transcript starts to render below. Bottom: a big red `★ Generate summary`
CTA. Below that, secondary actions: Ask · Translate · Export.

**Trigger.** Open from Saved capsule or library list.
**System state.** `recording_detail`
**Required.** Audio file · transcript JSON · player · tab navigation
**Fallback.** Static screenshot of the detail layout with audio file only.

**Copy.**
- Header: "Chicago launch sync · 12:48 · EN · 3 speakers · Nov 12 · 9:41"
- Tabs: "Transcript" · "Summary" · "Mind map"
- "★ Generate summary"
- Secondary: "Ask · Translate · Export"

**Screen reference.** `screens/STEP-09-detail.png`

**Implementation notes.**
- Audio player is `androidx.media3:media3-exoplayer` with `Player.Listener` for scrubber position.
- Tabs are `PrimaryTabRow` from Material 3.

---

### STEP-10 · Generate summary · config → progress

**Two screens.**

**STEP-10a · Config sheet.**

User taps "Generate summary". A bottom sheet rises with three settings:

- Template: Meeting (›)
- Detail: Standard (›)
- Output: Online (›)

Footer: Cancel · `Generate` (primary red).

**STEP-10b · Progress.**

After Generate, the sheet morphs into a progress view. Title: "Generating
summary". Below: a checklist of 6 plain-English steps:

```
✓ Enhancing audio        2.1s
✓ Transcribing           11s
✓ Separating speakers    6 spk
● Correcting names and terms    working…
○ Writing summary
○ Extracting actions
```

A progress bar shows ~62%. Sub-line: "~28s left · Audio is the source".

**Trigger.** User taps Generate summary.
**System state.** `generating`
**Required.** Transcript-to-summary pipeline · step-by-step events · ~30s estimated
**Fallback.** Pre-generated summary JSON with timed reveal (each step takes 1–4s).

**Copy.**
- Sheet: "Generate summary" / "Chicago launch sync · 12:48"
- Settings: "Template" / "Detail" / "Output"
- Values: "Meeting" / "Standard" / "Online"
- "Audio is the source · every line will link back"
- Progress steps (exact):
  1. "Enhancing audio"
  2. "Transcribing"
  3. "Separating speakers"
  4. "Correcting names and terms"
  5. "Writing summary"
  6. "Extracting actions"
- "~28s left · Audio is the source"

**Screen reference.** `screens/STEP-10a-config.png`, `screens/STEP-10b-progress.png`

**Implementation notes.**
- Each step emits a `StepEvent(step, status, elapsed)`. UI subscribes to a `Flow<StepEvent>` and renders the checklist.
- **Do not expose model names.** "Correcting names and terms" is the public-facing label for the prompt-correction pass. "Writing summary" is the public label for the summary LLM call. Etc.

---

### STEP-11 · Summary result · meeting summary + actions

**What the user sees.** Summary tab. Top: a 2–3 sentence summary. Then "KEY
DECISIONS" — two short bullets with green left-border. Then "ACTION ITEMS · 3"
— three cards, each with: bold task title, mono metadata row (`OWNER · X` /
`DUE · X` / `▶ Jump to MM:SS` in gold), inline `Edit` and `＋ Create task`
buttons.

**Trigger.** Generation complete.
**System state.** `summary_ready`
**Required.** Summary text · decisions · actions[] with `{who, due, anchorMs}` · jumpable anchors
**Fallback.** Static result JSON.

**Copy (demo content).**

Summary:
> Team aligned on the Chicago launch plan. Demo script will be finalised by
> Friday. A network-failure fallback will be prepared before rehearsal.

Decisions:
1. Rehearsal slot stays on Thursday.
2. Network fallback owned by Dev.

Action items:
1. **Finalize demo script** — OWNER · PM — DUE · Fri — ▶ Jump to 00:34
2. **Prepare network fallback** — OWNER · Dev — DUE · Thu — ▶ Jump to 01:02
3. **Deliver AOD/call-state UX** — OWNER · UX — DUE · Wed — ▶ Jump to 03:18

Footer: "Every line links to source audio"

**Screen reference.** `screens/STEP-11-summary.png`

**Implementation notes.**
- Action item card is reusable component `ActionItemCard(action: ActionItem, onJump, onCreateTask)`.
- "Jump to" tap → STEP-13.
- "Create task" tap → STEP-12.

---

### STEP-12 · Create task · system small window

**What the user sees.** User taps `＋ Create task`. A green-accented bottom
sheet rises. Top row: green check circle + "Task created". Below: a card
showing the task title, owner/due mono row, and a source attribution chip
with red dot: "Source · AI Recording · 04:12". Footer: Done · `Open Tasks`
(green primary).

**Trigger.** User taps `＋ Create task` on an action item.
**System state.** `task_created`
**Required.** Task system intent · provenance metadata · success sheet
**Fallback.** Local Room table + simple "Open Tasks" list screen.

**Copy.**
- "Task created"
- Task title (from action item)
- "OWNER · X" / "DUE · X"
- "Source · AI Recording · MM:SS"
- "Done" · "Open Tasks"

**Screen reference.** `screens/STEP-12-task.png`

**Implementation notes.**
- Real impl: `Intent` to Google Tasks (`com.google.android.apps.tasks`) or system Calendar with extras for the source link.
- Source URI scheme: `airecorder://recording/<id>?t=<ms>` — opens STEP-13.

---

### STEP-13 · Source traceback · summary → transcript → audio

**What the user sees.** Transcript tab opens. The audio scrubber jumps to
04:12, the playhead glows red. Above the transcript: small label "FROM SUMMARY
→ TRANSCRIPT". The matching transcript line is highlighted with a red border
and red background, while neighbouring context lines stay muted. Below the
list: a thin playing-now banner: "▶ Playing source audio · 04:12".

**Trigger.** User taps any timestamp anchor (summary line, action item, task chip).
**System state.** `transcript_at_anchor + audio_playing`
**Required.** Transcript line ↔ audio offset map · scroll + scrub + play
**Fallback.** Transcript scroll-and-highlight only; user scrubs audio manually.

**Copy.**
- "FROM SUMMARY → TRANSCRIPT"
- "▶ Playing source audio · MM:SS"

**Screen reference.** `screens/STEP-13-traceback.png`

**Implementation notes.**
- `LazyColumn.scrollToItem` with offset to center the matched line.
- ExoPlayer `seekTo(anchorMs)` + `play()`.
- Highlight border + bg animates in over 200ms then holds.

---

## 4. Optional · Focus Recording Mode

Optional enhancement for important meetings. Stronger status, deliberate stop,
can be minimized — the phone stays usable.

**Hard rule.** Minimize ≠ stop. Exit Focus Mode ≠ stop. Only an explicit Stop
ends a recording session.

### STEP-F-A · Enter Focus Mode

From the foreground recording card, an upsell row offers "Start Focus Mode"
with the value pitch: "Stronger status, fewer interruptions, visible safety".

**Trigger.** User taps "Start Focus Mode" on recorder card.
**System state.** `focus_active`
**Required.** Mode flag · UI swap · **same recorder session continues**
**Fallback.** Label-only mode toggle.

**Copy.** "Important meeting?" · "Switch to Focus Mode for stronger status and fewer interruptions." · "Start Focus Mode"

**Screen reference.** `screens/STEP-FA-entry.png`

---

### STEP-F-B · Focus Recording active

Dedicated full-screen recorder. Status bar shows shield + "FOCUS". Header
band: "FOCUS RECORDING" (spaced caps, muted cream). Center: red dot · `REC`
spaced caps · large mono timer (`14:02`) · live waveform · "Audio saved
continuously". Bottom row: Mark · Pause · `Stop` (red). Footer row: `↓ Minimize`
· `＋ Add note` · `⚙ Settings`.

**Trigger.** After Focus entry.
**System state.** `focus_active_foreground`
**Required.** Full-screen recorder surface · same session continues
**Fallback.** Normal recording UI with FOCUS header.

**Copy.** "FOCUS RECORDING" · "Audio saved continuously" · "↓ Minimize" · "＋ Add note" · "⚙ Settings"

**Screen reference.** `screens/STEP-FB-active.png`

---

### STEP-F-C · Minimize · phone remains usable

User taps Minimize. Recording continues; Focus stays on. A Focus-styled island
(shield + dot + `Focus REC · 14:02`) follows them across the system. Tap →
returns to the full Focus surface.

**Trigger.** User taps Minimize on focus surface.
**System state.** `focus_active_background`
**Required.** Focus-styled live capsule · deep link · same session
**Fallback.** Normal capsule with FOCUS label.

**Copy.** "Focus REC · 14:02"

**Screen reference.** `screens/STEP-FC-minimized.png`

---

### STEP-F-D · Stop confirmation

User taps Stop. A bottom sheet rises: "Stop and save recording?" "This will
end Focus Recording. Audio recorded so far is safe." Buttons: `Continue
Recording` (outline) and `Stop & Save` (red, primary). Tertiary text:
"Want to keep recording? **Minimize** · **Exit Focus Mode**".

**Trigger.** User taps Stop on the Focus surface.
**System state.** `focus_stop_confirm`
**Required.** Bottom sheet · "exit ≠ stop" link
**Fallback.** Native AlertDialog with the same content.

**Copy.**
- "Stop and save recording?"
- "This will end Focus Recording. Audio recorded so far is safe."
- "Continue Recording" · "Stop & Save"
- "Want to keep recording? Minimize · Exit Focus Mode"

**Screen reference.** `screens/STEP-FD-stop.png`

---

## 5. Test plan (visual)

After each phase, Codex must run `android screen capture --output=current.png`
and check against `screens/STEP-XX.png`. Match the **information hierarchy and
copy strings exactly**. Layout pixel-for-pixel is not required. Color tokens
must match.

## 6. Things to never do (also in AGENTS.md)

- Claim call audio is recorded.
- Pause the recorder while the phone is *ringing* (only on answer).
- Expose model names in the UI.
- Generate a summary without timestamp anchors.
- Auto-stop a recording without a user gesture.
- Use red for anything other than active REC and the Stop CTA.
