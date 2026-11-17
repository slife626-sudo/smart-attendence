package com.example.ui.staff

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.StaffEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.StaffInfoResult
import com.example.data.repository.StaffLeavePortalData
import com.example.domain.AttendanceCalculator
import com.example.hardware.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class StaffUiState(
    val selectedCode: String = "01",
    val pin: String = "",
    val orgName: String = "ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী",
    val clockText: String = "",
    val staffInfo: StaffInfoResult = StaffInfoResult(ok = false, message = "লোড হচ্ছে..."),
    val busy: Boolean = false,
    val statusMessage: String = "",
    val statusOk: Boolean = true,
    val showFaceCameraForType: String? = null,
    val showLeavePortal: Boolean = false,
    val leaveLoading: Boolean = false,
    val leavePortalData: StaffLeavePortalData? = null,
    val leaveStatusMsg: String? = null,
    val leaveStatusOk: Boolean = true
)

class StaffViewModel(
    private val repository: AttendanceRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    val staffList: StateFlow<List<StaffEntity>> = repository.observeStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val savedCode = prefs.staffCode.first()
            val savedPin = prefs.staffPin.first()
            val cfg = repository.getSettingsMap()
            _uiState.update {
                it.copy(
                    selectedCode = savedCode,
                    pin = savedPin,
                    orgName = cfg["OrgName"] ?: it.orgName
                )
            }
            loadStaffInfo(savedCode)
        }
        viewModelScope.launch {
            while (true) {
                val cfg = repository.getSettingsMap()
                val tz = cfg["Timezone"] ?: "Asia/Dhaka"
                val zone = try { ZoneId.of(tz) } catch (_: Exception) { ZoneId.of("Asia/Dhaka") }
                val zdt = ZonedDateTime.now(zone)
                val timeStr = zdt.format(DateTimeFormatter.ofPattern("hh:mm:ss a"))
                _uiState.update { it.copy(clockText = AttendanceCalculator.toBanglaDigits(timeStr)) }
                delay(1000L)
            }
        }
    }

    fun onCodeSelected(code: String) {
        viewModelScope.launch {
            prefs.saveStaffCode(code)
            _uiState.update { it.copy(selectedCode = code, statusMessage = "") }
            loadStaffInfo(code)
        }
    }

    fun onPinChanged(pin: String) {
        _uiState.update { it.copy(pin = pin) }
        viewModelScope.launch {
            prefs.saveStaffPin(pin)
        }
    }

    fun loadStaffInfo(code: String = _uiState.value.selectedCode) {
        viewModelScope.launch {
            val info = repository.getStaffInfo(code)
            val cfg = repository.getSettingsMap()
            _uiState.update {
                it.copy(
                    staffInfo = info,
                    orgName = cfg["OrgName"] ?: it.orgName
                )
            }
        }
    }

    fun setStatus(msg: String, ok: Boolean) {
        _uiState.update { it.copy(statusMessage = msg, statusOk = ok) }
    }

    fun startPunchFlow(type: String, context: Context) {
        val state = _uiState.value
        val pin = state.pin.trim()
        if (pin.isEmpty()) {
            setStatus("PIN দিন", false)
            return
        }
        viewModelScope.launch {
            prefs.saveStaffPin(pin)
        }
        if (state.staffInfo.faceEnrolled) {
            _uiState.update { it.copy(showFaceCameraForType = type, statusMessage = "ক্যামেরা চালু হচ্ছে...", statusOk = true) }
        } else {
            executePunchWithLocation(type, context, emptyList())
        }
    }

    fun dismissFaceCamera() {
        _uiState.update { it.copy(showFaceCameraForType = null, busy = false) }
    }

    fun onFaceCapturedForPunch(type: String, context: Context, descriptors: List<List<Double>>) {
        _uiState.update { it.copy(showFaceCameraForType = null) }
        executePunchWithLocation(type, context, descriptors)
    }

    private fun executePunchWithLocation(
        type: String,
        context: Context,
        descriptors: List<List<Double>>
    ) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, statusMessage = "লোকেশন নেওয়া হচ্ছে...", statusOk = true) }
            val locRes = LocationHelper.getCurrentLocation(context)
            locRes.fold(
                onSuccess = { loc ->
                    val res = repository.punch(
                        code = state.selectedCode,
                        pin = state.pin,
                        type = type,
                        lat = loc.latitude,
                        lng = loc.longitude,
                        accuracy = loc.accuracyMeters,
                        faceDescriptors = descriptors
                    )
                    _uiState.update { it.copy(busy = false, statusMessage = res.message, statusOk = res.ok) }
                    loadStaffInfo(state.selectedCode)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            busy = false,
                            statusMessage = err.message ?: "লোকেশন পাওয়া যায়নি — ফোনের Location চালু করে Allow দিন",
                            statusOk = false
                        )
                    }
                }
            )
        }
    }

    fun openLeavePortal() {
        val pin = _uiState.value.pin.trim()
        if (pin.isEmpty()) {
            setStatus("আগে PIN দিন", false)
            return
        }
        _uiState.update { it.copy(showLeavePortal = true, leaveStatusMsg = null) }
        refreshLeavePortal()
    }

    fun closeLeavePortal() {
        _uiState.update { it.copy(showLeavePortal = false, leaveStatusMsg = null) }
    }

    fun refreshLeavePortal(bannerMsg: Pair<String, Boolean>? = null) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(leaveLoading = true) }
            val data = repository.getStaffLeavePortal(s.selectedCode, s.pin)
            _uiState.update {
                it.copy(
                    leaveLoading = false,
                    leavePortalData = data,
                    leaveStatusMsg = bannerMsg?.first,
                    leaveStatusOk = bannerMsg?.second ?: true
                )
            }
        }
    }

    fun applyLeave(type: String, from: String, to: String, reason: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(leaveLoading = true) }
            val res = repository.applyStaffLeave(s.selectedCode, s.pin, type, from, to, reason)
            if (res.ok) {
                refreshLeavePortal(res.message to true)
            } else {
                _uiState.update { it.copy(leaveLoading = false, leaveStatusMsg = res.message, leaveStatusOk = false) }
            }
        }
    }

    fun cancelLeave(leaveId: Int) {
        val s = _uiState.value
        viewModelScope.launch {
            val res = repository.cancelStaffLeave(s.selectedCode, s.pin, leaveId)
            refreshLeavePortal(res.message to res.ok)
        }
    }

    class Factory(
        private val repository: AttendanceRepository,
        private val prefs: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StaffViewModel(repository, prefs) as T
        }
    }
}
