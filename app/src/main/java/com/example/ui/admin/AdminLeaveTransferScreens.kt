package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.BranchEntity
import com.example.data.db.HolidayEntity
import com.example.data.db.LeaveEntity
import com.example.data.db.LeaveTypeEntity
import com.example.data.db.StaffEntity
import com.example.data.db.TransferEntity
import com.example.domain.AttendanceCalculator
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.ExpandableHintBanner
import com.example.ui.staff.FaceCaptureDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PunchGreen
import com.example.ui.theme.PunchRed
import com.example.ui.theme.TealInfo
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeaveAppsSection(
    staffList: List<StaffEntity>,
    leaveTypes: List<LeaveTypeEntity>,
    leaves: List<LeaveEntity>,
    holidays: List<HolidayEntity>,
    transfers: List<TransferEntity>,
    settings: Map<String, String>,
    todayReal: String,
    onAddLeave: (code: String, type: String, from: String, to: String, reason: String) -> Unit,
    onDecideLeave: (docId: String, status: String, note: String) -> Unit,
    onDeleteLeave: (docId: String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var rejectTarget by remember { mutableStateOf<LeaveEntity?>(null) }
    var rejectNote by remember { mutableStateOf("") }

    val (ltMap, mainName) = remember(leaveTypes) { AttendanceCalculator.parseLeaveTypes(leaveTypes) }
    val filtered = remember(leaves, statusFilter) {
        val sorted = leaves.sortedByDescending { it.appliedOn }
        when (statusFilter) {
            "Pending" -> sorted.filter { it.status.equals("Pending", true) }
            "Approved" -> sorted.filter { AttendanceCalculator.isApproved(it) }
            "Rejected" -> sorted.filter { it.status.equals("Rejected", true) }
            else -> sorted
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All" to "সব", "Pending" to "অপেক্ষমাণ", "Approved" to "অনুমোদিত", "Rejected" to "প্রত্যাখ্যাত").forEach { (key, lbl) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(lbl) }
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                modifier = Modifier.testTag("admin_add_leave_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("যোগ")
            }
        }

        if (filtered.isEmpty()) {
            Text("কোনো ছুটির আবেদন নেই", color = TextMuted)
        }

        filtered.forEach { lv ->
            val stObj = staffList.find { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(lv.code) }
            val preview = remember(lv, leaves, holidays, transfers) {
                AttendanceCalculator.impactPreview(lv, stObj, leaves, ltMap, mainName, holidays, transfers, settings)
            }
            val hols = remember(stObj, holidays, transfers) {
                if (stObj != null) AttendanceCalculator.holMap(holidays, stObj, transfers) else emptyMap()
            }
            val offs = remember(stObj, settings) {
                if (stObj != null) AttendanceCalculator.weeklyOffSet(stObj, settings) else setOf("Fri")
            }
            val workDays = AttendanceCalculator.workDaysCount(lv.dateFrom, lv.dateTo, hols, offs)
            val isPending = lv.status.equals("Pending", true)
            val stColor = when {
                isPending -> AmberAccent
                lv.status.equals("Rejected", true) -> PunchRed
                else -> PunchGreen
            }
            val stLabel = when {
                isPending -> "অপেক্ষমাণ"
                lv.status.equals("Rejected", true) -> "প্রত্যাখ্যাত"
                else -> "অনুমোদিত"
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${lv.code} · ${stObj?.name ?: lv.code}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Surface(shape = RoundedCornerShape(999.dp), color = stColor.copy(alpha = 0.14f)) {
                            Text(
                                text = stLabel,
                                color = stColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "${lv.typ} · ${AttendanceCalculator.formatDateBn(lv.dateFrom)} — ${AttendanceCalculator.formatDateBn(lv.dateTo)} (কর্মদিবস ${AttendanceCalculator.toBanglaDigits(workDays)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (lv.reason.isNotBlank()) {
                        Text("কারণ: ${lv.reason}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    if (isPending && preview.isNotBlank()) {
                        Text(preview, style = MaterialTheme.typography.labelSmall, color = TealInfo, fontWeight = FontWeight.Bold)
                    }
                    if (lv.note.isNotBlank()) {
                        Text("অ্যাডমিন নোট: ${lv.note}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPending) {
                            OutlinedButton(
                                onClick = {
                                    rejectNote = ""
                                    rejectTarget = lv
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PunchRed)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("প্রত্যাখ্যান")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onDecideLeave(lv.docId, "Approved", "") },
                                colors = ButtonDefaults.buttonColors(containerColor = PunchGreen, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("অনুমোদন")
                            }
                        }
                        IconButton(onClick = { onDeleteLeave(lv.docId) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var code by remember { mutableStateOf(staffList.firstOrNull()?.code ?: "01") }
        var typ by remember { mutableStateOf(leaveTypes.firstOrNull()?.ltype ?: "সংরক্ষিত ছুটি") }
        var from by remember { mutableStateOf(todayReal) }
        var to by remember { mutableStateOf(todayReal) }
        var reason by remember { mutableStateOf("") }
        var sExp by remember { mutableStateOf(false) }
        var tExp by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("সরাসরি ছুটি যোগ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = sExp, onExpandedChange = { sExp = it }) {
                        val curS = staffList.find { it.code == code }
                        OutlinedTextField(
                            value = if (curS != null) "${curS.code} — ${curS.name}" else code,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("কর্মী") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sExp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = sExp, onDismissRequest = { sExp = false }) {
                            staffList.forEach { s ->
                                DropdownMenuItem(text = { Text("${s.code} — ${s.name}") }, onClick = { code = s.code; sExp = false })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = tExp, onExpandedChange = { tExp = it }) {
                        OutlinedTextField(
                            value = typ,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ছুটির ধরন") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tExp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = tExp, onDismissRequest = { tExp = false }) {
                            leaveTypes.forEach { lt ->
                                DropdownMenuItem(text = { Text(lt.ltype) }, onClick = { typ = lt.ltype; tExp = false })
                            }
                        }
                    }
                    DatePickerField("থেকে", from, { from = it; if (to < it) to = it }, Modifier.fillMaxWidth())
                    DatePickerField("পর্যন্ত", to, { to = it }, Modifier.fillMaxWidth())
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("কারণ") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddLeave(code, typ, from, to, reason)
                    showAddDialog = false
                }) { Text("যোগ করুন", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("বাতিল") }
            }
        )
    }

    val rej = rejectTarget
    if (rej != null) {
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("ছুটি প্রত্যাখ্যান", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = rejectNote,
                    onValueChange = { rejectNote = it },
                    label = { Text("কারণ / নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDecideLeave(rej.docId, "Rejected", rejectNote)
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PunchRed)
                ) { Text("প্রত্যাখ্যান", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("বাতিল") } }
        )
    }
}

@Composable
fun AdminLeaveBalanceSection(
    staffList: List<StaffEntity>,
    leaveTypes: List<LeaveTypeEntity>,
    leaves: List<LeaveEntity>,
    holidays: List<HolidayEntity>,
    transfers: List<TransferEntity>,
    settings: Map<String, String>,
    selectedMonth: String,
    selectedBranch: String
) {
    val year = selectedMonth.take(4).toIntOrNull() ?: 2026
    val (ltMap, mainName) = remember(leaveTypes) { AttendanceCalculator.parseLeaveTypes(leaveTypes) }
    val bl = selectedBranch.trim().lowercase()
    val scoped = remember(staffList, selectedBranch) {
        staffList.filter { AttendanceCalculator.isActive(it) && (bl.isEmpty() || it.branch.lowercase() == bl) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "${AttendanceCalculator.toBanglaDigits(year)} — কর্মীদের বাৎসরিক ছুটির হিসাব",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
        scoped.forEach { s ->
            val hols = AttendanceCalculator.holMap(holidays, s, transfers)
            val offs = AttendanceCalculator.weeklyOffSet(s, settings)
            val calc = AttendanceCalculator.leaveCalc(s, leaves, ltMap, mainName, hols, offs, year)
            val bals = AttendanceCalculator.balOf(s, ltMap, mainName, calc, year)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${s.code} · ${s.name} (${s.branch})", fontWeight = FontWeight.Bold)
                    bals.forEach { b ->
                        val qStr = b.quota?.let { AttendanceCalculator.toBanglaDigits(it) } ?: "∞"
                        val remStr = b.left?.let { AttendanceCalculator.toBanglaDigits(it) } ?: "—"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(b.type, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(
                                text = "ব্যব: ${AttendanceCalculator.toBanglaDigits(b.used)} / $qStr · বাকি: $remStr" +
                                    if (b.unpaid > 0) " · অবৈতনিক: ${AttendanceCalculator.toBanglaDigits(b.unpaid)}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (b.unpaid > 0) PunchRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTransferSection(
    staffList: List<StaffEntity>,
    branches: List<BranchEntity>,
    transfers: List<TransferEntity>,
    todayReal: String,
    onAddTransfer: (code: String, effDate: String, newBranch: String) -> Unit,
    onDeleteTransfer: (docId: String) -> Unit
) {
    var code by remember { mutableStateOf(staffList.firstOrNull()?.code ?: "01") }
    var newBranch by remember { mutableStateOf(branches.firstOrNull()?.branch ?: "") }
    var effDate by remember { mutableStateOf(todayReal) }
    var sExp by remember { mutableStateOf(false) }
    var bExp by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExpandableHintBanner("বদলি যোগ করলে ওই কার্যকর তারিখ থেকে কর্মীর শাখা ও জিওফেন্স হিসাব স্বয়ংক্রিয়ভাবে নতুন শাখায় পরিবর্তিত হবে।")

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("নতুন বদলি যোগ", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                ExposedDropdownMenuBox(expanded = sExp, onExpandedChange = { sExp = it }) {
                    val curS = staffList.find { it.code == code }
                    OutlinedTextField(
                        value = if (curS != null) "${curS.code} — ${curS.name} (${curS.branch})" else code,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("কর্মী") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = sExp, onDismissRequest = { sExp = false }) {
                        staffList.forEach { s ->
                            DropdownMenuItem(text = { Text("${s.code} — ${s.name} (${s.branch})") }, onClick = { code = s.code; sExp = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = bExp, onExpandedChange = { bExp = it }) {
                    OutlinedTextField(
                        value = newBranch,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("নতুন শাখা") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = bExp, onDismissRequest = { bExp = false }) {
                        branches.forEach { b ->
                            DropdownMenuItem(text = { Text(b.branch) }, onClick = { newBranch = b.branch; bExp = false })
                        }
                    }
                }

                DatePickerField("কার্যকর তারিখ", effDate, { effDate = it }, Modifier.fillMaxWidth())

                Button(
                    onClick = { onAddTransfer(code, effDate, newBranch) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("বদলি সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }

        transfers.sortedByDescending { it.effDate }.forEach { t ->
            val stObj = staffList.find { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(t.code) }
            val isFuture = t.effDate > todayReal
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${t.code} · ${stObj?.name ?: ""}", fontWeight = FontWeight.Bold)
                        Text("${t.fromBranch} → ${t.toBranch}", style = MaterialTheme.typography.bodyMedium, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "তারিখ: ${AttendanceCalculator.formatDateBn(t.effDate)} · ${if (isFuture) "ভবিষ্যতে" else "কার্যকর"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFuture) AmberAccent else PunchGreen
                        )
                    }
                    IconButton(onClick = { onDeleteTransfer(t.docId) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminFaceEnrollmentSection(
    staffList: List<StaffEntity>,
    onSaveFace: (code: String, vectors: List<List<Double>>) -> Unit,
    onDeleteFace: (code: String) -> Unit
) {
    var enrollTarget by remember { mutableStateOf<StaffEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<StaffEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExpandableHintBanner("কর্মীর মুখ নিবন্ধন করলে মোবাইল পাঞ্চের সময় সামনের ক্যামেরা ও চোখ খোলা থাকার যাচাই সক্রিয় হবে। নিবন্ধনে ৮টি স্যাম্পল নেওয়া হয়।")

        staffList.filter { AttendanceCalculator.isActive(it) }.forEach { s ->
            val enrolled = s.faceJson.isNotBlank()
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
                        Text(
                            text = if (enrolled) "✅ মুখ নিবন্ধিত আছে" else "⚠ এখনো নিবন্ধিত নয়",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enrolled) PunchGreen else TextMuted
                        )
                    }
                    Button(
                        onClick = { enrollTarget = s },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (enrolled) "পুনরায়" else "নিবন্ধন")
                    }
                    if (enrolled) {
                        IconButton(onClick = { deleteTarget = s }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "মুখ মুছুন", tint = PunchRed)
                        }
                    }
                }
            }
        }
    }

    val target = enrollTarget
    if (target != null) {
        FaceCaptureDialog(
            title = "মুখ নিবন্ধন — ${target.name} (${target.code})",
            targetSampleCount = 8,
            requireFrontalEyesOpen = true,
            onCaptured = { vecs ->
                enrollTarget = null
                onSaveFace(target.code, vecs)
            },
            onDismiss = { enrollTarget = null }
        )
    }

    val del = deleteTarget
    if (del != null) {
        ConfirmActionDialog(
            title = "মুখ-নিবন্ধন মুছবেন?",
            message = "${del.name} (${del.code}) এর সংরক্ষিত মুখের তথ্য মুছে ফেলা হবে।",
            onConfirm = {
                onDeleteFace(del.code)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
