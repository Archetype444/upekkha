# Upekkhā 4.0 verification

`assembleDebug testDebugUnitTest lintDebug assembleRelease` completed successfully. Both debug and optimized release APKs were built with the original Upekkhā signing identity.

All 214 JUnit tests passed with no skips, including 64 nested production checks. Coverage includes exact imported exercise layouts, complete Classical and Diminished sessions, pulse conversion, immutable screen snapshots, actual UI taps from the first CP through recording completion, recovery and final CP, discard-on-quit, history deletion, saved-practice editing, touch duration controls, visible cue changes, backup migration and short interval scheduling. Home, Sit, Freestyle, Instructions and Guided recording screens were rendered natively and inspected at Kompakt width.

Lint completed with no errors. Remaining warnings are style/API recommendations and intentional compatibility choices, including fullscreen flags supported by Android 12, programmatic views, legacy resource lookup and synchronous persistence for running meditation state.

APK signature verification, archive integrity and alignment passed. Package `org.breatheless.app`, versionCode `5`, versionName `4.0`, min/target API `31`. Original launcher identity and signing certificate are retained for updates over v3/2.1. The original MP3 is packaged once, unchanged and uncompressed; all 26 original raw audio files remain unchanged after shrinking. The supplied instructions and Adam Shand MIT notice are bundled. Private signing material and internet permission are absent.

## Physical testing limits

No physical Kompakt or full Android emulator was available. Robolectric cannot establish actual device installation/update, speaker/headphone routing, vibration strength, step-sensor availability or overnight Doze/reminder reliability. Those are not claimed as tested. Channel/vibration tests, exact-alarm permission status and DuraSpeed guidance are available in Settings.

## Checksums

```text
2dada407f6baf2d345667008293103c5501a3cd22a17cdffb9f2f3bf16ae6a58  Upekkha-v4.0.apk
135b217d7f23bfbc5aa95a55f1602dfc8afd85b76ec6baea6e165e75431f4c98  Upekkha-v4.0-debug.apk
```

The detailed report is [release-4.0.json](release-4.0.json). Repeat APK checks with [verify-apk.py](verify-apk.py).
