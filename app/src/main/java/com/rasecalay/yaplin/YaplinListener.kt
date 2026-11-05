package com.rasecalay.yaplin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class YaplinListener : NotificationListenerService() {

    private var currentMp: MediaPlayer? = null
    private var nextMp: MediaPlayer? = null
    private var currentPlaylist: List<Int> = emptyList()
    private var nextIndex = 0

    companion object {
        private const val TAG = "YaplinListener"
        private const val CHANNEL_ID = "yaplin_persistent_channel"
        private const val NOTIFICATION_ID = 8801
        private const val PKG_YAPE = "com.bcp.innovacxion.yapeapp"
        private const val PKG_PLIN_BBVA = "com.bbva.nxt_peru"
        private const val PKG_PLIN_INTERBANK = "pe.com.interbank.mobilebanking"
        private const val PKG_PLIN_SCOTIA = "com.scotiabank.peru.mobile"
    }

    override fun onCreate() {
        super.onCreate()
        startPersistentForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPersistentForeground()
        return START_STICKY
    }

    private fun startPersistentForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor de Cobros Activo",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Mantiene activo el lector automático de pagos Yaplin"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yaplin en ejecución")
            .setContentText("Escuchando notificaciones de Yape y Plin en segundo plano")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if (!LicenseManager.isActivated(this)) return

        val pkg = sbn.packageName ?: return
        val isYape = pkg == PKG_YAPE
        val isPlin = pkg == PKG_PLIN_BBVA || pkg == PKG_PLIN_INTERBANK || pkg == PKG_PLIN_SCOTIA

        if (!isYape && !isPlin) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val fullContent = "$title $text"

        val pattern = Pattern.compile("(?i)S\\/?\\.?\\s*(\\d+(?:[.,]\\d{1,2})?)")
        val matcher = pattern.matcher(fullContent)

        if (matcher.find()) {
            val rawAmount = matcher.group(1)?.replace(",", ".") ?: "0"
            val amountValue = rawAmount.toDoubleOrNull() ?: 0.0

            if (amountValue > 0.0) {
                val senderName = if (title.isNotBlank() && !title.contains("Yape", ignoreCase = true) && !title.contains("Plin", ignoreCase = true)) {
                    title.trim()
                } else {
                    "Cliente"
                }

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                val serviceName = if (isYape) "Yape" else "Plin"

                PaymentHistoryManager.addPayment(this, senderName, amountValue, serviceName, currentTime)

                vibrateAlert()
                playChainPlaylist(AudioAmountBuilder.buildAudioPlaylist(this, amountValue, isYape))

                val broadcastIntent = Intent("com.rasecalay.yaplin.NUEVO_PAGO").apply {
                    setPackage(packageName)
                    putExtra("service", serviceName)
                    putExtra("amount", String.format(Locale.US, "%.2f", amountValue))
                    putExtra("time", currentTime)
                    putExtra("sender", senderName)
                }
                sendBroadcast(broadcastIntent)
            }
        }
    }

    private fun playChainPlaylist(playlist: List<Int>) {
        stopPlayback()
        if (playlist.isEmpty()) return

        currentPlaylist = playlist
        nextIndex = 0

        currentMp = createPlayerForResource(currentPlaylist[nextIndex++])
        if (currentMp == null) {
            stopPlayback()
            return
        }

        prepareNextInChain()

        currentMp?.setOnCompletionListener {
            it.release()
            advanceChain()
        }

        currentMp?.start()
    }

    private fun prepareNextInChain() {
        if (nextIndex < currentPlaylist.size) {
            nextMp = createPlayerForResource(currentPlaylist[nextIndex++])
            try {
                if (nextMp != null && currentMp != null) {
                    currentMp?.setNextMediaPlayer(nextMp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en setNextMediaPlayer: ${e.message}")
            }
        } else {
            nextMp = null
        }
    }

    private fun advanceChain() {
        currentMp = nextMp
        nextMp = null

        if (currentMp != null) {
            prepareNextInChain()
            currentMp?.setOnCompletionListener {
                it.release()
                advanceChain()
            }
        } else {
            stopPlayback()
        }
    }

    private fun createPlayerForResource(resId: Int): MediaPlayer? {
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
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo preparar el recurso $resId: ${e.message}")
            null
        }
    }

    private fun stopPlayback() {
        try {
            currentMp?.stop()
            currentMp?.release()
        } catch (_: Exception) {}
        try {
            nextMp?.stop()
            nextMp?.release()
        } catch (_: Exception) {}
        currentMp = null
        nextMp = null
        currentPlaylist = emptyList()
        nextIndex = 0
    }

    private fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(400)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al vibrar: ${e.message}")
        }
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }
}
