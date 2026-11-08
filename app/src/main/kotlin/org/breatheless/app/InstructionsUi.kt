package org.breatheless.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.ui.*

@Composable
fun InstructionsScreen(onBack:()->Unit) {
    val context=LocalContext.current
    val paragraphs=remember{context.assets.open("instructions.md").bufferedReader().use{it.readText()}.split(Regex("\\n\\s*\\n")).filter{it.isNotBlank()}}
    PracticePage("Instructions",onBack){padding->PagedColumn(Modifier.padding(padding).fillMaxSize(),contentPadding=PaddingValues(20.dp)) {
        item{TextMMD(text="Source instructions from Adam Shand’s breathe-less and the course linked below.",style=MaterialTheme.typography.labelSmall)}
        item{TextMMD(text="CP is a personal tracking measure, not a direct CO₂ test or diagnosis. Maximum pauses are advanced practice. Stop if you feel dizzy or unwell, avoid fall hazards and water, and obtain qualified guidance where needed.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=12.dp))}
        paragraphs.forEach{paragraph->item{
            if(paragraph.startsWith("#")) {
                val heading=paragraph.trimStart('#',' ')
                if(heading!="Instructions")TextMMD(text=heading,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp,bottom=6.dp))
            } else {
                val body=paragraph.lines().joinToString("\n"){line->if(line.trimStart().startsWith("- "))"  ".repeat((line.length-line.trimStart().length)/2)+"• "+line.trimStart().removePrefix("- ") else line}
                val annotated=buildAnnotatedString {
                    var cursor=0
                    for(match in Regex("\\[([^]]+)]\\((https://[^ )]+)\\)").findAll(body)) {
                        append(body.substring(cursor,match.range.first));val url=match.groupValues[2]
                        withLink(LinkAnnotation.Clickable(url,TextLinkStyles(style=SpanStyle(color=Color.Black,textDecoration=TextDecoration.Underline))) {
                            runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}.onFailure{Toast.makeText(context,"The phone could not open this link.",Toast.LENGTH_SHORT).show()}
                        }){append(match.groupValues[1])}
                        cursor=match.range.last+1
                    }
                    append(body.substring(cursor))
                }
                Text(text=annotated,style=MaterialTheme.typography.bodySmall,color=Color.Black,modifier=Modifier.padding(bottom=12.dp))
            }
        }}
    }}
}
