package com.rasecalay.yaplin

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatusBadge: TextView
    private lateinit var tvStatusDesc: TextView
    private lateinit var tvLastAmount: TextView
    private lateinit var tvLastDetail: TextView
    private lateinit var tvDailyTotal: TextView
    private lateinit var tvDailyCount: TextView
    private lateinit var btnPermissions: Button

    private var testCurrentMp: MediaPlayer? = null
    private var testNextMp: MediaPlayer? = null
    private var testPlaylist: List<Int> = emptyList()
    private var testIndex = 0
    private var titleTapCount = 0

    private val paymentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.rasecalay.yaplin.NUEVO_PAGO") {
                val service = intent.getStringExtra("service") ?: "Yape"
                val amount = intent.getStringExtra("amount") ?: "0.00"
                val time = intent.getStringExtra("time") ?: ""
                val sender = intent.getStringExtra("sender") ?: "Cliente"

                tvLastAmount.text = "S/ $amount"
                tvLastDetail.text = "$service • $sender ($time)"
                refreshDailySummary()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!LicenseManager.isActivated(this)) {
            showActivationScreen()
            return
        }

        buildMainDashboard()

        if (!LicenseManager.isPermanentlyActivated(this) && LicenseManager.getRemainingTrialDays(this) == 1) {
            showLastDayTrialDialog()
        }
    }

    private fun showLastDayTrialDialog() {
        val deviceId = LicenseManager.getDeviceId(this)
        AlertDialog.Builder(this)
            .setTitle("Aviso de período de prueba")
            .setMessage("Te queda 1 día de prueba gratuita en Yaplin. Mañana la aplicación solicitará tu clave de activación permanente.\n\nID de tu equipo: $deviceId")
            .setPositiveButton("Solicitar clave por WhatsApp") { _, _ ->
                openWhatsAppForActivation(deviceId)
            }
            .setNegativeButton("Continuar", null)
            .show()
    }

    private fun openWhatsAppForActivation(deviceId: String) {
        try {
            val msg = "Hola, deseo activar Yaplin. Mi ID de dispositivo es: $deviceId"
            val url = "https://api.whatsapp.com/send?phone=51939525004&text=" + URLEncoder.encode(msg, "UTF-8")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showActivationScreen() {
        val deviceId = LicenseManager.getDeviceId(this)

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121218"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(36), dp(24), dp(36))
        }

        val lockIcon = TextView(this).apply {
            text = "🔒"
            textSize = 46f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }

        val title = TextView(this).apply {
            text = "Activación de Yaplin"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Tu período de prueba de 15 días ha finalizado. Envía tu ID por WhatsApp para obtener tu clave permanente."
            textSize = 14f
            setTextColor(Color.parseColor("#B0B3C6"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }

        val idCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = createCardDrawable("#1E1E2A", "#383A4F", 16)
        }

        val idLabel = TextView(this).apply {
            text = "TU ID DE DISPOSITIVO"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#9E9EB3"))
            gravity = Gravity.CENTER
        }

        val idValue = TextView(this).apply {
            text = deviceId
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00E676"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }

        idCard.addView(idLabel)
        idCard.addView(idValue)

        val btnWhatsapp = Button(this).apply {
            text = "SOLICITAR ACTIVACIÓN POR WHATSAPP"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createRoundedDrawable("#25D366", 12)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setOnClickListener {
                openWhatsAppForActivation(deviceId)
            }
        }

        val inputKey = EditText(this).apply {
            hint = "CLAVE DE ACTIVACIÓN"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#6E718A"))
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(6))
            background = createCardDrawable("#1E1E2A", "#5E17EB", 12)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val btnActivate = Button(this).apply {
            text = "ACTIVAR APLICACIÓN"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createRoundedDrawable("#5E17EB", 12)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setOnClickListener {
                val entered = inputKey.text.toString()
                if (LicenseManager.validateAndActivate(this@MainActivity, entered)) {
                    Toast.makeText(this@MainActivity, "¡Yaplin activado correctamente!", Toast.LENGTH_LONG).show()
                    recreate()
                } else {
                    Toast.makeText(this@MainActivity, "Clave incorrecta. Verifica con soporte.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        container.addView(lockIcon)
        container.addView(title)
        container.addView(subtitle)
        container.addView(idCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(16)
        })
        container.addView(btnWhatsapp, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(20)
        })
        container.addView(inputKey, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(14)
        })
        container.addView(btnActivate, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(container)
        setContentView(root)
    }

    private fun buildMainDashboard() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F3F4F8"))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val tvTitle = TextView(this).apply {
            text = "Yaplin"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#5E17EB"))
            gravity = Gravity.CENTER
            setOnClickListener {
                titleTapCount++
                if (titleTapCount >= 3) {
                    titleTapCount = 0
                    showDiagnosticDialog()
                }
            }
        }

        val tvSubtitle = TextView(this).apply {
            text = "Asistente de Cobros en Tiempo Real (Yape y Plin)"
            textSize = 13f
            setTextColor(Color.parseColor("#5F6368"))
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(12))
        }

        root.addView(tvTitle)
        root.addView(tvSubtitle)

        // Tarjeta 1: Estado del Servicio
        val cardStatus = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = createCardDrawable("#FFFFFF", "#E2E5EC", 14)
        }

        tvStatusBadge = TextView(this).apply {
            text = "● ESTADO: ACTIVO"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#137333"))
            background = createRoundedDrawable("#E6F4EA", 8)
            setPadding(dp(14), dp(5), dp(14), dp(5))
        }

        tvStatusDesc = TextView(this).apply {
            text = "Escuchando pagos en segundo plano"
            textSize = 12f
            setTextColor(Color.parseColor("#5F6368"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }

        cardStatus.addView(tvStatusBadge)
        cardStatus.addView(tvStatusDesc)
        root.addView(cardStatus, defaultCardParams())

        // Tarjeta 2: Último Pago Detectado
        val cardLast = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = createCardDrawable("#FFFFFF", "#E2E5EC", 14)
        }

        val lblLast = TextView(this).apply {
            text = "ÚLTIMO PAGO DETECTADO"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#80868B"))
        }

        tvLastAmount = TextView(this).apply {
            text = "S/ --"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#137333"))
            setPadding(0, dp(2), 0, dp(2))
        }

        tvLastDetail = TextView(this).apply {
            text = "A la espera de notificaciones..."
            textSize = 12f
            setTextColor(Color.parseColor("#5F6368"))
        }

        cardLast.addView(lblLast)
        cardLast.addView(tvLastAmount)
        cardLast.addView(tvLastDetail)
        root.addView(cardLast, defaultCardParams())

        // Tarjeta 3: Total Acumulado Hoy + Historial
        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = createCardDrawable("#FFFFFF", "#E2E5EC", 14)
        }

        val lblTotal = TextView(this).apply {
            text = "TOTAL ACUMULADO HOY"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#80868B"))
        }

        tvDailyTotal = TextView(this).apply {
            text = "S/ 0.00"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#137333"))
            setPadding(0, dp(2), 0, dp(2))
        }

        tvDailyCount = TextView(this).apply {
            text = "0 pagos registrados hoy"
            textSize = 12f
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, 0, 0, dp(8))
        }

        val btnHistory = Button(this).apply {
            text = "📊 VER DETALLE E HISTORIAL"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#5E17EB"))
            background = createRoundedDrawable("#F3E8FF", 10)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener {
                showHistoryDialog()
            }
        }

        cardTotal.addView(lblTotal)
        cardTotal.addView(tvDailyTotal)
        cardTotal.addView(tvDailyCount)
        cardTotal.addView(btnHistory, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(cardTotal, defaultCardParams())

        // Selector de Voz (Mujer / Hombre)
        val voiceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnVoiceFemale = Button(this)
        val btnVoiceMale = Button(this)

        fun refreshVoiceButtons() {
            val isFemale = AudioAmountBuilder.getVoiceGender(this) == "f"
            btnVoiceFemale.apply {
                text = "👩 VOZ MUJER"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (isFemale) Color.WHITE else Color.parseColor("#5E17EB"))
                background = if (isFemale) createRoundedDrawable("#5E17EB", 10) else createCardDrawable("#FFFFFF", "#5E17EB", 10)
                setPadding(dp(8), dp(9), dp(8), dp(9))
            }
            btnVoiceMale.apply {
                text = "👨 VOZ HOMBRE"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (!isFemale) Color.WHITE else Color.parseColor("#5E17EB"))
                background = if (!isFemale) createRoundedDrawable("#5E17EB", 10) else createCardDrawable("#FFFFFF", "#5E17EB", 10)
                setPadding(dp(8), dp(9), dp(8), dp(9))
            }
        }

        btnVoiceFemale.setOnClickListener {
            AudioAmountBuilder.setVoiceGender(this, "f")
            refreshVoiceButtons()
            playTestSequence()
        }

        btnVoiceMale.setOnClickListener {
            AudioAmountBuilder.setVoiceGender(this, "m")
            refreshVoiceButtons()
            playTestSequence()
        }

        refreshVoiceButtons()

        voiceRow.addView(btnVoiceFemale, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(5)
        })
        voiceRow.addView(btnVoiceMale, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(5)
        })
        root.addView(voiceRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })

        // Botón Prueba de Audio
        val btnTestAudio = Button(this).apply {
            text = "▶ PROBAR AUDIO DE PRUEBA"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createRoundedDrawable("#5E17EB", 10)
            setPadding(dp(12), dp(11), dp(12), dp(11))
            setOnClickListener {
                playTestSequence()
            }
        }
        root.addView(btnTestAudio, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })

        // Botón Evitar que Yape se duerma
        val btnWakeYape = Button(this).apply {
            text = "⚡ EVITAR QUE YAPE SE DUERMA"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#5E17EB"))
            background = createCardDrawable("#FFFFFF", "#5E17EB", 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                openYapeBatterySettings()
            }
        }
        root.addView(btnWakeYape, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })

        // Botón Ajustes de Batería / Permisos
        btnPermissions = Button(this).apply {
            text = "⚙ AJUSTES DE BATERÍA / PERMISOS"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#3C4043"))
            background = createRoundedDrawable("#E4E6EB", 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                openPermissionSettings()
            }
        }
        root.addView(btnPermissions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })

        val tvFooter = TextView(this).apply {
            text = "Creado por: rasec-alay • v1.4"
            textSize = 11f
            setTextColor(Color.parseColor("#80868B"))
            gravity = Gravity.CENTER
        }
        root.addView(tvFooter)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun showHistoryDialog() {
        val todayRecords = PaymentHistoryManager.getTodayRecords(this)
        val summaries = PaymentHistoryManager.get7DaysSummary(this)

        val sb = StringBuilder()
        sb.append("📅 RESUMEN ÚLTIMOS 7 DÍAS:\n")
        if (summaries.isEmpty()) {
            sb.append("Sin movimientos registrados.\n")
        } else {
            for (s in summaries) {
                sb.append("• ${s.fecha}: S/ ${String.format(Locale.US, "%.2f", s.total)} (${s.count} cobros)\n")
            }
        }

        sb.append("\n🧾 DETALLE DE HOY:\n")
        if (todayRecords.isEmpty()) {
            sb.append("Aún no hay pagos registrados hoy.")
        } else {
            for (r in todayRecords) {
                sb.append("• [${r.hora}] ${r.service} - ${r.remitente}: S/ ${String.format(Locale.US, "%.2f", r.monto)}\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Detalle e Historial (7 días)")
            .setMessage(sb.toString().trim())
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun openYapeBatterySettings() {
        val yapePkg = "com.bcp.innovacxion.yapeapp"
        AlertDialog.Builder(this)
            .setTitle("Evitar que Yape se duerma")
            .setMessage("Se abrirá la configuración de Yape:\n\n1. Entra a 'Ahorro de batería' y elige 'Sin restricciones'.\n2. Activa 'Inicio automático' si está disponible.\n\nEsto evita que el celular congele las notificaciones de Yape.")
            .setPositiveButton("Ir a ajustes de Yape") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$yapePkg")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(this, "No se encontró Yape instalado en este teléfono", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDiagnosticDialog() {
        val isListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val debugPrefs = getSharedPreferences("yaplin_debug", Context.MODE_PRIVATE)
        val lastEvent = debugPrefs.getString("last_event", "Ningún evento recibido aún.")
        val remainingDays = LicenseManager.getRemainingTrialDays(this)
        val statusLic = if (LicenseManager.isPermanentlyActivated(this)) "Licencia Permanente" else "Prueba ($remainingDays días restantes)"

        val report = """
            • Escucha de notificaciones: ${if (isListenerEnabled) "CONECTADO" else "DESCONECTADO"}
            • Estado de licencia: $statusLic
            
            Última notificación capturada:
            $lastEvent
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Diagnóstico Inteligente Yaplin")
            .setMessage(report)
            .setPositiveButton("Reconectar servicio") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NotificationListenerService.requestRebind(ComponentName(this, YaplinListener::class.java))
                }
                Toast.makeText(this, "Reconexión solicitada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (!LicenseManager.isActivated(this)) {
            recreate()
            return
        }
        updateServiceStatusUI()
        refreshDailySummary()

        val filter = IntentFilter("com.rasecalay.yaplin.NUEVO_PAGO")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(paymentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(paymentReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(paymentReceiver)
        } catch (_: Exception) {}
    }

    private fun updateServiceStatusUI() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        val isNotificationEnabled = enabledPackages.contains(packageName)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isBatteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true

        if (isNotificationEnabled) {
            tvStatusBadge.text = "● ESTADO: ACTIVO"
            tvStatusBadge.setTextColor(Color.parseColor("#137333"))
            tvStatusBadge.background = createRoundedDrawable("#E6F4EA", 8)
            tvStatusDesc.text = "Escuchando pagos en segundo plano"

            val serviceIntent = Intent(this, YaplinListener::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            tvStatusBadge.text = "● PERMISO REQUERIDO"
            tvStatusBadge.setTextColor(Color.parseColor("#C5221F"))
            tvStatusBadge.background = createRoundedDrawable("#FCE8E6", 8)
            tvStatusDesc.text = "Activa el acceso a notificaciones para empezar"
        }

        if (!isNotificationEnabled || !isBatteryIgnored) {
            btnPermissions.setTextColor(Color.WHITE)
            btnPermissions.background = createRoundedDrawable("#D93025", 10)
        } else {
            btnPermissions.setTextColor(Color.parseColor("#3C4043"))
            btnPermissions.background = createRoundedDrawable("#E4E6EB", 10)
        }
    }

    private fun openPermissionSettings() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!enabledPackages.contains(packageName)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun refreshDailySummary() {
        val todayRecords = PaymentHistoryManager.getTodayRecords(this)
        val total = PaymentHistoryManager.getTodayTotal(this)
        val count = todayRecords.size

        tvDailyTotal.text = String.format(Locale.US, "S/ %.2f", total)
        tvDailyCount.text = if (count == 1) "1 pago registrado hoy" else "$count pagos registrados hoy"

        if (todayRecords.isNotEmpty()) {
            val latest = todayRecords.first()
            tvLastAmount.text = String.format(Locale.US, "S/ %.2f", latest.monto)
            tvLastDetail.text = "${latest.service} • ${latest.remitente} (${latest.hora})"
        }
    }

    private fun playTestSequence() {
        stopTestAudio()
        testPlaylist = AudioAmountBuilder.buildAudioPlaylist(this, 12.50, true)
        if (testPlaylist.isEmpty()) return

        testIndex = 0
        testCurrentMp = createTestMp(testPlaylist[testIndex++]) ?: return
        prepareNextTestMp()

        testCurrentMp?.setOnCompletionListener {
            it.release()
            advanceTestChain()
        }
        testCurrentMp?.start()
    }

    private fun prepareNextTestMp() {
        if (testIndex < testPlaylist.size) {
            testNextMp = createTestMp(testPlaylist[testIndex++])
            try {
                if (testNextMp != null && testCurrentMp != null) {
                    testCurrentMp?.setNextMediaPlayer(testNextMp)
                }
            } catch (_: Exception) {}
        } else {
            testNextMp = null
        }
    }

    private fun advanceTestChain() {
        testCurrentMp = testNextMp
        testNextMp = null
        if (testCurrentMp != null) {
            prepareNextTestMp()
            testCurrentMp?.setOnCompletionListener {
                it.release()
                advanceTestChain()
            }
        } else {
            stopTestAudio()
        }
    }

    private fun createTestMp(resId: Int): MediaPlayer? {
        return try {
            val afd: AssetFileDescriptor = resources.openRawResourceFd(resId)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
            }
            afd.close()
            mp
        } catch (_: Exception) {
            null
        }
    }

    private fun stopTestAudio() {
        try { testCurrentMp?.stop(); testCurrentMp?.release() } catch (_: Exception) {}
        try { testNextMp?.stop(); testNextMp?.release() } catch (_: Exception) {}
        testCurrentMp = null
        testNextMp = null
    }

    private fun defaultCardParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(9)
        }
    }

    private fun createCardDrawable(bgHex: String, strokeHex: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun createRoundedDrawable(bgHex: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgHex))
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        stopTestAudio()
        super.onDestroy()
    }
}
