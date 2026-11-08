package org.breatheless.app

import kotlin.math.*

enum class Kind { REST, REDUCE, RECOVER, CP, LISTEN }
data class Phase(val kind:Kind,val seconds:Int=0) {
    val title get()=when(kind){Kind.REST->"Rest";Kind.REDUCE->"Reduced breathing";Kind.RECOVER->"Recovery";Kind.CP->"Control Pause";Kind.LISTEN->"Mindfulness Buteyko"}
    val guidance get()=when(kind){
        Kind.REST->"Sit comfortably and breathe naturally. Let your shoulders and abdomen relax."
        Kind.REDUCE->"Relax the breathing muscles and allow quieter nasal breaths, with gentle air hunger. Return to natural breathing if the effort increases."
        Kind.RECOVER->"Breathe naturally until comfortable. Avoid controlling the depth or rhythm."
        Kind.CP->"After a normal, passive exhalation, begin the pause. End at the first clear urge to breathe or involuntary movement."
        Kind.LISTEN->"Marilleke Vrancken · 20 minutes 27 seconds. Follow the recording at a comfortable level. You can pause or end the session at any time."
    }
}
data class Protocol(val id:String,val name:String,val description:String,val phases:List<Phase>,val custom:Boolean=false){val durationSeconds get()=phases.sumOf{it.seconds};val cpCount get()=phases.count{it.kind==Kind.CP}}
object Presets {
    val listening=Protocol("mindfulness","Mindfulness Buteyko","Marilleke Vrancken. CP before the recording and again after the end rest and five-minute wait.",listOf(Phase(Kind.REST,300),Phase(Kind.CP),Phase(Kind.LISTEN,1227),Phase(Kind.RECOVER,300),Phase(Kind.REST,300),Phase(Kind.CP)))
    val all=listOf(listening,
        Protocol("standard","Standard","Three reduced-breathing blocks with a rested CP before and after.",listOf(Phase(Kind.REST,300),Phase(Kind.CP),Phase(Kind.REDUCE,300),Phase(Kind.RECOVER,60),Phase(Kind.REDUCE,300),Phase(Kind.RECOVER,60),Phase(Kind.REDUCE,300),Phase(Kind.RECOVER,300),Phase(Kind.CP))),
        Protocol("short","Short","A five-minute practice block followed by recovery and CP.",listOf(Phase(Kind.REST,120),Phase(Kind.REDUCE,300),Phase(Kind.RECOVER,180),Phase(Kind.CP))),
        Protocol("simple","Simple","Ten minutes of reduced breathing with rest and recovery.",listOf(Phase(Kind.REST,120),Phase(Kind.REDUCE,600),Phase(Kind.RECOVER,180))),
        Protocol("meditative","Meditative","Fifteen minutes of reduced breathing between five-minute rests.",listOf(Phase(Kind.REST,300),Phase(Kind.REDUCE,900),Phase(Kind.RECOVER,300))),
        Protocol("morning","Morning CP","Five minutes to settle, followed by one CP measurement.",listOf(Phase(Kind.REST,300),Phase(Kind.CP))),
        Protocol("baseline","Rested baseline","Ten minutes of rest, followed by one CP measurement.",listOf(Phase(Kind.REST,600),Phase(Kind.CP))))
}
enum class State { TIMED, LISTENING, CP_READY, CP_RUNNING, CP_REVIEW, PAUSED, FINISHED }
data class CpValue(val seconds:Double,val timestamp:Long,val valid:Boolean,val label:String)
data class PulseValue(val bpm:Int,val timestamp:Long,val label:String)
data class WalkValue(val steps:Int,val seconds:Double,val source:String)
data class Entry(val id:String,val timestamp:Long,val name:String,val duration:Int,val status:String,val cps:List<CpValue> = emptyList(),val pulses:List<PulseValue> = emptyList(),val walks:List<WalkValue> = emptyList(),val symptom:Int?=null,val comment:String="",val context:String="Practice")

class Engine(val protocol:Protocol,private val started:Long){
    var index=0;private set
    var state=State.TIMED;private set
    private var previous=State.TIMED
    private var deadline=started;private var phaseStarted=started;private var pausedAt=0L;private var pausedTotal=0L;private var cpStarted=0L
    var pendingCp=0.0;private set
    var interrupted=false;private set
    val phase get()=protocol.phases[index.coerceAtMost(protocol.phases.lastIndex)]
    init{require(protocol.phases.isNotEmpty());enter(started)}
    private fun enter(now:Long){phaseStarted=now;deadline=now+phase.seconds*1000L;interrupted=false;state=when(phase.kind){Kind.CP->State.CP_READY;Kind.LISTEN->State.LISTENING;else->State.TIMED}}
    fun remaining(now:Long)=max(0,deadline-(if(state==State.PAUSED)pausedAt else now))
    fun total(now:Long)=(max(0,(if(state==State.PAUSED)pausedAt else now)-started-pausedTotal)/1000).toInt()
    fun cpElapsed(now:Long)=max(0.0,(now-cpStarted)/1000.0)
    fun advance(now:Long){if(state==State.FINISHED)return;if(index==protocol.phases.lastIndex)state=State.FINISHED else{index++;enter(now)}}
    fun tick(now:Long):Boolean{if(state==State.TIMED && now>=deadline){advance(now);return true};if(state==State.CP_RUNNING && cpElapsed(now)>=120){stopCp(now,true);return true};return false}
    fun startCp(now:Long){check(state==State.CP_READY);cpStarted=now;state=State.CP_RUNNING}
    fun stopCp(now:Long,invalid:Boolean=false){check(state==State.CP_RUNNING);pendingCp=cpElapsed(now);interrupted=invalid;state=State.CP_REVIEW}
    fun pause(now:Long){if(state==State.CP_RUNNING){stopCp(now,true);return};if(state==State.TIMED || state==State.LISTENING || state==State.CP_READY){previous=state;pausedAt=now;state=State.PAUSED}}
    fun resume(now:Long){if(state!=State.PAUSED)return;val gap=max(0,now-pausedAt);deadline+=gap;phaseStarted+=gap;pausedTotal+=gap;state=previous}
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
    val events=listOf("REST","REDUCE","RECOVER","CP","LISTEN","CP_END","END")
    val labels=listOf("Rest begins","Reduced breathing begins","Recovery begins","CP is ready","Recording begins","CP measurement stopped","Session finished")
    fun defaultTone(event:String)=when(event){"REST"->"low_knock";"REDUCE"->"rise";"RECOVER"->"fall";"CP"->"two_low";"LISTEN"->"fifth";"CP_END"->"single";else->"end"}
    val styles=listOf("Short","Double","Long","Triple","Rising","Heartbeat")
    fun pattern(style:String):LongArray=when(style){"Short"->longArrayOf(0,100);"Long"->longArrayOf(0,450);"Triple"->longArrayOf(0,90,100,90,100,180);"Rising"->longArrayOf(0,80,110,170,110,300);"Heartbeat"->longArrayOf(0,90,110,210);else->longArrayOf(0,130,110,230)}
    fun amplitudes(pattern:LongArray,intensity:Int,rising:Boolean=false):IntArray{val strength=intensity.coerceIn(0,100)*255/100;val count=pattern.size/2;return pattern.indices.map{if(it%2==0 || strength==0)0 else max(1,if(rising)strength*((it+1)/2)/count.coerceAtLeast(1)else strength)}.toIntArray()}
}
