package com.aircargo.manager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.Airlines
import com.aircargo.manager.ui.components.AirlineChips
import com.aircargo.manager.ui.components.SectionTitle
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy

@Composable
fun TrackingScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var airlineCode by rememberSaveable { mutableStateOf(vm.selectedAirline?.code ?: Airlines.all.first().code) }
    val airline = Airlines.byCode(airlineCode) ?: Airlines.all.first()
    var prefix by rememberSaveable { mutableStateOf(airline.awbPrefix) }
    var serial by rememberSaveable { mutableStateOf("") }

    // AWB 체크디지트: 일련번호 앞 7자리 mod 7 == 마지막 자리
    val checkOk = serial.length == 8 && serial.take(7).toLong() % 7 == serial.last().digitToInt().toLong()
    val awb = "$prefix-$serial"

    SubScreen(title = "화물 추적", onBack = onBack) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("AWB 조회")
                    AirlineChips(
                        selected = airline,
                        onSelect = { a ->
                            if (a != null) {
                                airlineCode = a.code
                                prefix = a.awbPrefix
                            }
                        },
                        includeAll = false,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Prefix") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                        )
                        Text("-", fontSize = 20.sp)
                        OutlinedTextField(
                            value = serial,
                            onValueChange = { serial = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("AWB 번호 8자리") },
                            singleLine = true,
                            isError = serial.length == 8 && !checkOk,
                            supportingText = {
                                when {
                                    serial.length < 8 -> Text("${serial.length}/8")
                                    checkOk -> Text("체크디지트 정상", color = Color(0xFF1E8E3E))
                                    else -> Text("체크디지트 불일치 — 번호 재확인")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        onClick = {
                            if (serial.isNotBlank()) {
                                clipboard.setText(AnnotatedString(if (prefix.isBlank()) serial else awb))
                                Toast.makeText(context, "AWB 복사됨 — 조회 화면에 붙여넣기", Toast.LENGTH_SHORT).show()
                            }
                            onOpenWeb(airline.cargoTrackingUrl, "${airline.nameKo} 화물추적")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Icon(Icons.Filled.TravelExplore, null)
                        Spacer(Modifier.width(6.dp))
                        Text("${airline.nameKo} 화물추적 WEB 열기", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "항공사 WEB은 외부 사이트로, AWB 번호를 클립보드에 복사한 뒤 해당 사이트의 조회 화면으로 연결합니다.",
                        fontSize = 11.sp, color = Color.Gray,
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle("항공사 화물 WEB 바로가기")
                    Airlines.all.forEachIndexed { i, a ->
                        if (i > 0) HorizontalDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWeb(a.cargoTrackingUrl, "${a.nameKo} 화물") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(a.code, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.width(40.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${a.nameKo} (${a.nameEn})")
                                Text(
                                    (if (a.awbPrefix.isNotBlank()) "AWB ${a.awbPrefix} · " else "") + a.cargoTrackingUrl,
                                    fontSize = 11.sp, color = Color.Gray,
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
