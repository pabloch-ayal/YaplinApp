package com.rasecalay.yaplin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PaymentRecord(
    val fecha: String,
    val hora: String,
    val remitente: String,
    val monto: Double,
    val service: String
)

data class DaySummary(
    val fecha: String,
    val total: Double,
    val count: Int
)

object PaymentHistoryManager {
    private const val PREFS_NAME = "yaplin_history_prefs"
    private const val KEY_RECORDS_JSON = "payment_records_json"

    fun addPayment(context: Context, remitente: String, monto: Double, service: String, hora: String): PaymentRecord {
        purgeOldRecords(context)
        val todayStr = getTodayDateString()
        val record = PaymentRecord(todayStr, hora, remitente, monto, service)

        val list = getAllRecords(context).toMutableList()
        list.add(0, record)
        saveRecords(context, list)
        return record
    }

    fun getTodayRecords(context: Context): List<PaymentRecord> {
        purgeOldRecords(context)
        val todayStr = getTodayDateString()
        return getAllRecords(context).filter { it.fecha == todayStr }
    }

    fun getTodayTotal(context: Context): Double {
        return getTodayRecords(context).sumOf { it.monto }
    }

    fun get7DaysSummary(context: Context): List<DaySummary> {
        purgeOldRecords(context)
        val all = getAllRecords(context)
        return all.groupBy { it.fecha }
            .map { (fecha, records) ->
                DaySummary(fecha, records.sumOf { it.monto }, records.size)
            }
            .sortedByDescending { it.fecha }
    }

    fun purgeOldRecords(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECORDS_JSON, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val cutoff = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

            val kept = mutableListOf<PaymentRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val fecha = obj.optString("fecha")
                if (fecha >= cutoff) {
                    kept.add(
                        PaymentRecord(
                            fecha = fecha,
                            hora = obj.optString("hora"),
                            remitente = obj.optString("remitente", "Cliente"),
                            monto = obj.optDouble("monto", 0.0),
                            service = obj.optString("service", "Pago")
                        )
                    )
                }
            }
            saveRecords(context, kept)
        } catch (_: Exception) {}
    }

    private fun getAllRecords(context: Context): List<PaymentRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECORDS_JSON, null) ?: return emptyList()
        val list = mutableListOf<PaymentRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PaymentRecord(
                        fecha = obj.optString("fecha"),
                        hora = obj.optString("hora"),
                        remitente = obj.optString("remitente", "Cliente"),
                        monto = obj.optDouble("monto", 0.0),
                        service = obj.optString("service", "Pago")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveRecords(context: Context, list: List<PaymentRecord>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("fecha", item.fecha)
            obj.put("hora", item.hora)
            obj.put("remitente", item.remitente)
            obj.put("monto", item.monto)
            obj.put("service", item.service)
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECORDS_JSON, arr.toString()).apply()
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
