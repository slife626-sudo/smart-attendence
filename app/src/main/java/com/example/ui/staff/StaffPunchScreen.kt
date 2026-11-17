package com.example.ui.staff

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AttendanceCalculator
import com.example.ui.components.OrgBannerHeader
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintSurface
import com.example.ui.theme.PunchGreen
import com.example.ui.theme.PunchRed
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPunchScreen(
    viewModel: StaffViewModel,
    signedInEmail: String,
    onGoogleSignOut: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var staffDropdownExpanded by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }
    var pendingPunchType by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        val pType = pendingPunchType
        pendingPunchType = null
        if (granted && pType != null) {
            viewModel.startPunchFlow(pType, context)
        } else {
            viewModel.setStatus("লোকেশন অনুমতি ছাড়া হাজিরা দেওয়া যাবে না", false)
        }
    }

    fun triggerPunch(type: String) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            viewModel.startPunchFlow(type, context)
        } else {
            pendingPunchType = type
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val activeStaff = remember(staffList) {
        staffList.filter { AttendanceCalculator.isActive(it) }
    }
    val selectedStaffLabel = remember(state.selectedCode, activeStaff) {
        val found = activeStaff.find { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(state.selectedCode) }
        if (found != null) "${found.code} — ${found.name}" else state.selectedCode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "হাজিরা",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    TextButton(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.testTag("open_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "অ্যাডমিন প্যানেল",
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "অ্যাডমিন",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.testTag("staff_google_signout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Google অ্যাকাউন্ট সাইন-আউট",
                            tint = TextMuted
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OrgBannerHeader(
                    orgName = state.orgName,
                    address = "",
                    subtitle = "ডিজিটাল উপস্থিতি ও সময় ব্যবস্থাপনা"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Bangla Clock
                        Text(
                            text = state.clockText.ifBlank { "--:--:--" },
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("live_clock_text")
                        )

                        // Staff Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = staffDropdownExpanded,
                            onExpandedChange = { staffDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedStaffLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("কর্মী নির্বাচন করুন") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("staff_selector_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = staffDropdownExpanded,
                                onDismissRequest = { staffDropdownExpanded = false }
                            ) {
                                activeStaff.forEach { st ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${st.code} — ${st.name} (${st.branch})")
                                        },
                                        onClick = {
                                            staffDropdownExpanded = false
                                            viewModel.onCodeSelected(st.code)
                                        }
                                    )
                                }
                            }
                        }

                        // Staff Info Card (#who)
                        val info = state.staffInfo
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            if (!info.ok) {
                                Text(
                                    text = info.message.ifBlank { "তথ্য পাওয়া যায়নি" },
                                    color = PunchRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(14.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    InfoLine("নাম", "${info.name} (${info.designation})")
                                    InfoLine("শাখা", info.branch)
                                    InfoLine("শিফট", AttendanceCalculator.toBanglaDigits(info.shift))
                                    InfoLine("আজ IN", if (info.inTime.isNotBlank()) AttendanceCalculator.formatTime12h(info.inTime) else "-")
                                    InfoLine("আজ OUT", if (info.outTime.isNotBlank()) AttendanceCalculator.formatTime12h(info.outTime) else "-")

                                    val breakText = when {
                                        info.openBreakStart.isNotBlank() ->
                                            "চলমান (${AttendanceCalculator.formatTime12h(info.openBreakStart)} থেকে)"
                                        info.breakCount > 0 ->
                                            "${AttendanceCalculator.toBanglaDigits(info.breakCount)} বার · মোট ${AttendanceCalculator.toBanglaDigits(info.breakMin)} মিনিট"
                                        else -> "-"
                                    }
                                    InfoLine("আজ ব্রেক", breakText)

                                    if (info.faceEnrolled) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Face,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "মুখ নিবন্ধিত — পাঞ্চে মুখ যাচাই চালু আছে",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (info.faceRequired) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "⚠ মুখ নিবন্ধিত নয় — অ্যাডমিনের কাছে যান",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PunchRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // PIN Input
                        OutlinedTextField(
                            value = state.pin,
                            onValueChange = viewModel::onPinChanged,
                            label = { Text("গোপন পিন (PIN)") },
                            placeholder = { Text("৪ সংখ্যার PIN (ডিফল্ট: 1234)") },
                            singleLine = true,
                            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            trailingIcon = {
                                IconButton(onClick = { pinVisible = !pinVisible }) {
                                    Icon(
                                        imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (pinVisible) "পিন লুকান" else "পিন দেখুন"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_pin_input")
                        )

                        // IN / OUT Primary Punch Row
                        val canPunch = info.ok && !state.busy && !(info.faceRequired && !info.faceEnrolled)
                        val inEnabled = canPunch && info.inTime.isBlank()
                        val outEnabled = canPunch && info.inTime.isNotBlank() && info.openBreakStart.isBlank()
                        val bsEnabled = canPunch && info.inTime.isNotBlank() && info.outTime.isBlank() && info.openBreakStart.isBlank()
                        val beEnabled = canPunch && info.inTime.isNotBlank() && info.outTime.isBlank() && info.openBreakStart.isNotBlank()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { triggerPunch("IN") },
                                enabled = inEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .testTag("punch_in_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PunchGreen)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "IN",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Button(
                                onClick = { triggerPunch("OUT") },
                                enabled = outEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .testTag("punch_out_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PunchRed)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OUT",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Break Start / Break End Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { triggerPunch("BS") },
                                enabled = bsEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("break_start_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                            ) {
                                Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ব্রেক শুরু",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { triggerPunch("BE") },
                                enabled = beEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("break_end_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ব্রেক শেষ",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Status feedback banner
                        if (state.statusMessage.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (state.statusOk) PunchGreen.copy(alpha = 0.12f) else PunchRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = state.statusMessage,
                                    color = if (state.statusOk) PunchGreen else PunchRed,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .testTag("punch_status_message")
                                )
                            }
                        }

                        // Leave Portal Button
                        OutlinedButton(
                            onClick = { viewModel.openLeavePortal() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_leave_portal_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MintBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MintSurface,
                                contentColor = EmeraldPrimary
                            )
                        ) {
                            Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ছুটির আবেদন ও হিসাব",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Face verification modal
    val facePunchType = state.showFaceCameraForType
    if (facePunchType != null) {
        FaceCaptureDialog(
            title = "মুখ যাচাই করুন ($facePunchType)",
            targetSampleCount = 2,
            requireFrontalEyesOpen = true,
            onCaptured = { vectors ->
                viewModel.onFaceCapturedForPunch(facePunchType, context, vectors)
            },
            onDismiss = { viewModel.dismissFaceCamera() }
        )
    }

    // Staff Leave Portal Modal
    if (state.showLeavePortal) {
        StaffLeaveDialog(
            staffName = state.staffInfo.name,
            loading = state.leaveLoading,
            portalData = state.leavePortalData,
            statusMessage = state.leaveStatusMsg,
            statusOk = state.leaveStatusOk,
            onApply = viewModel::applyLeave,
            onCancelLeave = viewModel::cancelLeave,
            onDismiss = viewModel::closeLeavePortal
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Google অ্যাকাউন্ট সাইন-আউট", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (signedInEmail.isNotBlank()) {
                        "বর্তমান অ্যাকাউন্ট: $signedInEmail\n\nসাইন-আউট করে অন্য অ্যাকাউন্টে প্রবেশ করতে চান?"
                    } else {
                        "Google অ্যাকাউন্ট থেকে সাইন-আউট করতে চান?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onGoogleSignOut()
                    }
                ) {
                    Text("সাইন-আউট", color = PunchRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.width(78.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
