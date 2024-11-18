import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Request audio focus
    fun requestAudioFocus(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // Regained focus, resume playback if necessary
                        mediaPlayer?.start()
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // Lost focus for an unbounded amount of time, stop playback
//                        stopAudio()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // Lost focus for a short time, pause playback
//                        mediaPlayer?.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Lost focus for a short time, lower volume
                        mediaPlayer?.setVolume(0.1f, 0.1f)
                    }
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    // Play audio
    fun playAudio(audioResource: Int) {
        if (requestAudioFocus()) {
            mediaPlayer = MediaPlayer.create(context, audioResource)
            mediaPlayer?.start()

            mediaPlayer?.setOnCompletionListener {
                // Release resources after playback completes
                stopAudio()
            }
        }
    }

    // Stop audio and release resources
    fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
    }

    // Release audio focus
    private fun abandonAudioFocus() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
    }
}
