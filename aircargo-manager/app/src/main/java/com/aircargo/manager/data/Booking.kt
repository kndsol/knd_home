package com.aircargo.manager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 화물예약(B) 고객 입력 항목 */
data class Booking(
    val id: String,
    val createdAt: Long,
    // 1. 항공사
    val airlineCode: String,
    // 2. 운송 정보
    val flightNo: String,
    val flightDate: String,      // yyyy-MM-dd
    val pieces: Int,
    val weightKg: Double,
    val item: String,
    val destination: String,
    val remark: String,
    val tempControl: Boolean,    // 온도유지
    val dryIce: Boolean,         // Dry Ice 탑재
    // 3. 예약자 정보
    val bookerCompany: String,
    val bookerName: String,
    val bookerPhone: String,
    val bookerEmail: String = "",   // 접수 확인 메일 수신 (선택)
    val status: String = STATUS_REQUESTED,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("createdAt", createdAt)
        put("airlineCode", airlineCode); put("flightNo", flightNo); put("flightDate", flightDate)
        put("pieces", pieces); put("weightKg", weightKg); put("item", item)
        put("destination", destination); put("remark", remark)
        put("tempControl", tempControl); put("dryIce", dryIce)
        put("bookerCompany", bookerCompany); put("bookerName", bookerName); put("bookerPhone", bookerPhone)
        put("bookerEmail", bookerEmail)
        put("status", status)
    }

    /** 공유(메일/메신저) 본문 */
    fun toShareText(): String {
        val airline = Airlines.byCode(airlineCode)
        val handling = buildList {
            if (tempControl) add("온도유지")
            if (dryIce) add("Dry Ice 탑재")
        }.joinToString(", ").ifBlank { "-" }
        return buildString {
            appendLine("[화물 예약 요청] $id")
            appendLine("항공사    : ${airline?.nameKo ?: airlineCode} ($airlineCode)")
            appendLine("편명/날짜 : $flightNo / $flightDate")
            appendLine("Pcs/Weight: $pieces PCS / ${formatWeight(weightKg)} KG")
            appendLine("Item      : $item")
            appendLine("Dest.     : $destination")
            appendLine("특수취급  : $handling")
            appendLine("Remark    : ${remark.ifBlank { "-" }}")
            appendLine("예약자    : $bookerCompany / $bookerName / $bookerPhone" + if (bookerEmail.isNotBlank()) " / $bookerEmail" else "")
            append("접수일시  : ${formatDateTime(createdAt)}")
        }
    }

    companion object {
        const val STATUS_REQUESTED = "요청"
        const val STATUS_CONFIRMED = "확정"
        const val STATUS_CANCELLED = "취소"

        fun fromJson(o: JSONObject) = Booking(
            id = o.optString("id"),
            createdAt = o.optLong("createdAt"),
            airlineCode = o.optString("airlineCode"),
            flightNo = o.optString("flightNo"),
            flightDate = o.optString("flightDate"),
            pieces = o.optInt("pieces"),
            weightKg = o.optDouble("weightKg", 0.0),
            item = o.optString("item"),
            destination = o.optString("destination"),
            remark = o.optString("remark"),
            tempControl = o.optBoolean("tempControl"),
            dryIce = o.optBoolean("dryIce"),
            bookerCompany = o.optString("bookerCompany"),
            bookerName = o.optString("bookerName"),
            bookerPhone = o.optString("bookerPhone"),
            bookerEmail = o.optString("bookerEmail"),
            status = o.optString("status", STATUS_REQUESTED),
        )

        fun formatWeight(w: Double): String =
            if (w % 1.0 == 0.0) w.toLong().toString() else String.format(Locale.US, "%.1f", w)

        fun formatDateTime(ms: Long): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(ms))
    }
}

/**
 * 예약 저장소 (단말 로컬 저장).
 * 서버 연동 시 이 클래스만 API 호출로 교체.
 */
class BookingRepository(context: Context) {
    private val prefs = context.getSharedPreferences("bookings", Context.MODE_PRIVATE)

    fun loadAll(): List<Booking> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Booking.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList()).sortedByDescending { it.createdAt }
    }

    fun saveAll(list: List<Booking>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun nextId(): String {
        val date = SimpleDateFormat("yyMMdd", Locale.KOREA).format(Date())
        val seq = prefs.getInt("seq_$date", 0) + 1
        prefs.edit().putInt("seq_$date", seq).apply()
        return "BK$date-" + seq.toString().padStart(3, '0')
    }

    private companion object { const val KEY = "items" }
}
