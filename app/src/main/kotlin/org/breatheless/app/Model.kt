package org.breatheless.app

import kotlin.math.*

enum class Kind { REST, REDUCE, RECOVER, CP, LISTEN, PULSE, MP_LIGHT, MP_MEDIUM, MP_FULL, EXTENDED, VERY_SHALLOW, SUPPRESS, DIMINISHED, SETTLE, GROUND }
val holdKinds = setOf(Kind.CP, Kind.MP_LIGHT, Kind.MP_MEDIUM, Kind.MP_FULL, Kind.EXTENDED)
data class Phase(val kind: Kind, val seconds: Int = 0) {
    val title get() = when(kind) {
        Kind.REST -> "Rest"; Kind.REDUCE -> "Reduced breathing"; Kind.RECOVER -> "Recovery"; Kind.CP -> "Control Pause"; Kind.LISTEN -> "Recording"
        Kind.PULSE -> "Pulse"; Kind.MP_LIGHT -> "Maximum pause · Light effort"; Kind.MP_MEDIUM -> "Maximum pause · Medium effort"; Kind.MP_FULL -> "Maximum pause · Full effort"
        Kind.EXTENDED -> "Extended pause"; Kind.VERY_SHALLOW -> "Very shallow breathing"; Kind.SUPPRESS -> "Shallow recovery"; Kind.DIMINISHED -> "Diminished breathing"; Kind.SETTLE -> "Settle"; Kind.GROUND -> "Ground and orient"
    }
    val guidance get() = when(kind) {
        Kind.REST,Kind.SETTLE -> "Sit comfortably with your back upright, feet supported and abdomen relaxed. Breathe naturally and let yourself settle."
        Kind.REDUCE,Kind.VERY_SHALLOW -> "Allow quieter nasal breaths with gentle air hunger. Keep your abdomen relaxed and let each exhalation happen naturally. Ease the effort if you become tense."
        Kind.RECOVER -> "Breathe naturally and allow the air hunger to fade."
        Kind.CP -> "After a normal, passive exhalation, begin the pause. Stop at the first clear urge to breathe or involuntary movement. Your next breath should be easy."
        Kind.LISTEN -> "Follow the recording at a comfortable level. Stop if you feel unwell."
        Kind.PULSE -> "Find your pulse at your wrist. Start the count, then tap once for each beat, or count mentally and adjust the total afterward."
        Kind.MP_LIGHT -> "After a normal exhalation, pinch your nose and begin a seated breath hold. Remain relaxed and still. Stop the hold yourself; stop immediately if you feel unwell."
        Kind.MP_MEDIUM -> "The original medium-effort variation adds gentle rocking and twisting to a seated pause. Use only the effort you have been taught, and stop if you feel unwell."
        Kind.MP_FULL -> "The original full-effort variation adds movement to a pause. This is advanced practice; stay safely seated if you have not been coached. Never continue through dizziness."
        Kind.EXTENDED -> "After the breathing block, begin a gentle pause after an ordinary exhalation. End it at a comfortable level of air hunger. Use light effort if you are unsure."
        Kind.SUPPRESS -> "Let the next breaths be small and relaxed as the urge to breathe deeply settles. Do not force recovery or continue through distress."
        Kind.DIMINISHED -> "Relax gradually from the top of your body to your feet. Allow breathing to reduce through relaxation and maintain light air hunger without forcing the rhythm."
        Kind.GROUND -> "Let air hunger fade and breathing settle. Notice the support beneath you and the details of your surroundings."
    }
}
data class Protocol(val id:String,val name:String,val description:String,val phases:List<Phase>,val custom:Boolean=false,val rounds:Int=1) {
    val durationSeconds get() = phases.sumOf { it.seconds } * rounds
    val cpCount get() = phases.count { it.kind==Kind.CP } * rounds
    fun expanded() = if(rounds==1)this else copy(phases=List(rounds){phases}.flatten(),rounds=1)
}
object Presets {
    val morning=Protocol("morning","Morning CP","",listOf(Phase(Kind.CP)))
    val listening=Protocol("mindfulness","Guided Buteyko","",listOf(Phase(Kind.CP),Phase(Kind.LISTEN,1227),Phase(Kind.RECOVER,300),Phase(Kind.CP)))
    // Stage order and durations ported from Adam Shand's breathe-less, MIT, commit 0612c637dc9a8cf4c685217ce4ec9f526f615b8e.
    val diminished=Protocol("diminished","Diminished Breathing","Adapted from Adam Shand’s breathe-less.",listOf(
        Phase(Kind.SETTLE,180),Phase(Kind.PULSE,15),Phase(Kind.CP),Phase(Kind.DIMINISHED,300),Phase(Kind.EXTENDED),Phase(Kind.DIMINISHED,300),Phase(Kind.GROUND,180),Phase(Kind.PULSE,15),Phase(Kind.CP)))
    val classical=Protocol("classical","Classical Buteyko","Adapted from Adam Shand’s breathe-less.",listOf(
        Phase(Kind.PULSE,15),Phase(Kind.CP),Phase(Kind.VERY_SHALLOW,180),Phase(Kind.RECOVER,30),
        Phase(Kind.MP_LIGHT),Phase(Kind.SUPPRESS,10),Phase(Kind.VERY_SHALLOW,180),Phase(Kind.RECOVER,30),
        Phase(Kind.MP_MEDIUM),Phase(Kind.SUPPRESS,20),Phase(Kind.VERY_SHALLOW,180),Phase(Kind.RECOVER,30),
        Phase(Kind.MP_FULL),Phase(Kind.SUPPRESS,30),Phase(Kind.VERY_SHALLOW,180),Phase(Kind.RECOVER,60),Phase(Kind.CP),Phase(Kind.PULSE,15)))
    val all=listOf(morning,listening,diminished,classical)
}
// Read the exact v3 freestyle arrangement when migrating existing settings.
data class Freestyle(val rest:Int=120,val reduce:Int=300,val recovery:Int=60,val finalRecovery:Int=300,val rounds:Int=3,val initialCp:Boolean=true,val finalCp:Boolean=true,val betweenCp:Boolean=false) {
    fun protocol():Protocol {
        require(rest in 0..3600 && (rest==0 || rest>=5));require(reduce in 5..3600 && recovery in 0..3600 && (recovery==0 || recovery>=5))
        require(finalRecovery in 0..3600 && (finalRecovery==0 || finalRecovery>=5) && rounds in 1..12)
        val phases=mutableListOf<Phase>()
        if(rest>0)phases+=Phase(Kind.REST,rest);if(initialCp)phases+=Phase(Kind.CP)
        repeat(rounds){n->phases+=Phase(Kind.REDUCE,reduce);val recover=if(n==rounds-1)finalRecovery else recovery;if(recover>0)phases+=Phase(Kind.RECOVER,recover);if(n<rounds-1 && betweenCp)phases+=Phase(Kind.CP)}
        if(finalCp)phases+=Phase(Kind.CP)
        require(phases.sumOf{it.seconds}<=14400){"A freestyle practice can be up to four hours."}
        return Protocol("freestyle","Freestyle","",phases,true)
    }
}
object Durations {
    val rest=listOf(15,30,45,60,120,300,600)
    val breathing=listOf(60,120,300,600,900,1200,1500,1800,2100,2400,2700,3000,3300,3600)
    val interval=listOf(0,15,30,60,120,300,600,900,1200,1800)
    fun label(seconds:Int):String=when {
        seconds==0 -> "None"
        seconds<60 -> "$seconds sec"
        seconds%3600==0 -> if(seconds==3600) "1 hour" else "${seconds/3600} hours"
        seconds%60==0 -> "${seconds/60} min"
        else -> "${seconds/60} min ${seconds%60} sec"
    }
    fun forKind(kind:Kind)=when(kind){ Kind.LISTEN->listOf(1227);Kind.PULSE->listOf(15,30,60);Kind.REDUCE,Kind.DIMINISHED,Kind.VERY_SHALLOW->breathing;Kind.SUPPRESS->listOf(10,15,20,30,45,60);else->rest }
    fun default(kind:Kind)=when {kind in holdKinds -> 0;kind==Kind.LISTEN->1227;kind==Kind.PULSE->15;kind in listOf(Kind.REDUCE,Kind.VERY_SHALLOW,Kind.DIMINISHED)->300;else->60}
}
enum class State { TIMED, LISTENING, CP_READY, CP_RUNNING, CP_REVIEW, HOLD_READY, HOLD_RUNNING, HOLD_REVIEW, PULSE_READY, PULSE_RUNNING, PULSE_REVIEW, PAUSED, FINISHED }
data class CpValue(val seconds:Double,val timestamp:Long,val valid:Boolean,val label:String)
data class HoldValue(val kind:Kind,val seconds:Double,val timestamp:Long,val valid:Boolean,val label:String)
data class PulseValue(val bpm:Int,val timestamp:Long,val label:String)
data class WalkValue(val steps:Int,val seconds:Double,val source:String)
data class Entry(val id:String,val timestamp:Long,val name:String,val duration:Int,val status:String,val cps:List<CpValue> = emptyList(),val pulses:List<PulseValue> = emptyList(),val walks:List<WalkValue> = emptyList(),val symptom:Int?=null,val comment:String="",val context:String="Practice",val holds:List<HoldValue> = emptyList())

data class PracticeSnapshot(val protocolName:String,val index:Int,val count:Int,val phase:Phase,val state:State,val remainingSeconds:Long,val pauseSeconds:Double,val pendingPause:Double,val interrupted:Boolean,val pulseBeats:Int,val note:String,val ending:Boolean,val hasPulseStages:Boolean)
class Engine(source:Protocol,private val started:Long) {
    val protocol=source.expanded()
    var index=0;private set
    var state=State.TIMED;private set
    private var previous=State.TIMED
    private var deadline=started;private var pausedAt=0L;private var pausedTotal=0L;private var cpStarted=0L
    var pendingCp=0.0;private set
    var pulseBeats=0;private set
    var interrupted=false;private set
    val phase get()=protocol.phases[index.coerceAtMost(protocol.phases.lastIndex)]
    init { require(protocol.phases.isNotEmpty());enter(started) }
    private fun enter(now:Long){deadline=now+phase.seconds*1000L;interrupted=false;pulseBeats=0;pendingCp=0.0;state=when{phase.kind==Kind.CP->State.CP_READY;phase.kind in holdKinds->State.HOLD_READY;phase.kind==Kind.PULSE->State.PULSE_READY;phase.kind==Kind.LISTEN->State.LISTENING;else->State.TIMED}}
    fun remaining(now:Long)=max(0,deadline-(if(state==State.PAUSED)pausedAt else now))
    fun total(now:Long)=(max(0,(if(state==State.PAUSED)pausedAt else now)-started-pausedTotal)/1000).toInt()
    fun cpElapsed(now:Long)=max(0.0,(now-cpStarted)/1000.0)
    fun advance(now:Long){if(state==State.FINISHED)return;if(index==protocol.phases.lastIndex)state=State.FINISHED else{index++;enter(now)}}
    fun tick(now:Long):Boolean {
        if(state==State.TIMED && now>=deadline){advance(now);return true}
        if(state==State.PULSE_RUNNING && now>=deadline){state=State.PULSE_REVIEW;return true}
        if(state in listOf(State.CP_RUNNING,State.HOLD_RUNNING) && cpElapsed(now)>=if(phase.kind==Kind.CP)120 else 600){stopCp(now,true);return true}
        return false
    }
    fun startCp(now:Long){check(state==State.CP_READY || state==State.HOLD_READY);cpStarted=now;state=if(phase.kind==Kind.CP)State.CP_RUNNING else State.HOLD_RUNNING}
    fun stopCp(now:Long,invalid:Boolean=false){check(state==State.CP_RUNNING || state==State.HOLD_RUNNING);pendingCp=cpElapsed(now);interrupted=invalid;state=if(phase.kind==Kind.CP)State.CP_REVIEW else State.HOLD_REVIEW}
    fun startPulse(now:Long){check(state==State.PULSE_READY);pulseBeats=0;deadline=now+phase.seconds*1000L;state=State.PULSE_RUNNING}
    fun setPulseBeats(beats:Int){check(state in listOf(State.PULSE_RUNNING,State.PULSE_REVIEW));pulseBeats=beats.coerceIn(0,250)}
    fun pause(now:Long){if(state in listOf(State.CP_RUNNING,State.HOLD_RUNNING)){stopCp(now,true);return};if(state==State.PULSE_RUNNING){pulseBeats=0;state=State.PULSE_READY;return};if(state in listOf(State.TIMED,State.LISTENING,State.CP_READY,State.HOLD_READY,State.PULSE_READY)){previous=state;pausedAt=now;state=State.PAUSED}}
    fun resume(now:Long){if(state!=State.PAUSED)return;val gap=max(0,now-pausedAt);deadline+=gap;pausedTotal+=gap;state=previous}
    fun snapshot(now:Long,note:String="",ending:Boolean=false)=PracticeSnapshot(protocol.name,index,protocol.phases.size,phase,state,(remaining(now)+999)/1000,if(state in listOf(State.CP_RUNNING,State.HOLD_RUNNING))cpElapsed(now) else 0.0,pendingCp,interrupted,pulseBeats,note,ending,protocol.phases.any{it.kind==Kind.PULSE})
}

data class DayPoint(val day:Long,val value:Double)
data class Stats(val count:Int,val average:Double,val low:Double,val best:Double,val change:Double?,val points:List<DayPoint>)
object Analysis {
    fun stats(values:List<CpValue>,dayKey:(Long)->Long):Stats?{val v=values.filter{it.valid && it.seconds.isFinite() && it.seconds>0};if(v.isEmpty())return null;val points=v.groupBy{dayKey(it.timestamp)}.toSortedMap().map{DayPoint(it.key,it.value.map{v->v.seconds}.average())};val n=max(1,points.size/2);return Stats(v.size,v.map{it.seconds}.average(),v.minOf{it.seconds},v.maxOf{it.seconds},if(points.size<2)null else points.takeLast(n).map{it.value}.average()-points.take(n).map{it.value}.average(),points)}
    fun csv(s:String):String{val safe=if(s.trimStart().firstOrNull() in listOf('=','+','-','@','\t','\r'))"'$s"else s;return "\""+safe.replace("\"","\"\"")+"\""}
}
object Signals {
    data class Tone(val id:String,val name:String,val notes:List<Pair<Double,Double>>)
    val tones=listOf(
        Tone("low_knock","Low knock",listOf(180.0 to .18)),Tone("two_low","Two low notes",listOf(220.0 to .16,0.0 to .12,220.0 to .24)),
        Tone("rise","Rising pair",listOf(440.0 to .20,0.0 to .09,660.0 to .30)),Tone("fall","Falling pair",listOf(660.0 to .20,0.0 to .09,440.0 to .30)),
        Tone("bell","Bell",listOf(528.0 to .85)),Tone("glass","Glass",listOf(1046.5 to .40)),Tone("wood","Woodblock",listOf(330.0 to .12)),
        Tone("three_rise","Three rising notes",listOf(330.0 to .18,0.0 to .07,440.0 to .18,0.0 to .07,660.0 to .30)),
        Tone("three_fall","Three falling notes",listOf(660.0 to .18,0.0 to .07,440.0 to .18,0.0 to .07,330.0 to .30)),
        Tone("deep_bell","Deep bell",listOf(196.0 to 1.05)),Tone("pulse","Four short pulses",listOf(440.0 to .10,0.0 to .10,440.0 to .10,0.0 to .10,440.0 to .10,0.0 to .10,440.0 to .16)),
        Tone("gong","Low gong",listOf(146.83 to 1.20)),Tone("chime","High chime",listOf(880.0 to .50,0.0 to .12,1174.66 to .55)),
        Tone("fifth","Open fifth",listOf(261.63 to .27,0.0 to .10,392.0 to .48)),Tone("single","Clear note",listOf(660.0 to .45)),
        Tone("end","Session complete",listOf(392.0 to .22,0.0 to .08,523.25 to .22,0.0 to .08,783.99 to .65)),
        Tone("soft_double","Two soft bells",listOf(396.0 to .40,0.0 to .18,396.0 to .50)),Tone("tuning","Tuning fork",listOf(440.0 to 1.20)),
        Tone("alternating","Alternating notes",listOf(330.0 to .15,0.0 to .08,660.0 to .15,0.0 to .08,330.0 to .30)),Tone("none","Silent",emptyList()))
    val events=Kind.entries.map{it.name}+listOf("CP_END","HOLD_END","PULSE_END","END")
    val labels=Kind.entries.map{Phase(it).title+if(it in holdKinds)" is ready" else " begins"}+listOf("CP measurement stopped","Pause measurement stopped","Pulse count finished","Session finished")
    fun defaultTone(event:String)=when(event){
        "REST"->"low_knock";"REDUCE"->"rise";"RECOVER"->"fall";"CP"->"two_low";"LISTEN"->"fifth";"CP_END"->"single";"END"->"end"
        "PULSE"->"glass";"MP_LIGHT"->"deep_bell";"MP_MEDIUM"->"three_rise";"MP_FULL"->"gong";"EXTENDED"->"tuning";"VERY_SHALLOW"->"soft_double";"SUPPRESS"->"wood";"DIMINISHED"->"bell";"SETTLE"->"chime";"GROUND"->"three_fall";"HOLD_END"->"alternating";"PULSE_END"->"pulse";else->"end"
    }
    val styles=listOf("Short","Double","Long","Triple","Rising","Heartbeat")
    fun pattern(style:String):LongArray=when(style){"Short"->longArrayOf(0,100);"Long"->longArrayOf(0,450);"Triple"->longArrayOf(0,90,100,90,100,180);"Rising"->longArrayOf(0,80,110,170,110,300);"Heartbeat"->longArrayOf(0,90,110,210);else->longArrayOf(0,130,110,230)}
    fun amplitudes(pattern:LongArray,intensity:Int,rising:Boolean=false):IntArray{val strength=intensity.coerceIn(0,100)*255/100;val count=pattern.size/2;return pattern.indices.map{if(it%2==0 || strength==0)0 else max(1,if(rising)strength*((it+1)/2)/count.coerceAtLeast(1)else strength)}.toIntArray()}
}

