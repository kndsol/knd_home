package com.aircargo.manager.data

/**
 * 관리 대상 항공사 정의.
 * - cargoTrackingUrl : 화물추적(C) 시 WebView로 연결하는 각 항공사 화물 웹
 * - awbPrefix        : AWB 앞 3자리 (입력 보조용). 미확인 항공사는 빈 값
 *
 * ※ URL/Prefix는 운영 전 각 항공사 GSA·지점에 최종 확인 필요.
 */
data class Airline(
    val code: String,
    val nameKo: String,
    val nameEn: String,
    val awbPrefix: String,
    val cargoTrackingUrl: String,
)

object Airlines {
    val all: List<Airline> = listOf(
        Airline(
            code = "CI",
            nameKo = "중화항공",
            nameEn = "China Airlines",
            awbPrefix = "297",
            cargoTrackingUrl = "https://cargo.china-airlines.com/",
        ),
        Airline(
            code = "UL",
            nameKo = "스리랑칸항공",
            nameEn = "SriLankan Airlines",
            awbPrefix = "603",
            cargoTrackingUrl = "https://www.srilankancargo.com/",
        ),
        Airline(
            code = "WE",
            nameKo = "파라타항공",
            nameEn = "Parata Air",
            awbPrefix = "",
            cargoTrackingUrl = "https://www.parataair.com/",
        ),
        Airline(
            code = "M7",
            nameKo = "MAS AIR",
            nameEn = "MASkargo",
            awbPrefix = "232",
            cargoTrackingUrl = "https://www.maskargo.com/en/shipment-tracking.html",
        ),
    )

    fun byCode(code: String?): Airline? = all.firstOrNull { it.code == code }
}

/** 외부 연결 URL 모음 */
object ExternalLinks {
    /** 인천공항 운항정보 (API 키 미설정 시 대체 화면) */
    const val ICN_FLIGHT_WEB = "https://www.airport.kr/"

    /** Flightradar24 - 인천공항 */
    const val FR24_ICN = "https://www.flightradar24.com/airport/icn"

    /** Flightradar24 - 편명 검색 (예: ci161) */
    fun fr24Flight(flightNo: String): String =
        "https://www.flightradar24.com/data/flights/" + flightNo.lowercase().replace(" ", "")
}
