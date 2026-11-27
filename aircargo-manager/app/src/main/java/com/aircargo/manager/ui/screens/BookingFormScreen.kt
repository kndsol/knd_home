package com.aircargo.manager.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.data.Airlines
import com.aircargo.manager.data.Booking
import com.aircargo.manager.ui.components.AirlineChips
import com.aircargo.manager.ui.components.SectionTitle
import com.aircargo.manager.ui.components.SubScreen
import com.aircargo.manager.ui.theme.Navy
import java.util.Calendar
import java.util.Locale

@Composable
fun BookingFormScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    var airlineCode by rememberSaveable { mutableStateOf(vm.selectedAirline?.code) }
    var flightNo by rememberSaveable { mutableStateOf(vm.selectedAirline?.code ?: "") }
    var flightDate by rememberSaveable { mutableStateOf(today()) }
    var pcs by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var item by rememberSaveable { mutableStateOf("") }
    var dest by rememberSaveable { mutableStateOf("") }
    var remark by rememberSaveable { mutableStateOf("") }
    var tempControl by rememberSaveable { mutableStateOf(false) }
    var dryIce by rememberSaveable { mutableStateOf(false) }
    var company by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }

    val pcsVal = pcs.toIntOrNull()
    val weightVal = weight.toDoubleOrNull()
    val errors = buildMap<String, String> {
        if (airlineCode == null) put("airline", "항공사를 선택하세요")
        if (!Regex("^[A-Z0-9]{2}\\d{1,4}[A-Z]?$").matches(flightNo)) put("flight", "편명 형식 확인 (예: CI161)")
        if (pcsVal == null || pcsVal <= 0) put("pcs", "1 이상 숫자")
        if (weightVal == null || weightVal <= 0.0) put("weight", "0보다 큰 숫자")
        if (item.isBlank()) put("item", "품목 입력")
        if (!Regex("^[A-Z]{3}$").matches(dest)) put("dest", "공항코드 3자리 (예: TPE)")
        if (company.isBlank()) put("company", "회사명 입력")
        if (name.isBlank()) put("name", "예약자명 입력")
        if (phone.filter { it.isDigit() }.length < 9) put("phone", "연락처 확인")
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) put("email", "이메일 형식 확인")
    }
    fun err(k: String): String? = if (showErrors) errors[k] else null

    SubScreen(title = "화물 예약", onBack = onBack) { pad ->
        Column(
            Modifier
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. 항공사 선택
            FormCard("1. 항공사 선택") {
                AirlineChips(
                    selected = Airlines.byCode(airlineCode),
                    onSelect = { a ->
                        val old = airlineCode
                        airlineCode = a?.code
                        // 편명 접두어 자동 변경
                        if (a != null && (flightNo.isBlank() || (old != null && flightNo.startsWith(old)))) {
                            flightNo = a.code + flightNo.removePrefix(old ?: "")
                        }
                    },
                    includeAll = false,
                )
                err("airline")?.let { ErrorText(it) }
            }

            // 2. 운송 정보
            FormCard("2. 운송 정보") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("편명", flightNo, { flightNo = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                        err("flight"), Modifier.weight(1f), caps = true)
                    DateField(flightDate, Modifier.weight(1f)) {
                        val c = Calendar.getInstance()
                        flightDate.split("-").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }?.let {
                            c.set(it[0], it[1] - 1, it[2])
                        }
                        DatePickerDialog(context, { _, y, m, d ->
                            flightDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Pcs", pcs, { pcs = it.filter { c -> c.isDigit() } }, err("pcs"), Modifier.weight(1f),
                        keyboard = KeyboardType.Number)
                    Field("Weight (KG)", weight, { v -> weight = v.filter { it.isDigit() || it == '.' } },
                        err("weight"), Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                }
                Field("Item (품목)", item, { item = it }, err("item"))
                Field("Destination (공항코드)", dest, { dest = it.uppercase().filter { c -> c.isLetter() }.take(3) },
                    err("dest"), caps = true)

                Text("특수 취급", fontSize = 13.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CheckItem("온도유지", tempControl) { tempControl = it }
                    CheckItem("Dry Ice 탑재", dryIce) { dryIce = it }
                }
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Remark (온도 범위, Dry Ice 중량 등)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 3. 예약자 정보
            FormCard("3. 예약자 정보") {
                Field("예약자 회사", company, { company = it }, err("company"))
                Field("예약자명", name, { name = it }, err("name"))
                Field("예약자 전화번호", phone, { phone = it.filter { c -> c.isDigit() || c == '-' } }, err("phone"),
                    keyboard = KeyboardType.Phone)
                Field("예약자 이메일 (접수 확인 메일 수신, 선택)", email, { email = it.trim() }, err("email"),
                    keyboard = KeyboardType.Email)
            }

            Button(
                onClick = {
                    showErrors = true
                    if (errors.isEmpty()) confirm = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("예약 요청 등록", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            if (showErrors && errors.isNotEmpty()) {
                ErrorText("입력 항목 ${errors.size}건 확인 필요")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirm) {
        val draft = Booking(
            id = "",
            createdAt = System.currentTimeMillis(),
            airlineCode = airlineCode.orEmpty(),
            flightNo = flightNo,
            flightDate = flightDate,
            pieces = pcsVal ?: 0,
            weightKg = weightVal ?: 0.0,
            item = item.trim(),
            destination = dest,
            remark = remark.trim(),
            tempControl = tempControl,
            dryIce = dryIce,
            bookerCompany = company.trim(),
            bookerName = name.trim(),
            bookerPhone = phone.trim(),
            bookerEmail = email.trim(),
        )
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("예약 내용 확인") },
            text = {
                Text(draft.toShareText().lines().drop(1).dropLast(1).joinToString("\n"),
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addBooking(draft.copy(id = vm.newBookingId(), createdAt = System.currentTimeMillis()))
                    confirm = false
                    onDone()
                }) { Text("등록") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("수정") } },
        )
    }
}

private fun today(): String {
    val c = Calendar.getInstance()
    return String.format(Locale.US, "%04d-%02d-%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text,
    caps: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (caps) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
        ),
        modifier = modifier,
    )
}

@Composable
private fun DateField(value: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("날짜") },
            trailingIcon = { Icon(Icons.Filled.CalendarMonth, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // readOnly TextField는 클릭 이벤트를 소비하므로 투명 오버레이로 처리
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
private fun CheckItem(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.clickable { onChange(!checked) }.padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun ErrorText(msg: String) {
    Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}
