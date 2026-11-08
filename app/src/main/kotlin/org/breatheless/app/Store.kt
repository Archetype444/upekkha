package org.breatheless.app

import android.content.Context
import android.util.AtomicFile
import org.json.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow

object Codec {
    fun phase(p:Phase)=JSONObject().put("kind",p.kind.name).put("seconds",p.seconds)
    fun protocol(p:Protocol)=JSONObject().put("id",p.id).put("name",p.name).put("description",p.description).put("rounds",p.rounds).put("phases",JSONArray(p.phases.map{phase(it)}))
    private fun txt(o:JSONObject,k:String,max:Int):String{val s=o.getString(k);require(s.length<=max && (k !in listOf("id","name") || s.isNotBlank())){"Invalid $k"};return s}
    fun readProtocol(o:JSONObject):Protocol {
        val a=o.getJSONArray("phases");val rounds=o.optInt("rounds",1)
        require(a.length() in 1..80 && rounds in 1..12 && a.length()*rounds<=512)
        val phases=(0 until a.length()).map{val v=a.getJSONObject(it);val k=Kind.valueOf(v.getString("kind"));val seconds=v.getInt("seconds");require(when{ k in holdKinds->seconds==0;k==Kind.LISTEN->seconds==1227;k==Kind.PULSE->seconds in listOf(15,30,60);else->seconds in 5..3600});Phase(k,seconds)}
        require(phases.count{it.kind==Kind.CP}*rounds<=100 && phases.count{it.kind in holdKinds}*rounds<=100 && phases.count{it.kind==Kind.PULSE}*rounds<=100 && phases.sumOf{it.seconds}*rounds<=14400)
        return Protocol(txt(o,"id",100),txt(o,"name",100),txt(o,"description",1000),phases,true,rounds)
    }
    fun entry(e:Entry)=JSONObject().put("id",e.id).put("timestamp",e.timestamp).put("name",e.name).put("duration",e.duration).put("status",e.status).put("context",e.context).put("symptom",e.symptom ?: JSONObject.NULL).put("comment",e.comment).put("cps",JSONArray(e.cps.map{JSONObject().put("seconds",it.seconds).put("timestamp",it.timestamp).put("valid",it.valid).put("label",it.label)})).put("pulses",JSONArray(e.pulses.map{JSONObject().put("bpm",it.bpm).put("timestamp",it.timestamp).put("label",it.label)})).put("holds",JSONArray(e.holds.map{JSONObject().put("kind",it.kind.name).put("seconds",it.seconds).put("timestamp",it.timestamp).put("valid",it.valid).put("label",it.label)})).put("walks",JSONArray(e.walks.map{JSONObject().put("steps",it.steps).put("seconds",it.seconds).put("source",it.source)}))
    private fun date(o:JSONObject,k:String)=o.getLong(k).also{require(it in 0..4102444800000L)}
    fun readEntry(o:JSONObject):Entry{val c=o.getJSONArray("cps");val p=o.getJSONArray("pulses");val w=o.getJSONArray("walks");require(c.length()<=100 && p.length()<=100 && w.length()<=200)
        val cps=(0 until c.length()).map{val v=c.getJSONObject(it);val n=v.getDouble("seconds");require(n.isFinite() && n>0 && n<=600);CpValue(n,date(v,"timestamp"),v.getBoolean("valid"),txt(v,"label",100))}
        val pulses=(0 until p.length()).map{val v=p.getJSONObject(it);val n=v.getInt("bpm");require(n in 20..250);PulseValue(n,date(v,"timestamp"),txt(v,"label",100))}
        val walks=(0 until w.length()).map{val v=w.getJSONObject(it);val n=v.getInt("steps");val s=v.getDouble("seconds");require(n in 0..100000 && s.isFinite() && s in 0.0..86400.0);WalkValue(n,s,txt(v,"source",40))}
        val h=o.optJSONArray("holds") ?:JSONArray();require(h.length()<=100)
        val holds=(0 until h.length()).map{val v=h.getJSONObject(it);val kind=Kind.valueOf(v.getString("kind"));val seconds=v.getDouble("seconds");require(kind in holdKinds && kind!=Kind.CP && seconds.isFinite() && seconds>0 && seconds<=600);HoldValue(kind,seconds,date(v,"timestamp"),v.getBoolean("valid"),txt(v,"label",100))}
        val symptom=if(o.isNull("symptom"))null else o.getInt("symptom").also{require(it in 0..10)};val d=o.getInt("duration");require(d in 0..86400)
        return Entry(txt(o,"id",100),date(o,"timestamp"),txt(o,"name",100),d,txt(o,"status",80),cps,pulses,walks,symptom,txt(o,"comment",10000),txt(o,"context",80),holds)}
    fun parse(raw:String):Pair<List<Entry>,List<Protocol>>{require(raw.toByteArray().size<=12_000_000){"Backup exceeds 12 MB"};val o=JSONObject(raw);require(o.getString("format")=="BreatheLess" && o.getInt("schemaVersion")==1){"Unsupported backup format"};val e=o.getJSONArray("entries");val p=o.getJSONArray("protocols");require(e.length()<=30000 && p.length()<=500);val entries=(0 until e.length()).map{readEntry(e.getJSONObject(it))};val protocols=(0 until p.length()).map{readProtocol(p.getJSONObject(it))};require(entries.map{it.id}.distinct().size==entries.size && protocols.map{it.id}.distinct().size==protocols.size){"Duplicate IDs"};return Pair(entries,protocols)}
    fun settings(input:JSONObject,old:JSONObject):JSONObject{val result=JSONObject(old.toString());result.remove("brightness");for(k in listOf("sound","haptic","hideCp","pulsePrompt","screenOn","volumeKeys","safetyAccepted"))if(input.has(k)){require(input.get(k) is Boolean);result.put(k,input.getBoolean(k))};for(k in listOf("cueVolume","intensity","volume"))if(input.has(k)){val n=input.getInt(k);require(n in 0..100);result.put(k,n)};for((k,allowed) in listOf("ambience" to listOf("Off","Wind","Fireplace"),"vibration" to Signals.styles))if(input.has(k)){val v=input.getString(k);require(v in allowed);result.put(k,v)};for(e in Signals.events){val k="tone_$e";if(input.has(k)){val v=input.getString(k);require(Signals.tones.any{it.id==v});result.put(k,v)}};if(input.has("favorites")){val a=input.getJSONArray("favorites");require(a.length()<=500);for(i in 0 until a.length())require(a.getString(i).length<=100);result.put("favorites",a)};if(input.has("lastProtocol")){val id=input.getString("lastProtocol");require(id.length<=100);result.put("lastProtocol",id)};if(input.has("fxChannel")){val v=input.getString("fxChannel");require(v in SoundRoute.names);result.put("fxChannel",v)}
        if(input.has("morningReminder")){require(input.get("morningReminder") is Boolean);result.put("morningReminder",input.getBoolean("morningReminder"))}
        for(k in listOf("reminderHour","reminderMinute"))if(input.has(k)){val n=input.getInt(k);require(n in if(k=="reminderHour")0..23 else 0..59);result.put(k,n)}
        if(input.has("freestyle")){val f=input.getJSONObject("freestyle");freestyle(f).protocol();result.put("freestyle",f)}
        if(input.has("freestyleV4")){val protocol=input.getJSONObject("freestyleV4");readProtocol(protocol);result.put("freestyleV4",protocol)}
        return result}
    fun freestyle(o:JSONObject)=Freestyle(o.optInt("rest",120),o.optInt("reduce",300),o.optInt("recovery",60),o.optInt("finalRecovery",300),o.optInt("rounds",3),o.optBoolean("initialCp",true),o.optBoolean("finalCp",true),o.optBoolean("betweenCp",false))
    fun freestyle(f:Freestyle)=JSONObject().put("rest",f.rest).put("reduce",f.reduce).put("recovery",f.recovery).put("finalRecovery",f.finalRecovery).put("rounds",f.rounds).put("initialCp",f.initialCp).put("finalCp",f.finalCp).put("betweenCp",f.betweenCp)
}
class Store private constructor(c:Context){
    private val ctx=c.applicationContext;private val file=AtomicFile(File(ctx.filesDir,"breatheless.json"));val active=AtomicFile(File(ctx.filesDir,"active-session.json"))
    val version=MutableStateFlow(0)
    val entries=mutableListOf<Entry>();val protocols=mutableListOf<Protocol>()
    var settings=JSONObject().put("sound",true).put("haptic",true).put("hideCp",true).put("pulsePrompt",false).put("ambience","Off").put("volume",30).put("cueVolume",45).put("intensity",55).put("vibration","Double").put("screenOn",false).put("volumeKeys",false)
    var error:String?=null;private set
    init{if(file.baseFile.exists() || File(file.baseFile.path+".bak").exists())try{val raw=String(file.readFully(),Charsets.UTF_8);val v=Codec.parse(raw);entries.addAll(v.first);protocols.addAll(v.second);val old=JSONObject(raw).optJSONObject("settings");if(old!=null)settings=Codec.settings(old,settings)}catch(e:Exception){error="Saved data could not be read. The original file has been kept. Restore a backup. ${e.message}"}}
    fun freestyle()=runCatching{Codec.freestyle(settings.optJSONObject("freestyle") ?:JSONObject())}.getOrDefault(Freestyle())
    fun freestylePlan():Protocol=runCatching { settings.optJSONObject("freestyleV4")?.let{Codec.readProtocol(it)} ?:freestyle().protocol() }.getOrElse{Freestyle().protocol()}
    fun saveFreestyle(protocol:Protocol){Codec.readProtocol(Codec.protocol(protocol));settings.put("freestyleV4",Codec.protocol(protocol.copy(id="freestyle",name="Freestyle")));save()}
    fun saveTemplate(protocol:Protocol){Codec.readProtocol(Codec.protocol(protocol));val index=protocols.indexOfFirst{it.id==protocol.id};if(index<0)protocols.add(protocol)else protocols[index]=protocol;save()}
    fun deleteTemplate(id:String){protocols.removeAll{it.id==id};save()}
    fun deleteEntry(id:String){val before=entries.toList();entries.removeAll{it.id==id};try{save()}catch(e:Exception){entries.clear();entries.addAll(before);throw e}}
    fun bool(key:String,default:Boolean=true)=settings.optBoolean(key,default)
    fun int(key:String,default:Int)=settings.optInt(key,default).coerceIn(0,100)
    fun str(key:String,default:String)=settings.optString(key,default)
    fun favorite(id:String)=settings.optJSONArray("favorites")?.let{a->(0 until a.length()).any{a.optString(it)==id}} ?:false
    fun toggleFavorite(id:String){val a=settings.optJSONArray("favorites") ?:JSONArray();val ids=(0 until a.length()).map{a.getString(it)}.toMutableList();if(id in ids)ids.remove(id)else ids.add(id);settings.put("favorites",JSONArray(ids));save()}
    fun json()=JSONObject().put("format","BreatheLess").put("schemaVersion",1).put("appVersion",4).put("exportedAt",System.currentTimeMillis()).put("entries",JSONArray(entries.map{Codec.entry(it)})).put("protocols",JSONArray(protocols.map{Codec.protocol(it)})).put("settings",settings)
    fun save(){check(error==null){error ?: "Data unavailable"};write(file,json().toString());version.value++}
    fun replace(raw:String){val data=Codec.parse(raw);val config=JSONObject(raw).optJSONObject("settings")?.let{Codec.settings(it,settings)} ?:settings;entries.clear();entries.addAll(data.first);protocols.clear();protocols.addAll(data.second);settings=config;error=null;save()}
    fun upsert(e:Entry){val old=entries.indexOfFirst{it.id==e.id};if(old<0)entries.add(e)else entries[old]=e;save()}
    fun restore(raw:String):Pair<Int,Int>{val data=Codec.parse(raw);val input=JSONObject(raw).optJSONObject("settings");val config=if(input==null)settings else Codec.settings(input,settings);val es=data.first.filter{n->entries.none{it.id==n.id}};val ps=data.second.filter{n->protocols.none{it.id==n.id}};val beforeE=entries.toList();val beforeP=protocols.toList();val beforeS=settings;val beforeError=error;if(error!=null && file.baseFile.exists())file.baseFile.copyTo(File(ctx.filesDir,"unreadable-preserved.json"),true);entries.addAll(es);protocols.addAll(ps);settings=config;error=null;try{save()}catch(e:Exception){entries.clear();entries.addAll(beforeE);protocols.clear();protocols.addAll(beforeP);settings=beforeS;error=beforeError;throw e};return Pair(es.size,ps.size)}
    fun recover():Boolean { active.delete();return false }
    companion object{private var instance:Store?=null;fun get(c:Context)=instance ?:synchronized(this){instance ?:Store(c).also{instance=it}};fun id()=UUID.randomUUID().toString();fun write(f:AtomicFile,text:String){var out:java.io.FileOutputStream?=null;try{out=f.startWrite();out.write(text.toByteArray(Charsets.UTF_8));f.finishWrite(out)}catch(e:Exception){f.failWrite(out);throw e}}}
}

