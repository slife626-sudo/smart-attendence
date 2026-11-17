package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settings",
    indices = [Index(value = ["key"], unique = true)]
)
data class SettingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val value: String
)

@Entity(
    tableName = "branches",
    indices = [Index(value = ["branch"], unique = true)]
)
data class BranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val branch: String,
    val lat: String = "",
    val lng: String = "",
    val radius: String = "150"
)

@Entity(
    tableName = "shifts",
    indices = [Index(value = ["shift"], unique = true)]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shift: String,
    val startTime: String = "09:30",
    val endTime: String = "21:30"
)

@Entity(
    tableName = "staff",
    indices = [Index(value = ["code"], unique = true)]
)
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val name: String,
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
    val faceJson: String = ""
)

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateFrom: String,
    val dateTo: String = "",
    val name: String = "",
    val branch: String = ""
)

@Entity(
    tableName = "leave_types",
    indices = [Index(value = ["ltype"], unique = true)]
)
data class LeaveTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ltype: String,
    val days: String = "",
    val paid: String = "Yes",
    val kind: String = "Leave",
    val probation: String = ""
)

@Entity(tableName = "leaves")
data class LeaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val dateFrom: String,
    val dateTo: String,
    val typ: String,
    val note: String = "",
    val status: String = "Approved",
    val appliedOn: String = "",
    val reason: String = ""
)

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val effDate: String,
    val fromBranch: String = "",
    val toBranch: String = ""
)

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["date", "code"], unique = true), Index(value = ["date"])]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val code: String,
    val name: String = "",
    val branch: String = "",
    val inTime: String = "",
    val outTime: String = "",
    val inDist: String = "",
    val outDist: String = "",
    val source: String = "Mobile",
    val note: String = "",
    val upd: String = ""
)

@Entity(
    tableName = "breaks",
    indices = [Index(value = ["date", "code"])]
)
data class BreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val code: String,
    val startTime: String,
    val endTime: String = "",
    val source: String = "Mobile",
    val upd: String = ""
)
