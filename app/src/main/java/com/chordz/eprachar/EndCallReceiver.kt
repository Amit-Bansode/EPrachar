package com.chordz.eprachar

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.chordz.eprachar.data.ElectionDataHolder.msgDetails
import com.chordz.eprachar.preferences.AppPreferences
import com.chordz.eprachar.preferences.AppPreferences.getBooleanValueFromSharedPreferences
import com.chordz.eprachar.preferences.AppPreferences.saveBooleanToSharedPreferences
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EndCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        /*if (!validate(context)) {
            return
        }*/
        val bundle = intent.extras
        val phoneNumber = bundle!!.getString("incoming_number").toString()

        val phoneStateString = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Log.e("TAG", "onReceive: $phoneNumber $phoneStateString")
        if (phoneNumber != null && !phoneNumber.isEmpty() && (phoneStateString!!.contains("OFFHOOK"))
        ) {
            onCallEnded(context, phoneNumber)
        }
    }

    private fun onCallEnded(context: Context, phoneNumber: String) {
        if (!AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.WHATSAPP_ON_OFF)||phoneNumber!=null) return
        // Fetch admin code (user_id), message, media_url, timestamp
        val userId = AppPreferences.getLongValueFromSharedPreferences(AppPreferences.ADMIN_NUMBER).toString()
        val message: String = msgDetails?.data?.getOrNull(0)?.aMessage ?: ""
        val mediaUrl: String = msgDetails?.data?.getOrNull(0)?.aImage ?: ""
        val isoDate: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val json = JSONObject().apply {
            put("user_id", userId)
            put("caller_number", phoneNumber)
            put("call_status", "missed") // Or update by call type
            put("timestamp", isoDate)
            put("message", message)
            put("media_url", mediaUrl)
        }
        sendWebhook(json)
    }

    private fun sendSMSMessage(phoneNumber: String, defaultMessage: String?) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, phoneNumber, defaultMessage, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openWhatsAppIntent(context: Context) {
        val intent = Intent()
        intent.setPackage("com.whatsapp")
        context.startActivity(intent)
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        // Implement your phone number formatting logic if needed
        return phoneNumber
    }

    private fun sendWebhook(payload: JSONObject) {
        val url = "https://distillable-omari-loathingly.ngrok-free.dev/webhook/eprachar"
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), payload.toString())
        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EPrachar", "Webhook failed", e)
            }
            override fun onResponse(call: Call, response: Response) {
                Log.i("EPrachar", "Webhook status: ${'$'}{response.code}")
            }
        })
    }

    internal interface OnPhoneStateReceived {
        fun onPhoneStateReceived(phoneNumber: String?)
    }

    companion object {
    }
}