package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AttendanceEntity
import com.example.data.db.BranchEntity
import com.example.data.db.BreakEntity
import com.example.data.db.HolidayEntity
import com.example.data.db.LeaveEntity
import com.example.data.db.LeaveTypeEntity
import com.example.data.db.SettingEntity
import com.example.data.db.ShiftEntity
import com.example.data.db.StaffEntity
import com.example.data.db.TransferEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.AttendanceRepository
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceSummaryTotals
import com.example.domain.BreakReportStaffRow
import com.example.domain.DashboardReport
import com.example.domain.StaffMonthlyReport
import com.example.domain.StaffSummaryEntry
import com.example.domain.YearlyStaffRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class AdminSection(val title: String) {
    DASH("ড্যাশবোর্ড"),
    TODAY("আজকের হাজিরা"),
    REP("কর্মীর রিপোর্ট"),
    SUM("মাসিক সারসংক্ষেপ"),
    BRK("ব্রেক রিপোর্ট"),
    YRS("বাৎসরিক সারসংক্ষেপ"),
    MAN("ম্যানুয়াল / সংশোধন"),
    FACE("মুখ নিবন্ধন"),
    STAFF("কর্মী তালিকা"),
    TRF("শাখা বদলি"),
    LAPP("ছুটির আবেদন"),
    LBAL("ছুটির ব্যালেন্স"),
    LEAVE_TYPES("ছুটির ধরন"),
    HOLIDAYS("সরকারি ছুটি"),
    SHIFTS("শিফট"),
    BRANCHES("শাখা"),
    SETTINGS("সেটিংস")
}

data class AdminControlState(
    val loggedIn: Boolean = false,
    val loginError: String = "",
    val section: AdminSection = AdminSection.DASH,
    val selectedMonth: String = "2026-11",
    val selectedBranch: String = "",
    val selectedDate: String = "2026-11-17",
    val selectedStaffCode: String = "01",
    val breakFrom: String = "2026-11-01",
    val breakTo: String = "2026-11-17",
    val toastMessage: String? = null,
    val toastOk: Boolean = true
)

data class AdminDataSnapshot(
    val settings: Map<String, String> = emptyMap(),
    val settingsList: List<SettingEntity> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val shifts: List<ShiftEntity> = emptyList(),
    val staff: List<StaffEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList(),
    val leaveTypes: List<LeaveTypeEntity> = emptyList(),
    val leaves: List<LeaveEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val breaks: List<BreakEntity> = emptyList(),
    val todayReal: String = "2026-11-17",
    val initialized: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val repository: AttendanceRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _controls = MutableStateFlow(AdminControlState())
    val controls: StateFlow<AdminControlState> = _controls.asStateFlow()

    private val selectedYearFlow = MutableStateFlow(2026)

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val cfg = repository.getSettingsMap()
            val (today, _, _) = AttendanceCalculator.nowInTimezone(cfg["Timezone"] ?: "Asia/Dhaka")
            val ym = today.take(7)
            val yr = today.take(4).toIntOrNull() ?: 2026
            selectedYearFlow.value = yr
            _controls.update {
                it.copy(
                    selectedMonth = ym,
                    selectedDate = today,
                    breakFrom = "$ym-01",
                    breakTo = today
                )
            }
        }
        viewModelScope.launch {
            prefs.adminLoggedIn.collect { isLogged ->
                _controls.update { it.copy(loggedIn = isLogged) }
            }
        }
    }

    private val coreBundleA = combine(
        repository.observeSettings,
        repository.observeBranches,
        repository.observeShifts,
        repository.observeStaff,
        repository.observeHolidays
    ) { settings, branches, shifts, staff, holidays ->
        listOf(settings, branches, shifts, staff, holidays)
    }

    private val coreBundleB = combine(
        repository.observeLeaveTypes,
        repository.observeLeaves,
        repository.observeTransfers,
        selectedYearFlow.flatMapLatest { repository.observeAttendanceForYear(it) },
        selectedYearFlow.flatMapLatest { repository.observeBreaksForYear(it) }
    ) { leaveTypes, leaves, transfers, att, brks ->
        listOf(leaveTypes, leaves, transfers, att, brks)
    }

    @Suppress("UNCHECKED_CAST")
    val snapshot: StateFlow<AdminDataSnapshot> = combine(coreBundleA, coreBundleB) { a, b ->
        val settingsList = a[0] as List<SettingEntity>
        val settings = AttendanceCalculator.settingsMap(settingsList)
        val (todayReal, _, _) = AttendanceCalculator.nowInTimezone(settings["Timezone"] ?: "Asia/Dhaka")
        AdminDataSnapshot(
            settings = settings,
            settingsList = settingsList,
            branches = a[1] as List<BranchEntity>,
            shifts = a[2] as List<ShiftEntity>,
            staff = a[3] as List<StaffEntity>,
            holidays = a[4] as List<HolidayEntity>,
            leaveTypes = b[0] as List<LeaveTypeEntity>,
            leaves = b[1] as List<LeaveEntity>,
            transfers = b[2] as List<TransferEntity>,
            attendance = b[3] as List<AttendanceEntity>,
            breaks = b[4] as List<BreakEntity>,
            todayReal = todayReal,
            initialized = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDataSnapshot())

    fun login(password: String) {
        viewModelScope.launch {
            if (repository.verifyAdminPassword(password.trim())) {
                prefs.setAdminLoggedIn(true)
                _controls.update { it.copy(loggedIn = true, loginError = "") }
            } else {
                _controls.update { it.copy(loginError = "পাসওয়ার্ড সঠিক নয়") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.setAdminLoggedIn(false)
            _controls.update { it.copy(loggedIn = false, section = AdminSection.DASH) }
        }
    }

    fun selectSection(section: AdminSection) {
        _controls.update { it.copy(section = section) }
    }

    fun selectBranch(branch: String) {
        _controls.update { it.copy(selectedBranch = branch) }
    }

    fun shiftMonth(delta: Int) {
        val cur = _controls.value.selectedMonth
        val parts = cur.split("-")
        val ym = if (parts.size == 2) {
            YearMonth.of(parts[0].toIntOrNull() ?: 2026, parts[1].toIntOrNull() ?: 1)
        } else YearMonth.now()
        val next = ym.plusMonths(delta.toLong())
        val nextStr = "${next.year}-${next.monthValue.toString().padStart(2, '0')}"
        selectedYearFlow.value = next.year
        _controls.update {
            it.copy(
                selectedMonth = nextStr,
                breakFrom = "$nextStr-01",
                breakTo = "$nextStr-${next.lengthOfMonth().toString().padStart(2, '0')}"
            )
        }
    }

    fun setSelectedDate(date: String) {
        if (AttendanceCalculator.isValidDate(date)) {
            _controls.update { it.copy(selectedDate = date) }
        }
    }

    fun shiftSelectedDate(deltaDays: Int) {
        val d = AttendanceCalculator.parseDateOrNull(_controls.value.selectedDate) ?: LocalDate.now()
        _controls.update {
            it.copy(selectedDate = d.plusDays(deltaDays.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
    }

    fun setTodayDate() {
        val t = snapshot.value.todayReal
        _controls.update { it.copy(selectedDate = t) }
    }

    fun selectStaffCode(code: String) {
        _controls.update { it.copy(selectedStaffCode = code) }
    }

    fun setBreakRange(from: String, to: String) {
        _controls.update { it.copy(breakFrom = from, breakTo = to) }
    }

    fun clearToast() {
        _controls.update { it.copy(toastMessage = null) }
    }

    private fun showToast(msg: String, ok: Boolean = true) {
        _controls.update { it.copy(toastMessage = msg, toastOk = ok) }
    }

    // Computed Reports
    fun computeDashboard(snap: AdminDataSnapshot, ctrl: AdminControlState): DashboardReport {
        val (y, m) = parseYm(ctrl.selectedMonth)
        val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(snap.leaveTypes)
        val approved = snap.leaves.filter { AttendanceCalculator.isApproved(it) }
        val shiftsMap = snap.shifts.associateBy { it.shift }
        return AttendanceCalculator.buildDashboard(
            year = y,
            month = m,
            selectedDate = ctrl.selectedDate,
            todayReal = snap.todayReal,
            branchFilter = ctrl.selectedBranch,
            staffList = snap.staff,
            branchCount = snap.branches.size,
            settings = snap.settings,
            shiftsMap = shiftsMap,
            transfers = snap.transfers,
            holidays = snap.holidays,
            approvedLeaves = approved,
            ltMap = ltMap,
            mainName = mainName,
            attendanceList = snap.attendance,
            breaksList = snap.breaks
        )
    }

    fun computeStaffMonthlyReport(snap: AdminDataSnapshot, ctrl: AdminControlState): StaffMonthlyReport? {
        val s = snap.staff.find { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(ctrl.selectedStaffCode) }
            ?: snap.staff.firstOrNull() ?: return null
        val (y, m) = parseYm(ctrl.selectedMonth)
        val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(snap.leaveTypes)
        val approved = snap.leaves.filter { AttendanceCalculator.isApproved(it) }
        val shiftsMap = snap.shifts.associateBy { it.shift }
        val attMap = snap.attendance.associateBy { "${it.date}|${AttendanceCalculator.nc(it.code)}" }
        val brkMap = snap.breaks.groupBy { "${it.date}|${AttendanceCalculator.nc(it.code)}" }
        val (days, totals) = AttendanceCalculator.buildStaffMonth(
            s, y, m, snap.todayReal, "", snap.settings, shiftsMap, snap.transfers,
            snap.holidays, approved, ltMap, mainName, attMap, brkMap
        )
        val yearLeaves = AttendanceCalculator.leaveCalc(
            s, approved, ltMap, mainName,
            AttendanceCalculator.holMap(snap.holidays, s, snap.transfers),
            AttendanceCalculator.weeklyOffSet(s, snap.settings),
            y
        )
        val sh = shiftsMap[s.shift]
        return StaffMonthlyReport(
            org = snap.settings["OrgName"].orEmpty(),
            addr = snap.settings["Address"].orEmpty(),
            code = s.code,
            name = s.name,
            designation = s.designation,
            branch = AttendanceCalculator.branchAt(s, snap.transfers, "$y-${m.toString().padStart(2, '0')}-01"),
            shiftStart = sh?.startTime ?: "09:30",
            shiftEnd = sh?.endTime ?: "21:30",
            grace = snap.settings["GraceMinutes"]?.toIntOrNull() ?: 0,
            joinDate = s.joinDate,
            leaveDate = s.leaveDate,
            reservedDays = AttendanceCalculator.numOrNull(s.reservedDays),
            probationMonths = AttendanceCalculator.numOrNull(s.probationMonths),
            days = days,
            sum = totals,
            year = y,
            leaveBalances = AttendanceCalculator.balOf(s, ltMap, mainName, yearLeaves, y)
        )
    }

    fun computeMonthlySummary(snap: AdminDataSnapshot, ctrl: AdminControlState): List<StaffSummaryEntry> {
        val (y, m) = parseYm(ctrl.selectedMonth)
        val mFrom = "$y-${m.toString().padStart(2, '0')}-01"
        val mTo = "$y-${m.toString().padStart(2, '0')}-${YearMonth.of(y, m).lengthOfMonth().toString().padStart(2, '0')}"
        val bl = ctrl.selectedBranch.trim().lowercase()
        val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(snap.leaveTypes)
        val approved = snap.leaves.filter { AttendanceCalculator.isApproved(it) }
        val shiftsMap = snap.shifts.associateBy { it.shift }
        val attMap = snap.attendance.associateBy { "${it.date}|${AttendanceCalculator.nc(it.code)}" }
        val brkMap = snap.breaks.groupBy { "${it.date}|${AttendanceCalculator.nc(it.code)}" }

        return snap.staff.filter { s ->
            AttendanceCalculator.inScope(s, mFrom, mTo) &&
                (bl.isEmpty() || AttendanceCalculator.brSpan(s, snap.transfers, mFrom, mTo).any { it.lowercase() == bl })
        }.map { s ->
            val (_, sum) = AttendanceCalculator.buildStaffMonth(
                s, y, m, snap.todayReal, bl, snap.settings, shiftsMap, snap.transfers,
                snap.holidays, approved, ltMap, mainName, attMap, brkMap
            )
            StaffSummaryEntry(
                code = s.code,
                name = s.name,
                designation = s.designation,
                branch = AttendanceCalculator.brSpan(s, snap.transfers, mFrom, mTo).joinToString(" → "),
                sum = sum
            )
        }
    }

    fun computeYearlySummary(snap: AdminDataSnapshot, ctrl: AdminControlState): List<YearlyStaffRow> {
        val (y, _) = parseYm(ctrl.selectedMonth)
        val yFrom = "$y-01-01"
        val yTo = "$y-12-31"
        val bl = ctrl.selectedBranch.trim().lowercase()
        val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(snap.leaveTypes)
        val approved = snap.leaves.filter { AttendanceCalculator.isApproved(it) }
        val shiftsMap = snap.shifts.associateBy { it.shift }
        val attMap = snap.attendance.associateBy { "${it.date}|${AttendanceCalculator.nc(it.code)}" }
        val brkMap = snap.breaks.groupBy { "${it.date}|${AttendanceCalculator.nc(it.code)}" }

        return snap.staff.filter { s ->
            AttendanceCalculator.inScope(s, yFrom, yTo) &&
                (bl.isEmpty() || AttendanceCalculator.brSpan(s, snap.transfers, yFrom, yTo).any { it.lowercase() == bl })
        }.map { s ->
            var work = 0; var present = 0; var late = 0; var absent = 0; var leave = 0
            var otMin = 0; var workMin = 0
            val monthlyPresent = (1..12).map { m ->
                val (_, sum) = AttendanceCalculator.buildStaffMonth(
                    s, y, m, snap.todayReal, bl, snap.settings, shiftsMap, snap.transfers,
                    snap.holidays, approved, ltMap, mainName, attMap, brkMap
                )
                work += sum.work; present += sum.present; late += sum.late; absent += sum.absent
                leave += sum.leave; otMin += sum.otMin; workMin += sum.workMin
                val lastD = YearMonth.of(y, m).lengthOfMonth()
                val mFrom = "$y-${m.toString().padStart(2, '0')}-01"
                val mTo = "$y-${m.toString().padStart(2, '0')}-${lastD.toString().padStart(2, '0')}"
                if (mFrom > snap.todayReal || !AttendanceCalculator.empIn(s, mFrom, mTo)) null else sum.present
            }
            YearlyStaffRow(
                code = s.code,
                name = s.name,
                branchSpan = AttendanceCalculator.brSpan(s, snap.transfers, yFrom, yTo).joinToString(" → "),
                monthlyPresent = monthlyPresent,
                totals = AttendanceSummaryTotals(
                    work = work, present = present, late = late, absent = absent,
                    leave = leave, otMin = otMin, workMin = workMin
                )
            )
        }
    }

    fun computeBreakReport(snap: AdminDataSnapshot, ctrl: AdminControlState): List<BreakReportStaffRow> {
        val from = ctrl.breakFrom
        val to = ctrl.breakTo
        val bl = ctrl.selectedBranch.trim().lowercase()
        val scoped = snap.staff.filter { s ->
            AttendanceCalculator.inScope(s, from, to) &&
                (bl.isEmpty() || AttendanceCalculator.brSpan(s, snap.transfers, from, to).any { it.lowercase() == bl })
        }
        return scoped.map { s ->
            val ncCode = AttendanceCalculator.nc(s.code)
            val items = snap.breaks.filter {
                AttendanceCalculator.nc(it.code) == ncCode && it.date >= from && it.date <= to
            }.sortedWith(compareBy<BreakEntity> { it.date }.thenBy { it.startTime })

            val daysSet = items.map { it.date }.toSet()
            var totalMin = 0
            var completedCount = 0
            var incompleteCount = 0
            var maxMin = 0
            var maxDate = ""

            items.forEach { b ->
                if (b.endTime.isBlank()) {
                    incompleteCount++
                } else {
                    val sm = AttendanceCalculator.parseMinutes(b.startTime)
                    val em = AttendanceCalculator.parseMinutes(b.endTime)
                    if (sm != null && em != null && em >= sm) {
                        val d = em - sm
                        totalMin += d
                        completedCount++
                        if (d > maxMin) {
                            maxMin = d
                            maxDate = b.date
                        }
                    }
                }
            }
            BreakReportStaffRow(
                staff = s,
                branchAtToday = AttendanceCalculator.branchAt(s, snap.transfers, snap.todayReal),
                items = items,
                daysCount = daysSet.size,
                totalCount = items.size,
                totalMin = totalMin,
                avgMin = if (completedCount > 0) totalMin / completedCount else 0,
                maxMin = maxMin,
                maxDate = maxDate,
                incompleteCount = incompleteCount
            )
        }
    }

    // Repository Actions
    fun saveManualPunch(code: String, date: String, inT: String, outT: String, note: String) {
        viewModelScope.launch {
            val res = repository.saveManualPunch(code, date, inT, outT, note)
            showToast(res.message, res.ok)
        }
    }

    fun deleteManualPunch(code: String, date: String) {
        viewModelScope.launch {
            val res = repository.deleteManualPunch(code, date)
            showToast(res.message, res.ok)
        }
    }

    fun saveBreak(id: Int, code: String, date: String, start: String, end: String) {
        viewModelScope.launch {
            val res = repository.saveBreak(id, code, date, start, end)
            showToast(res.message, res.ok)
        }
    }

    fun deleteBreak(id: Int) {
        viewModelScope.launch {
            repository.deleteBreak(id)
            showToast("ব্রেক মুছে ফেলা হয়েছে")
        }
    }

    fun addAdminLeave(code: String, type: String, from: String, to: String, reason: String) {
        viewModelScope.launch {
            val res = repository.adminAddLeave(code, type, from, to, reason)
            showToast(res.message, res.ok)
        }
    }

    fun decideLeave(id: Int, status: String, note: String) {
        viewModelScope.launch {
            repository.decideLeave(id, status, note)
            showToast(if (status == "Approved") "অনুমোদিত হয়েছে" else "প্রত্যাখ্যাত হয়েছে")
        }
    }

    fun deleteLeave(id: Int) {
        viewModelScope.launch {
            repository.deleteLeave(id)
            showToast("মুছে ফেলা হয়েছে")
        }
    }

    fun addTransfer(code: String, effDate: String, newBranch: String) {
        viewModelScope.launch {
            val res = repository.addTransfer(code, effDate, newBranch)
            showToast(res.message, res.ok)
        }
    }

    fun deleteTransfer(id: Int) {
        viewModelScope.launch {
            val res = repository.deleteTransfer(id)
            showToast(res.message, res.ok)
        }
    }

    fun saveFace(code: String, vectors: List<List<Double>>) {
        viewModelScope.launch {
            val res = repository.saveFace(code, vectors)
            showToast(res.message, res.ok)
        }
    }

    fun deleteFace(code: String) {
        viewModelScope.launch {
            val res = repository.deleteFace(code)
            showToast(res.message, res.ok)
        }
    }

    fun saveStaff(entity: StaffEntity) {
        viewModelScope.launch {
            repository.saveStaff(entity)
            showToast("কর্মী সংরক্ষিত হয়েছে")
        }
    }

    fun deleteStaff(id: Int) {
        viewModelScope.launch {
            repository.deleteStaff(id)
            showToast("কর্মী মুছে ফেলা হয়েছে")
        }
    }

    fun saveLeaveType(entity: LeaveTypeEntity) {
        viewModelScope.launch {
            repository.saveLeaveType(entity)
            showToast("ছুটির ধরন সংরক্ষিত হয়েছে")
        }
    }

    fun deleteLeaveType(id: Int) {
        viewModelScope.launch {
            repository.deleteLeaveType(id)
            showToast("ছুটির ধরন মুছে ফেলা হয়েছে")
        }
    }

    fun saveHoliday(entity: HolidayEntity) {
        viewModelScope.launch {
            repository.saveHoliday(entity)
            showToast("সরকারি ছুটি সংরক্ষিত হয়েছে")
        }
    }

    fun deleteHoliday(id: Int) {
        viewModelScope.launch {
            repository.deleteHoliday(id)
            showToast("সরকারি ছুটি মুছে ফেলা হয়েছে")
        }
    }

    fun saveShift(entity: ShiftEntity) {
        viewModelScope.launch {
            repository.saveShift(entity)
            showToast("শিফট সংরক্ষিত হয়েছে")
        }
    }

    fun deleteShift(id: Int) {
        viewModelScope.launch {
            repository.deleteShift(id)
            showToast("শিফট মুছে ফেলা হয়েছে")
        }
    }

    fun saveBranch(entity: BranchEntity) {
        viewModelScope.launch {
            repository.saveBranch(entity)
            showToast("শাখা সংরক্ষিত হয়েছে")
        }
    }

    fun deleteBranch(id: Int) {
        viewModelScope.launch {
            repository.deleteBranch(id)
            showToast("শাখা মুছে ফেলা হয়েছে")
        }
    }

    fun saveSettingsBatch(updates: List<SettingEntity>) {
        viewModelScope.launch {
            updates.forEach { repository.saveSetting(it) }
            showToast("সেটিংস সংরক্ষিত হয়েছে")
        }
    }

    private fun parseYm(ym: String): Pair<Int, Int> {
        val parts = ym.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 11
        return y to m
    }

    class Factory(
        private val repository: AttendanceRepository,
        private val prefs: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(repository, prefs) as T
        }
    }
}
