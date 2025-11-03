package com.chordz.eprachar.preferences

import android.content.Context
import android.content.SharedPreferences
import com.chordz.eprachar.data.response.ElectionMessageResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object AppPreferences {

    const val isFromEpracharService: String = "isFromEpracharService"
    const val DAILY_RESET_TIME = "DAILY_RESET_TIME"
    val SEND_MESSAGE: String = "SEND_MESSAGE"
    const val IS_CREATING_ESTIMATE: String = "is_creating_estimate"
    const val MOBILE_NUMBER: String = "mobile_number"
    const val DEVICE_INFO: String = "device_info"
    const val USER_ID: String = "user_id"
    const val BUSINESS_ID: String = "business_id"
    const val CLIENT_ID: String = "client_id"
    const val APP_STATUS: String = "Trail"
    const val DAILY_MESSAGE_LIMIT = "Daily_Message_Limit"
    const val TODAYS_MESSAGE_COUNT = "TODAYS_MESSAGE_COUNT"
    const val DAILY_RESET_DATE = "DAILY_RESET_DATE"
    const val HOURLY_RESET_DATE = "HOURLY_RESET_DATE"
    const val PRACHAR_ON_OFF = "PRACHAR_ON_OFF"
    const val SMS_ON_OFF = "SMS_ON_OFF"
    const val WHATSAPP_ON_OFF = "WHATSAPP_ON_OFF"
    const val ADMIN_NUMBER = "ADMIN_NUMBER"
    const val MSG_DETAILS = "MSG_DETAILS"
    private var sharedPreferences: SharedPreferences? = null

    private const val SharedPreferencesName = "DSBOX_PREFERENCES"
    fun getStringValueFromSharedPreferences(key: String): String? {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getString(key, "")
        }
        return ""
    }

    fun saveStringToSharedPreferences(context: Context, key: String, value: String) {
        if (sharedPreferences == null) {
            sharedPreferences =
                context.getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE)
        }
        val edit = sharedPreferences!!.edit()
        edit.putString(key, value)
        edit.apply()
        edit.commit()
    }

    fun isCreatingEstimate(context: Context, key: String, value: Boolean) {
        if (sharedPreferences == null) {
            sharedPreferences =
                context.getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE)
        }
        val edit = sharedPreferences!!.edit()
        edit.putBoolean(key, value)
        edit.apply()
        edit.commit()
    }

    fun getIsCreatingEstimate(key: String): Boolean {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getBoolean(key, false)
        }
        return false;
    }

    fun getLongValueFromSharedPreferences(key: String): Long {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getLong(key, 0)
        }
        return 0;
    }

    fun getIntValueFromSharedPreferences(key: String): Int {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getInt(key, 0)
        }
        return 0;
    }

    fun getBooleanValueFromSharedPreferences(key: String): Boolean {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getBoolean(key, false)
        }
        return false;
    }

    fun saveIntToSharedPreferences(context: Context, key: String, value: Int) {
        if (sharedPreferences == null) {
            sharedPreferences =
                context.getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE)
        }
        val edit = sharedPreferences!!.edit()
        edit.putInt(key, value)
        edit.apply()
        edit.commit()
    }

    fun saveLongToSharedPreferences(context: Context, key: String, value: Long) {
        if (sharedPreferences == null) {
            sharedPreferences =
                context.getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE)
        }
        val edit = sharedPreferences!!.edit()
        edit.putLong(key, value)
        edit.apply()
        edit.commit()
    }

    fun saveBooleanToSharedPreferences(context: Context, key: String, value: Boolean) {
        if (sharedPreferences == null) {
            sharedPreferences =
                context.getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE)
        }
        val edit = sharedPreferences!!.edit()
        edit.putBoolean(key, value)
        edit.apply()
        edit.commit()
    }

    fun saveMsgDetails(context: Context, msgDetails: ElectionMessageResponse?) {
        if (msgDetails == null) {
            saveStringToSharedPreferences(context, MSG_DETAILS, "")
            return
        }
        val gson = GsonBuilder().create()
        val jsonString = gson.toJson(msgDetails)
        saveStringToSharedPreferences(context, MSG_DETAILS, jsonString)
    }

    fun getMsgDetails(context: Context): ElectionMessageResponse? {
        if (sharedPreferences == null) {
            sharedPreferences =
                context.getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE)
        }
        val jsonString = sharedPreferences?.getString(MSG_DETAILS, "") ?: ""
        if (jsonString.isEmpty()) {
            return null
        }
        try {
            val gson = GsonBuilder().create()
            return gson.fromJson(jsonString, ElectionMessageResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}