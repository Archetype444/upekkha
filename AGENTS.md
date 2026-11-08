# Upekkhā maintenance

Keep applicationId `org.breatheless.app`. All versions after v2 must use the exact signing key in `signing/breatheless-update.p12`, alias `breatheless-update`. The password is in `signing/password.txt`. Never print the password or key bytes. Check the final APK certificate against `signing/identity.json`.

The v1 key was lost during workspace maintenance and was absent from the saved v1 project. It cannot be reconstructed from a signed APK or its certificate. Do not claim v1 update compatibility with the v2 key. If the original key is supplied later, resolve which signing series the user has installed before changing identity.

For each new published version, strictly increase the Android versionCode and update versionName. Keep the signing identity and source backed up with the deliverables, including a private signing backup. If files have been pruned, recover the exact project from an approved source or ask the user to attach it. Never generate a replacement key automatically.

Keep the interface dark with a pure black page background, muted text, direct English, and no emojis or marketing taglines. Do not animate a prescribed breathing rhythm. CP remains user-controlled and excluded if interrupted or overextended. Preserve the schema-1 backup reader.

Store the supplied MP3 exactly once as an uncompressed APK asset and play it without extracting a second on-device copy. Wait for real media completion before moving to post-recording phases. Retain independent per-phase and end-of-session sounds, global vibration pattern/intensity, and the physical test controls.

Build with `build-apk.py`. Run the production-core checks, signature verification, alignment and archive checks. Report device testing limitations honestly.

The user-visible brand is Upekkhā (ā is U+0101). Keep ASCII Upekkha filenames for exports, the existing applicationId, and the historical BreatheLess schema-1 backup identifier. Keep original source/publication attributions unchanged. Current release: versionName 2.1, versionCode 3.

## Repository publication

Keep all private signing files and backups out of Git and GitHub release assets. Only the public identity metadata and certificate belong in `signing/` in Git. Restore the existing private key and password locally from the separate backup before building; never generate a replacement. APK files belong in GitHub Releases rather than Git history.
