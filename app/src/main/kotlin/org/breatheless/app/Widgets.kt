package org.breatheless.app
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.View
import kotlin.math.*
object P {
    val bg=Color.BLACK;val panel=Color.BLACK;val text=Color.rgb(196,206,198);val muted=Color.rgb(132,148,138);val accent=Color.rgb(140,167,149);val line=Color.rgb(31,43,35);val button=Color.rgb(14,23,17);val danger=Color.rgb(194,144,129)
    fun shape(color:Int,radius:Float=18f,stroke:Int?=null)=GradientDrawable().apply{setColor(color);cornerRadius=radius;if(stroke!=null)setStroke(1,stroke)}
}
class Dial(c:Context):View(c){private val p=Paint(Paint.ANTI_ALIAS_FLAG);var progress=0f;set(v){field=v.coerceIn(0f,1f);invalidate()};override fun onDraw(canvas:Canvas){val d=resources.displayMetrics.density;val r=min(width,height)*.43f;val cx=width/2f;val cy=height/2f;p.style=Paint.Style.STROKE;p.strokeWidth=2*d;p.color=P.line;canvas.drawCircle(cx,cy,r,p);p.color=P.accent;p.strokeCap=Paint.Cap.ROUND;canvas.drawArc(RectF(cx-r,cy-r,cx+r,cy+r),-90f,progress*360,false,p)}}
class Trend(c:Context,private val points:List<DayPoint>):View(c){private val p=Paint(Paint.ANTI_ALIAS_FLAG);init{contentDescription=if(points.isEmpty())"No CP measurements"else"CP daily averages, ${points.size} days, latest ${"%.1f".format(points.last().value)} seconds"};override fun onDraw(canvas:Canvas){val d=resources.displayMetrics.density;val left=38*d;val right=width-12*d;val top=20*d;val bottom=height-30*d;val high=if(points.isEmpty())40.0 else max(10.0,ceil((points.maxOf{it.value}+2)/10)*10);p.textSize=11*resources.displayMetrics.scaledDensity;for(i in 0..3){val y=bottom-(bottom-top)*i/3;p.color=P.line;canvas.drawLine(left,y,right,y,p);p.color=P.muted;canvas.drawText("${(high*i/3).roundToInt()}s",0f,y+4*d,p)};if(points.isEmpty()){p.textAlign=Paint.Align.CENTER;p.color=P.muted;canvas.drawText("Record a CP to begin.",(left+right)/2,(bottom+top)/2,p);p.textAlign=Paint.Align.LEFT;return};val first=points.first().day;val span=max(1,points.last().day-first)
        fun x(i:Int)=if(points.size==1)(left+right)/2 else left+(right-left)*(points[i].day-first).toFloat()/span
        fun y(i:Int)=bottom-(bottom-top)*(points[i].value/high).toFloat()
        val path=Path();points.indices.forEach{if(it==0)path.moveTo(x(it),y(it))else path.lineTo(x(it),y(it))};p.style=Paint.Style.STROKE;p.strokeWidth=2*d;p.color=P.accent;canvas.drawPath(path,p);p.style=Paint.Style.FILL;points.indices.forEach{canvas.drawCircle(x(it),y(it),3*d,p)};val fmt=java.text.SimpleDateFormat("d MMM",java.util.Locale.getDefault()).apply{timeZone=java.util.TimeZone.getTimeZone("UTC")};p.color=P.muted;canvas.drawText(fmt.format(java.util.Date(first*86400000L)),left,bottom+23*d,p);if(points.size>1){p.textAlign=Paint.Align.RIGHT;canvas.drawText(fmt.format(java.util.Date(points.last().day*86400000L)),right,bottom+23*d,p);p.textAlign=Paint.Align.LEFT}}}
