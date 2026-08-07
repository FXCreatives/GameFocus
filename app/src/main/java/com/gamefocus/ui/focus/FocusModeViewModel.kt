package com.gamefocus.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.SettingsManager
import com.gamefocus.utils.FocusModeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FocusModeViewModel(application: Application) : AndroidViewModel(application) {
    private val focusModeManager = FocusModeManager(application)
    private val settingsManager = SettingsManager(application)

    private val _hasDndAccess = MutableStateFlow(false)
    val hasDndAccess: StateFlow<Boolean> = _hasDndAccess.asStateFlow()

    private val _isFocusModeOn = MutableStateFlow(false)
    val isFocusModeOn: StateFlow<Boolean> = _isFocusModeOn.asStateFlow()

    private val _autoFocusEnabled = MutableStateFlow(false)
    val autoFocusEnabled: StateFlow<Boolean> = _autoFocusEnabled.asStateFlow()

    private val _focusDuration = MutableStateFlow(0L)
    val focusDuration: StateFlow<Long> = _focusDuration.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var durationTickerJob: Job? = null

    init {
        observeSettings()
        observeFocusMode()
        viewModelScope.launch {
            refreshDndStatus()
            kotlinx.coroutines.delay(300)
            _isLoading.value = false
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsManager.autoFocusEnabled.collectLatest { enabled ->
                if (_isLoading.value) _isLoading.value = false
                _autoFocusEnabled.value = enabled
            }
        }
    }

    private fun observeFocusMode() {
        viewModelScope.launch {
            focusModeManager.isFocusModeOn.collectLatest { isOn ->
                if (_isLoading.value) _isLoading.value = false
                _isFocusModeOn.value = isOn
                if (isOn) startDurationTicker() else stopDurationTicker()
            }
        }
    }

    private fun startDurationTicker() {
        stopDurationTicker()
        durationTickerJob = viewModelScope.launch {
            while (true) {
                _focusDuration.value = focusModeManager.getFocusDuration() / 60000
                kotlinx.coroutines.delay(60000)
            }
        }
    }

    private fun stopDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = null
    }

    fun setFocusModeEnabled(enabled: Boolean) {
        if (enabled) {
            focusModeManager.enableFocusMode()
        } else {
            focusModeManager.disableFocusMode()
        }
    }

    fun setAutoFocusEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAutoFocusEnabled(enabled)
        }
    }

    fun refreshDndStatus() {
        _hasDndAccess.value = focusModeManager.hasDndAccess()
    }

    fun updateFocusDuration() {
        _focusDuration.value = focusModeManager.getFocusDuration() / 60000
    }

    override fun onCleared() {
        super.onCleared()
        stopDurationTicker()
    }
}