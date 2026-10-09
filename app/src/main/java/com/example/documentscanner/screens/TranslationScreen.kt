package com.example.documentscanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documentscanner.R
import com.example.documentscanner.data.TranslationModel
import com.example.documentscanner.data.TranslationResult
import com.example.documentscanner.viewModels.TranslationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(onBack: () -> Unit) {

    val viewModel: TranslationViewModel = viewModel()
    val languages by viewModel.availableLanguages.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val downloadedModels by viewModel.downloadedModels.collectAsState()

    var sourceText by remember { mutableStateOf("") }
    var selectedSourceLanguage by remember { mutableStateOf("en") }
    var selectedTargetLanguage by remember { mutableStateOf("es") }
    var showDownloadedModels by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.text_translation)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDownloadedModels = true }) {
                        Icon(Icons.Default.Storage, stringResource(R.string.downloaded_models))
                        if (downloadedModels.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(
                                            text = downloadedModels.size.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            ) {
                                // Empty content, badge is shown on the icon
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showDownloadedModels) {
            DownloadedModelsDialog(
                downloadedModels = downloadedModels,
                onDeleteModel = { languageCode ->
                    viewModel.deleteLanguageModel(languageCode)
                },
                onDismiss = { showDownloadedModels = false }
            )
        } else {
            TranslationContent(
                languages = languages,
                modifier = Modifier.padding(paddingValues),
                translationResult = translationResult,
                isInitializing = isInitializing,
                sourceText = sourceText,
                selectedSourceLanguage = selectedSourceLanguage,
                selectedTargetLanguage = selectedTargetLanguage,
                onSourceTextChange = { sourceText = it },
                onSourceLanguageChange = { selectedSourceLanguage = it },
                onTargetLanguageChange = { selectedTargetLanguage = it },
                onTranslate = {
                    if (sourceText.isNotBlank()) {
                        viewModel.translateText(
                            sourceText,
                            selectedSourceLanguage,
                            selectedTargetLanguage
                        )
                    }
                },
                onDownloadModel = { languageCode ->
                    viewModel.downloadLanguageModel(languageCode)
                },
                onClear = {viewModel.clearResult()}
            )
        }
    }
}

@Composable
fun TranslationContent(
    modifier: Modifier = Modifier,
    languages: List<TranslationModel>,
    translationResult: TranslationResult,
    isInitializing: Boolean,
    sourceText: String,
    selectedSourceLanguage: String,
    selectedTargetLanguage: String,
    onSourceTextChange: (String) -> Unit,
    onSourceLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onDownloadModel: (String) -> Unit,
    onClear: () -> Unit
) {
    @Suppress("deprecation")
    val clipboardManager = LocalClipboardManager.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.translate_text),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Source Language
                    Text(stringResource(R.string.from), style = MaterialTheme.typography.bodyMedium)
                    LanguageDropdown(
                        languages = languages,
                        selectedLanguage = selectedSourceLanguage,
                        onLanguageSelected = onSourceLanguageChange,
                        onDownloadModel = onDownloadModel
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Language
                    Text(stringResource(R.string.to), style = MaterialTheme.typography.bodyMedium)
                    LanguageDropdown(
                        languages = languages,
                        selectedLanguage = selectedTargetLanguage,
                        onLanguageSelected = onTargetLanguageChange,
                        onDownloadModel = onDownloadModel
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Input
                    OutlinedTextField(
                        value = sourceText,
                        onValueChange = onSourceTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        label = { Text(stringResource(R.string.text_to_translate)) },
                        placeholder = { Text(stringResource(R.string.enter_text_to_translate)) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Translate Button
                    Button(
                        onClick = onTranslate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = sourceText.isNotBlank() && !translationResult.isLoading
                    ) {
                        if (translationResult.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.translating))
                        } else {
                            Text(stringResource(R.string.translate))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.please_download))
                }
            }
        }

        // Translation Result
        if (translationResult.translatedText.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.translation_result),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = translationResult.translatedText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(translationResult.translatedText))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.copy_to_clipboard))
                        }
                    }
                }
            }
        }

        // Error Message
        translationResult.error?.let { error ->
            item {
                ErrorMessages(
                    message = error,
                    onDismiss = { onClear() }
                )
            }
        }

        // Loading for initial language load
        if (isInitializing) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        item {
            Box(modifier = Modifier
                .fillMaxWidth()
            ) {
                BannerScreen()
            }
        }
    }
}

@Composable
fun LanguageDropdown(
    languages: List<TranslationModel>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDownloadModel: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            val selectedLang = languages.find { it.languageCode == selectedLanguage }
            Text(
                text = selectedLang?.languageName ?: selectedLanguage
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.languageName,
                                modifier = Modifier.weight(1f)
                            )

                            if (!language.downloaded) {
                                IconButton(
                                    onClick = {
                                        onDownloadModel(language.languageCode)
                                        expanded = false
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        stringResource(R.string.download),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    stringResource(R.string.downloaded),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onLanguageSelected(language.languageCode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DownloadedModelsDialog(
    downloadedModels: List<TranslationModel>,
    onDeleteModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.downloaded_languages)) },
        text = {
            Column {
                if (downloadedModels.isEmpty()) {
                    Text(
                        stringResource(R.string.no_language_models),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn {
                        items(downloadedModels) { model ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = model.languageName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    IconButton(
                                        onClick = { onDeleteModel(model.languageCode) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun ErrorMessages(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
