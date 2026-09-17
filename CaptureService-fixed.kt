package com.example.a1bot

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.concurrent.atomic.AtomicReference

object BotState {
    @Volatile var enabled = false
    @Volatile var frameCount = 0L
    val frame = AtomicReference<Bitmap?>(null)
}

class CaptureService : Service() {
    private var reader: ImageReader? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseCapture()
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startCaptureForeground()

        val resultCode =
            intent?.getIntExtra("code", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY

        @Suppress("DEPRECATION")
        val resultData: Intent? =
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra("data", Intent::class.java)
            } else {
                intent.getParcelableExtra("data")
            }

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val manager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            projection = manager.getMediaProjection(resultCode, resultData)

            // Required on Android 14+ before createVirtualDisplay().
            projection?.registerCallback(projectionCallback, mainHandler)

            val dm = resources.displayMetrics
            val width = dm.widthPixels
            val height = dm.heightPixels

            reader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

            virtualDisplay = projection?.createVirtualDisplay(
                "A1Bot",
                width,
                height,
                dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader!!.surface,
                null,
                mainHandler
            )

            reader?.setOnImageAvailableListener({ r ->
                val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val plane = image.planes[0]
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val bitmapWidth = width + rowPadding / pixelStride

                    val padded =
                        Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                    padded.copyPixelsFromBuffer(plane.buffer)

                    val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
                    padded.recycle()

                    BotState.frame.getAndSet(cropped)?.recycle()
                    BotState.frameCount++
                } finally {
                    image.close()
                }
            }, mainHandler)

        } catch (t: Throwable) {
            t.printStackTrace()
            releaseCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startCaptureForeground() {
        val channelId = "capture"

        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "A1 Bot capture",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val notification =
            if (Build.VERSION.SDK_INT >= 26) {
                Notification.Builder(this, channelId)
                    .setContentTitle("A1 Bot")
                    .setContentText("Screen capture active")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setOngoing(true)
                    .build()
            } else {
                Notification.Builder(this)
                    .setContentTitle("A1 Bot")
                    .setContentText("Screen capture active")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setOngoing(true)
                    .build()
            }

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(1, notification)
        }
    }

    private fun releaseCapture() {
        reader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        virtualDisplay = null

        reader?.close()
        reader = null

        projection?.unregisterCallback(projectionCallback)
        projection = null

        BotState.frame.getAndSet(null)?.recycle()
    }

    override fun onDestroy() {
        releaseCapture()
        super.onDestroy()
    }
}
