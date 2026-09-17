package com.example.a1bot

import android.app.*
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val REQ=9001
    override fun onCreate(b: Bundle?) { super.onCreate(b)
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(40,40,40,40) }
        fun add(t:String, f:()->Unit) { box.addView(Button(this).apply { text=t; setOnClickListener{f()} }, LinearLayout.LayoutParams(-1,-2)) }
        box.addView(TextView(this).apply{text="A1 Osmosmerka Bot\n7×7 • adjacent swaps • 3+ straight"; textSize=22f; gravity=Gravity.CENTER; setPadding(0,0,0,40)})
        add("1. ALLOW SCREEN CAPTURE") { val m=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager; startActivityForResult(m.createScreenCaptureIntent(),REQ) }
        add("2. ENABLE ACCESSIBILITY BOT") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        add("START BOT") { BotState.enabled=true; Toast.makeText(this,"Armed — switch to A1",Toast.LENGTH_SHORT).show() }
        add("STOP BOT") { BotState.enabled=false; Toast.makeText(this,"Stopped",Toast.LENGTH_SHORT).show() }
        setContentView(box)
    }
    override fun onActivityResult(req:Int,res:Int,data:Intent?) { super.onActivityResult(req,res,data); if(req==REQ && res==RESULT_OK && data!=null) { startForegroundService(Intent(this,CaptureService::class.java).putExtra("code",res).putExtra("data",data)); Toast.makeText(this,"Capture active",Toast.LENGTH_SHORT).show() } }
}
