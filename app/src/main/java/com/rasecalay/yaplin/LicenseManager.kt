package com.rasecalay.yaplin

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.security.SecureRandom

object LicenseManager {
    private const val PREFS_NAME = "yaplin_license_prefs"
    private const val KEY_ACTIVATED = "is_activated"
    private const val KEY_DEVICE_ID = "device_id"
    private const val MASTER_KEY = "CONOSUR2026"
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACTIVATED, false)
    }

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrEmpty()) {
            val random = SecureRandom()
            val sb = java.lang.StringBuilder(6)
            for (i in 0 until 6) {
                sb.append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
            id = sb.toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun generateValidKey(deviceId: String): String {
        val id = deviceId.trim().uppercase()
        var h1 = 5381
        var h2 = 0x1234567

        for (c in id) {
            h1 = ((h1 shl 5) + h1) xor c.code
            h2 = ((h2 shl 7) + h2) + c.code
        }

        var combinada = Math.abs(h1 xor h2)
        val sb = java.lang.StringBuilder()

        for (i in 0 until 6) {
            sb.append(ALPHABET[combinada % ALPHABET.length])
            combinada = (combinada / ALPHABET.length) + (id[i % id.length].code * 17)
        }
        return sb.toString()
    }

    fun validateAndActivate(context: Context, enteredKey: String): Boolean {
        val cleanKey = enteredKey.trim().uppercase()
        if (cleanKey == MASTER_KEY) {
            markAsActivated(context)
            return true
        }
        val expected = generateValidKey(getOrCreateDeviceId(context))
        if (cleanKey == expected) {
            markAsActivated(context)
            return true
        }
        return false
    }

    private fun markAsActivated(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ACTIVATED, true).apply()
    }

    fun openWhatsAppActivation(context: Context, deviceId: String) {
        try {
            val message = "Hola, deseo activar Yaplin en mi celular. Mi ID es: $deviceId"
            val url = "https://api.whatsapp.com/send?phone=51939525004&text=" + Uri.encode(message)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/51928625004"))
            context.startActivity(fallback)
        }
    }
}
