package com.aircargo.manager.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.NotifySettings
import com.aircargo.manager.data.Recipient
import com.aircargo.manager.ui.components.SectionTitle
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy

private fun isEmail(s: String) = Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()

@Composable
fun NotifySettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val init = vm.notifySettings
    var enabled by rememberSaveable { mutableStateOf(init.enabled) }
    var gmail by rememberSaveable { mutableStateOf(init.gmail) }
    var appPw by rememberSaveable { mutableStateOf(init.appPassword) }
    var senderName by rememberSaveable { mutableStateOf(init.senderName) }
    var onCreated by rememberSaveable { mutableStateOf(init.onBookingCreated) }
    var onStatus by rememberSaveable { mutableStateOf(init.onStatusChanged) }
    val recipients = remember { mutableStateListOf<Recipient>().apply { addAll(init.recipients) } }
    var showPw by rememberSaveable { mutableStateOf(false) }

    var newEmail by rememberSaveable { mutableStateOf("") }
    var newName by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }

    val gmailOk = gmail.isBlank() || isEmail(gmail)
    val pwLen = appPw.replace(" ", "").length

    fun current() = NotifySettings(
        enabled = enabled,
        gmail = gmail.trim(),
        appPassword = appPw.trim(),
        senderName = senderName.trim(),
        onBookingCreated = onCreated,
        onStatusChanged = onStatus,
        recipients = recipients.toList(),
    )

    SubScreen(title = "알림 설정", onBack = onBack) { pad ->
        Column(
            Modifier
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 사용 여부 + 이벤트
            NCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("메일 알림 사용", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("예약 이벤트 발생 시 등록 수신자에게 자동 발송", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it; saved = false })
                }
                HorizontalDivider()
                Text("알림 대상 이벤트", fontSize = 13.sp, color = Color.Gray)
                CheckRow("화물 예약 등록", onCreated) { onCreated = it; saved = false }
                CheckRow("예약 상태 변경 (확정 / 취소)", onStatus) { onStatus = it; saved = false }
            }

            // 발신 계정
            NCard {
                SectionTitle("발신 계정 (Gmail SMTP)")
                OutlinedTextField(
                    value = gmail,
                    onValueChange = { gmail = it; saved = false },
                    label = { Text("Gmail 주소") },
                    placeholder = { Text("example@gmail.com") },
                    isError = !gmailOk,
                    supportingText = if (!gmailOk) { { Text("이메일 형식 확인") } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = appPw,
                    onValueChange = { appPw = it; saved = false },
                    label = { Text("앱 비밀번호 (16자리)") },
                    supportingText = { Text("입력 ${pwLen}/16 · Google 계정 일반 비밀번호 사용 불가") },
                    singleLine = true,
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
                    value = senderName,
                    onValueChange = { senderName = it; saved = false },
                    label = { Text("발신자 표시명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "· SMTP: smtp.gmail.com : 587 (STARTTLS)\n" +
                        "· 사전 준비: Google 계정 2단계 인증 활성화 → 보안 > 앱 비밀번호 생성 → 위 항목에 입력",
                    fontSize = 11.sp, color = Color.Gray,
                )
            }

            // 수신자
            NCard {
                SectionTitle("수신자 (${recipients.count { it.enabled }}/${recipients.size}명 활성)")
                if (recipients.isEmpty()) {
                    Text("등록된 수신자 없음", fontSize = 13.sp, color = Color.Gray)
                }
                recipients.forEachIndexed { i, r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = r.enabled, onCheckedChange = {
                            recipients[i] = r.copy(enabled = it); saved = false
                        })
                        Column(Modifier.weight(1f)) {
                            Text(r.name.ifBlank { "(이름 없음)" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(r.email, fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { recipients.removeAt(i); saved = false }) {
                            Icon(Icons.Filled.Delete, "삭제", tint = Color(0xFFC5221F))
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                val dup = recipients.any { it.email.equals(newEmail.trim(), ignoreCase = true) }
                val newOk = isEmail(newEmail) && !dup
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("수신 이메일") },
                    isError = newEmail.isNotBlank() && !newOk,
                    supportingText = if (newEmail.isNotBlank() && !newOk) {
                        { Text(if (dup) "이미 등록된 주소" else "이메일 형식 확인") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("이름 / 소속 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        recipients.add(Recipient(newEmail.trim(), newName.trim()))
                        newEmail = ""; newName = ""; saved = false
                    },
                    enabled = newOk,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PersonAdd, null)
                    Spacer(Modifier.width(6.dp))
                    Text("수신자 추가")
                }
            }

            Button(
                onClick = { vm.saveNotify(current()); saved = true },
                enabled = gmailOk,
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text(if (saved) "저장 완료" else "설정 저장", fontWeight = FontWeight.Bold) }

            val canTest = isEmail(gmail) && pwLen > 0 && recipients.any { it.enabled }
            OutlinedButton(
                onClick = { vm.saveNotify(current()); saved = true; vm.sendTestMail() },
                enabled = canTest && !vm.mailSending,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (vm.mailSending) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                }
                Spacer(Modifier.width(6.dp))
                Text("저장 후 테스트 메일 발송")
            }
            if (!canTest) {
                Text("테스트 발송: Gmail 주소·앱 비밀번호·활성 수신자 1명 이상 필요",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
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
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, fontSize = 14.sp)
    }
}
