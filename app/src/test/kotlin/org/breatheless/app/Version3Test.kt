package org.breatheless.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class Version3Test {
    @Test fun productionRegressionChecks() = runProductionChecks()
    @Test fun morningStartsReadyWithoutRest() { val e=Engine(Presets.morning,0); assertEquals(State.CP_READY,e.state); assertEquals(1,e.protocol.phases.size) }
    @Test fun defaultFreestyleKeepsTwoMeasurements() { val p=Freestyle().protocol();assertEquals(2,p.cpCount);assertEquals(Phase(Kind.REST,120),p.phases.first());assertEquals(Phase(Kind.CP),p.phases.last()) }
    @Test fun optionalBlocksCanAllBeLeftOut() { val p=Freestyle(rest=0,recovery=0,finalRecovery=0,rounds=1,initialCp=false,finalCp=false).protocol();assertEquals(listOf(Phase(Kind.REDUCE,300)),p.phases) }
    @Test fun betweenRoundMeasurementsRemainManual() { val p=Freestyle(rounds=4,betweenCp=true).protocol();assertEquals(5,p.cpCount);assertTrue(p.phases.filter{it.kind==Kind.CP}.all{it.seconds==0}) }
    @Test fun freestyleRetainsExactSeconds() { val f=Freestyle(37,123,19,301,2,false,true,true);assertEquals(f,Codec.freestyle(Codec.freestyle(f)));assertEquals(f.protocol().phases,Codec.readProtocol(Codec.protocol(f.protocol())).phases) }
    @Test(expected=IllegalArgumentException::class) fun excessFreestyleDurationIsRejected() { Freestyle(reduce=3600,rounds=12).protocol() }
    @Test(expected=IllegalArgumentException::class) fun zeroRoundsAreRejected() { Freestyle(rounds=0).protocol() }
    @Test fun oldBrightnessIsDiscarded() { val s=Codec.settings(JSONObject().put("brightness",5),JSONObject().put("brightness",99).put("sound",true));assertFalse(s.has("brightness"));assertTrue(s.getBoolean("sound")) }
    @Test fun routesAndReminderRoundTrip() { val s=Codec.settings(JSONObject().put("fxChannel","Media").put("morningReminder",true).put("reminderHour",6).put("reminderMinute",37),JSONObject());assertEquals("Media",s.getString("fxChannel"));assertEquals(37,s.getInt("reminderMinute"));assertTrue(s.getBoolean("morningReminder"));assertEquals(5,SoundRoute.names.size) }
    @Test(expected=IllegalArgumentException::class) fun unknownSoundRouteRejected() { Codec.settings(JSONObject().put("fxChannel","Unknown"),JSONObject()) }
    @Test(expected=IllegalArgumentException::class) fun invalidReminderHourRejected() { Codec.settings(JSONObject().put("reminderHour",24),JSONObject()) }
    @Test fun nextReminderIsTodayOrTomorrow() { val now=ZonedDateTime.of(2026,11,8,7,0,0,0,ZoneId.of("UTC"));assertEquals(now.withHour(8),nextReminder(now,8,0));assertEquals(now.plusDays(1),nextReminder(now,7,0)) }
    @Test fun remindersHandleDstGapAndOverlap() { val zone=ZoneId.of("Europe/London");val spring=ZonedDateTime.of(2026,3,29,0,0,0,0,zone);assertEquals(2,nextReminder(spring,1,30).hour);val autumn=ZonedDateTime.of(2026,10,25,2,0,0,0,zone);assertTrue(nextReminder(autumn,1,30).isAfter(autumn));assertEquals(26,nextReminder(autumn,1,30).dayOfMonth) }
}
