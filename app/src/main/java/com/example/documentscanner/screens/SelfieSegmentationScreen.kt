package com.example.documentscanner.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.documentscanner.R
import com.example.documentscanner.data.CameraState
import com.example.documentscanner.data.SelfieSegmentationResult
import com.example.documentscanner.viewModels.SelfieSegmentationViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfieSegmentationScreen(onBack: () -> Unit) {
    val viewModel: SelfieSegmentationViewModel = viewModel()
    val file by viewModel.selectedImage.collectAsState()
    val segmentationResult by viewModel.segmentationResult.collectAsState()
    val scope = rememberCoroutineScope()
    if (file != null) {
        LaunchedEffect(Unit) {
            scope.launch {
                val image = file?.toImageBitmap()?.asAndroidBitmap()!!
                viewModel.processImage(image)
                viewModel.changIsLoadingState(true)
                delay(10000)
                Log.d("Debug-image", "Image processed")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Selfie Segmentation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (!segmentationResult.isLoading && segmentationResult.originalBitmap == null) {
                CameraPreviewSection(
                    viewModel = viewModel
                )
            } else {
                SegmentationResultSection(
                    result = segmentationResult,
                    onRetake = {
                        viewModel.clearResult()
                        viewModel.setCameraState(CameraState.PREVIEW)
                    }
                )

            }

            // Error Handling
            segmentationResult.error?.let { error ->
                ErrorMessage(
                    message = error,
                    onDismiss = { viewModel.clearResult() }
                )
            }
        }
    }
}

@Composable
fun CameraPreviewSection(
    viewModel: SelfieSegmentationViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.pick_an_image),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        FilePickerScreen(viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier
            .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter

        ) {
            BannerScreen()
        }
    }
}

@Composable
fun SegmentationResultSection(
    result: SelfieSegmentationResult,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bitmap = result.segmentedBitmap
        if (result.isLoading) {
            CircularProgressIndicator(modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally))
            Text(stringResource(R.string.processing), modifier = Modifier.padding(16.dp))
        } else {
            // Show original and segmented images side by side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Original Image
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.original),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    result.originalBitmap?.let { bitmap ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(bitmap)
                                .build(),
                            contentDescription = stringResource(R.string.original_image),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Segmented Image
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.segmented),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    result.segmentedBitmap.let { bitmap ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(bitmap)
                                .build(),
                            contentDescription = stringResource(R.string.segmented_image),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onRetake) {
                    Text(stringResource(R.string.take_another))
                }
                if (bitmap != null) {
                    SaveImage(bitmap)
                }
            }

            Box(modifier = Modifier
                .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                BannerScreen()
            }
        }
    }
}


@OptIn(ExperimentalTime::class)
@Composable
private fun SaveImage(file: Bitmap) {
    val scop = rememberCoroutineScope()
    Column {
        Button(
            onClick = {
                scop.launch {
                    val input = file.asImageBitmap().encodeToByteArray(ImageFormat.PNG)
                    val name = "seg_${Clock.System.now()}"
                    val output = FileKit.openFileSaver(
                        suggestedName = name,
                        extension = "png"
                    )
                    output?.write(input)
                }
            }
        ) {
            Text(stringResource(R.string.save_the_image))
        }
    }
}


@Composable
fun FilePickerScreen(viewModel: SelfieSegmentationViewModel) {
    val scop = rememberCoroutineScope()
    Column {
        Button(
            onClick = {
                scop.launch {
                    val setting = FileKitDialogSettings.createDefault()
                    val file = FileKit.openFilePicker(type = FileKitType.Image, mode = FileKitMode.Single, dialogSettings = setting)
                    viewModel.setImage(file)
                    Log.d("Set", "File Picked: ${file?.name}")

                }
            }
        ) {
            Text(stringResource(R.string.pick_image))
        }
    }
}
