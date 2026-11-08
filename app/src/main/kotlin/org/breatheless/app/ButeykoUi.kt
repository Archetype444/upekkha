package org.breatheless.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.nikka.sit.Breathing
import com.wanderwildwood.nikka.sit.Sitting
import com.wanderwildwood.nikka.ui.*
import kotlinx.coroutines.delay
import java.util.Locale

object ButeykoActions {
    fun start(context:Context,protocol:Protocol,pulse:Int?=null) {
        check(PracticeService.live==null && Sitting.running.value==null && Breathing.running.value==null){"A practice is already running."}
        check(Store.get(context).error==null){Store.get(context).error ?: "Stored data is unavailable."}
        Codec.readProtocol(Codec.protocol(protocol));PracticeService.failure=null
        context.startForegroundService(Intent(context,PracticeService::class.java).setAction("START").putExtra("protocol",Codec.protocol(protocol).toString()).putExtra("pulse",pulse ?:0))
    }
}
fun secondsLabel(value:Int)=Durations.label(value)
fun cpLabel(value:Double)=String.format(Locale.getDefault(),"%.1f s",value)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticePage(title:String,onBack:(()->Unit)?=null,content:@Composable (PaddingValues)->Unit) {
    if(onBack!=null)BackHandler(onBack=onBack)
    Scaffold(containerColor=MaterialTheme.colorScheme.surface,topBar={TopAppBarMMD(title={TextMMD(text=title)},navigationIcon={if(onBack!=null)BarButton(Icons.Back,"Back",onBack)})},content=content)
}

@Composable
fun ButeykoTab() {
    val context=LocalContext.current;val store=remember{Store.get(context)}
    val version by store.version.collectAsState()
    val templates=remember(version){store.protocols.toList()}
    var page by remember{mutableStateOf("home")};var editing by remember{mutableStateOf<String?>(null)}
    var pending by remember{mutableStateOf<Protocol?>(null)};var pulse by remember{mutableStateOf(false)};var problem by remember{mutableStateOf<String?>(null)}
    fun launch(p:Protocol,bpm:Int?=null){runCatching{ButeykoActions.start(context,p,bpm)}.onFailure{problem=it.message}}
    fun request(p:Protocol){pending=p;if(p.id!="morning" && p.phases.none{it.kind==Kind.PULSE} && store.bool("pulsePrompt",false))pulse=true else launch(p)}
    BackHandler(enabled=page!="home"){page="home";editing=null}
    when(page) {
        "editor" -> FreestyleEditor(editing,onBack={page="home";editing=null},onStart=::request,onWalking={page="walking"})
        "walking" -> WalkingScreen{page="editor"}
        else -> PagedColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=20.dp)) {
            Presets.all.forEach{protocol->item{SettingRow(protocol.name,null){request(protocol)};DottedRule(start=0.dp)}}
            item{SettingRow("Freestyle","Arrange a practice"){editing=null;page="editor"}}
            if(templates.isNotEmpty())item{Heading("Saved practices")}
            templates.forEach{protocol->item{
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)){SettingRow(protocol.name,null){request(protocol)}}
                    HouseButton("Edit",{editing=protocol.id;page="editor"},Modifier.padding(top=8.dp),compact=true)
                }
                DottedRule(start=0.dp)
            }}
            store.error?.let{item{TextMMD(text=it)}}
        }
    }
    if(pulse)PulseDialog(onDismiss={pulse=false}){bpm->pulse=false;pending?.let{launch(it,bpm)}}
    problem?.let{ErrorDialog(it){problem=null}}
}

@Composable
fun FreestyleEditor(templateId:String?,onBack:()->Unit,onStart:(Protocol)->Unit,onWalking:()->Unit) {
    val store=Store.get(LocalContext.current)
    var protocol by remember(templateId){mutableStateOf(templateId?.let{id->store.protocols.find{it.id==id}} ?:store.freestylePlan())}
    var step by remember{mutableStateOf<Int?>(null)};var duration by remember{mutableStateOf<Int?>(null)};var add by remember{mutableStateOf(false)};var rounds by remember{mutableStateOf(false)}
    var naming by remember{mutableStateOf<String?>(null)};var deleting by remember{mutableStateOf(false)};var problem by remember{mutableStateOf<String?>(null)};var undo by remember{mutableStateOf<Protocol?>(null)}
    fun change(next:Protocol,rememberUndo:Boolean=true) {
        runCatching {
            Codec.readProtocol(Codec.protocol(next))
            if(templateId==null)store.saveFreestyle(next)else store.saveTemplate(next)
            if(rememberUndo)undo=protocol;protocol=next
        }.onFailure{problem=it.message ?:"Check the length of this practice."}
    }
    PagedColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=20.dp)) {
        item{SettingRow("Back",null,onBack)}
        item{Heading(if(templateId==null)"Freestyle"else protocol.name)}
        item{DialogButton("Start practice",true){onStart(protocol)}}
        item{SettingRow("Rounds",protocol.rounds.toString()){rounds=true};TextMMD(text="Each round repeats the steps below.",style=MaterialTheme.typography.labelSmall)}
        protocol.phases.forEachIndexed{index,phase->item{
            val name=if(index==0 && phase.kind==Kind.REST)"Initial rest"else phase.title
            SettingRow("${index+1}. $name",if(phase.kind in holdKinds)"Measured by you"else Durations.label(phase.seconds)){step=index}
            DottedRule(start=0.dp)
        }}
        item{DialogButton("Add a step"){add=true}}
        if(undo!=null)item{DialogButton("Undo last change"){val previous=undo!!;change(previous,false);undo=null}}
        item{DialogButton("Save as a new practice"){naming="new"}}
        if(templateId!=null){item{SettingRow("Rename practice",null){naming="rename"}};item{SettingRow("Delete practice",null){deleting=true}}}
        item{SettingRow("Walking exercise","Count steps during gentle holds",onWalking)}
    }
    if(rounds)OptionsDialog("Rounds",(1..12).map{it to it.toString()},protocol.rounds,{change(protocol.copy(rounds=it))},{rounds=false})
    if(add)OptionsDialog("Add a step",Kind.entries.map{it to Phase(it).title},Kind.REST,{kind->change(protocol.copy(phases=protocol.phases+Phase(kind,Durations.default(kind))))},{add=false})
    val index=step
    if(index!=null && index in protocol.phases.indices) {
        val phase=protocol.phases[index]
        EInkDialog(onDismiss={step=null}) {
            TextMMD(text=phase.title,fontWeight=FontWeight.Bold)
            if(phase.kind !in holdKinds && phase.kind!=Kind.LISTEN)SettingRow("Duration",Durations.label(phase.seconds)){duration=index}
            if(phase.kind in holdKinds)TextMMD(text="Start and stop this measurement yourself.",style=MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                HouseButton("Move up",{val list=protocol.phases.toMutableList();val removed=list.removeAt(index);list.add(index-1,removed);change(protocol.copy(phases=list));step=index-1},Modifier.weight(1f),enabled=index>0,compact=true)
                HouseButton("Move down",{val list=protocol.phases.toMutableList();val removed=list.removeAt(index);list.add(index+1,removed);change(protocol.copy(phases=list));step=index+1},Modifier.weight(1f),enabled=index<protocol.phases.lastIndex,compact=true)
            }
            DialogButton("Remove step"){if(protocol.phases.size==1)problem="Keep at least one step in the practice."else{change(protocol.copy(phases=protocol.phases.filterIndexed{i,_->i!=index}));step=null}}
            DialogButton("Done"){step=null}
        }
    }
    duration?.let{n->if(n in protocol.phases.indices)DurationDialog("Duration",protocol.phases[n].seconds,Durations.forKind(protocol.phases[n].kind),{duration=null}){seconds->val list=protocol.phases.toMutableList();list[n]=list[n].copy(seconds=seconds);change(protocol.copy(phases=list));duration=null}}
    naming?.let{mode->NameDialog(if(mode=="rename")"Rename practice"else"Save practice",if(mode=="rename")protocol.name else "",{naming=null}){name->
        if(mode=="rename")change(protocol.copy(name=name))else runCatching{store.saveTemplate(protocol.copy(id="custom-"+Store.id(),name=name,custom=true))}.onFailure{problem=it.message}
        naming=null
    }}
    if(deleting)EInkDialog(onDismiss={deleting=false}){TextMMD(text="Delete ${protocol.name} from saved practices?");DialogButtons("Cancel","Delete",{deleting=false},{runCatching{store.deleteTemplate(protocol.id);deleting=false;onBack()}.onFailure{problem=it.message}})}
    problem?.let{ErrorDialog(it){problem=null}}
}
@Composable
fun NameDialog(title:String,initial:String,onDismiss:()->Unit,onSave:(String)->Unit) {
    var value by remember{mutableStateOf(initial)};var error by remember{mutableStateOf(false)}
    EInkDialog(onDismiss=onDismiss){Field(title,value,{value=it.take(100)});if(error)TextMMD(text="Enter a name.");DialogButtons("Cancel","Save",onDismiss,{if(value.isBlank())error=true else onSave(value.trim())})}
}

@Composable
fun ButeykoSessionScreen() {
    val state by PracticeService.snapshot.collectAsState()
    val session=state
    val service=PracticeService.live
    if(session==null || service==null){TextMMD(text="Starting practice…");return}
    val context=LocalContext.current;val store=remember{Store.get(context)}
    var quit by remember{mutableStateOf(false)}
    BackHandler{if(!session.ending)quit=true}
    DisposableEffect(session.state){val window=(context as? Activity)?.window;if(session.state in listOf(State.CP_RUNNING,State.HOLD_RUNNING,State.PULSE_RUNNING))window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);onDispose{window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
    // Only immutable snapshot values enter lazy content. A mutable Engine must never be captured here.
    PracticePage(session.protocolName){padding->
        PagedColumn(Modifier.padding(padding).fillMaxSize(),contentPadding=PaddingValues(20.dp)) {
            item{TextMMD(text=session.phase.title,style=MaterialTheme.typography.titleLarge)}
            item{TextMMD(text="${session.index+1} of ${session.count}",style=MaterialTheme.typography.labelSmall)}
            item{
                val face=when {
                    session.ending -> "Practice saved"
                    session.state in listOf(State.CP_READY,State.HOLD_READY,State.PULSE_READY) -> "Ready"
                    session.state in listOf(State.CP_RUNNING,State.HOLD_RUNNING) -> if(store.bool("hideCp"))"Measuring"else cpLabel(session.pauseSeconds)
                    session.state in listOf(State.CP_REVIEW,State.HOLD_REVIEW) -> cpLabel(session.pendingPause)
                    session.state==State.LISTENING -> "Playing"
                    session.state in listOf(State.TIMED,State.PULSE_RUNNING) -> PracticeService.clock(session.remainingSeconds)
                    session.state==State.PULSE_REVIEW -> "${session.pulseBeats*60/session.phase.seconds} bpm"
                    session.state==State.PAUSED -> "Interrupted"
                    else -> "Finished"
                }
                TextMMD(text=face,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(vertical=16.dp))
            }
            if(!session.ending) {
                item{TextMMD(text=session.phase.guidance,style=MaterialTheme.typography.bodySmall)}
                if(session.note.isNotBlank())item{TextMMD(text=session.note,modifier=Modifier.padding(vertical=10.dp))}
                when(session.state) {
                    State.CP_READY,State.HOLD_READY -> item{DialogButton(if(session.phase.kind==Kind.CP)"Start CP"else"Start pause",true){service.startCp()}}
                    State.CP_RUNNING,State.HOLD_RUNNING -> item{DialogButton(if(session.phase.kind==Kind.CP)"Stop CP"else"Stop pause",true){service.stopCp()}}
                    State.CP_REVIEW,State.HOLD_REVIEW -> {
                        item{TextMMD(text=if(session.interrupted)"This interrupted value will be excluded."else if(session.phase.kind==Kind.CP)"Keep the value only if the next breath felt easy and natural."else"Record this pause separately from your CP measurements.",style=MaterialTheme.typography.bodySmall)}
                        if(!session.interrupted)item{DialogButton("Keep measurement",true){service.review(true)}}
                        item{DialogButton(if(session.interrupted)"Continue"else"Exclude measurement"){service.review(false)}}
                    }
                    State.PULSE_READY -> {item{DialogButton("Start ${session.phase.seconds}-second count",true){service.startPulse()}};item{DialogButton("Skip pulse"){service.recordPulse(true)}}}
                    State.PULSE_RUNNING -> item{DialogButton("Beat · ${session.pulseBeats}",true){service.pulseBeats(session.pulseBeats+1)}}
                    State.PULSE_REVIEW -> {
                        item{NumberStepper("Beats counted",session.pulseBeats,{service.pulseBeats(it)},0..250)}
                        item{DialogButton("Record pulse",true){service.recordPulse()}}
                        item{DialogButton("Skip pulse"){service.recordPulse(true)}}
                    }
                    State.PAUSED -> item{DialogButton(if(session.phase.kind==Kind.LISTEN)"Resume recording"else"Continue practice",true){service.pause()}}
                    State.FINISHED -> item{DialogButton("Retry saving",true){service.retrySave()}}
                    else -> Unit
                }
                item{Spacer(Modifier.height(12.dp));DialogButton("Quit session"){quit=true}}
            }
        }
    }
    if(quit)EInkDialog(onDismiss={quit=false}){TextMMD(text="Quit this practice? Its measurements will be discarded.");DialogButtons("Continue","Quit",{quit=false},{quit=false;service.discard()})}
}

@Composable
fun ButeykoEndScreen(id:String) {
    val store=Store.get(LocalContext.current);val version by store.version.collectAsState();val entry=remember(id,version){store.entries.find{it.id==id}}
    var pulse by remember(id){mutableStateOf<Int?>(null)};var symptom by remember(id){mutableStateOf(entry?.symptom)};var counter by remember{mutableStateOf(false)};var score by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    fun save(){runCatching{if(entry!=null)store.upsert(entry.copy(pulses=entry.pulses+listOfNotNull(pulse?.let{PulseValue(it,System.currentTimeMillis(),"After practice")}),symptom=symptom));PracticeService.lastResult=null;PracticeService.result.value=null}.onFailure{error=it.message}}
    BackHandler{save()}
    PracticePage("Practice saved"){padding->PagedColumn(Modifier.padding(padding).fillMaxSize(),contentPadding=PaddingValues(20.dp)) {
        item{TextMMD(text=entry?.name ?:"Practice",style=MaterialTheme.typography.titleLarge)}
        entry?.cps?.forEach{cp->item{SettingRow(cp.label,cpLabel(cp.seconds)+if(cp.valid)""else" · excluded",null)}}
        entry?.holds?.forEach{hold->item{SettingRow(hold.label,cpLabel(hold.seconds)+if(hold.valid)""else" · excluded",null)}}
        if(store.bool("pulsePrompt",false) && entry?.pulses?.none{it.label=="After practice"}!=false)item{SettingRow("Pulse after practice",pulse?.let{"$it bpm"} ?:"Optional"){counter=true}}
        item{SettingRow("Symptom score",symptom?.let{"$it / 10"} ?:"Optional"){score=true}}
        item{TextMMD(text="0 means no symptoms; 10 means the worst symptoms for you.",style=MaterialTheme.typography.labelSmall)}
        error?.let{item{TextMMD(text=it)}}
        item{DialogButton("Save and close",true,::save)}
    }}
    if(counter)PulseCounter({counter=false}){pulse=it;counter=false}
    if(score)OptionsDialog("Symptom score",listOf<Int?>(null).map{it to "No score"}+(0..10).map{it to "$it / 10"},symptom,{symptom=it},{score=false})
}
@Composable
fun PulseDialog(onDismiss:()->Unit,onDone:(Int?)->Unit) {
    var counter by remember{mutableStateOf(false)};var bpm by remember{mutableStateOf<Int?>(null)};var direct by remember{mutableStateOf(false)}
    EInkDialog(onDismiss=onDismiss){TextMMD(text="Pulse before practice");SettingRow("Pulse",bpm?.let{"$it bpm"} ?:"Optional"){direct=true};DialogButton("Count pulse"){counter=true};DialogButtons("Skip","Begin",{onDone(null)},{onDone(bpm)})}
    if(counter)PulseCounter({counter=false}){bpm=it;counter=false}
    if(direct)NumberDialog("Pulse (bpm)",bpm ?:60,20..250,{direct=false}){bpm=it;direct=false}
}
@Composable
fun PulseCounter(onDismiss:()->Unit,onDone:(Int)->Unit) {
    var duration by remember{mutableIntStateOf(15)};var began by remember{mutableLongStateOf(0)};var beats by remember{mutableIntStateOf(0)};var left by remember{mutableIntStateOf(15)}
    val pulseContext=LocalContext.current
    val feedback=remember{Feedback(pulseContext)}
    DisposableEffect(feedback){onDispose{feedback.release()}}
    LaunchedEffect(began){if(began>0)while(left>0){delay(200);val remaining=(duration-(SystemClock.elapsedRealtime()-began)/1000).toInt().coerceAtLeast(0);if(remaining==0 && left>0)feedback.signal("PULSE_END");left=remaining}}
    EInkDialog(onDismiss=onDismiss) {
        TextMMD(text="Count pulse",fontWeight=FontWeight.Bold)
        if(began==0L){HouseChoiceRow(listOf("15 sec","30 sec","60 sec"),listOf(15,30,60).indexOf(duration),{duration=listOf(15,30,60)[it];left=duration},compact=true);TextMMD(text="Tap for each beat, or count mentally and adjust the total at the end.",style=MaterialTheme.typography.bodySmall);DialogButton("Start count",true){began=SystemClock.elapsedRealtime();feedback.signal("PULSE")}}
        else if(left>0){TextMMD(text="$left seconds",style=MaterialTheme.typography.headlineMedium);DialogButton("Beat · $beats",true){beats++}}
        else{NumberStepper("Beats counted",beats,{beats=it},0..250);val bpm=beats*60/duration;TextMMD(text="$bpm bpm${if(duration==15)" · rough estimate"else""}");HouseButton("Use pulse",{onDone(bpm)},Modifier.fillMaxWidth(),on=true,enabled=bpm in 20..250)}
        DialogButton("Close",onClick=onDismiss)
    }
}
@Composable
fun ErrorDialog(message:String,onDismiss:()->Unit){EInkDialog(onDismiss=onDismiss){TextMMD(text=message);DialogButton("Close",onClick=onDismiss)}}
