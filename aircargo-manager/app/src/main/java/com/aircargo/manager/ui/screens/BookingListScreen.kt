package com.aircargo.manager.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.Airlines
import com.aircargo.manager.data.Booking
import com.aircargo.manager.ui.components.AirlineChips
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy

@Composable
fun BookingListScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onNew: () -> Unit,
) {
    val context = LocalContext.current
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }

    val list = vm.bookings.filter { vm.selectedAirline == null || it.airlineCode == vm.selectedAirline?.code }

    SubScreen(
        title = "예약 내역",
        onBack = onBack,
        floating = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("신규 예약") },
                containerColor = Navy,
                contentColor = Color.White,
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            AirlineChips(vm.selectedAirline, { vm.selectedAirline = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("예약 내역 없음", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { b ->
                        BookingCard(
                            b = b,
                            expanded = expandedId == b.id,
                            onToggle = { expandedId = if (expandedId == b.id) null else b.id },
                            onShare = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "[화물예약] ${b.flightNo} ${b.flightDate} ${b.id}")
                                    putExtra(Intent.EXTRA_TEXT, b.toShareText())
                                }
                                context.startActivity(Intent.createChooser(send, "예약 내용 공유"))
                            },
                            onStatus = { vm.updateStatus(b.id, it) },
                            onDelete = { deleteId = b.id },
                        )
                    }
                }
            }
        }
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("예약 삭제") },
            text = { Text("$id 예약을 삭제합니다. 복구 불가.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteBooking(id); deleteId = null }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("닫기") } },
        )
    }
}

@Composable
private fun BookingCard(
    b: Booking,
    expanded: Boolean,
    onToggle: () -> Unit,
    onShare: () -> Unit,
    onStatus: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val airline = Airlines.byCode(b.airlineCode)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${b.flightNo} → ${b.destination}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${airline?.nameKo ?: b.airlineCode} · ${b.flightDate}", fontSize = 12.sp, color = Color.Gray)
                    Text("${b.pieces}PCS / ${Booking.formatWeight(b.weightKg)}KG · ${b.item}", fontSize = 13.sp)
                }
                StatusBadge(b.status)
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(b.toShareText(), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Action(Icons.Filled.Share, "공유", Navy, onShare)
                    Action(Icons.Filled.CheckCircle, "확정", Color(0xFF1E8E3E)) { onStatus(Booking.STATUS_CONFIRMED) }
                    Action(Icons.Filled.Cancel, "취소", Color(0xFFB26A00)) { onStatus(Booking.STATUS_CANCELLED) }
                    Action(Icons.Filled.Delete, "삭제", Color(0xFFC5221F), onDelete)
                }
            }
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, null, tint = color)
        Text(" $label", color = color)
    }
}
