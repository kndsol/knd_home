package com.aircargo.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.aircargo.manager.ui.theme.Accent
import com.aircargo.manager.ui.theme.Navy
import com.aircargo.manager.ui.theme.NavyCard
import com.aircargo.manager.ui.theme.NavyText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class MenuItem(val label: String, val icon: ImageVector, val route: String)

/** 서비스 메뉴 — 고객·관리자 공통 */
private val serviceMenus = listOf(
    MenuItem("운항 스케쥴", Icons.Filled.CalendarMonth, Routes.SCHEDULE),
    MenuItem("화물 예약", Icons.Filled.Inventory2, Routes.BOOKING),
    MenuItem("화물 추적", Icons.Filled.TravelExplore, Routes.TRACKING),
    MenuItem("실시간 운항", Icons.Filled.Radar, Routes.LIVE),
)

private val Line = Color(0xFFE3E8EF)

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
        Header(vm, onSettings = { onNavigate(Routes.SETTINGS) }, onLogout = onLogout)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // 항공사 선택
            Section("항공사 선택") {
                AirlineSelector(vm.selectedAirline) { vm.selectedAirline = it }
            }

            // 서비스 메뉴 (2×2)
            Section("서비스 메뉴") {
                serviceMenus.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { m -> MenuTile(m, Modifier.weight(1f)) { onNavigate(m.route) } }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // 관리 메뉴 (관리자 전용)
            if (vm.isAdmin) {
                Section("관리 메뉴") {
                    ManageRow(vm.bookings.count { it.status == Booking.STATUS_REQUESTED }) { onNavigate(Routes.BOOKINGS) }
                    Spacer(Modifier.height(12.dp))
                    RecentBookings(vm.bookings.take(3))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Header(vm: AppViewModel, onSettings: () -> Unit, onLogout: () -> Unit) {
    val today = SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date())
    Column(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)
    ) {
        // 상단: 역할 표시 + 우측 아이콘 (설정은 관리자만)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (vm.isAdmin) "관리자" else "고객",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(if (vm.isAdmin) Accent else NavyCard, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
            Spacer(Modifier.weight(1f))
            if (vm.isAdmin) {
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "설정", tint = Color.White) }
            }
            IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "로그아웃", tint = Color.White) }
        }
        Text("AIR CARGO MANAGER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("항공화물 통합 관리 시스템 · $today", color = NavyText, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("관리 항공사", Airlines.all.size.toString().padStart(2, '0'), Modifier.weight(1f))
            if (vm.isAdmin) StatCard("오늘 예약", vm.todayBookingCount.toString(), Modifier.weight(1f))
            StatCard("운영 공항", "ICN", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NavyCard)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = NavyText, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

/** 항공사 선택 — 한 줄 칩 */
@Composable
private fun AirlineSelector(selected: Airline?, onSelect: (Airline?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AirlineChip("전체", null, selected == null) { onSelect(null) }
        Airlines.all.forEach { a -> AirlineChip(a.nameKo, a.code, selected?.code == a.code) { onSelect(a) } }
    }
}

@Composable
private fun AirlineChip(label: String, code: String?, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(if (on) Navy else Color.White)
            .border(1.dp, if (on) Navy else Line, RoundedCornerShape(21.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (code != null) {
            Text(code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) NavyText else Color.Gray)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else Navy)
    }
}

@Composable
private fun MenuTile(m: MenuItem, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(52.dp).background(Color(0xFFE6EDF7), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(m.icon, null, tint = Navy, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(m.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ManageRow(pending: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.ListAlt, null, tint = Navy)
        Spacer(Modifier.width(12.dp))
        Text("예약 내역", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f))
        if (pending > 0) {
            Text(
                "요청 $pending", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Accent, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
    }
}

@Composable
private fun RecentBookings(list: List<Booking>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("최근 예약", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 6.dp))
        if (list.isEmpty()) {
            Text("등록된 예약 없음", color = Color.Gray, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), textAlign = TextAlign.Center)
        }
        list.forEachIndexed { i, b ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FlightTakeoff, null, tint = Navy, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${b.flightNo} → ${b.destination}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${b.flightDate} · ${b.pieces}PCS / ${Booking.formatWeight(b.weightKg)}KG",
                        fontSize = 12.sp, color = Color.Gray)
                }
                StatusBadge(b.status)
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
