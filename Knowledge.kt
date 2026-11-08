package org.breatheless.app

object Knowledge{
    data class Article(val category:String,val title:String,val summary:String,val body:String,val sources:List<Pair<String,String>> = emptyList())
    private val manual="Buteyko · 1988 practitioner manual" to "https://buteyko.ru/izdan/Buteyko_63_Instruction.pdf"
    private val patent="Buteyko · original patent disclosure" to "https://buteyko.ru/eng/document/patent1.shtml"
    private val interview="Buteyko · 1982 interview, translated reprint" to "https://www.consciousbreathing.com/blogs/co2-academy/konstantin-buteyko-interview-1982"
    private val first="M. and V. Buteyko · First-Hand Method (2005)" to "https://d3ciwvs59ifrt8.cloudfront.net/014cf302-bcd5-408b-812d-a1d019de896f/50b68d7e-934a-4e2f-889b-396539f9fede.pdf"
    private val bowler="Bowler et al. · asthma trial (1998)" to "https://www.mja.com.au/journal/1998/169/11/buteyko-breathing-techniques-asthma-blinded-randomised-controlled-trial"
    private val courtney="Courtney and Cohen · CP and CO₂ (2008)" to "https://journals.sagepub.com/doi/abs/10.1089/acm.2007.7204"
    private val vagedes="Vagedes et al. · asthma trial (2024)" to "https://link.springer.com/article/10.1186/s40001-023-01634-1"
    private val bohr="Bohr, Hasselbalch and Krogh · 1904 translation" to "https://www1.udel.edu/chem/white/C342/Bohr%281904%29.html"
    private val no="Alving et al. · nasal nitric oxide (1993)" to "https://pubmed.ncbi.nlm.nih.gov/7507065/"
    val articles=listOf(
        Article("Practice","Reduced breathing","Relaxation, gentle air hunger and ordinary recovery.","""Sit comfortably with support if needed. Allow the shoulders, jaw and abdomen to relax. If nasal breathing is comfortable, let it become quieter and smaller while keeping the effort gentle. Exhalation should remain passive.

The supplied Hyde article describes reducing tidal volume while remaining relaxed. The app uses a phase clock rather than an inhale/exhale cadence. The circle shows elapsed phase time; it does not instruct a respiratory rhythm.

If air hunger becomes strong, distressing or difficult to manage, ease off. Return to ordinary breathing and stop if symptoms develop. Completing every minute is optional. Protocols can be shortened or adapted with a practitioner who understands your health.

The presets are application templates. They are not presented as authenticated prescriptions from Dr Buteyko.""",listOf(manual,first)),
        Article("Measurement","Control Pause","How to take and review a comfortable CP.","""Rest quietly first and use a consistent posture and rest period for comparisons. After an ordinary, passive exhalation, gently pinch the nose if comfortable and start the CP timer. Do not prepare with a deep breath or force extra air out.

Stop at the first clear urge to breathe or the first involuntary movement in the diaphragm or throat, whichever occurs first. Release the nose and resume an easy, ordinary breath. If recovery requires a deeper or gasping breath, exclude the measurement.

The First-Hand account emphasizes an involuntary muscle movement; the supplied article also describes the first urge, swallowing impulse or diaphragm movement. Upekkhā uses the earlier signal. A CP should not be prolonged to obtain a score.

Running CP time is hidden by default. Excluded results remain in the diary but do not contribute to trends. A two-minute software stop protects against an unattended timer; it is never a target. Breathe whenever needed.""",listOf(first,vagedes)),
        Article("Measurement","Reading CP trends","Use comparable conditions and retain the context.","""CP varies with technique, attention, recent activity, meals, stress, sleep and illness. A pattern of comparable measurements is more useful than a single result. Record anything that may have affected the measurement.

Each chart point is the mean of valid CP measurements on that calendar day. Average, lowest and best summarize the individual valid values in the chosen period. The trend compares the earlier and later halves of recorded days. This is a descriptive calculation, not a clinical prediction.

Courtney and Cohen studied 83 adults and found that breath-holding time did not predict resting end-tidal CO₂ as Buteyko proposed. The app therefore does not convert CP into a CO₂ estimate or health grade. The supplied article's CP health bands are not used to diagnose illness or guide medication.""",listOf(courtney,patent)),
        Article("Audio","Mindfulness Buteyko","Recording by Marilleke Vrancken, supplied with this version.","""The recording lasts 20 minutes 27 seconds and plays directly from the installed application. It is stored once, without converting it or creating a second copy on the phone.

The default sequence is: five minutes of settling rest; a CP measurement; the recording; five minutes of recovery; a further five-minute wait; then a second CP measurement. The second CP begins only when you choose to start it. The recovery and waiting intervals can be edited in a copy of the protocol.

The recording phase ends when playback actually finishes. Pausing, seeking, or an audio interruption does not let the timer move on in the background. The application pauses when audio focus is lost and waits for you to resume.

The recording and ambience use media volume; phase cues use alarm volume. Ambience is muted during the recording. Start with a comfortable listening volume and use the on-screen controls or a supported headset."""),
        Article("Controls","Phase cues and screen-off use","Configure recognisable sounds before relying on them.","""Settings offers separate tones for rest, reduced breathing, recovery, CP readiness, recording playback, stopping CP, and session completion. Preview the sounds individually and learn the pattern used by your chosen protocol. Sound and vibration can be enabled independently.

There are twenty tone choices, including silence, and six vibration patterns. Intensity works when the phone reports amplitude-control support. Other devices use the selected pattern at the system's supported strength. Use the test button to check the physical result.

A headset play/pause button can start or stop CP, keep a reviewed result, or pause and resume a timed phase. A headset Next command excludes a reviewed result. Support varies by headset and Android version. Notification controls provide the same main action and an exclude action for reviewed CP.

Optional volume-key controls work while the timer screen is open: Volume up performs the main action; Volume down stops a running CP, excludes a reviewed result, or pauses and resumes. Leaving the screen during an on-screen CP interrupts and excludes that measurement. If using headset CP with the display off, stay attentive and stop at the first urge.

Do Not Disturb, alarm volume, headphones, output routing, battery restrictions and force-stop can suppress signals. Check the selected cues on the phone. Always breathe when needed, regardless of the signal."""),
        Article("Routine","Morning measurements","Choose one setup and use it consistently.","""The supplied article recommends morning CP soon after waking in the waking position. Historical practitioner accounts also describe seated measurements after a longer settling rest. These are different setups; record which one you use.

Morning CP in this app provides five minutes of rest, followed by one measurement. Rested baseline provides ten minutes. If your routine uses a different rest or an immediate waking measurement, edit the protocol and use Morning as the diary context.

The Morning filter shows entries labelled Morning. Add posture, time since waking, sleep and recent activity to notes when useful.

The supplied article suggests five minutes before reassessing CP after seated practice, and ten to fifteen minutes after walking. These are practical suggestions rather than guaranteed physiological cutoffs. Wait until breathing is comfortable before another measurement.""",listOf(first)),
        Article("History","Buteyko's original method","Historical material reviewed for the application.","""Konstantin Buteyko called his approach voluntary elimination of deep breathing (ВЛГД). His 1988 practitioner manual describes reducing breathing depth through relaxation of the breathing muscles while maintaining a sensation of air shortage. It distinguishes CP from the effortful and maximum parts of a prolonged hold and warns against a deep recovery breath.

The manual includes pulse and a diary, and describes standard conditions after a ten-minute rest. It was written for practitioners trained with the author. The application does not implement its provocation tests or medication-change instructions.

The original patent disclosure concerns reduced ventilation with measured exhaled carbon dioxide. A phone stopwatch does not perform that measurement. The 1982 interview records Buteyko's account of his theory and observations. These historical sources document the method's intentions; they do not independently validate broad disease-treatment claims.""",listOf(manual,patent,interview)),
        Article("Physiology","CO₂ and the Bohr effect","The role of carbon dioxide in oxygen binding.","""Carbon dioxide is produced during metabolism and participates in acid–base regulation. Bohr, Hasselbalch and Krogh showed that carbon dioxide changes blood's oxygen-binding behaviour, particularly at lower oxygen pressures. This helps explain oxygen unloading in tissues.

Oxygen delivery also depends on ventilation, circulation, haemoglobin and tissue demand. The Bohr effect does not establish that deliberately raising CO₂ is always beneficial, or that breathing less treats every cause of poor oxygen delivery.

A pulse oximeter estimates arterial oxygen saturation. It does not directly measure tissue oxygen delivery or carbon dioxide. CP measures a duration. Clinical questions about blood gases or oxygenation need appropriate clinical assessment.

Do not increase room CO₂, rebreathe into a bag or use inhaled gas mixtures on the basis of this app.""",listOf(bohr,courtney)),
        Article("Physiology","Overbreathing and the nose","Ventilation depends on both volume and frequency.","""Overventilation means ventilation exceeds what is needed for carbon dioxide production. Large breaths at a slower frequency can produce excessive ventilation as well as fast breathing. A timer cannot determine whether this is happening in an individual.

Breathing varies with age, activity, sleep, illness and emotion. The app does not prescribe a single respiratory rate or use a breathing-frequency threshold to diagnose a problem. During rest, allow comfortable, unforced breathing.

The nose warms, humidifies and filters air. Alving and colleagues found that much of the nitric oxide in healthy participants' exhaled air originated in the nasal airways. That physiological finding does not establish that a particular CP target treats inflammation.

Use nasal breathing when comfortable. Do not force a blocked nose or use mouth taping as an app exercise. Persistent obstruction, recurrent breathlessness, snoring or suspected sleep apnoea deserves assessment.""",listOf(no,interview,vagedes)),
        Article("Evidence","Clinical studies","Results and limits relevant to a practice companion.","""Bowler and colleagues' 1998 randomized trial enrolled 39 people with asthma. The Buteyko group reduced reliever use and minute ventilation over three months. Measured airway calibre and end-tidal CO₂ did not improve. This does not demonstrate a universal CO₂-restoration mechanism.

Vagedes and colleagues' 2024 trial compared 30 adults receiving Buteyko training with 30 receiving usual therapy. CP and symptom questionnaires improved in the training group, alongside reduced recorded medication use. FEV₁ and exhaled nitric oxide did not significantly change. Participants had training and usual clinical care; the trial did not test this app.

These findings support investigation of breathing retraining as an adjunct. They do not establish that all claims in the supplied article are correct, or that users should change medication themselves. Track symptoms, function and clinician-recommended measures alongside CP.""",listOf(bowler,vagedes,courtney)),
        Article("Diary","Pulse, symptoms and exports","Keep enough detail to discuss a session later.","""Record the time, posture, recent meals or activity, sleep, symptoms and recovery quality when useful. The diary retains excluded CP measurements and all recorded pulse and walking values.

Pulse is entered manually or counted at the wrist for thirty seconds. The beat counter doubles the count to estimate bpm; tapping and timing affect the result. This version does not use the camera or a medical sensor for pulse measurement.

The symptom score is a personal 0–10 rating, where higher means worse. Keep its meaning consistent. It is not a validated clinical questionnaire.

CSV exports include values, validity, timestamps, walking rounds and notes. Image exports paginate the diary. Choose an email application and recipient in Android's share sheet; sending remains your action. The JSON backup includes entries, protocols and preferences. Restore imports missing IDs while keeping existing records with matching IDs. Both version 1 and version 2 backups use the same schema.

Backups and exports are unencrypted files. Store them somewhere appropriate for personal health information."""),
        Article("Safety","Safety information","Symptoms and ordinary breathing take priority.","""Stop a hold or reduced breathing for dizziness, faintness, chest pain, worsening wheeze, marked distress or unusual breathlessness. Return to comfortable ordinary breathing. Seek urgent medical help for severe symptoms, collapse or breathing difficulty that does not settle with the prescribed action plan. Do not interpret worsening illness as a necessary healing reaction.

Do not hold your breath in water, while driving or cycling, near traffic, on stairs or at heights. Choose a supported seated position for seated practice and safe level ground for walking.

Seek individual clinical advice before reduced breathing or hold exercises if pregnant, under eighteen, affected by fainting or seizures, or living with significant heart, lung or another medical condition. Avoid experimenting during acute illness or an asthma attack. This application is an adult practice companion.

The safety boundaries here are conservative product choices, rather than a universally validated contraindication list. A CP value cannot establish whether practice is suitable. Keep prescribed treatment, clinician-directed monitoring and an asthma action plan. Medication changes belong with the prescriber.

The app does not prescribe maximum holds, strong air hunger, provocation tests, mouth taping, rebreathing, gas mixtures or medication withdrawal. Every CP can be skipped. Software stops are guardrails, not durations to aim for.""",listOf(manual,vagedes)),
        Article("Sources","Sources and credits","Original materials, supplied recording and open-source ambience.","""Foundation reviewed for version 1: Buteyko's patent disclosure (priority 1962, registered 1983), the 1988 Russian practitioner-manual scan and its modern text transcription, and a translated reprint of the 1982 interview published in the 1990 collection. Modern editorial insertions were distinguished from original material. A separate 1969 lecture PDF could not be retrieved and is not claimed as reviewed.

The request supplied Hyde's “Buteyko Method 101”, BreatheLess, 4 November 2024. Its discussion of volume, CP, context, diary use and walking informed the application. Strong-air-hunger escalation, CP health grades and gas-administration suggestions were not adopted.

The Mindfulness Buteyko MP3 is by Marilleke Vrancken and was supplied by the user for version 2. Its original file is bundled unchanged. It is distinct from the open-source ambience collection.

Wind and Fireplace are from Muges/AmbientSounds. Wind is by felix.blume, Freesound 139337, CC0; Fireplace.wav is by inchadney, Freesound 132534, CC0. The collection's Ogg loop edits are bundled without further edits. Phase tones are original synthesized WAVs. Kotlin's runtime license and notices are in the application assets.

The application makes no network connections. Source links open in your selected browser.""",listOf(manual,patent,interview,first,bowler,courtney,vagedes,bohr,no,"AmbientSounds" to "https://github.com/Muges/ambientsounds","Wind credit" to "https://freesound.org/people/felix.blume/sounds/139337/","Fireplace credit" to "https://freesound.org/people/inchadney/sounds/132534/"))
    )
}
