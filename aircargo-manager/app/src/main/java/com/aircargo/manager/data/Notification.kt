package com.aircargo.manager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Date
import java.util.Properties
import javax.mail.Address
import javax.mail.AuthenticationFailedException
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.SendFailedException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/** 알림 수신자 — scopes: "ALL" 또는 항공사 코드 목록 (항공사별 담당) */
data class Recipient(
    val email: String,
    val name: String,
    val enabled: Boolean = true,
    val scopes: List<String> = listOf(SCOPE_ALL),
) {
    fun covers(airlineCode: String?): Boolean =
        SCOPE_ALL in scopes || (airlineCode != null && airlineCode in scopes)

    companion object { const val SCOPE_ALL = "ALL" }
}

/** 메일 양식 — {변수} 치환 */
data class MailTemplate(val subject: String, val body: String) {
    fun subjectFor(b: Booking) = fill(subject, b)
    fun bodyFor(b: Booking) = fill(body, b)

    companion object {
        val VARIABLES = listOf("예약번호", "항공사", "편명", "날짜", "PCS", "중량", "품목", "목적지", "상태", "예약자", "회사", "전화")

        val DEFAULT_CREATE = MailTemplate(
            "[예약접수] {편명} {날짜} {예약번호}",
            "화물 예약이 접수되었습니다.\n\n예약번호: {예약번호}\n항공사: {항공사}\n편명/날짜: {편명} / {날짜}\n" +
                "Pcs/Weight: {PCS} PCS / {중량} KG\nItem: {품목}\nDest.: {목적지}\n예약자: {회사} {예약자} ({전화})",
        )
        val DEFAULT_STATUS = MailTemplate(
            "[예약{상태}] {편명} {날짜} {예약번호}",
            "예약 상태가 '{상태}'(으)로 변경되었습니다.\n\n예약번호: {예약번호}\n편명/날짜: {편명} / {날짜}\n" +
                "Pcs/Weight: {PCS} PCS / {중량} KG\nDest.: {목적지}",
        )

        fun fill(t: String, b: Booking): String {
            val a = Airlines.byCode(b.airlineCode)
            val map = mapOf(
                "예약번호" to b.id,
                "항공사" to (a?.let { "${it.nameKo} (${it.code})" } ?: b.airlineCode),
                "편명" to b.flightNo,
                "날짜" to b.flightDate,
                "PCS" to b.pieces.toString(),
                "중량" to Booking.formatWeight(b.weightKg),
                "품목" to b.item,
                "목적지" to b.destination,
                "상태" to b.status,
                "예약자" to b.bookerName,
                "회사" to b.bookerCompany,
                "전화" to b.bookerPhone,
            )
            return Regex("\\{([^}]+)\\}").replace(t) { m -> map[m.groupValues[1]] ?: m.value }
        }
    }
}

/** 알림 설정 (Gmail SMTP) */
data class NotifySettings(
    val enabled: Boolean = false,
    val gmail: String = "",           // 발신 Gmail 계정
    val appPassword: String = "",     // Google 앱 비밀번호 16자리 (일반 비밀번호 불가)
    val senderName: String = "Air Cargo Manager",
    val onBookingCreated: Boolean = true,
    val onStatusChanged: Boolean = true,
    val bookerConfirm: Boolean = true, // 예약자 이메일로 접수 확인 메일
    val recipients: List<Recipient> = emptyList(),
    val tplCreate: MailTemplate = MailTemplate.DEFAULT_CREATE,
    val tplStatus: MailTemplate = MailTemplate.DEFAULT_STATUS,
) {
    val isConfigured: Boolean
        get() = diagnose(this).all { it.ok }
}

/** 설정 점검 항목 */
data class Check(val ok: Boolean, val label: String, val fix: String)

fun diagnose(s: NotifySettings): List<Check> {
    val pw = s.appPassword.replace(" ", "")
    return listOf(
        Check(
            Regex("^[^@\\s]+@(gmail|googlemail)\\.com$", RegexOption.IGNORE_CASE).matches(s.gmail.trim()),
            "발신 주소가 Gmail 계정",
            "xxx@gmail.com 형식의 Gmail 주소 입력",
        ),
        Check(pw.length == 16, "앱 비밀번호 16자리 (${pw.length}/16)",
            "Google 계정 > 보안 > 앱 비밀번호에서 발급한 16자리 입력 (일반 비밀번호 불가)"),
        Check(pw.length != 16 || pw.all { it.isLetter() }, "앱 비밀번호 형식 (영문자)",
            "앱 비밀번호는 영문자 16자리 — 숫자·기호가 있으면 일반 비밀번호일 가능성"),
        Check(s.recipients.any { it.enabled }, "활성 수신자 1명 이상", "수신자 추가 또는 활성화"),
    )
}

/** 발송 이력 */
data class MailLog(
    val id: Long,
    val time: Long,
    val event: String,          // create / status / test
    val bookingId: String,
    val subject: String,
    val body: String,
    val to: List<String>,
    val ok: Boolean,
    val error: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("time", time).put("event", event).put("bookingId", bookingId)
        .put("subject", subject).put("body", body).put("to", JSONArray(to))
        .put("ok", ok).put("error", error)

    companion object {
        const val EV_CREATE = "create"
        const val EV_STATUS = "status"
        const val EV_TEST = "test"

        fun fromJson(o: JSONObject): MailLog {
            val arr = o.optJSONArray("to") ?: JSONArray()
            return MailLog(
                id = o.optLong("id"), time = o.optLong("time"), event = o.optString("event"),
                bookingId = o.optString("bookingId"), subject = o.optString("subject"),
                body = o.optString("body"), to = (0 until arr.length()).map { arr.optString(it) },
                ok = o.optBoolean("ok"), error = o.optString("error"),
            )
        }
    }
}

class NotifyRepository(context: Context) {
    private val prefs = context.getSharedPreferences("notify", Context.MODE_PRIVATE)

    fun load(): NotifySettings {
        val raw = prefs.getString("settings", null) ?: return NotifySettings()
        return runCatching {
            val o = JSONObject(raw)
            val arr = o.optJSONArray("recipients") ?: JSONArray()
            NotifySettings(
                enabled = o.optBoolean("enabled"),
                gmail = o.optString("gmail"),
                appPassword = o.optString("appPassword"),
                senderName = o.optString("senderName", "Air Cargo Manager"),
                onBookingCreated = o.optBoolean("onBookingCreated", true),
                onStatusChanged = o.optBoolean("onStatusChanged", true),
                bookerConfirm = o.optBoolean("bookerConfirm", true),
                recipients = (0 until arr.length()).map {
                    val r = arr.getJSONObject(it)
                    val sc = r.optJSONArray("scopes")
                    Recipient(
                        r.optString("email"), r.optString("name"), r.optBoolean("enabled", true),
                        if (sc == null || sc.length() == 0) listOf(Recipient.SCOPE_ALL)
                        else (0 until sc.length()).map { i -> sc.optString(i) },
                    )
                },
                tplCreate = tpl(o.optJSONObject("tplCreate"), MailTemplate.DEFAULT_CREATE),
                tplStatus = tpl(o.optJSONObject("tplStatus"), MailTemplate.DEFAULT_STATUS),
            )
        }.getOrDefault(NotifySettings())
    }

    private fun tpl(o: JSONObject?, def: MailTemplate) =
        if (o == null) def else MailTemplate(o.optString("subject", def.subject), o.optString("body", def.body))

    fun save(s: NotifySettings) {
        val arr = JSONArray()
        s.recipients.forEach {
            arr.put(
                JSONObject().put("email", it.email).put("name", it.name)
                    .put("enabled", it.enabled).put("scopes", JSONArray(it.scopes))
            )
        }
        val o = JSONObject()
            .put("enabled", s.enabled)
            .put("gmail", s.gmail)
            .put("appPassword", s.appPassword)
            .put("senderName", s.senderName)
            .put("onBookingCreated", s.onBookingCreated)
            .put("onStatusChanged", s.onStatusChanged)
            .put("bookerConfirm", s.bookerConfirm)
            .put("recipients", arr)
            .put("tplCreate", JSONObject().put("subject", s.tplCreate.subject).put("body", s.tplCreate.body))
            .put("tplStatus", JSONObject().put("subject", s.tplStatus.subject).put("body", s.tplStatus.body))
        prefs.edit().putString("settings", o.toString()).apply()
    }

    fun loadLogs(): List<MailLog> {
        val raw = prefs.getString("logs", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { MailLog.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun saveLogs(list: List<MailLog>) {
        val arr = JSONArray()
        list.take(MAX_LOGS).forEach { arr.put(it.toJson()) }
        prefs.edit().putString("logs", arr.toString()).apply()
    }

    private companion object { const val MAX_LOGS = 100 }
}

/** Gmail SMTP 발송 (smtp.gmail.com:587, STARTTLS) */
object GmailSender {
    private const val HOST = "smtp.gmail.com"
    private const val PORT = "587"

    suspend fun send(s: NotifySettings, to: List<String>, subject: String, body: String) =
        withContext(Dispatchers.IO) {
            require(s.gmail.isNotBlank() && s.appPassword.isNotBlank()) { "발신 Gmail 계정/앱 비밀번호 미설정" }
            require(to.isNotEmpty()) { "수신자 없음" }

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
                put("mail.smtp.host", HOST)
                put("mail.smtp.port", PORT)
                put("mail.smtp.ssl.trust", HOST)
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "15000")
                put("mail.smtp.writetimeout", "15000")
            }
            val password = s.appPassword.replace(" ", "")
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(s.gmail.trim(), password)
            })
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(s.gmail.trim(), s.senderName.ifBlank { s.gmail }, "UTF-8"))
                setRecipients(
                    Message.RecipientType.TO,
                    to.map<String, Address> { InternetAddress(it.trim()) }.toTypedArray(),
                )
                setSubject(subject, "UTF-8")
                setText(body, "UTF-8")
                sentDate = Date()
            }
            Transport.send(msg)
            to.size
        }

    /** 예외 → 사용자 안내 문구 (오류 원인 구분) */
    fun explain(e: Throwable): String {
        var c: Throwable? = e
        while (c != null) {
            when (c) {
                is AuthenticationFailedException ->
                    return "Gmail 인증 실패 — 2단계 인증 활성화 및 앱 비밀번호(16자리) 확인"
                is UnknownHostException -> return "네트워크 연결 없음 — 인터넷 연결 확인"
                is ConnectException -> return "SMTP 서버 연결 실패 — 네트워크/방화벽(587 포트) 확인"
                is SocketTimeoutException -> return "응답 시간 초과 — 잠시 후 재발송"
                is SendFailedException -> return "수신 주소 오류 — 수신자 이메일 확인"
                is IllegalArgumentException -> return c.message ?: "설정 확인 필요"
            }
            val m = c.message.orEmpty()
            if (m.contains("535") || m.contains("Username and Password", true))
                return "Gmail 인증 실패 — 2단계 인증 활성화 및 앱 비밀번호(16자리) 확인"
            c = c.cause
        }
        return "발송 실패: ${e.message.orEmpty().take(120)}"
    }
}
