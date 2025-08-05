package com.example.myvlc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var libVLC: LibVLC
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: Button
    private lateinit var currentTimeText: TextView
    private lateinit var durationText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isSeeking = false

    companion object {
        // OGG
        val ogg: String = "https://www.sfxlibrary.com/data/sounds/69/69.ogg"
        //"https://upload.wikimedia.org/wikipedia/commons/c/c8/Example.ogg"

        // MP3
        val mp3: String = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seekBar = findViewById(R.id.seekBar)
        playPauseButton = findViewById(R.id.playPauseButton)
        currentTimeText = findViewById(R.id.currentTime)
        durationText = findViewById(R.id.duration)

        val args = ArrayList<String>()
        libVLC = LibVLC(this, args)
        mediaPlayer = MediaPlayer(libVLC)

        val media = Media(libVLC, mp3.toUri())
        media.parse()
        mediaPlayer.media = media
        media.release()

        Log.d("VLCPLAYER", "MEDIA LENGTH = ${media.duration}  ---- ${media.isParsed}")
        seekBar.max = mediaPlayer.length.toInt()

        Log.d("VLCPLAYER", "seekbar max = ${seekBar.max}")
        Log.d("VLCPLAYER", "seekbar min = ${seekBar.min}")

        mediaPlayer.setEventListener { event ->
            Log.d("VLCPLAYER", "${event.type}")
            when (event.type) {
                MediaPlayer.Event.LengthChanged -> {
                    runOnUiThread {
                        seekBar.max = mediaPlayer.length.toInt()
                        durationText.text = formatTime(mediaPlayer.length)
                    }
                }

                MediaPlayer.Event.TimeChanged -> {
                    Log.d("VLCPLAYER", "PLAYER TIME = ${getCurrentTime()}")
                    runOnUiThread {
                        seekBar.progress = mediaPlayer.time.toInt()
                        currentTimeText.text = formatTime(mediaPlayer.time)
                    }
                }

                MediaPlayer.Event.EndReached -> {
                    playPauseButton.text = "Play"
                }

                MediaPlayer.Event.EncounteredError -> {
                    Log.d("VLCPLAYER", "Error Occured!")
                }
            }
        }

        playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                playPauseButton.text = "Play"
            } else {
                mediaPlayer.play()
                playPauseButton.text = "Pause"
            }
        }


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val pos = seekBar.progress * 1000L
                mediaPlayer.time = pos
                isSeeking = false
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("VLCPLAYER", "onProgressChanged = $progress")
                currentTimeText.text = formatTime(progress * 1000L)
            }
        })
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private var lastPlayTime: Long = 0
    private var lastPlayTimeGlobal: Long = 0

    /**
     * Get current play time (interpolated)
     * @see https://github.com/caprica/vlcj/issues/74
     *
     * @return
     */
    fun getCurrentTime(): Float {
        var currentTime: Long = mediaPlayer.time

        if (lastPlayTime == currentTime && lastPlayTime != 0L) {
            currentTime += System.currentTimeMillis() - lastPlayTimeGlobal
        } else {
            lastPlayTime = currentTime
            lastPlayTimeGlobal = System.currentTimeMillis()
        }

        return currentTime * 0.001f //to float
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVLC.release()
    }
}
