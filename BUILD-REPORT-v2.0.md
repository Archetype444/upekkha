# BreatheLess 2.0 — build and installation report

Built and verified 21 October 2026.

## APK

- File: `BreatheLess-v2.0-debug.apk`
- Size: 27,273,531 bytes (26.01 MiB)
- Package: `org.breatheless.app`
- Version code/name: 2 / 2.0
- Android: 8.0 (API 26) or later; target/compile API 35
- Native Kotlin; debug build; no CPU-specific native libraries

## Update compatibility: v1 requires a one-time reinstall

**This APK cannot update the previously supplied v1 in place.** Workspace maintenance removed the original signing key, and I had excluded that key from the saved v1 project archive. Android update signing depends on that exact key. The saved APK and its public certificate cannot reconstruct the private key.

The package ID and version counter are retained, but the v2 certificate differs from v1. This is an unresolved limitation against the requested direct v1 update; it is not presented as a successful update test.

Before removing v1, save a complete JSON backup from its Settings screen to a location outside private app storage, such as Downloads. Keep that file, uninstall v1, install v2 and restore/merge the backup. Uninstalling v1 before exporting its data would remove that private data. The schema-1 reader remains compatible, and an independent literal v1 backup fixture is included in the passing tests. Actual file-picker and restore-on-device integration was not tested here.

The exact v2 key, password file and public certificate are preserved in the private signing-backup ZIP and project archive. The build script fails if the identity is missing or changes, and checks the final APK certificate against the pinned fingerprint. Future versions can update this v2 series by keeping the application ID and key and increasing versionCode. An installed v2-to-future-version update was not exercised on a phone. If the original v1 key is supplied, this source can be re-signed for that original series; it would differ from the supplied v2 series.

## Requested changes

| Area | Implementation |
| --- | --- |
| Supplied recording | Mindfulness Buteyko by Marilleke Vrancken, unchanged MP3, 20:27.168 |
| Default sequence | 5 min settling rest → CP → recording → 5 min recovery → further 5 min wait → CP |
| Timing | Real media completion starts recovery. Pause/seek/focus interruption does not let an estimated recording timer run ahead. Both rests are editable. CP always waits for user input. |
| Night display | Pure black page, panel, splash and dialog backgrounds; muted text, subtle button fill/borders, brightness control and dim timer view |
| Copy | Previous wellness taglines removed; direct English labels and instructions; no emojis |
| Tones | 20 choices including silence; separate selection for 7 events: rest, reduction, recovery, CP ready, recording, CP stopped and session finished |
| Vibration | 6 patterns; intensity control where amplitude support is reported; device capability notice and test button |
| Audio controls | Playback/pause/resume, scrubber, ±15-second seek; notification/media-session controls; supported headset actions |
| Quality of life | Favourites, repeat last protocol, diary filters, CP summary with notes, optional volume-key controls, dim view, cache cleanup |
| Existing features | Custom protocols, CP/pulse/walking/symptom diary, charts, CSV/PNG exports, JSON backup/merge restore, offline ambience and research/safety reading retained |

The five-minute recovery followed by a further five-minute wait implements the wording “five minutes after a standard rest period at the end.” This places the final CP ten minutes after recording playback ends. The protocol editor can change or remove either rest interval if a different routine is intended.

## Storage

The MP3 occurs **once** in the APK as an uncompressed asset, with the same SHA-256 as the uploaded file. MediaPlayer streams through the asset file descriptor. The app does not extract another playback copy or transcode the recording. Small synthesized WAVs and two FOSS Ogg ambience loops are separate assets. Data and active-session checkpoints use atomic JSON. Export cache cleanup removes files older than seven days and older cache files above 128 MiB. Image reports have a 200-page limit; CSV includes the full diary.

## Verification results

| Check | Result |
| --- | --- |
| Kotlin compilation | Passed; no compilation errors |
| Android resource compile/link | Passed |
| DEX generation | Passed with official D8 8.9.35 |
| Signing | Passed with preserved v2 RSA key |
| APK signature verification | Scheme v2 and v3 verified |
| Signing-identity pin | Final certificate equals saved v2 certificate |
| ZIP alignment | Passed |
| APK ZIP integrity | Passed |
| Package/versions/min SDK/launch activity | Checked |
| Required app/runtime classes | Present |
| Test classes in APK | Absent |
| MP3 | One unchanged, uncompressed copy |
| Audible cue WAVs | 19 valid PCM files, bounded duration; silence is the twentieth choice |
| Internet permission | Absent |
| Automated core tests | **67 passed** |

The 67 tests execute the production engine, JSON codec/settings migration, analytics, cue configuration/pattern calculations and CSV routines. They include recording completion gating, recording pause/resume, the two post-recording waits, CP user gating/review/interruption, invalid values, independent legacy backup parsing, favourites/phase-tone settings, waveform amplitude bounds, statistics and export escaping/formula protection. The test JVM uses a real JSON library ahead of Android API stubs.

Build tools: Java 17, Kotlin 2.1.20, Android SDK platform/build-tools 35.0.0, official D8 8.9.35, AAPT2, zipalign and apksigner. Remaining compiler warnings concern backward-compatible deprecated Android APIs, not compilation errors.

## Device testing limitations

**No Android device or usable emulator was available.** ADB could not start normally in the execution environment. The APK has not been installed or launched on a phone here. There are no claimed UI screenshots, Android instrumentation results or successful in-place update installations.

Physical sound loudness/routing, vibration amplitude, Bluetooth/headset actions, lock-screen controls, audio interruption behaviour, screen-off timing, UI rendering and accessibility, hardware steps, native file-picker/backup/restore writes, rendered image exports and email attachment handoff still require device testing. Code and asset checks do not prove their physical behaviour.

Phase tones use Android alarm volume; the recording and ambience use media volume. Sound and haptic options are independent. Do Not Disturb, output routing, force-stop and manufacturer battery restrictions can affect cues. The app provides separate previews/tests and reports missing vibrator/amplitude support. A tone choice is not a guarantee against system restrictions.

## Sources and credits

The version 1 foundation, including original Buteyko material and the supplied Hyde article, remains represented in the Guide. Historical claims are separated from clinical evidence. No new therapeutic claims are made for the attached recording or this application. The MP3 is the user-supplied Marilleke Vrancken file; it is distinct from the FOSS ambient library. Wind and Fireplace are CC0 loop files from [Muges/AmbientSounds](https://github.com/Muges/ambientsounds), with in-app credits. Phase WAVs are original syntheses. Kotlin license notices are bundled.

Technical references checked: [Android app signing](https://developer.android.com/studio/publish/app-signing), [foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types), and [VibrationEffect](https://developer.android.com/reference/android/os/VibrationEffect).

## Fingerprints

APK SHA-256:
`ca33e2d2493ac7436d220da0531bf3bee490103af96407b6276c1f410fbcdb0b`

V2 signing certificate SHA-256:
`a5edb5474ed2435c35c086b1920a0acfdfeac91ad00c7fb6659c9017a44e2268`

Unchanged recording SHA-256:
`b6468bc0e09e70e2ba9723b463c748947b96c6f8808111a769d11d6cc44a178a`
