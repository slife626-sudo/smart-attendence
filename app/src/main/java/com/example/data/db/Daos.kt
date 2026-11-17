package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings ORDER BY id")
    fun observeAll(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings ORDER BY id")
    suspend fun getAll(): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SettingEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<SettingEntity>)

    @Update
    suspend fun update(item: SettingEntity)
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches ORDER BY id")
    fun observeAll(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches ORDER BY id")
    suspend fun getAll(): List<BranchEntity>

    @Query("SELECT * FROM branches WHERE branch = :name LIMIT 1")
    suspend fun getByName(name: String): BranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BranchEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<BranchEntity>)

    @Update
    suspend fun update(item: BranchEntity)

    @Query("DELETE FROM branches WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY id")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts ORDER BY id")
    suspend fun getAll(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE shift = :name LIMIT 1")
    suspend fun getByName(name: String): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<ShiftEntity>)

    @Update
    suspend fun update(item: ShiftEntity)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY id")
    fun observeAll(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff ORDER BY id")
    suspend fun getAll(): List<StaffEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: StaffEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<StaffEntity>)

    @Update
    suspend fun update(item: StaffEntity)

    @Query("UPDATE staff SET branch = :branch WHERE code = :code")
    suspend fun updateBranchByCode(code: String, branch: String)

    @Query("UPDATE staff SET faceJson = :faceJson WHERE id = :id")
    suspend fun updateFace(id: Int, faceJson: String)

    @Query("DELETE FROM staff WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY id")
    fun observeAll(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays ORDER BY id")
    suspend fun getAll(): List<HolidayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HolidayEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<HolidayEntity>)

    @Update
    suspend fun update(item: HolidayEntity)

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface LeaveTypeDao {
    @Query("SELECT * FROM leave_types ORDER BY id")
    fun observeAll(): Flow<List<LeaveTypeEntity>>

    @Query("SELECT * FROM leave_types ORDER BY id")
    suspend fun getAll(): List<LeaveTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LeaveTypeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<LeaveTypeEntity>)

    @Update
    suspend fun update(item: LeaveTypeEntity)

    @Query("DELETE FROM leave_types WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leaves ORDER BY id")
    fun observeAll(): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leaves ORDER BY id")
    suspend fun getAll(): List<LeaveEntity>

    @Query("SELECT * FROM leaves WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): LeaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LeaveEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<LeaveEntity>)

    @Update
    suspend fun update(item: LeaveEntity)

    @Query("UPDATE leaves SET status = :status, note = :note WHERE id = :id")
    suspend fun updateDecision(id: Int, status: String, note: String)

    @Query("DELETE FROM leaves WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY id")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers ORDER BY id")
    suspend fun getAll(): List<TransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransferEntity)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date LIKE :prefix ORDER BY date, code")
    fun observeByPrefix(prefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date LIKE :prefix ORDER BY date, code")
    suspend fun getByPrefix(prefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date AND code = :code LIMIT 1")
    suspend fun getForDay(date: String, code: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE date = :date AND code = :code LIMIT 1")
    fun observeForDay(date: String, code: String): Flow<AttendanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE date = :date AND code = :code")
    suspend fun deleteForDay(date: String, code: String)
}

@Dao
interface BreakDao {
    @Query("SELECT * FROM breaks WHERE date = :date AND code = :code ORDER BY startTime, id")
    fun observeForDay(date: String, code: String): Flow<List<BreakEntity>>

    @Query("SELECT * FROM breaks WHERE date = :date AND code = :code ORDER BY startTime, id")
    suspend fun getForDay(date: String, code: String): List<BreakEntity>

    @Query("SELECT * FROM breaks WHERE date LIKE :prefix ORDER BY date, startTime, id")
    fun observeByPrefix(prefix: String): Flow<List<BreakEntity>>

    @Query("SELECT * FROM breaks WHERE date >= :from AND date <= :to ORDER BY date, startTime, id")
    suspend fun getInRange(from: String, to: String): List<BreakEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BreakEntity)

    @Update
    suspend fun update(item: BreakEntity)

    @Query("DELETE FROM breaks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM breaks WHERE date = :date AND code = :code")
    suspend fun deleteForDay(date: String, code: String)
}
