package com.chordz.eprachar

import android.app.Application
import android.util.Log

class EPracharApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("EPracharApp", "Application onCreate")
        
        // Try to start the service on app launch
        // This ensures the service is running even if MainActivity hasn't been opened yet
        try {
            WhatsAppForegroundService.startService(this)
            Log.d("EPracharApp", "Service started from Application")
        } catch (e: Exception) {
            Log.e("EPracharApp", "Failed to start service from Application", e)
        }
    }
}


