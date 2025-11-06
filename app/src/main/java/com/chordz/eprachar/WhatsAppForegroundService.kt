package com.chordz.eprachar

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chordz.eprachar.preferences.AppPreferences
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WhatsAppForegroundService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "WhatsAppServiceChannel"
        private const val ACTION_SEND_WEBHOOK = "com.chordz.eprachar.SEND_WEBHOOK"
        
        // Intent extras
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CALL_STATUS = "call_status"
        
        fun startService(context: Context) {
            val intent = Intent(context, WhatsAppForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun sendWebhook(context: Context, phoneNumber: String, callStatus: String) {
            val intent = Intent(context, WhatsAppForegroundService::class.java).apply {
                action = ACTION_SEND_WEBHOOK
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_CALL_STATUS, callStatus)
            }
            context.startService(intent)
        }
    }
    
    private val okHttpClient = OkHttpClient()
    
    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d("WhatsAppService", "Service created and started in foreground")
        } catch (e: Exception) {
            Log.e("WhatsAppService", "Error starting service", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_SEND_WEBHOOK -> {
                    val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return START_STICKY
                    val callStatus = intent.getStringExtra(EXTRA_CALL_STATUS) ?: "missed"
                    
                    Log.d("WhatsAppService", "Received webhook task: $phoneNumber, status: $callStatus")
                    processWebhook(phoneNumber, callStatus)
                }
                else -> {
                    // Service started without action - just keep it running
                    Log.d("WhatsAppService", "Service started to keep running")
                }
            }
        } catch (e: Exception) {
            Log.e("WhatsAppService", "Error in onStartCommand", e)
        }
        
        return START_STICKY // Service will restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WhatsApp Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for sending WhatsApp messages via AI"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WhatsApp Service Running")
            .setContentText("Monitoring calls and sending messages")
            .setSmallIcon(R.mipmap.ic_elauncher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun processWebhook(phoneNumber: String, callStatus: String) {
        // Check if WhatsApp toggle is enabled
        if (!AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.WHATSAPP_ON_OFF)) {
            Log.d("WhatsAppService", "WhatsApp toggle is off, skipping webhook")
            return
        }
        
        // Fetch msgDetails from preferences
        val msgDetails = AppPreferences.getMsgDetails(this)
        if (msgDetails == null) {
            Log.w("WhatsAppService", "No msgDetails available for webhook")
            return
        }
        
        // Fetch admin code (user_id), message, media_url, timestamp
        val userId = AppPreferences.getLongValueFromSharedPreferences(AppPreferences.ADMIN_NUMBER).toString()
        val message: String = msgDetails.data?.getOrNull(0)?.aMessage ?: ""
        val mediaUrl: String = msgDetails.data?.getOrNull(0)?.aImage ?: ""
        val user = msgDetails.data?.getOrNull(0)?.client ?: "chordz"
        
        val isoDate: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        
        val json = JSONObject().apply {
            put("user_id", userId)
            put("caller_number", phoneNumber)
            put("call_status", callStatus)
            put("timestamp", isoDate)
            put("message", message)
            put("media_url", mediaUrl)
            put("client", user.lowercase())
        }
        
        val url = "https://nonrecurently-diverse-deedee.ngrok-free.dev/webhook/aimessage"
        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json.toString())
        val req = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        
        okHttpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WhatsAppService", "Webhook failed for $phoneNumber", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val success = it.isSuccessful
                    Log.d(
                        "WhatsAppService",
                        "Webhook response: ${it.code} for $phoneNumber, success: $success"
                    )
                }
            }
        })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("WhatsAppService", "Service destroyed")
        // Service will be restarted by START_STICKY if killed by system
    }
}

