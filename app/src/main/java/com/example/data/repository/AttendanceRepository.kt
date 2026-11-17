package com.example.data.repository

import com.example.data.db.AppDatabase
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
import com.example.domain.AttendanceCalculator
import com.example.domain.BreakSummary
import com.example.domain.LeaveBalanceRow
import com.example.domain.LeaveTypeAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.sqrt

data class StaffInfoResult(
    val ok: Boolean,
    val message: String = "",
    val code: String = "",
    val name: String = "",
    val designation: String = "",
    val branch: String = "",
    val shift: String = "",
    val inTime: String = "",
    val outTime: String = "",
    val breakCount: Int = 0,
    val breakMin: Int = 0,
    val openBreakStart: String = "",
    val faceEnrolled: Boolean = false,
    val faceRequired: Boolean = false
)

data class PunchResult(
    val ok: Boolean,
    val message: String
)

data class StaffLeavePortalData(
    val ok: Boolean,
    val message: String = "",
    val year: Int = 2026,
    val today: String = "",
    val types: List<LeaveTypeAvailability> = emptyList(),
    val balances: List<LeaveBalanceRow> = emptyList(),
    val applications: List<LeaveEntity> = emptyList()
)

class AttendanceRepository(private val db: AppDatabase) {
    private val settingDao = db.settingDao()
    private val branchDao = db.branchDao()
    private val shiftDao = db.shiftDao()
    private val staffDao = db.staffDao()
    private val holidayDao = db.holidayDao()
    private val leaveTypeDao = db.leaveTypeDao()
    private val leaveDao = db.leaveDao()
    private val transferDao = db.transferDao()
    private val attendanceDao = db.attendanceDao()
    private val breakDao = db.breakDao()

    val observeSettings: Flow<List<SettingEntity>> = settingDao.observeAll()
    val observeBranches: Flow<List<BranchEntity>> = branchDao.observeAll()
    val observeShifts: Flow<List<ShiftEntity>> = shiftDao.observeAll()
    val observeStaff: Flow<List<StaffEntity>> = staffDao.observeAll()
    val observeHolidays: Flow<List<HolidayEntity>> = holidayDao.observeAll()
    val observeLeaveTypes: Flow<List<LeaveTypeEntity>> = leaveTypeDao.observeAll()
    val observeLeaves: Flow<List<LeaveEntity>> = leaveDao.observeAll()
    val observeTransfers: Flow<List<TransferEntity>> = transferDao.observeAll()

    fun observeAttendanceForYear(year: Int): Flow<List<AttendanceEntity>> =
        attendanceDao.observeByPrefix("$year-%")

    fun observeBreaksForYear(year: Int): Flow<List<BreakEntity>> =
        breakDao.observeByPrefix("$year-%")

    suspend fun ensureSeeded() {
        AppDatabase.seedIfEmpty(db)
    }

    suspend fun getSettingsMap(): Map<String, String> =
        AttendanceCalculator.settingsMap(settingDao.getAll())

    suspend fun verifyAdminPassword(password: String): Boolean {
        val cfg = getSettingsMap()
        val expected = cfg["AdminPass"].orEmpty().ifEmpty { "admin123" }
        return password == expected
    }

    suspend fun findStaffByCode(code: String): StaffEntity? {
        val target = AttendanceCalculator.nc(code)
        if (target.isEmpty()) return null
        return staffDao.getAll().find { AttendanceCalculator.nc(it.code) == target }
    }

    suspend fun getStaffInfo(code: String): StaffInfoResult {
        val s = findStaffByCode(code) ?: return StaffInfoResult(ok = false, message = "স্টাফ পাওয়া যায়নি")
        if (!AttendanceCalculator.isActive(s)) {
            return StaffInfoResult(ok = false, message = "এই স্টাফ সক্রিয় নয়")
        }
        val cfg = getSettingsMap()
        val (today, _, _) = AttendanceCalculator.nowInTimezone(cfg["Timezone"] ?: "Asia/Dhaka")
        if (!AttendanceCalculator.empOn(s, today)) {
            return StaffInfoResult(ok = false, message = AttendanceCalculator.empMessage(s, today))
        }
        val att = attendanceDao.getForDay(today, s.code)
        val shift = shiftDao.getByName(s.shift)
        val shiftStr = if (shift != null) "${shift.startTime} - ${shift.endTime}" else "09:30 - 21:30"
        val transfers = transferDao.getAll()
        val branch = AttendanceCalculator.branchAt(s, transfers, today)
        val brks = breakDao.getForDay(today, s.code)
        val brSum: BreakSummary = AttendanceCalculator.breakSummary(brks, today, today)
        return StaffInfoResult(
            ok = true,
            code = s.code,
            name = s.name,
            designation = s.designation,
            branch = branch,
            shift = shiftStr,
            inTime = att?.inTime.orEmpty(),
            outTime = att?.outTime.orEmpty(),
            breakCount = brSum.n,
            breakMin = brSum.min,
            openBreakStart = brSum.open,
            faceEnrolled = s.faceJson.isNotBlank(),
            faceRequired = cfg["FaceRequired"] == "1"
        )
    }

    private fun parseFaceVectors(jsonStr: String): List<List<Double>> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = Json.parseToJsonElement(jsonStr).jsonArray
            if (arr.isEmpty()) return emptyList()
            if (arr.first() is JsonArray) {
                arr.map { row -> row.jsonArray.mapNotNull { it.jsonPrimitive.doubleOrNull } }.filter { it.isNotEmpty() }
            } else {
                listOf(arr.mapNotNull { it.jsonPrimitive.doubleOrNull }).filter { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun punch(
        code: String,
        pin: String,
        type: String, // "IN", "OUT", "BS", "BE"
        lat: Double,
        lng: Double,
        accuracy: Float,
        faceDescriptors: List<List<Double>> = emptyList()
    ): PunchResult {
        val s = findStaffByCode(code)
        val cfg = getSettingsMap()
        if (s == null || s.pin.trim() != pin.trim()) {
            return PunchResult(false, "PIN সঠিক নয়")
        }
        if (!AttendanceCalculator.isActive(s)) {
            return PunchResult(false, "স্টাফ সক্রিয় নয়")
        }
        val (today, timeNow, tsNow) = AttendanceCalculator.nowInTimezone(cfg["Timezone"] ?: "Asia/Dhaka")
        if (!AttendanceCalculator.empOn(s, today)) {
            return PunchResult(false, AttendanceCalculator.empMessage(s, today))
        }

        var source = "Mobile"
        if (s.faceJson.isNotBlank()) {
            val refs = parseFaceVectors(s.faceJson)
            if (faceDescriptors.isEmpty()) {
                return PunchResult(false, "মুখ যাচাই সম্পন্ন হয়নি — ক্যামেরার সামনে সোজা তাকান")
            }
            if (refs.isEmpty()) {
                return PunchResult(false, "মুখের নিবন্ধন নষ্ট — অ্যাডমিনকে বলুন আবার নিবন্ধন করতে")
            }
            var minDist = 9.0
            for (a in faceDescriptors) {
                for (b in refs) {
                    if (a.size == b.size && a.isNotEmpty()) {
                        var sumSq = 0.0
                        for (i in a.indices) {
                            val d = a[i] - b[i]
                            sumSq += d * d
                        }
                        val d = sqrt(sumSq)
                        if (d < minDist) minDist = d
                    }
                }
            }
            val threshold = cfg["FaceThreshold"]?.toDoubleOrNull() ?: 0.55
            if (minDist > threshold) {
                return PunchResult(false, "মুখ মিলছে না (${String.format("%.2f", minDist)})। নিজের মুখ দিয়ে চেষ্টা করুন")
            }
            source = "Mobile+Face"
        } else if (cfg["FaceRequired"] == "1") {
            return PunchResult(false, "আপনার মুখ নিবন্ধন করা নেই — অ্যাডমিনের সাথে যোগাযোগ করুন")
        }

        if (lat == 0.0 && lng == 0.0) {
            return PunchResult(false, "লোকেশন পাওয়া যায়নি")
        }
        val maxAccuracy = cfg["MaxAccuracy"]?.toDoubleOrNull() ?: 100.0
        if (accuracy > maxAccuracy) {
            return PunchResult(
                false,
                "GPS নির্ভুল নয় (${AttendanceCalculator.toBanglaDigits(accuracy.toInt())} মি)। বাইরে খোলা জায়গায় গিয়ে আবার চেষ্টা করুন"
            )
        }

        val transfers = transferDao.getAll()
        val tb = AttendanceCalculator.branchAt(s, transfers, today)
        val branch = branchDao.getByName(tb)
        val bLat = branch?.lat?.toDoubleOrNull()
        val bLng = branch?.lng?.toDoubleOrNull()
        if (branch == null || bLat == null || bLng == null) {
            return PunchResult(false, "এই শাখার লোকেশন সেট করা নেই")
        }
        val radius = branch.radius.toIntOrNull() ?: 150
        val dist = AttendanceCalculator.distanceMeters(lat, lng, bLat, bLng)
        if (dist > radius) {
            return PunchResult(false, "আপনি শাখা থেকে ${AttendanceCalculator.toBanglaDigits(dist)} মিটার দূরে আছেন")
        }

        val existing = attendanceDao.getForDay(today, s.code)
        if (type == "BS" || type == "BE") {
            if (existing == null || existing.inTime.isBlank()) {
                return PunchResult(false, "আগে IN দিন")
            }
            if (existing.outTime.isNotBlank()) {
                return PunchResult(false, "আজ আপনি OUT দিয়েছেন (${existing.outTime}) — এখন ব্রেক নেওয়া যাবে না")
            }
            val dayBreaks = breakDao.getForDay(today, s.code)
            val openBreak = dayBreaks.find { it.endTime.isBlank() }
            return if (type == "BS") {
                if (openBreak != null) {
                    PunchResult(false, "আপনার ব্রেক চলছে (${openBreak.startTime} থেকে)")
                } else {
                    breakDao.upsert(
                        BreakEntity(
                            date = today,
                            code = s.code,
                            startTime = timeNow,
                            endTime = "",
                            source = source,
                            upd = tsNow
                        )
                    )
                    PunchResult(true, "ব্রেক শুরু হয়েছে: ${AttendanceCalculator.formatTime12h(timeNow)}")
                }
            } else {
                if (openBreak == null) {
                    PunchResult(false, "এখন কোনো ব্রেক চলছে না")
                } else {
                    val sMin = AttendanceCalculator.parseMinutes(openBreak.startTime) ?: 0
                    val eMin = AttendanceCalculator.parseMinutes(timeNow) ?: 0
                    val one = kotlin.math.max(0, eMin - sMin)
                    val prevSum = AttendanceCalculator.breakSummary(dayBreaks.filter { it.id != openBreak.id }, today, today).min
                    val tot = prevSum + one
                    breakDao.update(openBreak.copy(endTime = timeNow, upd = tsNow))
                    PunchResult(
                        true,
                        "ব্রেক শেষ: ${AttendanceCalculator.formatTime12h(timeNow)} (এবার ${AttendanceCalculator.toBanglaDigits(one)} মিনিট, আজ মোট ${AttendanceCalculator.toBanglaDigits(tot)} মিনিট)"
                    )
                }
            }
        }

        return if (type == "IN") {
            if (existing != null && existing.inTime.isNotBlank()) {
                PunchResult(false, "আজ আপনি ${AttendanceCalculator.formatTime12h(existing.inTime)}-এ IN দিয়েছেন")
            } else {
                attendanceDao.upsert(
                    (existing ?: AttendanceEntity(date = today, code = s.code)).copy(
                        name = s.name,
                        branch = tb,
                        inTime = timeNow,
                        inDist = dist.toString(),
                        source = source,
                        upd = tsNow
                    )
                )
                PunchResult(true, "IN সফল হয়েছে: ${AttendanceCalculator.formatTime12h(timeNow)}")
            }
        } else {
            if (existing == null || existing.inTime.isBlank()) {
                PunchResult(false, "আগে IN দিন")
            } else {
                val openBreak = breakDao.getForDay(today, s.code).find { it.endTime.isBlank() }
                if (openBreak != null) {
                    PunchResult(false, "আপনার ব্রেক চলছে (${openBreak.startTime} থেকে) — আগে \"ব্রেক শেষ\" দিন, তারপর OUT")
                } else {
                    attendanceDao.upsert(
                        existing.copy(
                            outTime = timeNow,
                            outDist = dist.toString(),
                            upd = tsNow
                        )
                    )
                    PunchResult(true, "OUT সফল হয়েছে: ${AttendanceCalculator.formatTime12h(timeNow)}")
                }
            }
        }
    }

    private suspend fun authStaffForLeave(code: String, pin: String): Pair<StaffEntity?, String?> {
        val s = findStaffByCode(code)
        if (s == null || s.pin.trim() != pin.trim()) return null to "PIN সঠিক নয়"
        if (!AttendanceCalculator.isActive(s)) return null to "স্টাফ সক্রিয় নয়"
        val cfg = getSettingsMap()
        val (today, _, _) = AttendanceCalculator.nowInTimezone(cfg["Timezone"] ?: "Asia/Dhaka")
        if (!AttendanceCalculator.empOn(s, today)) return null to AttendanceCalculator.empMessage(s, today)
        return s to null
    }

    suspend fun getStaffLeavePortal(code: String, pin: String): StaffLeavePortalData {
        val (s, err) = authStaffForLeave(code, pin)
        if (s == null) return StaffLeavePortalData(ok = false, message = err ?: "ত্রুটি")
        val cfg = getSettingsMap()
        val (today, _, _) = AttendanceCalculator.nowInTimezone(cfg["Timezone"] ?: "Asia/Dhaka")
        val year = today.take(4).toIntOrNull() ?: 2026
        val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(leaveTypeDao.getAll())
        val allLeaves = leaveDao.getAll()
        val transfers = transferDao.getAll()
        val hols = AttendanceCalculator.holMap(holidayDao.getAll(), s, transfers)
        val offs = AttendanceCalculator.weeklyOffSet(s, cfg)
        val calc = AttendanceCalculator.leaveCalc(s, allLeaves, ltMap, mainName, hols, offs, year, false)
        val calcP = AttendanceCalculator.leaveCalc(s, allLeaves, ltMap, mainName, hols, offs, year, true)
        val joinDate = AttendanceCalculator.validDateOrEmpty(s.joinDate)

        val types = ltMap.values.filter { !it.holiday }.map { t0 ->
            val t = AttendanceCalculator.effType(t0, mainName, s)
            val (q, fromStr) = AttendanceCalculator.quotaOf(t, joinDate, year)
            val com = calcP.byType[t.name]?.size ?: 0
            val full = t.paid && q != null && (q - com) <= 0
            val why = if (!full) "" else if (q == 0) (if (fromStr.isNotEmpty()) "probation" else "none") else "exhausted"
            LeaveTypeAvailability(name = t.name, avail = !full, why = why)
        }

        val apps = allLeaves
            .filter { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(s.code) }
            .sortedByDescending { it.id }
            .take(15)

        return StaffLeavePortalData(
            ok = true,
            year = year,
            today = today,
            types = types,
            balances = AttendanceCalculator.balOf(s, ltMap, mainName, calc, year),
            applications = apps
        )
    }

    suspend fun applyStaffLeave(
        code: String,
        pin: String,
        type: String,
        from: String,
        to: String,
        reason: String
    ): PunchResult {
        val (s, err) = authStaffForLeave(code, pin)
        if (s == null) return PunchResult(false, err ?: "ত্রুটি")
        return leaveAddInternal(s, type, from, to, reason, "Pending", byStaff = true)
    }

    suspend fun cancelStaffLeave(code: String, pin: String, leaveId: Int): PunchResult {
        val (s, err) = authStaffForLeave(code, pin)
        if (s == null) return PunchResult(false, err ?: "ত্রুটি")
        val l = leaveDao.getById(leaveId)
        if (l == null || AttendanceCalculator.nc(l.code) != AttendanceCalculator.nc(s.code) || l.status.trim() != "Pending") {
            return PunchResult(false, "এই আবেদন বাতিল করা যাবে না")
        }
        leaveDao.deleteById(leaveId)
        return PunchResult(true, "আবেদন বাতিল হয়েছে")
    }

    suspend fun adminAddLeave(
        code: String,
        type: String,
        from: String,
        to: String,
        reason: String
    ): PunchResult {
        val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
        return leaveAddInternal(s, type, from, to, reason, "Approved", byStaff = false)
    }

    private suspend fun leaveAddInternal(
        s: StaffEntity,
        type: String,
        fromRaw: String,
        toRaw: String,
        reason: String,
        status: String,
        byStaff: Boolean
    ): PunchResult {
        val from = fromRaw.trim()
        val to = toRaw.trim().ifEmpty { from }
        if (!AttendanceCalculator.isValidDate(from) || !AttendanceCalculator.isValidDate(to)) {
            return PunchResult(false, "তারিখ দিন")
        }
        if (to < from) {
            return PunchResult(false, "\"পর্যন্ত\" তারিখ \"থেকে\" তারিখের আগে হতে পারে না")
        }
        val range = AttendanceCalculator.dateRange(from, to, 405)
        if (range.size >= 400) {
            return PunchResult(false, "এত লম্বা সময়ের ছুটি দেওয়া যায় না")
        }
        val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(leaveTypeDao.getAll())
        val tBase = ltMap[type.trim()]
        if (tBase == null || (byStaff && tBase.holiday)) {
            return PunchResult(false, "ছুটির ধরন বেছে নিন")
        }
        val allLeaves = leaveDao.getAll()
        val overlap = allLeaves.filter {
            AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(s.code) &&
                it.status.trim().lowercase() != "rejected" &&
                it.dateFrom <= to && it.dateTo >= from
        }
        if (overlap.isNotEmpty()) {
            return PunchResult(
                false,
                "এই সময়ে আগেই আবেদন/ছুটি আছে (${overlap.first().dateFrom} থেকে ${overlap.first().dateTo})"
            )
        }
        val cfg = getSettingsMap()
        val (today, _, _) = AttendanceCalculator.nowInTimezone(cfg["Timezone"] ?: "Asia/Dhaka")
        val year = from.take(4).toIntOrNull() ?: today.take(4).toInt()
        val transfers = transferDao.getAll()
        val hols = AttendanceCalculator.holMap(holidayDao.getAll(), s, transfers)
        val offs = AttendanceCalculator.weeklyOffSet(s, cfg)
        val tEff = AttendanceCalculator.effType(tBase, mainName, s)
        val joinDate = AttendanceCalculator.validDateOrEmpty(s.joinDate)
        val (qoQ, qoFrom) = AttendanceCalculator.quotaOf(tEff, joinDate, year)

        if (byStaff && tEff.paid && tEff.days != null) {
            val calcP = AttendanceCalculator.leaveCalc(s, allLeaves, ltMap, mainName, hols, offs, year, true)
            val com = calcP.byType[tEff.name]?.size ?: 0
            if ((qoQ ?: 0) - com <= 0) {
                val msg = when {
                    qoQ == 0 && qoFrom.isEmpty() ->
                        "আপনার \"${tEff.name}\" এর কোটা নেই (চুক্তি অনুযায়ী)। অন্য ধরন বেছে নিন বা অ্যাডমিনের সঙ্গে কথা বলুন"
                    qoQ == 0 && qoFrom.isNotEmpty() ->
                        "শিক্ষানবিস কাল চলছে: \"${tEff.name}\" এর কোটা ${qoFrom.take(7)} থেকে শুরু, তাই এখন এই ধরনে আবেদন করা যাবে না"
                    else ->
                        "\"${tEff.name}\" এর কোটা শেষ (বছরে ${AttendanceCalculator.toBanglaDigits(qoQ)} দিন, অপেক্ষমাণ আবেদনসহ)। এই ধরনে আর আবেদন করা যাবে না"
                }
                return PunchResult(false, msg)
            }
        }

        leaveDao.upsert(
            LeaveEntity(
                code = s.code,
                dateFrom = from,
                dateTo = to,
                typ = tEff.name,
                note = "",
                status = status,
                appliedOn = today,
                reason = reason.trim().take(300)
            )
        )

        var unpaidCount = 0
        if (byStaff && tEff.paid && qoQ != null) {
            val updatedLeaves = leaveDao.getAll()
            val calcAfter = AttendanceCalculator.leaveCalc(s, updatedLeaves, ltMap, mainName, hols, offs, year, true)
            val leaveDate = AttendanceCalculator.validDateOrEmpty(s.leaveDate)
            val req = range.filter { d ->
                d.startsWith("$year-") && !hols.containsKey(d) && !offs.contains(AttendanceCalculator.dayNameEn(d)) &&
                    !(joinDate.isNotEmpty() && d < joinDate) && !(leaveDate.isNotEmpty() && d > leaveDate)
            }
            val byList = calcAfter.byType[tEff.name].orEmpty()
            val late = if (byList.size > qoQ) byList.drop(qoQ) else emptyList()
            val pr = calcAfter.probByType[tEff.name].orEmpty()
            unpaidCount = req.count { late.contains(it) || pr.contains(it) }
        }

        val msg = if (byStaff) {
            "আবেদন জমা হয়েছে — অনুমোদনের অপেক্ষায়" +
                if (unpaidCount > 0) "। এর মধ্যে ${AttendanceCalculator.toBanglaDigits(unpaidCount)} দিন কোটার বাইরে বা শিক্ষানবিস কালে পড়ায় বিনা বেতনে গণ্য হবে" else ""
        } else {
            "ছুটি যোগ হয়েছে"
        }
        return PunchResult(true, msg)
    }

    suspend fun decideLeave(id: Int, status: String, note: String) {
        leaveDao.updateDecision(id, status, note.take(200))
    }

    suspend fun deleteLeave(id: Int) = leaveDao.deleteById(id)

    suspend fun addTransfer(code: String, effDateRaw: String, newBranchRaw: String): PunchResult {
        val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
        val eff = effDateRaw.trim()
        val nb = newBranchRaw.trim()
        if (!AttendanceCalculator.isValidDate(eff)) return PunchResult(false, "তারিখ দিন")
        if (nb.isEmpty()) return PunchResult(false, "নতুন শাখা বাছুন")
        if (branchDao.getByName(nb) == null) return PunchResult(false, "এই নামে কোনো শাখা নেই")

        val mine = transferDao.getAll()
            .filter { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(s.code) }
            .sortedBy { it.effDate }
        val last = mine.lastOrNull()
        if (last != null && eff <= last.effDate) {
            return PunchResult(false, "এই কর্মীর সর্বশেষ বদলি ${last.effDate} তারিখে। নতুন বদলির তারিখ তার পরে হতে হবে")
        }
        val old = last?.toBranch ?: s.branch
        if (old.trim().equals(nb, ignoreCase = true)) {
            return PunchResult(false, "কর্মী আগে থেকেই এই শাখায় আছেন")
        }
        transferDao.insert(TransferEntity(code = s.code, effDate = eff, fromBranch = old, toBranch = nb))
        staffDao.updateBranchByCode(s.code, nb)
        return PunchResult(true, "বদলি যোগ হয়েছে")
    }

    suspend fun deleteTransfer(id: Int): PunchResult {
        val all = transferDao.getAll()
        val t = all.find { it.id == id } ?: return PunchResult(false, "বদলি পাওয়া যায়নি")
        if (all.any { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(t.code) && it.effDate > t.effDate }) {
            return PunchResult(false, "আগে এই কর্মীর পরের বদলিটা মুছুন")
        }
        transferDao.deleteById(t.id)
        staffDao.updateBranchByCode(t.code, t.fromBranch)
        return PunchResult(true, "মুছে ফেলা হয়েছে")
    }

    suspend fun getManualPunch(code: String, date: String): AttendanceEntity? {
        val s = findStaffByCode(code) ?: return null
        if (!AttendanceCalculator.isValidDate(date)) return null
        return attendanceDao.getForDay(date, s.code)
    }

    suspend fun saveManualPunch(
        code: String,
        date: String,
        inTimeRaw: String,
        outTimeRaw: String,
        noteRaw: String
    ): PunchResult {
        val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
        if (!AttendanceCalculator.isValidDate(date)) return PunchResult(false, "তারিখ দিন")
        if (!AttendanceCalculator.empOn(s, date)) {
            return PunchResult(false, "এই তারিখে কর্মী কর্মরত নন (যোগদান বা শেষ কর্মদিবস দেখুন)")
        }
        val tin = inTimeRaw.trim()
        val tout = outTimeRaw.trim()
        if (tin.isEmpty() && tout.isEmpty()) {
            return PunchResult(false, "IN বা OUT এর অন্তত একটি সময় দিন")
        }
        if ((tin.isNotEmpty() && !AttendanceCalculator.isValidTime(tin)) ||
            (tout.isNotEmpty() && !AttendanceCalculator.isValidTime(tout))
        ) {
            return PunchResult(false, "সময় HH:MM ফরম্যাটে দিন")
        }
        val existing = attendanceDao.getForDay(date, s.code)
        val branch = AttendanceCalculator.branchAt(s, transferDao.getAll(), date)
        val ts = java.time.Instant.now().toString()
        attendanceDao.upsert(
            (existing ?: AttendanceEntity(date = date, code = s.code)).copy(
                name = s.name,
                branch = branch,
                inTime = tin.ifEmpty { existing?.inTime.orEmpty() },
                outTime = tout.ifEmpty { existing?.outTime.orEmpty() },
                source = "Manual",
                note = noteRaw.trim().ifEmpty { existing?.note.orEmpty() },
                upd = ts
            )
        )
        return PunchResult(true, "সংরক্ষিত হয়েছে")
    }

    suspend fun deleteManualPunch(code: String, date: String): PunchResult {
        val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
        if (!AttendanceCalculator.isValidDate(date)) return PunchResult(false, "তারিখ দিন")
        attendanceDao.deleteForDay(date, s.code)
        breakDao.deleteForDay(date, s.code)
        return PunchResult(true, "হাজিরা মুছে ফেলা হয়েছে")
    }

    suspend fun getBreaksForDay(code: String, date: String): List<BreakEntity> {
        val s = findStaffByCode(code) ?: return emptyList()
        if (!AttendanceCalculator.isValidDate(date)) return emptyList()
        return breakDao.getForDay(date, s.code)
    }

    suspend fun getBreaksInRange(from: String, to: String): List<BreakEntity> =
        breakDao.getInRange(from, to)

    suspend fun saveBreak(id: Int, code: String, date: String, startRaw: String, endRaw: String): PunchResult {
        val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
        val d = date.trim()
        val st = startRaw.trim()
        val en = endRaw.trim()
        if (!AttendanceCalculator.isValidDate(d)) return PunchResult(false, "তারিখ দিন")
        if (!AttendanceCalculator.empOn(s, d)) return PunchResult(false, "এই তারিখে কর্মী কর্মরত নন")
        if (!AttendanceCalculator.isValidTime(st)) return PunchResult(false, "ব্রেক শুরুর সময় দিন")
        if (en.isNotEmpty() && !AttendanceCalculator.isValidTime(en)) {
            return PunchResult(false, "সময় HH:MM ফরম্যাটে দিন")
        }
        val a1 = AttendanceCalculator.parseMinutes(st)!!
        val b1 = if (en.isNotEmpty()) AttendanceCalculator.parseMinutes(en)!! else a1
        if (en.isNotEmpty() && b1 < a1) {
            return PunchResult(false, "ব্রেক শেষের সময় শুরুর আগে হতে পারে না")
        }
        val others = breakDao.getForDay(d, s.code).filter { it.id != id }
        if (en.isEmpty() && others.any { it.endTime.isBlank() }) {
            return PunchResult(false, "এই দিনে আগে থেকেই একটি চলমান ব্রেক আছে — সেটার শেষ সময় দিন")
        }
        for (b in others) {
            val a2 = AttendanceCalculator.parseMinutes(b.startTime) ?: continue
            val b2 = if (b.endTime.isBlank()) a2 else (AttendanceCalculator.parseMinutes(b.endTime) ?: a2)
            if ((a1 < b2 && a2 < b1) || a1 == a2) {
                return PunchResult(
                    false,
                    "অন্য ব্রেকের সঙ্গে সময় মিলে যাচ্ছে (${b.startTime}${if (b.endTime.isNotBlank()) " - ${b.endTime}" else ""})"
                )
            }
        }
        val ts = java.time.Instant.now().toString()
        breakDao.upsert(
            BreakEntity(
                id = id,
                date = d,
                code = s.code,
                startTime = st,
                endTime = en,
                source = "Manual",
                upd = ts
            )
        )
        return PunchResult(true, "ব্রেক সংরক্ষিত হয়েছে")
    }

    suspend fun deleteBreak(id: Int) = breakDao.deleteById(id)

    suspend fun saveFace(code: String, vectors: List<List<Double>>): PunchResult {
        val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
        if (vectors.isEmpty()) return PunchResult(false, "মুখের তথ্য সঠিক নয়")
        val json = vectors.joinToString(prefix = "[", postfix = "]") { row ->
            row.joinToString(prefix = "[", postfix = "]") { String.format("%.5f", it) }
        }
        staffDao.updateFace(s.id, json)
        return PunchResult(true, "মুখ নিবন্ধিত হয়েছে")
    }

    suspend fun deleteFace(code: String): PunchResult {
        val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
        staffDao.updateFace(s.id, "")
        return PunchResult(true, "মুখ-নিবন্ধন মুছে ফেলা হয়েছে")
    }

    // CRUD for Setup Tables
    suspend fun saveStaff(item: StaffEntity) = staffDao.upsert(item)
    suspend fun deleteStaff(id: Int) = staffDao.deleteById(id)

    suspend fun saveLeaveType(item: LeaveTypeEntity) = leaveTypeDao.upsert(item)
    suspend fun deleteLeaveType(id: Int) = leaveTypeDao.deleteById(id)

    suspend fun saveHoliday(item: HolidayEntity) = holidayDao.upsert(item)
    suspend fun deleteHoliday(id: Int) = holidayDao.deleteById(id)

    suspend fun saveShift(item: ShiftEntity) = shiftDao.upsert(item)
    suspend fun deleteShift(id: Int) = shiftDao.deleteById(id)

    suspend fun saveBranch(item: BranchEntity) = branchDao.upsert(item)
    suspend fun deleteBranch(id: Int) = branchDao.deleteById(id)

    suspend fun saveSetting(item: SettingEntity) = settingDao.upsert(item)
}
