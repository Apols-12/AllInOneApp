package com.example.documentscanner.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documentscanner.data.TranslationModel
import com.example.documentscanner.data.TranslationResult
import com.example.documentscanner.features.TranslationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslationViewModel() : ViewModel() {

    private val _availableLanguages = MutableStateFlow<List<TranslationModel>>(emptyList())
    val availableLanguages: StateFlow<List<TranslationModel>> = _availableLanguages.asStateFlow()

    private val _translationResult = MutableStateFlow(TranslationResult())
    val translationResult: StateFlow<TranslationResult> = _translationResult.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _downloadedModels = MutableStateFlow<List<TranslationModel>>(emptyList())
    val downloadedModels: StateFlow<List<TranslationModel>> = _downloadedModels.asStateFlow()

    private val translator = TranslationManager()

    init {
        loadAvailableLanguages()
        loadDownloadedModels()
    }

    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            _isInitializing.value = true
            try {
                val languages = translator.getAvailableLanguages()
                _availableLanguages.value = languages
            } catch (e: Exception) {
                // Handle error
                _translationResult.value = TranslationResult(
                    error = "Failed to load languages: ${e.message}"
                )
            } finally {
                _isInitializing.value = false
            }
        }
    }

    private fun loadDownloadedModels() {
        viewModelScope.launch {
            try {
                val models = translator.getDownloadedModels()
                _downloadedModels.value = models
            } catch (e: Exception) {
                Log.d("Load Models", "error loading the language models ${e.message}")
                // Handle error
            }
        }
    }

    fun translateText(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ) {
        viewModelScope.launch {
            _translationResult.value = TranslationResult(
                originalText = text,
                isLoading = true
            )

            try {
                val result = translator.translateText(text, sourceLanguageCode, targetLanguageCode)
                _translationResult.value = TranslationResult(
                    originalText = text,
                    translatedText = result,
                    sourceLanguage = sourceLanguageCode,
                    targetLanguage = targetLanguageCode
                )
            } catch (e: Exception) {
                Log.d("Translation", "error translating ${e.message}")
                _translationResult.value = TranslationResult(
                    originalText = text,
                    error = "Translation failed: Make sure you have downloaded the language",
                    sourceLanguage = sourceLanguageCode,
                    targetLanguage = targetLanguageCode
                )
            }
        }
    }

    fun downloadLanguageModel(languageCode: String) {
        viewModelScope.launch {
            _translationResult.value = TranslationResult(isLoading = true)

            try {
                translator.downloadLanguageModel(languageCode)
                // Refresh both lists
                loadAvailableLanguages()
                loadDownloadedModels()

                _translationResult.value = TranslationResult(
                    translatedText = "Language downloaded successfully!"
                )
            } catch (e: Exception) {
                _translationResult.value = TranslationResult(
                    error = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun deleteLanguageModel(languageCode: String) {
        viewModelScope.launch {
            try {
                val success = translator.deleteLanguageModel(languageCode)
                if (success) {
                    loadAvailableLanguages()
                    loadDownloadedModels()
                    _translationResult.value = TranslationResult(
                        translatedText = "Language deleted successfully!"
                    )
                } else {
                    _translationResult.value = TranslationResult(
                        error = "Failed to delete the language"
                    )
                }
            } catch (e: Exception) {
                _translationResult.value = TranslationResult(
                    error = "Delete failed: ${e.message}"
                )
            }
        }
    }

    fun clearResult() {
        _translationResult.value = TranslationResult()
    }

    override fun onCleared() {
        super.onCleared()
        translator.closeAllTranslators()
    }
}

