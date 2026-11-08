# Adam Shand exercise port

Source: [adamshand/breathe-less](https://github.com/adamshand/breathe-less), inspected at commit `0612c637dc9a8cf4c685217ce4ec9f526f615b8e`. Copyright 2025 Adam Shand, MIT license. The license is included in assets, raw resources and the in-app Licenses section. The original ding/dong audio files are not bundled; Upekkhā uses its existing per-phase cue system.

The Kotlin programs preserve all active stages and their order and duration. The website's introductory and finished screens are represented by the app's selectable activity and completion screen. Manual measurement stages remain manual, with review/exclusion for holds. Pulse uses a 15-second count by default, supports tapping beats or adjusting the count afterward, and can be skipped. Maximum and extended pauses are stored separately from CP.

| Diminished Breathing stage | Timing |
| --- | --- |
| Settle | 3 min |
| Pulse | 15 sec, started by the user |
| Control Pause | Manual |
| Diminished breathing | 5 min |
| Extended pause | Manual |
| Diminished breathing | 5 min |
| Ground and orient | 3 min |
| Pulse | 15 sec, started by the user |
| Control Pause | Manual |

| Classical Buteyko stage | Timing |
| --- | --- |
| Pulse | 15 sec, started by the user |
| Control Pause | Manual |
| Very shallow breathing | 3 min |
| Recovery | 30 sec |
| Maximum pause, light effort | Manual |
| Shallow recovery (source Suppress) | 10 sec |
| Very shallow breathing | 3 min |
| Recovery | 30 sec |
| Maximum pause, medium effort | Manual |
| Shallow recovery | 20 sec |
| Very shallow breathing | 3 min |
| Recovery | 30 sec |
| Maximum pause, full effort | Manual |
| Shallow recovery | 30 sec |
| Very shallow breathing | 3 min |
| Recovery | 1 min |
| Control Pause | Manual |
| Pulse | 15 sec, started by the user |

The instructions supplied by the user are included in Settings with their links and original exercise descriptions. An attribution and short context note distinguish these source instructions from a medical assessment. In-session prompts use direct wording and encourage stopping on distress rather than forcing recovery. The app does not turn CP into a CO₂ estimate or health diagnosis.

Further safety context reviewed for v4:

- https://www.normalbreathing.com/breath-holds/ — the contraindications page explicitly linked by the user, including concerns about prolonged holds and cardiovascular responses.
- https://journals.physiology.org/doi/10.1152/advan.00030.2015 — primary physiology teaching paper, including fainting/dizziness considerations for extended breath holds.
- https://www.breathetrain.co.uk/assets/downloads/pdf_asthma_booklet.pdf — controlled holds ending with a relaxed next breath.

The CP software limit remains two minutes; other pauses have a ten-minute unattended guard. These limits exclude unattended measurements and are not training targets. Late callbacks clamp excluded measurements to the storage limit so an interruption cannot create an unreadable backup.
