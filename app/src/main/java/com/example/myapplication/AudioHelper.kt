import android.content.Context
import android.media.AudioManager

class AudioHelper(private val context: Context) {

    fun isAudioPlaying(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMusicActive
    }
}
