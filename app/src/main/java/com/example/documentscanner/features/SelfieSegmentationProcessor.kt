package com.example.documentscanner.features

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import kotlinx.coroutines.tasks.await
import androidx.core.graphics.get
import androidx.core.graphics.scale

class SelfieSegmentationProcessor {

    val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .build()

    private val segmenter by lazy {
        Segmentation.getClient(options)
    }

    suspend fun processImage(bitmap: Bitmap): SegmentationResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val mask = segmenter.process(inputImage).await()
        return processMask(mask, bitmap)
    }

    private fun processMask(mask: SegmentationMask, originalBitmap: Bitmap): SegmentationResult {
        val maskBitmap = createMaskBitmap(mask)
        val segmentedBitmap = applyTransparentBackground(originalBitmap, mask)

        return SegmentationResult(
            segmentedBitmap = segmentedBitmap,
            maskBitmap = maskBitmap
        )
    }

    private fun createMaskBitmap(mask: SegmentationMask): Bitmap {
        val buffer = mask.buffer
        val width = mask.width
        val height = mask.height

        val pixels = FloatArray(width * height)
        buffer.rewind()
        buffer.asFloatBuffer().get(pixels)

        val bitmap = createBitmap(width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val confidence = pixels[y * width + x]
                val alpha = (confidence * 255).toInt()
                val color = Color.argb(alpha, 255, 255, 255)
                bitmap[x, y] = color
            }
        }

        return bitmap
    }

    private fun applyTransparentBackground(original: Bitmap, mask: SegmentationMask): Bitmap {
        val buffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height

        // Create a new ARGB bitmap for the result
        val scaledBitmap = original.scale(maskWidth, maskHeight)
        val result = createBitmap(maskWidth, maskHeight)

        buffer.rewind()

        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                // Gets the confidence of the (x,y) pixel in the mask being in the foreground.
                val maskVal = buffer.float
                val pixel = scaledBitmap[x, y]

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if (maskVal > 0.5) {
                    result[x, y] = Color.argb(255, r, g, b)
                } else {
                    result[x, y] = Color.TRANSPARENT
                }

            }
        }
        return result
    }


    fun close() {
        segmenter.close()
    }
}

data class SegmentationResult(
    val segmentedBitmap: Bitmap? = null,
    val maskBitmap: Bitmap? = null
)
