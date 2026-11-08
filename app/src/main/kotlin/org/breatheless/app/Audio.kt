package org.breatheless.app

import android.content.Context
import android.media.*
import android.os.*

class Feedback(private val c:Context){
    private val store=Store.get(c)
    private val vibrator=c.getSystemService(Vibrator::class.java)
    private val pool=SoundPool.Builder().setMaxStreams(2).setAudioAttributes(SoundRoute.attributes(c)).build()
    private val samples=mutableMapOf<String,Int>();private val ready=mutableSetOf<Int>();private var pending:String?=null;private var stream=0
    init{pool.setOnLoadCompleteListener{_,id,status->if(status==0){ready.add(id);pending?.let{tone->if(samples[tone]==id){pending=null;play(tone)}}}};Signals.tones.filter{it.notes.isNotEmpty()}.forEach{samples[it.id]=pool.load(c,c.resources.getIdentifier("cue_${it.id}","raw",c.packageName),1)}}
    private fun play(id:String){val sample=samples[id] ?:return;if(sample !in ready){pending=id;return};if(stream!=0)pool.stop(stream);val gain=store.int("cueVolume",45)/100f;stream=pool.play(sample,gain,gain,1,0,1f)}
    fun tone(id:String){pending=null;if(id!="none")play(id)}
    fun vibrate(style:String=store.str("vibration","Double"),strength:Int=store.int("intensity",55)){
        if(strength==0 || vibrator?.hasVibrator()!=true)return
        val pattern=Signals.pattern(style);val effect=if(vibrator.hasAmplitudeControl())VibrationEffect.createWaveform(pattern,Signals.amplitudes(pattern,strength,style=="Rising"),-1)else VibrationEffect.createWaveform(pattern,-1)
        vibrator.vibrate(effect,SoundRoute.attributes(c))
    }
    fun signal(event:String){if(store.bool("sound"))tone(store.str("tone_$event",Signals.defaultTone(event)));if(store.bool("haptic"))vibrate()}
    fun hasVibrator()=vibrator?.hasVibrator()==true
    fun amplitudeControl()=vibrator?.hasAmplitudeControl()==true
    fun release(){pending=null;pool.release()}
}

