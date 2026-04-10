# Project BlindEye

> An adaptive AI overlay for Android that sees, understands, and navigates any interface on behalf of elderly users.

---

## What is this?

BlindEye is an Android accessibility assistant designed for people who struggle with modern smartphone interfaces — particularly elderly users. It sits as a persistent overlay on top of any app, reads the screen using Android's Accessibility API, understands what the user is trying to do (via voice or touch), and either guides them step-by-step or acts on their behalf autonomously.

**Three modes of operation:**
- **Guide mode** — AI explains what to tap, user does it themselves
- **Autonomous mode** — AI taps, types, and navigates on the user's behalf
- **Adaptive mode** — system learns the user's habits over time and gets smarter for that specific person

---

## Status

| Phase | Name | Status |
|-------|------|--------|
| 0 | Foundation | 🔵 In progress |
| 1 | Guide Mode | ⬜ Not started |
| 2 | Autonomous Mode | ⬜ Not started |
| 3 | Adaptive Memory | ⬜ Not started |
| 4 | Hardening + Launch | ⬜ Not started |

---

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android 10+
- **AI Backend:** Gemini 1.5 Pro / GPT-4o (multimodal)
- **Screen reading:** AccessibilityService API
- **Overlay:** SYSTEM_ALERT_WINDOW
- **Wake word:** Porcupine (on-device)
- **Local storage:** Room + SQLCipher (AES-256 encrypted)
- **Build system:** Gradle (Kotlin DSL)

---

## Project Structure

```
app/
├── src/main/
│   ├── kotlin/
│   │   ├── accessibility/     # AccessibilityService + UI parser
│   │   ├── ai/                # AI API client, intent planner
│   │   ├── executor/          # Action synthesizer (taps, swipes, text)
│   │   ├── overlay/           # Floating UI layer
│   │   ├── memory/            # Room database, habit tracking
│   │   ├── security/          # PII scrubber, key management
│   │   └── voice/             # Wake word, TTS
│   └── res/
docs/
├── user-guide.md
├── guardian-setup.md
└── adr/
ARCHITECTURE.md
SECURITY.md
PRIVACY.md
API.md
CHANGELOG.md
```

---

## Quick Start (Development)

> Prerequisites: Android Studio, JDK 17+, Android device or emulator running API 29+

```bash
# Clone the repo
git clone https://github.com/YOUR_USERNAME/project-blindeye.git
cd project-blindeye

# Open in Android Studio and sync Gradle
# Run on device or emulator
```

You will need to:
1. Grant Accessibility Service permission in device settings
2. Grant overlay (draw over apps) permission
3. Add your AI API key to `local.properties` (never commit this file)

```
# local.properties
GEMINI_API_KEY=your_key_here
```

---

## Security

This app operates with system-level access to the entire device. Security is taken seriously.  
Read [`SECURITY.md`](SECURITY.md) before contributing or deploying.

**Never commit API keys. Never commit `local.properties`.**

---

## Documentation

| File | Contents |
|------|----------|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Full system design, layers, data flow |
| [`SECURITY.md`](SECURITY.md) | Threat model, security decisions |
| [`PRIVACY.md`](PRIVACY.md) | Data collection, storage, deletion |
| [`API.md`](API.md) | AI integration, prompt templates |
| [`CHANGELOG.md`](CHANGELOG.md) | Release notes |
| [`docs/user-guide.md`](docs/user-guide.md) | Guide for elderly users and families |

---

## Author

Răzvan — [@ShadowAndrei](https://github.com/ShadowAndrei)

*Built in Adjud, Romania.*
