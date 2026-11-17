package com.example.ui.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.StaffLeavePortalData
import com.example.domain.AttendanceCalculator
import com.example.ui.components.DatePickerField
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PunchGreen
import com.example.ui.theme.PunchRed
import com.example.ui.theme.TealInfo
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffLeaveDialog(
    staffName: String,
    loading: Boolean,
    portalData: StaffLeavePortalData?,
    statusMessage: String?,
    statusOk: Boolean,
    onApply: (type: String, from: String, to: String, reason: String) -> Unit,
    onCancelLeave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(portalData) {
        if (portalData != null && portalData.ok) {
            if (fromDate.isBlank()) fromDate = portalData.today
            if (toDate.isBlank()) toDate = portalData.today
            val availTypes = portalData.types.filter { it.avail }
            if (selectedType.isBlank() || portalData.types.none { it.name == selectedType && it.avail }) {
                selectedType = availTypes.firstOrNull()?.name.orEmpty()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BeachAccess,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ছুটির আবেদন ও হিসাব",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        if (staffName.isNotBlank()) {
                            Text(
                                text = staffName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_leave_portal_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }
                HorizontalDivider()

                if (loading && portalData == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                } else if (portalData != null && !portalData.ok) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = portalData.message.ifBlank { "তথ্য লোড করা যায়নি" },
                            color = PunchRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onDismiss) { Text("বন্ধ করুন") }
                    }
                } else if (portalData != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Yearly Balance Section
                        Text(
                            text = "বার্ষিক ছুটির হিসাব (${AttendanceCalculator.toBanglaDigits(portalData.year)})",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        portalData.balances.forEach { b ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = b.type,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val sub = when {
                                            b.quota == 0 && b.from.isNotEmpty() ->
                                                "শিক্ষানবিস কাল · কোটা শুরু ${b.from.take(7)}"
                                            b.quota == 0 -> "চুক্তি অনুযায়ী কোটা নেই"
                                            b.paid && b.quota != null ->
                                                "ব্যবহৃত ${AttendanceCalculator.toBanglaDigits(b.used)} / ${AttendanceCalculator.toBanglaDigits(b.quota)} দিন" +
                                                    if (b.from.isNotEmpty()) " (${b.from.take(7)} থেকে)" else ""
                                            b.paid -> "ব্যবহৃত ${AttendanceCalculator.toBanglaDigits(b.used)} দিন"
                                            else -> "বিনা বেতনে ব্যবহৃত ${AttendanceCalculator.toBanglaDigits(b.used)} দিন"
                                        }
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                        if (b.unpaid > 0 && b.paid) {
                                            Text(
                                                text = "বিনা বেতনে গণ্য: ${AttendanceCalculator.toBanglaDigits(b.unpaid)} দিন",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PunchRed
                                            )
                                        }
                                    }
                                    val rightVal = when {
                                        b.left != null -> "বাকি ${AttendanceCalculator.toBanglaDigits(b.left)} দিন"
                                        !b.paid -> "বিনা বেতন"
                                        else -> "সীমাহীন"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if ((b.left ?: 1) > 0 && b.paid) PunchGreen.copy(alpha = 0.12f) else AmberAccent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = rightVal,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if ((b.left ?: 1) > 0 && b.paid) PunchGreen else AmberAccent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Leave Application Form
                        Text(
                            text = "নতুন ছুটির আবেদন",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedType.ifBlank { "ছুটির ধরন নেই" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("ছুটির ধরন") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("leave_type_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                portalData.types.forEach { t ->
                                    val suffix = when (t.why) {
                                        "probation" -> " (শিক্ষানবিস কালে প্রযোজ্য নয়)"
                                        "none" -> " (কোটা নেই)"
                                        "exhausted" -> " (কোটা শেষ)"
                                        else -> ""
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = t.name + suffix,
                                                color = if (t.avail) MaterialTheme.colorScheme.onSurface else TextMuted
                                            )
                                        },
                                        enabled = t.avail,
                                        onClick = {
                                            selectedType = t.name
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DatePickerField(
                                label = "থেকে",
                                value = fromDate,
                                onValueSelected = {
                                    fromDate = it
                                    if (toDate.isBlank() || toDate < it) toDate = it
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("leave_from_date")
                            )
                            DatePickerField(
                                label = "পর্যন্ত",
                                value = toDate,
                                onValueSelected = { toDate = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("leave_to_date")
                            )
                        }

                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it.take(300) },
                            label = { Text("কারণ / বিবরণ (ঐচ্ছিক)") },
                            singleLine = false,
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leave_reason_input")
                        )

                        if (!statusMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (statusOk) PunchGreen.copy(alpha = 0.12f) else PunchRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = statusMessage,
                                    color = if (statusOk) PunchGreen else PunchRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { onApply(selectedType, fromDate, toDate, reason) },
                            enabled = !loading && selectedType.isNotBlank() && fromDate.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_leave_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (loading) "জমা হচ্ছে..." else "আবেদন জমা দিন", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider()

                        // My Applications
                        Text(
                            text = "আমার আবেদনসমূহ",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        if (portalData.applications.isEmpty()) {
                            Text(
                                text = "কোনো ছুটির আবেদন নেই",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        } else {
                            portalData.applications.forEach { app ->
                                val st = app.status.trim()
                                val stColor = when {
                                    st.equals("Approved", true) || st.isEmpty() -> PunchGreen
                                    st.equals("Rejected", true) -> PunchRed
                                    else -> AmberAccent
                                }
                                val stLabel = when {
                                    st.equals("Approved", true) || st.isEmpty() -> "অনুমোদিত"
                                    st.equals("Rejected", true) -> "প্রত্যাখ্যাত"
                                    else -> "অপেক্ষমাণ"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${AttendanceCalculator.formatDateBn(app.dateFrom)} — ${AttendanceCalculator.formatDateBn(app.dateTo)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = app.typ + if (app.reason.isNotBlank()) " · ${app.reason}" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                            if (app.note.isNotBlank()) {
                                                Text(
                                                    text = "নোট: ${app.note}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TealInfo
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = stColor.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = stLabel,
                                                color = stColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (st.equals("Pending", true)) {
                                            IconButton(
                                                onClick = { onCancelLeave(app.id) },
                                                modifier = Modifier.testTag("cancel_leave_${app.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "আবেদন বাতিল",
                                                    tint = PunchRed
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
        }
    }
}
