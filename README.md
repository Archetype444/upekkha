# Upekkhā 3.0

An offline meditation and Buteyko practice app for Mudita Kompakt. Kotlin, Jetpack Compose and Mudita MMD; black text on a white background. Brightness follows Android system settings.

The meditation features incorporate [Nikka](https://github.com/wanderwildwood/nikka) at commit `557713c30021e936b92d9e9c59fe328cfbb1dd43`. See [UPSTREAM-NIKKA.md](UPSTREAM-NIKKA.md) and LICENSE. Upekkhā retains the existing application ID and signing identity for in-place updates from 2.1.

## Practice

- **Sit:** adjustable meditation duration, warm-up, interval and ending bowls, optional breathing warm-up, exact alarms and optional Do Not Disturb.
- **Breathe:** 4–7–8, box, coherent and custom breathing patterns; adjustable duration and screen, vibration or sound cues.
- **Buteyko:** Morning CP opens ready to measure. Mindfulness Buteyko follows CP → the original recording → five-minute recovery → CP. Freestyle remembers the exact phase durations, rounds and CP choices without named templates. Walking practice supports the optional step detector and manual counting.
- **Today:** habits, weekly targets, cues, rest days, calendar history and optional streaks.
- **Track:** all practices, 7/30/90-day or all-time CP charts, morning-only filtering, average/lowest/best/trend, pulse, symptom scores and walking totals.

Each Buteyko phase, CP stop and session finish has its own tone selection. Twenty tone choices include silence. Sound and vibration are independently enabled; six vibration patterns and adjustable strength are available. Hardware without amplitude control uses its supported fixed strength. Effects can use Alarm, Media, Notification, Ringtone or System audio; recordings and ambience always use Media. Test controls let you check the phone's channel volume before practising. The selected channel remains subject to the phone's volume, DND and audio routing.

Morning CP reminders have an adjustable time and reschedule after reboot, app update and time changes. Tapping a reminder opens CP ready; it never starts a breath hold. Exact reminders require Android's alarm permission.

The app stores data locally and has no internet permission. A combined JSON backup contains both meditation and Buteyko records, habits and settings. It also accepts legacy BreatheLess/Upekkhā 1–2.1 backups and Nikka backups. Old comments and custom protocols remain stored and backed up; diary, learn and named-template screens have been removed.

During Buteyko practice there are no manual pause, dim, settings or pulse controls. Timed blocks run in a foreground service, using monotonic deadlines and a partial wake lock. CP phases wait for user input; interrupted or unattended measurements are excluded. Playback interruptions offer an explicit resume action. The recording moves to recovery on actual media completion rather than a guessed duration. Saved measurements are checkpointed and recovered after process interruption without resuming a breath hold.

## Build and verify

Requires JDK 17, Android SDK platform 36 / Build Tools 36.0.0 and Gradle 8.14.3 (wrapper supplied). Android 12/API 31 is the minimum and target, matching the Kompakt.

Restore the **original private signing backup** into `signing/breatheless-update.p12` and `signing/password.txt`. The alias is `breatheless-update`. These files are excluded from Git. Packaging fails if the original signing material is missing; never generate a replacement key for an update.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease
```

The debug and optimized release builds use the same original certificate. Expected SHA-256 certificate fingerprint:

```text
a5edb5474ed2435c35c086b1920a0acfdfeac91ad00c7fb6659c9017a44e2268
```

Package `org.breatheless.app`, versionName `3.0`, versionCode `4`. Both APK variants are installable. Release downloads are attached to the GitHub release; the optimized release is the normal installation choice.

`app/src/test` contains the upstream Nikka regression suite and Upekkhā timer, cue, backup, migration, reminder and compact Android 12 UI checks. See the release verification report for actual results and physical-device limitations.

## Method and media

[docs/METHOD-FOUNDATION.md](docs/METHOD-FOUNDATION.md) records the method sources and practical design decisions. The app treats CP as a personal tracking measurement rather than a diagnostic score. It does not advise medication changes, maximum breath holds, mouth taping, gas inhalation or deliberate hypoxia.

The user-supplied recording is retained unchanged from 2.1. It is packaged once as an uncompressed APK asset and played from a file descriptor. Its separate media rights are unchanged. Wind/fireplace ambience comes from the FOSS Ambientsounds project; credits are in `app/src/main/assets/CREDITS.txt`. Third-party dependency licenses are available inside the app.

Copyright (C) 2026 wander wildwood, Nikka contributors, and Upekkhā contributors. Code is licensed under GNU GPL version 3; see LICENSE. Original notices are retained. Audio and fonts retain their individual licenses.
