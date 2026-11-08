# Upekkhā 4 maintenance

Preserve applicationId org.breatheless.app and signing certificate SHA-256 a5edb5474ed2435c35c086b1920a0acfdfeac91ad00c7fb6659c9017a44e2268. Key: signing/breatheless-update.p12, alias breatheless-update, password file signing/password.txt. Never print passwords or key bytes. Never generate replacement update keys. Version 4.0 uses versionCode 5. Keys/passwords/private backups must never enter Git or release assets.

The app incorporates Nikka (GPL-3.0-only); preserve license/attributions. Target Mudita Kompakt, black on white, MMD controls, no brightness override. No Diary/Learn. Preserve existing tracking data and backup readers. Morning CP has no settling timer. The audio sequence is CP, original recording, 300-second recovery, CP. Unnamed freestyle remembers its settings. Pulse only before/after practice. No manual pause, dim or settings buttons during a practice. Keep phase sound/haptic customization, walking, symptom tracking and unified data.

Push a development checkpoint before and after every significant working stage, not only at completion. Never rely on scratch storage for the only copy of unfinished source. Keep secrets in their separate private backup. Before ending or handing off work, push the latest source checkpoint and record the commit and checks.

Build/test with Gradle; verify final package/version/certificate and the single unchanged MP3. Report physical-device testing limitations.

V4 supersedes the older UI requirements: remove Today/habit screens; fullscreen status bar; touch list/stepper controls for numbers; Guided Buteyko plus Adam Shand’s MIT exercise layouts; ordered/repeatable freestyle and saved/editable templates. Incomplete Buteyko sessions are discarded, including old interrupted checkpoints. CP and maximum/extended pause records remain separate. UI must observe immutable session snapshots; never capture a mutable Engine inside Compose lazy content. No before-practising popup. Instructions remain accessible in Settings.
