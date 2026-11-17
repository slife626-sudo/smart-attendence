package com.example.domain

import com.example.data.db.AttendanceEntity
import com.example.data.db.BreakEntity
import com.example.data.db.HolidayEntity
import com.example.data.db.LeaveEntity
import com.example.data.db.LeaveTypeEntity
import com.example.data.db.SettingEntity
import com.example.data.db.ShiftEntity
import com.example.data.db.StaffEntity
import com.example.data.db.TransferEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LeaveTypeSpec(
    val name: String,
    val days: Int?,
    val paid: Boolean,
    val holiday: Boolean,
    val prob: Int
)

data class LeaveBalanceRow(
    val type: String,
    val quota: Int?,
    val paid: Boolean,
    val used: Int,
    val prob: Int,
    val from: String,
    val unpaid: Int,
    val left: Int?
)

data class LeaveTypeAvailability(
    val name: String,
    val avail: Boolean,
    val why: String
)

data class BreakSummary(
    val n: Int = 0,
    val min: Int = 0,
    val open: String = "",
    val live: Boolean = false
)

enum class DayStatusClass {
    PRESENT, LATE, ABSENT, INCOMPLETE, HOLIDAY_OR_OFF, LEAVE_PAID, LEAVE_UNPAID, OFF_DUTY, FUTURE_OR_OUT
}

data class DayAttendanceRow(
    val date: String,
    val dayName: String,
    val inTime: String,
    val outTime: String,
    val status: String,
    val cls: DayStatusClass,
    val note: String,
    val mins: Int,
    val breakSummary: BreakSummary? = null
)

data class AttendanceSummaryTotals(
    val work: Int = 0,
    val present: Int = 0,
    val late: Int = 0,
    val absent: Int = 0,
    val leave: Int = 0,
    val leavePaid: Int = 0,
    val leaveUnpaid: Int = 0,
    val holiday: Int = 0,
    val off: Int = 0,
    val incomplete: Int = 0,
    val offduty: Int = 0,
    val workMin: Int = 0,
    val otMin: Int = 0,
    val brkMin: Int = 0,
    val brkN: Int = 0,
    val brkDays: Int = 0,
    val brkBad: Int = 0
)

data class StaffMonthlyReport(
    val org: String,
    val addr: String,
    val code: String,
    val name: String,
    val designation: String,
    val branch: String,
    val shiftStart: String,
    val shiftEnd: String,
    val grace: Int,
    val joinDate: String,
    val leaveDate: String,
    val reservedDays: Int?,
    val probationMonths: Int?,
    val days: List<DayAttendanceRow>,
    val sum: AttendanceSummaryTotals,
    val year: Int,
    val leaveBalances: List<LeaveBalanceRow>
)

data class StaffSummaryEntry(
    val code: String,
    val name: String,
    val designation: String,
    val branch: String,
    val sum: AttendanceSummaryTotals
)

data class TodayStaffRow(
    val code: String,
    val name: String,
    val branch: String,
    val inTime: String,
    val outTime: String,
    val status: String,
    val cls: DayStatusClass,
    val breakSummary: BreakSummary?
)

data class BranchStat(
    val branch: String,
    val work: Int,
    val present: Int,
    val staff: Int
)

data class RankedStaffCount(
    val name: String,
    val count: Int
)

data class DailyPresencePoint(
    val dayOfMonth: Int,
    val presentCount: Int
)

data class DashboardReport(
    val org: String,
    val addr: String,
    val selectedDate: String,
    val todayReal: String,
    val staffCount: Int,
    val branchCount: Int,
    val todayPresent: Int,
    val todayLate: Int,
    val todayAbsent: Int,
    val todayLeave: Int,
    val todayOff: Int,
    val totalWork: Int,
    val totalPresent: Int,
    val totalLate: Int,
    val totalAbsent: Int,
    val totalLeave: Int,
    val totalOtMin: Int,
    val totalOff: Int,
    val dailySeries: List<DailyPresencePoint>,
    val branchStats: List<BranchStat>,
    val topLate: List<RankedStaffCount>,
    val topAbsent: List<RankedStaffCount>,
    val todayRows: List<TodayStaffRow>
)

data class YearlyStaffRow(
    val code: String,
    val name: String,
    val branchSpan: String,
    val monthlyPresent: List<Int?>, // 12 elements (Jan..Dec)
    val totals: AttendanceSummaryTotals
)

data class BreakReportStaffRow(
    val staff: StaffEntity,
    val branchAtToday: String,
    val items: List<BreakEntity>,
    val daysCount: Int,
    val totalCount: Int,
    val totalMin: Int,
    val avgMin: Int,
    val maxMin: Int,
    val maxDate: String,
    val incompleteCount: Int
)

object AttendanceCalculator {
    private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val MONTH_NAMES_BN = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    val MONTH_SHORT_BN = listOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )
    private val DAY_NAMES_EN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val DAY_NAMES_BN = mapOf(
        "Sun" to "রবিবার",
        "Mon" to "সোমবার",
        "Tue" to "মঙ্গলবার",
        "Wed" to "বুধবার",
        "Thu" to "বৃহস্পতিবার",
        "Fri" to "শুক্রবার",
        "Sat" to "শনিবার"
    )
    private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val TIME_REGEX = Regex("^\\d{2}:\\d{2}$")

    val DEFAULT_LEAVE_TYPE = LeaveTypeSpec(
        name = "সংরক্ষিত ছুটি",
        days = 12,
        paid = true,
        holiday = false,
        prob = 0
    )

    fun toBanglaDigits(input: Any?): String {
        val s = input?.toString() ?: ""
        val sb = StringBuilder(s.length)
        for (ch in s) {
            if (ch in '0'..'9') {
                sb.append(BN_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun monthLabelBn(yyyyMm: String): String {
        val parts = yyyyMm.split("-")
        if (parts.size != 2) return yyyyMm
        val y = parts[0]
        val m = parts[1].toIntOrNull() ?: return yyyyMm
        val mn = MONTH_NAMES_BN.getOrElse(m - 1) { parts[1] }
        return "$mn ${toBanglaDigits(y)}"
    }

    fun formatDateBn(yyyyMmDd: String): String {
        val parts = yyyyMmDd.split("-")
        if (parts.size != 3) return yyyyMmDd
        return toBanglaDigits("${parts[2]}-${parts[1]}-${parts[0]}")
    }

    fun dayNameEn(yyyyMmDd: String): String {
        val d = parseDateOrNull(yyyyMmDd) ?: return "Fri"
        val idx = d.dayOfWeek.value % 7 // Sun=0, Mon=1, ..., Sat=6
        return DAY_NAMES_EN[idx]
    }

    fun dayNameBn(dayEn: String): String = DAY_NAMES_BN[dayEn] ?: dayEn

    fun formatTime12h(hhmm: String?): String {
        val t = hhmm?.trim().orEmpty()
        if (t.isEmpty()) return "-"
        val m = parseMinutes(t) ?: return t
        val h24 = (m / 60) % 24
        val min = m % 60
        val amPm = if (h24 >= 12) "PM" else "AM"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "${toBanglaDigits(h12)}:${toBanglaDigits(min.toString().padStart(2, '0'))} $amPm"
    }

    fun formatHmShort(mins: Int): String {
        if (mins <= 0) return "০"
        return "${toBanglaDigits(mins / 60)} ঘ ${toBanglaDigits(mins % 60)} মি"
    }

    fun formatHmLong(mins: Int): String {
        if (mins <= 0) return "০"
        return "${toBanglaDigits(mins / 60)} ঘণ্টা ${toBanglaDigits(mins % 60)} মি."
    }

    fun formatBreakMin(mins: Int): String {
        return if (mins >= 60) formatHmShort(mins) else "${toBanglaDigits(mins)} মি"
    }

    fun formatTimeShortColon(mins: Int): String {
        if (mins <= 0) return "—"
        return "${toBanglaDigits(mins / 60)}:${toBanglaDigits((mins % 60).toString().padStart(2, '0'))}"
    }

    fun sourceLabelBn(src: String?): String {
        val s = src.orEmpty()
        return when {
            s.contains("Device", ignoreCase = true) -> "ডিভাইস"
            s.contains("Manual", ignoreCase = true) -> "ম্যানুয়াল"
            else -> "ফোন"
        }
    }

    fun nc(code: String?): String {
        return (code ?: "").trim().replace(Regex("^0+(?=\\d)"), "")
    }

    fun isActive(staff: StaffEntity): Boolean {
        return staff.status.ifBlank { "Active" }.trim().lowercase() != "inactive"
    }

    fun validDateOrEmpty(v: String?): String {
        val s = v?.trim().orEmpty()
        return if (DATE_REGEX.matches(s) && parseDateOrNull(s) != null) s else ""
    }

    fun isValidDate(v: String?): Boolean = validDateOrEmpty(v).isNotEmpty()
    fun isValidTime(v: String?): Boolean = TIME_REGEX.matches(v?.trim().orEmpty())

    fun parseDateOrNull(v: String?): LocalDate? {
        return try {
            val s = v?.trim().orEmpty()
            if (!DATE_REGEX.matches(s)) null else LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            null
        }
    }

    fun parseMinutes(t: String?): Int? {
        val m = Regex("(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])?").find(t?.trim().orEmpty()) ?: return null
        var h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        val ap = m.groupValues[3].lowercase()
        if (ap == "pm" && h < 12) h += 12
        if (ap == "am" && h == 12) h = 0
        return h * 60 + min
    }

    fun numOrNull(v: String?): Int? {
        val x = v?.trim().orEmpty()
        if (x.isEmpty()) return null
        val d = x.toDoubleOrNull() ?: return null
        return max(0, d.toInt())
    }

    fun empOn(s: StaffEntity, date: String): Boolean {
        val j = validDateOrEmpty(s.joinDate)
        val l = validDateOrEmpty(s.leaveDate)
        return (j.isEmpty() || date >= j) && (l.isEmpty() || date <= l)
    }

    fun empMessage(s: StaffEntity, date: String): String {
        val j = validDateOrEmpty(s.joinDate)
        return if (j.isNotEmpty() && date < j) {
            "আপনার যোগদানের তারিখ এখনো আসেনি"
        } else {
            "আপনার চাকরির শেষ কর্মদিবস পার হয়ে গেছে"
        }
    }

    fun empIn(s: StaffEntity, from: String, to: String): Boolean {
        val j = validDateOrEmpty(s.joinDate)
        val l = validDateOrEmpty(s.leaveDate)
        return (j.isEmpty() || j <= to) && (l.isEmpty() || l >= from)
    }

    fun inScope(s: StaffEntity, from: String, to: String): Boolean {
        return empIn(s, from, to) && (isActive(s) || validDateOrEmpty(s.leaveDate).isNotEmpty())
    }

    fun settingsMap(list: List<SettingEntity>): Map<String, String> {
        val m = mutableMapOf<String, String>()
        list.forEach { m[it.key] = it.value }
        if (m["Timezone"].isNullOrBlank()) m["Timezone"] = "Asia/Dhaka"
        return m
    }

    fun nowInTimezone(tz: String): Triple<String, String, String> {
        val zone = try {
            ZoneId.of(tz.ifBlank { "Asia/Dhaka" })
        } catch (_: Exception) {
            ZoneId.of("Asia/Dhaka")
        }
        val zdt = ZonedDateTime.now(zone)
        val date = zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val time = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
        val ts = Instant.now().toString()
        return Triple(date, time, ts)
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val r = Math.PI / 180.0
        val dLat = (lat2 - lat1) * r
        val dLng = (lng2 - lng1) * r
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * r) * cos(lat2 * r) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return (6371000.0 * c).roundToInt()
    }

    fun dateRange(from: String, to: String, maxDays: Int = 400): List<String> {
        val start = parseDateOrNull(from) ?: return emptyList()
        val end = parseDateOrNull(to) ?: return emptyList()
        if (end.isBefore(start)) return emptyList()
        val out = mutableListOf<String>()
        var cur = start
        var count = 0
        while (!cur.isAfter(end) && count < maxDays) {
            out.add(cur.format(DateTimeFormatter.ISO_LOCAL_DATE))
            cur = cur.plusDays(1)
            count++
        }
        return out
    }

    fun branchAt(staff: StaffEntity, transfers: List<TransferEntity>, date: String): String {
        val list = transfers
            .filter { nc(it.code) == nc(staff.code) }
            .sortedBy { it.effDate.trim() }
        if (list.isEmpty()) return staff.branch.trim()
        if (date < list.first().effDate.trim()) return list.first().fromBranch.trim()
        var b = list.first().toBranch.trim()
        for (t in list) {
            if (t.effDate.trim() <= date) b = t.toBranch.trim()
        }
        return b
    }

    fun brSpan(staff: StaffEntity, transfers: List<TransferEntity>, from: String, to: String): List<String> {
        val list = transfers.filter { nc(it.code) == nc(staff.code) }
        if (list.isEmpty()) return listOf(staff.branch.trim())
        val out = mutableListOf<String>()
        dateRange(from, to, 4000).forEach { ds ->
            val b = branchAt(staff, transfers, ds)
            if (!out.contains(b)) out.add(b)
        }
        return out
    }

    fun parseLeaveTypes(rows: List<LeaveTypeEntity>): Pair<Map<String, LeaveTypeSpec>, String> {
        val map = linkedMapOf<String, LeaveTypeSpec>()
        var main = ""
        rows.forEach { r ->
            val n = r.ltype.trim()
            if (n.isEmpty()) return@forEach
            val dn = r.days.trim().toIntOrNull()
            val paid = r.paid.ifBlank { "Yes" }.trim().lowercase() != "no"
            val holiday = r.kind.trim().lowercase() == "holiday"
            val prob = max(0, r.probation.trim().toIntOrNull() ?: 0)
            val spec = LeaveTypeSpec(name = n, days = dn, paid = paid, holiday = holiday, prob = prob)
            map[n] = spec
            if (main.isEmpty() && spec.days != null && spec.paid && !spec.holiday) {
                main = n
            }
        }
        if (map.isEmpty()) {
            map[DEFAULT_LEAVE_TYPE.name] = DEFAULT_LEAVE_TYPE
            main = DEFAULT_LEAVE_TYPE.name
        }
        return map to main
    }

    fun effType(t: LeaveTypeSpec, mainName: String, staff: StaffEntity): LeaveTypeSpec {
        if (t.name != mainName) return t
        val rd = numOrNull(staff.reservedDays)
        val pm = numOrNull(staff.probationMonths)
        return t.copy(
            days = rd ?: t.days,
            prob = pm ?: t.prob
        )
    }

    fun probEnd(joinDate: String, months: Int): String {
        val y = joinDate.take(4).toIntOrNull() ?: return ""
        val m = joinDate.substring(5, 7).toIntOrNull() ?: return ""
        val tot = y * 12 + (m - 1) + months
        return "${tot / 12}-${((tot % 12) + 1).toString().padStart(2, '0')}-01"
    }

    fun quotaOf(t: LeaveTypeSpec, joinDate: String, year: Int): Pair<Int?, String> {
        if (t.days == 0) return 0 to ""
        if (t.prob <= 0 || joinDate.isEmpty()) return t.days to ""
        val e = probEnd(joinDate, t.prob)
        val ey = e.take(4).toIntOrNull() ?: return t.days to ""
        val em = e.substring(5, 7).toIntOrNull() ?: return t.days to ""
        return when {
            ey > year -> 0 to e
            ey == year -> {
                val q = t.days?.let { (it * (13 - em) / 12.0).roundToInt() }
                q to e
            }
            else -> t.days to ""
        }
    }

    fun holMap(
        holidays: List<HolidayEntity>,
        staff: StaffEntity,
        transfers: List<TransferEntity>
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        holidays.forEach { h ->
            val from = validDateOrEmpty(h.dateFrom)
            val to = validDateOrEmpty(h.dateTo).ifEmpty { from }
            if (from.isEmpty() || to < from) return@forEach
            val branchList = h.branch.split(',', '،').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            dateRange(from, to, 4000).forEach { d ->
                val bl = branchAt(staff, transfers, d).trim().lowercase()
                if (branchList.isNotEmpty() && !branchList.contains(bl)) return@forEach
                if (!map.containsKey(d)) map[d] = h.name.ifBlank { "ছুটি" }
            }
        }
        return map
    }

    fun isApproved(l: LeaveEntity): Boolean {
        val s = l.status.trim().lowercase()
        return s.isEmpty() || s == "approved"
    }

    fun isPending(l: LeaveEntity): Boolean = l.status.trim().lowercase() == "pending"

    fun weeklyOffSet(staff: StaffEntity, settings: Map<String, String>): Set<String> {
        val raw = staff.weeklyOff.ifBlank { settings["WeeklyOff"] ?: "Fri" }
        return raw.split(' ', ',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    data class LeaveCalcResult(
        val dates: Map<String, Pair<String, Boolean>>, // date -> (typeName, isProbation)
        val idxByDate: Map<String, Int>,
        val byType: Map<String, List<String>>,
        val probByType: Map<String, List<String>>,
        val quotaByType: Map<String, Int?>
    )

    fun leaveCalc(
        staff: StaffEntity,
        allLeaves: List<LeaveEntity>,
        ltMap: Map<String, LeaveTypeSpec>,
        mainName: String,
        hols: Map<String, String>,
        offs: Set<String>,
        year: Int,
        includePending: Boolean = false
    ): LeaveCalcResult {
        val code = nc(staff.code)
        val joinDate = validDateOrEmpty(staff.joinDate)
        val leaveDate = validDateOrEmpty(staff.leaveDate)
        val hk = mutableSetOf<String>()
        allLeaves.filter {
            nc(it.code) == code && isApproved(it) && isValidDate(it.dateFrom) && isValidDate(it.dateTo) &&
                (ltMap[it.typ.trim()]?.holiday == true)
        }.forEach { l ->
            dateRange(l.dateFrom, l.dateTo, 4000).forEach { hk.add(it) }
        }

        val set = linkedMapOf<String, Pair<String, Boolean>>()
        val quotaMap = mutableMapOf<String, Int?>()
        allLeaves.filter {
            nc(it.code) == code && (isApproved(it) || (includePending && isPending(it)))
        }.sortedBy { it.dateFrom }.forEach { l ->
            if (!isValidDate(l.dateFrom) || !isValidDate(l.dateTo)) return@forEach
            val baseT = ltMap[l.typ.trim()] ?: ltMap[mainName] ?: DEFAULT_LEAVE_TYPE
            val t = effType(baseT, mainName, staff)
            if (t.holiday) return@forEach
            dateRange(l.dateFrom, l.dateTo, 4000).forEach { d ->
                if (!d.startsWith("$year-") || hols.containsKey(d) || hk.contains(d)) return@forEach
                if ((joinDate.isNotEmpty() && d < joinDate) || (leaveDate.isNotEmpty() && d > leaveDate)) return@forEach
                if (offs.contains(dayNameEn(d)) || set.containsKey(d)) return@forEach
                val qo = quotaMap.getOrPut(t.name) { quotaOf(t, joinDate, year).first }
                val fromStr = quotaOf(t, joinDate, year).second
                quotaMap[t.name] = qo
                set[d] = t.name to (fromStr.isNotEmpty() && d < fromStr)
            }
        }

        val by = mutableMapOf<String, MutableList<String>>()
        val pr = mutableMapOf<String, MutableList<String>>()
        val idxByDate = mutableMapOf<String, Int>()
        set.keys.sorted().forEach { d ->
            val (tName, isProb) = set[d]!!
            if (isProb) {
                pr.getOrPut(tName) { mutableListOf() }.add(d)
            } else {
                val list = by.getOrPut(tName) { mutableListOf() }
                idxByDate[d] = list.size
                list.add(d)
            }
        }
        return LeaveCalcResult(set, idxByDate, by, pr, quotaMap)
    }

    fun isDatePaid(calc: LeaveCalcResult, date: String, typeSpec: LeaveTypeSpec): Boolean {
        val entry = calc.dates[date] ?: return typeSpec.paid
        if (entry.second) return false
        val q = calc.quotaByType[entry.first]
        val idx = calc.idxByDate[date] ?: 0
        return typeSpec.paid && (q == null || idx < q)
    }

    fun balOf(
        staff: StaffEntity,
        ltMap: Map<String, LeaveTypeSpec>,
        mainName: String,
        calc: LeaveCalcResult,
        year: Int
    ): List<LeaveBalanceRow> {
        val joinDate = validDateOrEmpty(staff.joinDate)
        return ltMap.values.filter { !it.holiday }.map { t0 ->
            val t = effType(t0, mainName, staff)
            val (qq, fromStr) = quotaOf(t, joinDate, year)
            val used = calc.byType[t.name]?.size ?: 0
            val pr = calc.probByType[t.name]?.size ?: 0
            val pd = if (!t.paid) 0 else if (qq == null) used else min(used, qq)
            LeaveBalanceRow(
                type = t.name,
                quota = qq,
                paid = t.paid,
                used = used,
                prob = pr,
                from = fromStr,
                unpaid = (used - pd) + pr,
                left = if (t.paid && qq != null) max(0, qq - used) else null
            )
        }
    }

    fun breakSummary(breaks: List<BreakEntity>, date: String, today: String): BreakSummary {
        var n = 0
        var minTotal = 0
        var open = ""
        breaks.forEach { b ->
            n++
            if (b.endTime.isBlank()) {
                open = b.startTime
            } else {
                val s = parseMinutes(b.startTime)
                val e = parseMinutes(b.endTime)
                if (s != null && e != null && e >= s) minTotal += (e - s)
            }
        }
        return BreakSummary(n = n, min = minTotal, open = open, live = open.isNotEmpty() && date >= today)
    }

    fun breakTextPlain(k: BreakSummary?): String {
        if (k == null || k.n == 0) return "-"
        val completed = k.n - (if (k.open.isNotEmpty()) 1 else 0)
        val parts = mutableListOf<String>()
        if (completed > 0) parts.add("${formatBreakMin(k.min)} (${toBanglaDigits(completed)} বার)")
        if (k.open.isNotEmpty()) {
            parts.add(
                if (k.live) "চলমান (${formatTime12h(k.open)} থেকে)"
                else "⚠ শেষ নেই (${formatTime12h(k.open)})"
            )
        }
        return parts.joinToString(" · ")
    }

    fun workDaysCount(
        from: String,
        to: String,
        hols: Map<String, String>,
        offs: Set<String>
    ): Int {
        return dateRange(from, to, 4000).count { d ->
            !hols.containsKey(d) && !offs.contains(dayNameEn(d))
        }
    }

    fun impactPreview(
        leave: LeaveEntity,
        staff: StaffEntity?,
        allLeaves: List<LeaveEntity>,
        ltMap: Map<String, LeaveTypeSpec>,
        mainName: String,
        holidays: List<HolidayEntity>,
        transfers: List<TransferEntity>,
        settings: Map<String, String>
    ): String {
        val t = ltMap[leave.typ.trim()] ?: ltMap[mainName] ?: DEFAULT_LEAVE_TYPE
        if (t.holiday) return "ছুটি গণনায় নয় (সরকারি ছুটির মতো)"
        val s = staff ?: StaffEntity(code = leave.code, name = "")
        val year = leave.dateFrom.take(4).toIntOrNull() ?: return ""
        val hols = holMap(holidays, s, transfers)
        val offs = weeklyOffSet(s, settings)
        val combined = allLeaves.filter { isApproved(it) } + leave.copy(status = "Approved")
        val calc = leaveCalc(s, combined, ltMap, mainName, hols, offs, year)
        var paidCount = 0
        var unpaidCount = 0
        calc.dates.forEach { (ds, pair) ->
            if (ds >= leave.dateFrom && ds <= leave.dateTo) {
                val typeForDate = ltMap[pair.first] ?: t
                if (isDatePaid(calc, ds, typeForDate)) paidCount++ else unpaidCount++
            }
        }
        return "অনুমোদনে: সবেতন ${toBanglaDigits(paidCount)} · বিনা বেতনে ${toBanglaDigits(unpaidCount)}"
    }

    fun buildStaffMonth(
        staff: StaffEntity,
        year: Int,
        month: Int,
        today: String,
        onlyBranchLower: String,
        settings: Map<String, String>,
        shiftsMap: Map<String, ShiftEntity>,
        transfers: List<TransferEntity>,
        holidays: List<HolidayEntity>,
        approvedLeaves: List<LeaveEntity>,
        ltMap: Map<String, LeaveTypeSpec>,
        mainName: String,
        attendanceMap: Map<String, AttendanceEntity>, // key: "$date|${nc(code)}"
        breaksMap: Map<String, List<BreakEntity>> // key: "$date|${nc(code)}"
    ): Pair<List<DayAttendanceRow>, AttendanceSummaryTotals> {
        val code = nc(staff.code)
        val sh = shiftsMap[staff.shift]
        val shiftS = parseMinutes(sh?.startTime ?: "09:30") ?: 570
        val shiftE = parseMinutes(sh?.endTime ?: "21:30") ?: 1290
        val grace = settings["GraceMinutes"]?.toIntOrNull() ?: 0
        val offs = weeklyOffSet(staff, settings)
        val hols = holMap(holidays, staff, transfers)
        val lastDay = YearMonth.of(year, month).lengthOfMonth()
        val li by lazy { leaveCalc(staff, approvedLeaves, ltMap, mainName, hols, offs, year) }

        var work = 0
        var present = 0
        var late = 0
        var absent = 0
        var leave = 0
        var leavePaid = 0
        var leaveUnpaid = 0
        var holiday = 0
        var off = 0
        var incomplete = 0
        var offduty = 0
        var workMin = 0
        var otMin = 0
        var brkMin = 0
        var brkN = 0
        var brkDays = 0
        var brkBad = 0

        val rows = mutableListOf<DayAttendanceRow>()
        val joinD = validDateOrEmpty(staff.joinDate)
        val leaveD = validDateOrEmpty(staff.leaveDate)

        for (d in 1..lastDay) {
            val date = "$year-${month.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
            if (onlyBranchLower.isNotEmpty() && branchAt(staff, transfers, date).lowercase() != onlyBranchLower) {
                continue
            }
            val dw = dayNameEn(date)
            val a = attendanceMap["$date|$code"]
            val inM = a?.let { parseMinutes(it.inTime) }
            val outM = a?.let { parseMinutes(it.outTime) }

            val dayBrks = breaksMap["$date|$code"]
            val bk = if (!dayBrks.isNullOrEmpty()) breakSummary(dayBrks, date, today) else null
            if (bk != null) {
                brkMin += bk.min
                brkN += bk.n
                brkDays++
                if (bk.open.isNotEmpty() && !bk.live) brkBad++
            }

            if (!empOn(staff, date)) {
                val note = if (joinD.isNotEmpty() && date < joinD) "যোগদানের আগে" else "চাকরি ছেড়েছেন"
                rows.add(DayAttendanceRow(date, dw, "", "", "—", DayStatusClass.FUTURE_OR_OUT, note, 0, bk))
                continue
            }

            val dayLeaves = approvedLeaves.filter { nc(it.code) == code && date >= it.dateFrom && date <= it.dateTo }
            val hk = dayLeaves.find { (ltMap[it.typ.trim()]?.holiday == true) }
            val lv = hk ?: dayLeaves.firstOrNull()
            val nqh = hk != null
            val holName = hols[date]
            val isOff = offs.contains(dw)

            fun span(): Int {
                if (inM == null || outM == null || outM == inM) return 0
                var x = outM - inM
                if (x < 0) x += 1440
                return x
            }

            var st = "উপস্থিত"
            var cls = DayStatusClass.PRESENT
            var note = a?.note.orEmpty()
            var mins = 0

            when {
                date > today -> {
                    st = "—"
                    cls = DayStatusClass.FUTURE_OR_OUT
                }
                (holName != null || isOff || nqh) && inM != null -> {
                    st = "অফ-ডে ডিউটি"
                    cls = DayStatusClass.OFF_DUTY
                    offduty++
                    mins = span()
                    workMin += mins
                    otMin += mins
                    if (note.isEmpty()) note = holName ?: (if (nqh) lv?.typ.orEmpty() else "সাপ্তাহিক ছুটিতে কাজ")
                }
                holName != null -> {
                    st = "সরকারি ছুটি"
                    cls = DayStatusClass.HOLIDAY_OR_OFF
                    note = holName
                    holiday++
                }
                isOff -> {
                    st = "সাপ্তাহিক ছুটি"
                    cls = DayStatusClass.HOLIDAY_OR_OFF
                    off++
                }
                nqh && lv != null -> {
                    st = lv.typ
                    cls = DayStatusClass.HOLIDAY_OR_OFF
                    note = lv.note
                    holiday++
                }
                lv != null -> {
                    val info = li.dates[date]
                    val t = ltMap[info?.first ?: lv.typ.trim()] ?: ltMap[mainName] ?: DEFAULT_LEAVE_TYPE
                    val un = !isDatePaid(li, date, t)
                    st = if (un) "বিনা বেতনে ছুটি" else "ছুটি"
                    cls = if (un) DayStatusClass.LEAVE_UNPAID else DayStatusClass.LEAVE_PAID
                    note = buildString {
                        append(lv.typ)
                        if (un && t.paid) {
                            if (lv.typ.isNotEmpty()) append(" · ")
                            append(if (info?.second == true) "শিক্ষানবিস কাল" else "সবেতন কোটা শেষ")
                        }
                    }
                    leave++
                    if (un) leaveUnpaid++ else leavePaid++
                }
                else -> {
                    work++
                    if (inM == null) {
                        st = "অনুপস্থিত"
                        cls = DayStatusClass.ABSENT
                        absent++
                    } else {
                        present++
                        val lateB = inM > shiftS + grace
                        val missOut = outM == null || outM == inM
                        if (lateB) {
                            late++
                            st = "দেরিতে"
                            cls = DayStatusClass.LATE
                            if (note.isEmpty()) note = "শিফটের ${toBanglaDigits(inM - shiftS)} মিনিট পরে"
                        }
                        if (missOut) {
                            incomplete++
                            val an = a?.note.orEmpty()
                            if (lateB) {
                                st = "${toBanglaDigits(inM - shiftS)} মিনিট লেট"
                                cls = DayStatusClass.LATE
                                note = (if (an.isNotEmpty()) "$an · " else "") + "আউট পাঞ্চ নেই"
                            } else {
                                st = "উপস্থিত"
                                cls = DayStatusClass.PRESENT
                                note = (if (an.isNotEmpty()) "$an · " else "") + "আউট পাঞ্চ নেই"
                            }
                        } else {
                            mins = span()
                            workMin += mins
                            if (outM!! > shiftE) otMin += (outM - shiftE)
                            else if (outM < shiftE && note.isEmpty()) note = "শিফটের আগে বের হয়েছেন"
                        }
                    }
                }
            }

            rows.add(
                DayAttendanceRow(
                    date = date,
                    dayName = dw,
                    inTime = a?.inTime.orEmpty(),
                    outTime = a?.outTime.orEmpty(),
                    status = st,
                    cls = cls,
                    note = note,
                    mins = mins,
                    breakSummary = bk
                )
            )
        }

        val totals = AttendanceSummaryTotals(
            work = work, present = present, late = late, absent = absent,
            leave = leave, leavePaid = leavePaid, leaveUnpaid = leaveUnpaid,
            holiday = holiday, off = off, incomplete = incomplete, offduty = offduty,
            workMin = workMin, otMin = otMin, brkMin = brkMin, brkN = brkN,
            brkDays = brkDays, brkBad = brkBad
        )
        return rows to totals
    }

    fun buildDashboard(
        year: Int,
        month: Int,
        selectedDate: String,
        todayReal: String,
        branchFilter: String,
        staffList: List<StaffEntity>,
        branchCount: Int,
        settings: Map<String, String>,
        shiftsMap: Map<String, ShiftEntity>,
        transfers: List<TransferEntity>,
        holidays: List<HolidayEntity>,
        approvedLeaves: List<LeaveEntity>,
        ltMap: Map<String, LeaveTypeSpec>,
        mainName: String,
        attendanceList: List<AttendanceEntity>,
        breaksList: List<BreakEntity>
    ): DashboardReport {
        val mFrom = "$year-${month.toString().padStart(2, '0')}-01"
        val lastD = YearMonth.of(year, month).lengthOfMonth()
        val mTo = "$year-${month.toString().padStart(2, '0')}-${lastD.toString().padStart(2, '0')}"
        val bl = branchFilter.trim().lowercase()

        val attMap = attendanceList.associateBy { "${it.date}|${nc(it.code)}" }
        val brkMap = breaksList.groupBy { "${it.date}|${nc(it.code)}" }

        val scoped = staffList.filter { s ->
            inScope(s, mFrom, mTo) && (bl.isEmpty() || brSpan(s, transfers, mFrom, mTo).any { it.lowercase() == bl })
        }

        var tPresent = 0
        var tLate = 0
        var tAbsent = 0
        var tLeave = 0
        var tOff = 0

        var totWork = 0
        var totPresent = 0
        var totLate = 0
        var totAbsent = 0
        var totLeave = 0
        var totOtMin = 0
        var totOff = 0

        val dailyCounts = mutableMapOf<String, Int>()
        val brStats = linkedMapOf<String, Triple<Int, Int, Int>>() // work, present, staff
        val rowsToday = mutableListOf<TodayStaffRow>()
        val lateList = mutableListOf<RankedStaffCount>()
        val absList = mutableListOf<RankedStaffCount>()

        val presentClasses = setOf(DayStatusClass.PRESENT, DayStatusClass.LATE, DayStatusClass.INCOMPLETE)
        val todayPresentClasses = presentClasses + DayStatusClass.OFF_DUTY

        scoped.forEach { s ->
            val (days, m) = buildStaffMonth(
                s, year, month, todayReal, bl, settings, shiftsMap, transfers, holidays, approvedLeaves,
                ltMap, mainName, attMap, brkMap
            )
            totWork += m.work
            totPresent += m.present
            totLate += m.late
            totAbsent += m.absent
            totLeave += m.leave
            totOtMin += m.otMin
            totOff += (m.off + m.holiday)

            days.forEach { d ->
                if (d.cls in presentClasses) {
                    dailyCounts[d.date] = (dailyCounts[d.date] ?: 0) + 1
                }
                if (d.date == selectedDate && empOn(s, selectedDate)) {
                    rowsToday.add(
                        TodayStaffRow(
                            code = s.code,
                            name = s.name,
                            branch = branchAt(s, transfers, selectedDate),
                            inTime = d.inTime,
                            outTime = d.outTime,
                            status = d.status,
                            cls = d.cls,
                            breakSummary = d.breakSummary
                        )
                    )
                    if (d.cls in todayPresentClasses) tPresent++
                    if (d.cls == DayStatusClass.LATE) tLate++
                    if (d.cls == DayStatusClass.ABSENT) tAbsent++
                    if (d.cls == DayStatusClass.LEAVE_PAID || d.cls == DayStatusClass.LEAVE_UNPAID) tLeave++
                    if (d.cls == DayStatusClass.HOLIDAY_OR_OFF) tOff++
                }
            }

            val spans = brSpan(s, transfers, mFrom, mTo).filter { bl.isEmpty() || it.lowercase() == bl }
            spans.forEach { bb ->
                val mm = if (spans.size > 1 || bl.isNotEmpty()) {
                    buildStaffMonth(
                        s, year, month, todayReal, bb.lowercase(), settings, shiftsMap, transfers,
                        holidays, approvedLeaves, ltMap, mainName, attMap, brkMap
                    ).second
                } else m
                val prev = brStats[bb] ?: Triple(0, 0, 0)
                brStats[bb] = Triple(prev.first + mm.work, prev.second + mm.present, prev.third + 1)
            }

            lateList.add(RankedStaffCount(s.name, m.late))
            absList.add(RankedStaffCount(s.name, m.absent))
        }

        val dailySeries = (1..lastD).mapNotNull { d ->
            val k = "$year-${month.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
            if (k <= todayReal) DailyPresencePoint(d, dailyCounts[k] ?: 0) else null
        }

        return DashboardReport(
            org = settings["OrgName"].orEmpty(),
            addr = settings["Address"].orEmpty(),
            selectedDate = selectedDate,
            todayReal = todayReal,
            staffCount = scoped.size,
            branchCount = branchCount,
            todayPresent = tPresent,
            todayLate = tLate,
            todayAbsent = tAbsent,
            todayLeave = tLeave,
            todayOff = tOff,
            totalWork = totWork,
            totalPresent = totPresent,
            totalLate = totLate,
            totalAbsent = totAbsent,
            totalLeave = totLeave,
            totalOtMin = totOtMin,
            totalOff = totOff,
            dailySeries = dailySeries,
            branchStats = brStats.entries.map { BranchStat(it.key, it.value.first, it.value.second, it.value.third) },
            topLate = lateList.filter { it.count > 0 }.sortedByDescending { it.count }.take(5),
            topAbsent = absList.filter { it.count > 0 }.sortedByDescending { it.count }.take(5),
            todayRows = rowsToday
        )
    }
}
