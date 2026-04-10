# Security — Project BlindEye

> This app has system-level access to the entire device of a vulnerable person.  
> Security is not a feature — it is the foundation.

---

## Threat Model Summary

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|------------|
| API key extracted from APK | High | High | Android Keystore, ProGuard, no hardcoded keys |
| Screen content sent to AI unfiltered | Medium | Critical | PII scrubber runs before every API call |
| Man-in-the-middle attack on API calls | Low | High | Certificate pinning, HTTPS enforced |
| Unauthorized autonomous action | Medium | High | Confirmation dialogs, STOP button, undo buffer |
| Local memory read by malicious app | Low | High | SQLCipher AES-256 encryption |
| Caretaker abusing Guardian Mode | Medium | High | Opt-in only, limited to alerts, no screen access |
| AI prompt injection via malicious app UI | Low | Medium | Input sanitization before AI call |
| APK reverse engineering | Medium | High | R8 + ProGuard obfuscation |
| User accidentally performing destructive action | High | Medium | 3-second delay + large confirmation dialog |

---

## 1. Data Privacy

### What we capture
- UI hierarchy (button labels, element types) from the foreground app — only when a session is active
- Screenshots — only on demand, immediately deleted after AI call
- Voice audio — only after wake word detection, not continuously recorded

### What we never capture
- Passwords and credential fields — detected via `inputType` flags and permanently excluded
- Payment card numbers, OTPs, bank PINs — pattern-matched and stripped before any processing
- Content from banking apps — optional global exclusion list configurable by the user

### Where data lives
- All user habit and preference data is stored **locally only** in an encrypted SQLite database (SQLCipher, AES-256)
- The encryption key is derived from the device ID combined with a user-set PIN — never stored in plaintext
- No data is synced to any server by default
- Guardian Mode alerts (confusion notifications only, no screen content) use end-to-end encryption if enabled

### Data retention
- Session logs: auto-purged after 7 days
- App pattern cache: kept until the user clears it or uninstalls
- Confusion heatmap: aggregated and anonymized after 30 days

---

## 2. API Key Security

**Never store API keys in the APK. Never commit them to version control.**

### How keys are stored
- Keys are stored in the **Android Keystore** system — hardware-backed on supported devices
- At build time, keys are injected from `local.properties` (which is in `.gitignore`) into `BuildConfig`
- At runtime, the key is retrieved from Keystore and used for a single session token — it is never held in memory longer than needed

### How API calls are secured
- All calls use HTTPS
- **Certificate pinning** is enforced — if the certificate does not match the expected public key hash, the request is rejected
- Per-session ephemeral tokens are used where the AI provider supports them
- Client-side rate limiting prevents accidental runaway API usage

### `.gitignore` must include:
```
local.properties
*.jks
*.keystore
google-services.json
```

---

## 3. Action Safety

The app can perform actions on behalf of the user. This must never happen silently or irreversibly.

### Confirmation-required actions
The following actions always require explicit user confirmation, regardless of mode:
- Sending a message (SMS, WhatsApp, email, etc.)
- Making a phone call
- Deleting any content
- Making a purchase or financial transaction
- Sharing files or media externally
- Changing account settings or passwords

### Confirmation dialog design
- Large font (minimum 20sp) — readable by low-vision users
- Clear description of what is about to happen ("This will send a message to Maria")
- 3-second enforced delay before the confirm button becomes tappable
- CANCEL is always the leftmost, most prominent option

### STOP button
- Always rendered on top of all other content
- Cannot be covered by any app
- Immediately halts any in-progress action
- Triggers the undo buffer if applicable

### Undo buffer
- Maintains a reversible record of the last 5 actions
- Each entry includes: action type, target element, previous state
- Non-reversible actions (sent messages, calls) are logged but not undoable — user is warned before these actions

---

## 4. Permission Model

BlindEye requests only the permissions it actively uses, and explains each one in plain language during onboarding.

| Permission | Why it's needed | When it's active |
|-----------|----------------|-----------------|
| `BIND_ACCESSIBILITY_SERVICE` | Read UI hierarchy of any app | Only while a session is running |
| `SYSTEM_ALERT_WINDOW` | Draw the overlay panel above apps | Whenever the app is running |
| `RECORD_AUDIO` | Wake word detection + voice input | Always-on wake word only; full audio only during session |
| `INTERNET` | Send sanitized data to AI API | Only during an active AI call |
| `FOREGROUND_SERVICE` | Keep the service alive | While the app is enabled |
| `RECEIVE_BOOT_COMPLETED` | Restart service after reboot (optional) | Once at boot |

**Not requested:**
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
- `READ_CONTACTS` / `WRITE_CONTACTS`
- `CAMERA`
- `READ_EXTERNAL_STORAGE` (unless a task explicitly needs it and user consents)

All permission usage is logged locally and viewable by the user in the app settings.

---

## 5. Guardian Mode Security

Guardian Mode allows a trusted caretaker to receive alerts when the user appears confused.

### What a Guardian can see
- Confusion alerts: "User has been on the same screen for 3+ minutes"
- App usage summary (which apps were opened, not what was done in them)
- Session count and average session length

### What a Guardian cannot see
- Screenshots or screen content of any kind
- Messages, photos, contacts, or any personal data
- Real-time screen mirroring

### How Guardian access is granted
1. The device owner (the elderly user or their family setup helper) enables Guardian Mode in settings
2. A unique invite link is generated — expires in 24 hours
3. The caretaker accepts on their device with their own BlindEye account
4. The device owner can revoke access at any time from their device settings

### Guardian communication
- All Guardian alerts are transmitted via end-to-end encrypted channel
- BlindEye servers (if used as relay) never have access to message content
- Alerts are batched — not real-time — to prevent the Guardian from inferring real-time location or activity

---

## 6. Code Security

### Obfuscation
- R8 minification and ProGuard rules are applied to all release builds
- Class names, method names, and field names are obfuscated
- Sensitive class names (key management, scrubber logic) have custom keep rules to prevent accidental exposure via stack traces

### Logging
- No sensitive data is ever written to Logcat in release builds
- Debug logs are stripped at compile time using `BuildConfig.DEBUG` guards
- Session logs written to disk are redacted before writing (PII stripped)
- All disk logs auto-purge after 7 days

### Dependencies
- All third-party dependencies are pinned to exact versions in `build.gradle.kts`
- Dependency checksums are verified via Gradle verification metadata
- No dependency is included without a documented reason

---

## Responsible Disclosure

If you discover a security vulnerability in Project BlindEye:

1. **Do not open a public GitHub issue.**
2. Contact the maintainer directly (email TBD when project goes public).
3. Include a clear description of the vulnerability, steps to reproduce, and potential impact.
4. You will receive a response within 72 hours.
5. We will work with you to understand and fix the issue before any public disclosure.

We do not have a bug bounty program at this time, but we will credit responsible disclosers in the changelog.
