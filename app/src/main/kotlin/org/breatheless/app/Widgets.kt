package org.breatheless.app
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.View
import kotlin.math.*
object P {
    val bg=Color.WHITE;val panel=Color.WHITE;val text=Color.BLACK;val muted=Color.BLACK;val accent=Color.BLACK;val line=Color.BLACK;val button=Color.WHITE;val danger=Color.BLACK
    fun shape(color:Int,radius:Float=18f,stroke:Int?=null)=GradientDrawable().apply{setColor(color);cornerRadius=radius;if(stroke!=null)setStroke(1,stroke)}
}
class Trend(c:Context,private val points:List<DayPoint>):View(c){private val p=Paint(Paint.ANTI_ALIAS_FLAG);init{contentDescription=if(points.isEmpty())"No CP measurements"else"CP daily averages, ${points.size} days, latest ${"%.1f".format(points.last().value)} seconds"};override fun onDraw(canvas:Canvas){val d=resources.displayMetrics.density;val left=38*d;val right=width-12*d;val top=20*d;val bottom=height-30*d;val high=if(points.isEmpty())40.0 else max(10.0,ceil((points.maxOf{it.value}+2)/10)*10);p.textSize=11*resources.displayMetrics.scaledDensity;for(i in 0..3){val y=bottom-(bottom-top)*i/3;p.color=P.line;canvas.drawLine(left,y,right,y,p);p.color=P.muted;canvas.drawText("${(high*i/3).roundToInt()}s",0f,y+4*d,p)};if(points.isEmpty()){p.textAlign=Paint.Align.CENTER;p.color=P.muted;canvas.drawText("Record a CP to begin.",(left+right)/2,(bottom+top)/2,p);p.textAlign=Paint.Align.LEFT;return};val first=points.first().day;val span=max(1,points.last().day-first)
        fun x(i:Int)=if(points.size==1)(left+right)/2 else left+(right-left)*(points[i].day-first).toFloat()/span
        fun y(i:Int)=bottom-(bottom-top)*(points[i].value/high).toFloat()
        val path=Path();points.indices.forEach{if(it==0)path.moveTo(x(it),y(it))else path.lineTo(x(it),y(it))};p.style=Paint.Style.STROKE;p.strokeWidth=2*d;p.color=P.accent;canvas.drawPath(path,p);p.style=Paint.Style.FILL;points.indices.forEach{canvas.drawCircle(x(it),y(it),3*d,p)};val fmt=java.text.SimpleDateFormat("d MMM",java.util.Locale.getDefault()).apply{timeZone=java.util.TimeZone.getTimeZone("UTC")};p.color=P.muted;canvas.drawText(fmt.format(java.util.Date(first*86400000L)),left,bottom+23*d,p);if(points.size>1){p.textAlign=Paint.Align.RIGHT;canvas.drawText(fmt.format(java.util.Date(points.last().day*86400000L)),right,bottom+23*d,p);p.textAlign=Paint.Align.LEFT}}}

