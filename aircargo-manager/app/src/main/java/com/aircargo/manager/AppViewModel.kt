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
import com.aircargo.manager.data.MailLog
import com.aircargo.manager.data.diagnose
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
        notifyBooking(MailLog.EV_CREATE, b)
    }

    fun updateStatus(id: String, status: String) {
        val idx = bookings.indexOfFirst { it.id == id }
        if (idx >= 0 && bookings[idx].status != status) {
            val updated = bookings[idx].copy(status = status)
            bookings[idx] = updated
            bookingRepo.saveAll(bookings)
            notifyBooking(MailLog.EV_STATUS, updated)
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
    val mailLogs = mutableStateListOf<MailLog>().apply { addAll(notifyRepo.loadLogs()) }

    fun saveNotify(s: NotifySettings) {
        notifyRepo.save(s)
        notifySettings = s
    }

    fun consumeMailResult() { mailResult = null }

    fun clearMailLogs() {
        mailLogs.clear()
        notifyRepo.saveLogs(mailLogs)
    }

    /** 이벤트별 수신자: 항공사 담당 + (등록 시) 예약자 */
    private fun recipientsFor(event: String, b: Booking, s: NotifySettings): List<String> {
        val list = s.recipients.filter { it.enabled && it.covers(b.airlineCode) }.map { it.email.trim() }.toMutableList()
        if (event == MailLog.EV_CREATE && s.bookerConfirm && b.bookerEmail.isNotBlank()) list += b.bookerEmail.trim()
        return list.distinctBy { it.lowercase() }
    }

    private fun notifyBooking(event: String, b: Booking) {
        val s = notifySettings
        if (!s.enabled) return
        if (event == MailLog.EV_CREATE && !s.onBookingCreated) return
        if (event == MailLog.EV_STATUS && !s.onStatusChanged) return
        val tpl = if (event == MailLog.EV_CREATE) s.tplCreate else s.tplStatus
        dispatch(event, b.id, recipientsFor(event, b, s), tpl.subjectFor(b), tpl.bodyFor(b))
    }

    fun sendTestMail() {
        val s = notifySettings
        dispatch(
            MailLog.EV_TEST, "-",
            s.recipients.filter { it.enabled }.map { it.email.trim() },
            "[Air Cargo Manager] 알림 테스트",
            "Gmail SMTP 알림 설정 테스트 메일입니다.\n발신: ${s.gmail}\n발송일시: ${Booking.formatDateTime(System.currentTimeMillis())}",
        )
    }

    /** 실패 이력 재발송 — 원본 예약이 있으면 현재 양식·수신자로 재구성 */
    fun retry(log: MailLog) {
        val b = bookings.firstOrNull { it.id == log.bookingId }
        if (log.event != MailLog.EV_TEST && b != null) {
            val s = notifySettings
            val tpl = if (log.event == MailLog.EV_CREATE) s.tplCreate else s.tplStatus
            dispatch(log.event, b.id, recipientsFor(log.event, b, s), tpl.subjectFor(b), tpl.bodyFor(b))
        } else {
            dispatch(log.event, log.bookingId, log.to, log.subject, log.body)
        }
    }

    private fun dispatch(event: String, bookingId: String, to: List<String>, subject: String, body: String) {
        val s = notifySettings
        val bad = diagnose(s).filter { !it.ok && !it.label.startsWith("활성 수신자") }
        fun record(ok: Boolean, err: String) {
            val log = MailLog(System.currentTimeMillis(), System.currentTimeMillis(), event, bookingId,
                subject, body, to, ok, err)
            mailLogs.add(0, log)
            while (mailLogs.size > 100) mailLogs.removeAt(mailLogs.lastIndex)
            notifyRepo.saveLogs(mailLogs)
        }
        if (bad.isNotEmpty() || to.isEmpty()) {
            val err = if (to.isEmpty()) "해당 항공사 담당 수신자 없음" else bad.joinToString(", ") { it.label }
            record(false, err)
            mailResult = "메일 발송 실패: $err"
            return
        }
        mailSending = true
        viewModelScope.launch {
            runCatching { GmailSender.send(s, to, subject, body) }
                .onSuccess {
                    record(true, "")
                    mailResult = "알림 메일 발송 완료 (${it}명)"
                }
                .onFailure { e ->
                    val msg = GmailSender.explain(e)
                    record(false, msg)
                    mailResult = msg
                }
            mailSending = false
        }
    }

    companion object {
        const val LOGIN_ID = "admin"
        const val LOGIN_PW = "admin"
    }
}
