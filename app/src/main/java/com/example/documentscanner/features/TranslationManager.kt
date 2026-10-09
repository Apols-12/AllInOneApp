package com.example.documentscanner.features

import com.example.documentscanner.data.TranslationModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class TranslationManager {

    private val translators = mutableMapOf<String, Translator>()
    private val remoteModelManager = RemoteModelManager.getInstance()
    private var downloadedModels: Set<TranslateRemoteModel>? = null

    suspend fun getAvailableLanguages(): List<TranslationModel> {
        return try {
            val languageCodes = TranslateLanguage.getAllLanguages()
            val downloadedLanguageCodes = getDownloadedLanguageCodes()

            languageCodes.map { code ->
                TranslationModel(
                    languageCode = code,
                    languageName = getLanguageName(code),
                    downloaded = downloadedLanguageCodes.contains(code)
                )
            }.sortedBy { it.languageName }
        } catch (e: Exception) {
            // Fallback to basic language list if ML Kit fails
            getBasicLanguageList()
        }
    }

    suspend fun translateText(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): String {
        val translator = getOrCreateTranslator(sourceLanguageCode, targetLanguageCode)
        return translator.translate(text).await()
    }

    suspend fun downloadLanguageModel(languageCode: String) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        // Create a translator to trigger download
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(languageCode)
            .build()

        val translator = Translation.getClient(options)
        translators[languageCode] = translator

        // Download the model
        translator.downloadModelIfNeeded(conditions).await()

        // Refresh downloaded models cache
        refreshDownloadedModels()
    }

    suspend fun deleteLanguageModel(languageCode: String): Boolean {
        return try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            remoteModelManager.deleteDownloadedModel(model).await()

            // Remove from cache and close translator
            translators[languageCode]?.close()
            translators.remove(languageCode)
            refreshDownloadedModels()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getDownloadedModels(): List<TranslationModel> {
        val downloadedCodes = getDownloadedLanguageCodes()
        return downloadedCodes.map { code ->
            TranslationModel(
                languageCode = code,
                languageName = getLanguageName(code),
                downloaded = true
            )
        }.sortedBy { it.languageName }
    }

    private suspend fun getDownloadedLanguageCodes(): Set<String> {
        if (downloadedModels == null) {
            refreshDownloadedModels()
        }
        return downloadedModels?.map { it.language }?.toSet() ?: emptySet()
    }

    private suspend fun refreshDownloadedModels() {
        downloadedModels = try {
            remoteModelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
        } catch (e: Exception) {
            // If we can't get the downloaded models, assume none are downloaded
            emptySet()
        }
    }

    private fun getOrCreateTranslator(
        sourceLanguage: String,
        targetLanguage: String
    ): Translator {
        val key = "$sourceLanguage-$targetLanguage"
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }
    }

    private fun getLanguageName(languageCode: String): String {
        return try {
            // Convert language code to display name
            when (languageCode) {
                "en" -> "English"
                "es" -> "Spanish"
                "fr" -> "French"
                "de" -> "German"
                "it" -> "Italian"
                "pt" -> "Portuguese"
                "ru" -> "Russian"
                "zh" -> "Chinese"
                "ja" -> "Japanese"
                "ko" -> "Korean"
                "ar" -> "Arabic"
                "hi" -> "Hindi"
                "tr" -> "Turkish"
                "nl" -> "Dutch"
                "pl" -> "Polish"
                "sv" -> "Swedish"
                "da" -> "Danish"
                "fi" -> "Finnish"
                "no" -> "Norwegian"
                "cs" -> "Czech"
                "el" -> "Greek"
                "he" -> "Hebrew"
                "id" -> "Indonesian"
                "th" -> "Thai"
                "vi" -> "Vietnamese"
                "bg" -> "Bulgarian"
                "hr" -> "Croatian"
                "hu" -> "Hungarian"
                "ro" -> "Romanian"
                "sk" -> "Slovak"
                "sl" -> "Slovenian"
                "uk" -> "Ukrainian"
                "af" -> "Afrikaans"
                "sw" -> "Swahili"
                "ca" -> "Catalan"
                "et" -> "Estonian"
                "tl" -> "Filipino"
                "gu" -> "Gujarati"
                "is" -> "Icelandic"
                "kn" -> "Kannada"
                "lv" -> "Latvian"
                "lt" -> "Lithuanian"
                "ms" -> "Malay"
                "ml" -> "Malayalam"
                "mr" -> "Marathi"
                "ne" -> "Nepali"
                "pa" -> "Punjabi"
                "si" -> "Sinhala"
                "sr" -> "Serbian"
                "ta" -> "Tamil"
                "te" -> "Telugu"
                "ur" -> "Urdu"
                "cy" -> "Welsh"
                "eu" -> "Basque"
                "gl" -> "Galician"
                "ga" -> "Irish"
                "mt" -> "Maltese"
                "sq" -> "Albanian"
                "hy" -> "Armenian"
                "az" -> "Azerbaijani"
                "be" -> "Belarusian"
                "bs" -> "Bosnian"
                "eo" -> "Esperanto"
                "ht" -> "Haitian Creole"
                "ha" -> "Hausa"
                "haw" -> "Hawaiian"
                "hmn" -> "Hmong"
                "ig" -> "Igbo"
                "jw" -> "Javanese"
                "kk" -> "Kazakh"
                "km" -> "Khmer"
                "ku" -> "Kurdish"
                "ky" -> "Kyrgyz"
                "lo" -> "Lao"
                "lb" -> "Luxembourgish"
                "mk" -> "Macedonian"
                "mg" -> "Malagasy"
                "my" -> "Myanmar (Burmese)"
                "mn" -> "Mongolian"
                "ny" -> "Chichewa"
                "ps" -> "Pashto"
                "sm" -> "Samoan"
                "gd" -> "Scots Gaelic"
                "st" -> "Sesotho"
                "sn" -> "Shona"
                "sd" -> "Sindhi"
                "so" -> "Somali"
                "su" -> "Sundanese"
                "tg" -> "Tajik"
                "uz" -> "Uzbek"
                "xh" -> "Xhosa"
                "yi" -> "Yiddish"
                "yo" -> "Yoruba"
                "zu" -> "Zulu"
                "fy" -> "Frisian"
                "ka" -> "Georgian"
                "am" -> "Amharic"
                "co" -> "Corsican"
                "iu" -> "Inuktitut"
                "mi" -> "Maori"
                "or" -> "Odia (Oriya)"
                "qu" -> "Quechua"
                "tt" -> "Tatar"
                "tk" -> "Turkmen"
                "ug" -> "Uyghur"
                else -> {
                    // Fallback: capitalize the language code
                    languageCode.replaceFirstChar { it.uppercase() }
                }
            }
        } catch (e: Exception) {
            languageCode
        }
    }

    private fun getBasicLanguageList(): List<TranslationModel> {
        return listOf(
            TranslationModel("en", "English"),
            TranslationModel("es", "Spanish"),
            TranslationModel("fr", "French"),
            TranslationModel("de", "German"),
            TranslationModel("it", "Italian"),
            TranslationModel("pt", "Portuguese"),
            TranslationModel("ru", "Russian"),
            TranslationModel("zh", "Chinese"),
            TranslationModel("ja", "Japanese"),
            TranslationModel("ko", "Korean"),
            TranslationModel("ar", "Arabic"),
            TranslationModel("hi", "Hindi")
        )
    }

    fun closeAllTranslators() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
