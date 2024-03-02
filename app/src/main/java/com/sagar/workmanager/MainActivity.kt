package com.sagar.workmanager

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sagar.workmanager.ui.theme.WorkManagerTheme

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager
    private val viewModel by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        workManager = WorkManager.getInstance(applicationContext)

        setContent {
            WorkManagerTheme {

                val workerResult = viewModel.workId?.let { id ->
                    workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                }

                LaunchedEffect(key1 = workerResult?.outputData) {
                    if (workerResult?.outputData != null) {
                        val filePath = workerResult.outputData.getString(
                            PhotoCompressionWorker.CONTENT_PATH_KEY
                        )

                        filePath?.let { file ->
                            val bitmap = BitmapFactory.decodeFile(file)
                            viewModel.updateCompressedBitmap(bitmap)
                        }
                    }
                }

            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // 33
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return

        viewModel.updateUncompressedUri(uri)

        val workRequest = OneTimeWorkRequestBuilder<PhotoCompressionWorker>()
            .setInputData(
                workDataOf(
                    PhotoCompressionWorker.CONTENT_URI_KEY to  uri.toString(),
                    PhotoCompressionWorker.COMPRESSED_THRESHOLD_KEY to 1024 * 20L
                )
            )
            .setConstraints(Constraints(
                requiresStorageNotLow = true
            ))
            .build()

        viewModel.updateWorkId(workRequest.id)

        workManager.enqueue(workRequest)
    }
}