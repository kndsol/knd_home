package com.aircargo.manager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.Properties
import javax.mail.Address
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/** 알림 수신자 */
data class Recipient(val email: String, val name: String, val enabled: Boolean = true)

/** 알림 설정 (Gmail SMTP) */
data class NotifySettings(
    val enabled: Boolean = false,
    val gmail: String = "",           // 발신 Gmail 계정
    val appPassword: String = "",     // Google 앱 비밀번호 16자리 (일반 비밀번호 불가)
    val senderName: String = "Air Cargo Manager",
    val onBookingCreated: Boolean = true,
    val onStatusChanged: Boolean = true,
    val recipients: List<Recipient> = emptyList(),
) {
    val isConfigured: Boolean
        get() = gmail.isNotBlank() && appPassword.isNotBlank() && recipients.any { it.enabled }
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
                recipients = (0 until arr.length()).map {
                    val r = arr.getJSONObject(it)
                    Recipient(r.optString("email"), r.optString("name"), r.optBoolean("enabled", true))
                },
            )
        }.getOrDefault(NotifySettings())
    }

    fun save(s: NotifySettings) {
        val arr = JSONArray()
        s.recipients.forEach {
            arr.put(JSONObject().put("email", it.email).put("name", it.name).put("enabled", it.enabled))
        }
        val o = JSONObject()
            .put("enabled", s.enabled)
            .put("gmail", s.gmail)
            .put("appPassword", s.appPassword)
            .put("senderName", s.senderName)
            .put("onBookingCreated", s.onBookingCreated)
            .put("onStatusChanged", s.onStatusChanged)
            .put("recipients", arr)
        prefs.edit().putString("settings", o.toString()).apply()
    }
}

/** Gmail SMTP 발송 (smtp.gmail.com:587, STARTTLS) */
object GmailSender {
    private const val HOST = "smtp.gmail.com"
    private const val PORT = "587"

    suspend fun send(s: NotifySettings, subject: String, body: String) = withContext(Dispatchers.IO) {
        val to = s.recipients.filter { it.enabled }
        require(s.gmail.isNotBlank() && s.appPassword.isNotBlank()) { "발신 Gmail 계정/앱 비밀번호 미설정" }
        require(to.isNotEmpty()) { "활성 수신자 없음" }

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
        val password = s.appPassword.replace(" ", "")   // 앱 비밀번호 표시 형식(xxxx xxxx …) 공백 제거
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(s.gmail.trim(), password)
        })
        val msg = MimeMessage(session).apply {
            setFrom(InternetAddress(s.gmail.trim(), s.senderName.ifBlank { s.gmail }, "UTF-8"))
            setRecipients(
                Message.RecipientType.TO,
                to.map<Recipient, Address> {
                    InternetAddress(it.email.trim(), it.name.ifBlank { it.email }, "UTF-8")
                }.toTypedArray(),
            )
            setSubject(subject, "UTF-8")
            setText(body, "UTF-8")
            sentDate = Date()
        }
        Transport.send(msg)
        to.size
    }
}
