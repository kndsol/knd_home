package com.aircargo.manager.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.Airlines
import com.aircargo.manager.data.Booking
import com.aircargo.manager.data.MailLog
import com.aircargo.manager.data.MailTemplate
import com.aircargo.manager.data.NotifySettings
import com.aircargo.manager.data.Recipient
import com.aircargo.manager.data.diagnose
import com.aircargo.manager.ui.components.SectionTitle
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy

private val OK = Color(0xFF1E8E3E)
private val BAD = Color(0xFFC5221F)
private fun isEmail(s: String) = Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()

@Composable
fun NotifySettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    val init = vm.notifySettings
    var enabled by rememberSaveable { mutableStateOf(init.enabled) }
    var gmail by rememberSaveable { mutableStateOf(init.gmail) }
    var appPw by rememberSaveable { mutableStateOf(init.appPassword) }
    var senderName by rememberSaveable { mutableStateOf(init.senderName) }
    var onCreated by rememberSaveable { mutableStateOf(init.onBookingCreated) }
    var onStatus by rememberSaveable { mutableStateOf(init.onStatusChanged) }
    var bookerConfirm by rememberSaveable { mutableStateOf(init.bookerConfirm) }
    val recipients = remember { mutableStateListOf<Recipient>().apply { addAll(init.recipients) } }
    var tplC by remember { mutableStateOf(init.tplCreate) }
    var tplS by remember { mutableStateOf(init.tplStatus) }
    var showPw by rememberSaveable { mutableStateOf(false) }
    var newEmail by rememberSaveable { mutableStateOf("") }
    var newName by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var viewLog by remember { mutableStateOf<MailLog?>(null) }

    fun current() = NotifySettings(
        enabled = enabled, gmail = gmail.trim(), appPassword = appPw.trim(), senderName = senderName.trim(),
        onBookingCreated = onCreated, onStatusChanged = onStatus, bookerConfirm = bookerConfirm,
        recipients = recipients.toList(), tplCreate = tplC, tplStatus = tplS,
    )
    fun dirty() { saved = false }
    val checks = diagnose(current())

    SubScreen(title = "알림 설정", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 사용 여부 + 이벤트
            NCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("메일 알림 사용", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("예약 이벤트 발생 시 자동 발송", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it; dirty() })
                }
                HorizontalDivider()
                CheckRow("화물 예약 등록 시", onCreated) { onCreated = it; dirty() }
                CheckRow("예약 상태 변경 시 (확정 / 취소)", onStatus) { onStatus = it; dirty() }
                CheckRow("예약자 이메일로 접수 확인 메일 자동 발송", bookerConfirm) { bookerConfirm = it; dirty() }
            }

            // ① 앱 비밀번호 발급 안내
            NCard {
                SectionTitle("① 앱 비밀번호 발급 안내")
                Step(1, "Google 계정 2단계 인증 켜기") {
                    LinkButton("2단계 인증 설정 열기") { uri.openUri("https://myaccount.google.com/signinoptions/two-step-verification") }
                }
                Step(2, "앱 비밀번호 만들기") {
                    LinkButton("앱 비밀번호 페이지 열기") { uri.openUri("https://myaccount.google.com/apppasswords") }
                }
                Step(3, "앱 이름 입력(예: AirCargo) → 만들기 → 16자리 복사 (1회만 표시)")
                Step(4, "아래 '앱 비밀번호'에 붙여넣기 — 공백 자동 제거")
                Text("알림 전용 Gmail 계정 사용 권장 · 계정 비밀번호 변경 시 앱 비밀번호 재발급 · 개인 Gmail 일 500통 한도",
                    fontSize = 11.sp, color = Color.Gray)
            }

            // 발신 계정
            NCard {
                SectionTitle("발신 계정 (Gmail SMTP)")
                OutlinedTextField(
                    value = gmail, onValueChange = { gmail = it; dirty() },
                    label = { Text("Gmail 주소") }, placeholder = { Text("alert.aircargo@gmail.com") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = appPw, onValueChange = { appPw = it; dirty() },
                    label = { Text("앱 비밀번호 (16자리)") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPw = !showPw }) {
                            Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "표시")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = senderName, onValueChange = { senderName = it; dirty() },
                    label = { Text("발신자 표시명") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text("smtp.gmail.com : 587 (STARTTLS)", fontSize = 11.sp, color = Color.Gray)
            }

            // ② 연결 점검
            NCard {
                SectionTitle("② 연결 점검")
                checks.forEach { c ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(if (c.ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, null,
                            tint = if (c.ok) OK else BAD, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(c.label, fontSize = 14.sp)
                            if (!c.ok) Text(c.fix, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { vm.saveNotify(current()); saved = true; vm.sendTestMail() },
                    enabled = checks.all { it.ok } && !vm.mailSending,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (vm.mailSending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, null)
                    Spacer(Modifier.width(6.dp))
                    Text("저장 후 테스트 메일 발송")
                }
                Text("발송 실패 시 원인(인증·네트워크·주소 오류)을 구분해 안내", fontSize = 11.sp, color = Color.Gray)
            }

            // ③ 수신자 · 항공사별 담당
            NCard {
                SectionTitle("③ 수신자 · 항공사별 담당 (${recipients.count { it.enabled }}/${recipients.size})")
                if (recipients.isEmpty()) Text("등록된 수신자 없음", fontSize = 13.sp, color = Color.Gray)
                recipients.forEachIndexed { i, r ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = r.enabled, onCheckedChange = { recipients[i] = r.copy(enabled = it); dirty() })
                            Column(Modifier.weight(1f)) {
                                Text(r.name.ifBlank { "(이름 없음)" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(r.email, fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { recipients.removeAt(i); dirty() }) {
                                Icon(Icons.Filled.Delete, "삭제", tint = BAD)
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (listOf(Recipient.SCOPE_ALL) + Airlines.all.map { it.code }).forEach { code ->
                                FilterChip(
                                    selected = code in r.scopes,
                                    onClick = {
                                        val sc = if (code == Recipient.SCOPE_ALL) listOf(Recipient.SCOPE_ALL) else {
                                            val base = r.scopes - Recipient.SCOPE_ALL
                                            val next = if (code in base) base - code else base + code
                                            next.ifEmpty { listOf(Recipient.SCOPE_ALL) }
                                        }
                                        recipients[i] = r.copy(scopes = sc); dirty()
                                    },
                                    label = { Text(if (code == Recipient.SCOPE_ALL) "전체" else code, fontSize = 12.sp) },
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
                val dup = recipients.any { it.email.equals(newEmail.trim(), ignoreCase = true) }
                val newOk = isEmail(newEmail) && !dup
                OutlinedTextField(
                    value = newEmail, onValueChange = { newEmail = it }, label = { Text("수신 이메일") },
                    isError = newEmail.isNotBlank() && !newOk,
                    supportingText = if (newEmail.isNotBlank() && !newOk) {
                        { Text(if (dup) "이미 등록된 주소" else "이메일 형식 확인") }
                    } else null,
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it }, label = { Text("이름 / 소속 (선택)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        recipients.add(Recipient(newEmail.trim(), newName.trim()))
                        newEmail = ""; newName = ""; dirty()
                    },
                    enabled = newOk, modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("수신자 추가")
                }
                Text("'전체'는 모든 항공사 예약, 항공사 코드는 해당 항공사 예약만 수신", fontSize = 11.sp, color = Color.Gray)
            }

            // ④ 메일 양식
            NCard {
                SectionTitle("④ 메일 양식")
                TemplateEditor(
                    create = tplC, status = tplS,
                    onChange = { isCreate, t -> if (isCreate) tplC = t else tplS = t; dirty() },
                    onPreview = { preview = true },
                )
            }

            // ⑤ 발송 이력
            NCard {
                val fails = vm.mailLogs.count { !it.ok }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⑤ 발송 이력" + if (fails > 0) " · 실패 $fails" else "", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (vm.mailLogs.isNotEmpty()) TextButton(onClick = { vm.clearMailLogs() }) { Text("비우기") }
                }
                if (vm.mailLogs.isEmpty()) Text("발송 이력 없음 · 예약 등록/상태 변경 시 기록", fontSize = 13.sp, color = Color.Gray)
                vm.mailLogs.take(20).forEach { l ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l.subject, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    Booking.formatDateTime(l.time) + " · ${l.to.size}명" + if (l.ok) "" else " · ${l.error}",
                                    fontSize = 12.sp, color = if (l.ok) Color.Gray else BAD,
                                )
                            }
                            Text(
                                if (l.ok) "발송" else "실패", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (l.ok) OK else BAD,
                                modifier = Modifier.background((if (l.ok) OK else BAD).copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                        Row {
                            TextButton(onClick = { viewLog = l }) { Text("내용") }
                            if (!l.ok) TextButton(onClick = { vm.retry(l) }, enabled = !vm.mailSending) { Text("재발송") }
                        }
                    }
                    HorizontalDivider()
                }
            }

            Button(
                onClick = { vm.saveNotify(current()); saved = true },
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text(if (saved) "저장 완료" else "설정 저장", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (preview) {
        val sample = vm.bookings.firstOrNull() ?: Booking(
            id = "BK000000-001", createdAt = System.currentTimeMillis(), airlineCode = "CI", flightNo = "CI161",
            flightDate = "2026-11-28", pieces = 3, weightKg = 120.0, item = "ELECTRONICS", destination = "TPE",
            remark = "", tempControl = false, dryIce = false, bookerCompany = "길기리물류", bookerName = "홍길동",
            bookerPhone = "010-1234-5678", status = Booking.STATUS_CONFIRMED,
        )
        AlertDialog(
            onDismissRequest = { preview = false },
            title = { Text("미리보기") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    listOf("예약 등록" to tplC, "상태 변경" to tplS).forEach { (t, tpl) ->
                        Text(t, fontSize = 12.sp, color = Color.Gray)
                        Text(tpl.subjectFor(sample), fontWeight = FontWeight.Bold)
                        Text(tpl.bodyFor(sample), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    Text(if (vm.bookings.isEmpty()) "예시 예약 기준" else "최근 예약 기준", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = { TextButton(onClick = { preview = false }) { Text("닫기") } },
        )
    }

    viewLog?.let { l ->
        AlertDialog(
            onDismissRequest = { viewLog = null },
            title = { Text(l.subject, fontSize = 16.sp) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text("${Booking.formatDateTime(l.time)}\n수신: ${l.to.joinToString(", ").ifBlank { "-" }}",
                        fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text(l.body, fontSize = 13.sp)
                    if (!l.ok) Text("실패 사유: ${l.error}", color = BAD, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { viewLog = null }) { Text("닫기") } },
        )
    }
}

@Composable
private fun TemplateEditor(
    create: MailTemplate,
    status: MailTemplate,
    onChange: (Boolean, MailTemplate) -> Unit,
    onPreview: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val cur = if (tab == 0) create else status
    var subj by remember(tab) { mutableStateOf(TextFieldValue(cur.subject)) }
    var body by remember(tab) { mutableStateOf(TextFieldValue(cur.body)) }
    var focusBody by remember { mutableStateOf(true) }

    fun push() = onChange(tab == 0, MailTemplate(subj.text, body.text))
    fun insert(v: String) {
        val ins = "{$v}"
        val t = if (focusBody) body else subj
        val start = minOf(t.selection.start, t.selection.end)
        val end = maxOf(t.selection.start, t.selection.end)
        val next = TextFieldValue(t.text.replaceRange(start, end, ins), TextRange(start + ins.length))
        if (focusBody) body = next else subj = next
        push()
    }

    TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("예약 등록") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("상태 변경") })
    }
    OutlinedTextField(
        value = subj, onValueChange = { subj = it; push() }, label = { Text("제목") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusBody = false },
    )
    OutlinedTextField(
        value = body, onValueChange = { body = it; push() }, label = { Text("본문") }, minLines = 6,
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusBody = true },
    )
    Text("변수 삽입 (${if (focusBody) "본문" else "제목"} 커서 위치)", fontSize = 12.sp, color = Color.Gray)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MailTemplate.VARIABLES.forEach { v ->
            FilterChip(selected = false, onClick = { insert(v) }, label = { Text("{$v}", fontSize = 12.sp) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val d = if (tab == 0) MailTemplate.DEFAULT_CREATE else MailTemplate.DEFAULT_STATUS
            subj = TextFieldValue(d.subject); body = TextFieldValue(d.body); push()
        }, modifier = Modifier.weight(1f)) { Text("기본 양식 복원") }
        OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) { Text("미리보기") }
    }
}

@Composable
private fun NCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Step(n: Int, text: String, extra: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).background(Navy, CircleShape), contentAlignment = Alignment.Center) {
            Text("$n", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text, fontSize = 14.sp)
            extra()
        }
    }
}

@Composable
private fun LinkButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label); Spacer(Modifier.width(4.dp)); Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, fontSize = 14.sp)
    }
}
