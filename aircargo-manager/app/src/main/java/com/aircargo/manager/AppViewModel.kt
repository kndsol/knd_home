package com.aircargo.manager

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aircargo.manager.data.Airline
import com.aircargo.manager.data.Booking
import com.aircargo.manager.data.BookingRepository
import com.aircargo.manager.data.Direction
import com.aircargo.manager.data.Flight
import com.aircargo.manager.data.GmailSender
import com.aircargo.manager.data.NotifyRepository
import com.aircargo.manager.data.NotifySettings
import com.aircargo.manager.data.ScheduleRepository
import com.aircargo.manager.data.ScheduleSource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val bookingRepo = BookingRepository(app)
    private val scheduleRepo = ScheduleRepository(app)
    private val notifyRepo = NotifyRepository(app)

    // ── 로그인 ──────────────────────────────
    var loggedIn by mutableStateOf(false)
        private set

    fun login(id: String, pw: String): Boolean {
        val ok = id.trim() == LOGIN_ID && pw == LOGIN_PW
        loggedIn = ok
        return ok
    }

    fun logout() { loggedIn = false }

    // ── 공통 : 선택 항공사 (null = 전체) ──────
    var selectedAirline by mutableStateOf<Airline?>(null)

    // ── B. 화물예약 ─────────────────────────
    val bookings = mutableStateListOf<Booking>().apply { addAll(bookingRepo.loadAll()) }

    fun newBookingId(): String = bookingRepo.nextId()

    fun addBooking(b: Booking) {
        bookings.add(0, b)
        bookingRepo.saveAll(bookings)
        if (notifySettings.onBookingCreated) {
            sendNotification("[화물예약 등록] ${b.flightNo} ${b.flightDate} ${b.id}", b.toShareText(), auto = true)
        }
    }

    fun updateStatus(id: String, status: String) {
        val idx = bookings.indexOfFirst { it.id == id }
        if (idx >= 0 && bookings[idx].status != status) {
            val updated = bookings[idx].copy(status = status)
            bookings[idx] = updated
            bookingRepo.saveAll(bookings)
            if (notifySettings.onStatusChanged) {
                sendNotification(
                    "[화물예약 $status] ${updated.flightNo} ${updated.flightDate} ${updated.id}",
                    "예약 상태가 '$status'(으)로 변경되었습니다.\n\n" + updated.toShareText(),
                    auto = true,
                )
            }
        }
    }

    fun deleteBooking(id: String) {
        bookings.removeAll { it.id == id }
        bookingRepo.saveAll(bookings)
    }

    val todayBookingCount: Int
        get() {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
            return bookings.count { it.flightDate == today && it.status != Booking.STATUS_CANCELLED }
        }

    // ── A. 운항스케쥴 ───────────────────────
    var serviceKey by mutableStateOf(scheduleRepo.serviceKey)
        private set

    fun saveServiceKey(key: String) {
        scheduleRepo.serviceKey = key
        serviceKey = scheduleRepo.serviceKey
    }

    var scheduleSource by mutableStateOf(ScheduleSource.PAX_DAILY)
    var direction by mutableStateOf(Direction.DEPARTURE)
    var flights by mutableStateOf<List<Flight>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var scheduleMessage by mutableStateOf<String?>(null)
        private set
    var isSample by mutableStateOf(false)
        private set

    fun loadSchedule() {
        if (serviceKey.isBlank()) {
            flights = scheduleRepo.sample(direction)
            isSample = true
            scheduleMessage = "서비스키 미설정 — 예시 데이터 표시 (설정에서 data.go.kr 키 입력)"
            return
        }
        loading = true
        scheduleMessage = null
        viewModelScope.launch {
            runCatching { scheduleRepo.fetch(scheduleSource, direction) }
                .onSuccess {
                    flights = it
                    isSample = false
                    scheduleMessage = if (it.isEmpty()) "조회 결과 없음" else null
                }
                .onFailure {
                    flights = emptyList()
                    scheduleMessage = "조회 실패: ${it.message}"
                }
            loading = false
        }
    }

    // ── 알림 (Gmail SMTP) ───────────────────
    var notifySettings by mutableStateOf(notifyRepo.load())
        private set
    var mailSending by mutableStateOf(false)
        private set
    /** 최근 발송 결과 (화면 Toast 표시 후 consumeMailResult) */
    var mailResult by mutableStateOf<String?>(null)
        private set

    fun saveNotify(s: NotifySettings) {
        notifyRepo.save(s)
        notifySettings = s
    }

    fun consumeMailResult() { mailResult = null }

    fun sendTestMail() = sendNotification(
        "[Air Cargo Manager] 알림 테스트",
        "Gmail SMTP 알림 설정 테스트 메일입니다.\n발송일시: ${Booking.formatDateTime(System.currentTimeMillis())}",
        auto = false,
    )

    /** auto=true : 이벤트 자동 알림 → 알림 비활성/미설정 시 무시 */
    private fun sendNotification(subject: String, body: String, auto: Boolean) {
        val s = notifySettings
        if (auto && (!s.enabled || !s.isConfigured)) return
        mailSending = true
        viewModelScope.launch {
            mailResult = runCatching { GmailSender.send(s, subject, body) }
                .fold(
                    onSuccess = { "알림 메일 발송 완료 (${it}명)" },
                    onFailure = { e ->
                        val m = e.message.orEmpty()
                        if (m.contains("535") || m.contains("Username and Password", true))
                            "메일 발송 실패: Gmail 인증 오류 — 2단계 인증 + 앱 비밀번호 확인"
                        else "메일 발송 실패: ${m.take(150)}"
                    },
                )
            mailSending = false
        }
    }

    companion object {
        const val LOGIN_ID = "admin"
        const val LOGIN_PW = "admin"
    }
}
