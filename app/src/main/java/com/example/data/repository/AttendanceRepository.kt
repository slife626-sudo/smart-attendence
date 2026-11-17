package com.example.data.repository

import com.example.data.db.AttendanceEntity
import com.example.data.db.BranchEntity
import com.example.data.db.BreakEntity
import com.example.data.db.HolidayEntity
import com.example.data.db.LeaveEntity
import com.example.data.db.LeaveTypeEntity
import com.example.data.db.OrgWorkspace
import com.example.data.db.SeedData
import com.example.data.db.SettingEntity
import com.example.data.db.ShiftEntity
import com.example.data.db.StaffEntity
import com.example.data.db.TransferEntity
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.AttendanceCalculator
import com.example.domain.BreakSummary
import com.example.domain.LeaveBalanceRow
import com.example.domain.LeaveTypeAvailability
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import kotlin.math.max
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

class AttendanceRepository(
    private val db: FirebaseFirestore,
    val requestedOrgId: String,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        const val DEFAULT_ADMIN_ORG_ID = "wahidiya_main"
        const val BOOTSTRAPPED_ADMIN_EMAIL = "slife626@gmail.com"
        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")
        private val seedMutex = Mutex()
    }

    private var cachedMemberUids: List<String>? = null

    fun currentUid(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun requireUserId(): String {
        return currentUid()
            ?: throw IllegalStateException("Google অ্যাকাউন্ট দিয়ে সাইন ইন করা আবশ্যক।")
    }

    fun isBootstrappedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    val effectiveOrgId: String
        get() {
            val cleaned = requestedOrgId.trim()
            if (cleaned.isNotEmpty() && VALID_ID_REGEX.matches(cleaned)) return cleaned
            return if (isBootstrappedAdmin()) DEFAULT_ADMIN_ORG_ID else (currentUid() ?: DEFAULT_ADMIN_ORG_ID)
        }

    private fun orgRef(): DocumentReference = db.collection("orgs").document(effectiveOrgId)

    private fun subCol(name: String) = orgRef().collection(name)

    private fun scopedQuery(sub: String): Query {
        val col = subCol(sub)
        val uid = requireUserId()
        return if (isBootstrappedAdmin() || effectiveOrgId == uid) {
            col
        } else {
            col.whereArrayContains("memberUids", uid)
        }
    }

    private suspend fun getWorkspaceMemberUids(): List<String> {
        val uid = requireUserId()
        val cached = cachedMemberUids
        if (cached != null) {
            return if (uid in cached) cached else (cached + uid).distinct().take(50)
        }
        return try {
            val snap = orgRef().get().await()
            val ws = snap.toObject(OrgWorkspace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val list = ((ws?.memberUids ?: emptyList()) + uid).filter { it.isNotBlank() }.distinct().take(50)
            cachedMemberUids = list
            list
        } catch (_: Exception) {
            listOf(uid)
        }
    }

    private inline fun <reified T> observeSubcollection(sub: String): Flow<List<T>> = flow {
        val uid = requireUserId()
        val path = "orgs/$effectiveOrgId/$sub"
        emitAll(
            scopedQuery(sub)
                .snapshots()
                .map { snap -> snap.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    val observeWorkspace: Flow<OrgWorkspace?> = flow {
        val uid = requireUserId()
        val path = "orgs/$effectiveOrgId"
        emitAll(
            orgRef()
                .snapshots()
                .map { snap ->
                    val ws = snap.toObject(OrgWorkspace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (ws != null) cachedMemberUids = ws.memberUids
                    ws
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    val observeSettings: Flow<List<SettingEntity>> =
        observeSubcollection<SettingEntity>("settings").map { list -> list.sortedBy { it.key } }

    val observeBranches: Flow<List<BranchEntity>> =
        observeSubcollection<BranchEntity>("branches").map { list -> list.sortedBy { it.branch } }

    val observeShifts: Flow<List<ShiftEntity>> =
        observeSubcollection<ShiftEntity>("shifts").map { list -> list.sortedBy { it.shift } }

    val observeStaff: Flow<List<StaffEntity>> =
        observeSubcollection<StaffEntity>("staff").map { list -> list.sortedBy { it.code } }

    val observeHolidays: Flow<List<HolidayEntity>> =
        observeSubcollection<HolidayEntity>("holidays").map { list -> list.sortedBy { it.dateFrom } }

    val observeLeaveTypes: Flow<List<LeaveTypeEntity>> =
        observeSubcollection<LeaveTypeEntity>("leave_types").map { list -> list.sortedBy { it.ltype } }

    val observeLeaves: Flow<List<LeaveEntity>> =
        observeSubcollection<LeaveEntity>("leaves").map { list -> list.sortedByDescending { it.appliedOn } }

    val observeTransfers: Flow<List<TransferEntity>> =
        observeSubcollection<TransferEntity>("transfers").map { list -> list.sortedByDescending { it.effDate } }

    fun observeAttendanceForYear(year: Int): Flow<List<AttendanceEntity>> =
        observeSubcollection<AttendanceEntity>("attendance").map { list ->
            val prefix = "$year-"
            list.filter { it.date.startsWith(prefix) }.sortedBy { it.date }
        }

    fun observeBreaksForYear(year: Int): Flow<List<BreakEntity>> =
        observeSubcollection<BreakEntity>("breaks").map { list ->
            val prefix = "$year-"
            list.filter { it.date.startsWith(prefix) }.sortedBy { it.date }
        }

    private suspend inline fun <reified T> fetchAll(sub: String): List<T> {
        val path = "orgs/$effectiveOrgId/$sub"
        return try {
            scopedQuery(sub).get().await()
                .toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    private fun hashShort(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun safeIdPart(raw: String): String {
        val cleaned = raw.trim()
        return if (Regex("^[a-zA-Z0-9_\\-]{1,48}$").matches(cleaned)) cleaned else hashShort(cleaned)
    }

    private fun settingDocId(key: String) = "setting_${safeIdPart(key)}"
    private fun branchDocId(branch: String) = "br_${hashShort(branch.trim())}"
    private fun shiftDocId(shift: String) = "sh_${hashShort(shift.trim())}"
    private fun staffDocId(code: String) = "staff_${safeIdPart(AttendanceCalculator.nc(code))}"
    private fun holidayDocId(dateFrom: String, dateTo: String, name: String) =
        "hol_${hashShort("${dateFrom.trim()}|${dateTo.trim()}|${name.trim()}")}"
    private fun leaveTypeDocId(ltype: String) = "lt_${hashShort(ltype.trim())}"
    private fun transferDocId(code: String, effDate: String) =
        "trf_${safeIdPart(AttendanceCalculator.nc(code))}_${safeIdPart(effDate)}"
    private fun attendanceDocId(date: String, code: String) =
        "att_${safeIdPart(date)}_${safeIdPart(AttendanceCalculator.nc(code))}"

    suspend fun ensureSeeded(): Result<Unit> = seedMutex.withLock {
        val uid = currentUid() ?: return Result.success(Unit)
        if (!isBootstrappedAdmin() && effectiveOrgId != uid) {
            return Result.success(Unit)
        }
        val orgPath = "orgs/$effectiveOrgId"
        return try {
            val orgSnap = orgRef().get().await()
            val staffSnap = subCol("staff").limit(1).get().await()
            if (orgSnap.exists() && !staffSnap.isEmpty) {
                return Result.success(Unit)
            }
            val members = if (orgSnap.exists()) {
                val ws = orgSnap.toObject(OrgWorkspace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ((ws?.memberUids ?: emptyList()) + uid).distinct().take(50)
            } else {
                listOf(uid)
            }
            cachedMemberUids = members

            val ops = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            if (!orgSnap.exists()) {
                ops.add(
                    orgRef() to mapOf(
                        "ownerId" to uid,
                        "memberUids" to members,
                        "orgName" to "ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (staffSnap.isEmpty) {
                SeedData.settings.forEach { s ->
                    ops.add(subCol("settings").document(settingDocId(s.key)) to settingCreateMap(s, uid, members))
                }
                SeedData.branches.forEach { b ->
                    ops.add(subCol("branches").document(branchDocId(b.branch)) to branchCreateMap(b, uid, members))
                }
                SeedData.shifts.forEach { sh ->
                    ops.add(subCol("shifts").document(shiftDocId(sh.shift)) to shiftCreateMap(sh, uid, members))
                }
                SeedData.staff.forEach { st ->
                    ops.add(subCol("staff").document(staffDocId(st.code)) to staffCreateMap(st, uid, members))
                }
                SeedData.leaveTypes.forEach { lt ->
                    ops.add(subCol("leave_types").document(leaveTypeDocId(lt.ltype)) to leaveTypeCreateMap(lt, uid, members))
                }
                SeedData.holidays.forEach { h ->
                    ops.add(subCol("holidays").document(holidayDocId(h.dateFrom, h.dateTo, h.name)) to holidayCreateMap(h, uid, members))
                }
                SeedData.leaves.forEachIndexed { idx, lv ->
                    val id = "lv_seed_${idx}_${hashShort("${lv.code}|${lv.dateFrom}|${lv.typ}")}"
                    ops.add(subCol("leaves").document(id) to leaveCreateMap(lv, uid, members))
                }
                SeedData.initialAttendance.forEach { att ->
                    ops.add(subCol("attendance").document(attendanceDocId(att.date, att.code)) to attendanceCreateMap(att, uid, members))
                }
            }

            commitSetChunks(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, orgPath)
            Result.failure(e)
        }
    }

    private suspend fun commitSetChunks(ops: List<Pair<DocumentReference, Map<String, Any>>>) {
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, map) -> batch.set(ref, map) }
            batch.commit().await()
        }
    }

    suspend fun getSettingsMap(): Map<String, String> =
        AttendanceCalculator.settingsMap(fetchAll("settings"))

    suspend fun verifyAdminPassword(password: String): Boolean {
        val cfg = getSettingsMap()
        val expected = cfg["AdminPass"].orEmpty().ifEmpty { "admin123" }
        return password == expected
    }

    suspend fun findStaffByCode(code: String): StaffEntity? {
        val target = AttendanceCalculator.nc(code)
        if (target.isEmpty()) return null
        return fetchAll<StaffEntity>("staff").find { AttendanceCalculator.nc(it.code) == target }
    }

    suspend fun getStaffInfo(code: String): StaffInfoResult {
        if (currentUid() == null) return StaffInfoResult(ok = false, message = "সাইন ইন করুন")
        val s = findStaffByCode(code) ?: return StaffInfoResult(ok = false, message = "স্টাফ পাওয়া যায়নি")
        if (!AttendanceCalculator.isActive(s)) {
            return StaffInfoResult(ok = false, message = "এই স্টাফ সক্রিয় নয়")
        }
        val cfg = getSettingsMap()
        val (today, _, _) = AttendanceCalculator.nowInTimezone(cfg["Timezone"] ?: "Asia/Dhaka")
        if (!AttendanceCalculator.empOn(s, today)) {
            return StaffInfoResult(ok = false, message = AttendanceCalculator.empMessage(s, today))
        }
        val att = getAttendanceForDay(today, s.code)
        val shift = fetchAll<ShiftEntity>("shifts").find { it.shift == s.shift }
        val shiftStr = if (shift != null) "${shift.startTime} - ${shift.endTime}" else "09:30 - 21:30"
        val transfers = fetchAll<TransferEntity>("transfers")
        val branch = AttendanceCalculator.branchAt(s, transfers, today)
        val brks = getBreaksForDay(s.code, today)
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

    private suspend fun getAttendanceForDay(date: String, code: String): AttendanceEntity? {
        val docId = attendanceDocId(date, code)
        val docPath = "orgs/$effectiveOrgId/attendance/$docId"
        return try {
            val snap = subCol("attendance").document(docId).get().await()
            if (snap.exists()) {
                snap.toObject(AttendanceEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                val nc = AttendanceCalculator.nc(code)
                fetchAll<AttendanceEntity>("attendance").find {
                    it.date == date && AttendanceCalculator.nc(it.code) == nc
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            null
        }
    }

    suspend fun getBreaksForDay(code: String, date: String): List<BreakEntity> {
        val target = AttendanceCalculator.nc(code)
        if (target.isEmpty() || !AttendanceCalculator.isValidDate(date)) return emptyList()
        return fetchAll<BreakEntity>("breaks")
            .filter { it.date == date && AttendanceCalculator.nc(it.code) == target }
            .sortedBy { it.startTime }
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
        type: String,
        lat: Double,
        lng: Double,
        accuracy: Float,
        faceDescriptors: List<List<Double>> = emptyList()
    ): PunchResult {
        return try {
            val uid = requireUserId()
            val members = getWorkspaceMemberUids()
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

            val transfers = fetchAll<TransferEntity>("transfers")
            val tb = AttendanceCalculator.branchAt(s, transfers, today)
            val branch = fetchAll<BranchEntity>("branches").find { it.branch.equals(tb, ignoreCase = true) }
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

            val existing = getAttendanceForDay(today, s.code)
            if (type == "BS" || type == "BE") {
                if (existing == null || existing.inTime.isBlank()) {
                    return PunchResult(false, "আগে IN দিন")
                }
                if (existing.outTime.isNotBlank()) {
                    return PunchResult(false, "আজ আপনি OUT দিয়েছেন (${existing.outTime}) — এখন ব্রেক নেওয়া যাবে না")
                }
                val dayBreaks = getBreaksForDay(s.code, today)
                val openBreak = dayBreaks.find { it.endTime.isBlank() }
                return if (type == "BS") {
                    if (openBreak != null) {
                        PunchResult(false, "আপনার ব্রেক চলছে (${openBreak.startTime} থেকে)")
                    } else {
                        val ref = subCol("breaks").document()
                        val item = BreakEntity(
                            date = today,
                            code = s.code,
                            startTime = timeNow,
                            endTime = "",
                            source = source,
                            upd = tsNow
                        )
                        ref.set(breakCreateMap(item, uid, members)).await()
                        PunchResult(true, "ব্রেক শুরু হয়েছে: ${AttendanceCalculator.formatTime12h(timeNow)}")
                    }
                } else {
                    if (openBreak == null) {
                        PunchResult(false, "এখন কোনো ব্রেক চলছে না")
                    } else {
                        val sMin = AttendanceCalculator.parseMinutes(openBreak.startTime) ?: 0
                        val eMin = AttendanceCalculator.parseMinutes(timeNow) ?: 0
                        val one = max(0, eMin - sMin)
                        val prevSum = AttendanceCalculator.breakSummary(
                            dayBreaks.filter { it.docId != openBreak.docId },
                            today,
                            today
                        ).min
                        val tot = prevSum + one
                        subCol("breaks").document(openBreak.docId).update(
                            mapOf(
                                "endTime" to timeNow.take(20),
                                "upd" to tsNow.take(60),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        PunchResult(
                            true,
                            "ব্রেক শেষ: ${AttendanceCalculator.formatTime12h(timeNow)} (এবার ${AttendanceCalculator.toBanglaDigits(one)} মিনিট, আজ মোট ${AttendanceCalculator.toBanglaDigits(tot)} মিনিট)"
                        )
                    }
                }
            }

            if (type == "IN") {
                if (existing != null && existing.inTime.isNotBlank()) {
                    PunchResult(false, "আজ আপনি ${AttendanceCalculator.formatTime12h(existing.inTime)}-এ IN দিয়েছেন")
                } else {
                    val item = (existing ?: AttendanceEntity(date = today, code = s.code)).copy(
                        name = s.name,
                        branch = tb,
                        inTime = timeNow,
                        inDist = dist.toString(),
                        source = source,
                        upd = tsNow
                    )
                    upsertAttendanceDoc(item, uid, members)
                    PunchResult(true, "IN সফল হয়েছে: ${AttendanceCalculator.formatTime12h(timeNow)}")
                }
            } else {
                if (existing == null || existing.inTime.isBlank()) {
                    PunchResult(false, "আগে IN দিন")
                } else {
                    val openBreak = getBreaksForDay(s.code, today).find { it.endTime.isBlank() }
                    if (openBreak != null) {
                        PunchResult(false, "আপনার ব্রেক চলছে (${openBreak.startTime} থেকে) — আগে \"ব্রেক শেষ\" দিন, তারপর OUT")
                    } else {
                        val docId = existing.docId.ifBlank { attendanceDocId(today, s.code) }
                        subCol("attendance").document(docId).update(
                            mapOf(
                                "outTime" to timeNow.take(20),
                                "outDist" to dist.toString().take(30),
                                "upd" to tsNow.take(60),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        PunchResult(true, "OUT সফল হয়েছে: ${AttendanceCalculator.formatTime12h(timeNow)}")
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orgs/$effectiveOrgId/attendance")
            PunchResult(false, "সিঙ্ক ত্রুটি: ${e.localizedMessage ?: "আবার চেষ্টা করুন"}")
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
        val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(fetchAll("leave_types"))
        val allLeaves = fetchAll<LeaveEntity>("leaves")
        val transfers = fetchAll<TransferEntity>("transfers")
        val hols = AttendanceCalculator.holMap(fetchAll("holidays"), s, transfers)
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
            .sortedByDescending { it.appliedOn }
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

    suspend fun cancelStaffLeave(code: String, pin: String, leaveDocId: String): PunchResult {
        val path = "orgs/$effectiveOrgId/leaves/$leaveDocId"
        return try {
            val (s, err) = authStaffForLeave(code, pin)
            if (s == null) return PunchResult(false, err ?: "ত্রুটি")
            if (leaveDocId.isBlank()) return PunchResult(false, "আবেদন খুঁজে পাওয়া যায়নি")
            val snap = subCol("leaves").document(leaveDocId).get().await()
            val l = snap.toObject(LeaveEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (l == null || AttendanceCalculator.nc(l.code) != AttendanceCalculator.nc(s.code) || l.status.trim() != "Pending") {
                return PunchResult(false, "এই আবেদন বাতিল করা যাবে না")
            }
            subCol("leaves").document(leaveDocId).delete().await()
            PunchResult(true, "আবেদন বাতিল হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            PunchResult(false, "বাতিল ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
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
        val path = "orgs/$effectiveOrgId/leaves"
        return try {
            val uid = requireUserId()
            val members = getWorkspaceMemberUids()
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
            val (ltMap, mainName) = AttendanceCalculator.parseLeaveTypes(fetchAll("leave_types"))
            val tBase = ltMap[type.trim()]
            if (tBase == null || (byStaff && tBase.holiday)) {
                return PunchResult(false, "ছুটির ধরন বেছে নিন")
            }
            val allLeaves = fetchAll<LeaveEntity>("leaves")
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
            val transfers = fetchAll<TransferEntity>("transfers")
            val hols = AttendanceCalculator.holMap(fetchAll("holidays"), s, transfers)
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

            val leave = LeaveEntity(
                code = s.code,
                dateFrom = from,
                dateTo = to,
                typ = tEff.name,
                note = "",
                status = status,
                appliedOn = today,
                reason = reason.trim().take(300)
            )
            subCol("leaves").document().set(leaveCreateMap(leave, uid, members)).await()

            var unpaidCount = 0
            if (byStaff && tEff.paid && qoQ != null) {
                val updatedLeaves = fetchAll<LeaveEntity>("leaves")
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
            PunchResult(true, msg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            PunchResult(false, "ছুটি সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    suspend fun decideLeave(docId: String, status: String, note: String): Result<Unit> {
        val path = "orgs/$effectiveOrgId/leaves/$docId"
        return try {
            requireUserId()
            require(docId.isNotBlank())
            val safeStatus = if (status in listOf("Approved", "Rejected", "Pending")) status else "Pending"
            subCol("leaves").document(docId).update(
                mapOf(
                    "status" to safeStatus,
                    "note" to note.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLeave(docId: String): Result<Unit> = deleteSubDoc("leaves", docId)

    suspend fun addTransfer(code: String, effDateRaw: String, newBranchRaw: String): PunchResult {
        val path = "orgs/$effectiveOrgId/transfers"
        return try {
            val uid = requireUserId()
            val members = getWorkspaceMemberUids()
            val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
            val eff = effDateRaw.trim()
            val nb = newBranchRaw.trim()
            if (!AttendanceCalculator.isValidDate(eff)) return PunchResult(false, "তারিখ দিন")
            if (nb.isEmpty()) return PunchResult(false, "নতুন শাখা বাছুন")
            val branches = fetchAll<BranchEntity>("branches")
            if (branches.none { it.branch.equals(nb, ignoreCase = true) }) {
                return PunchResult(false, "এই নামে কোনো শাখা নেই")
            }

            val mine = fetchAll<TransferEntity>("transfers")
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

            val trf = TransferEntity(code = s.code, effDate = eff, fromBranch = old, toBranch = nb)
            val trfRef = subCol("transfers").document(transferDocId(s.code, eff))
            val staffRef = subCol("staff").document(s.docId.ifBlank { staffDocId(s.code) })

            val batch = db.batch()
            val existingTrf = trfRef.get().await()
            if (existingTrf.exists()) {
                batch.update(trfRef, transferUpdateMap(trf, members))
            } else {
                batch.set(trfRef, transferCreateMap(trf, uid, members))
            }
            batch.update(
                staffRef,
                mapOf(
                    "branch" to nb.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            PunchResult(true, "বদলি যোগ হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            PunchResult(false, "বদলি সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    suspend fun deleteTransfer(docId: String): PunchResult {
        val path = "orgs/$effectiveOrgId/transfers/$docId"
        return try {
            requireUserId()
            val all = fetchAll<TransferEntity>("transfers")
            val t = all.find { it.docId == docId } ?: return PunchResult(false, "বদলি পাওয়া যায়নি")
            if (all.any { AttendanceCalculator.nc(it.code) == AttendanceCalculator.nc(t.code) && it.effDate > t.effDate }) {
                return PunchResult(false, "আগে এই কর্মীর পরের বদলিটা মুছুন")
            }
            val batch = db.batch()
            batch.delete(subCol("transfers").document(t.docId))
            val staff = findStaffByCode(t.code)
            if (staff != null) {
                val staffRef = subCol("staff").document(staff.docId.ifBlank { staffDocId(staff.code) })
                batch.update(
                    staffRef,
                    mapOf(
                        "branch" to t.fromBranch.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            PunchResult(true, "মুছে ফেলা হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            PunchResult(false, "মুছতে ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    suspend fun saveManualPunch(
        code: String,
        date: String,
        inTimeRaw: String,
        outTimeRaw: String,
        noteRaw: String
    ): PunchResult {
        val path = "orgs/$effectiveOrgId/attendance"
        return try {
            val uid = requireUserId()
            val members = getWorkspaceMemberUids()
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
            val existing = getAttendanceForDay(date, s.code)
            val branch = AttendanceCalculator.branchAt(s, fetchAll("transfers"), date)
            val ts = java.time.Instant.now().toString()
            val updated = (existing ?: AttendanceEntity(date = date, code = s.code)).copy(
                name = s.name,
                branch = branch,
                inTime = tin.ifEmpty { existing?.inTime.orEmpty() },
                outTime = tout.ifEmpty { existing?.outTime.orEmpty() },
                source = "Manual",
                note = noteRaw.trim().ifEmpty { existing?.note.orEmpty() },
                upd = ts
            )
            upsertAttendanceDoc(updated, uid, members)
            PunchResult(true, "সংরক্ষিত হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            PunchResult(false, "সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    private suspend fun upsertAttendanceDoc(item: AttendanceEntity, uid: String, members: List<String>) {
        val docId = item.docId.ifBlank { attendanceDocId(item.date, item.code) }
        val ref = subCol("attendance").document(docId)
        val snap = ref.get().await()
        if (snap.exists()) {
            ref.update(attendanceUpdateMap(item, members)).await()
        } else {
            ref.set(attendanceCreateMap(item, uid, members)).await()
        }
    }

    suspend fun deleteManualPunch(code: String, date: String): PunchResult {
        val path = "orgs/$effectiveOrgId/attendance"
        return try {
            requireUserId()
            val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
            if (!AttendanceCalculator.isValidDate(date)) return PunchResult(false, "তারিখ দিন")
            val nc = AttendanceCalculator.nc(s.code)
            val batch = db.batch()
            val att = getAttendanceForDay(date, s.code)
            if (att != null && att.docId.isNotBlank()) {
                batch.delete(subCol("attendance").document(att.docId))
            } else {
                batch.delete(subCol("attendance").document(attendanceDocId(date, s.code)))
            }
            val brks = getBreaksForDay(s.code, date)
            brks.forEach { b ->
                if (b.docId.isNotBlank()) batch.delete(subCol("breaks").document(b.docId))
            }
            batch.commit().await()
            PunchResult(true, "হাজিরা মুছে ফেলা হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            PunchResult(false, "মুছতে ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    suspend fun saveBreak(
        docId: String,
        code: String,
        date: String,
        startRaw: String,
        endRaw: String
    ): PunchResult {
        val path = "orgs/$effectiveOrgId/breaks"
        return try {
            val uid = requireUserId()
            val members = getWorkspaceMemberUids()
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
            val others = getBreaksForDay(s.code, d).filter { it.docId != docId }
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
            val brk = BreakEntity(
                docId = docId,
                date = d,
                code = s.code,
                startTime = st,
                endTime = en,
                source = "Manual",
                upd = ts
            )
            if (docId.isNotBlank()) {
                subCol("breaks").document(docId).update(breakUpdateMap(brk, members)).await()
            } else {
                subCol("breaks").document().set(breakCreateMap(brk, uid, members)).await()
            }
            PunchResult(true, "ব্রেক সংরক্ষিত হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            PunchResult(false, "ব্রেক সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    suspend fun deleteBreak(docId: String): Result<Unit> = deleteSubDoc("breaks", docId)

    suspend fun saveFace(code: String, vectors: List<List<Double>>): PunchResult {
        val path = "orgs/$effectiveOrgId/staff"
        return try {
            requireUserId()
            val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
            if (vectors.isEmpty()) return PunchResult(false, "মুখের তথ্য সঠিক নয়")
            val json = vectors.joinToString(prefix = "[", postfix = "]") { row ->
                row.joinToString(prefix = "[", postfix = "]") { String.format("%.5f", it) }
            }
            val docId = s.docId.ifBlank { staffDocId(s.code) }
            subCol("staff").document(docId).update(
                mapOf(
                    "faceJson" to json.take(16384),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            PunchResult(true, "মুখ নিবন্ধিত হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            PunchResult(false, "মুখ সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    suspend fun deleteFace(code: String): PunchResult {
        val path = "orgs/$effectiveOrgId/staff"
        return try {
            requireUserId()
            val s = findStaffByCode(code) ?: return PunchResult(false, "স্টাফ পাওয়া যায়নি")
            val docId = s.docId.ifBlank { staffDocId(s.code) }
            subCol("staff").document(docId).update(
                mapOf(
                    "faceJson" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            PunchResult(true, "মুখ-নিবন্ধন মুছে ফেলা হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            PunchResult(false, "মুছতে ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
        }
    }

    // CRUD for Setup Tables
    suspend fun saveStaff(item: StaffEntity): Result<Unit> {
        val id = item.docId.ifBlank { staffDocId(item.code) }
        return upsertDoc(
            sub = "staff",
            docId = id,
            createFn = { uid, members -> staffCreateMap(item, uid, members) },
            updateFn = { members -> staffUpdateMap(item, members) }
        )
    }

    suspend fun deleteStaff(docId: String): Result<Unit> = deleteSubDoc("staff", docId)

    suspend fun saveLeaveType(item: LeaveTypeEntity): Result<Unit> {
        val id = item.docId.ifBlank { leaveTypeDocId(item.ltype) }
        return upsertDoc(
            sub = "leave_types",
            docId = id,
            createFn = { uid, members -> leaveTypeCreateMap(item, uid, members) },
            updateFn = { members -> leaveTypeUpdateMap(item, members) }
        )
    }

    suspend fun deleteLeaveType(docId: String): Result<Unit> = deleteSubDoc("leave_types", docId)

    suspend fun saveHoliday(item: HolidayEntity): Result<Unit> {
        val id = item.docId.ifBlank { holidayDocId(item.dateFrom, item.dateTo, item.name) }
        return upsertDoc(
            sub = "holidays",
            docId = id,
            createFn = { uid, members -> holidayCreateMap(item, uid, members) },
            updateFn = { members -> holidayUpdateMap(item, members) }
        )
    }

    suspend fun deleteHoliday(docId: String): Result<Unit> = deleteSubDoc("holidays", docId)

    suspend fun saveShift(item: ShiftEntity): Result<Unit> {
        val id = item.docId.ifBlank { shiftDocId(item.shift) }
        return upsertDoc(
            sub = "shifts",
            docId = id,
            createFn = { uid, members -> shiftCreateMap(item, uid, members) },
            updateFn = { members -> shiftUpdateMap(item, members) }
        )
    }

    suspend fun deleteShift(docId: String): Result<Unit> = deleteSubDoc("shifts", docId)

    suspend fun saveBranch(item: BranchEntity): Result<Unit> {
        val id = item.docId.ifBlank { branchDocId(item.branch) }
        return upsertDoc(
            sub = "branches",
            docId = id,
            createFn = { uid, members -> branchCreateMap(item, uid, members) },
            updateFn = { members -> branchUpdateMap(item, members) }
        )
    }

    suspend fun deleteBranch(docId: String): Result<Unit> = deleteSubDoc("branches", docId)

    suspend fun saveSetting(item: SettingEntity): Result<Unit> {
        val id = item.docId.ifBlank { settingDocId(item.key) }
        val res = upsertDoc(
            sub = "settings",
            docId = id,
            createFn = { uid, members -> settingCreateMap(item, uid, members) },
            updateFn = { members -> settingUpdateMap(item, members) }
        )
        if (res.isSuccess && item.key == "OrgName" && item.value.isNotBlank()) {
            try {
                orgRef().update(
                    mapOf(
                        "orgName" to item.value.trim().take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {
            }
        }
        return res
    }

    /**
     * Adds another Google user's UID to /orgs/{orgId} and propagates memberUids across subcollections
     * so multiple devices signed in with different Google accounts can view and sync live.
     */
    suspend fun addAuthorizedMemberUid(memberUidRaw: String): PunchResult {
        val targetUid = memberUidRaw.trim()
        if (!VALID_ID_REGEX.matches(targetUid)) {
            return PunchResult(false, "সঠিক Firebase User UID দিন")
        }
        val path = "orgs/$effectiveOrgId"
        return try {
            val uid = requireUserId()
            val orgSnap = orgRef().get().await()
            val currentMembers = if (orgSnap.exists()) {
                val ws = orgSnap.toObject(OrgWorkspace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ((ws?.memberUids ?: emptyList()) + uid + targetUid).distinct().take(50)
            } else {
                listOf(uid, targetUid).distinct().take(50)
            }
            if (orgSnap.exists()) {
                orgRef().update(
                    mapOf(
                        "memberUids" to currentMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                orgRef().set(
                    mapOf(
                        "ownerId" to uid,
                        "memberUids" to currentMembers,
                        "orgName" to "ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            cachedMemberUids = currentMembers

            val subs = listOf(
                "settings", "branches", "shifts", "staff",
                "holidays", "leave_types", "leaves", "transfers", "attendance", "breaks"
            )
            val updates = mutableListOf<DocumentReference>()
            subs.forEach { sub ->
                val docs = subCol(sub).get().await().documents
                docs.forEach { updates.add(it.reference) }
            }
            updates.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref ->
                    batch.update(
                        ref,
                        mapOf(
                            "memberUids" to currentMembers,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            PunchResult(true, "সদস্য UID যোগ হয়েছে এবং সব ডেটায় শেয়ার করা হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            PunchResult(false, "শেয়ার ব্যর্থ: ${e.localizedMessage ?: "অনুমতি নেই"}")
        }
    }

    /**
     * Imports an exported Neon PostgreSQL JSON bundle directly into Firestore subcollections.
     * Supports both snake_case PostgreSQL column names and camelCase keys.
     */
    suspend fun importNeonJson(rawJson: String): PunchResult {
        val path = "orgs/$effectiveOrgId"
        return try {
            val uid = requireUserId()
            val members = getWorkspaceMemberUids()
            val root = Json.parseToJsonElement(rawJson.trim()).jsonObject

            val createOps = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            val updateOps = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

            suspend fun existingIdsOf(sub: String): Set<String> =
                subCol(sub).get().await().documents.map { it.id }.toSet()

            fun JsonObject.str(vararg keys: String): String {
                for (k in keys) {
                    val el: JsonElement = this[k] ?: continue
                    val raw = try {
                        el.jsonPrimitive.contentOrNull
                    } catch (_: Exception) {
                        el.toString()
                    }
                    if (!raw.isNullOrBlank() && raw != "null") return raw.trim()
                }
                return ""
            }

            fun JsonObject.arr(key: String): List<JsonObject> {
                val el = this[key] ?: return emptyList()
                return try {
                    el.jsonArray.mapNotNull { it as? JsonObject }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            var totalCount = 0

            // 1. settings
            val settingsRows = root.arr("settings")
            if (settingsRows.isNotEmpty()) {
                val existing = existingIdsOf("settings")
                settingsRows.forEach { row ->
                    val k = row.str("key", "k", "setting_key")
                    if (k.isNotBlank()) {
                        val v = row.str("value", "v", "val")
                        val item = SettingEntity(key = k, value = v)
                        val id = settingDocId(k)
                        val ref = subCol("settings").document(id)
                        if (id in existing) updateOps.add(ref to settingUpdateMap(item, members))
                        else createOps.add(ref to settingCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 2. branches
            val branchRows = root.arr("branches")
            if (branchRows.isNotEmpty()) {
                val existing = existingIdsOf("branches")
                branchRows.forEach { row ->
                    val bName = row.str("branch", "name")
                    if (bName.isNotBlank()) {
                        val item = BranchEntity(
                            branch = bName,
                            lat = row.str("lat", "latitude"),
                            lng = row.str("lng", "longitude"),
                            radius = row.str("radius").ifBlank { "150" }
                        )
                        val id = branchDocId(bName)
                        val ref = subCol("branches").document(id)
                        if (id in existing) updateOps.add(ref to branchUpdateMap(item, members))
                        else createOps.add(ref to branchCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 3. shifts
            val shiftRows = root.arr("shifts")
            if (shiftRows.isNotEmpty()) {
                val existing = existingIdsOf("shifts")
                shiftRows.forEach { row ->
                    val sName = row.str("shift", "name")
                    if (sName.isNotBlank()) {
                        val item = ShiftEntity(
                            shift = sName,
                            startTime = row.str("start_time", "startTime", "start").ifBlank { "09:30" },
                            endTime = row.str("end_time", "endTime", "end").ifBlank { "21:30" }
                        )
                        val id = shiftDocId(sName)
                        val ref = subCol("shifts").document(id)
                        if (id in existing) updateOps.add(ref to shiftUpdateMap(item, members))
                        else createOps.add(ref to shiftCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 4. staff
            val staffRows = root.arr("staff")
            if (staffRows.isNotEmpty()) {
                val existing = existingIdsOf("staff")
                staffRows.forEach { row ->
                    val code = row.str("code")
                    val name = row.str("name")
                    if (code.isNotBlank() && name.isNotBlank()) {
                        val item = StaffEntity(
                            code = code,
                            name = name,
                            designation = row.str("designation", "desig"),
                            branch = row.str("branch"),
                            shift = row.str("shift").ifBlank { "সাধারণ" },
                            pin = row.str("pin").ifBlank { "1234" },
                            status = row.str("status").ifBlank { "Active" },
                            weeklyOff = row.str("weekly_off", "weeklyOff"),
                            joinDate = row.str("join_date", "joinDate"),
                            leaveDate = row.str("leave_date", "leaveDate"),
                            reservedDays = row.str("reserved_days", "reservedDays"),
                            probationMonths = row.str("probation_months", "probationMonths"),
                            faceJson = row.str("face_json", "faceJson")
                        )
                        val id = staffDocId(code)
                        val ref = subCol("staff").document(id)
                        if (id in existing) updateOps.add(ref to staffUpdateMap(item, members))
                        else createOps.add(ref to staffCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 5. holidays
            val holidayRows = root.arr("holidays")
            if (holidayRows.isNotEmpty()) {
                val existing = existingIdsOf("holidays")
                holidayRows.forEach { row ->
                    val df = row.str("date_from", "dateFrom", "date")
                    if (df.isNotBlank()) {
                        val dt = row.str("date_to", "dateTo").ifBlank { df }
                        val nm = row.str("name", "title")
                        val item = HolidayEntity(
                            dateFrom = df,
                            dateTo = dt,
                            name = nm,
                            branch = row.str("branch")
                        )
                        val id = holidayDocId(df, dt, nm)
                        val ref = subCol("holidays").document(id)
                        if (id in existing) updateOps.add(ref to holidayUpdateMap(item, members))
                        else createOps.add(ref to holidayCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 6. leave_types
            val ltRows = root.arr("leave_types")
            if (ltRows.isNotEmpty()) {
                val existing = existingIdsOf("leave_types")
                ltRows.forEach { row ->
                    val lt = row.str("ltype", "name", "type")
                    if (lt.isNotBlank()) {
                        val item = LeaveTypeEntity(
                            ltype = lt,
                            days = row.str("days"),
                            paid = row.str("paid").ifBlank { "Yes" },
                            kind = row.str("kind").ifBlank { "Leave" },
                            probation = row.str("probation")
                        )
                        val id = leaveTypeDocId(lt)
                        val ref = subCol("leave_types").document(id)
                        if (id in existing) updateOps.add(ref to leaveTypeUpdateMap(item, members))
                        else createOps.add(ref to leaveTypeCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 7. leaves
            val leaveRows = root.arr("leaves")
            if (leaveRows.isNotEmpty()) {
                val existing = existingIdsOf("leaves")
                leaveRows.forEachIndexed { idx, row ->
                    val code = row.str("code")
                    val df = row.str("date_from", "dateFrom")
                    val typ = row.str("typ", "type")
                    if (code.isNotBlank() && df.isNotBlank() && typ.isNotBlank()) {
                        val dt = row.str("date_to", "dateTo").ifBlank { df }
                        val stRaw = row.str("status")
                        val safeSt = if (stRaw in listOf("Pending", "Approved", "Rejected", "")) stRaw else "Approved"
                        val item = LeaveEntity(
                            code = code,
                            dateFrom = df,
                            dateTo = dt,
                            typ = typ,
                            note = row.str("note"),
                            status = safeSt,
                            appliedOn = row.str("applied_on", "appliedOn").ifBlank { df },
                            reason = row.str("reason")
                        )
                        val id = "lv_${hashShort("$code|$df|$dt|$typ|$idx")}"
                        val ref = subCol("leaves").document(id)
                        if (id !in existing) {
                            createOps.add(ref to leaveCreateMap(item, uid, members))
                            totalCount++
                        }
                    }
                }
            }

            // 8. transfers
            val trfRows = root.arr("transfers")
            if (trfRows.isNotEmpty()) {
                val existing = existingIdsOf("transfers")
                trfRows.forEach { row ->
                    val code = row.str("code")
                    val eff = row.str("eff_date", "effDate")
                    if (code.isNotBlank() && eff.isNotBlank()) {
                        val item = TransferEntity(
                            code = code,
                            effDate = eff,
                            fromBranch = row.str("from_branch", "fromBranch"),
                            toBranch = row.str("to_branch", "toBranch")
                        )
                        val id = transferDocId(code, eff)
                        val ref = subCol("transfers").document(id)
                        if (id in existing) updateOps.add(ref to transferUpdateMap(item, members))
                        else createOps.add(ref to transferCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 9. attendance
            val attRows = root.arr("attendance")
            if (attRows.isNotEmpty()) {
                val existing = existingIdsOf("attendance")
                attRows.forEach { row ->
                    val date = row.str("date")
                    val code = row.str("code")
                    if (date.isNotBlank() && code.isNotBlank()) {
                        val item = AttendanceEntity(
                            date = date,
                            code = code,
                            name = row.str("name"),
                            branch = row.str("branch"),
                            inTime = row.str("in_time", "inTime"),
                            outTime = row.str("out_time", "outTime"),
                            inDist = row.str("in_dist", "inDist"),
                            outDist = row.str("out_dist", "outDist"),
                            source = row.str("source").ifBlank { "Imported" },
                            note = row.str("note"),
                            upd = row.str("upd")
                        )
                        val id = attendanceDocId(date, code)
                        val ref = subCol("attendance").document(id)
                        if (id in existing) updateOps.add(ref to attendanceUpdateMap(item, members))
                        else createOps.add(ref to attendanceCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            // 10. breaks
            val breakRows = root.arr("breaks")
            if (breakRows.isNotEmpty()) {
                val existing = existingIdsOf("breaks")
                breakRows.forEach { row ->
                    val date = row.str("date")
                    val code = row.str("code")
                    val st = row.str("start_time", "startTime")
                    if (date.isNotBlank() && code.isNotBlank() && st.isNotBlank()) {
                        val item = BreakEntity(
                            date = date,
                            code = code,
                            startTime = st,
                            endTime = row.str("end_time", "endTime"),
                            source = row.str("source").ifBlank { "Imported" },
                            upd = row.str("upd")
                        )
                        val id = "brk_${safeIdPart(date)}_${safeIdPart(AttendanceCalculator.nc(code))}_${safeIdPart(st)}"
                        val ref = subCol("breaks").document(id)
                        if (id in existing) updateOps.add(ref to breakUpdateMap(item, members))
                        else createOps.add(ref to breakCreateMap(item, uid, members))
                        totalCount++
                    }
                }
            }

            if (totalCount == 0) {
                return PunchResult(false, "JSON ফাইলে কোনো বৈধ রেকর্ড পাওয়া যায়নি")
            }

            if (!orgRef().get().await().exists()) {
                orgRef().set(
                    mapOf(
                        "ownerId" to uid,
                        "memberUids" to members,
                        "orgName" to "ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            commitSetChunks(createOps)
            updateOps.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, map) -> batch.update(ref, map) }
                batch.commit().await()
            }
            PunchResult(true, "সফলভাবে ${AttendanceCalculator.toBanglaDigits(totalCount)}টি রেকর্ড Firestore-এ ইমপোর্ট হয়েছে")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            PunchResult(false, "ইমপোর্ট ব্যর্থ: ${e.localizedMessage ?: "JSON ফরম্যাট পরীক্ষা করুন"}")
        }
    }

    private suspend fun upsertDoc(
        sub: String,
        docId: String,
        createFn: (uid: String, members: List<String>) -> Map<String, Any>,
        updateFn: (members: List<String>) -> Map<String, Any>
    ): Result<Unit> {
        val path = "orgs/$effectiveOrgId/$sub/$docId"
        return try {
            val uid = requireUserId()
            require(VALID_ID_REGEX.matches(docId)) { "Invalid document ID" }
            val members = getWorkspaceMemberUids()
            val ref = subCol(sub).document(docId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(updateFn(members)).await()
            } else {
                ref.set(createFn(uid, members)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun deleteSubDoc(sub: String, docId: String): Result<Unit> {
        val path = "orgs/$effectiveOrgId/$sub/$docId"
        return try {
            requireUserId()
            require(docId.isNotBlank()) { "Document ID cannot be blank" }
            subCol(sub).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // Strict Code-to-Rules Parity Maps (Never include "docId" or "id"; null-safe bounded lengths)
    private fun settingCreateMap(item: SettingEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "key" to item.key.trim().take(80),
        "value" to item.value.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun settingUpdateMap(item: SettingEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "key" to item.key.trim().take(80),
        "value" to item.value.trim().take(500),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun branchCreateMap(item: BranchEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "branch" to item.branch.trim().take(120),
        "lat" to item.lat.trim().take(40),
        "lng" to item.lng.trim().take(40),
        "radius" to item.radius.trim().take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun branchUpdateMap(item: BranchEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "branch" to item.branch.trim().take(120),
        "lat" to item.lat.trim().take(40),
        "lng" to item.lng.trim().take(40),
        "radius" to item.radius.trim().take(20),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun shiftCreateMap(item: ShiftEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "shift" to item.shift.trim().take(100),
        "startTime" to item.startTime.trim().take(20),
        "endTime" to item.endTime.trim().take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun shiftUpdateMap(item: ShiftEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "shift" to item.shift.trim().take(100),
        "startTime" to item.startTime.trim().take(20),
        "endTime" to item.endTime.trim().take(20),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun staffCreateMap(item: StaffEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "code" to item.code.trim().take(40),
        "name" to item.name.trim().take(160),
        "designation" to item.designation.trim().take(120),
        "branch" to item.branch.trim().take(120),
        "shift" to item.shift.trim().take(100),
        "pin" to item.pin.trim().ifBlank { "1234" }.take(40),
        "status" to item.status.trim().take(30),
        "weeklyOff" to item.weeklyOff.trim().take(60),
        "joinDate" to item.joinDate.trim().take(20),
        "leaveDate" to item.leaveDate.trim().take(20),
        "reservedDays" to item.reservedDays.trim().take(20),
        "probationMonths" to item.probationMonths.trim().take(20),
        "faceJson" to item.faceJson.take(16384),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun staffUpdateMap(item: StaffEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "code" to item.code.trim().take(40),
        "name" to item.name.trim().take(160),
        "designation" to item.designation.trim().take(120),
        "branch" to item.branch.trim().take(120),
        "shift" to item.shift.trim().take(100),
        "pin" to item.pin.trim().ifBlank { "1234" }.take(40),
        "status" to item.status.trim().take(30),
        "weeklyOff" to item.weeklyOff.trim().take(60),
        "joinDate" to item.joinDate.trim().take(20),
        "leaveDate" to item.leaveDate.trim().take(20),
        "reservedDays" to item.reservedDays.trim().take(20),
        "probationMonths" to item.probationMonths.trim().take(20),
        "faceJson" to item.faceJson.take(16384),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun holidayCreateMap(item: HolidayEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "dateFrom" to item.dateFrom.trim().take(20),
        "dateTo" to item.dateTo.trim().take(20),
        "name" to item.name.trim().take(160),
        "branch" to item.branch.trim().take(400),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun holidayUpdateMap(item: HolidayEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "dateFrom" to item.dateFrom.trim().take(20),
        "dateTo" to item.dateTo.trim().take(20),
        "name" to item.name.trim().take(160),
        "branch" to item.branch.trim().take(400),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun leaveTypeCreateMap(item: LeaveTypeEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "ltype" to item.ltype.trim().take(120),
        "days" to item.days.trim().take(20),
        "paid" to item.paid.trim().take(20),
        "kind" to item.kind.trim().take(30),
        "probation" to item.probation.trim().take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun leaveTypeUpdateMap(item: LeaveTypeEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "ltype" to item.ltype.trim().take(120),
        "days" to item.days.trim().take(20),
        "paid" to item.paid.trim().take(20),
        "kind" to item.kind.trim().take(30),
        "probation" to item.probation.trim().take(20),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun leaveCreateMap(item: LeaveEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "code" to item.code.trim().take(40),
        "dateFrom" to item.dateFrom.trim().take(20),
        "dateTo" to item.dateTo.trim().take(20),
        "typ" to item.typ.trim().take(120),
        "note" to item.note.trim().take(300),
        "status" to (if (item.status in listOf("Pending", "Approved", "Rejected", "")) item.status else "Approved"),
        "appliedOn" to item.appliedOn.trim().take(30),
        "reason" to item.reason.trim().take(350),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun transferCreateMap(item: TransferEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "code" to item.code.trim().take(40),
        "effDate" to item.effDate.trim().take(20),
        "fromBranch" to item.fromBranch.trim().take(120),
        "toBranch" to item.toBranch.trim().take(120),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun transferUpdateMap(item: TransferEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "code" to item.code.trim().take(40),
        "effDate" to item.effDate.trim().take(20),
        "fromBranch" to item.fromBranch.trim().take(120),
        "toBranch" to item.toBranch.trim().take(120),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun attendanceCreateMap(item: AttendanceEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "date" to item.date.trim().take(20),
        "code" to item.code.trim().take(40),
        "name" to item.name.trim().take(160),
        "branch" to item.branch.trim().take(120),
        "inTime" to item.inTime.trim().take(20),
        "outTime" to item.outTime.trim().take(20),
        "inDist" to item.inDist.trim().take(30),
        "outDist" to item.outDist.trim().take(30),
        "source" to item.source.trim().take(40),
        "note" to item.note.trim().take(300),
        "upd" to item.upd.trim().take(60),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun attendanceUpdateMap(item: AttendanceEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "name" to item.name.trim().take(160),
        "branch" to item.branch.trim().take(120),
        "inTime" to item.inTime.trim().take(20),
        "outTime" to item.outTime.trim().take(20),
        "inDist" to item.inDist.trim().take(30),
        "outDist" to item.outDist.trim().take(30),
        "source" to item.source.trim().take(40),
        "note" to item.note.trim().take(300),
        "upd" to item.upd.trim().take(60),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun breakCreateMap(item: BreakEntity, uid: String, members: List<String>) = mapOf(
        "ownerId" to uid,
        "memberUids" to members,
        "date" to item.date.trim().take(20),
        "code" to item.code.trim().take(40),
        "startTime" to item.startTime.trim().take(20),
        "endTime" to item.endTime.trim().take(20),
        "source" to item.source.trim().take(40),
        "upd" to item.upd.trim().take(60),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun breakUpdateMap(item: BreakEntity, members: List<String>) = mapOf(
        "memberUids" to members,
        "startTime" to item.startTime.trim().take(20),
        "endTime" to item.endTime.trim().take(20),
        "source" to item.source.trim().take(40),
        "upd" to item.upd.trim().take(60),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
