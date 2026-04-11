# Architecture — Project BlindEye

*Last updated: Phase 0 complete*

---

## Phase Status

| Phase | Name | Status |
|-------|------|--------|
| 0 | Foundation | ✅ Complete |
| 1 | Guide Mode | ⬜ Not started |
| 2 | Autonomous Mode | ⬜ Not started |
| 3 | Adaptive Memory | ⬜ Not started |
| 4 | Hardening + Launch | ⬜ Not started |

### Phase 0 — What was built
- `BlindEyeAccessibilityService` — connects, reads the live UI hierarchy on window state changes only (not every content change), counts clickable elements, logs the node tree behind a `DEBUG_LOG_TREE` flag
- `OverlayManager` — single persistent view (no teardown on toggle), collapsed 👁 floating button + expanded bottom panel with status text and STOP button
- `MainActivity` — setup screen with two permission cards (overlay + accessibility), deep-links to correct settings screens, re-checks on every resume
- `stopRequested` flag on the service companion — STOP button sets it; Layer 3 will check it before every action

---

## Overview

BlindEye is a system-level Android overlay that enables elderly users to interact with any app through voice commands and AI-driven guidance or automation. It does not modify target apps — it reads them through the Accessibility API and acts on them like a human would.

---

## Design Principles

1. **Privacy first** — all personal data stays on the device by default. Nothing is synced to the cloud unless the user explicitly opts in.
2. **Safety over speed** — the system always asks for confirmation before destructive actions. The user can stop everything at any moment.
3. **Graceful degradation** — if the AI API is unavailable, cached responses handle common flows. The app never hard-crashes on the user.
4. **Minimal permissions** — only the permissions actually needed are requested, and each one is explained in plain language.
5. **Isolation** — each layer has one job. No layer reaches into another layer's responsibility.

---

## System Layers

BlindEye is organized into six stacked layers, each with a clearly isolated responsibility.

```
┌─────────────────────────────────────────────┐
│  Layer 5 — Adaptive Memory                  │
│  Habit store · App pattern cache · Profile  │
├─────────────────────────────────────────────┤
│  Layer 4 — UX Overlay                       │
│  Floating panel · TTS · STOP button         │
├─────────────────────────────────────────────┤
│  Layer 3 — Action Executor                  │
│  Tap · Scroll · Type · Navigate             │
├─────────────────────────────────────────────┤
│  Layer 2 — Action Router                    │
│  Guide vs autonomous · Confidence gate      │
├─────────────────────────────────────────────┤
│  Layer 1 — Intent Engine                    │
│  NLU · Task planner · AI API client         │
├─────────────────────────────────────────────┤
│  Layer 0 — Perception                       │
│  AccessibilityService · Screenshot · Voice  │
└─────────────────────────────────────────────┘
```

### Layer 0 — Perception
Responsible for gathering information about the current state of the device.

- **AccessibilityService** — reads the live UI hierarchy of the foreground app (button labels, positions, types, enabled/disabled state). This is the primary input source.
- **Screenshot engine** — captures a compressed screenshot on demand (never continuously). Used when the UI hierarchy is insufficient (e.g., canvas-based apps, games, webviews).
- **Voice input** — always-on wake word detection (Porcupine, on-device). Full audio is only processed after wake word is detected.
- **UI element classifier** — categorizes interactive elements (button, input field, checkbox, link) to help the Intent Engine understand what can be acted on.

### Layer 1 — Intent Engine
Responsible for understanding what the user wants and planning how to achieve it.

- **NLU / voice-to-intent** — converts the user's spoken request into a structured intent (action + target + parameters).
- **Context manager** — tracks current app, current screen, and recent actions to give the AI accurate context.
- **Task planner** — breaks a high-level goal ("send a photo to my son") into an ordered sequence of sub-steps across one or more apps.
- **AI API client** — sends sanitized screen data + intent to the multimodal AI (Gemini 1.5 Pro or GPT-4o). Handles retries, timeouts, and rate limits.
- **Fallback handler** — if AI returns low confidence or fails, falls back to cached patterns or Guide Mode.

### Layer 2 — Action Router
Responsible for deciding what to do with the AI's response.

- **Mode selector** — decides whether to enter Guide Mode (user acts, AI narrates) or Autonomous Mode (AI acts directly) based on user preference and confidence score.
- **Confidence threshold** — AI responses below a configurable threshold automatically route to Guide Mode.
- **Permission gate** — certain actions (send, delete, purchase, call) require explicit user confirmation regardless of mode.
- **Undo/cancel manager** — maintains a buffer of the last 5 reversible actions.

### Layer 3 — Action Executor
Responsible for physically performing actions on the device.

- **Tap synthesizer** — performs programmatic taps on AccessibilityNodeInfo elements.
- **Text input injector** — fills text fields using accessibility actions (not simulated keyboard events).
- **Scroll controller** — scrolls lists and pages to bring off-screen elements into view.
- **Navigation actions** — Back, Home, Recents, opening apps by package name.
- **System action bus** — brightness, volume, notifications (when needed for task completion).

### Layer 4 — UX Overlay
Responsible for communicating with the user.

- **Floating panel** — rendered via SYSTEM_ALERT_WINDOW, visible over any app. Shows current step, progress, and a large STOP button.
- **Step indicator** — displays the current step number and total steps (e.g., "Step 2 of 4").
- **Voice TTS** — speaks each step aloud. Speed and voice are adjustable. Uses Android TTS on-device, with ElevenLabs as an optional upgrade.
- **STOP button** — always visible, always tappable, immediately halts any in-progress action. Cannot be covered by app content.
- **Panic shortcut** — triple-tap the overlay to instantly call a pre-configured contact.

### Layer 5 — Adaptive Memory
Responsible for learning and personalizing the experience over time.

- **User habit store** — records which apps, screens, and flows this specific user struggles with. Stored locally, encrypted.
- **App pattern cache** — caches the step sequence for common tasks (e.g., how to open WhatsApp camera and send a photo). Allows execution without an AI call after the first successful run.
- **Confusion heatmap** — tracks where in a flow the user hesitated, asked for help, or triggered a retry. Used to improve future guidance.
- **Preference profile** — stores voice speed preference, overlay font size, guide verbosity, language.
- **Guardian alerts** — optionally notifies a trusted caretaker when the user is confused for more than N seconds. Does not send screen content.

---

## Data Flow

```
User speaks / taps
       │
       ▼
[Security gate — PII check]
       │
       ▼
[Intent Engine — parse + plan]
       │
       ├── Cache hit? ──► [Action Router] ──► [Executor] ──► Done
       │
       └── No cache
              │
              ▼
       [AI API call — sanitized data only]
              │
              ▼
       [Action Router — guide or execute?]
              │
       ┌──────┴──────┐
       ▼             ▼
  [Guide Mode]  [Auto Mode]
  Narrate steps  Execute + confirm
       │             │
       └──────┬───────┘
              ▼
       [UX Overlay — show result]
              │
              ▼
       [Memory — record outcome]
```

---

## Key Design Decisions

### Why AccessibilityService over screenshot-only?
AccessibilityService gives us structured data — button labels, element types, enabled/disabled state — without needing vision AI on every frame. Screenshots are expensive (latency + API cost). We use screenshots only when the structured data is insufficient.

### Why local-first memory?
The user base is elderly people who may not understand cloud sync. Keeping all personal data on-device by default is both safer and more trustworthy. Guardian Mode sync, if enabled, uses end-to-end encryption.

### Why Gemini 1.5 Pro?
Multimodal (text + image in one call), large context window (useful for multi-step task planning), and better Android ecosystem integration. GPT-4o is the fallback if Gemini is unavailable or underperforms on a specific task.

### Why not root access?
Root would give us more power, but it would exclude the vast majority of devices, violate Play Store policies, and create security risks. Everything BlindEye needs can be done within the official Accessibility API.

---

## Future Considerations

- **iOS port** — requires different approach: iOS 17+ has limited accessibility automation. Likely requires a companion app model or Screen Time API workarounds.
- **Windows port** — UI Automation API is the Windows equivalent of AccessibilityService. Significant rewrite but conceptually similar.
- **On-device AI** — as edge models improve (Gemini Nano, Phi-3), some intent parsing could move fully on-device, eliminating latency and API costs.
- **Multi-language support** — architecture supports it; needs trained NLU models and TTS voices per language.
