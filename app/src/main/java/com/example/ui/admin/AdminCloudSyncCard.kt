package com.example.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.OrgWorkspace
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintSurface
import com.example.ui.theme.PunchRed
import com.example.ui.theme.TextMuted

private const val NEON_EXPORT_SQL = """SELECT json_build_object(
  'settings', COALESCE((SELECT json_agg(t) FROM settings t), '[]'::json),
  'branches', COALESCE((SELECT json_agg(t) FROM branches t), '[]'::json),
  'shifts', COALESCE((SELECT json_agg(t) FROM shifts t), '[]'::json),
  'staff', COALESCE((SELECT json_agg(t) FROM staff t), '[]'::json),
  'holidays', COALESCE((SELECT json_agg(t) FROM holidays t), '[]'::json),
  'leave_types', COALESCE((SELECT json_agg(t) FROM leave_types t), '[]'::json),
  'leaves', COALESCE((SELECT json_agg(t) FROM leaves t), '[]'::json),
  'transfers', COALESCE((SELECT json_agg(t) FROM transfers t), '[]'::json),
  'attendance', COALESCE((SELECT json_agg(t) FROM attendance t), '[]'::json),
  'breaks', COALESCE((SELECT json_agg(t) FROM breaks t), '[]'::json)
) AS neon_export;"""

@Composable
fun AdminCloudSyncAndImportCards(
    workspace: OrgWorkspace?,
    effectiveOrgId: String,
    signedInEmail: String,
    signedInUid: String,
    importingData: Boolean,
    onAddMemberUid: (String) -> Unit,
    onSwitchWorkspace: (String) -> Unit,
    onImportNeonJson: (String) -> Unit,
    onSignOutGoogle: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val dbId = remember(context) { context.getString(R.string.firestore_database_id) }

    var memberUidInput by remember { mutableStateOf("") }
    var workspaceInput by remember(effectiveOrgId) { mutableStateOf(effectiveOrgId) }
    var jsonInput by remember { mutableStateOf("") }
    var showSqlQuery by remember { mutableStateOf(false) }
    var feedbackNote by remember { mutableStateOf("") }

    val jsonPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                if (text.isNotBlank()) {
                    jsonInput = text
                    feedbackNote = "JSON ফাইল লোড হয়েছে (${text.length} অক্ষর) — এখন ইমপোর্ট বাটনে চাপুন"
                } else {
                    feedbackNote = "ফাইলটি খালি"
                }
            } catch (e: Exception) {
                feedbackNote = "ফাইল পড়তে সমস্যা: ${e.localizedMessage ?: ""}"
            }
        }
    }

    // Card 1: Cloud Sync & Multi-Device Access
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = "ক্লাউড সিঙ্ক ও মাল্টি-ডিভাইস অ্যাক্সেস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MintSurface,
                border = BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Google অ্যাকাউন্ট: ${signedInEmail.ifBlank { "সংযুক্ত" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "আপনার UID: $signedInUid",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(signedInUid))
                                feedbackNote = "User UID কপি করা হয়েছে"
                            },
                            modifier = Modifier.size(48.dp).testTag("copy_my_uid_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "UID কপি করুন", tint = EmeraldPrimary)
                        }
                    }
                    Text(
                        text = "সক্রিয় Workspace ID: $effectiveOrgId · অনুমোদিত সদস্য: ${workspace?.memberUids?.size ?: 1} জন",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary
                    )
                }
            }

            OutlinedTextField(
                value = workspaceInput,
                onValueChange = { workspaceInput = it },
                label = { Text("Workspace ID পরিবর্তন করুন (যেমন: wahidiya_main)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("workspace_id_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSwitchWorkspace(workspaceInput) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("switch_workspace_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ওয়ার্কস্পেস চালু")
                }
                OutlinedButton(
                    onClick = {
                        workspaceInput = ""
                        onSwitchWorkspace("")
                    },
                    modifier = Modifier.height(48.dp).testTag("reset_workspace_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ডিফল্ট")
                }
            }

            HorizontalDivider()

            Text(
                text = "অন্য মোবাইল ডিভাইসকে এই প্রতিষ্ঠানের ডেটা দেখার অনুমতি দিতে ওই ফোনের Google UID এখানে যোগ করুন:",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            OutlinedTextField(
                value = memberUidInput,
                onValueChange = { memberUidInput = it },
                label = { Text("অন্য ফোনের Firebase User UID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("member_uid_input")
            )
            Button(
                onClick = {
                    if (memberUidInput.isNotBlank()) {
                        onAddMemberUid(memberUidInput.trim())
                        memberUidInput = ""
                    }
                },
                enabled = memberUidInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_member_uid_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecondary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("টিম সদস্য UID যোগ ও শেয়ার করুন", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSignOutGoogle,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_google_signout_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PunchRed),
                border = BorderStroke(1.dp, PunchRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google অ্যাকাউন্ট সাইন-আউট", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Card 2: Neon PostgreSQL JSON Importer
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = "Neon PostgreSQL ডেটা ইমপোর্ট (.json)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
            Text(
                text = "আপনার আগের Neon ডাটাবেজের সব কর্মী, হাজিরা, ব্রেক, ছুটি, শাখা বদলি ও সেটিংস সরাসরি Firestore ক্লাউডে আনতে নিচের SQL কুয়েরি চালিয়ে প্রাপ্ত JSON ফাইল বা টেক্সট ইমপোর্ট করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { showSqlQuery = !showSqlQuery },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(if (showSqlQuery) "SQL লুকান" else "Neon এক্সপোর্ট SQL দেখুন")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(NEON_EXPORT_SQL))
                        feedbackNote = "Neon SQL কুয়েরি কপি করা হয়েছে"
                    },
                    modifier = Modifier.height(48.dp).testTag("copy_neon_sql_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SQL কপি")
                }
            }

            AnimatedVisibility(visible = showSqlQuery) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = NEON_EXPORT_SQL,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = { jsonPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                enabled = !importingData,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_neon_json_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("JSON ফাইল বাছুন (.json)", fontWeight = FontWeight.SemiBold)
            }

            OutlinedTextField(
                value = jsonInput,
                onValueChange = { jsonInput = it },
                label = { Text("অথবা Neon এক্সপোর্ট JSON এখানে পেস্ট করুন") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth().testTag("neon_json_input")
            )

            if (feedbackNote.isNotBlank()) {
                Text(
                    text = feedbackNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { onImportNeonJson(jsonInput) },
                enabled = !importingData && jsonInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_neon_json_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
            ) {
                if (importingData) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ইমপোর্ট হচ্ছে...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firestore-এ ইমপোর্ট করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Card 3: Local PC / Hardware Attendance Device Bridge Info
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Devices, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = "লোকাল পিসি / হাজিরা ডিভাইস সিঙ্ক",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
            Text(
                text = "লোকাল কম্পিউটার সার্ভার বা ফিঙ্গারপ্রিন্ট/হাজিরা ডিভাইস থেকে Firebase Admin SDK দিয়ে সরাসরি এই একই ক্লাউড ডাটাবেজে পাঞ্চ পাঠানো যাবে এবং সাথে সাথেই সব ফোনে লাইভ আপডেট দেখাবে।",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Firestore Database ID:\n$dbId",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Attendance Path:\norgs/$effectiveOrgId/attendance/att_<YYYY-MM-DD>_<code>",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString("databaseId=$dbId\nworkspace=$effectiveOrgId"))
                    feedbackNote = "ব্রিজ কনফিগারেশন কপি করা হয়েছে"
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("copy_bridge_info_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Database ID ও Workspace কপি করুন")
            }
        }
    }
}
