# Upekkhā 3.0 verification

The final Gradle invocation `assembleDebug testDebugUnitTest lintDebug assembleRelease` completed successfully. Both APKs were signed using the original Upekkhā 2.x identity. All 171 JUnit tests passed, including 64 nested production checks, Android 12 foreground service behaviour, legacy/combined backup migration and compact-screen Compose UI tests. Native rendering of the home screen was visually inspected. Lint completed with no errors; remaining warnings concern code style, deprecated API alternatives, programmatic views, intentional synchronous persistence and conservative dependency versions.

Package: `org.breatheless.app`. Version name: `3.0`. Version code: `4`. Minimum/target Android API: `31`. The existing launcher component is retained as an activity alias.

Signature verification, ZIP integrity and alignment passed for both APKs. The original recording is present once, stored uncompressed, and its SHA-256 is unchanged. All 26 original raw cue/ambience/breathing audio files are present byte for byte, including after resource shrinking. The app has no internet permission. No private signing material is packaged.

Install over Upekkhā 2.1 without uninstalling to retain local data. New installations can import the older JSON backups.

## Limitations

No physical Kompakt or full Android emulator was available. Robolectric checks do not establish actual device installation/update behaviour, speaker/headphone routing, vibration strength, step-sensor support, interruption handling with real audio hardware, or overnight Doze/reminder reliability. These remain physical-device checks. Channel/vibration previews and reminder permission status are available in Settings.

## APK checksums

```text
1a0a9c195bda1d21e82d7bf53ce19521571ac14114e2fa41ec11c1a348a8f637  Upekkha-v3.0.apk
2c98903ad5d77c611b01a4f4dde57e0c5e54b21f1e144a58cf7cf311618cb012  Upekkha-v3.0-debug.apk
```

See [release-3.0.json](release-3.0.json) for the full machine-readable report and [verify-apk.py](verify-apk.py) to repeat APK checks.
