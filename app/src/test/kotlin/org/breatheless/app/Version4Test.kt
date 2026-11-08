package org.breatheless.app

import org.json.*
import org.junit.Assert.*
import org.junit.Test

class Version4Test {
    @Test fun diminishedSequenceMatchesSourceTimingAndOrder() {
        assertEquals(listOf(Phase(Kind.SETTLE,180),Phase(Kind.PULSE,15),Phase(Kind.CP),Phase(Kind.DIMINISHED,300),Phase(Kind.EXTENDED),Phase(Kind.DIMINISHED,300),Phase(Kind.GROUND,180),Phase(Kind.PULSE,15),Phase(Kind.CP)),Presets.diminished.phases)
        assertEquals(990,Presets.diminished.durationSeconds)
    }
    @Test fun classicalSequenceMatchesSourceTimingAndOrder() {
        assertEquals(listOf(Kind.PULSE,Kind.CP,Kind.VERY_SHALLOW,Kind.RECOVER,Kind.MP_LIGHT,Kind.SUPPRESS,Kind.VERY_SHALLOW,Kind.RECOVER,Kind.MP_MEDIUM,Kind.SUPPRESS,Kind.VERY_SHALLOW,Kind.RECOVER,Kind.MP_FULL,Kind.SUPPRESS,Kind.VERY_SHALLOW,Kind.RECOVER,Kind.CP,Kind.PULSE),Presets.classical.phases.map{it.kind})
        assertEquals(listOf(10,20,30),Presets.classical.phases.filter{it.kind==Kind.SUPPRESS}.map{it.seconds})
        assertEquals(listOf(30,30,30,60),Presets.classical.phases.filter{it.kind==Kind.RECOVER}.map{it.seconds})
        assertEquals(960,Presets.classical.durationSeconds)
    }
    @Test fun guidedSequenceKeepsOriginalAudioAndSingleRecovery() {assertEquals("Guided Buteyko",Presets.listening.name);assertEquals(listOf(Kind.CP,Kind.LISTEN,Kind.RECOVER,Kind.CP),Presets.listening.phases.map{it.kind})}
    @Test fun allProgramsValidateAndHaveNonemptyGuidance() {Presets.all.forEach{Codec.readProtocol(Codec.protocol(it));it.phases.forEach{phase->assertTrue(phase.guidance.isNotBlank())}}}
    @Test fun restOptionsAreTheRequestedDurations() {assertEquals(listOf(15,30,45,60,120,300,600),Durations.rest)}
    @Test fun breathingOptionsReachOneHour() {assertEquals(listOf(60,120,300,600,900,1200,1500,1800,2100,2400,2700,3000,3300,3600),Durations.breathing)}
    @Test fun labelsUseMinutesAndHours() {assertEquals("15 sec",Durations.label(15));assertEquals("2 min",Durations.label(120));assertEquals("1 hour",Durations.label(3600));assertEquals("2 min 3 sec",Durations.label(123))}
    @Test fun shortSittingIntervalsExist() {assertTrue(Durations.interval.containsAll(listOf(15,30,60,120)))}
    @Test fun freestyleRoundOrderSurvivesBackup() {val p=Protocol("custom","A practice","",listOf(Phase(Kind.RECOVER,15),Phase(Kind.MP_MEDIUM),Phase(Kind.CP)),true,3);val restored=Codec.readProtocol(Codec.protocol(p));assertEquals(p,restored);assertEquals(p.phases+p.phases+p.phases,restored.expanded().phases)}
    @Test fun engineActuallyRepeatsOrderedSequence() {val e=Engine(Protocol("repeat","Repeat","",listOf(Phase(Kind.REST,15),Phase(Kind.CP)),true,2),0);assertEquals(4,e.protocol.phases.size);e.tick(15000);e.startCp(16000);e.stopCp(18000);e.advance(19000);assertEquals(2,e.index);assertEquals(State.TIMED,e.state);assertEquals(15000,e.remaining(19000).toInt())}
    @Test fun snapshotKeepsPreviousValuesWhenEngineAdvances() {val e=Engine(Presets.listening,0);val ready=e.snapshot(0);e.startCp(1000);val running=e.snapshot(2000);e.stopCp(3000);e.advance(4000);val audio=e.snapshot(4000);assertEquals(State.CP_READY,ready.state);assertEquals(State.CP_RUNNING,running.state);assertEquals(1.0,running.pauseSeconds,0.0);assertEquals(State.LISTENING,audio.state);assertEquals(1,audio.index);assertNotEquals(ready,audio)}
    @Test fun maximumPauseHasIndependentStates() {val e=Engine(Protocol("hold","Hold","",listOf(Phase(Kind.MP_LIGHT))),0);assertEquals(State.HOLD_READY,e.state);e.startCp(1000);assertEquals(State.HOLD_RUNNING,e.state);e.stopCp(22000);assertEquals(State.HOLD_REVIEW,e.state);assertEquals(21.0,e.pendingCp,0.0)}
    @Test fun pulseWaitsForUserThenUsesExactFifteenSeconds() {val e=Engine(Protocol("pulse","Pulse","",listOf(Phase(Kind.PULSE,15))),0);assertFalse(e.tick(999999));e.startPulse(1000000);e.setPulseBeats(16);assertFalse(e.tick(1014999));assertTrue(e.tick(1015000));assertEquals(State.PULSE_REVIEW,e.state);assertEquals(16,e.pulseBeats)}
    @Test fun interruptedPulseMustRestart() {val e=Engine(Protocol("pulse","Pulse","",listOf(Phase(Kind.PULSE,15))),0);e.startPulse(1000);e.setPulseBeats(8);e.pause(7000);assertEquals(State.PULSE_READY,e.state);assertEquals(0,e.pulseBeats)}
    @Test fun automaticMaximumPauseGuardExcludesValue() {val e=Engine(Protocol("hold","Hold","",listOf(Phase(Kind.MP_FULL))),0);e.startCp(1000);assertFalse(e.tick(600999));assertTrue(e.tick(601000));assertTrue(e.interrupted)}
    @Test fun holdsRoundTripWithoutEnteringCpStatistics() {val e=Entry("v4",1760000000000,"Classical",1000,"Completed",listOf(CpValue(20.0,1760000000000,true,"CP")),holds=listOf(HoldValue(Kind.MP_FULL,90.0,1760000000000,true,"Full effort")));assertEquals(e,Codec.readEntry(Codec.entry(e)));assertEquals(20.0,Analysis.stats(e.cps){0}!!.average,0.0)}
    @Test(expected=IllegalArgumentException::class) fun cpCannotBeSmuggledIntoOtherHoldRecords() {val e=Entry("v4",1760000000000,"Test",1,"Completed",holds=listOf(HoldValue(Kind.CP,10.0,1760000000000,true,"bad")));Codec.readEntry(Codec.entry(e))}
    @Test fun everyBlockAndEndingUsesAnExistingCue() {assertEquals(Signals.events.size,Signals.events.distinct().size);Signals.events.forEach{event->assertTrue(Signals.tones.any{it.id==Signals.defaultTone(event)})}}
    @Test fun delayedHoldStopCannotProduceAnUnreadableRecord() {val e=Engine(Protocol("late","Late","",listOf(Phase(Kind.MP_FULL))),0);e.startCp(1000);e.tick(1900000);assertTrue(e.interrupted);assertEquals(600.0,e.pendingCp,0.0);val row=Entry("late",1760000000000,"Late",1900,"Completed",holds=listOf(HoldValue(Kind.MP_FULL,e.pendingCp,1760000000000,false,"Late pause")));assertEquals(row,Codec.readEntry(Codec.entry(row)))}

}
