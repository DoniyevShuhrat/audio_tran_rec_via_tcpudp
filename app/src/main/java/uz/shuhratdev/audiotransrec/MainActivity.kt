package uz.shuhratdev.audiotransrec

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.format.Formatter
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var btnStart: Button
    private lateinit var etPort: EditText
    private lateinit var tvLogs: TextView
    private var isServiceRunning = false

    // Servisdan keladigan loglarni real vaqtda ushlab qoluvchi qabul qilgich
    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message") ?: ""
            // Yangi logni terminal simulyatsiyasiga qo'shamiz
            tvLogs.append("$message\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Dinamik interfeys (Layout) yaratamiz
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = Gravity.TOP
        }

        // IP haqida ma'lumot qismi
        val deviceIp = getWifiIpAddress()
        val tvIpInfo = TextView(this).apply {
            text = "Telefon Wi-Fi IP: $deviceIp\nADB Local IP: 127.0.0.1 (Kabel uchun)\n"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        rootLayout.addView(tvIpInfo)

        // Port kiritish maydoni (EditText)
        etPort = EditText(this).apply {
            hint = "Port raqamini kiriting (Masalan: 12345)"
            setText("12345")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        rootLayout.addView(etPort)

        // Boshlash tugmasi (Button)
        btnStart = Button(this).apply {
            text = "Audio Oqimni Boshlash"
        }
        btnStart.setOnClickListener {
            checkPermissionAndStartService()
        }
        rootLayout.addView(btnStart)

        // Log sarlavhasi
        val tvLogTitle = TextView(this).apply {
            text = "\nLog Terminal:"
            textSize = 16f
        }
        rootLayout.addView(tvLogTitle)

        // Loglarni aylantirib (scroll) ko'rish uchun ScrollView va ichida TextView (Terminal ko'rinishi)
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // Qolgan hamma bo'sh joyni terminal egallaydi
            )
        }

        tvLogs = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.GREEN) // Terminal kabi yashil rang
            setPadding(50, 50, 50, 50)
            text = "[#] Ilova tayyor. Servisni yoqing...\n"
        }
        scrollView.addView(tvLogs)
        rootLayout.addView(scrollView)

        setContentView(rootLayout)

        // 2. Log qabul qilgichni xavfsiz flag bilan ro'yxatga olamiz (Android 14+ mosligi uchun)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, IntentFilter("AUDIO_STREAM_LOG"), RECEIVER_NOT_EXPORTED)
        } else {
            // Android 12 va undan past versiyalar uchun ContextCompat yordamida flag beramiz
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                logReceiver,
                IntentFilter("AUDIO_STREAM_LOG"),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
//            registerReceiver(logReceiver, IntentFilter("AUDIO_STREAM_LOG"))
        }

        //==============

//        btnStart = Button(this).apply {
//            text = "Audio Oqimni Boshlash"
//        }
//        setContentView(btnStart)
//
//        btnStart.setOnClickListener {
//            checkPermissionAndStartService()
//        }
    }

    private fun checkPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
                return
            }
        }
        startAudioService()
    }

    private fun startAudioService() {
        val portString = etPort.text.toString()
        val port = portString.toIntOrNull()

        if (port == null || port !in 1024..65535) {
            Toast.makeText(this, "Iltimos to'g'ri port kiriting (1024-65535)!", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val intent = Intent(this, AudioStreamService::class.java).apply {
            putExtra("PORT_KEY", port) // Kiritilgan dinamik portni servisga uzatamiz
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        tvLogs.append("[#] Servis $port portida start qilindi.\n")
        btnStart.text = "Oqim Faol (Fonda ishlamoqda)"
        btnStart.isEnabled = false
        etPort.isEnabled = false
        isServiceRunning = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAudioService()
        } else {
            Toast.makeText(
                this,
                "Bildirishnoma ruxsati berilmadi! Servis ishlamasligi mumkin.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Telefon Wi-Fi IP manzilini aniqlash funksiyasi
    private fun getWifiIpAddress(): String {
        return try {
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            Formatter.formatIpAddress(ipAddress)
        } catch (e: Exception) {
            "Aniqlab bo'lmadi"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(logReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}