package com.chordz.eprachar

import android.Manifest
import android.R.attr
import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.chordz.eprachar.data.ElectionDataHolder
import com.chordz.eprachar.data.ElectionDataHolder.getBitmapUriFromBitmap
import com.chordz.eprachar.data.ElectionDataHolder.msgDetails
import com.chordz.eprachar.data.MainRepository
import com.chordz.eprachar.data.remote.RetroFitService.Companion.getInstance
import com.chordz.eprachar.data.response.DataItem
import com.chordz.eprachar.data.response.ElectionMessageResponse
import com.chordz.eprachar.preferences.AppPreferences
import com.chordz.eprachar.viewModel.AppViewModelFactory
import com.chordz.eprachar.viewModel.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private var bmpUri: Uri? = null
    private lateinit var textView2: TextView
    private lateinit var root: ConstraintLayout
    private var imageBitmap: Bitmap? = null
    private lateinit var swSMSOnOff: SwitchCompat
    private lateinit var swWhatsAppOnOff: SwitchCompat
    private lateinit var serviceManager: AccessibilityServiceManager
    private lateinit var etMaxNoOfMsg: EditText
    var editText = ""
    var apiResponse = ""
    private var phoneNumberEditText: EditText? = null
    private var openWhatsAppButton: Button? = null
    private lateinit var swOnOff: SwitchCompat
    private var homeViewModel: HomeViewModel? = null
    private var detailsList: ArrayList<DataItem?>? = ArrayList()
    private val dataItem: DataItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!foregroundServiceRunning()) {
            val serviceIntent = Intent(this, MyForegroundService::class.java)

            startForegroundService(serviceIntent)
        }
        initView()
        updateUI()
        fetchData()
        val phoneNumber = intent.getStringExtra("PHONE_NUMBER")
        if (phoneNumber != null) {
            Log.e("TAG", "processPrachar onCreate: ")
//            processPrachar(phoneNumber);
        }
    }

    private fun processPrachar(phoneNumber: String) {
        if (ElectionDataHolder.pracharContactsMap.keys.contains(phoneNumber)) {
            val contactedTimes: Int = ElectionDataHolder.pracharContactsMap[phoneNumber]!!
            ElectionDataHolder.pracharContactsMap[phoneNumber] = contactedTimes + 1
        } else {
            ElectionDataHolder.pracharContactsMap.put(phoneNumber, 1)
        }
        if (ElectionDataHolder.pracharContactsMap[phoneNumber]!! >
            AppPreferences.getIntValueFromSharedPreferences(AppPreferences.DAILY_MESSAGE_LIMIT)
        ) {
            Snackbar.make(root, "Message limit is overed for this number", Snackbar.LENGTH_LONG)
                .show()
            return
        }

        Handler(Looper.myLooper()!!).postDelayed(Runnable {
            openWhatsApp(this, phoneNumber)
            check()
            finish()
        }, 1000)
    }

    private fun updateUI() {
        if (AppPreferences.getLongValueFromSharedPreferences(AppPreferences.ADMIN_NUMBER) != 0L) {
            phoneNumberEditText!!.setText(
                AppPreferences.getLongValueFromSharedPreferences(
                    AppPreferences.ADMIN_NUMBER
                ).toString()
            )
        }
        if (AppPreferences.getIntValueFromSharedPreferences(AppPreferences.DAILY_MESSAGE_LIMIT) != 0) {
            etMaxNoOfMsg.setText(
                AppPreferences.getIntValueFromSharedPreferences(AppPreferences.DAILY_MESSAGE_LIMIT)
                    .toString()
            )
        }
        if (AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.SMS_ON_OFF)) {
            swSMSOnOff.isChecked =
                AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.SMS_ON_OFF)
        }
        if (AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.WHATSAPP_ON_OFF)) {
            swWhatsAppOnOff.isChecked =
                AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.WHATSAPP_ON_OFF)
        }
    }

    private fun shareViaWhatsApp(image: Bitmap, text: String, phoneNumber: String) {
//        val callingNumber = phoneNumber.replace("+", "")
//        var phoneNumberWithCC = callingNumber
//        if (callingNumber.length == 10) {
//            phoneNumberWithCC = "91$callingNumber"
//        }
//        val shareIntent = Intent(Intent.ACTION_SEND).apply {
//            setPackage("com.whatsapp");
//            component = ComponentName("com.whatsapp", "com.whatsapp.contact.picker.ContactPicker")
//            putExtra(Intent.EXTRA_STREAM, getBitmapUriFromBitmap(this@MainActivity, image))
//            putExtra("jid", phoneNumberWithCC + "@s.whatsapp.net"); //phone number without "+" prefix
//            putExtra(Intent.EXTRA_TEXT, text)
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            type = "image/*"
//        }
//        startActivity(shareIntent)
//        finishAffinity()

        try {
            shareOnNormalWhatsApp(image, text, phoneNumber)
        } catch (e: Exception) {
            Handler(Looper.myLooper()!!).postDelayed({
                shareViaWhatsAppBuzz(image, text, phoneNumber)
            }, 2000)
        }
    }

    private fun shareOnNormalWhatsApp(image: Bitmap, text: String, phoneNumber: String) {
        Toast.makeText(this, "Sending WhatsApp Post", Toast.LENGTH_SHORT).show()

        val toNumber = phoneNumber.replace("+", "").replace(" ", "")
        // Check if the phoneNumber starts with "+91", if not, prepend "+91"
        val formattedNumber = if (!phoneNumber.startsWith("+91")) {
            "91$toNumber"
        } else {
            toNumber
        }
        val sentIntent = Intent("android.intent.action.MAIN")
        sentIntent.putExtra("jid", "$formattedNumber@s.whatsapp.net")
        sentIntent.putExtra(
            Intent.EXTRA_STREAM,
            getBitmapUriFromBitmap(this@MainActivity, image)
        )
//        sentIntent.putExtra(Intent.EXTRA_TEXT, text)
        sentIntent.action = Intent.ACTION_SEND
        sentIntent.setPackage("com.whatsapp")
        sentIntent.type = "text/plain"
        sentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sentIntent.type = "image/*"
        startActivity(sentIntent)
        finishAffinity()
    }

    private fun shareViaWhatsAppBuzz(image: Bitmap, text: String, phoneNumber: String) {
//        val callingNumber = phoneNumber.replace("+", "")
//        var phoneNumberWithCC = callingNumber
//        if (callingNumber.length == 10) {
//            phoneNumberWithCC = "91$callingNumber"
//        }
//        val shareIntent = Intent(Intent.ACTION_SEND).apply {
//            setPackage("com.whatsapp");
//            component = ComponentName("com.whatsapp", "com.whatsapp.contact.picker.ContactPicker")
//            putExtra(Intent.EXTRA_STREAM, getBitmapUriFromBitmap(this@MainActivity, image))
//            putExtra("jid", phoneNumberWithCC + "@s.whatsapp.net"); //phone number without "+" prefix
//            putExtra(Intent.EXTRA_TEXT, text)
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            type = "image/*"
//        }
//        startActivity(shareIntent)
//        finishAffinity()

        try {
            Toast.makeText(this, "Sending WhatsApp Buzz Post", Toast.LENGTH_SHORT).show()

            val toNumber = phoneNumber.replace("+", "").replace(" ", "")
            // Check if the phoneNumber starts with "+91", if not, prepend "+91"
            val formattedNumber = if (!phoneNumber.startsWith("+91")) {
                "91$toNumber"
            } else {
                toNumber
            }
            val sentIntent = Intent("android.intent.action.MAIN")
            sentIntent.putExtra("jid", "$formattedNumber@s.whatsapp.net")
            sentIntent.putExtra(
                Intent.EXTRA_STREAM,
                getBitmapUriFromBitmap(this@MainActivity, image)
            )
//        sentIntent.putExtra(Intent.EXTRA_TEXT, text)
            sentIntent.action = Intent.ACTION_SEND
            sentIntent.setPackage("com.whatsapp.w4b")
            sentIntent.type = "text/plain"
            sentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sentIntent.type = "image/*"
            startActivity(sentIntent)
            finishAffinity()
        } catch (e: Exception) {
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun getImageUri(inContext: Context?, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            this@MainActivity.contentResolver,
            inImage,
            UUID.randomUUID().toString() + ".png",
            "drawing"
        )
        return Uri.parse(path)
    }

    private fun constructShareableContent(context: Context, image: Bitmap, text: String) {
        // Combine image and text for sharing
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, getBitmapUriFromBitmap(context, image))
            putExtra(Intent.EXTRA_TEXT, text)
            type = "image/*"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }


    private fun openWhatsApp(context: Context, mPhoneNumber: String) {
        if (mPhoneNumber.equals("null") || mPhoneNumber.isNullOrBlank()) {
            return
        }
        var phoneNumber = ""
        if (mPhoneNumber.startsWith("0")) {
            phoneNumber = mPhoneNumber.subSequence(1, mPhoneNumber.length).toString()
            phoneNumber = "+91$phoneNumber"
        } else {
            phoneNumber = mPhoneNumber
        }
        serviceManager = AccessibilityServiceManager(context)
        if (serviceManager.hasAccessibilityServicePermission(MyAccessibilityService::class.java)) {
            val response = msgDetails
            if (response != null && response.data != null) {
                val details = response.data
                val defaultMessage = details[0]!!.aMessage+"\n click link for more details\n"+details[0]!!.aImage
                if (AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.SMS_ON_OFF)) {
                    Toast.makeText(this, "Sending SMS", Toast.LENGTH_SHORT).show()
                    sendSMSMessage(formatPhoneNumber(phoneNumber)!!, defaultMessage!!)
                }else{
                    Toast.makeText(this, "Sending SMS is off", Toast.LENGTH_SHORT).show()
                }
                if (!AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.WHATSAPP_ON_OFF)) {
                    Toast.makeText(this, "WhatsApp Posting is off", Toast.LENGTH_SHORT).show()
                    return
                }


                //Code For New Line
//                String formattedMessage = defaultMessage.replace("|", "\n");


                // Format the phone number to include the country code (e.g., +1 for the US)
                val formattedPhoneNumber: String = formatPhoneNumber(phoneNumber)!!

                // Create an Intent to open WhatsApp with the specified phone number and default message
                val whatsappIntent = Intent(Intent.ACTION_VIEW)
                whatsappIntent.data = Uri.parse(
                    "https://wa.me/" + formattedPhoneNumber + "?text=" + Uri.encode(defaultMessage)
                )
                Toast.makeText(this, "Sending WhatsApp Message", Toast.LENGTH_SHORT).show()

                // Add FLAG_ACTIVITY_NEW_TASK flag
                whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                this.startActivity(whatsappIntent)
                if (ElectionDataHolder.messageBitmap == null) {
                    Snackbar.make(root, "Message not downloaded", Snackbar.LENGTH_SHORT).show()
                } else if (AppPreferences.getBooleanValueFromSharedPreferences(AppPreferences.WHATSAPP_ON_OFF)) {
                    Handler(Looper.myLooper()!!).postDelayed({
                        shareViaWhatsApp(
                            ElectionDataHolder.messageBitmap!!,
                            defaultMessage!!,
                            phoneNumber
                        )
                    }, 3000)
                }
            } else {
                Toast.makeText(this, "Reset Prachar", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            //            serviceManager.requestUserForAccessibilityService( context);
        }
    }


    private fun sendSMSMessage(phoneNumber: String, defaultMessage: String) {
        try {
            val sms = SmsManager.getDefault()
            val parts: ArrayList<String> = sms.divideMessage(defaultMessage)
            sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
//            smsManager.sendTextMessage(phoneNumber, phoneNumber, defaultMessage, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        val toNumber = phoneNumber.replace("+", "").replace(" ", "")
        // Check if the phoneNumber starts with "+91", if not, prepend "+91"
        val formattedNumber = if (!phoneNumber.startsWith("+91") ) {
            "+91$toNumber"
        } else {
            toNumber
        }
        return formattedNumber
    }


    private fun isAccessibilityOn(
        context: Context,
        clazz: Class<out AccessibilityService?>
    ): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/" + clazz.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (ignored: Settings.SettingNotFoundException) {
        }
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue: String = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                colonSplitter.setString(settingValue)
                while (colonSplitter.hasNext()) {
                    val accessibilityService = colonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun fetchData() {
//        homeViewModel.getMsg();
    }

    private fun initObservable() {
        homeViewModel!!.electionmsgLiveData.observe(this) { electionMessageResponse: ElectionMessageResponse ->
            if (electionMessageResponse.code == 200 && !electionMessageResponse.data!!.isEmpty()) {
                detailsList = electionMessageResponse.data as ArrayList<DataItem?>?
                msgDetails = electionMessageResponse
                Toast.makeText(this, "Sync Success", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sync Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (MyForegroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun initView() {

        if (ElectionDataHolder.hourlyMessageUpdateTime == null) {
            ElectionDataHolder.hourlyMessageUpdateTime = Calendar.getInstance().timeInMillis
        }
        //off from e prachar service after 7 second

        if (ElectionDataHolder.DailyCountUpdateTime == null) {
            ElectionDataHolder.hourlyMessageUpdateTime = Calendar.getInstance().timeInMillis
        }

        val retrofitService = getInstance()
        val mainRepository = MainRepository(retrofitService)
        homeViewModel = ViewModelProvider(this, AppViewModelFactory(mainRepository)).get(
            HomeViewModel::class.java
        )

        initObservable()
        root = findViewById(R.id.root)
        etMaxNoOfMsg = findViewById(R.id.etMaxNoOfMsg)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        openWhatsAppButton = findViewById(R.id.openWhatsAppButton)
        swOnOff = findViewById(R.id.swOnOff)
        swOnOff.visibility = View.GONE
        swWhatsAppOnOff = findViewById(R.id.swWhatsAppOnOff)
        swSMSOnOff = findViewById(R.id.swSMSOnOff)
        textView2 = findViewById(R.id.textView2)
        textView2.visibility = View.GONE
        swSMSOnOff.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked)
                AppPreferences.saveBooleanToSharedPreferences(
                    this@MainActivity,
                    AppPreferences.SMS_ON_OFF,
                    true
                )
            else
                AppPreferences.saveBooleanToSharedPreferences(
                    this@MainActivity,
                    AppPreferences.SMS_ON_OFF,
                    false
                )
        }
        swWhatsAppOnOff.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked)
                AppPreferences.saveBooleanToSharedPreferences(
                    this@MainActivity,
                    AppPreferences.WHATSAPP_ON_OFF,
                    true
                )
            else
                AppPreferences.saveBooleanToSharedPreferences(
                    this@MainActivity,
                    AppPreferences.WHATSAPP_ON_OFF,
                    false
                )
        }
        /*swOnOff.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                swWhatsAppOnOff.isChecked = true
                swSMSOnOff.isChecked = true
                AppPreferences.saveBooleanToSharedPreferences(
                    this@MainActivity,
                    AppPreferences.PRACHAR_ON_OFF,
                    true
                )
            } else {
                swWhatsAppOnOff.isChecked = false
                swSMSOnOff.isChecked = false
                AppPreferences.saveBooleanToSharedPreferences(
                    this@MainActivity,
                    AppPreferences.PRACHAR_ON_OFF,
                    false
                )
            }
        }*/
        openWhatsAppButton?.setOnClickListener(View.OnClickListener {
            resetAllValues(this)
            check()
        })
        Log.e("TAG", "initView: " + ElectionDataHolder.hourlyMessageUpdateTime!!)
        if (DateUtils.needToHourlyMessageUpdated(
                Calendar.getInstance().timeInMillis,
                ElectionDataHolder.hourlyMessageUpdateTime!!
            )
        ) {
            Toast.makeText(this, "Refreshing Message", Toast.LENGTH_SHORT).show()
            check()
        }
//        AccessibilityServiceManager serviceManager = new AccessibilityServiceManager(this);
//        openWhatsAppButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (serviceManager.hasAccessibilityServicePermission(MyAccessibilityService.class)) {
//                    String message = "Your message", to = "917972546880";
//                    startActivity(
//                            new Intent(Intent.ACTION_VIEW,
//                                    Uri.parse(
//                                            String.format("https://api.whatsapp.com/send?phone=%s&text=%s", to, message)
//                                    )
//                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    );
//                } else {
//                    serviceManager.requestUserForAccessibilityService(MainActivity.this);
//                }
//            }
//        });
        if (checkPermissions()) {
            // Permissions are already granted, proceed with your logic
        } else {
            // Request permissions
            requestPermissions()
        }
        val intent = Intent(this, MyBackgroundService::class.java)
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            val REQUEST_CODE = 101
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            myIntent.data = Uri.parse("package:$packageName")
            this.startActivity(myIntent)
        }
        // Removed automatic accessibility settings check to prevent popups
        // Users can manually enable accessibility services if needed
        val serviceManager = AccessibilityServiceManager(this)
        if (!serviceManager.hasAccessibilityServicePermission(MyAccessibilityService::class.java)) {
            serviceManager.requestUserForAccessibilityService(this)
        }
    }

    private fun resetAllValues(context: Context) {
        ElectionDataHolder.pracharContactsMap.clear()
        AppPreferences.saveStringToSharedPreferences(
            context, AppPreferences.DAILY_RESET_DATE,
            DateUtils.getCurrentDateTime()
        )
        AppPreferences.saveIntToSharedPreferences(
            context, AppPreferences.TODAYS_MESSAGE_COUNT,
            0
        )
    }

    private fun check() {
        //Admin number long
        // daily limit int


        val editTextValue = phoneNumberEditText!!.text.toString().trim { it <= ' ' }

        if (etMaxNoOfMsg.text.toString().isEmpty()) {
            Toast.makeText(this, "Please enter Daily Message Limit", Toast.LENGTH_SHORT).show()
            return
        }
        if (editTextValue.isEmpty()) {
            Toast.makeText(this, "Please enter admin number", Toast.LENGTH_SHORT).show()
            return
        }

        AppPreferences.saveLongToSharedPreferences(
            this,
            AppPreferences.ADMIN_NUMBER,
            editTextValue.toLong()
        )
        AppPreferences.saveBooleanToSharedPreferences(
            this,
            AppPreferences.SMS_ON_OFF,
            swSMSOnOff.isChecked
        )
        AppPreferences.saveBooleanToSharedPreferences(
            this,
            AppPreferences.WHATSAPP_ON_OFF,
            swWhatsAppOnOff.isChecked
        )

        AppPreferences.saveIntToSharedPreferences(
            this,
            AppPreferences.DAILY_MESSAGE_LIMIT,
            etMaxNoOfMsg.text.toString().toInt()
        )


        try {
//            int messageId = Integer.parseInt(editTextValue);
//            homeViewModel.getMsgById(messageId);
            if (isConnectedToInternet()) {
                homeViewModel!!.getMsgContactNo(this, editTextValue)
            } else {
                showNoInternetDialog()
            }

        } catch (e: NumberFormatException) {
            // Handle the case when the input is not a valid integer
            showMismatchAlertDialog()
        }
    }

    private fun showMismatchAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Path Parameter Mismatch")
        builder.setMessage("The input does not match the expected path parameter.")
        builder.setPositiveButton("OK") { dialog: DialogInterface?, which: Int -> }
        builder.setCancelable(false)
        builder.show()
    }

    private fun checkPermissions(): Boolean {
        AppPreferences.saveStringToSharedPreferences(
            this, AppPreferences.DAILY_RESET_DATE,
            DateUtils.getCurrentDateTime()
        )
        val readCallLogPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
        val readPhoneStatePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        val readOutgoingCallPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS)
        val sendSmsPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val receiveSmsPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
        return readCallLogPermission == PackageManager.PERMISSION_GRANTED && readPhoneStatePermission == PackageManager.PERMISSION_GRANTED && readOutgoingCallPermission == PackageManager.PERMISSION_GRANTED && sendSmsPermission == PackageManager.PERMISSION_GRANTED && receiveSmsPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                // All permissions are granted, proceed with your logic
            } else {
                // Permissions are not granted, show an alert dialog
                showPermissionAlertDialog()
            }
        }
    }

    private fun showPermissionAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
        builder.setMessage("Please allow all the required permissions to use this app.")
        builder.setPositiveButton("OK") { dialog: DialogInterface?, which: Int ->
            // Request permissions again
            requestPermissions()
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface?, which: Int ->
            // Handle the case when the user cancels the permission request
            // You can choose to finish the activity or take appropriate action
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val phoneNumber = intent.getStringExtra("PHONE_NUMBER")
        Log.e("TAG", "initView: " + ElectionDataHolder.hourlyMessageUpdateTime!!)
        if (DateUtils.needToHourlyMessageUpdated(
                Calendar.getInstance().timeInMillis,
                ElectionDataHolder.hourlyMessageUpdateTime!!
            )
        ) {
            Toast.makeText(this, "Refreshing Message", Toast.LENGTH_SHORT).show()
            check()
        }

        if (phoneNumber != null) {
            Log.e("TAG", "processPrachar onNewIntent: ")
            processPrachar(phoneNumber)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    open fun isConnectedToInternet(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    open fun showNoInternetDialog() {
        Toast.makeText(
            this,
            "Please check your internet connection and try again.",
            Toast.LENGTH_SHORT
        ).show()

    }
}