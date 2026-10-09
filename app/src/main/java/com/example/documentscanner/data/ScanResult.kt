package com.example.documentscanner.data

import android.graphics.Bitmap
import android.net.Uri

data class ScanResult(
    val imageUris: List<Uri> = emptyList(),
    val pdfUri: Uri? = null,
    val pageCount: Int = 0,
    val error: String? = null
)

data class TranslationModel(
    val languageCode: String,
    val languageName: String,
    val downloaded: Boolean = false
)

data class TranslationResult(
    val originalText: String = "",
    val translatedText: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
    val error: String? = null,
    val isLoading: Boolean = false
)

data class SelfieSegmentationResult(
    val originalBitmap: Bitmap? = null,
    val segmentedBitmap: Bitmap? = null,
    val maskBitmap: Bitmap? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)

sealed class CameraState {
    object IDLE : CameraState()
    object PREVIEW : CameraState()
}
