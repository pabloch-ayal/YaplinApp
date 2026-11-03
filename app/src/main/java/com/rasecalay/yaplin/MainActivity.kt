package com.rasecalay.yaplin

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var testPlayer: MediaPlayer? = null
    private var paymentReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkLicenseState()
    }

    private fun checkLicenseState() {
        val layoutMainApp = findViewById<View>(R.id.layoutMainApp)
        val layoutLockOverlay = findViewById<View>(R.id.layoutLockOverlay)

        if (LicenseManager.isActivated(this)) {
            layoutLockOverlay.visibility = View.GONE
            layoutMainApp.visibility = View.VISIBLE
            setupMainApp()
        } else {
            layoutLockOverlay.visibility = View.VISIBLE
            layoutMainApp.visibility = View.GONE
            setupLockOverlay()
        }
    }

    private fun setupLockOverlay() {
        val tvDeviceId = findViewById<TextView>(R.id.tvDeviceId)
        val btnRequestWhatsApp = findViewById<Button>(R.id.btnRequestWhatsApp)
        val etLicenseKey = findViewById<EditText>(R.id.etLicenseKey)
        val btnActivateApp = findViewById<Button>(R.id.btnActivateApp)

        val deviceId = LicenseManager.getOrCreateDeviceId(this)
        tvDeviceId.text = deviceId

        btnRequestWhatsApp.setOnClickListener {
            LicenseManager.openWhatsAppActivation(this, deviceId)
        }

        btnActivateApp.setOnClickListener {
            val key = etLicenseKey.text.toString()
            if (LicenseManager.validateAndActivate(this, key)) {
                Toast.makeText(this, "¡Activación exitosa! Bienvenido a Yaplin.", Toast.LENGTH_LONG).show()
                checkLicenseState()
            } else {
                Toast.makeText(this, "Clave incorrecta. Verifica o solicita activación.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMainApp() {
        val btnTestAudio = findViewById<Button>(R.id.btnTestAudio)
        val btnBatterySettings = findViewById<Button>(R.id.btnBatterySettings)
        val btnViewHistory = findViewById<Button>(R.id.btnViewHistory)
        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)

        tvStatusBadge.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnTestAudio.setOnClickListener {
            playTestSequence()
        }

        btnBatterySettings.setOnClickListener {
            openBatterySettings()
        }

        btnViewHistory.setOnClickListener {
            showHistoryBottomSheet()
        }

        paymentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val service = intent?.getStringExtra("service") ?: "Pago"
                val amount = intent?.getStringExtra("amount") ?: "0.00"
                val time = intent?.getStringExtra("time") ?: ""

                findViewById<TextView>(R.id.tvLastAmount)?.text = "S/ $amount"
                findViewById<TextView>(R.id.tvLastServiceDetail)?.text = "$service • $time"

                updateDailyCard()
            }
        }

        val filter = IntentFilter("com.rasecalay.yaplin.NUEVO_PAGO")
        registerReceiver(paymentReceiver, filter)

        updateDailyCard()
    }

    private fun updateDailyCard() {
        val todayTotal = PaymentHistoryManager.getTodayTotal(this)
        val todayRecords = PaymentHistoryManager.getTodayRecords(this)

        findViewById<TextView>(R.id.tvDailyAccumulated)?.text = String.format(Locale.US, "S/ %.2f", todayTotal)
        findViewById<TextView>(R.id.tvDailyCountText)?.text = "${todayRecords.size} pagos registrados hoy"
    }

    private fun showHistoryBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_history, null)
        dialog.setContentView(view)

        val tabToday = view.findViewById<TextView>(R.id.tabToday)
        val tab7Days = view.findViewById<TextView>(R.id.tab7Days)
        val container = view.findViewById<LinearLayout>(R.id.containerHistoryItems)

        fun loadTodayTab() {
            tabToday.setBackgroundColor(Color.parseColor("#5E17EB"))
            tabToday.setTextColor(Color.WHITE)
            tab7Days.setBackgroundColor(Color.TRANSPARENT)
            tab7Days.setTextColor(Color.parseColor("#666666"))

            container.removeAllViews()
            val records = PaymentHistoryManager.getTodayRecords(this)

            if (records.isEmpty()) {
                val emptyTv = TextView(this).apply {
                    text = "No hay pagos registrados hoy todavía."
                    textSize = 14f
                    setTextColor(Color.parseColor("#888888"))
                    setPadding(16, 24, 16, 24)
                }
                container.addView(emptyTv)
                return
            }

            for (r in records) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 16, 0, 16)
                }

                val leftCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val title = TextView(this).apply {
                    text = "${r.service} • ${r.remitente}"
                    textSize = 15f
                    setTextColor(Color.parseColor("#222222"))
                }
                val sub = TextView(this).apply {
                    text = r.hora
                    textSize = 12f
                    setTextColor(Color.parseColor("#888888"))
                }
                leftCol.addView(title)
                leftCol.addView(sub)

                val amountTv = TextView(this).apply {
                    text = String.format(Locale.US, "+ S/ %.2f", r.monto)
                    textSize = 16f
                    setTextColor(Color.parseColor("#2E7D32"))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                row.addView(leftCol)
                row.addView(amountTv)
                container.addView(row)

                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                }
                container.addView(divider)
            }
        }

        fun load7DaysTab() {
            tab7Days.setBackgroundColor(Color.parseColor("#5E17EB"))
            tab7Days.setTextColor(Color.WHITE)
            tabToday.setBackgroundColor(Color.TRANSPARENT)
            tabToday.setTextColor(Color.parseColor("#666666"))

            container.removeAllViews()
            val summaries = PaymentHistoryManager.get7DaysSummary(this)

            if (summaries.isEmpty()) {
                val emptyTv = TextView(this).apply {
                    text = "No hay datos en los últimos 7 días."
                    textSize = 14f
                    setTextColor(Color.parseColor("#888888"))
                    setPadding(16, 24, 16, 24)
                }
                container.addView(emptyTv)
                return
            }

            for (s in summaries) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 18, 0, 18)
                }

                val leftCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val labelDate = if (s.fecha == PaymentHistoryManager.getTodayDateString()) "Hoy (${s.fecha})" else s.fecha
                val title = TextView(this).apply {
                    text = labelDate
                    textSize = 15f
                    setTextColor(Color.parseColor("#222222"))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val countTv = TextView(this).apply {
                    text = "${s.count} cobro(s) realizado(s)"
                    textSize = 12f
                    setTextColor(Color.parseColor("#777777"))
                }
                leftCol.addView(title)
                leftCol.addView(countTv)

                val totalTv = TextView(this).apply {
                    text = String.format(Locale.US, "S/ %.2f", s.total)
                    textSize = 16f
                    setTextColor(Color.parseColor("#5E17EB"))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                row.addView(leftCol)
                row.addView(totalTv)
                container.addView(row)

                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                }
                container.addView(divider)
            }
        }

        tabToday.setOnClickListener { loadTodayTab() }
        tab7Days.setOnClickListener { load7DaysTab() }

        loadTodayTab()
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (LicenseManager.isActivated(this)) {
            updatePermissionStatus()
            updateDailyCard()
        }
    }

    private fun updatePermissionStatus() {
        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
        val tvStatusSub = findViewById<TextView>(R.id.tvStatusSub)
        val hasPermission = isNotificationServiceEnabled()

        if (hasPermission) {
            tvStatusBadge.text = "● ESTADO: ACTIVO"
            tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"))
            tvStatusBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
            tvStatusSub.text = "Escuchando pagos en segundo plano"
        } else {
            tvStatusBadge.text = "● ESTADO: INACTIVO (Toca aquí)"
            tvStatusBadge.setTextColor(Color.parseColor("#C62828"))
            tvStatusBadge.setBackgroundColor(Color.parseColor("#FFEBEE"))
            tvStatusSub.text = "Toca para conceder permiso de notificaciones"
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val cn = ComponentName(this, YaplinListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun playTestSequence() {
        testPlayer?.release()
        testPlayer = null

        val playlist = AudioAmountBuilder.buildAudioPlaylist(this, 12.0, true)
        var index = 0

        fun playNext() {
            if (index >= playlist.size) {
                testPlayer?.release()
                testPlayer = null
                return
            }

            val resId = playlist[index++]
            try {
                testPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    val afd = resources.openRawResourceFd(resId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    start()
                    setOnCompletionListener {
                        it.release()
                        testPlayer = null
                        playNext()
                    }
                }
            } catch (_: Exception) {
                testPlayer?.release()
                testPlayer = null
                playNext()
            }
        }

        playNext()
    }

    private fun openBatterySettings() {
        try {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
                } else {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.parse("package:$packageName")
                }
            } else {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    override fun onDestroy() {
        testPlayer?.release()
        testPlayer = null
        try {
            paymentReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
