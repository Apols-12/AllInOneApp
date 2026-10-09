package com.example.documentscanner.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.documentscanner.R
import com.example.documentscanner.data.ScanResult
import com.example.documentscanner.features.DocumentScanner
import com.example.documentscanner.viewModels.DocumentScannerViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    onNavigateToTranslation: () -> Unit,
    onNavigateToSelfieSegmentation: () -> Unit
) {
    val viewModel: DocumentScannerViewModel = viewModel()
    val context = LocalContext.current

    val documentScanner = remember { DocumentScanner(context) }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            viewModel.setLoading(false)

            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = documentScanner.processScanResult(result.data)
                viewModel.updateScanResult(scanResult)

                if (scanResult.error != null) {
                    viewModel.setError(scanResult.error)
                }
            } else {
                viewModel.setError(context.getString(R.string.scan_cancelled_or_failed))
            }
        }
    )

    val startDocumentScan: () -> Unit = {
        viewModel.setLoading(true)
        viewModel.setError(null)

        val scanner = documentScanner.getDocumentScanner()
        documentScanner.getScanIntent(scanner)
            .addOnSuccessListener { intentSenderRequest ->
                scanLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener { exception ->
                viewModel.setLoading(false)
                viewModel.setError(
                    context.getString(
                        R.string.failed_to_start_scanner,
                        exception.message
                    ))
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.features)) },
                actions = {
                    IconButton(onClick = onNavigateToTranslation) {
                        Icon(Icons.Default.Translate, stringResource(id = R.string.translation))
                    }
                    IconButton(onClick = onNavigateToSelfieSegmentation) {
                        Icon(Icons.Default.Person, stringResource(R.string.selfie_segmentation))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = startDocumentScan) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = stringResource(R.string.scan_document)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Feature selection buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureCard(
                        title = stringResource(R.string.document_scanner),
                        icon = Icons.Default.DocumentScanner,
                        description = stringResource(R.string.scan_documents_to_pdf_jpg),
                        onClick = { /* Already on this screen */ }
                    )

                    FeatureCard(
                        title = stringResource(R.string.translation),
                        icon = Icons.Default.Translate,
                        description = stringResource(R.string.translate_text_between_languages),
                        onClick = onNavigateToTranslation
                    )

                    FeatureCard(
                        title = stringResource(R.string.segmentation),
                        icon = Icons.Default.Person,
                        description = stringResource(R.string.remove_or_change_backgrounds),
                        onClick = onNavigateToSelfieSegmentation
                    )
                }
            }

            //Ad going here

            item {
                DocumentScannerContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DocumentScannerContent(viewModel: DocumentScannerViewModel) {
    val scanResult by viewModel.scanResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Error Message
    errorMessage?.let { message ->
        ErrorMessage(
            message = message,
            onDismiss = { viewModel.setError(null) }
        )
    }

    // Loading Indicator
    if (isLoading) {
        LoadingIndicator()
    }

    // Scan Results
    ScanResultContent(
        scanResult = scanResult,
        onClearResults = { viewModel.clearResults() }
    )

    // Empty State
    if (scanResult == null && !isLoading && errorMessage == null) {
        EmptyState()
    }
}

@Composable
fun FeatureCard(
    title: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(200.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_documents_scanned_yet),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tap_the_scan_button_to_start),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Box(
            modifier = Modifier
            .fillMaxWidth()) {
            BannerScreen()
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
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
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun ScanResultContent(
    scanResult: ScanResult?,
    onClearResults: () -> Unit
) {

    val uri = scanResult?.pdfUri


    scanResult?.let { result ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            // PDF Info
            if (result.pdfUri != null) {
                PdfInfoCard(
                    pageCount = result.pageCount,
                    onClearResults = onClearResults
                )
            }

            // Scanned Images
            if (result.imageUris.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.scanned_pages, result.imageUris.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                result.imageUris.forEach { imageUri ->
                    ScannedImageItem(imageUri = imageUri)
                }
            }

            uri?.let {
                SavePdfPage(it)
            }
            Box(modifier = Modifier
                .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter) {
                BannerScreen()
            }
        }
    }
}

@Composable
fun PdfInfoCard(pageCount: Int, onClearResults: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.pdf_document),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.pages, pageCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onClearResults) {
                    Text(stringResource(R.string.new_scan))
                }
            }
        }
    }
}

@Composable
fun ScannedImageItem(imageUri: Uri) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.scanned_document_page),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Fit
        )
    }
}


@OptIn(ExperimentalTime::class)
@Composable
private fun SavePdfPage(uri: Uri) {
    val scop = rememberCoroutineScope()
    Column {
        Button(
            onClick = {
                scop.launch {
                    val file = PlatformFile(uri.path!!)
                    val name = "pdf_${Clock.System.now()}"
                    val output = FileKit.openFileSaver(
                        suggestedName = name,
                        extension = "pdf"
                    )


                    output?.write(file)
                }
            }
        ) {
            Text(stringResource(R.string.save_the_pdf))
        }
    }
}

@Composable
fun BannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val adSize = AdSize.BANNER

    val adView = remember {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = "ca-app-pub-8738300695122947/1272312509"
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier.wrapContentSize()
            .fillMaxWidth()
    )
}



