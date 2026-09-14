# PRD: Daydream Audio — Plain-English Audio EQ, Virtualizer & Restoration Suite for Android

**Status:** Draft v3 — named ("Daydream Audio"), build-ready spec for handoff to an AI coding agent
**Tagline:** *Sound the way you remember it.*
**Owner:** Kashyap
**Doc type:** Product Requirements Document — functional spec + acceptance criteria. No code included by design; this is the source-of-truth for an engineering/agent build pass.

> **One honest flag before this starts:** most of this document is real, buildable Android engineering (EQ, dynamics, virtualizer, loudness). Three areas — real-time spectral noise reduction (6.10), head-tracked spatial audio (6.7b), and real-time audio-reactive "Sonic Glass" rendering (22.7) — are genuinely hard DSP/ML/graphics problems, not "flip a switch in an API" problems. I've marked those with engineering-risk notes and a fallback approach so a coding agent doesn't stall trying to build the hardest version first. Building the P0 scope in section 13, plus the static Liquid Glass system (22.1–22.6), gets you a real, shippable, differentiated app. The P2 stretch items are where the actual R&D risk lives — worth sequencing accordingly rather than blocking on them.

---

## Table of Contents

1. One-line pitch
2. Problem Statement
3. Target User & Personas
4. Goals & Non-Goals
5. Core Concept: "Translate the Slider"
6. Feature List (6.1–6.10)
7. UX Principles & Onboarding Flow
8. Technical Requirements & Signal Chain
9. Cross-Android-Version Support Strategy
10. Competitive Landscape
11. Data & Settings Model (conceptual)
12. Permissions
13. Prioritization (P0 / P1 / P2)
14. Functional Requirements Table (FR-IDs + acceptance criteria)
15. Non-Functional Requirements
16. Success Metrics
17. Testing Strategy
18. Risks / Open Questions
19. Monetization
20. Roadmap / Milestones
21. Glossary (plain-language ↔ technical term mapping)
22. UI/UX Design System — "Liquid Glass"
23. Out of Scope

---

## 1. One-line pitch

An Android sound-enhancement and restoration app where the equalizer, dynamics, virtualizer, and noise reduction controls don't assume you know what "3.2kHz" or "spectral subtraction" means — every control is explained in the language of what you'll actually *hear* — while still giving power users the real numbers and a full restoration chain underneath.

---

## 2. Problem Statement

Every EQ app on the Play Store (Wavelet, Poweramp, XEQ, Music Volume EQ) ships the same interface: 5–10 vertical sliders labeled with Hz values and a handful of genre presets like "Rock" or "Bass Booster." This works for audio nerds. It's meaningless for a normal user who knows their old MP3/cassette-transfer/YouTube-rip "sounds thin" or "sounds hissy" and has no idea whether that's an EQ problem, a dynamics problem, or a noise problem.

**Result:** most users never touch anything beyond a preset, and the app's real value — fixing *their specific* bad recording — never gets used. Separately, none of the current apps address the actual defects of old/cheaply-mastered audio (narrow dynamic range, tape hiss, hum, muddy mids) — they only offer generic EQ presets.

---

## 3. Target User & Personas

**Persona A — "The Archivist"**
Has a large library of old, cheaply-mastered music (old Bollywood, 60s–90s rips, cassette transfers, YouTube-ripped MP3s). Wants it to sound noticeably better. Zero audio engineering vocabulary. This is the primary persona and drives the plain-language requirement.

**Persona B — "The Everyday Listener"**
Regular Android user who just wants headphones/speaker to sound less flat. Same "noob" profile as Persona A but without old/degraded source material specifically — cares more about EQ/volume/spatial than restoration.

**Persona C — "The Tinkerer" (secondary, not primary design target)**
Already knows Wavelet/Poweramp. Wants raw parametric control. Served by Advanced Mode, but the app is not designed *around* this persona first.

---

## 4. Goals & Non-Goals

### Goals
- Make frequency-based sound editing understandable without audio vocabulary.
- Make the virtualizer's effect explainable and controllable, not a mystery toggle.
- Let a user fix an actual bad recording in under 30 seconds without knowing what "tinny" corresponds to on a graph.
- Give power users a real 10+ band parametric EQ and full dynamics chain underneath.
- Offer at least a baseline noise/hiss reduction option for restoring old recordings — the single most-requested capability that no competitor addresses.

### Non-Goals (v1)
- No full AI stem separation or source remastering at iZotope RX quality (that's a different, much bigger product).
- No streaming service partnership integrations (Spotify/YT Music API deals) — works via standard Android audio session hooking only.
- No desktop or iOS version in v1.
- No cloud processing requirement for core features — should work fully offline (cloud-assisted processing may be an optional P2 tier for the hardest noise-reduction cases, see 6.10).

---

## 5. Core Concept: "Translate the Slider"

Every control — EQ band, dynamics, virtualizer, noise reduction — gets three layers of explanation, revealed progressively:

| Layer | Shown to | Example |
|---|---|---|
| **Plain-English label** | Everyone, always visible | "Warmth", "Punch", "Space", "Hiss Removal" |
| **What it fixes / what it does** | On tap/long-press | "Turn this up if vocals sound buried or muddy." |
| **Real technical value** | Toggle "Show technical values" in settings | "250Hz, +3dB" / "Compressor ratio 3:1" |

This is the single biggest differentiator vs. every competitor — nobody else does the middle layer, and nobody does it consistently across EQ *and* dynamics *and* noise reduction.

---

## 6. Feature List

### 6.1 Simple Mode (default, first-run experience)

**5 plain-language EQ sliders**, not 10+ Hz bands:

| Label | Frequency range | Plain description |
|---|---|---|
| Rumble | ~20–60Hz | Deep bass you feel more than hear |
| Warmth | ~60–250Hz | Fullness and punch |
| Body | ~250Hz–2kHz | Vocal richness; boxy if too high |
| Clarity | ~2–6kHz | Sharpness of vocals/instruments; harsh if too high |
| Air | ~6–16kHz | Sparkle, cymbals, sense of "openness" |

**"What's wrong with my song?" wizard** — the core "aha" feature. User picks a plain-language complaint, app applies a pre-mapped adjustment:

| User complaint | System response |
|---|---|
| "Sounds thin / tinny" | Boost Warmth + Body, slight cut to Air |
| "Sounds muddy / boxy" | Cut Body, boost Clarity |
| "Vocals buried" | Boost Body/Clarity, small dip in Warmth |
| "Too harsh / sibilant" | Cut Clarity/Air |
| "Sounds flat / lifeless" | Light boost to Rumble + Air (smiley curve) + suggest enabling "Space" |
| "Sounds hissy / noisy" | Route to Noise Reduction (6.10) rather than EQ — the wizard must correctly triage this to the right subsystem, not just apply an EQ cut |

**Live before/after (A/B) toggle**: one tap, so the user can confirm the change actually did what was promised. This must be instant (<50ms switch) or the comparison is useless.

### 6.2 Advanced Mode (single toggle, not paywalled)

- Full 10–31 band parametric EQ with real Hz/Q/gain.
- Manual dynamics controls: compressor threshold/ratio/attack/release, limiter ceiling — plain-language tooltips still present.
- Import/export EQ + full-chain presets as shareable files.

### 6.3 Virtualizer — "Space" control

- Single **"Space" slider (0–100%)**: *"Makes sound feel like it's coming from around you instead of glued inside your head. Higher = wider, but too high on old mono recordings can sound hollow or phasey."*
- **Auto-detect mono vs. stereo source and cap/warn on the Space slider for mono content** — this is a required behavior, not optional polish, because applying full virtualization to mono old recordings actively makes them sound worse (established in earlier discussion of what a virtualizer technically does — crosstalk cancellation + HRTF + decorrelation don't have real stereo information to work with on mono sources).
- Advanced Mode exposes HRTF profile choice (Narrow / Natural / Wide).

### 6.4 Auto-Profile per output device

- Detect headphones/speaker/Bluetooth device, offer a tuned starting profile (AutoEq-style device database, same category as Wavelet's approach).
- Remember last-used settings per device (wired ≠ car Bluetooth ≠ phone speaker).

### 6.5 Per-app / per-song memory

- Save settings per playback app (podcast app vs. music player) and optionally per song/playlist (P1/P2, not core P0).

### 6.6 Volume Boost ("Loudness")

- Push output beyond the OS 100% ceiling via `LoudnessEnhancer`.
- Must be paired with an **automatic soft limiter** so boosting doesn't clip — this is the specific failure mode of nearly every existing "Volume Booster" app, and fixing it is a real differentiator.
- Visible warning past a safe threshold: *"Past this point you're trading clarity for volume, and it can distort."*

### 6.7 Spatial Audio

**6.7a — Static Spatial (P0/P1, works on any headphones)**
- A richer HRTF-based "room simulation" than the basic virtualizer — plain label: *"Simulates a small room / concert hall around you."*
- Toggle + intensity slider.

**6.7b — Head-Tracked Spatial (P2, hardware-dependent — ENGINEERING RISK)**
- Sound field shifts as the user turns their head, using gyro/accelerometer data relayed via Bluetooth LE Audio or headphone-specific sensor APIs.
- **Engineering risk note:** Only a small subset of Android + headphone combinations expose the needed sensor data at all, and there's no unified Android API for this the way Apple has for AirPods. This feature should be built to gracefully detect absence of support and hide itself completely, not degrade into a broken toggle. Recommend treating this as a post-launch investigation spike, not a v1 commitment.

### 6.8 Dynamic Range Control ("Punch")

- Simple Mode: single **"Punch" slider** — *"Evens out quiet and loud parts. Turn this up for old recordings that sound flat or lifeless; turn it down for modern tracks that already sound loud and compressed."*
- Internally merges compressor ratio + makeup gain for Simple Mode; Advanced Mode splits into full parametric controls (threshold, ratio, attack, release, ceiling).
- This is the single highest-impact feature for the original use case (old, badly-mastered recordings) — more impactful than EQ alone, because the core defect in those recordings is usually narrow/inconsistent dynamic range, not wrong tonal balance.

### 6.9 Audio Clarity ("Clarity" macro — distinct from the EQ band of the same name)

- Blends a de-harshening filter + presence boost + subtle multiband exciter — not a single EQ slider.
- Plain description: *"Makes vocals and instruments easier to pick out of the mix without just turning up treble."*
- Targets the "everything is smeared together" problem specific to old/muddy recordings, which plain EQ can't fix (EQ shifts loudness of frequency ranges; it doesn't separate overlapping content the way a multiband exciter/transient shaper can).
- **Engineering note:** requires a real multiband compressor/exciter chain, not Android's stock `Equalizer` effect — see section 9 for API-level implications.

### 6.10 Noise Reduction — "Hiss & Crackle Removal" (NEW)

This is the newly requested capability and needs to be treated as its own subsystem, not a slider bolted onto EQ.

**What it needs to actually fix (from Persona A's real source material):**
- Tape hiss (broadband high-frequency noise, constant)
- Vinyl crackle/pop (short transient noise spikes)
- Mains hum (50/60Hz + harmonics, constant tonal noise)
- Low-level background rumble/room noise from cheap original recording equipment

**Two-tier approach (this is the realistic engineering path, not overpromising a single magic slider):**

**Tier 1 — Real-time, lightweight (P0/P1):**
- A single **"Hiss Removal" slider (0–100%)**, plain description: *"Reduces background hiss and noise floor. Higher settings can slightly dull very high frequencies — find the point where hiss disappears but the music still sounds bright."*
- Technical approach: a real-time noise gate + adaptive high-shelf attenuation, tunable against a rough noise-floor estimate taken from quiet passages in the track. This is computationally cheap enough to run live during playback on any supported Android version.
- Separate **"De-Hum" toggle**: a notch filter targeting 50Hz/60Hz + harmonics, since mains hum is a distinct, narrow-band problem from broadband hiss and should not be conflated with it in the UI or DSP.
- Separate **"De-Crackle" toggle**: transient/click detection and suppression for vinyl-style pops — fundamentally different signal (short spikes, not constant noise), needs its own detector rather than being folded into the hiss slider.

**Tier 2 — High-quality, ML-based, offline/per-file (P2 — ENGINEERING RISK):**
- A genuinely good hiss/noise removal (the kind that sounds transparent rather than "underwater") typically needs a trained noise-suppression model (e.g., an RNNoise-class or spectral-gating neural model) rather than a simple filter — this is the same category of tool as Adobe Podcast Enhance, referenced earlier in this project's research.
- **Recommended approach if pursued:** run this as an **offline "Enhance this track" file-processing action** (user picks a specific song, taps "Restore," waits a few seconds while an on-device or cloud model processes it, gets a cleaned file back) rather than attempting real-time ML noise suppression during live playback, which is a much harder and more battery/CPU-intensive problem, especially across the OS/hardware fragmentation covered in section 9.
- This tier should be explicitly scoped as a **v2 investigation**, not a v1 commitment — the P0 real-time filter (Tier 1) is what should ship first and already meaningfully addresses Persona A's core complaint.

### 6.11 Time Machine Presets (NEW — creative addition)

- A dedicated preset category, distinct from genre presets, tagged by **decade/format** rather than musical style: e.g. "60s Mono Transfer," "70s Cassette," "80s Bollywood Cinema Print," "90s Radio Rip," "Early Digital / Low-Bitrate MP3."
- Each preset is a pre-tuned *combination* across subsystems already in this PRD — not a new engine: specific Punch (6.8) settings for that era's typical dynamic range problems, Hiss Removal/De-Hum (6.10) tuned to that format's typical noise signature (tape hiss for cassette, needle noise for vinyl transfers, mains hum for cinema prints), and EQ tilt matching what that era's playback systems commonly lacked.
- Plain framing in UI: *"Tell us roughly when/how this recording was made, and we'll start you off with a fix built for that specific problem — you can still fine-tune from there."*
- **Why this matters more than a generic genre preset:** it directly answers Persona A's actual problem statement (old, cheaply-mastered recordings) with specificity, rather than making the user manually diagnose "is this a hiss problem or a dynamics problem" themselves — the preset already encodes that diagnosis.
- Low engineering risk — this is curation and preset design on top of subsystems already scoped in 6.1–6.10, not new DSP.

### 6.12 Golden Ear Trainer (NEW — creative addition)

- A short (~60 second), optional, gamified onboarding module: the app plays a clip, applies a boost/cut to one random plain-language band or control (Warmth, Clarity, Air, Space, Punch), and the user guesses which one changed.
- Purpose: teaches the plain-language vocabulary the whole app is built around *experientially*, rather than relying solely on static tooltips (section 5) to land the concept.
- Secondary purpose: a genuine retention/engagement hook independent of actual audio-fixing use cases — gives the app a reason to be reopened that has nothing to do with "my song sounds bad today."
- Should be skippable on first run (not a gate to using the app), but resurfaced as an optional "Train Your Ear" entry point from the main screen for users who want to keep playing it.
- Stretch idea (P2, not required): track accuracy over time and show a simple "Golden Ear Level" so users see themselves getting better at identifying frequency changes — purely a nice-to-have, not core to the mechanic.

### 6.13 Reverse Time Machine — "Vintage-ify" (NEW — creative addition, mind-blowing swing)

This is the idea that reframes what the app *is*, not just what it does: **every restoration subsystem already built in this PRD can run backwards.**

- Section 6.10 (Hiss Removal/De-Hum) exists to *subtract* noise from an old recording. Run in reverse, the same engine can *synthesize* authentic tape hiss, vinyl crackle, and mains hum and layer it onto **any** audio — a modern song, a voice memo, a video clip's audio track.
- Section 6.8 (Punch/dynamic range) exists to *restore* dynamic range that old mastering lost. Run in reverse, it *compresses* dynamic range back down, recreating the "squashed," intimate sound of an old cassette or AM radio.
- Section 6.11 (Time Machine presets) already encodes exactly what makes each era/format sound the way it does. Same presets, same curation work already done — just applied additively instead of subtractively. "60s Mono Transfer," "70s Cassette," "80s Cinema Print" become one-tap filters that turn *new* audio into an aged memory, not just fixes for old audio.
- **One genuinely new (small) DSP addition needed:** a subtle wow-and-flutter effect — a slow, gentle pitch/speed modulation (LFO-driven) that recreates the characteristic "warble" of tape and turntable playback. This is the one authentically new signal-processing piece in this feature; everything else reuses subsystems already scoped, which keeps engineering risk low relative to how big the idea feels.
- Plain-language framing: *"Make it sound like a memory."* — a single before/after toggle, era presets exactly like 6.11, no separate learning curve for the user.
- **Why this matters beyond being a cute trick:** it turns Daydream Audio from a restoration *utility* (used occasionally, when someone happens to have an old bad recording) into a creative *tool* people reach for proactively — turning a voice memo of their kid, a phone-recorded live moment, or literally any clip into something that feels like a cherished old memory. That's a fundamentally larger and more frequent use case than "fix my old cassette rip."

### 6.14 Memory Postcard — Shareable Export (NEW — creative addition, growth loop)

- One-tap export of a **Vintage-ified (6.13) or restored (6.11) clip as a short shareable video**, not just an audio file — combining the processed audio with a rendered clip of the Liquid Glass UI itself, including the Sonic Glass (22.7) ripple/tint reactivity baked in as the visual.
- Designed explicitly for WhatsApp Status, Instagram Reels/Stories, and similar vertical-video sharing surfaces — the exact behavior that made filter-driven apps (VSCO, old-camera-effect apps) grow virally: the *output* is inherently shareable and inherently shows off the product's visual identity to everyone who sees it.
- Every shared postcard can carry a subtle, non-intrusive "Made with Daydream Audio" watermark/end-card — organic distribution built into the sharing mechanic itself, not a separate marketing feature.
- **Engineering note:** this depends on 6.13 (Vintage-ify) and 22.7 (Sonic Glass) both existing first, so it should be sequenced after those, not built in parallel — it's a packaging/export layer on top of features that need to be stable first.
- This is the feature most likely to make the app *spread* rather than just be *used* — worth treating as a genuine growth lever in prioritization, not a nice-to-have polish item.

---

### Principles
1. Never show a raw technical value without a plain-language anchor beside it, even in Advanced Mode.
2. Show consequences, not just controls — every slider's tooltip states what happens if pushed too far.
3. One-tap undo/reset always visible.
4. Progressive disclosure — Simple Mode is genuinely simple; Advanced Mode is one deliberate tap away, never forced.
5. Noise reduction, dynamics, EQ, and spatial controls are visually and conceptually separated into distinct sections — not merged into one giant slider wall, since they solve different problems and a user should be able to reason about which one they need (reinforced by the "What's wrong with my song?" wizard triaging to the correct subsystem).

### First-run onboarding flow (proposed)
1. Welcome screen — one sentence: "Fix your sound without needing to know audio jargon."
2. Permission/setup: detect current output device (headphones/speaker/Bluetooth), offer tuned starting profile.
3. Prompt: "What's wrong with your music right now?" → launches the wizard (6.1) immediately, before showing any raw sliders — first impression should be the differentiator feature, not a generic EQ screen.
4. After wizard applies a fix, show the A/B toggle prominently so the user immediately experiences the before/after.
5. Only after this flow, reveal the full Simple Mode screen with all 5 EQ sliders + Space + Punch + Clarity + Hiss Removal, each collapsed with just its plain label until tapped.
6. Advanced Mode toggle is visible but not explained unless tapped ("For audio enthusiasts who want raw control").

---

## 8. Technical Requirements & Signal Chain

- Must work system-wide via Android's `AudioEffect` framework (`Equalizer`, `BassBoost`, `Virtualizer`, `DynamicsProcessing`, `LoudnessEnhancer`) as the foundation — same base every serious competitor (Wavelet, Poweramp) uses.
- No root required for core features.
- Must work across the top 5 music/streaming apps' audio sessions (Spotify, YouTube Music, local player, Bluetooth output, phone speaker) at minimum for P0.

### 8.1 Required signal chain order (this matters and should not be left to implementation-time guessing)

Effects are not order-independent — applying them in the wrong sequence produces audibly different (and often worse) results. Recommended chain, source → output:

1. **Noise Reduction** (hiss/hum/crackle) — must happen first, before anything else amplifies or reshapes the noise floor.
2. **Equalizer** (the 5-band or full parametric EQ) — tonal shaping on cleaned signal.
3. **Clarity macro** (de-harsh + presence + exciter) — operates on the EQ'd signal.
4. **Dynamics ("Punch": compressor/limiter)** — evens out level after tonal shaping, since compression responds to loudness and should see the final tonal balance, not the raw pre-EQ signal.
5. **Virtualizer / Spatial ("Space")** — stereo/spatial shaping should be one of the last steps so it's not undone or altered by subsequent gain changes.
6. **Volume Boost / Loudness Enhancer + final limiter** — always last, as a safety ceiling on the fully processed signal.

This ordering should be treated as a hard requirement for whoever/whatever implements the audio pipeline, not a suggestion — it directly affects whether the "Punch" and "Clarity" features actually deliver the described plain-language result.

### 8.2 Engineering-risk features requiring special handling

- **6.9 Clarity macro** and **6.10 Tier 2 noise reduction** cannot be built from Android's stock `AudioEffect` classes alone — they need custom DSP (multiband processing / spectral gating) or an embedded ML model (e.g., TensorFlow Lite / ONNX Runtime for Android, running something in the RNNoise family for noise suppression). This has real implications for APK size, CPU/battery load, and minimum supported hardware, and should be scoped as its own technical spike before committing to a ship date.
- **6.7b Head-tracked spatial** has no unified Android API — treat as a research spike, not a scheduled feature (see 6.7b engineering risk note).

### 8.3 DSP Algorithm & Parameter Specifications

Section 8.1 specified *order*. This specifies *what each stage actually is* — the algorithm family and parameter ranges each subsystem should implement, so a coding agent isn't left guessing at filter types or detector behavior. This is still a product/functional spec, not code — but it's the level of detail an implementer needs to not reinvent (or misjudge) basic DSP decisions.

**Equalizer (6.1/6.2):**
- Each band = a **parametric peaking filter** (biquad IIR, RBJ/Audio-EQ-cookbook coefficient design is the standard reference) rather than an FIR approach — lower latency, standard for real-time EQ.
- Simple Mode's 5 bands should use **moderate Q (~0.7–1.0)** for broad, musical shaping (matches the "Warmth/Body/Clarity" plain-language framing — narrow surgical cuts would contradict the "broad character" framing given to the user).
- Advanced Mode's parametric bands allow **Q from ~0.3 (broad) to ~10 (surgical)**, user-adjustable.
- The two extreme bands (Rumble, Air) should use **shelving filters**, not peaking — a low-shelf and high-shelf respectively — since "boost everything below X" is what a shelf does and is what "Rumble"/"Air" conceptually mean, unlike the three middle bands which are genuinely peak/dip adjustments.

**Dynamics / "Punch" (6.8):**
- **Detector type: RMS-based**, not pure peak-detection — RMS better matches perceived loudness, which is what "evens out quiet and loud parts" actually means to a listener; peak-only detection reacts to transients in a way that doesn't match the plain-language promise.
- **Soft-knee compression** (not hard-knee) — smoother, less audible "pumping," appropriate for a general-purpose "make my old recording sound fuller" tool rather than an aggressive broadcast limiter.
- Simple Mode "Punch" slider should map to a **sensible constrained range** (e.g., ratio ~1.5:1 at low settings up to ~4:1 at max, with attack ~10–30ms and release ~100–300ms as reasonable defaults) — Advanced Mode exposes these four parameters directly plus ceiling/threshold.
- The final **Volume Boost limiter (6.6, always last in the chain per 8.1)** should be a **true-peak brick-wall limiter**, separate from the Punch compressor — its only job is guaranteeing no clipping after all other gain changes, not shaping tone/dynamics.

**Noise Reduction — Hiss Removal, Tier 1 (6.10):**
- **Noise floor estimation:** during quiet passages (detected via a rolling RMS threshold), sample and hold an estimate of the broadband noise floor — a lightweight version of *minimum-statistics noise tracking*, not a full spectral model. This is what lets the Hiss slider auto-calibrate rather than requiring the user to manually specify how much noise exists.
- **Reduction method:** adaptive high-shelf attenuation combined with a **gentle spectral gate** (attenuate below the estimated noise floor, primarily in upper-mid/high frequencies where tape hiss concentrates) — deliberately *not* full spectral subtraction (that's the ML-based Tier 2 territory in 6.10, and naive spectral subtraction produces audible "musical noise" artifacts that would undermine the plain-language promise of a clean fix).
- **De-Hum:** a narrow **notch filter (high Q, ~10–20) at 50Hz or 60Hz** (region-dependent — should auto-detect or let the user pick mains frequency) **plus its first 2–3 harmonics** (100/150/200Hz or 120/180/240Hz), since hum is rarely a single pure tone.
- **De-Crackle:** **transient/click detection** via short-window amplitude-derivative spiking (a sample-level discontinuity detector), followed by brief interpolation/replacement of the flagged samples — this is fundamentally a *different* detection method from the hiss/hum filters above (time-domain spike detection vs. frequency-domain filtering) and confirms why 6.10 correctly treats these as three separate controls rather than one blended "noise" slider.

**Clarity macro (6.9):**
- **Multiband split** into at minimum 3 bands (low/mid/high) using linkwitz-riley or similar crossover filters, so the "de-harsh" and "presence boost" behaviors can be applied independently per band rather than as one broad EQ move.
- **De-harshening:** a **dynamic EQ or multiband compressor** targeting the 2–6kHz region specifically when it spikes above a threshold (harshness/sibilance is transient and level-dependent, which is why this can't just be a static EQ cut without also dulling quiet passages).
- **Exciter/presence:** subtle **harmonic saturation/waveshaping** applied to the high-mid band only, adding low-order harmonics to increase perceived clarity/definition without raw volume increase — this is the specific technique that separates the "Clarity macro" from a plain EQ boost, per the distinction already made in 6.9.

**Virtualizer / "Space" (6.3) + mono-detection (FR-4):**
- **Mono detection method:** compute the **correlation coefficient between L and R channels** over a short rolling window; a coefficient near +1 indicates mono/near-mono content, triggering the Space slider's cap per FR-4. This is a standard, cheap, real-time-safe technique — no ML needed for this specific detection.
- **Core virtualizer technique:** crosstalk cancellation (the classic "transaural" approach — feeding a filtered, delayed, inverted-polarity version of each channel into the opposite channel) combined with **generic HRTF convolution** for the "Narrow/Natural/Wide" profiles in Advanced Mode.

**Reverse Time Machine / "Vintage-ify" (6.13):**
- **Wow-and-flutter:** implemented as a **low-frequency oscillator (LFO) driving a small variable delay line**, modulating pitch/timing — wow is the slow component (~0.5–2Hz), flutter is the faster component (~4–8Hz); both should be present at low depth (a few cents of pitch deviation) for authenticity without sounding like an obvious "wobble" effect.
- Noise synthesis (added hiss/hum/crackle) reuses the **same filter primitives** as 6.10's detectors, run as generators instead of detectors — i.e., filtered noise added at controlled levels rather than a wholly separate synthesis engine, keeping this feature's engineering cost low as noted in 6.13.

**Loudness / Volume Boost (6.6) — reference standard:**
- Use **ITU-R BS.1770 (LUFS)** as the loudness measurement standard for any normalization or "how loud is this" internal logic, rather than simple peak or RMS metering — this is the modern industry standard (used by Spotify/YouTube/broadcast loudness normalization) and keeps the app's internal loudness math consistent with what streaming platforms already do to source audio.
- **True-peak limiting** (oversampled peak detection, not sample-peak) on the final limiter stage — inter-sample peaks that a naive sample-peak limiter would miss are a common source of audible clipping-like distortion after aggressive processing chains like this one.

### 8.4 Audio Format & Buffer Handling

- **Sample rate:** process natively at the source's sample rate (44.1kHz or 48kHz are the two realistic cases from streaming/local files) rather than forcing a fixed internal rate — avoids unnecessary resampling artifacts and CPU cost.
- **Bit depth:** internal processing should run at **32-bit float**, regardless of the source file's bit depth, to avoid quantization noise accumulating across a chain this long (6+ processing stages per 8.1) — converting back to the output format only at the final stage.
- **Buffer size:** should target Android's **reported native buffer size** (via `AudioManager.getProperty` for low-latency devices) rather than a fixed hardcoded buffer, since this varies significantly across OEMs and directly affects the total latency budget specified in the Non-Functional Requirements (section 15, <20ms target).
- This ties directly back to the cross-version strategy in section 9 — buffer size and achievable latency will differ across the OEM matrix already defined there, so latency testing (section 17) should be measured per-device-tier, not assumed uniform.

## 9. Cross-Android-Version Support Strategy

Android's audio effect APIs are not uniform across versions or OEMs, and this is the #1 review complaint on every competitor app referenced in section 10. This needs an explicit strategy, not an assumption that "it'll just work."

### Tiered feature availability by API level

| Android version | Guaranteed | Conditional |
|---|---|---|
| 5.0–8.1 (API 21–27) | Basic Equalizer, BassBoost, Virtualizer (stable, old APIs) | No `DynamicsProcessing` → "Punch" falls back to the app's own lightweight compressor implementation instead of the OS effect |
| 9.0+ (API 28+) | Full `DynamicsProcessing` → real Punch/compressor/limiter chain | — |
| 10+ (API 29+) | Improved per-app audio focus handling | Spatial audio tier depends on OEM support |
| 12+ (API 31+) | Access to newer Bluetooth LE Audio hooks | Head-tracked spatial only if headphone hardware exposes it |

### Required strategies
- **"Legacy Mode"** (naming intentionally matches Wavelet's existing convention so it's recognizable to anyone who's used similar apps): on OEM skins where system-wide audio hooking is unreliable, fall back to an in-app player mode where the app controls its own playback pipeline directly rather than intercepting other apps' output. Guarantees core features work for *something* even where system-wide hooking fails.
- **OEM testing matrix is a required pre-launch QA step**, not an engineering afterthought: Samsung (OneUI), Xiaomi (MIUI/HyperOS), and stock Android (Pixel) minimum, since these three diverge the most in third-party audio hooking behavior.
- **Graceful degradation, never silent feature-gating**: every unavailable advanced feature must tell the user why ("Your device doesn't support head-tracked spatial audio") rather than disappearing without explanation — ties back to UX Principle 4/5 in section 7.
- **Minimum viable OS target for v1 full feature set:** Android 9 (Pie), matching the `DynamicsProcessing` requirement shared by Wavelet/Poweramp. Legacy Mode extends basic EQ/Virtualizer/BassBoost support back to Android 5.0 for reach. Final cutoff should be validated against current Play Store Android-version distribution data at build time, not fixed permanently by this document.

---

## 10. Competitive Landscape

| App | Strength | Gap this app fills |
|---|---|---|
| Wavelet | Device-tuned AutoEQ, real limiter/compressor | Zero plain-language explanation; pure Hz sliders; no noise reduction |
| Poweramp | Strong parametric EQ + DVC | Built for people who already know audio terms; no noise reduction |
| XEQ / Music Volume EQ | Simple presets, easy UI | Genre-based presets, not problem-based diagnosis; no restoration features at all |

None of them do the "what's wrong with my song → fix it" translation layer, and **none of them address noise/hiss/hum reduction at all** — that's a second, independent wedge on top of the plain-language wedge.

---

## 11. Data & Settings Model (conceptual — not a schema for implementation, just what needs to persist)

- **User profile settings:** per-output-device saved chain state (EQ values, Punch, Space, Hiss Removal, Volume Boost level) — keyed by device identifier (headphone model / Bluetooth device ID / "phone speaker").
- **Per-app overrides (P1):** optional settings keyed by the source app's package name.
- **Presets:** named, exportable/importable bundles of the full chain state (all sliders across EQ, Space, Punch, Clarity, Hiss Removal).
- **Wizard history (optional, P2):** last complaint selected + resulting adjustment, so the app can suggest "last time you said this sounded thin, want the same fix?" for the current track/session.
- All of the above should be **local-only in P0/P1** — no account system or cloud sync required for core functionality (ties to Non-Goals in section 4).

---

## 12. Permissions & Minimal-Friction Architecture

This needs to be a deliberate architectural split, not an afterthought — the single biggest reason competitor apps feel "heavy" is that they ask for permissions upfront, or require the same permission tier for simple and advanced features alike. This PRD requires strict separation.

### 12.1 Why most of this app needs *zero* dangerous runtime permissions

The stock effects this PRD already relies on for the majority of P0 features — `Equalizer`, `BassBoost`, `Virtualizer`, `DynamicsProcessing`, `LoudnessEnhancer` — are attached to an existing audio session via Android's standard session-broadcast mechanism, not by capturing raw audio in the app's own process. The actual signal processing runs inside Android's audio framework itself. This is why a well-built EQ app can feel instant and permission-free: **there's nothing to request, because the app never touches the raw PCM stream.**

**Every feature that can be built this way should be** — this is a hard architectural preference, not just a nice-to-have:

| Feature | Mechanism | Dangerous permission required? |
|---|---|---|
| Simple/Advanced EQ (6.1/6.2) | Stock `Equalizer` effect | None |
| Space / Virtualizer (6.3) | Stock `Virtualizer` effect | None |
| Punch / Dynamics (6.8) | Stock `DynamicsProcessing` effect (API 28+; see 9 for fallback) | None |
| Volume Boost (6.6) | Stock `LoudnessEnhancer` effect | None |
| Hiss Removal Tier 1, De-Hum, De-Crackle (6.10) | Custom filters, but can run as *additional* `AudioEffect`-style processors attached the same way as the stock ones — no raw capture required | None |

### 12.2 Features that genuinely require a heavier permission — and how to minimize the damage

| Feature | Why it needs raw audio access | Required permission/consent | Mitigation strategy |
|---|---|---|---|
| Clarity macro (6.9) applied to **live playback from other apps** (e.g. Spotify) | Multiband exciter processing isn't a stock effect type | `AudioPlaybackCaptureConfiguration` + one-time `MediaProjection` system consent dialog | **Only trigger this for the "live enhancement" use case.** |
| Tier 2 ML noise reduction (6.10) | Needs the actual waveform for a neural model | None, IF scoped as file-based (see mitigation) | **Already scoped in 6.10 as an offline "Enhance this track" action on a file the user explicitly opens** — reading a file the user picked via the system file picker needs no dangerous permission at all. Keep it this way. |
| Vintage-ify (6.13) | Needs raw waveform to add noise/wow-flutter | None, IF scoped as file/recording-based | Same fix as above — Vintage-ify should operate on **a file or voice memo the user explicitly selects**, not on live system audio. This sidesteps the capture-consent dialog entirely for this feature's primary use case. |
| Sonic Glass audio-reactive rendering (22.7) | Needs live amplitude/frequency data to drive visuals | Can be sourced from the **app's own playback**, not other apps' audio, if Daydream Audio has its own built-in player mode | Scope Sonic Glass reactivity to audio the app itself is playing (its own player, or the file being actively processed), not a live capture of another app's stream. |

**The one deliberate exception, called out explicitly:** if product ever wants "enhance Spotify/YouTube Music in real time" as a live feature (rather than the file-based flows above), that unavoidably needs the `MediaProjection` capture consent — Android has no lighter-weight path for reading another app's live audio. **Recommendation: do not build this in P0/P1.** The file-based/own-player flows already cover Persona A and B's real use cases (restoring a specific old recording, vintage-ifying a specific clip) without ever showing that dialog. This should be treated as a deliberate, permanent scope boundary, not a temporary limitation — reflected in the "Out of Scope" list in section 23.

### 12.3 Secondary permissions — request contextually, never upfront

| Permission | Needed for | When to request |
|---|---|---|
| `BLUETOOTH_CONNECT` (dangerous, API 31+) | Reading connected headphone/device *name* for Auto-Profile (6.4) | **Only when the user taps "Auto-detect my headphones."** Default/fallback: a manual device-profile picker requiring no permission at all — this must exist as a first-class option, not a degraded one. |
| `POST_NOTIFICATIONS` (dangerous, API 33+) | Only relevant if a persistent background service/notification is ever needed | **Not required for Tier A (12.1) features at all** — stock `AudioEffect` attachment does not require a foreground service. Only request this if a Tier B (12.2) background use case is added later, and even then, prefer a non-persistent, dismissible notification model over one that requires the permission at all if avoidable. |
| Storage/file access | Opening a specific file for restoration/Vintage-ify (6.10 Tier 2, 6.13) | Use the **system file picker (`ACTION_OPEN_DOCUMENT`)**, which requires **no storage permission at all** on modern Android — this is a well-known but often-missed way to avoid `READ_EXTERNAL_STORAGE` entirely. |

### 12.4 Product principle (add to section 5's "Translate the Slider" philosophy)

Every permission request in this app should follow the same plain-language rule as every slider: **explain what it's for, in one sentence, at the exact moment it's needed** — e.g. *"To auto-detect your headphones, we need Bluetooth access. You can skip this and pick your device manually instead."* Never a permissions wall at launch. This is as much a trust/retention feature as it is a technical one — the smoothness the user experienced yesterday **is** the product decision, not an accident, and Daydream Audio's whole P0/P1 scope should be achievable with the app requesting **nothing** on first launch.

---

## 13. Prioritization

**P0 (v1 ship-blocking):**
- Simple Mode 5-band EQ + wizard (6.1)
- Advanced Mode parametric EQ (6.2)
- Space/virtualizer with mono-detection cap (6.3)
- Auto device profile (6.4)
- Volume Boost with auto-limiter (6.6)
- Static spatial audio (6.7a)
- Punch / dynamic range control, both simple and advanced (6.8)
- Hiss Removal + De-Hum + De-Crackle, real-time lightweight tier (6.10 Tier 1)
- Cross-version Legacy Mode fallback (9)
- Correct signal chain ordering (8.1)

**P1 (fast follow, not blocking v1):**
- Per-app memory (6.5)
- Clarity macro, full version (6.9) — may ship a simplified version in P0 if the exciter chain is ready in time, otherwise defers here
- Preset import/export (6.2)
- Time Machine Presets (6.11) — pure curation on top of P0 subsystems, cheap to add once 6.8/6.10/6.2 exist
- Golden Ear Trainer, base version without accuracy tracking (6.12)
- Reverse Time Machine / "Vintage-ify" (6.13) — reuses 6.8/6.10/6.11 subsystems in reverse; only new DSP is the wow-and-flutter modulator

**P2 (research spikes / v2 candidates — explicitly not scheduled):**
- Head-tracked spatial audio (6.7b)
- ML-based offline noise restoration, "Enhance this track" (6.10 Tier 2)
- Wizard history / smart suggestions (11)
- Per-song memory granularity (6.5)
- Golden Ear Trainer accuracy tracking / "Golden Ear Level" (6.12 stretch)
- Memory Postcard shareable export (6.14) — sequenced after 6.13 and 22.7 are both stable; treat as a growth-lever milestone once core restoration/UI are proven, not a v1 commitment

---

## 14. Functional Requirements (FR-IDs + acceptance criteria)

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-1 | Simple Mode displays 5 plain-language EQ sliders | Labels never show raw Hz unless "Show technical values" is enabled in settings |
| FR-2 | Wizard maps user complaint to correct subsystem | "Sounds hissy" routes to Noise Reduction, not EQ; all 5 complaints in 6.1 table produce the specified adjustment |
| FR-3 | A/B toggle switches processed/unprocessed audio | Switch completes in <50ms with no audible glitch/pop |
| FR-4 | Space slider auto-detects mono sources | On confirmed mono input, Space is capped at a reduced max and a plain-language warning is shown before the user can override |
| FR-5 | Volume Boost includes automatic limiting | No clipping/distortion is audible or measurable (peak level) at any Loudness slider position |
| FR-6 | Punch control available in both Simple and Advanced Mode | Simple Mode exposes one merged slider; Advanced Mode exposes threshold/ratio/attack/release/ceiling separately |
| FR-7 | Hiss Removal, De-Hum, De-Crackle are separate controls | Each has its own toggle/slider and independent plain-language description; none are merged into a single "noise" slider |
| FR-8 | Signal chain follows the order specified in 8.1 | Verified via test tone/measurement that Noise Reduction → EQ → Clarity → Dynamics → Virtualizer → Volume Boost order is respected |
| FR-9 | Feature availability degrades gracefully by Android version/OEM | Unsupported feature shows an explanatory message; app never crashes due to missing effect API |
| FR-10 | Auto device profile detection on output change | Switching from wired to Bluetooth to speaker prompts a profile suggestion within 2 seconds of the switch |
| FR-11 | Legacy Mode activates when system-wide hooking fails | App detects hooking failure (or known-bad OEM condition) and offers in-app-only playback mode rather than failing silently |
| FR-12 | Preset export/import | A saved preset file, when imported on a second device/install, reproduces the identical chain state |
| FR-13 | App requests zero permissions on first launch | All Tier A features (12.1) are usable immediately after install with no permission dialog shown |
| FR-14 | Dangerous permissions are requested contextually, not upfront | `BLUETOOTH_CONNECT` is only requested when the user taps "Auto-detect my headphones"; a no-permission manual device picker is always available as an alternative |
| FR-15 | File-based Tier B features avoid storage permissions | Opening a file for Vintage-ify (6.13) or Tier 2 restoration (6.10) uses the system file picker (`ACTION_OPEN_DOCUMENT`), never `READ_EXTERNAL_STORAGE` |

---

## 15. Non-Functional Requirements

- **Latency:** total added processing latency across the full chain should stay under ~20ms to avoid perceptible lag versus unprocessed audio, especially relevant for video/call use cases.
- **Battery/CPU:** real-time chain (P0 scope) must be lightweight enough for all-day background use without notable battery drain — this is a hard constraint specifically because Tier 1 noise reduction and dynamics are both always-on possibilities.
- **Stability:** no crashes attributable to audio effect initialization across the OEM test matrix in section 9.
- **Accessibility:** all plain-language descriptions should also be screen-reader friendly (no reliance on visual-only slider position to convey meaning).

---

## 16. Success Metrics

- % of new users completing the "What's wrong with my song?" wizard in session 1 (target: >60%)
- % of users who ever open Advanced Mode (signals whether Simple Mode alone satisfies most people)
- % of users engaging with Noise Reduction specifically (validates whether this is the differentiator it's intended to be)
- D7/D30 retention on *any* active EQ/virtualizer/noise-reduction setting (not just app opens)
- Review sentiment: reduction in "I don't understand these sliders" and "doesn't work on my phone" complaint categories relative to competitor baselines

---

## 17. Testing Strategy

- **OEM matrix testing** (section 9) as a release gate, not optional QA — Samsung, Xiaomi, Pixel minimum, across at least 3 Android version tiers (Legacy/9-era/current).
- **Signal chain verification**: automated test-tone/measurement pass to confirm effect ordering (8.1) is respected and no stage introduces clipping.
- **Mono-detection accuracy testing**: verify Space-slider capping triggers correctly across a range of real mono and "fake stereo" source files.
- **A/B toggle latency testing**: confirm the <50ms switch requirement (FR-3) under real device conditions, not just emulator.
- **Battery/CPU profiling**: sustained playback sessions (1hr+) across the P0 chain to validate the non-functional requirement in section 15.

---

## 18. Risks / Open Questions

- **Risk:** Plain-language frequency mapping (e.g., "Body" = 250Hz–2kHz) is a simplification — real mixes vary, so wizard auto-fixes will sometimes be imperfect for a given track. Needs a nudge/correct path, not just re-running the wizard from scratch.
- **Risk:** OEM audio hooking reliability (section 9) is the single biggest source of 1-star reviews across every competitor — Legacy Mode is the mitigation, but its UX (in-app-only playback) is a real limitation that should be communicated honestly, not buried.
- **Risk:** Tier 2 noise reduction (6.10) and the Clarity exciter chain (6.9) both plausibly require embedding an ML model or heavy custom DSP — this has real APK size, battery, and dev-time cost that shouldn't be underestimated just because it's "just noise reduction" conceptually.
- **Open question:** Monetization model — one-time unlock (Wavelet-style) vs. subscription vs. free+ads — not decided in this doc (see section 19 for options only).
- **Open question:** Does Space (virtualizer) or Hiss Removal need dedicated onboarding tutorials, or do inline warnings/descriptions suffice?
- **Open question:** Is cloud-assisted Tier 2 noise reduction acceptable given the local-only/offline goal in section 4, or should that entire tier be deferred until a fully on-device model is validated as performant enough?

---

## 19. Monetization (options only — not decided)

- One-time unlock for Advanced Mode + Tier 1 restoration features (Wavelet-style, ~₹400–500 range as a reference point from competitor pricing).
- Free tier: Simple Mode + wizard + basic EQ/Space/Punch; Paid tier: Advanced Mode + full Hiss/De-Hum/De-Crackle + presets.
- Any Tier 2 (P2) ML-based restoration, if built, would likely justify a separate premium/consumable pricing model given its higher compute cost — to be decided if/when that tier is greenlit.

---

## 20. Roadmap / Milestones (indicative, not date-committed)

1. **M1 — Core chain + Simple Mode:** EQ (6.1/6.2), Space (6.3), device profiles (6.4), correct signal chain (8.1), cross-version Legacy Mode (9).
2. **M2 — Restoration P0:** Punch (6.8), Hiss/De-Hum/De-Crackle Tier 1 (6.10), Volume Boost with limiter (6.6).
3. **M3 — Polish + P1:** Clarity macro (6.9), per-app memory (6.5), presets (6.2), static spatial audio (6.7a).
4. **M4 — Research spikes (P2, unscheduled):** head-tracked spatial (6.7b), ML-based Tier 2 restoration (6.10 Tier 2), wizard history (11).
5. **M5 — Growth layer (post-launch, once M1–M3 are stable):** Reverse Time Machine / "Vintage-ify" (6.13), Memory Postcard shareable export (6.14) — this milestone is explicitly about turning the app into a proactive creative tool with a viral sharing loop, not just a restoration utility, and should only be pursued once the core product (M1–M3) is proven and stable.

---

## 21. Glossary (plain-language ↔ technical mapping)

| Plain label | Technical term | Notes |
|---|---|---|
| Rumble | Sub-bass EQ band (~20–60Hz) | |
| Warmth | Bass EQ band (~60–250Hz) | |
| Body | Low-mid EQ band (~250Hz–2kHz) | |
| Clarity (EQ band) | High-mid EQ band (~2–6kHz) | Distinct from the "Clarity macro" |
| Air | Treble EQ band (~6–16kHz) | |
| Space | Virtualizer (crosstalk cancellation + HRTF + decorrelation) | See mono-detection requirement (FR-4) |
| Punch | Dynamic range compressor + limiter | Simple Mode merges ratio + makeup gain into one slider |
| Loudness | LoudnessEnhancer + auto soft limiter | |
| Clarity macro | Multiband de-harshener + presence boost + exciter | Not a simple EQ band |
| Hiss Removal | Noise gate + adaptive high-shelf attenuation (Tier 1) or ML noise suppression (Tier 2) | Two tiers, see 6.10 |
| De-Hum | Notch filter at 50/60Hz + harmonics | |
| De-Crackle | Transient/click detection and suppression | |
| Spatial Sound | Extended HRTF room simulation (static) or head-tracked spatial (P2) | |
| Vintage-ify | Reverse-mode restoration chain: noise synthesis + dynamic range compression + wow-and-flutter modulation | See 6.13 — reuses 6.8/6.10/6.11 subsystems in reverse |
| Memory Postcard | Rendered audio+visual export combining processed audio with the Liquid Glass/Sonic Glass UI | See 6.14 — shareable video export, sequenced after 6.13 and 22.7 |

---

## 22. UI/UX Design System — "Liquid Glass"

### 22.1 Design philosophy

The entire interface should feel like it's carved out of a single sheet of glass floating over the user's chosen background (a photo, or — see 22.6 — the current track's album art), rather than opaque cards sitting on top of a wallpaper. This directly mirrors Apple's Liquid Glass material language: translucency, specular highlight, real-time refraction, and content-aware adaptive contrast, reinterpreted here so the *glass itself becomes part of the audio-app metaphor* rather than a purely cosmetic skin (see 22.7 for the creative addition that makes this functional, not just decorative).

This is a visual identity decision with real technical weight — it affects performance budget, accessibility, and cross-version support (22.9), so it needs to be treated as seriously as the DSP sections above, not bolted on at the end.

### 22.2 Material hierarchy (three glass elevations)

| Tier | Used for | Blur radius | Opacity | Behavior |
|---|---|---|---|---|
| **Base Glass** | Full-screen background scrim behind all content | Heavy blur, minimal tint | Low (background stays mostly visible through it) | Static, subtly shifts tint based on background luminance |
| **Raised Glass** | Cards/panels — EQ slider group, Time Machine preset cards, wizard cards | Medium blur | Medium | Casts a soft inner shadow/highlight edge to read as "raised" above Base Glass |
| **Floating Glass** | Top nav, bottom tab bar, active modal (e.g. the A/B toggle popover) | Light blur, sharper edge highlight | Higher (most legible layer) | Appears to hover — strongest specular highlight, reacts to scroll/drag with a subtle parallax shift |

This hierarchy exists so a noob user's eye is drawn to the *right* layer without needing borders or drop shadows in the traditional sense — depth is communicated through glass properties alone, consistent with the Liquid Glass reference language.

### 22.3 Background image treatment

- User can set a custom background photo, or default to a curated set of ambient, non-distracting images (soft gradients, blurred bokeh, abstract textures) — never a busy photo that fights with glass legibility.
- The background is never shown "raw" behind primary content — it always sits underneath at least Base Glass, ensuring text/controls stay legible regardless of what image the user picks.
- **Required accessibility fallback:** a "Reduce Glass" setting (same spirit as Apple's own "Reduce Transparency" accessibility toggle) that swaps all three glass tiers for solid, high-contrast backgrounds. This is not optional polish — glass-over-image UIs have a well-known real risk of failing contrast/legibility for low-vision users, and this must ship as a first-class setting, not an afterthought.

### 22.4 Sliders as "liquid" controls

- Every slider (EQ bands, Punch, Space, Hiss Removal, Volume Boost) renders its draggable thumb as a soft, glass-like blob rather than a flat circle — subtle squash/stretch and a moving specular highlight as it's dragged, reinforcing the "liquid" part of Liquid Glass rather than just "frosted panel."
- Track fill (the portion of the slider already engaged) reads as a denser, more saturated glass rather than a flat color bar, so the amount of effect applied is communicated through material density, not just position.
- This needs to stay subtle — the motion should read as "premium," not "bouncy/game-like," given the app's core promise is trustworthy audio fixing, not a toy.

### 22.5 Typography & iconography

- Rounded, geometric sans-serif (Android equivalent of SF Pro Rounded — e.g. a variable-weight font like Google's own rounded system font) for a soft, friendly feel that matches the noob-friendly mission, not a technical/engineering typeface.
- **Adaptive legibility requirement:** text color/weight must dynamically adjust based on the luminance of whatever's behind the glass at that moment (light background image → darker/heavier text; dark image → lighter text) — same principle as Liquid Glass's own adaptive contrast behavior. This cannot be a fixed color choice made at design time; it needs to be computed at render time against the actual background.
- Icons follow a consistent thin-stroke, rounded-corner style so they read as etched into the glass rather than pasted on top.

### 22.6 Dynamic content: album-art-aware glass

- When music is actively playing, the background can optionally shift from the user's static photo to a heavily blurred, color-extracted version of the current track's album art (same pattern as Apple Music/Spotify's now-playing screens) — glass panels above it pick up a subtle ambient tint from the art's dominant color.
- This should be opt-in (some users will prefer a stable static background over one that shifts per song) and must respect the same Base Glass blur/legibility rules in 22.3 — the art is a mood layer, never a legibility risk.

### 22.7 Creative addition: "Sonic Glass" — audio-reactive material (NEW)

This is the one idea worth adding that makes the glass metaphor actually *mean something* for an audio app rather than being a borrowed Apple aesthetic: **the glass should visibly react to the audio itself.**

- Bass hits and transients (kick drums, strong low-end content) trigger a soft, low-amplitude ripple/refraction distortion that radiates outward from the Base Glass layer — literally "liquid" glass responding to sound, like a droplet hitting still water. Subtle enough to never distract from reading a slider, but present enough to feel alive.
- The ambient tint of Floating/Raised Glass panels can shift *very subtly* in real time based on which plain-language band (Rumble/Warmth/Body/Clarity/Air) currently has the most energy in the playing track — e.g. a faint warm tint when bass-heavy content plays, a cooler tint on treble-heavy passages. This is not a full visualizer or spectrum analyzer (that would clutter the noob-friendly UI) — it's an ambient, almost subconscious cue.
- **Direct tie-in to the Golden Ear Trainer (6.12):** during that mini-game specifically, the ripple/tint reactivity can be made more pronounced as a *visual hint layer* — sighted users get a secondary (optional, toggleable) visual cue reinforcing which band just changed, turning the glass into a teaching tool, not just decoration.
- **Engineering-risk note:** real-time audio-reactive rendering (analyzing the live audio buffer and driving a shader-based ripple/tint effect) has real GPU/battery cost and should be scoped as P1/P2, not P0 — ship the static Liquid Glass system first (22.1–22.6), then layer in Sonic Glass reactivity once the base visual system is proven stable. It should also respect the "Reduce Motion" and "Reduce Glass" accessibility settings (22.3) by disabling itself entirely, not just toning down.

### 22.8 Screen-by-screen application (mapping the design system onto existing PRD screens)

| Screen | Glass treatment |
|---|---|
| Onboarding / wizard (7) | Floating Glass cards for each complaint option, over a calm default background |
| Simple Mode sliders | Raised Glass panel grouping all 5 EQ bands + Punch + Space + Hiss Removal, each slider using the liquid thumb style (22.4) |
| Advanced Mode | Denser Raised Glass grid for the full parametric EQ — slightly less blur than Simple Mode so numeric values stay crisp for power users |
| Time Machine presets (6.11) | Horizontal carousel of Raised Glass cards, each subtly tinted per era (e.g. a warmer/sepia glass tint for "60s Mono Transfer") |
| Golden Ear Trainer (6.12) | Floating Glass modal, Sonic Glass (22.7) hint layer enabled by default here specifically |
| Settings | Base Glass throughout — this screen should be the calmest, least decorated, most legible screen in the app, including the Reduce Glass / Reduce Motion toggles themselves |

### 22.9 Cross-version & performance implications (ties to section 9)

- True real-time backdrop blur (the actual Liquid Glass look) relies on hardware-accelerated blur/shader support that isn't uniform across the Android versions and devices this PRD already commits to supporting (section 9).
- **Recommended tiered rendering approach, matching the existing cross-version philosophy:**
  | Capability tier | Rendering approach |
  |---|---|
  | Modern devices/API levels with strong GPU blur support | Full live Liquid Glass: real-time blur, Sonic Glass reactivity (22.7), dynamic album-art background (22.6) |
  | Mid-range/older devices | Precomputed/static frosted-glass look (a baked blur applied once, not recalculated live) — visually similar at a glance, far cheaper to render |
  | Legacy Mode devices (section 9) or "Reduce Glass" users | Solid, high-contrast backgrounds, no blur/translucency at all |
- This tiering must be automatic (device-capability-detected) with a manual override available in Settings, so a user on a capable device who simply prefers a calmer UI isn't forced into the full effect, and a user on a weak device isn't stuck with a laggy one.

### 22.10 Design Tokens — Exact Values (mandatory reference — do not invent new values)

**Spacing scale (8dp base grid):**

| Token | Value |
|---|---|
| space-xs | 4dp |
| space-sm | 8dp |
| space-md | 16dp |
| space-lg | 24dp |
| space-xl | 32dp |
| space-xxl | 48dp |
| screen-margin | 20dp |

**Corner radius tokens:**

| Token | Value | Used for |
|---|---|---|
| radius-sm | 12dp | Chips, small badges |
| radius-md | 24dp | Cards, Raised Glass panels |
| radius-lg | 32dp | Floating Glass sheets/modals |
| radius-pill | 999dp (fully rounded) | Buttons, sliders, nav bar indicator |

**Glass tier values (blur + opacity, light background / dark background):**

| Tier | Blur radius | Fill opacity (on light bg) | Fill opacity (on dark bg) | Border highlight opacity |
|---|---|---|---|---|
| Base Glass | 40dp | White 8% | White 10% | White 15% |
| Raised Glass | 24dp | White 14% | White 18% | White 20% |
| Floating Glass | 16dp | White 22% | White 26% | White 30% |

**Elevation/shadow tokens:**

| Token | Offset Y | Blur | Color |
|---|---|---|---|
| shadow-raised | 4dp | 16dp | Black @ 12% |
| shadow-floating | 8dp | 24dp | Black @ 18% |
| specular-highlight | inner border, 1dp, top-left edge | — | White @ 40%, blend mode "screen" |

**Color tokens:**

| Token | Value | Usage |
|---|---|---|
| accent-primary | Gradient #F5A623 → #F76B1C | Active slider fill, primary CTA — warm/amber, ties to the "Daydream/nostalgia" identity |
| accent-safe | #34C759 | Limiter safe-zone indicator |
| accent-warning | #FF3B30 | Volume Boost past-safe-threshold warning (6.6) |
| text-primary (light bg) | #1C1C1E @ 92% | Body text over light backgrounds |
| text-primary (dark bg) | #FFFFFF @ 95% | Body text over dark backgrounds |
| text-secondary | 60% opacity of the applicable text-primary token | Captions, technical values, inactive nav labels |

**Typography scale (rounded, variable-weight sans-serif — e.g. a Google Sans Rounded–class font):**

| Style | Size | Weight | Line height | Tracking | Usage |
|---|---|---|---|---|---|
| Display | 32sp | 700 | 40sp | -0.5 | Screen titles, wizard headline |
| Title | 22sp | 600 | 28sp | 0 | Card headers, section titles |
| Body | 16sp | 400 | 24sp | 0 | Descriptions, tooltips |
| Label | 14sp | 500 | 20sp | 0.1 | Slider plain-language labels ("Warmth," "Clarity") |
| Caption | 12sp | 400 | 16sp | 0.2 | Technical values, shown only when "Show technical values" is on |

**Iconography:** 24dp × 24dp grid, 2dp stroke weight, rounded line caps/joins. Icons are stroke-only by default; an icon fills solid with `accent-primary` only in its active/selected state — never otherwise.

### 22.11 Component Specifications (build to these exactly — do not improvise layout)

**Slider (EQ bands, Punch, Space, Hiss Removal):**
- Track: 6dp height, `radius-pill` ends, inactive portion = Base Glass tier values.
- Active/fill portion: `accent-primary` gradient; opacity/saturation increases as value increases, so the fill visually reads as "more effect = denser glass" (ties to 22.4).
- Thumb: 28dp diameter circle, idle = Raised Glass tier + `specular-highlight`. On drag: scales to 34dp with up to 10% vertical squash at grab, springs back within 150ms (see motion tokens, 22.12).
- Long-press on thumb or label opens the plain-language tooltip (section 5) in a Floating Glass popover, positioned above the thumb, max-width 240dp.
- Disabled (e.g. feature unsupported on this Android version, section 9): drop to 40% opacity, remove blur entirely (render flat, not glass), tap surfaces the "why unavailable" message per FR-9.

**Buttons:**
- Primary CTA (e.g. "Apply Fix"): `radius-pill`, 52dp height, 24dp horizontal padding, Floating Glass fill + `accent-primary` gradient overlay at 90% opacity, label in Title style at 16sp/weight 600.
- Secondary/ghost (e.g. "Skip"): same shape/height, Base Glass fill only, label color = `accent-primary`.
- Pressed state (both): scale to 97%, 100ms ease-out (`ease-standard`), glass fill opacity +10% to simulate pressing into the surface.
- Disabled: 40% opacity, no press animation.

**Cards (Time Machine presets 6.11, wizard complaint options 6.1):**
- `radius-md` (24dp), 16dp internal padding, Raised Glass tier values.
- Selected/active state: border highlight opacity → 100%, plus a 2dp `accent-primary` outline.

**Navigation / Tab bar:**
- Floating Glass tier, 64dp height + safe-area inset. Active tab shows a `radius-pill` indicator sliding behind the icon+label, 250ms `ease-standard`.
- Icons per the 24dp iconography spec; active icon fills `accent-primary`; inactive = `text-secondary`, stroke only.

**Modal / Popover (A/B toggle, tooltips, Golden Ear Trainer):**
- `radius-lg` (32dp) for full sheets, `radius-md` (24dp) for small popovers — both Floating Glass tier.
- Entrance: slide up + fade, 300ms `ease-out-quart`. Exit: 200ms `ease-in-quart`.

**Toggle switch (e.g. "Reduce Glass," "Reduce Motion" in Settings):**
- Track: 51dp × 31dp, `radius-pill`, Base Glass fill when off, `accent-primary` fill when on.
- Thumb: 27dp circle, flat white @ 100% opacity — **deliberately not glass**, since Settings is the calmest, most utilitarian screen per 22.8. 200ms slide, `ease-standard`.

### 22.12 Motion & Animation Specification

**Easing curves (use these exact curves — no per-component custom timing):**

| Token | Curve | Used for |
|---|---|---|
| ease-standard | cubic-bezier(0.4, 0.0, 0.2, 1) | Default for most transitions |
| ease-out-quart | cubic-bezier(0.25, 1, 0.5, 1) | Entrances |
| ease-in-quart | cubic-bezier(0.5, 0, 0.75, 0) | Exits |
| spring-thumb | damping ratio 0.7, stiffness 300 | Slider thumb drag/release interactions specifically |

**Duration tokens:**

| Token | Duration | Used for |
|---|---|---|
| duration-instant | 100ms | Press/release feedback |
| duration-fast | 150–200ms | Toggles, small state changes |
| duration-medium | 250–300ms | Screen transitions, card expand/collapse |
| duration-slow | 400–500ms | Onboarding sequence transitions, Time Machine era-switch full-theme change |

**Sonic Glass ripple (22.7), when enabled:**
- Trigger: onset detection on transients exceeding a set amplitude threshold in the low-frequency band.
- Ripple: expands from origin to ~120dp over 400ms, opacity fades 25% → 0%, `ease-out-quart`.
- Tint shift: cross-fade between current and new ambient tint over 600ms, `ease-standard` — must always crossfade, never snap instantly, to avoid visual jitter.

### 22.13 Agent Compliance Rules (mandatory — not suggestions)

These exist specifically so multiple build passes stay visually consistent without a human re-reviewing every screen:

1. **Never introduce a corner radius, spacing value, blur radius, or color outside the tokens in 22.10.** If nothing fits, flag it for review rather than inventing a one-off value.
2. **Every glass surface must use exactly one of the three defined tiers (Base/Raised/Floating) — no ad hoc opacity/blur combinations anywhere in the app.**
3. **All text must pass WCAG AA contrast (4.5:1 minimum) against whatever is actually behind it at render time** — backgrounds are dynamic (user photos, album art per 22.6), so this cannot be assumed from a static design mock; compute it live.
4. **"Reduce Glass" and "Reduce Motion" must fully disable — not partially reduce — blur, transparency, ripple, and animation effects** when enabled. Partial compliance defeats the accessibility purpose in 22.3.
5. **Never hardcode a raw color value inside a component — always reference the semantic token** (`accent-primary`, `text-secondary`, etc.), so a future rebrand or theme pass doesn't require touching every screen individually.
6. **All motion must use the duration/easing tokens in 22.12 — no per-component custom timing curves.**
7. **Any screen not explicitly mapped in the table in 22.8 must default to the nearest analogous tier before implementation — when in doubt, treat it like Settings** (Base Glass, minimal decoration) rather than guessing toward something more elaborate.

---

## 23. Out of Scope (v1)

- Full AI stem separation / studio-grade remastering
- Cross-device cloud sync of settings/presets
- iOS version
- Social/sharing features beyond preset file export
- Streaming service partnership APIs
- **Real-time enhancement of other apps' live audio streams (e.g. "enhance Spotify live") for any Tier B feature (Clarity macro, Vintage-ify, Tier 2 noise reduction)** — this requires `MediaProjection` capture consent with no lighter alternative on Android, and is a deliberate permanent scope boundary (see 12.2), not a temporary limitation. All Tier B features operate on files/recordings the user explicitly selects instead.
