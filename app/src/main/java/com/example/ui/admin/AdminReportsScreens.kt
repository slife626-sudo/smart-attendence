package com.example.ui.admin

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.AttendanceEntity
import com.example.data.db.BreakEntity
import com.example.data.db.StaffEntity
import com.example.domain.AttendanceCalculator
import com.example.domain.BreakReportStaffRow
import com.example.domain.StaffMonthlyReport
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.ExpandableHintBanner
import com.example.ui.components.KpiCard
import com.example.ui.components.OrgBannerHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TimePickerField
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PunchGreen
import com.example.ui.theme.PunchRed
import com.example.ui.theme.PurpleOvertime
import com.example.ui.theme.TealInfo
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffReportSection(
    report: StaffMonthlyReport?,
    staffList: List<StaffEntity>,
    selectedStaffCode: String,
    selectedMonth: String,
    onSelectStaff: (String) -> Unit
) {
    val context = LocalContext.current
    var dropdownOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = dropdownOpen,
                onExpandedChange = { dropdownOpen = it },
                modifier = Modifier.weight(1f)
            ) {
                val cur = staffList.find { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(selectedStaffCode) }
                OutlinedTextField(
                    value = if (cur != null) "${cur.code} — ${cur.name}" else selectedStaffCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("কর্মী নির্বাচন") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("report_staff_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false }
                ) {
                    staffList.forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s.code} — ${s.name} (${s.branch})") },
                            onClick = {
                                dropdownOpen = false
                                onSelectStaff(s.code)
                            }
                        )
                    }
                }
            }
            if (report != null) {
                Button(
                    onClick = { printStaffReport(context, report, selectedMonth) },
                    modifier = Modifier.height(54.dp).testTag("print_staff_report_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "প্রিন্ট বা PDF")
                }
            }
        }

        if (report == null) {
            Text("কোনো কর্মী পাওয়া যায়নি", color = TextMuted)
            return
        }

        OrgBannerHeader(
            orgName = report.org,
            address = report.addr,
            subtitle = "মাসিক হাজিরা রিপোর্ট — ${AttendanceCalculator.monthLabelBn(selectedMonth)}"
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${report.code} · ${report.name} (${report.designation})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "শাখা: ${report.branch} · শিফট: ${AttendanceCalculator.toBanglaDigits(report.shiftStart)}-${AttendanceCalculator.toBanglaDigits(report.shiftEnd)} · গ্রেস: ${AttendanceCalculator.toBanglaDigits(report.grace)} মি",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                if (report.joinDate.isNotBlank() || report.leaveDate.isNotBlank()) {
                    Text(
                        "যোগদান: ${if (report.joinDate.isNotBlank()) AttendanceCalculator.formatDateBn(report.joinDate) else "-"}" +
                            if (report.leaveDate.isNotBlank()) " · শেষ দিন: ${AttendanceCalculator.formatDateBn(report.leaveDate)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // KPI Summary Tiles (2 rows x 4)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("কর্মদিবস", AttendanceCalculator.toBanglaDigits(report.sum.work), "", EmeraldPrimary, Modifier.weight(1f))
            KpiCard("উপস্থিত", AttendanceCalculator.toBanglaDigits(report.sum.present), "", PunchGreen, Modifier.weight(1f))
            KpiCard("দেরি", AttendanceCalculator.toBanglaDigits(report.sum.late), "", AmberAccent, Modifier.weight(1f))
            KpiCard("অনুপস্থিত", AttendanceCalculator.toBanglaDigits(report.sum.absent), "", PunchRed, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("সবেতন ছুটি", AttendanceCalculator.toBanglaDigits(report.sum.leavePaid), "অবৈতনিক ${AttendanceCalculator.toBanglaDigits(report.sum.leaveUnpaid)}", TealInfo, Modifier.weight(1f))
            KpiCard("সরকারি/সাপ্তাহিক", AttendanceCalculator.toBanglaDigits(report.sum.holiday + report.sum.off), "", BlueAccent, Modifier.weight(1f))
            KpiCard("মোট কর্মঘণ্টা", AttendanceCalculator.formatHmShort(report.sum.workMin), "OT ${AttendanceCalculator.formatHmShort(report.sum.otMin)}", PurpleOvertime, Modifier.weight(1f))
            KpiCard("ব্রেক", AttendanceCalculator.formatBreakMin(report.sum.brkMin), "${AttendanceCalculator.toBanglaDigits(report.sum.brkN)} বার", AmberAccent, Modifier.weight(1f))
        }

        // Yearly leave balance summary line
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${AttendanceCalculator.toBanglaDigits(report.year)} ছুটির ব্যালেন্স:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                report.leaveBalances.forEach { b ->
                    val txt = when {
                        b.quota == 0 && b.from.isNotEmpty() -> "${b.type}: শিক্ষানবিস (${b.from.take(7)} থেকে)"
                        b.paid && b.quota != null -> "${b.type}: ${AttendanceCalculator.toBanglaDigits(b.used)}/${AttendanceCalculator.toBanglaDigits(b.quota)} (বাকি ${AttendanceCalculator.toBanglaDigits(b.left ?: 0)})"
                        else -> "${b.type}: ব্যবহৃত ${AttendanceCalculator.toBanglaDigits(b.used)}"
                    }
                    Text(txt, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Daily Attendance Breakdown
        report.days.forEach { d ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${AttendanceCalculator.formatDateBn(d.date)} (${AttendanceCalculator.dayNameBn(d.dayName)})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "IN: ${AttendanceCalculator.formatTime12h(d.inTime)} · OUT: ${AttendanceCalculator.formatTime12h(d.outTime)}" +
                                if (d.mins > 0) " · ${AttendanceCalculator.formatHmShort(d.mins)}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        val brk = AttendanceCalculator.breakTextPlain(d.breakSummary)
                        if (brk != "-") {
                            Text("ব্রেক: $brk", style = MaterialTheme.typography.labelSmall, color = AmberAccent)
                        }
                        if (d.note.isNotBlank()) {
                            Text(d.note, style = MaterialTheme.typography.labelSmall, color = TealInfo)
                        }
                    }
                    StatusBadge(text = d.status, cls = d.cls)
                }
            }
        }
    }
}

@Composable
fun AdminBreakReportSection(
    rows: List<BreakReportStaffRow>,
    from: String,
    to: String,
    onRangeChange: (String, String) -> Unit
) {
    val totalMin = rows.sumOf { it.totalMin }
    val totalN = rows.sumOf { it.totalCount }
    val badCount = rows.sumOf { it.incompleteCount }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatePickerField("থেকে", from, { onRangeChange(it, to) }, Modifier.weight(1f))
                DatePickerField("পর্যন্ত", to, { onRangeChange(from, it) }, Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("মোট ব্রেক সময়", AttendanceCalculator.formatHmShort(totalMin), "", AmberAccent, Modifier.weight(1f))
            KpiCard("মোট বার", AttendanceCalculator.toBanglaDigits(totalN), "", EmeraldPrimary, Modifier.weight(1f))
            KpiCard("অসম্পূর্ণ", AttendanceCalculator.toBanglaDigits(badCount), "শেষ পাঞ্চ নেই", PunchRed, Modifier.weight(1f))
        }

        rows.forEach { r ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${r.staff.code} · ${r.staff.name}", fontWeight = FontWeight.Bold)
                            Text(
                                text = "${r.branchAtToday} · ${AttendanceCalculator.toBanglaDigits(r.daysCount)} দিন · ${AttendanceCalculator.toBanglaDigits(r.totalCount)} বার · মোট ${AttendanceCalculator.formatBreakMin(r.totalMin)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            if (r.maxMin > 0) {
                                Text(
                                    text = "গড়: ${AttendanceCalculator.formatBreakMin(r.avgMin)} · সর্বোচ্চ: ${AttendanceCalculator.formatBreakMin(r.maxMin)} (${AttendanceCalculator.formatDateBn(r.maxDate)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TealInfo
                                )
                            }
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "বিস্তারিত"
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            HorizontalDivider()
                            if (r.items.isEmpty()) {
                                Text("কোনো ব্রেক রেকর্ড নেই", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            } else {
                                r.items.forEach { b ->
                                    val sm = AttendanceCalculator.parseMinutes(b.startTime)
                                    val em = AttendanceCalculator.parseMinutes(b.endTime)
                                    val dur = if (sm != null && em != null && em >= sm) "${AttendanceCalculator.formatBreakMin(em - sm)}" else "চলমান/অসম্পূর্ণ"
                                    Text(
                                        text = "${AttendanceCalculator.formatDateBn(b.date)} · ${AttendanceCalculator.formatTime12h(b.startTime)} → ${if (b.endTime.isNotBlank()) AttendanceCalculator.formatTime12h(b.endTime) else "—"} ($dur)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManualCorrectionSection(
    staffList: List<StaffEntity>,
    attendanceList: List<AttendanceEntity>,
    breaksList: List<BreakEntity>,
    todayReal: String,
    timezone: String,
    selectedStaffCode: String,
    selectedDate: String,
    onSelectStaff: (String) -> Unit,
    onSelectDate: (String) -> Unit,
    onSavePunch: (code: String, date: String, inT: String, outT: String, note: String) -> Unit,
    onDeletePunch: (code: String, date: String) -> Unit,
    onSaveBreak: (id: Int, code: String, date: String, start: String, end: String) -> Unit,
    onDeleteBreak: (id: Int) -> Unit
) {
    var staffMenu by remember { mutableStateOf(false) }
    var inTime by remember { mutableStateOf("") }
    var outTime by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var confirmDeletePunch by remember { mutableStateOf(false) }

    var editingBreak by remember { mutableStateOf<BreakEntity?>(null) }
    var showAddBreak by remember { mutableStateOf(false) }

    val ncCode = AttendanceCalculator.nc(selectedStaffCode)
    val existingAtt = remember(attendanceList, ncCode, selectedDate) {
        attendanceList.find { it.date == selectedDate && AttendanceCalculator.nc(it.code) == ncCode }
    }
    val dayBreaks = remember(breaksList, ncCode, selectedDate) {
        breaksList.filter { it.date == selectedDate && AttendanceCalculator.nc(it.code) == ncCode }
            .sortedBy { it.startTime }
    }

    LaunchedEffect(existingAtt, selectedStaffCode, selectedDate) {
        inTime = existingAtt?.inTime.orEmpty()
        outTime = existingAtt?.outTime.orEmpty()
        note = existingAtt?.note.orEmpty()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExpandableHintBanner("কোনো কর্মী পাঞ্চ করতে ভুলে গেলে বা সময় সংশোধন করতে চাইলে এখান থেকে IN/OUT ও ব্রেক সংরক্ষণ বা মুছতে পারবেন।")

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = staffMenu,
                    onExpandedChange = { staffMenu = it }
                ) {
                    val cur = staffList.find { AttendanceCalculator.nc(it.code) == ncCode }
                    OutlinedTextField(
                        value = if (cur != null) "${cur.code} — ${cur.name}" else selectedStaffCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("কর্মী") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = staffMenu, onDismissRequest = { staffMenu = false }) {
                        staffList.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.code} — ${s.name}") },
                                onClick = {
                                    staffMenu = false
                                    onSelectStaff(s.code)
                                }
                            )
                        }
                    }
                }

                DatePickerField("তারিখ", selectedDate, onSelectDate, Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimePickerField("IN সময়", inTime, { inTime = it }, Modifier.weight(1f))
                    TimePickerField("OUT সময়", outTime, { outTime = it }, Modifier.weight(1f))
                }

                // Quick now buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { inTime = AttendanceCalculator.nowInTimezone(timezone).second },
                        modifier = Modifier.weight(1f)
                    ) { Text("এখনই IN") }
                    OutlinedButton(
                        onClick = { outTime = AttendanceCalculator.nowInTimezone(timezone).second },
                        modifier = Modifier.weight(1f)
                    ) { Text("এখনই OUT") }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট / মন্তব্য") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSavePunch(selectedStaffCode, selectedDate, inTime, outTime, note) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("save_manual_punch_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                    if (existingAtt != null) {
                        OutlinedButton(
                            onClick = { confirmDeletePunch = true },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PunchRed)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন")
                        }
                    }
                }
            }
        }

        // Daily Breaks Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Coffee, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("এই দিনের ব্রেক (${AttendanceCalculator.toBanglaDigits(dayBreaks.size)})", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { showAddBreak = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ব্রেক যোগ")
                    }
                }

                if (dayBreaks.isEmpty()) {
                    Text("কোনো ব্রেক নেই", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    dayBreaks.forEach { b ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${AttendanceCalculator.formatTime12h(b.startTime)} → ${if (b.endTime.isNotBlank()) AttendanceCalculator.formatTime12h(b.endTime) else "চলমান"}",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = { editingBreak = b }) {
                                    Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = EmeraldPrimary)
                                }
                                IconButton(onClick = { onDeleteBreak(b.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDeletePunch) {
        ConfirmActionDialog(
            title = "হাজিরা মুছবেন?",
            message = "$selectedStaffCode কর্মীর ${AttendanceCalculator.formatDateBn(selectedDate)} তারিখের হাজিরা ও ব্রেক মুছে ফেলা হবে।",
            onConfirm = {
                confirmDeletePunch = false
                onDeletePunch(selectedStaffCode, selectedDate)
            },
            onDismiss = { confirmDeletePunch = false }
        )
    }

    val brkToEdit = editingBreak ?: if (showAddBreak) BreakEntity(date = selectedDate, code = selectedStaffCode, startTime = "13:30", endTime = "14:00") else null
    if (brkToEdit != null) {
        var st by remember(brkToEdit) { mutableStateOf(brkToEdit.startTime) }
        var en by remember(brkToEdit) { mutableStateOf(brkToEdit.endTime) }
        AlertDialog(
            onDismissRequest = { editingBreak = null; showAddBreak = false },
            title = { Text(if (brkToEdit.id == 0) "নতুন ব্রেক যোগ" else "ব্রেক সংশোধন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimePickerField("শুরু", st, { st = it }, Modifier.fillMaxWidth(), allowClear = false)
                    TimePickerField("শেষ (ঐচ্ছিক)", en, { en = it }, Modifier.fillMaxWidth(), allowClear = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveBreak(brkToEdit.id, selectedStaffCode, selectedDate, st, en)
                    editingBreak = null
                    showAddBreak = false
                }) { Text("সংরক্ষণ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editingBreak = null; showAddBreak = false }) { Text("বাতিল") }
            }
        )
    }
}

private fun printStaffReport(context: Context, report: StaffMonthlyReport, month: String) {
    val rowsHtml = report.days.joinToString("") { d ->
        "<tr><td>${d.date}</td><td>${AttendanceCalculator.dayNameBn(d.dayName)}</td><td>${d.inTime.ifBlank { "-" }}</td><td>${d.outTime.ifBlank { "-" }}</td><td>${d.status}</td><td>${d.note}</td></tr>"
    }
    val html = """
        <!DOCTYPE html><html lang="bn"><head><meta charset="utf-8">
        <style>body{font-family:sans-serif;padding:16px}h2,p{margin:4px 0}table{width:100%;border-collapse:collapse;margin-top:12px;font-size:12px}th,td{border:1px solid #cbd5e1;padding:6px;text-align:left}th{background:#f1f5f9}</style>
        </head><body>
        <h2>${report.org}</h2>
        <p>${report.addr}</p>
        <p><strong>${report.code} · ${report.name}</strong> (${report.designation}) — শাখা: ${report.branch} · মাস: $month</p>
        <p>কর্মদিবস: ${report.sum.work} | উপস্থিত: ${report.sum.present} | দেরি: ${report.sum.late} | অনুপস্থিত: ${report.sum.absent} | ছুটি: ${report.sum.leave}</p>
        <table><thead><tr><th>তারিখ</th><th>বার</th><th>IN</th><th>OUT</th><th>অবস্থা</th><th>নোট</th></tr></thead>
        <tbody>$rowsHtml</tbody></table>
        </body></html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val adapter = view.createPrintDocumentAdapter("Report_${report.code}_$month")
            printManager.print("Report_${report.code}_$month", adapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}
