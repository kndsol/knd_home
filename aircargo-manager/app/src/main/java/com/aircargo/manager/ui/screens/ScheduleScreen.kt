package com.aircargo.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.Direction
import com.aircargo.manager.data.ExternalLinks
import com.aircargo.manager.data.Flight
import com.aircargo.manager.data.ScheduleSource
import com.aircargo.manager.ui.components.AirlineChips
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy

@Composable
fun ScheduleScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String, String) -> Unit,
    onSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // 키 미설정 시 인천공항 공식 운항정보(웹) 기본 표시
    var webMode by rememberSaveable { mutableStateOf(vm.serviceKey.isBlank()) }

    LaunchedEffect(vm.scheduleSource, vm.direction, vm.serviceKey) { vm.loadSchedule() }

    val filtered = remember(vm.flights, vm.selectedAirline, query) {
        vm.flights.filter { f ->
            (vm.selectedAirline == null || f.airlineCode == vm.selectedAirline?.code) &&
                (query.isBlank() || listOf(f.flightNo, f.airport, f.airportCode, f.airlineName)
                    .any { it.contains(query.trim(), ignoreCase = true) })
        }.sortedBy { it.scheduleTime }
    }

    SubScreen(
        title = "운항 스케쥴",
        onBack = onBack,
        actions = {
            IconButton(onClick = { vm.loadSchedule() }) { Icon(Icons.Filled.Refresh, "새로고침") }
            IconButton(onClick = { onOpenWeb(ExternalLinks.ICN_FLIGHT_WEB, "인천공항 운항정보") }) {
                Icon(Icons.Filled.Language, "인천공항 웹")
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = vm.direction.ordinal, containerColor = Color.White) {
                Direction.entries.forEach { d ->
                    Tab(selected = vm.direction == d, onClick = { vm.direction = d },
                        text = { Text("ICN ${d.label}", fontWeight = FontWeight.SemiBold) })
                }
            }
            Column(Modifier.background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleSource.entries.forEach { s ->
                        FilterChip(selected = vm.scheduleSource == s, onClick = { vm.scheduleSource = s },
                            label = { Text(s.label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = webMode, onClick = { webMode = true },
                        label = { Text("인천공항 운항정보") })
                    FilterChip(selected = !webMode, onClick = { webMode = false },
                        label = { Text("API 목록") })
                }
                if (!webMode) AirlineChips(vm.selectedAirline, { vm.selectedAirline = it })
                if (!webMode) OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("편명 / 공항 검색") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }

            if (webMode) {
                Text(
                    "인천공항 공식 운항정보 화면" + if (vm.isAdmin && vm.serviceKey.isBlank()) " · 설정에서 API 키 입력 시 'API 목록' 조회 가능" else "",
                    fontSize = 11.sp, color = Color.Gray,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFEDF1F6)).padding(horizontal = 12.dp, vertical = 4.dp),
                )
                EmbeddedWeb(
                    ExternalLinks.icnWeb(
                        cargo = vm.scheduleSource == ScheduleSource.CARGO,
                        departure = vm.direction == Direction.DEPARTURE,
                    ),
                    Modifier.fillMaxSize(),
                )
            } else vm.scheduleMessage?.let { msg ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF4E5))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Info, null, tint = Color(0xFFB26A00))
                    Spacer(Modifier.width(6.dp))
                    Text(msg, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (vm.isAdmin && (vm.isSample || vm.serviceKey.isBlank())) {
                        TextButton(onClick = onSettings) { Text("설정") }
                    }
                }
            }

            if (!webMode) Box(Modifier.fillMaxSize()) {
                if (vm.loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text("${filtered.size}편 · ${vm.scheduleSource.desc}", fontSize = 11.sp, color = Color.Gray)
                        }
                        items(filtered) { f ->
                            FlightCard(f, vm.direction) {
                                onOpenWeb(ExternalLinks.fr24Flight(f.flightNo), "${f.flightNo} 실시간 위치")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightCard(f: Flight, direction: Direction, onLive: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onLive),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(92.dp)) {
                Text(f.scheduleTime.takeLast(5), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
                if (f.estimatedTime.isNotBlank() && f.estimatedTime != f.scheduleTime) {
                    Text("변경 ${f.estimatedTime.takeLast(5)}", fontSize = 11.sp, color = Color(0xFFC5221F))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(f.flightNo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    (if (direction == Direction.DEPARTURE) "→ " else "← ") +
                        f.airport + if (f.airportCode.isNotBlank()) " (${f.airportCode})" else "",
                    fontSize = 13.sp,
                )
                val sub = listOf(f.airlineName, f.extra).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, fontSize = 11.sp, color = Color.Gray)
            }
            if (f.status.isNotBlank()) {
                Text(
                    f.status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor(f.status),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor(f.status).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

private fun statusColor(s: String): Color = when {
    s.contains("결항") || s.contains("취소") -> Color(0xFFC5221F)
    s.contains("지연") -> Color(0xFFB26A00)
    s.contains("출발") || s.contains("도착") -> Color(0xFF1E8E3E)
    else -> Color(0xFF2E6BD6)
}
