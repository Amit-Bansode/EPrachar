package com.chordz.eprachar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d("BootReceiver", "Device booted or app updated, starting WhatsAppForegroundService")
                // Start the foreground service after boot
                WhatsAppForegroundService.startService(context)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Check if it's our package
                val packageName = intent.data?.schemeSpecificPart
                if (packageName == context.packageName) {
                    Log.d("BootReceiver", "App updated, starting WhatsAppForegroundService")
                    WhatsAppForegroundService.startService(context)
                }
            }
        }
    }
}

