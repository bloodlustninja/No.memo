# NO.MEMO — Nothing OS 5.0 Bento Notes & Kinetic Studio

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Kotlin-000000?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-D71921?style=for-the-badge" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-ffffff?style=for-the-badge&logoColor=black" alt="Target SDK" />
  <img src="https://img.shields.io/badge/Compose%20BOM-2024.10.01-000000?style=for-the-badge&logo=jetpackcompose" alt="Compose BOM" />
  <img src="https://img.shields.io/badge/Performance-120FPS%20Skia%20GPU-green?style=for-the-badge" alt="Performance" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

---

> **NO.MEMO** is an unforgettable, zero-friction Android notes and ideation studio built in 100% pure Jetpack Compose. It faithfully embodies the iconic industrial, monochrome, and dot-matrix bento design language of **Nothing OS 5.0**—powered by 120 FPS hardware-accelerated Skia GPU canvases, procedural mechanical PCM audio synthesis, an interactive Nothing Phone Glyph LED simulator, live Markdown WYSIWYG rendering, an infinite 2D Bento Knowledge Graph, and a physical homescreen bento widget.

---

## ✨ System Architecture & Key Innovations

### 1. 🔤 Kinetic Typography & Telemetry Matrix
* **Mechanical Glyph Scrambler (`KineticDotMatrixText.kt`)**: Headers and cards scramble through pseudo-random matrix characters (`░`, `▒`, `▓`, `•`, `*`, `0`, `1`, `X`) before locking into place, mimicking the physical LED dot-matrix displays of Nothing hardware.
* **Real-time Live Telemetry HUD (`KineticTelemetryValue.kt`)**: Monospace telemetry calculates real-time typing speed (**WPM**), character count, word count, and reading duration with zero perceptible lag.

### 2. ⚡ Zero-Friction "Flash Note" Quick-Draw Scratchpad
* **0ms Scratchpad (`FlashNoteOverlay.kt`)**: Summon a frosted scratchpad with instant keyboard focus, auto-hashtag extraction (`#idea`, `#urgent`, `#dev`), and single-tap persistence directly to the SQLite Room database.
* **Physical Shake Gesture (`NothingShakeDetector.kt`)**: Shake the device to summon the scratchpad from anywhere without touching the screen.

### 3. 🎙️ High-Fidelity Audio Memo & Scrubbable Dot-Matrix Player
* **Zero-Loss AAC Voice Recording (`AudioRecorderManager.kt`)**: Records high-fidelity `.m4a` voice memos while capturing real-time amplitude waveforms.
* **Interactive Waveform Player (`DotMatrixAudioPlayer.kt`)**: Scrubbable dot-matrix waveform player built directly into Bento cards with mechanical haptic toggles and timestamp telemetry (`00:14 // 00:30`).

### 4. 💡 Nothing Phone Rear Glyph LED Simulator & Morse Telegraph
* **Authentic Rear Glyph LED Array (`NothingGlyphInterface.kt`)**: Procedurally models the Camera Ring, Diagonal Slash, G-Loop, and Exclamation LEDs, pulsing in real time to voice RMS audio volume and playback.
* **Morse Telegraph Studio (`NothingMorseTransmitterOverlay.kt` & `GlyphCipherManager.kt`)**: Translates any note into Morse signals, pulses the rear Glyph LED strips in real time, and synthesizes 880Hz telegraph audio tones. Includes an interactive mechanical DOT/DASH telegraph keyer.

### 5. 🔒 Glyph Biometric Hardware Vault
* **Optical Fingerprint Scanner (`NothingBiometricLockOverlay.kt`)**: Dot-Matrix concentric optical fingerprint sensor with dynamic touch-and-hold radial fill, mechanical tumbler release sounds, and 4-digit PIN fallback.
* **Privacy Shutter (`BentoCardComposable.kt`)**: Locked cards conceal contents behind an animated matrix privacy shield until authenticated.

### 6. 🕸️ 2D Bento Knowledge Graph Constellation
* **Infinite Pan & Zoom Canvas (`BentoKnowledgeGraphScreen.kt`)**: Visualizes all notes as interactive celestial nodes connected by laser vectors based on shared `#hashtags`. Tap any star node to instantly open the corresponding note.

### 7. 📝 Live Markdown WYSIWYG Renderer
* **WYSIWYG Markdown Engine (`NothingMarkdownRenderer.kt`)**: Renders `# Headers` in NDot uppercase typography, `> Blockquotes` with crimson accent bars, interactive `- [ ] Checkboxes`, and `` `code blocks` `` with 1-tap clipboard copying.

### 8. 🧾 1-Bit Thermal Receipt Exporter
* **Industrial Dot-Matrix Tape Exporter (`NothingReceiptExporter.kt`)**: Renders any note into an authentic monochrome receipt bitmap with perforated edges, cryptographic checksum, and Nothing Crimson red stamp, ready to share via Android's share sheet.

### 9. ⏱️ Focus Matrix (Pomodoro Studio) & 📦 Recovery Vault
* **Pomodoro Studio (`NothingFocusTimerScreen.kt`)**: 48-dot procedural circular glyph progress ring with kinetic countdown timer.
* **Archive & Recovery Vault (`NothingArchiveVaultScreen.kt`)**: Dedicated recovery space for archived and soft-deleted notes with 1-tap restore chimes and permanent shredding.

### 10. ❖ Standby Ambient Desk HUD Mode
* **Low-Power AOD Mode (`NothingAmbientDeskScreen.kt`)**: Monumental NDot digital clock (`HH:mm:ss`), ambient breathing Glyph LED orbit ring, and live pinned notes ticker tape in pure pitch-black OLED (`#000000`).

### 11. 🔊 Procedural Mechanical Audio Feedback Engine
* **Zero-Asset PCM Audio Synthesizer (`NothingAudioFeedback.kt`)**: Procedurally generates mechanical shutter clicks, metallic checkbox chimes, and tumbler bolt snaps directly via 16-bit PCM AudioTrack buffers with 0ms latency and 0 bytes APK overhead.

### 12. 📱 Physical Homescreen Glyph Bento Widget
* **AppWidget Studio (`NothingBentoWidgetProvider.kt`)**: 4x2 / 2x2 Nothing OS bento widget for the Android homescreen featuring NDot typography, latest note previews, and 1-tap quick capture triggers.

---

## 🛠 Tech Stack & Dependencies

| Category | Component / Library | Version | Description |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.2.10` | Modern, safe Kotlin stdlib |
| **UI Framework** | Jetpack Compose | `2024.10.01` | Pure Compose 120 FPS Architecture |
| **Database** | Room Database | `2.6.1` | Local SQLite ORM with KSP codegen (v3 schema) |
| **Image Engine** | Coil Compose | `2.7.0` | Asynchronous image rendering |
| **JSON Serialization** | Gson | `2.10.1` | TypeConverters for Checklists, Waveforms, & Revisions |
| **Audio Synthesis** | Android AudioTrack | Native | 16-bit PCM procedural audio feedback |
| **Concurrency** | Kotlin Coroutines & StateFlow | `2.8.7` | Reactive asynchronous state management |

---

## 🏗 Architecture Diagram

```mermaid
graph TD
    A[User Gesture / Shake / Voice] --> B[MainActivity & MainNotesScreen]
    B --> C[MainViewModel / StateFlow]
    C --> D[NoteRepository]
    D --> E[Room AppDatabase]
    E --> D
    D --> C
    C --> F[120 FPS Compose Bento Grid]
    F --> G[FrostedBentoCard & BentoCardComposable]
    B --> H[NothingBiometricLockOverlay]
    B --> I[NothingAmbientDeskScreen]
    B --> J[BentoKnowledgeGraphScreen]
    B --> K[FlashNoteOverlay]
    B --> L[NothingFocusTimerScreen]
    B --> M[NothingArchiveVaultScreen]
    B --> N[NothingVoiceOverlay & GlyphInterface]
```

---

## 📁 Directory Structure

```
com.example.nomemo
├── data
│   ├── db           # AppDatabase (v3), NoteDao, Converters
│   ├── entity       # Room Note Entity & NoteType Constants
│   ├── model        # ChecklistItem, NoteRevision
│   ├── pref         # PreferencesManager (Pitch Black, Glassmorphism, Haptics, Accent)
│   └── repository   # NoteRepository
├── export
│   └── NothingReceiptExporter.kt   # 1-Bit Thermal Receipt & Tape Exporter
├── sensor
│   └── NothingShakeDetector.kt     # Hardware Accelerometer Gesture Engine
├── ui
│   ├── anim         # NothingAudioFeedback (Procedural PCM Audio Synthesizer)
│   ├── compose      # MainNotesScreen, FrostedBentoCard, NothingNoteEditorScreen,
│   │                # BentoKnowledgeGraphScreen, NothingMarkdownRenderer,
│   │                # NothingBiometricLockOverlay, NothingAmbientDeskScreen,
│   │                # NothingMorseTransmitterOverlay, NothingVersionHistorySheet,
│   │                # NothingFocusTimerScreen, NothingArchiveVaultScreen,
│   │                # DotMatrixAudioPlayer, DotMatrixDoodleSheet, FlashNoteOverlay,
│   │                # NothingGlyphInterface, KineticTypography
│   ├── editor       # NoteEditorActivity (Pure Compose Host)
│   ├── settings     # SettingsActivity & NothingSettingsScreen
│   └── viewmodel    # MainViewModel (0ms In-Memory Pre-Filtering)
├── voice
│   ├── AudioRecorderManager.kt     # AAC M4A Recorder & Waveform Engine
│   ├── GlyphCipherManager.kt       # Morse Code & Telegraph Tone Synthesizer
│   ├── VoiceToTextManager.kt       # Persistent Silent Speech Recognizer
│   └── VoiceParser.kt              # NLP Keyword Title & Body Parser
├── widget
│   └── NothingBentoWidgetProvider.kt # Android Homescreen Glyph Bento AppWidget
└── MainActivity.kt                 # Root Compose Dashboard Host
```

---

## 🚀 Building & Running

```bash
# Build the complete Debug APK
./gradlew assembleDebug

# Install on a connected physical device or emulator
./gradlew installDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
Released under the **MIT License**.
Designed and engineered following the **Nothing OS 5.0 Bento Design Specification**.
