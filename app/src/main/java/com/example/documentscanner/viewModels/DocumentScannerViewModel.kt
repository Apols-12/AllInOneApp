package com.example.documentscanner.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documentscanner.data.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentScannerViewModel : ViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateScanResult(result: ScanResult) {
        viewModelScope.launch {
            _scanResult.value = result
        }
    }

    fun setLoading(loading: Boolean) {
        viewModelScope.launch {
            _isLoading.value = loading
        }
    }

    fun setError(error: String?) {
        viewModelScope.launch {
            _errorMessage.value = error
        }
    }

    fun clearResults() {
        viewModelScope.launch {
            _scanResult.value = null
            _errorMessage.value = null
        }
    }
}
