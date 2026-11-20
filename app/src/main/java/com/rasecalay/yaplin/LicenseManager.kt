package com.rasecalay.yaplin

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

object LicenseManager {

    private const val PREFS_NAME = "yaplin_license_prefs"
    private const val KEY_IS_ACTIVATED = "is_activated"
    private const val KEY_FIRST_LAUNCH = "first_launch_timestamp"
    private const val SECRET_SALT = "YAPLIN_RASEC_ALAY_2026_SECURE"
    private const val TRIAL_DAYS = 15L

    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN_DEVICE_ID"

        val hash = sha256(androidId + "_ID_SALT")
        return hash.take(6).uppercase(Locale.US)
    }

    fun generateExpectedKey(deviceId: String): String {
        val cleanId = deviceId.trim().uppercase(Locale.US)
        val raw = "$cleanId:$SECRET_SALT"
        val hash = sha256(raw)
        return hash.take(6).uppercase(Locale.US)
    }

    fun validateAndActivate(context: Context, inputKey: String): Boolean {
        val deviceId = getDeviceId(context)
        val expected = generateExpectedKey(deviceId)
        val cleanInput = inputKey.trim().uppercase(Locale.US)

        return if (cleanInput == expected) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_ACTIVATED, true).apply()
            true
        } else {
            false
        }
    }

    // Devuelve true si ya pagó licencia definitiva
    fun isPermanentlyActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_ACTIVATED, false)
    }

    // Devuelve true si tiene licencia definitiva O si sigue dentro de los 15 días de prueba
    fun isActivated(context: Context): Boolean {
        if (isPermanentlyActivated(context)) return true
        return getRemainingTrialDays(context) > 0
    }

    fun getRemainingTrialDays(context: Context): Int {
        if (isPermanentlyActivated(context)) return 999

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        var firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, 0L)

        if (firstLaunch == 0L) {
            firstLaunch = now
            prefs.edit().putLong(KEY_FIRST_LAUNCH, firstLaunch).apply()
        }

        val elapsedMillis = now - firstLaunch
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis)
        val remaining = (TRIAL_DAYS - elapsedDays).toInt()

        return if (remaining > 0) remaining else 0
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
