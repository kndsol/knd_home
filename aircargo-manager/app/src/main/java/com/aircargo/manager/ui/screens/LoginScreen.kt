package com.aircargo.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.ui.theme.Navy
import com.aircargo.manager.ui.theme.NavyText

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Boolean,
    onCustomer: () -> Unit,
    onSuccess: () -> Unit,
) {
    var admin by rememberSaveable { mutableStateOf(false) }
    var id by rememberSaveable { mutableStateOf("") }
    var pw by rememberSaveable { mutableStateOf("") }
    var showPw by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        if (onLogin(id, pw)) onSuccess() else error = "아이디 또는 비밀번호가 올바르지 않습니다."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Flight, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("AIR CARGO MANAGER", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("항공화물 통합 관리 시스템 · ICN", color = NavyText, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 고객 / 관리자 선택
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFFEDF1F6), RoundedCornerShape(12.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(false to "고객", true to "관리자").forEach { (isAdmin, label) ->
                        val on = admin == isAdmin
                        Box(
                            Modifier.weight(1f).height(46.dp)
                                .background(if (on) Navy else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { admin = isAdmin; error = null },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = if (on) Color.White else Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                if (!admin) {
                    Text("계정 없이 이용", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("운항 스케쥴 · 화물 예약 · 화물 추적 · 실시간 운항", fontSize = 13.sp, color = Color.Gray)
                    Button(
                        onClick = { onCustomer(); onSuccess() },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("고객으로 시작", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                } else {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it; error = null },
                        label = { Text("관리자 아이디") },
                        leadingIcon = { Icon(Icons.Filled.Person, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it; error = null },
                        label = { Text("비밀번호") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPw = !showPw }) {
                                Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "비밀번호 표시")
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    Button(
                        onClick = { submit() },
                        enabled = id.isNotBlank() && pw.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("관리자 로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("PROTOTYPE", color = NavyText, fontSize = 12.sp)
    }
}
