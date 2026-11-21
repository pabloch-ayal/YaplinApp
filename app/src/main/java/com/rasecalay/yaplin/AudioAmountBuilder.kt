package com.rasecalay.yaplin

import android.content.Context

object AudioAmountBuilder {

    private const val PREFS_VOICE = "yaplin_voice_prefs"
    private const val KEY_VOICE_GENDER = "voice_gender" // "f" = Mujer, "m" = Hombre

    fun setVoiceGender(context: Context, gender: String) {
        val clean = if (gender == "m") "m" else "f"
        context.getSharedPreferences(PREFS_VOICE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VOICE_GENDER, clean)
            .apply()
    }

    fun getVoiceGender(context: Context): String {
        return context.getSharedPreferences(PREFS_VOICE, Context.MODE_PRIVATE)
            .getString(KEY_VOICE_GENDER, "f") ?: "f"
    }

    fun buildAudioPlaylist(context: Context, amountDouble: Double, isYape: Boolean): List<Int> {
        val playlist = mutableListOf<Int>()
        val prefix = getVoiceGender(context) // "f" o "m"

        val introKey = if (isYape) "${prefix}_yape" else "${prefix}_plin"
        getRawId(context, introKey)?.let { playlist.add(it) }

        val totalCentavos = Math.round(amountDouble * 100).toInt()
        val entero = totalCentavos / 100
        val centavos = totalCentavos % 100

        if (entero == 1) {
            getRawId(context, "${prefix}_1_sol")?.let { playlist.add(it) }
        } else if (entero > 1) {
            playlist.addAll(resolveNumberToAudios(context, prefix, entero))
            getRawId(context, "${prefix}_soles")?.let { playlist.add(it) }
        }

        if (centavos > 0) {
            if (entero > 0) {
                getRawId(context, "${prefix}_con")?.let { playlist.add(it) }
            }
            playlist.addAll(resolveNumberToAudios(context, prefix, centavos))
            getRawId(context, "${prefix}_centimos")?.let { playlist.add(it) }
        }

        return playlist
    }

    private fun resolveNumberToAudios(context: Context, prefix: String, number: Int): List<Int> {
        val list = mutableListOf<Int>()
        if (number <= 0) return list

        var resto = number

        // Centenas (100 a 999)
        if (resto >= 100) {
            if (resto == 100) {
                getRawId(context, "${prefix}_100")?.let { list.add(it) }
                return list
            } else if (resto in 101..199) {
                getRawId(context, "${prefix}_ciento")?.let { list.add(it) }
                resto %= 100
            } else {
                val centena = (resto / 100) * 100
                getRawId(context, "${prefix}_$centena")?.let { list.add(it) }
                resto %= 100
            }
        }

        if (resto == 0) return list

        // 1 a 29 (en una sola palabra)
        if (resto in 1..29) {
            getRawId(context, "${prefix}_$resto")?.let { list.add(it) }
            return list
        }

        // 30 a 99
        if (resto in 30..99) {
            val decena = (resto / 10) * 10
            val unidad = resto % 10
            getRawId(context, "${prefix}_$decena")?.let { list.add(it) }
            if (unidad > 0) {
                getRawId(context, "${prefix}_y")?.let { list.add(it) }
                getRawId(context, "${prefix}_$unidad")?.let { list.add(it) }
            }
        }

        return list
    }

    private fun getRawId(context: Context, resourceName: String): Int? {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        return if (resId != 0) resId else null
    }
}
