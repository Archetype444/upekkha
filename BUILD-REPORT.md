# Upekkhā 2.1

Built and verified on 21 October 2026.

## Changes

The launcher name, practice heading, About panel, notifications, media metadata, diary images, share subjects, backup filenames and diary export filenames now use Upekkhā (ASCII Upekkha in filenames). The icon and all other v2 functionality are retained. Original publication attributions and the historical backup format identifier are preserved.

## Installation

Install `Upekkha-v2.1-debug.apk` over v2 without uninstalling. The application ID remains `org.breatheless.app`; the version code increases from 2 to 3 and version name is 2.1. The signing key is unchanged. Android update prerequisites are verified; an actual in-place installation was not exercised here. The prior v1 key limitation remains documented in README.md.

## Verification

- Kotlin compilation, resource packaging and DEX generation succeeded.
- APK v2 and v3 signatures verified; ZIP alignment and archive integrity passed.
- The packaged launcher label is Upekkhā, versionName 2.1, versionCode 3.
- All 67 production-core checks passed, including v1 backup compatibility.
- The supplied recording and packaged resources match v2 byte for byte. The MP3 occurs once, uncompressed.
- APK size: 27,273,531 bytes.
- APK SHA-256: `c0ab7f69f06cb6c54827d42d3e7e533748b7ac29ffe1841d42fba3eda6eb7625`
- Certificate SHA-256: `a5edb5474ed2435c35c086b1920a0acfdfeac91ad00c7fb6659c9017a44e2268`

No Android phone or usable emulator was available. Installation/update, UI rendering, hardware sound/vibration, screen-off operation, sensors, audio routing, file picker and sharing were not exercised on a device.

## Future builds

The project archive includes the original v2-series signing key and password file under `signing/`; keep that archive private. Preserve this exact key and application ID in future releases, use a versionCode greater than 3, and keep the display name Upekkhā. Read AGENTS.md. The separate signing backup preserves the same identity.
