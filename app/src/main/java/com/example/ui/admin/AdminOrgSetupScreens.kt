package com.example.ui.admin

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.db.BranchEntity
import com.example.data.db.OrgWorkspace
import com.example.data.db.SettingEntity
import com.example.data.db.ShiftEntity
import com.example.domain.AttendanceCalculator
import com.example.ui.components.TimePickerField
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PunchRed
import com.example.ui.theme.TextMuted

@Composable
fun AdminShiftsSetupSection(
    shifts: List<ShiftEntity>,
    onSave: (ShiftEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<ShiftEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("শিফট তালিকা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Button(
                onClick = { editing = ShiftEntity(shift = "", startTime = "09:30", endTime = "21:30") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("যোগ")
            }
        }
        shifts.forEach { sh ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sh.shift, fontWeight = FontWeight.Bold)
                        Text("${AttendanceCalculator.formatTime12h(sh.startTime)} — ${AttendanceCalculator.formatTime12h(sh.endTime)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    IconButton(onClick = { editing = sh }) { Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = EmeraldPrimary) }
                    IconButton(onClick = { onDelete(sh.docId) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed) }
                }
            }
        }
    }

    val ed = editing
    if (ed != null) {
        var name by remember(ed) { mutableStateOf(ed.shift) }
        var st by remember(ed) { mutableStateOf(ed.startTime) }
        var en by remember(ed) { mutableStateOf(ed.endTime) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("শিফট", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("শিফটের নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    TimePickerField("শুরু", st, { st = it }, Modifier.fillMaxWidth(), allowClear = false)
                    TimePickerField("শেষ", en, { en = it }, Modifier.fillMaxWidth(), allowClear = false)
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    onSave(ed.copy(shift = name.trim(), startTime = st, endTime = en))
                    editing = null
                }) { Text("সংরক্ষণ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("বাতিল") } }
        )
    }
}

@Composable
fun AdminBranchesSetupSection(
    branches: List<BranchEntity>,
    onSave: (BranchEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<BranchEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("শাখা তালিকা (${AttendanceCalculator.toBanglaDigits(branches.size)})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Button(
                onClick = { editing = BranchEntity(branch = "", lat = "22.35", lng = "91.83", radius = "150") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("যোগ")
            }
        }
        branches.forEach { b ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(b.branch, fontWeight = FontWeight.Bold)
                        Text("GPS: ${b.lat}, ${b.lng} · ব্যাসার্ধ: ${AttendanceCalculator.toBanglaDigits(b.radius)} মি", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    IconButton(onClick = { editing = b }) { Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = EmeraldPrimary) }
                    IconButton(onClick = { onDelete(b.docId) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = PunchRed) }
                }
            }
        }
    }

    val ed = editing
    if (ed != null) {
        var name by remember(ed) { mutableStateOf(ed.branch) }
        var lat by remember(ed) { mutableStateOf(ed.lat) }
        var lng by remember(ed) { mutableStateOf(ed.lng) }
        var radius by remember(ed) { mutableStateOf(ed.radius) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("শাখা", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("শাখার নাম") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text("Longitude") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = radius, onValueChange = { radius = it }, label = { Text("জিওফেন্স ব্যাসার্ধ (মিটার)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    onSave(ed.copy(branch = name.trim(), lat = lat.trim(), lng = lng.trim(), radius = radius.trim().ifBlank { "150" }))
                    editing = null
                }) { Text("সংরক্ষণ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("বাতিল") } }
        )
    }
}

@Composable
fun AdminSettingsSetupSection(
    settingsList: List<SettingEntity>,
    settingsMap: Map<String, String>,
    workspace: OrgWorkspace?,
    effectiveOrgId: String,
    signedInEmail: String,
    signedInUid: String,
    importingData: Boolean,
    onSaveSettings: (List<SettingEntity>) -> Unit,
    onAddMemberUid: (String) -> Unit,
    onSwitchWorkspace: (String) -> Unit,
    onImportNeonJson: (String) -> Unit,
    onSignOutGoogle: () -> Unit
) {
    var orgName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var grace by remember { mutableStateOf("") }
    var weeklyOff by remember { mutableStateOf("") }
    var maxAccuracy by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("") }
    var faceRequired by remember { mutableStateOf(false) }
    var faceThreshold by remember { mutableStateOf("") }
    var adminPass by remember { mutableStateOf("") }

    LaunchedEffect(settingsMap) {
        orgName = settingsMap["OrgName"].orEmpty()
        address = settingsMap["Address"].orEmpty()
        grace = settingsMap["GraceMinutes"] ?: "15"
        weeklyOff = settingsMap["WeeklyOff"] ?: "Fri"
        maxAccuracy = settingsMap["MaxAccuracy"] ?: "100"
        timezone = settingsMap["Timezone"] ?: "Asia/Dhaka"
        faceRequired = settingsMap["FaceRequired"] == "1"
        faceThreshold = settingsMap["FaceThreshold"] ?: "0.55"
        adminPass = settingsMap["AdminPass"] ?: "admin123"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("সিস্টেম সেটিংস", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                OutlinedTextField(value = orgName, onValueChange = { orgName = it }, label = { Text("প্রতিষ্ঠানের নাম") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("ঠিকানা") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = grace, onValueChange = { grace = it }, label = { Text("গ্রেস (মিনিট)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = weeklyOff, onValueChange = { weeklyOff = it }, label = { Text("সাপ্তাহিক ছুটি") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = maxAccuracy, onValueChange = { maxAccuracy = it }, label = { Text("সর্বোচ্চ GPS ত্রুটি (মি)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = timezone, onValueChange = { timezone = it }, label = { Text("সময় অঞ্চল") }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("পাঞ্চে মুখ যাচাই বাধ্যতামূলক (FaceRequired)")
                    Switch(checked = faceRequired, onCheckedChange = { faceRequired = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = faceThreshold, onValueChange = { faceThreshold = it }, label = { Text("Face Threshold") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = adminPass, onValueChange = { adminPass = it }, label = { Text("অ্যাডমিন পাসওয়ার্ড") }, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        val existingByKey = settingsList.associateBy { it.key }
                        fun make(k: String, v: String) = (existingByKey[k] ?: SettingEntity(key = k, value = v)).copy(value = v)
                        onSaveSettings(
                            listOf(
                                make("OrgName", orgName.trim()),
                                make("Address", address.trim()),
                                make("GraceMinutes", grace.trim().ifBlank { "15" }),
                                make("WeeklyOff", weeklyOff.trim().ifBlank { "Fri" }),
                                make("MaxAccuracy", maxAccuracy.trim().ifBlank { "100" }),
                                make("Timezone", timezone.trim().ifBlank { "Asia/Dhaka" }),
                                make("FaceRequired", if (faceRequired) "1" else "0"),
                                make("FaceThreshold", faceThreshold.trim().ifBlank { "0.55" }),
                                make("AdminPass", adminPass.trim().ifBlank { "admin123" })
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }

        AdminCloudSyncAndImportCards(
            workspace = workspace,
            effectiveOrgId = effectiveOrgId,
            signedInEmail = signedInEmail,
            signedInUid = signedInUid,
            importingData = importingData,
            onAddMemberUid = onAddMemberUid,
            onSwitchWorkspace = onSwitchWorkspace,
            onImportNeonJson = onImportNeonJson,
            onSignOutGoogle = onSignOutGoogle
        )
    }
}
