package com.chordz.eprachar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import com.chordz.eprachar.preferences.AppPreferences
import com.chordz.eprachar.preferences.AppPreferences.getBooleanValueFromSharedPreferences
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EndCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                Log.d("EndCallReceiver", "Phone state: $phoneState, Number: $incomingNumber")
                
                // Only trigger on RINGING state - this covers incoming calls and missed calls
                if (phoneState == TelephonyManager.EXTRA_STATE_RINGING && !incomingNumber.isNullOrEmpty()) {
                    Log.d("EndCallReceiver", "Call ringing: $incomingNumber")
                    handleCallEnded(context, incomingNumber, "missed")
                }
            }
            
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                if (!phoneNumber.isNullOrEmpty()) {
                    Log.d("EndCallReceiver", "Outgoing call: $phoneNumber")
                    // For outgoing calls, also trigger on ringing (if supported) or just trigger immediately
                    // Since outgoing calls don't have RINGING state, we trigger immediately
                    handleCallEnded(context, phoneNumber, "outgoing")
                }
            }
        }
    }
    
    private fun handleCallEnded(context: Context, phoneNumber: String, callStatus: String = "missed") {
        Log.d("EndCallReceiver", "Handling call: $phoneNumber, Status: $callStatus")
        
        // Send SMS if enabled
        if (AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.SMS_ON_OFF)) {
            sendSMS(context, phoneNumber)
        }
        
        // Send webhook if AI WhatsApp toggle is enabled
        if (AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.WHATSAPP_ON_OFF)) {
            sendWebhookForAI(context, phoneNumber, callStatus)
        }
    }

    private fun sendSMS(context: Context, phoneNumber: String) {
        try {
            val msgDetails = AppPreferences.getMsgDetails(context)
            val message = msgDetails?.data?.getOrNull(0)?.aMessage ?: ""
            if (message.isEmpty()) {
                Log.w("EndCallReceiver", "No message available for SMS")
                return
            }
            
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.d("EndCallReceiver", "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("EndCallReceiver", "Failed to send SMS", e)
        }
    }

    private fun sendWebhookForAI(context: Context, phoneNumber: String, callStatus: String) {
        // Fetch msgDetails from preferences
        val msgDetails = AppPreferences.getMsgDetails(context)
        if (msgDetails == null) {
            Log.w("EndCallReceiver", "No msgDetails available for webhook")
            return
        }
        
        // Fetch admin code (user_id), message, media_url, timestamp
        val userId = AppPreferences.getLongValueFromSharedPreferences(AppPreferences.ADMIN_NUMBER).toString()
        val message: String = msgDetails.data?.getOrNull(0)?.aMessage ?: ""
        val mediaUrl: String = msgDetails.data?.getOrNull(0)?.aImage ?: ""
        val isoDate: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { 
            timeZone = TimeZone.getTimeZone("UTC") 
        }.format(Date())
        
        val json = JSONObject().apply {
            put("user_id", userId)
            put("caller_number", phoneNumber)
            put("call_status", callStatus) // Use actual call status: "incoming", "missed", "outgoing", or "ended"
            put("timestamp", isoDate)
            put("message", message)
            put("media_url", mediaUrl)
        }
        
        val url = "https://nonrecurently-diverse-deedee.ngrok-free.dev/webhook/eprachar"
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json.toString())
        val req = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body)
            .build()
            
        client.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EndCallReceiver", "Webhook failed for $phoneNumber", e)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d("EndCallReceiver", "Webhook response: ${it.code} for $phoneNumber")
                }
            }
        })
    }
}