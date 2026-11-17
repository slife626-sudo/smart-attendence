package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.db.BranchEntity
import com.example.data.db.HolidayEntity
import com.example.data.db.LeaveTypeEntity
import com.example.data.db.ShiftEntity
import com.example.data.db.StaffEntity
import com.example.domain.AttendanceCalculator
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DatePickerField
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PunchGreen
import com.example.ui.theme.PunchRed
import com.example.ui.theme.TealInfo
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffSetupSection(
    staffList: List<StaffEntity>,
    branches: List<BranchEntity>,
    shifts: List<ShiftEntity>,
    onSaveStaff: (StaffEntity) -> Unit,
    onDeleteStaff: (String) -> Unit
) {
    var editing by remember { mutableStateOf<StaffEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<StaffEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "মোট কর্মী: ${AttendanceCalculator.toBanglaDigits(staffList.size)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
            Button(
                onClick = {
                    val nextNum = (staffList.mapNotNull { it.code.toIntOrNull() }.maxOrNull() ?: 0) + 1
                    editing = StaffEntity(
                        code = nextNum.toString().padStart(2, '0'),
                        name = "",
                        branch = branches.firstOrNull()?.branch.orEmpty(),
                        shift = shifts.firstOrNull()?.shift ?: "সাধারণ",
                        pin = "1234",
                        status = "Active"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                modifier = Modifier.testTag("add_staff_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন কর্মী")
            }
        }

        staffList.forEach { s ->
            val active = AttendanceCalculator.isActive(s)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${s.code} · ${s.name}", fontWeight = FontWeight.Bold)
                        Text("${s.designation} · ${s.branch} · ${s.shift}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(
                            text = (if (active) "সক্রিয়" else "নিষ্ক্রিয়") +
                                if (s.joinDate.isNotBlank()) " · যোগদান: ${AttendanceCalculator.formatDateBn(s.joinDate)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) PunchGreen else PunchRed
                        )
                    }
                    IconButton(onClick = { editing = s }) {
                        Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = EmeraldPrimary)
                    }
                    IconButton(onClick = { deleteTarget = s }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed)
                    }
                }
            }
        }
    }

    val ed = editing
    if (ed != null) {
        var code by remember(ed) { mutableStateOf(ed.code) }
        var name by remember(ed) { mutableStateOf(ed.name) }
        var designation by remember(ed) { mutableStateOf(ed.designation) }
        var branch by remember(ed) { mutableStateOf(ed.branch) }
        var shift by remember(ed) { mutableStateOf(ed.shift) }
        var pin by remember(ed) { mutableStateOf(ed.pin) }
        var active by remember(ed) { mutableStateOf(AttendanceCalculator.isActive(ed)) }
        var weeklyOff by remember(ed) { mutableStateOf(ed.weeklyOff) }
        var joinDate by remember(ed) { mutableStateOf(ed.joinDate) }
        var leaveDate by remember(ed) { mutableStateOf(ed.leaveDate) }
        var reservedDays by remember(ed) { mutableStateOf(ed.reservedDays) }
        var probationMonths by remember(ed) { mutableStateOf(ed.probationMonths) }

        var bExp by remember { mutableStateOf(false) }
        var shExp by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (ed.docId.isBlank()) "নতুন কর্মী" else "কর্মী সম্পাদনা", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("কোড") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("পদবি") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    ExposedDropdownMenuBox(expanded = bExp, onExpandedChange = { bExp = it }) {
                        OutlinedTextField(
                            value = branch,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("শাখা") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bExp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = bExp, onDismissRequest = { bExp = false }) {
                            branches.forEach { b ->
                                DropdownMenuItem(text = { Text(b.branch) }, onClick = { branch = b.branch; bExp = false })
                            }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = shExp, onExpandedChange = { shExp = it }) {
                        OutlinedTextField(
                            value = shift,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("শিফট") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(shExp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = shExp, onDismissRequest = { shExp = false }) {
                            shifts.forEach { sh ->
                                DropdownMenuItem(text = { Text(sh.shift) }, onClick = { shift = sh.shift; shExp = false })
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("সক্রিয় (Active)")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }

                    OutlinedTextField(value = weeklyOff, onValueChange = { weeklyOff = it }, label = { Text("সাপ্তাহিক ছুটি (ফাঁকা = ডিফল্ট)") }, placeholder = { Text("উদা: Fri বা Sun,Thu") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DatePickerField("যোগদান", joinDate, { joinDate = it }, Modifier.fillMaxWidth(), allowClear = true)
                    DatePickerField("শেষ দিন", leaveDate, { leaveDate = it }, Modifier.fillMaxWidth(), allowClear = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = reservedDays, onValueChange = { reservedDays = it }, label = { Text("সংরক্ষিত ছুটি (দিন)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = probationMonths, onValueChange = { probationMonths = it }, label = { Text("শিক্ষানবিস (মাস)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = code.isNotBlank() && name.isNotBlank(),
                    onClick = {
                        onSaveStaff(
                            ed.copy(
                                code = code.trim(),
                                name = name.trim(),
                                designation = designation.trim(),
                                branch = branch.trim(),
                                shift = shift.trim(),
                                pin = pin.trim().ifBlank { "1234" },
                                status = if (active) "Active" else "Inactive",
                                weeklyOff = weeklyOff.trim(),
                                joinDate = joinDate.trim(),
                                leaveDate = leaveDate.trim(),
                                reservedDays = reservedDays.trim(),
                                probationMonths = probationMonths.trim()
                            )
                        )
                        editing = null
                    }
                ) { Text("সংরক্ষণ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("বাতিল") } }
        )
    }

    val del = deleteTarget
    if (del != null) {
        ConfirmActionDialog(
            title = "কর্মী মুছবেন?",
            message = "${del.code} · ${del.name} কে কর্মী তালিকা থেকে মুছে ফেলা হবে।",
            onConfirm = { onDeleteStaff(del.docId); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
fun AdminLeaveTypesSetupSection(
    leaveTypes: List<LeaveTypeEntity>,
    onSave: (LeaveTypeEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<LeaveTypeEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ছুটির ধরনসমূহ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Button(
                onClick = { editing = LeaveTypeEntity(ltype = "", days = "10", paid = "Yes", kind = "Leave", probation = "0") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("যোগ")
            }
        }

        leaveTypes.forEach { lt ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lt.ltype, fontWeight = FontWeight.Bold)
                        Text(
                            text = "দিন: ${lt.days.ifBlank { "সীমাহীন" }} · ${if (lt.paid.equals("No", true)) "বিনা বেতন" else "সবেতন"} · ধরন: ${if (lt.kind.equals("Holiday", true)) "বন্ধ" else "ছুটি"} · শিক্ষানবিস: ${lt.probation.ifBlank { "০" }} মাস",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = { editing = lt }) {
                        Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = EmeraldPrimary)
                    }
                    IconButton(onClick = { onDelete(lt.docId) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed)
                    }
                }
            }
        }
    }

    val ed = editing
    if (ed != null) {
        var ltype by remember(ed) { mutableStateOf(ed.ltype) }
        var days by remember(ed) { mutableStateOf(ed.days) }
        var paid by remember(ed) { mutableStateOf(!ed.paid.equals("No", true)) }
        var isHol by remember(ed) { mutableStateOf(ed.kind.equals("Holiday", true)) }
        var prob by remember(ed) { mutableStateOf(ed.probation) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("ছুটির ধরন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ltype, onValueChange = { ltype = it }, label = { Text("নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("বাৎসরিক দিন (ফাঁকা = ∞)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = prob, onValueChange = { prob = it }, label = { Text("শিক্ষানবিস মাস") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("সবেতন (Paid)")
                        Switch(checked = paid, onCheckedChange = { paid = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("প্রতিষ্ঠান বন্ধের মতো (Holiday)")
                        Switch(checked = isHol, onCheckedChange = { isHol = it })
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = ltype.isNotBlank(), onClick = {
                    onSave(ed.copy(ltype = ltype.trim(), days = days.trim(), paid = if (paid) "Yes" else "No", kind = if (isHol) "Holiday" else "Leave", probation = prob.trim()))
                    editing = null
                }) { Text("সংরক্ষণ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("বাতিল") } }
        )
    }
}

@Composable
fun AdminHolidaysSetupSection(
    holidays: List<HolidayEntity>,
    branches: List<BranchEntity>,
    todayReal: String,
    onSave: (HolidayEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<HolidayEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("সরকারি ছুটি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Button(
                onClick = { editing = HolidayEntity(dateFrom = todayReal, dateTo = todayReal, name = "", branch = "") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("যোগ")
            }
        }

        holidays.sortedBy { it.dateFrom }.forEach { h ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(h.name.ifBlank { "সরকারি ছুটি" }, fontWeight = FontWeight.Bold)
                        Text("${AttendanceCalculator.formatDateBn(h.dateFrom)} — ${AttendanceCalculator.formatDateBn(h.dateTo.ifBlank { h.dateFrom })}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("শাখা: ${h.branch.ifBlank { "সব শাখা" }}", style = MaterialTheme.typography.labelSmall, color = TealInfo)
                    }
                    IconButton(onClick = { editing = h }) { Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = EmeraldPrimary) }
                    IconButton(onClick = { onDelete(h.docId) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed) }
                }
            }
        }
    }

    val ed = editing
    if (ed != null) {
        var name by remember(ed) { mutableStateOf(ed.name) }
        var from by remember(ed) { mutableStateOf(ed.dateFrom) }
        var to by remember(ed) { mutableStateOf(ed.dateTo.ifBlank { ed.dateFrom }) }
        val selectedBranches = remember(ed) {
            mutableStateListOf<String>().apply {
                addAll(ed.branch.split(',').map { it.trim() }.filter { it.isNotEmpty() })
            }
        }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("সরকারি ছুটি", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ছুটির নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DatePickerField("থেকে", from, { from = it; if (to < it) to = it }, Modifier.fillMaxWidth())
                    DatePickerField("পর্যন্ত", to, { to = it }, Modifier.fillMaxWidth())
                    Text("প্রযোজ্য শাখা (সব ফাঁকা রাখলে সব শাখায় প্রযোজ্য):", style = MaterialTheme.typography.labelMedium)
                    branches.forEach { b ->
                        val checked = selectedBranches.any { it.equals(b.branch, true) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = { isC ->
                                if (isC) selectedBranches.add(b.branch) else selectedBranches.removeAll { it.equals(b.branch, true) }
                            })
                            Text(b.branch, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && from.isNotBlank(), onClick = {
                    onSave(ed.copy(name = name.trim(), dateFrom = from, dateTo = to.ifBlank { from }, branch = selectedBranches.joinToString(", ")))
                    editing = null
                }) { Text("সংরক্ষণ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("বাতিল") } }
        )
    }
}
