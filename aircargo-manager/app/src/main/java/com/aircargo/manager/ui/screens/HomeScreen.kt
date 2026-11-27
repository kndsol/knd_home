package com.aircargo.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.Airline
import com.aircargo.manager.data.Airlines
import com.aircargo.manager.data.Booking
import com.aircargo.manager.ui.Routes
import com.aircargo.manager.ui.components.SectionTitle
import com.aircargo.manager.ui.theme.MenuSelected
import com.aircargo.manager.ui.theme.Navy
import com.aircargo.manager.ui.theme.NavyCard
import com.aircargo.manager.ui.theme.NavyText
import com.aircargo.manager.ui.theme.Prototype

private data class MenuItem(val label: String, val sub: String, val icon: ImageVector, val route: String)

private val menus = listOf(
    MenuItem("운항 스케쥴", "인천공항 연동", Icons.Filled.CalendarMonth, Routes.SCHEDULE),
    MenuItem("화물 예약", "예약 요청 등록", Icons.Filled.Inventory2, Routes.BOOKING),
    MenuItem("화물 추적", "항공사 WEB", Icons.Filled.TravelExplore, Routes.TRACKING),
    MenuItem("실시간 운항", "Flightradar24", Icons.Filled.Radar, Routes.LIVE),
    MenuItem("예약 내역", "조회·상태관리", Icons.AutoMirrored.Filled.ListAlt, Routes.BOOKINGS),
    MenuItem("알림 설정", "Gmail 메일 알림", Icons.Filled.NotificationsActive, Routes.NOTIFY),
    MenuItem("설정", "API 키 관리", Icons.Filled.Settings, Routes.SETTINGS),
)

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Header(vm, onLogout)

        Card(
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle("항공사 선택")
                AirlineRadioRow(vm.selectedAirline) { vm.selectedAirline = it }

                HorizontalDivider(Modifier.padding(vertical = 16.dp))

                SectionTitle("업무 메뉴")
                menus.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            MenuTile(m, Modifier.weight(1f)) { onNavigate(m.route) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        RecentBookings(vm.bookings.take(3), onMore = { onNavigate(Routes.BOOKINGS) })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Header(vm: AppViewModel, onLogout: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("AIR CARGO MANAGER", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("항공화물 통합 관리 시스템 · ICN", color = NavyText, fontSize = 12.sp)
            }
            Text(
                "PROTOTYPE",
                color = Prototype,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Prototype.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Logout, "로그아웃", tint = Color.White)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("관리 항공사", Airlines.all.size.toString().padStart(2, '0'), Modifier.weight(1f))
            StatCard("오늘 예약", vm.todayBookingCount.toString(), Modifier.weight(1f))
            StatCard("운영 공항", "ICN", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NavyCard)
            .padding(10.dp)
    ) {
        Text(label, color = NavyText, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AirlineRadioRow(selected: Airline?, onSelect: (Airline?) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioOption("전체", selected == null) { onSelect(null) }
        Airlines.all.forEach { a ->
            RadioOption(a.nameKo, selected?.code == a.code) { onSelect(a) }
        }
    }
}

@Composable
private fun RadioOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 15.sp)
    }
}

@Composable
private fun MenuTile(m: MenuItem, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MenuSelected)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(m.icon, null, tint = Navy, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(m.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy, textAlign = TextAlign.Center)
        Text(m.sub, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RecentBookings(list: List<Booking>, onMore: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("최근 예약", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("전체보기", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onMore).padding(4.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (list.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) { Text("등록된 예약 없음", color = Color.Gray) }
        } else {
            list.forEach { b ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.FlightTakeoff, null, tint = Navy)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${b.flightNo} · ${b.destination}", fontWeight = FontWeight.SemiBold)
                        Text("${b.flightDate} · ${b.pieces}PCS / ${Booking.formatWeight(b.weightKg)}KG",
                            fontSize = 12.sp, color = Color.Gray)
                    }
                    StatusBadge(b.status)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        Booking.STATUS_CONFIRMED -> Color(0xFF1E8E3E)
        Booking.STATUS_CANCELLED -> Color(0xFFC5221F)
        else -> Color(0xFF2E6BD6)
    }
    Text(
        status,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
