# Upekkhā 2.1

Native Kotlin Android breathing-practice application. Minimum Android 8.0 / API 26; target API 35; applicationId `org.breatheless.app`; versionCode 3, versionName 2.1.

## Download

The installable debug APK is distributed as an asset of the GitHub release tagged `v2.1`. The release includes a SHA-256 checksum file.

## Updating from v2

Install `Upekkha-v2.1-debug.apk` over v2. The package and signing certificate are unchanged, and versionCode increases from 2 to 3. Existing local data is retained by Android during an update. Version 2.1 changes the launcher label, in-app name, notifications, media metadata, diary branding and export filenames to Upekkhā. The historic backup format identifier is preserved for compatibility. An actual device update was not exercised here.

## Installation and v1 data

**This build cannot be installed over v1.** The original v1 signing key was removed by workspace maintenance and was not included in the saved v1 source archive. Android requires the old key for an in-place update. A signed APK or certificate cannot reconstruct that key.

Before uninstalling v1:

1. In v1, use Settings → Save a complete backup. Save the JSON outside the app's private storage, for example in Downloads.
2. Keep that backup. Uninstalling an Android app removes its private data.
3. Uninstall v1, install `Upekkha-v2.1-debug.apk`, and use Settings → Restore or merge backup.
4. Check the restored diary and custom protocols.

The v2 backup reader retains v1's schema and merges missing IDs while preserving existing records. Its parser is covered by tests; Android file-picker/disk integration was not device-tested here. If the original v1 key becomes available, this source can be re-signed for a direct v1 update, but that would be a different signing series from the v2 build supplied here.

## New in v2

- Pure black page, panel and splash backgrounds; dark dialogs, muted text, low-brightness controls, adjustable app brightness, and a dim timer view.
- Direct interface copy with the previous taglines removed. No emojis.
- The supplied Mindfulness Buteyko recording by Marilleke Vrancken is bundled unchanged as one MP3 asset and streamed from the APK.
- Default audio sequence: 5-minute settling rest → CP → 20:27 recording → 5-minute recovery → further 5-minute wait → CP. Both rest intervals are editable in the protocol editor. Playback completion, rather than an estimated phase deadline, triggers recovery.
- Recording pause/resume, scrubber and 15-second seek controls; pause on audio interruption; no ambience over the recording.
- Twenty tone choices, including silence; independent choices for Rest, Reduced breathing, Recovery, CP readiness, Recording, CP stopped, and Session finished. Individual previews and a tone-volume control.
- Six vibration styles with an intensity slider and a test button. Devices without amplitude control use the selected pattern at supported system strength; the app reports that limitation.
- End-of-session sound is allowed to finish before the service exits.
- Favourites, repeat-last-protocol, filtered diary views, CP summary in session notes, and a dim timer layout.
- Notification controls and supported headset media buttons; optional volume-key timer controls while the timer screen is open.

The v1 functions remain: six original practice presets plus the audio activity, custom phases/protocols, manual CP quality review and exclusion, pulse entry/counting, walking step sensor/manual fallback, symptom score, diary and comments, 7/30/90/all-time CP charts, morning filter, CSV/image exports, local backup/merge restore, offline ambience, safety information and source-backed background reading.

## Signals and controls

Sound uses the phone's **alarm stream**; the recording and ambience use the **media stream**. The app's tone-volume slider scales the phase cues further. Check both streams on the phone, particularly with headphones and Do Not Disturb enabled.

The headset main action starts a ready CP, stops a running CP, keeps a reviewed CP, or pauses/resumes a timed phase. Next excludes a reviewed CP. Interrupted CP values remain excluded. Optional Volume up performs the same main action while the timer screen is open; Volume down stops a running CP, excludes a reviewed CP, or pauses/resumes. Support and routing depend on the device and headset.

The app never starts a breath hold automatically. Software stop limits are guardrails, not targets. Headset operation with the display off still requires attention to breathing and CP recovery quality.

## Storage

The MP3 is present once, uncompressed in the APK, and opened through its asset file descriptor. Playback does not extract or cache a second copy. Audio is not transcoded. Small generated WAV cues and two Ogg ambience loops are bundled separately. Application data uses atomic JSON files. Export cache files older than seven days are removed on later exports; older files are also trimmed above 128 MiB. Very large image exports are limited to 200 pages; the CSV includes the full diary.

There is no network permission or account requirement. Backups and exports are unencrypted files selected by the user. Android cloud app backup is disabled. No email is sent automatically.

## Build and future updates

Requirements: Java 17, Kotlin compiler 2.1.20, Android SDK platform 35 and build-tools 35.0.0, official D8 8.9.35. Use `build-apk.py`. The environment variables `ANDROID_HOME`, `KOTLIN_HOME`, `BUILD_TOOLS`, `ANDROID_JAR` and `R8_JAR` may point to installed toolchain components.

The public signing identity and certificate are included in `signing/`. The private keystore and password are excluded from Git and must be restored locally from the separate private signing backup before running a signed build. Restore them as `signing/breatheless-update.p12` and `signing/password.txt`; both paths are ignored by Git. Keep that backup private and retain it. The build script fails if they are missing and verifies the resulting certificate against `signing/identity.json`; it never generates a replacement key. Future versions must use the same applicationId and key and increase versionCode above 3. Read `AGENTS.md` before future edits.

No compiler/SDK caches or private signing backup are included in this repository. The project includes the successful build log, core test output, APK/asset inspection, signing identity and source checksums.

## Verification

See the 2.1 build report and retained v2 verification notes. Core tests, compilation, DEX generation, signing, signature verification, package/asset integrity and alignment are checked. There was no usable Android device/emulator; installation, UI rendering, physical sound/vibration, screen-off timing, headset controls, sensor readings, file-picker integration and email handoff remain unverified on a phone.
