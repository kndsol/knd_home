package com.aircargo.manager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.ScheduleSource
import com.aircargo.manager.ui.components.SectionTitle
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onNotify: () -> Unit) {
    val context = LocalContext.current
    var key by rememberSaveable { mutableStateOf(vm.serviceKey) }
    var show by rememberSaveable { mutableStateOf(false) }

    SubScreen(title = "설정", onBack = onBack) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("인천공항 API 서비스키")
                    Text(
                        "공공데이터포털(data.go.kr)에서 아래 API 활용신청 후 발급된 일반 인증키 입력. " +
                            "Encoding/Decoding 키 모두 사용 가능.",
                        fontSize = 12.sp, color = Color.Gray,
                    )
                    ScheduleSource.entries.forEach {
                        Text("• ${it.label} — ${it.desc}", fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("serviceKey") },
                        singleLine = true,
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { show = !show }) {
                                Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "표시")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                vm.saveServiceKey(key)
                                Toast.makeText(context, "저장됨", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy),
                            modifier = Modifier.weight(1f),
                        ) { Text("저장") }
                        OutlinedButton(
                            onClick = { key = ""; vm.saveServiceKey("") },
                            modifier = Modifier.weight(1f),
                        ) { Text("초기화 (예시 데이터)") }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("메일 알림")
                    val n = vm.notifySettings
                    Text(
                        if (n.enabled && n.isConfigured) "사용 중 · ${n.gmail} → 수신자 ${n.recipients.count { it.enabled }}명"
                        else if (n.enabled) "사용 설정됨 · 발신 계정/수신자 설정 미완료"
                        else "사용 안 함",
                        fontSize = 13.sp,
                    )
                    OutlinedButton(onClick = onNotify, modifier = Modifier.fillMaxWidth()) { Text("알림 설정 열기") }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle("앱 정보")
                    Text("Air Cargo Manager v1.0.0 (Prototype)", fontSize = 13.sp)
                    Text("로그인 계정: ${AppViewModel.LOGIN_ID} (고정)", fontSize = 13.sp)
                    Text("예약 데이터: 단말 내부 저장 (서버 미연동)", fontSize = 13.sp)
                }
            }
        }
    }
}
