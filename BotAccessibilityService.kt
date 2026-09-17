package com.example.a1bot

import android.accessibilityservice.*
import android.graphics.*
import android.os.*
import android.view.accessibility.AccessibilityEvent

class BotAccessibilityService: AccessibilityService() {
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var templates:List<Bitmap>
    private var busy=false
    private val loop=object:Runnable { override fun run(){ if(BotState.enabled && !busy) step(); handler.postDelayed(this,45) } }
    override fun onServiceConnected(){
        templates=listOf(R.drawable.tile_wifi,R.drawable.tile_orange,R.drawable.tile_tower,R.drawable.tile_green,R.drawable.tile_phone).map{ BitmapFactory.decodeResource(resources,it) }
        handler.post(loop)
    }
    override fun onAccessibilityEvent(e:AccessibilityEvent?){ }
    override fun onInterrupt(){ }

    private fun step(){
        val bm=BotState.frame.get()?:return
        val board=classifyBoard(bm) ?: return
        val move=bestMove(board) ?: return
        busy=true
        val (a,b)=move; val p1=center(bm.width,bm.height,a.first,a.second); val p2=center(bm.width,bm.height,b.first,b.second)
        val path=Path().apply{moveTo(p1.x,p1.y);lineTo(p2.x,p2.y)}
        val g=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,38)).build()
        dispatchGesture(g,object:GestureResultCallback(){ override fun onCompleted(g:GestureDescription?){ handler.postDelayed({busy=false},125) }; override fun onCancelled(g:GestureDescription?){busy=false} },null)
    }

    // Calibrated from the supplied 691×1536 screenshot. Coordinates scale with capture size.
    private fun center(w:Int,h:Int,r:Int,c:Int):PointF {
        val x=(108f + 79f*c) * w/691f
        val y=(638f + 80f*r) * h/1536f
        return PointF(x,y)
    }
    private fun classifyBoard(b:Bitmap):Array<IntArray>? {
        val out=Array(7){IntArray(7)}
        for(r in 0..6) for(c in 0..6){ val p=center(b.width,b.height,r,c); out[r][c]=classify(b,p) }
        return out
    }
    private fun classify(b:Bitmap,p:PointF):Int {
        val half=(29f*b.width/691f).toInt().coerceAtLeast(18)
        val l=(p.x-half).toInt().coerceIn(0,b.width-2); val t=(p.y-half).toInt().coerceIn(0,b.height-2)
        val sz=(half*2).coerceAtMost(minOf(b.width-l,b.height-t))
        val crop=Bitmap.createBitmap(b,l,t,sz,sz)
        val small=Bitmap.createScaledBitmap(crop,24,24,true); crop.recycle()
        var best=0; var bestD=Double.MAX_VALUE
        templates.forEachIndexed { i,tmp ->
            val tt=Bitmap.createScaledBitmap(tmp,24,24,true); var d=0.0
            for(y in 2 until 22 step 2) for(x in 2 until 22 step 2){ val a=small.getPixel(x,y); val q=tt.getPixel(x,y); val dr=Color.red(a)-Color.red(q); val dg=Color.green(a)-Color.green(q); val db=Color.blue(a)-Color.blue(q); d+=(dr*dr+dg*dg+db*db).toDouble() }
            tt.recycle(); if(d<bestD){bestD=d;best=i}
        }
        small.recycle(); return best
    }
    private fun bestMove(src:Array<IntArray>):Pair<Pair<Int,Int>,Pair<Int,Int>>? {
        var bestScore=0; var best:Pair<Pair<Int,Int>,Pair<Int,Int>>?=null
        for(r in 0..6) for(c in 0..6) for((dr,dc) in arrayOf(0 to 1,1 to 0)){
            val rr=r+dr; val cc=c+dc; if(rr>6||cc>6||src[r][c]==src[rr][cc]) continue
            val b=Array(7){src[it].clone()}; val z=b[r][c]; b[r][c]=b[rr][cc]; b[rr][cc]=z
            val score=matchedCells(b)
            if(score>bestScore){bestScore=score;best=(r to c) to (rr to cc)}
        }
        return best
    }
    private fun matchedCells(b:Array<IntArray>):Int {
        val hit=Array(7){BooleanArray(7)}
        for(r in 0..6){ var s=0; while(s<7){var e=s+1;while(e<7&&b[r][e]==b[r][s])e++;if(e-s>=3)for(c in s until e)hit[r][c]=true;s=e} }
        for(c in 0..6){ var s=0; while(s<7){var e=s+1;while(e<7&&b[e][c]==b[s][c])e++;if(e-s>=3)for(r in s until e)hit[r][c]=true;s=e} }
        return hit.sumOf{row->row.count{it}}
    }
}
