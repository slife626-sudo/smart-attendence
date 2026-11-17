package com.example.data.db

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class OrgWorkspace(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val orgName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SettingEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val key: String = "",
    val value: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BranchEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val branch: String = "",
    val lat: String = "",
    val lng: String = "",
    val radius: String = "150",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShiftEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val shift: String = "",
    val startTime: String = "09:30",
    val endTime: String = "21:30",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StaffEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val code: String = "",
    val name: String = "",
    val designation: String = "",
    val branch: String = "",
    val shift: String = "সাধারণ",
    val pin: String = "1234",
    val status: String = "Active",
    val weeklyOff: String = "",
    val joinDate: String = "",
    val leaveDate: String = "",
    val reservedDays: String = "",
    val probationMonths: String = "",
    val faceJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HolidayEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val dateFrom: String = "",
    val dateTo: String = "",
    val name: String = "",
    val branch: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaveTypeEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val ltype: String = "",
    val days: String = "",
    val paid: String = "Yes",
    val kind: String = "Leave",
    val probation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaveEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val code: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val typ: String = "",
    val note: String = "",
    val status: String = "Approved",
    val appliedOn: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TransferEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val code: String = "",
    val effDate: String = "",
    val fromBranch: String = "",
    val toBranch: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val date: String = "",
    val code: String = "",
    val name: String = "",
    val branch: String = "",
    val inTime: String = "",
    val outTime: String = "",
    val inDist: String = "",
    val outDist: String = "",
    val source: String = "Mobile",
    val note: String = "",
    val upd: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BreakEntity(
    @DocumentId val docId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val date: String = "",
    val code: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val source: String = "Mobile",
    val upd: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
