package com.aircargo.manager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.ExternalLinks
import com.aircargo.manager.ui.components.AirlineChips
import com.aircargo.manager.ui.components.SectionTitle
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveStatusScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var flightNo by rememberSaveable { mutableStateOf("") }

    // 편명 빠른 선택용 스케쥴 확보
    LaunchedEffect(Unit) { if (vm.flights.isEmpty()) vm.loadSchedule() }

    fun openExternal(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun searchFlight() {
        val f = flightNo.trim()
        if (f.isNotEmpty()) onOpenWeb(ExternalLinks.fr24Flight(f), "$f 실시간 운항")
    }

    SubScreen(title = "실시간 운항현황", onBack = onBack) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Flightradar24 · 인천공항(ICN)")
                    Button(
                        onClick = { onOpenWeb(ExternalLinks.FR24_ICN, "ICN 실시간 운항") },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Icon(Icons.Filled.Map, null)
                        Spacer(Modifier.width(6.dp))
                        Text("인천공항 실시간 출도착 보기", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { openExternal(ExternalLinks.FR24_ICN) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Flightradar24 앱/브라우저로 열기")
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("편명 위치 조회")
                    OutlinedTextField(
                        value = flightNo,
                        onValueChange = { flightNo = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                        label = { Text("편명 (예: CI161)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Search,
                        ),
                        keyboardActions = KeyboardActions(onSearch = { searchFlight() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { searchFlight() },
                        enabled = flightNo.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("조회") }

                    Text("오늘 스케쥴에서 선택", fontSize = 13.sp, color = Color.Gray)
                    AirlineChips(vm.selectedAirline, { vm.selectedAirline = it })
                    val quick = vm.flights
                        .filter { vm.selectedAirline == null || it.airlineCode == vm.selectedAirline?.code }
                        .map { it.flightNo }
                        .distinct()
                        .take(30)
                    if (quick.isEmpty()) {
                        Text("스케쥴 없음", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            quick.forEach { no ->
                                AssistChip(
                                    onClick = { onOpenWeb(ExternalLinks.fr24Flight(no), "$no 실시간 운항") },
                                    label = { Text(no) },
                                )
                            }
                        }
                    }
                }
            }
            Text(
                "실시간 데이터는 Flightradar24 제공 화면이며, 일부 기능은 FR24 정책에 따라 로그인·구독이 필요할 수 있습니다.",
                fontSize = 11.sp, color = Color.Gray,
            )
        }
    }
}
