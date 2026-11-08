package org.breatheless.app
import org.json.*
import kotlin.math.abs
private var count=0
private fun test(name:String,block:()->Unit){try{block();count++;println("PASS $name")}catch(e:Throwable){throw AssertionError("FAIL $name",e)}}
private fun near(a:Double,b:Double){check(abs(a-b)<.001){"$a != $b"}}
private fun reject(block:()->Unit){var failed=false;try{block()}catch(_:Exception){failed=true};check(failed){"Invalid input accepted"}}
fun main(){
 val p=Protocol("test","Test","",listOf(Phase(Kind.REST,5),Phase(Kind.CP),Phase(Kind.REDUCE,10),Phase(Kind.RECOVER,5),Phase(Kind.CP)))
 test("Timed phases honour the deadline"){val e=Engine(p,1000);check(!e.tick(5999));check(e.tick(6000));check(e.state==State.CP_READY)}
 test("CP waits for user input"){val e=Engine(p,0);e.tick(5000);check(!e.tick(9999999));check(e.index==1 && e.state==State.CP_READY)}
 test("CP has a stopwatch rather than a fixed hold target"){val e=Engine(p,0);e.tick(5000);e.startCp(6000);e.stopCp(18750);near(e.pendingCp,12.75);check(e.state==State.CP_REVIEW)}
 test("CP review does not advance on a timer"){val e=Engine(p,0);e.tick(5000);e.startCp(6000);e.stopCp(9000);check(!e.tick(999999));check(e.state==State.CP_REVIEW)}
 test("Long unattended CP is marked interrupted"){val e=Engine(p,0);e.tick(5000);e.startCp(6000);check(e.tick(126000));check(e.interrupted && e.state==State.CP_REVIEW)}
 test("Pausing a running CP stops and excludes it"){val e=Engine(p,0);e.tick(5000);e.startCp(6000);e.pause(9000);check(e.interrupted && e.state==State.CP_REVIEW);near(e.pendingCp,3.0)}
 test("Paused timed phases preserve remaining time"){val e=Engine(p,0);e.pause(2000);check(e.remaining(50000)==3000L);e.resume(52000);check(!e.tick(54999));check(e.tick(55000))}
 test("Paused time is removed from session duration"){val e=Engine(p,0);e.pause(2000);e.resume(52000);check(e.total(55000)==5)}
 test("Repeated pause cannot reset the pause instant"){val e=Engine(p,0);e.pause(1000);e.pause(3000);check(e.remaining(90000)==4000L)}
 test("Delayed ticks never skip a CP"){val e=Engine(p,0);e.tick(999999);check(e.index==1 && e.state==State.CP_READY)}
 test("A phase starts with its full duration"){val e=Engine(p,0);e.advance(10000);e.advance(20000);check(e.remaining(20000)==10000L)}
 test("Finished sessions stay in bounds"){val e=Engine(Protocol("one","One","",listOf(Phase(Kind.REST,5))),0);e.tick(5000);e.advance(6000);check(e.index==0 && e.state==State.FINISHED)}
 test("CP cannot start in a timed phase"){reject{Engine(p,0).startCp(1)}}
 test("CP cannot stop before it begins"){reject{Engine(p,0).stopCp(1)}}
 val audio=Presets.listening
 test("Audio protocol contains both CP measurements"){check(audio.cpCount==2 && audio.phases.first().kind==Kind.REST && audio.phases.last().kind==Kind.CP)}
 test("Audio protocol includes end rest and a separate five-minute wait"){check(audio.phases.map{it.kind}==listOf(Kind.REST,Kind.CP,Kind.LISTEN,Kind.RECOVER,Kind.REST,Kind.CP));check(audio.phases[3].seconds==300 && audio.phases[4].seconds==300)}
 test("Initial CP precedes recording playback"){val e=Engine(audio,0);e.tick(300000);check(e.state==State.CP_READY && e.index==1);e.startCp(301000);e.stopCp(310000);check(e.index==1);e.advance(311000);check(e.state==State.LISTENING && e.index==2)}
 test("Recording never completes from wall time alone"){val e=Engine(audio,0);e.advance(1);e.advance(2);check(e.state==State.LISTENING);check(!e.tick(9999999));check(e.index==2)}
 test("Pause and resume retain recording phase"){val e=Engine(audio,0);e.advance(1);e.advance(2);e.pause(3000);check(e.state==State.PAUSED);e.resume(900000);check(e.state==State.LISTENING && e.index==2)}
 test("Completion advances recording into recovery"){val e=Engine(audio,0);e.advance(1);e.advance(2);e.advance(1230000);check(e.index==3 && e.state==State.TIMED);check(e.remaining(1230000)==300000L)}
 test("Second CP waits until both post-recording intervals have ended"){val e=Engine(audio,0);e.advance(1);e.advance(2);e.advance(1000);check(!e.tick(300999));check(e.tick(301000));check(e.index==4);check(!e.tick(600999));check(e.tick(601000));check(e.state==State.CP_READY && e.index==5)}
 test("Post-recording wait can be paused"){val e=Engine(audio,0);repeat(4){e.advance(1000)};e.pause(101000);e.resume(501000);check(e.remaining(501000)==200000L)}
 test("Audio metadata duration is included exactly once in default protocol"){check(audio.phases.count{it.kind==Kind.LISTEN}==1);check(audio.phases[2].seconds==1227);check(audio.durationSeconds==2127)}
 test("All seven presets pass protocol validation"){check(Presets.all.size==7);Presets.all.forEach{Codec.readProtocol(Codec.protocol(it))}}
 test("Twenty cue choices have unique IDs"){check(Signals.tones.size==20);check(Signals.tones.map{it.id}.distinct().size==20)}
 test("All seven events have different audible defaults"){val ids=Signals.events.map{Signals.defaultTone(it)};check(ids.distinct().size==7 && "none" !in ids);ids.forEach{id->check(Signals.tones.any{it.id==id})}}
 test("Every phase type has a cue event"){Kind.entries.forEach{check(it.name in Signals.events)};check("END" in Signals.events && "CP_END" in Signals.events)}
 test("Silent tone has no samples"){check(Signals.tones.single{it.id=="none"}.notes.isEmpty())}
 test("Audible tones have bounded durations"){Signals.tones.filter{it.id!="none"}.forEach{val seconds=it.notes.sumOf{n->n.second};check(seconds>0 && seconds<2);check(it.notes.any{n->n.first>0})}}
 test("All six vibration patterns contain real on intervals"){check(Signals.styles.size==6);Signals.styles.forEach{val a=Signals.pattern(it);check(a[0]==0L && a.size>=2);check(a.withIndex().any{(i,v)->i%2==1 && v>0})}}
 test("Vibration strength is bounded and off intervals stay zero"){for(strength in listOf(1,55,100)){val a=Signals.pattern("Triple");val v=Signals.amplitudes(a,strength);check(v.size==a.size);v.forEachIndexed{i,n->check(if(i%2==0)n==0 else n in 1..255)}}}
 test("Zero intensity maps to no vibration"){check(Signals.amplitudes(Signals.pattern("Double"),0).all{it==0})}
 test("Rising vibration increases amplitude"){val a=Signals.amplitudes(Signals.pattern("Rising"),60,true).filter{it>0};check(a.size==3 && a[0]<a[1] && a[1]<a[2])}
 test("Invalid CP values are excluded from statistics"){val s=Analysis.stats(listOf(CpValue(10.0,1,true,"a"),CpValue(999.0,1,false,"b"),CpValue(20.0,2,true,"c"))){it}!!;check(s.count==2);near(s.average,15.0);near(s.low,10.0);near(s.best,20.0)}
 test("Chart groups multiple measurements per day"){val s=Analysis.stats(listOf(CpValue(10.0,1,true,"a"),CpValue(30.0,1,true,"b"))){it}!!;check(s.points.size==1);near(s.points.first().value,20.0);check(s.change==null)}
 test("Trend weights recorded days equally"){val s=Analysis.stats(listOf(CpValue(10.0,1,true,"a"),CpValue(12.0,2,true,"b"),CpValue(18.0,3,true,"c"),CpValue(20.0,4,true,"d"))){it}!!;near(s.change!!,8.0)}
 test("An empty or excluded-only chart stays empty"){check(Analysis.stats(emptyList()){it}==null);check(Analysis.stats(listOf(CpValue(42.0,1,false,"bad"))){it}==null)}
 test("Non-finite values cannot affect charts"){check(Analysis.stats(listOf(CpValue(Double.NaN,1,true,"bad"))){it}==null)}
 test("CSV quotes preserve newlines and embedded quotes"){check(Analysis.csv("text,\"quoted\"\nline")=="\"text,\"\"quoted\"\"\nline\"")}
 test("CSV formula-like text is neutralised"){check(Analysis.csv("=1+1")=="\"'=1+1\"");check(Analysis.csv("  @SUM(A1)").startsWith("\"'"))}
 val entry=Entry("v1-id",1760000000000L,"Standard",1620,"Completed",listOf(CpValue(17.25,1760000000000L,true,"Before"),CpValue(30.0,1760000001000L,false,"Excluded")),listOf(PulseValue(64,1760000000000L,"Before")),listOf(WalkValue(5,3.5,"Manual")),4,"A note, \"quote\"\nand second line","Morning")
 fun backup()=JSONObject().put("format","BreatheLess").put("schemaVersion",1).put("entries",JSONArray().put(Codec.entry(entry))).put("protocols",JSONArray().put(Codec.protocol(p)))
 test("V1-shaped diary record round-trips"){check(Codec.readEntry(Codec.entry(entry))==entry)}
 test("V1 backup schema remains accepted"){val data=Codec.parse(backup().toString());check(data.first.single()==entry && data.second.single().phases==p.phases)}
 test("Independent literal v1 backup imports without rewriting its measurements"){
  val raw="""{"format":"BreatheLess","schemaVersion":1,"exportedAt":1760000000000,"entries":[{"id":"legacy-1","timestamp":1760000000000,"name":"Morning CP","duration":315,"status":"Completed","context":"Morning","cps":[{"seconds":14.8,"timestamp":1760000000000,"valid":true,"label":"CP 1"}],"pulses":[{"bpm":62,"timestamp":1760000000000,"label":"Before practice"}],"walks":[],"symptom":2,"comment":"Original version one note"}],"protocols":[{"id":"legacy-custom","name":"My morning","description":"Saved in v1","phases":[{"kind":"REST","seconds":300},{"kind":"CP","seconds":0}]}],"settings":{"sound":false,"haptic":true,"ambience":"Wind","volume":22,"hideCp":true,"pulsePrompt":false}}"""
  val decoded=Codec.parse(raw);val e=decoded.first.single();check(e.id=="legacy-1" && e.comment=="Original version one note" && e.context=="Morning");near(e.cps.single().seconds,14.8);check(e.cps.single().valid && e.pulses.single().bpm==62 && e.symptom==2);check(decoded.second.single().phases==listOf(Phase(Kind.REST,300),Phase(Kind.CP)));val prefs=Codec.settings(JSONObject(raw).getJSONObject("settings"),JSONObject().put("cueVolume",45));check(!prefs.getBoolean("sound") && prefs.getInt("volume")==22 && prefs.getInt("cueVolume")==45)
 }
 test("Audio phase order survives backup"){val b=backup().put("protocols",JSONArray().put(Codec.protocol(audio)));check(Codec.parse(b.toString()).second.single().phases==audio.phases)}
 test("Unsupported backup versions are rejected"){reject{Codec.parse(backup().put("schemaVersion",2).toString())}}
 test("Unrelated JSON is rejected"){reject{Codec.parse("{}")}}
 test("Broken JSON is rejected"){reject{Codec.parse("{broken")}}
 test("Duplicate entry IDs are rejected"){val b=backup();b.getJSONArray("entries").put(Codec.entry(entry));reject{Codec.parse(b.toString())}}
 test("Invalid CP values are rejected"){val o=Codec.entry(entry);o.getJSONArray("cps").getJSONObject(0).put("seconds",-1);reject{Codec.readEntry(o)}}
 test("Invalid symptom scores are rejected"){reject{Codec.readEntry(Codec.entry(entry).put("symptom",11))}}
 test("Invalid pulse values are rejected"){val o=Codec.entry(entry);o.getJSONArray("pulses").getJSONObject(0).put("bpm",999);reject{Codec.readEntry(o)}}
 test("Invalid timed phase lengths are rejected"){val o=Codec.protocol(p);o.getJSONArray("phases").getJSONObject(0).put("seconds",0);reject{Codec.readProtocol(o)}}
 test("Fixed CP targets are rejected"){val o=Codec.protocol(p);o.getJSONArray("phases").getJSONObject(1).put("seconds",30);reject{Codec.readProtocol(o)}}
 test("False audio durations are rejected"){val o=Codec.protocol(audio);o.getJSONArray("phases").getJSONObject(2).put("seconds",10);reject{Codec.readProtocol(o)}}
 test("Empty protocols are rejected"){reject{Codec.readProtocol(Codec.protocol(p).put("phases",JSONArray()))}}
 test("More than twenty CPs are rejected"){reject{Codec.readProtocol(Codec.protocol(p.copy(phases=List(21){Phase(Kind.CP)})))}}
 test("Unknown phase kinds are rejected"){val o=Codec.protocol(p);o.getJSONArray("phases").getJSONObject(0).put("kind","MAXIMUM");reject{Codec.readProtocol(o)}}
 test("Oversized backups are rejected"){reject{Codec.parse(" ".repeat(12000001))}}
 test("Per-phase tone choices survive settings import"){val input=JSONObject();Signals.events.forEach{input.put("tone_$it","glass")};val out=Codec.settings(input,JSONObject());Signals.events.forEach{check(out.getString("tone_$it")=="glass")}}
 test("Favorites and last protocol survive settings import"){val o=Codec.settings(JSONObject().put("favorites",JSONArray().put("mindfulness")).put("lastProtocol","mindfulness"),JSONObject());check(o.getJSONArray("favorites").getString(0)=="mindfulness" && o.getString("lastProtocol")=="mindfulness")}
 test("V1 audio preferences migrate without new keys"){val o=Codec.settings(JSONObject().put("sound",false).put("haptic",true).put("ambience","Wind").put("volume",30),JSONObject().put("cueVolume",45));check(!o.getBoolean("sound") && o.getBoolean("haptic") && o.getInt("cueVolume")==45)}
 test("Unsupported tone identifiers are rejected"){reject{Codec.settings(JSONObject().put("tone_REST","missing"),JSONObject())}}
 test("Invalid intensity is rejected"){reject{Codec.settings(JSONObject().put("intensity",101),JSONObject())}}
 test("Invalid vibration pattern is rejected"){reject{Codec.settings(JSONObject().put("vibration","unknown"),JSONObject())}}
 test("Export retains CP validity, pulse, walking and notes"){val s=Exports.csv(listOf(entry));check(s.startsWith("\uFEFF") && s.contains("excluded") && s.contains("64 |") && s.contains("Manual") && s.contains("second line") && s.contains("Morning"))}
 test("Export neutralises formula-like titles"){check(Exports.csv(listOf(entry.copy(name="=HYPERLINK(\"bad\")"))).contains("'=HYPERLINK"))}
 test("Empty exports retain headers"){check(Exports.csv(emptyList()).contains("Entry ID"))}
 println("\n$count checks passed")
}

