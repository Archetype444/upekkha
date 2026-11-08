# Buteyko foundation and v3 behaviour

The project began with the user's supplied full article **Hyde, BreatheLess, “Buteyko Method 101,” 4 November 2024**. Its practical distinctions informed the app: breath reduction through relaxation, a CP after an ordinary passive exhalation, stopping at the first clear breathing impulse, and comparing measurements under similar conditions. The article is commentary, rather than an original clinical source. It is not copied into the app.

Original-method sources consulted:

1. **Konstantin P. Buteyko, interview taken in 1982**, published in *The Buteyko Method: An Experience of Use in Medicine* (Moscow, Patriot, 1990), as hosted by the Buteyko family centre: https://buteyko.ru/eng/interw.shtml . The interview's practical account emphasizes reducing depth by relaxing the respiratory muscles; it also contains historical efficacy claims that the app does not turn into medical promises.
2. **Vladimir K. Buteyko and Marina M. Buteyko (2005), *The Buteyko Theory About a Key Role of Breathing for Human Health***, bilingual centre publication: https://buteyko.ru/izdan/Buteyko%20theory%20rus-eng.pdf . The English section begins on printed page 52, with CP discussion on pages 66–68 and application on page 83. This is a first-party account of the school’s theory, not independent validation of its broad disease claims.
3. **K. P. Buteyko, 1969 Moscow State University lecture**, English translation and editorial context by Artour Rakhimov: https://www.normalbreathing.com/book-buteyko-lecture/ . The publication distinguishes the original lecture from later translator notes. The linked full PDF mirror was unavailable during the v3 rebuild; the accessible publication description was reviewed, and the first two primary sources are the principal foundation.

These sources led to concrete implementation choices. Reduced breathing is a timed period of gentle, self-regulated nasal breathing rather than an imposed inhalation/exhalation rhythm. CP is a user-ended stopwatch, never a prescribed hold target. The next breath should be natural; a strained or interrupted measurement can be excluded. A two-minute unattended timer limit is a software stop and excludes that measurement, not a practice goal. CP trends use only included values and can isolate morning readings.

The article’s suggested disease thresholds, universal disease explanations and medication statements are not used as app diagnoses or instructions. CP duration is not converted to blood CO₂, oxygen saturation or a health grade. Breath reduction remains distinct from the separate Nikka-style paced-breathing feature. The user specifically requested CP → recording → five-minute recovery → CP for the audio activity in v3; actual media completion determines the start of recovery.

Safety guidance stays available in Settings and before the first Buteyko practice. It directs the user to stop on distress, avoid water/driving/fall hazards, obtain clinical guidance where relevant, and continue prescribed care. The app does not recommend maximal holds, deliberate hypoxia, mouth taping or CO₂ administration.

## Device foundation

Mudita's official specification lists a 4.3-inch 800×480 E Ink display. Mudita's own forum response confirms AOSP 12. The UI therefore uses white backgrounds, black text, MMD controls, steady cursors, discrete page navigation and one-second timer display updates. It never sets app/window brightness.

- https://support.mudita.com/en/support/solutions/articles/77000577372-kompakt-technical-specifications
- https://forum.mudita.com/t/questions-about-mudita-kompakt/14668/2

The upstream meditation source was inspected at https://github.com/wanderwildwood/nikka and incorporated with its GPL license and attributions.
