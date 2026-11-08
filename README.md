# Upekkhā 4.0

Offline meditation, Buteyko practice and CP tracking for Mudita Kompakt. Kotlin, Jetpack Compose and Mudita MMD. Black text on white, with the status bar hidden throughout the app. Brightness follows Android system settings.

Install the release APK over v3 or v2.1 without uninstalling. The application ID remains `org.breatheless.app`, the signing certificate is unchanged, versionName is `4.0`, and versionCode is `5`.

## Practice

The four tabs are **Sit**, **Breathe**, **Buteyko** and **Track**. Today and habit controls have been removed.

- **Sit:** adjustable length, settling or breathing warm-up, interval and ending bowls. Interval choices include every 15 seconds, 30 seconds, 1 minute and 2 minutes, as well as the longer intervals. Short intervals use alarm-clock scheduling when exact-alarm permission is available, avoiding the quota on ordinary idle alarms.
- **Breathe:** 4–7–8, coherent, box and custom patterns. Numeric settings use touch controls.
- **Buteyko:** Morning CP, Guided Buteyko, Diminished Breathing, Classical Buteyko, Freestyle and saved practices.
- **Track:** combined history, CP trends over 7/30/90 days or all time, morning-only filtering, average/lowest/best/trend, pulse, symptom scores and walking data. Sessions can be deleted from history.

**Morning CP** opens ready to measure. **Guided Buteyko** follows CP → original recording → five-minute recovery → CP. The recording moves to recovery on actual playback completion. The session screen observes immutable service snapshots, so touch and volume-button controls update its phase and buttons immediately.

**Diminished Breathing** and **Classical Buteyko** port the active stage order and durations from [Adam Shand’s breathe-less](https://github.com/adamshand/breathe-less), commit `0612c637dc9a8cf4c685217ce4ec9f526f615b8e`, under MIT. They use Upekkhā’s existing sounds. See [docs/ADAM-PROTOCOLS.md](docs/ADAM-PROTOCOLS.md) for exact layouts and attribution. Maximum pauses, their effort variants and the extended pause are recorded separately from CP and do not enter the CP chart. Pulse stages offer 15-second counting; longer counts and direct pulse adjustment are also available.

**Freestyle** is an ordered list of steps. Add a step, select its duration, move it up/down or remove it. Rounds repeat the displayed sequence. Changes are remembered without naming the practice. Existing v3 settings migrate to the same sequence. Save a setup as a named practice for its own home-page button; Edit opens its steps, rename and delete controls. Name entry is the only place that requires a text keyboard. Number settings use lists or steppers. Rest/recovery choices are 15/30/45 seconds and 1/2/5/10 minutes; breathing blocks have choices from 1 minute to 1 hour. Saved or migrated durations remain readable in minutes/seconds.

There is no before-practising popup. The supplied exercise instructions and links remain available in Settings. User-ended pauses have no prescribed hold target. Software limits exclude unattended measurements and are not practice goals.

Completed Buteyko sessions are saved once. **Quit session discards the entire incomplete session**, including any CP or pulse values already taken. Interrupted-session checkpoints from older versions are discarded rather than imported as new partial records. Previously saved historical data is retained and can be deleted in Track. Walking remains self-paced: Finish and save completes it deliberately; Quit or Back discards it without an automatic save.

## Sounds, reminders and data

Twenty tone choices include silence. Each block type, measurement stop and completion has an independent tone choice. Sound and vibration are independently enabled, with six vibration patterns and adjustable strength where hardware supports it. Effects may use Alarm, Media, Notification, Ringtone or System audio. Recordings and ambience use Media. Test controls are available in Settings; output remains subject to system volume, DND and hardware routing.

Morning CP reminders have an adjustable time and reschedule after reboot, app update and time changes. Tapping a reminder opens CP ready and never starts a hold. Exact reminders require Android’s alarm permission. DuraSpeed guidance remains available on supported phones.

The app has no internet permission. A JSON backup contains practice records, other pauses, saved templates, freestyle order/rounds and settings. Combined backup schema 4 accepts schema 3 files, legacy BreatheLess/Upekkhā 1–2.1 files and Nikka backups. Old comments and habit data remain preserved for backup compatibility, without habit screens. Storage writes are atomic; no private signing files are packaged or committed.

## Build and verification

Requires JDK 17, Android SDK platform 36 / Build Tools 36.0.0, and Gradle 8.14.3 (wrapper supplied). Minimum/target Android API is 31, matching the Kompakt.

Restore the original private signing backup to `signing/breatheless-update.p12` and `signing/password.txt`, alias `breatheless-update`. Packaging refuses to use a replacement signing identity. Never publish those files.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease
```

Both debug and optimized release APKs use the original certificate:

```text
a5edb5474ed2435c35c086b1920a0acfdfeac91ad00c7fb6659c9017a44e2268
```

The release includes APKs, checksums and a verification report. Tests cover the exact exercise layouts, real UI taps through Guided Buteyko and media completion, full exercise execution, incomplete-session discard, deletion, template editing, backups, compact Android 12 layouts and touch controls. Actual results and physical-device limitations are recorded in [verification/RELEASE-v4.0.md](verification/RELEASE-v4.0.md).

Repeat APK checks with:

```sh
python3 verification/verify-apk.py Upekkha-v4.0.apk --sdk "$ANDROID_HOME"
```

## Credits

The meditation foundation incorporates [Nikka](https://github.com/wanderwildwood/nikka), copyright 2026 wander wildwood, under GPL-3.0-only. See LICENSE and [UPSTREAM-NIKKA.md](UPSTREAM-NIKKA.md). Adam Shand’s MIT notice, MMD/AndroidX Apache notices and Lato’s OFL notice are retained. The original user-supplied recording has separate media rights and is packaged once, unchanged and uncompressed. Wind/fireplace ambience comes from the FOSS Ambientsounds collection under CC0. In-app Licenses and `app/src/main/assets/CREDITS.txt` provide details.

The original Buteyko research foundation is documented in [docs/METHOD-FOUNDATION.md](docs/METHOD-FOUNDATION.md), with the v4 exercise port described separately. The app treats CP as personal tracking, not a diagnosis or a direct oxygen/CO₂ measurement. It does not advise medication changes.
