package com.sagar.workmanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class PhotoCompressionWorker(
    private val context: Context,
    private val params: WorkerParameters
): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { // Executed when we want to run worker

        return withContext(Dispatchers.IO) {
            val stringUri = params.inputData.getString(CONTENT_URI_KEY)
            val compressionThresholdInBytes = params.inputData.getLong(COMPRESSED_THRESHOLD_KEY, 0L)

            val uri = Uri.parse(stringUri)
            val byteArray = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: return@withContext Result.failure()

            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            var outputBytes: ByteArray
            var quality = 100  // Initial quality
            do {
                val outputStream = ByteArrayOutputStream()
                outputStream.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality,outputStream)
                    outputBytes = outputStream.toByteArray()
                    quality -= (quality * 0.1).roundToInt() // subtract 10% from current quality
                }
            } while (outputBytes.size > compressionThresholdInBytes && quality > 5)

            val file = File(context.cacheDir, "${params.id}.jpg")
            file.writeBytes(outputBytes)

            Result.success(
                workDataOf(
                    CONTENT_PATH_KEY to file.absolutePath
                )
            )
        }

    }

    companion object {
        const val CONTENT_URI_KEY = "CONTENT_URI_KEY"
        const val COMPRESSED_THRESHOLD_KEY = "COMPRESSED_THRESHOLD_KEY"
        const val CONTENT_PATH_KEY = "CONTENT_PATH_KEY"
    }
}