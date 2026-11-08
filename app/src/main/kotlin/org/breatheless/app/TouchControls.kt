package org.breatheless.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.ui.*

@Composable
fun NumberStepper(label:String,value:Int,onChange:(Int)->Unit,range:IntRange,step:Int=1) {
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
        TextMMD(text=label,style=MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            HouseButton("−",{onChange((value-step).coerceIn(range))},enabled=value>range.first)
            TextMMD(text=value.toString(),style=MaterialTheme.typography.headlineMedium)
            HouseButton("+",{onChange((value+step).coerceIn(range))},enabled=value<range.last)
        }
    }
}
@Composable
fun NumberDialog(title:String,initial:Int,range:IntRange,onDismiss:()->Unit,onSet:(Int)->Unit) {
    var value by remember { mutableIntStateOf(initial.coerceIn(range)) }
    EInkDialog(onDismiss=onDismiss) {
        NumberStepper(title,value,{value=it},range)
        if(range.last-range.first>30) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            HouseButton("−10",{value=(value-10).coerceIn(range)},Modifier.weight(1f))
            HouseButton("+10",{value=(value+10).coerceIn(range)},Modifier.weight(1f))
        }
        DialogButtons("Cancel","Set",onDismiss,{onSet(value)})
    }
}
@Composable
fun DurationDialog(title:String,current:Int,options:List<Int>,onDismiss:()->Unit,onSet:(Int)->Unit) {
    val choices=if(current in options)options else listOf(current)+options
    OptionsDialog(title,choices.distinct().map{it to Durations.label(it)},current,onSet,onDismiss)
}
