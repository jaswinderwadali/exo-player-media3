package media3.sample

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import media3.sample.ui.theme.Media3SampleTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Media3SampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    // State to hold the URL entered by the user
    val urlState = remember { mutableStateOf("https://dev.bot.touchkin.com/test2") }
    // State to track buffering progress
    var isBuffering by remember { mutableStateOf(false) }
    // State to track time difference (in milliseconds)
    var timeDifference by remember { mutableStateOf(0L) }
    // State to track whether the audio has started playing
    var isPlayingStarted by remember { mutableStateOf(false) }

    // State to hold the ExoPlayer instance
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    // Variable to hold the time when the user clicked "Play"
    var playButtonClickTime by remember { mutableStateOf(0L) }

    val context = LocalContext.current

    // Box layout to center the UI
    Box(
        modifier = modifier
            .fillMaxSize() // Fill the whole screen
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // TextField to get URL input from the user
            TextField(
                value = urlState.value,
                onValueChange = { urlState.value = it },
                label = { Text("Enter WAV URL") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Button to play the media from the entered URL
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val url = urlState.value
                    if (url.isNotEmpty()) {
                        playButtonClickTime = System.currentTimeMillis() // Capture the time when the button is clicked
                        // If player is null, create a new one
                        if (player == null) {
                            player = ExoPlayer.Builder(context).build()
                        }
                        play(context, url, player!!, { isBuffering = true }, { isBuffering = false }, { timeDifference = System.currentTimeMillis() - playButtonClickTime; isPlayingStarted = true })
                    }
                }
            ) {
                Text(text = "Play")
            }

            // Button to stop the media
            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                onClick = {
                    player?.stop() // Stop the player
                    player?.seekTo(0) // Optionally seek to the start if needed
                    isPlayingStarted = false
                    timeDifference = 0
                    isBuffering = false
                }
            ) {
                Text(text = "Stop")
            }

            // Show buffering indicator if the media is buffering
            if (isBuffering) {
                CircularProgressIndicator()
            }

            // Show the time difference (in milliseconds) once playback starts
            if (isPlayingStarted) {
                Text(text = "Time to start playing: $timeDifference ms")
            }
        }
    }
}

fun play(
    context: Context,
    url: String,
    player: ExoPlayer,
    onBufferingStarted: () -> Unit,
    onBufferingEnded: () -> Unit,
    onPlayingStarted: () -> Unit
) {
    // Setup the media item
    val mediaItem = MediaItem.fromUri(url)
    player.setMediaItem(mediaItem)

    // Add a listener to track buffering and playback events
    player.addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    onBufferingStarted() // Start showing buffering indicator
                }
                Player.STATE_READY -> {
                    onBufferingEnded() // Hide buffering indicator
                    onPlayingStarted() // Track when playback actually starts
                }
                Player.STATE_ENDED -> {
                    // Handle media end if needed
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Optionally, you could also handle changes in the playing state here
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            // Handle error if needed
        }
    })

    // Prepare and play the media item
    player.prepare()
    player.play()
}
