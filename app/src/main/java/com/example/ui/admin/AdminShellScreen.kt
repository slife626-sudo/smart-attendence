package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AttendanceCalculator
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PunchRed
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShellScreen(
    viewModel: AdminViewModel,
    onNavigateBackToStaff: () -> Unit
) {
    val ctrl by viewModel.controls.collectAsStateWithLifecycle()
    val snap by viewModel.snapshot.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ctrl.toastMessage) {
        val msg = ctrl.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    if (!ctrl.loggedIn) {
        BackHandler(onBack = onNavigateBackToStaff)
        AdminLoginScreen(
            orgName = snap.settings["OrgName"].orEmpty().ifBlank { "ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী" },
            error = ctrl.loginError,
            onLogin = viewModel::login,
            onBack = onNavigateBackToStaff
        )
        return
    }

    // Back from secondary admin tabs returns to Dashboard; Back on Dashboard exits to Staff screen
    BackHandler {
        if (ctrl.section != AdminSection.DASH) {
            viewModel.selectSection(AdminSection.DASH)
        } else {
            onNavigateBackToStaff()
        }
    }

    val pendingCount = remember(snap.leaves) {
        snap.leaves.count { it.status.equals("Pending", true) }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        val drawerContent: @Composable () -> Unit = {
            AdminDrawerMenu(
                currentSection = ctrl.section,
                pendingLeavesCount = pendingCount,
                onSelectSection = { sec ->
                    viewModel.selectSection(sec)
                    if (!isWide) scope.launch { drawerState.close() }
                },
                onBackToStaff = onNavigateBackToStaff,
                onLogout = viewModel::logout
            )
        }

        val mainScaffold: @Composable () -> Unit = {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = ctrl.section.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (!isWide) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("admin_menu_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "মেনু খুলুন")
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onNavigateBackToStaff,
                                modifier = Modifier.testTag("admin_to_staff_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "স্টাফ পাঞ্চ স্ক্রিন")
                            }
                            IconButton(
                                onClick = viewModel::logout,
                                modifier = Modifier.testTag("admin_logout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "লগআউট", tint = PunchRed)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    // Global Month + Branch Filter Strip
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.shiftMonth(-1) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "আগের মাস")
                                }
                                Text(
                                    text = AttendanceCalculator.monthLabelBn(ctrl.selectedMonth),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                IconButton(onClick = { viewModel.shiftMonth(1) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "পরের মাস")
                                }
                            }

                            var brExpanded by remember { mutableStateOf(false) }
                            Box {
                                FilterChip(
                                    selected = ctrl.selectedBranch.isNotBlank(),
                                    onClick = { brExpanded = true },
                                    label = {
                                        Text(
                                            text = ctrl.selectedBranch.ifBlank { "সব শাখা" },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                DropdownMenu(expanded = brExpanded, onDismissRequest = { brExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("সব শাখা") },
                                        onClick = { viewModel.selectBranch(""); brExpanded = false }
                                    )
                                    snap.branches.forEach { b ->
                                        DropdownMenuItem(
                                            text = { Text(b.branch) },
                                            onClick = { viewModel.selectBranch(b.branch); brExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Section Body
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        when (ctrl.section) {
                            AdminSection.DASH -> {
                                val dash = remember(snap, ctrl.selectedMonth, ctrl.selectedBranch, ctrl.selectedDate) {
                                    viewModel.computeDashboard(snap, ctrl)
                                }
                                AdminDashboardSection(
                                    report = dash,
                                    selectedMonth = ctrl.selectedMonth,
                                    selectedBranch = ctrl.selectedBranch,
                                    onOpenToday = { viewModel.selectSection(AdminSection.TODAY) },
                                    onOpenRep = { code ->
                                        viewModel.selectStaffCode(code)
                                        viewModel.selectSection(AdminSection.REP)
                                    }
                                )
                            }
                            AdminSection.TODAY -> {
                                val dash = remember(snap, ctrl.selectedMonth, ctrl.selectedBranch, ctrl.selectedDate) {
                                    viewModel.computeDashboard(snap, ctrl)
                                }
                                AdminTodaySection(
                                    report = dash,
                                    selectedDate = ctrl.selectedDate,
                                    onDateChange = viewModel::setSelectedDate,
                                    onPrevDay = { viewModel.shiftSelectedDate(-1) },
                                    onNextDay = { viewModel.shiftSelectedDate(1) },
                                    onTodayClick = viewModel::setTodayDate,
                                    onOpenStaffReport = { code ->
                                        viewModel.selectStaffCode(code)
                                        viewModel.selectSection(AdminSection.REP)
                                    }
                                )
                            }
                            AdminSection.REP -> {
                                val rep = remember(snap, ctrl.selectedStaffCode, ctrl.selectedMonth) {
                                    viewModel.computeStaffMonthlyReport(snap, ctrl)
                                }
                                AdminStaffReportSection(
                                    report = rep,
                                    staffList = snap.staff,
                                    selectedStaffCode = ctrl.selectedStaffCode,
                                    selectedMonth = ctrl.selectedMonth,
                                    onSelectStaff = viewModel::selectStaffCode
                                )
                            }
                            AdminSection.SUM -> {
                                val entries = remember(snap, ctrl.selectedMonth, ctrl.selectedBranch) {
                                    viewModel.computeMonthlySummary(snap, ctrl)
                                }
                                AdminMonthlySummarySection(
                                    entries = entries,
                                    selectedMonth = ctrl.selectedMonth,
                                    selectedBranch = ctrl.selectedBranch,
                                    onSelectStaffReport = { code ->
                                        viewModel.selectStaffCode(code)
                                        viewModel.selectSection(AdminSection.REP)
                                    }
                                )
                            }
                            AdminSection.BRK -> {
                                val brkRows = remember(snap, ctrl.breakFrom, ctrl.breakTo, ctrl.selectedBranch) {
                                    viewModel.computeBreakReport(snap, ctrl)
                                }
                                AdminBreakReportSection(
                                    rows = brkRows,
                                    from = ctrl.breakFrom,
                                    to = ctrl.breakTo,
                                    onRangeChange = viewModel::setBreakRange
                                )
                            }
                            AdminSection.YRS -> {
                                val yrRows = remember(snap, ctrl.selectedMonth, ctrl.selectedBranch) {
                                    viewModel.computeYearlySummary(snap, ctrl)
                                }
                                AdminYearlySummarySection(rows = yrRows, selectedMonth = ctrl.selectedMonth)
                            }
                            AdminSection.MAN -> {
                                AdminManualCorrectionSection(
                                    staffList = snap.staff,
                                    attendanceList = snap.attendance,
                                    breaksList = snap.breaks,
                                    todayReal = snap.todayReal,
                                    timezone = snap.settings["Timezone"] ?: "Asia/Dhaka",
                                    selectedStaffCode = ctrl.selectedStaffCode,
                                    selectedDate = ctrl.selectedDate,
                                    onSelectStaff = viewModel::selectStaffCode,
                                    onSelectDate = viewModel::setSelectedDate,
                                    onSavePunch = viewModel::saveManualPunch,
                                    onDeletePunch = viewModel::deleteManualPunch,
                                    onSaveBreak = viewModel::saveBreak,
                                    onDeleteBreak = viewModel::deleteBreak
                                )
                            }
                            AdminSection.FACE -> {
                                AdminFaceEnrollmentSection(
                                    staffList = snap.staff,
                                    onSaveFace = viewModel::saveFace,
                                    onDeleteFace = viewModel::deleteFace
                                )
                            }
                            AdminSection.STAFF -> {
                                AdminStaffSetupSection(
                                    staffList = snap.staff,
                                    branches = snap.branches,
                                    shifts = snap.shifts,
                                    onSaveStaff = viewModel::saveStaff,
                                    onDeleteStaff = viewModel::deleteStaff
                                )
                            }
                            AdminSection.TRF -> {
                                AdminTransferSection(
                                    staffList = snap.staff,
                                    branches = snap.branches,
                                    transfers = snap.transfers,
                                    todayReal = snap.todayReal,
                                    onAddTransfer = viewModel::addTransfer,
                                    onDeleteTransfer = viewModel::deleteTransfer
                                )
                            }
                            AdminSection.LAPP -> {
                                AdminLeaveAppsSection(
                                    staffList = snap.staff,
                                    leaveTypes = snap.leaveTypes,
                                    leaves = snap.leaves,
                                    holidays = snap.holidays,
                                    transfers = snap.transfers,
                                    settings = snap.settings,
                                    todayReal = snap.todayReal,
                                    onAddLeave = viewModel::addAdminLeave,
                                    onDecideLeave = viewModel::decideLeave,
                                    onDeleteLeave = viewModel::deleteLeave
                                )
                            }
                            AdminSection.LBAL -> {
                                AdminLeaveBalanceSection(
                                    staffList = snap.staff,
                                    leaveTypes = snap.leaveTypes,
                                    leaves = snap.leaves,
                                    holidays = snap.holidays,
                                    transfers = snap.transfers,
                                    settings = snap.settings,
                                    selectedMonth = ctrl.selectedMonth,
                                    selectedBranch = ctrl.selectedBranch
                                )
                            }
                            AdminSection.LEAVE_TYPES -> {
                                AdminLeaveTypesSetupSection(
                                    leaveTypes = snap.leaveTypes,
                                    onSave = viewModel::saveLeaveType,
                                    onDelete = viewModel::deleteLeaveType
                                )
                            }
                            AdminSection.HOLIDAYS -> {
                                AdminHolidaysSetupSection(
                                    holidays = snap.holidays,
                                    branches = snap.branches,
                                    todayReal = snap.todayReal,
                                    onSave = viewModel::saveHoliday,
                                    onDelete = viewModel::deleteHoliday
                                )
                            }
                            AdminSection.SHIFTS -> {
                                AdminShiftsSetupSection(
                                    shifts = snap.shifts,
                                    onSave = viewModel::saveShift,
                                    onDelete = viewModel::deleteShift
                                )
                            }
                            AdminSection.BRANCHES -> {
                                AdminBranchesSetupSection(
                                    branches = snap.branches,
                                    onSave = viewModel::saveBranch,
                                    onDelete = viewModel::deleteBranch
                                )
                            }
                            AdminSection.SETTINGS -> {
                                AdminSettingsSetupSection(
                                    settingsList = snap.settingsList,
                                    settingsMap = snap.settings,
                                    onSaveSettings = viewModel::saveSettingsBatch
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isWide) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(260.dp)) {
                        drawerContent()
                    }
                },
                content = mainScaffold
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                        drawerContent()
                    }
                },
                content = mainScaffold
            )
        }
    }
}

@Composable
private fun AdminDrawerMenu(
    currentSection: AdminSection,
    pendingLeavesCount: Int,
    onSelectSection: (AdminSection) -> Unit,
    onBackToStaff: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "অ্যাডমিন প্যানেল",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = EmeraldPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))

        val items: List<Triple<AdminSection, ImageVector, Int>> = listOf(
            Triple(AdminSection.DASH, Icons.Default.Dashboard, 0),
            Triple(AdminSection.TODAY, Icons.Default.Today, 0),
            Triple(AdminSection.REP, Icons.Default.Badge, 0),
            Triple(AdminSection.SUM, Icons.Default.CalendarMonth, 0),
            Triple(AdminSection.BRK, Icons.Default.Coffee, 0),
            Triple(AdminSection.YRS, Icons.Default.DateRange, 0),
            Triple(AdminSection.MAN, Icons.Default.EditCalendar, 0),
            Triple(AdminSection.FACE, Icons.Default.Face, 0),
            Triple(AdminSection.STAFF, Icons.Default.Groups, 0),
            Triple(AdminSection.TRF, Icons.Default.SwapHoriz, 0),
            Triple(AdminSection.LAPP, Icons.Default.BeachAccess, pendingLeavesCount),
            Triple(AdminSection.LBAL, Icons.Default.AccountBalanceWallet, 0),
            Triple(AdminSection.LEAVE_TYPES, Icons.Default.Category, 0),
            Triple(AdminSection.HOLIDAYS, Icons.Default.EventAvailable, 0),
            Triple(AdminSection.SHIFTS, Icons.Default.Schedule, 0),
            Triple(AdminSection.BRANCHES, Icons.Default.Store, 0),
            Triple(AdminSection.SETTINGS, Icons.Default.Settings, 0)
        )

        items.forEach { (sec, icon, badge) ->
            NavigationDrawerItem(
                label = { Text(sec.title) },
                selected = currentSection == sec,
                onClick = { onSelectSection(sec) },
                icon = { Icon(icon, contentDescription = null) },
                badge = if (badge > 0) {
                    {
                        Badge(containerColor = AmberAccent, contentColor = Color.White) {
                            Text(AttendanceCalculator.toBanglaDigits(badge))
                        }
                    }
                } else null
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            label = { Text("স্টাফ পাঞ্চ স্ক্রিন") },
            selected = false,
            onClick = onBackToStaff,
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("লগআউট", color = PunchRed) },
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PunchRed) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminLoginScreen(
    orgName: String,
    error: String,
    onLogin: (String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("অ্যাডমিন লগইন", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = orgName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("অ্যাডমিন পাসওয়ার্ড") },
                        placeholder = { Text("ডিফল্ট: admin123") },
                        singleLine = true,
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(
                                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (visible) "লুকান" else "দেখান"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                    )
                    if (error.isNotBlank()) {
                        Text(error, color = PunchRed, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onLogin(password) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("প্রবেশ করুন", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "ডিফল্ট পাসওয়ার্ড: admin123 (সেটিংস থেকে পরিবর্তনযোগ্য)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
