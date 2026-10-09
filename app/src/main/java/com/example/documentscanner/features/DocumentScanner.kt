package com.example.documentscanner.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import com.example.documentscanner.data.ScanResult
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.net.URI

class DocumentScanner(
    private val context: Context
) {

    fun getDocumentScanner(): GmsDocumentScanner {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        return GmsDocumentScanning.getClient(options)
    }

    fun getScanIntent(scanner: GmsDocumentScanner): Task<IntentSenderRequest> {
        return scanner.getStartScanIntent(context as Activity)
            .continueWith { task ->
                val intentSender = task.result
                IntentSenderRequest.Builder(intentSender).build()
            }
    }

    fun processScanResult(intent: Intent?): ScanResult {
        return try {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(intent)

            val imageUris = result?.pages?.map { it.imageUri } ?: emptyList()
            val pdf = result?.pdf
            val pdfUri = pdf?.uri
            val pageCount = pdf?.pageCount ?: 0

            ScanResult(
                imageUris = imageUris,
                pdfUri = pdfUri,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            ScanResult(error = "Failed to process scan result: ${e.message}")
        }
    }
}
