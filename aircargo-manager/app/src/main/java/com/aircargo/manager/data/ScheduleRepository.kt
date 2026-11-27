package com.aircargo.manager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Direction(val label: String) { DEPARTURE("출발"), ARRIVAL("도착") }

/**
 * 인천공항 공공데이터(data.go.kr) — 인천국제공항공사_항공기 운항 현황 상세 조회 (statusOfAllFltDeOdp)
 * - 조회 범위: 조회일 기준 D-3 ~ D+6, 여객기(P)/화물기(C) 구분
 * - 활용신청 후 발급받은 serviceKey 필요 (개발계정 일 500건)
 */
enum class ScheduleSource(val label: String, val desc: String, val code: String) {
    PAX("여객편", "여객기(벨리카고) 운항현황 · statusOfAllFltDeOdp", "P"),
    CARGO("화물기", "화물기 운항현황 · statusOfAllFltDeOdp", "C");

    fun url(direction: Direction): String = BASE +
        if (direction == Direction.DEPARTURE) "getFltDeparturesDeOdp" else "getFltArrivalsDeOdp"

    private companion object { const val BASE = "https://apis.data.go.kr/B551177/statusOfAllFltDeOdp/" }
}

data class Flight(
    val flightNo: String,
    val airlineName: String,
    val airport: String,        // 출발편: 목적지 / 도착편: 출발지
    val airportCode: String,
    val scheduleTime: String,   // 표시용 (HH:mm 또는 yyyy-MM-dd HH:mm)
    val estimatedTime: String,
    val status: String,         // 출발/지연/결항 등
    val extra: String,          // 운항요일·시즌기간 등
) {
    val airlineCode: String get() = flightNo.take(2).uppercase()
}

class ScheduleRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var serviceKey: String
        get() = prefs.getString("serviceKey", "") ?: ""
        set(v) = prefs.edit().putString("serviceKey", v.trim()).apply()

    suspend fun fetch(source: ScheduleSource, direction: Direction, date: String = todayYmd()): List<Flight> =
        withContext(Dispatchers.IO) {
            val key = serviceKey
            require(key.isNotBlank()) { "서비스키 미설정" }
            // 포털의 Encoding 키(% 포함)는 그대로, Decoding 키는 인코딩 후 사용
            val encKey = if (key.contains('%')) key else URLEncoder.encode(key, "UTF-8")
            val url = source.url(direction) +
                "?serviceKey=$encKey&pageNo=1&numOfRows=1000&type=json" +
                "&searchdtCode=S&searchDate=$date&searchFrom=0000&searchTo=2400&passengerOrCargo=${source.code}"

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("HTTP $code ${body.take(200)}")
                if (body.trimStart().startsWith("<")) {
                    // 키 오류 등은 XML로 반환되는 경우가 있음
                    val msg = Regex("<returnAuthMsg>(.*?)</returnAuthMsg>|<resultMsg>(.*?)</resultMsg>")
                        .find(body)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }
                    error("API 오류: ${msg ?: body.take(200)}")
                }
                parse(body, direction)
            } finally {
                conn.disconnect()
            }
        }

    private fun parse(json: String, direction: Direction): List<Flight> {
        val root = JSONObject(json)
        val response = root.optJSONObject("response") ?: root
        val header = response.optJSONObject("header")
        val resultCode = header?.optString("resultCode").orEmpty()
        if (resultCode.isNotEmpty() && resultCode != "00") {
            error("API 오류(${resultCode}): ${header?.optString("resultMsg")}")
        }
        val body = response.optJSONObject("body") ?: return emptyList()
        val items: JSONArray = when (val raw = body.opt("items")) {
            is JSONArray -> raw
            is JSONObject -> when (val inner = raw.opt("item")) {
                is JSONArray -> inner
                is JSONObject -> JSONArray().put(inner)
                else -> JSONArray()
            }
            else -> JSONArray()
        }
        return (0 until items.length()).mapNotNull { i ->
            items.optJSONObject(i)?.let { toFlight(Fields(it), direction) }
        }
    }

    /** 응답 필드명이 API마다 상이하므로 대소문자 무시 + 후보 키로 조회 */
    private class Fields(o: JSONObject) {
        private val map: Map<String, String> = buildMap {
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = o.opt(k)
                if (v != null && v != JSONObject.NULL) put(k.lowercase(), v.toString())
            }
        }
        fun get(vararg candidates: String): String =
            candidates.firstNotNullOfOrNull { map[it.lowercase()]?.takeIf { v -> v.isNotBlank() } }.orEmpty()
    }

    private fun toFlight(f: Fields, direction: Direction): Flight? {
        val flightNo = f.get("flightId", "flightid", "fid", "flightNo").uppercase()
        if (flightNo.isBlank()) return null
        val days = listOf(
            "monday" to "월", "tuesday" to "화", "wednesday" to "수", "thursday" to "목",
            "friday" to "금", "saturday" to "토", "sunday" to "일",
        ).filter { (k, _) -> f.get(k).let { it.equals("Y", true) || it == "1" } }
            .joinToString("") { it.second }
        val period = listOf(f.get("firstdate", "startDate"), f.get("lastdate", "endDate"))
            .filter { it.isNotBlank() }.joinToString(" ~ ") { fmtDate(it) }
        val extra = listOf(
            if (days.isNotBlank()) "운항 $days" else "",
            period,
            f.get("gateNumber", "gatenumber", "gate").let { if (it.isNotBlank()) "Gate $it" else "" },
            f.get("terminalId", "terminalid", "terminal"),
        ).filter { it.isNotBlank() }.joinToString(" · ")

        return Flight(
            flightNo = flightNo,
            airlineName = f.get("airline", "airlineNm", "airlineName"),
            airport = f.get("airport", "airportNm", "airportName",
                if (direction == Direction.DEPARTURE) "arrivedKor" else "boardingKor"),
            airportCode = f.get("airportCode", "airportcode", "cityCode"),
            scheduleTime = fmtTime(f.get("scheduleDatetime", "scheduleDateTime", "scheduletime", "st", "std", "scheduleTime")),
            estimatedTime = fmtTime(f.get("estimatedDatetime", "estimatedDateTime", "changeDatetime", "etd", "eta")),
            status = f.get("remark", "status", "flightStatus", "fltStatus"),
            extra = extra,
        )
    }

    private fun fmtTime(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return when (d.length) {
            12 -> "${d.substring(4, 6)}/${d.substring(6, 8)} ${d.substring(8, 10)}:${d.substring(10, 12)}"
            4 -> "${d.substring(0, 2)}:${d.substring(2, 4)}"
            else -> raw
        }
    }

    private fun fmtDate(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.length >= 8) "${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}" else raw
    }

    private fun todayYmd(): String = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())

    /** 서비스키 미설정 시 화면 확인용 예시 데이터 */
    fun sample(direction: Direction): List<Flight> {
        val today = SimpleDateFormat("MM/dd", Locale.KOREA).format(Date())
        val dep = direction == Direction.DEPARTURE
        fun f(no: String, name: String, ap: String, code: String, t: String, st: String) =
            Flight(no, name, ap, code, "$today $t", "", st, "예시 데이터")
        return if (dep) listOf(
            f("CI161", "중화항공", "타이베이", "TPE", "09:30", "출발"),
            f("CI5251", "중화항공", "타이베이", "TPE", "13:10", "탑승중"),
            f("UL471", "스리랑칸항공", "콜롬보", "CMB", "20:45", ""),
            f("WE503", "파라타항공", "나리타", "NRT", "11:20", "출발"),
            f("WE853", "파라타항공", "선전", "SZX", "22:10", ""),
            f("M7621", "MAS AIR", "멕시코시티", "MEX", "02:40", "출발"),
        ) else listOf(
            f("CI160", "중화항공", "타이베이", "TPE", "08:25", "도착"),
            f("UL470", "스리랑칸항공", "콜롬보", "CMB", "07:55", "도착"),
            f("WE854", "파라타항공", "선전", "SZX", "08:05", "도착"),
            f("WE504", "파라타항공", "나리타", "NRT", "15:50", ""),
            f("M7620", "MAS AIR", "멕시코시티", "MEX", "23:30", ""),
        )
    }
}
