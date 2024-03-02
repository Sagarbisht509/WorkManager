package com.sagar.workmanager

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
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

                ScreenContent(viewModel)
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

@Composable
fun ScreenContent(viewModel: PhotoViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
        viewModel.uncompressedUri?.let { uri ->
            ImageWithHeading(
                heading = "Uncompressed Image",
                uri = uri
            )
        }
        viewModel.compressedBitmap?.let { bitmap ->
            ImageWithHeading(
                heading = "Compressed Image",
                bitmap = bitmap
            )
        }
    }
}

@Composable
fun ImageWithHeading(
    heading: String = "",
    uri: Uri? = null,
    bitmap: Bitmap? = null
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = heading)
        Spacer(modifier = Modifier.height(5.dp))
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = heading, modifier = Modifier.fillMaxWidth().height(400.dp))
        } else {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = heading, modifier = Modifier.fillMaxWidth().height(400.dp))
        }
    }
}










