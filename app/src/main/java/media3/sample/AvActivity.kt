package media3.sample

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class AvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            100
        )

        setContent {
            AudioCaptureWithVisualizer()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCaptureWithVisualizer() {
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val audioAmplitudes = remember { mutableStateListOf(0f, 0f, 0f) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Audio Visualizer") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Visualizer
                AudioVisualizer(
                    amplitudes = audioAmplitudes,
                    barColor = Color.Cyan,
                    maxHeight = 250f, // Maximum height for bars in dp
                    cornerRadius = 32f // Rounded corners
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Start/Stop Recording Button
                Button(
                    onClick = {
                        isRecording = !isRecording
                        if (isRecording) {
                            coroutineScope.launch(Dispatchers.IO) {
                                startRecording(audioAmplitudes)
                            }
                        }
                    }
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
            }
        }
    )
}

@SuppressLint("MissingPermission")
fun startRecording(audioAmplitudes: MutableList<Float>) {
    // AudioRecord configuration
    val sampleRate = 44100 // Sample rate in Hz
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelConfig,
        audioFormat,
        bufferSize
    )

    val audioData = ShortArray(bufferSize)

    try {
        audioRecord.startRecording()
        while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val readSize = audioRecord.read(audioData, 0, bufferSize)
            if (readSize > 0) {
                // Calculate amplitudes for the visualizer
                val amplitude = calculateAmplitude(audioData)
                audioAmplitudes[0] = amplitude * 0.8f
                audioAmplitudes[1] = amplitude * 0.6f
                audioAmplitudes[2] = amplitude * 0.4f
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        audioRecord.stop()
        audioRecord.release()
    }
}

fun calculateAmplitude(data: ShortArray): Float {
    // Convert raw audio data to an average amplitude
    val maxAmplitude = data.maxOrNull()?.toFloat() ?: 0f
    val normalizedAmplitude = abs(maxAmplitude) / Short.MAX_VALUE // Normalize to 0.0 - 1.0 range
    return normalizedAmplitude * 200f // Scale it for visualizer effect
}
@Composable
fun AudioVisualizer(
    amplitudes: List<Float>,
    barColor: Color,
    maxHeight: Float,
    cornerRadius: Float = 10f // Corner radius for rounded bars
) {
    val barCount = 4 // Set to 4 bars
    val fixedBarWidth = 20f // Set a fixed width for the bars
    val spacing = 10f // Adjust the spacing between bars as needed

    // Ensure that amplitudes have at least 4 values, or provide default values (0f for missing amplitudes)
    val safeAmplitudes = amplitudes.take(barCount) + List(barCount - amplitudes.size) { 0f }

    // Rearranged order of the bars as required
    val reorderedAmplitudes = listOf(
        safeAmplitudes[2],  // 3rd amplitude becomes the 1st bar
        safeAmplitudes[0],  // 1st amplitude becomes the 2nd bar
        safeAmplitudes[2],  // 4th amplitude becomes the 3rd bar
        safeAmplitudes[1]   // 2nd amplitude becomes the 4th bar
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(maxHeight.dp)) {
        // Calculate the total width required for the bars and their spacing
        val totalWidth = fixedBarWidth * barCount + spacing * (barCount - 1)
        // Ensure bars fit within the available space
        val startX = (size.width - totalWidth) / 2 // Center the bars horizontally

        val centerY = size.height / 2 // Centerline for symmetric visualization

        // Iterate over the reordered amplitudes and draw bars
        reorderedAmplitudes.forEachIndexed { index, amplitude ->
            val scaledHeight = (amplitude / 40f) * maxHeight // Scale the height based on amplitude
            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = startX + index * (fixedBarWidth + spacing), // Space the bars correctly
                    y = centerY - (scaledHeight / 2) // Center bars vertically
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = fixedBarWidth,
                    height = scaledHeight
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}
