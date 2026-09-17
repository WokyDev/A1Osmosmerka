package com.example.a1bot

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.*
import java.util.concurrent.atomic.AtomicReference

object BotState { @Volatile var enabled=false; val frame=AtomicReference<Bitmap?>(null) }

class CaptureService: Service() {
    private var reader:ImageReader?=null
    override fun onBind(i:Intent?)=null
    override fun onStartCommand(i:Intent?, flags:Int, id:Int):Int {
        val ch="capture"; if(Build.VERSION.SDK_INT>=26){ val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager; nm.createNotificationChannel(NotificationChannel(ch,"A1 Bot capture",NotificationManager.IMPORTANCE_LOW)) }
        val n=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,ch).setContentTitle("A1 Bot").setContentText("Reading puzzle board").setSmallIcon(android.R.drawable.ic_media_play).build() else Notification.Builder(this).setContentTitle("A1 Bot").setSmallIcon(android.R.drawable.ic_media_play).build()
        startForeground(1,n)
        val code=i?.getIntExtra("code",Activity.RESULT_CANCELED)?:return START_NOT_STICKY
        @Suppress("DEPRECATION") val data=if(Build.VERSION.SDK_INT>=33) i.getParcelableExtra("data",Intent::class.java) else i.getParcelableExtra("data")
        val mp=(getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code,data!!)
        val dm=resources.displayMetrics; val w=dm.widthPixels; val h=dm.heightPixels
        reader=ImageReader.newInstance(w,h,android.graphics.PixelFormat.RGBA_8888,2)
        mp.createVirtualDisplay("A1Bot",w,h,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,null)
        reader!!.setOnImageAvailableListener({ r ->
            val img=r.acquireLatestImage()?:return@setOnImageAvailableListener
            val p=img.planes[0]; val row=p.rowStride; val pix=p.pixelStride; val rw=row/pix
            val tmp=Bitmap.createBitmap(rw,h,Bitmap.Config.ARGB_8888); tmp.copyPixelsFromBuffer(p.buffer)
            BotState.frame.set(Bitmap.createBitmap(tmp,0,0,w,h)); tmp.recycle(); img.close()
        },Handler(Looper.getMainLooper()))
        return START_STICKY
    }
}
