package com.example.documentscanner.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documentscanner.data.CameraState
import com.example.documentscanner.data.SelfieSegmentationResult
import com.example.documentscanner.features.SelfieSegmentationProcessor
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SelfieSegmentationViewModel : ViewModel() {

    private val _segmentationResult = MutableStateFlow(SelfieSegmentationResult())
    val segmentationResult: StateFlow<SelfieSegmentationResult> = _segmentationResult.asStateFlow()

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.IDLE)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _selectedImage = MutableStateFlow<PlatformFile?>(null)
    val selectedImage: StateFlow<PlatformFile?> = _selectedImage.asStateFlow()

    private val segmentationProcessor = SelfieSegmentationProcessor()

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _segmentationResult.value = SelfieSegmentationResult(isLoading = false)

            try {
                val result = segmentationProcessor.processImage(bitmap)
                _segmentationResult.value = SelfieSegmentationResult(
                    originalBitmap = bitmap,
                    segmentedBitmap = result.segmentedBitmap,
                    maskBitmap = result.maskBitmap
                )
            } catch (e: Exception) {
                _segmentationResult.value = SelfieSegmentationResult(
                    error = "Segmentation failed: ${e.message}"
                )
            }
        }
    }


    fun setCameraState(state: CameraState) {
        _cameraState.value = state
    }

    fun changIsLoadingState(state: Boolean) {
        _segmentationResult.value = SelfieSegmentationResult(isLoading = state)
    }

    fun clearResult() {
        _segmentationResult.value = SelfieSegmentationResult()
    }

    fun setImage(file: PlatformFile?) {
        _selectedImage.value = file
    }

    override fun onCleared() {
        super.onCleared()
        segmentationProcessor.close()
    }
}
