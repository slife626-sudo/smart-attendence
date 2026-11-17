package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SettingEntity::class,
        BranchEntity::class,
        ShiftEntity::class,
        StaffEntity::class,
        HolidayEntity::class,
        LeaveTypeEntity::class,
        LeaveEntity::class,
        TransferEntity::class,
        AttendanceEntity::class,
        BreakEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingDao
    abstract fun branchDao(): BranchDao
    abstract fun shiftDao(): ShiftDao
    abstract fun staffDao(): StaffDao
    abstract fun holidayDao(): HolidayDao
    abstract fun leaveTypeDao(): LeaveTypeDao
    abstract fun leaveDao(): LeaveDao
    abstract fun transferDao(): TransferDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun breakDao(): BreakDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wahidiya_attendance.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { seedIfEmpty(it) }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedIfEmpty(db: AppDatabase) {
            if (db.settingDao().getAll().isEmpty()) {
                db.settingDao().insertAllIgnore(SeedData.settings)
                db.branchDao().insertAllIgnore(SeedData.branches)
                db.shiftDao().insertAllIgnore(SeedData.shifts)
                db.staffDao().insertAllIgnore(SeedData.staff)
                db.leaveTypeDao().insertAllIgnore(SeedData.leaveTypes)
                db.holidayDao().insertAllIgnore(SeedData.holidays)
                db.leaveDao().insertAllIgnore(SeedData.leaves)
                db.attendanceDao().insertAllIgnore(SeedData.initialAttendance)
            }
        }
    }
}
