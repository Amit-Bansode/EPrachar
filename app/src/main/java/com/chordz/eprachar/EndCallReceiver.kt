package com.chordz.eprachar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import com.chordz.eprachar.preferences.AppPreferences
import com.chordz.eprachar.preferences.AppPreferences.getBooleanValueFromSharedPreferences

class EndCallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val PREFS_NAME = "CallStatePrefs"
        private const val KEY_LAST_STATE = "last_phone_state"
        private const val KEY_LAST_NUMBER = "last_call_number"
        private const val KEY_IS_OUTGOING = "is_outgoing"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                Log.d("EndCallReceiver", "Phone state: $phoneState, Number: $incomingNumber")
                
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastState = prefs.getString(KEY_LAST_STATE, null)
                val lastNumber = prefs.getString(KEY_LAST_NUMBER, null)
                val isOutgoing = prefs.getBoolean(KEY_IS_OUTGOING, false)
                
                // Save current state
                if (phoneState != null) {
                    prefs.edit().putString(KEY_LAST_STATE, phoneState).apply()
                }
                if (incomingNumber != null) {
                    prefs.edit().putString(KEY_LAST_NUMBER, incomingNumber).apply()
                }
                
                // Detect when call ends: IDLE state after RINGING or OFFHOOK
                if (phoneState == TelephonyManager.EXTRA_STATE_IDLE && lastState != null) {
                    // Call has ended
                    val callStatus = when {
                        isOutgoing -> "outgoing"
                        lastState == TelephonyManager.EXTRA_STATE_RINGING -> "missed" // Was ringing but never went offhook
                        lastState == TelephonyManager.EXTRA_STATE_OFFHOOK -> "incoming" // Was answered
                        else -> "ended"
                    }
                    
                    // Use last number if available
                    val phoneNumber = lastNumber ?: incomingNumber
                    if (!phoneNumber.isNullOrEmpty()) {
                        Log.d("EndCallReceiver", "Call ended: $phoneNumber, Status: $callStatus")
                        handleCallEnded(context, phoneNumber, callStatus)
                        
                        // Clear stored state
                        prefs.edit().remove(KEY_LAST_STATE)
                            .remove(KEY_LAST_NUMBER)
                            .remove(KEY_IS_OUTGOING)
                            .apply()
                    }
                } else if (phoneState == TelephonyManager.EXTRA_STATE_RINGING && !incomingNumber.isNullOrEmpty()) {
                    // Incoming call started ringing
                    prefs.edit().putBoolean(KEY_IS_OUTGOING, false).apply()
                    Log.d("EndCallReceiver", "Incoming call ringing: $incomingNumber")
                } else if (phoneState == TelephonyManager.EXTRA_STATE_OFFHOOK && !incomingNumber.isNullOrEmpty()) {
                    // Call was answered (went offhook)
                    Log.d("EndCallReceiver", "Call answered (offhook): $incomingNumber")
                }
            }
            
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                if (!phoneNumber.isNullOrEmpty()) {
                    Log.d("EndCallReceiver", "Outgoing call initiated: $phoneNumber")
                    // Store outgoing call info
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString(KEY_LAST_NUMBER, phoneNumber)
                        .putBoolean(KEY_IS_OUTGOING, true)
                        .apply()
                    // Note: We'll handle the actual call end when IDLE state is detected
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
        
        // Start foreground service to handle WhatsApp/webhook (will check toggle inside service)
        WhatsAppForegroundService.startService(context)
        WhatsAppForegroundService.sendWebhook(context, phoneNumber, callStatus)
    }

    private fun sendSMS(context: Context, phoneNumber: String) {
        try {
            val msgDetails = AppPreferences.getMsgDetails(context)
            val message = msgDetails?.data?.getOrNull(0)?.aMessage ?: ""
            val client = msgDetails?.data?.getOrNull(0)?.client?:"chordz"
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

}