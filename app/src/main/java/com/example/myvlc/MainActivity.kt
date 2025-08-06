package com.example.myvlc

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.io.FileOutputStream

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
        val ogg2: String = "https://dn720200.ca.archive.org/0/items/FREE_background_music_dhalius/BackgroundMusica2.ogg"
        val ogg3: String = "https://commondatastorage.googleapis.com/codeskulptor-demos/pyman_assets/ateapill.ogg"
        //"https://upload.wikimedia.org/wikipedia/commons/c/c8/Example.ogg"

        // MP3
        val mp3: String = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        seekBar = findViewById(R.id.seekBar)
        playPauseButton = findViewById(R.id.playPauseButton)
        currentTimeText = findViewById(R.id.currentTime)
        durationText = findViewById(R.id.duration)

        val args = arrayListOf(
            "--codec=avcodec",             // force software decoding
            //"--no-mediacodec",             // disable MediaCodec completely
            "--no-audio-time-stretch",     // optional: disable pitch preservation
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--verbose=2"
        )

        libVLC = LibVLC(this, args)
        mediaPlayer = MediaPlayer(libVLC)

        val oggFile = copyAssetToInternalStorage(this, "ateapill.ogg")

        val media = Media(libVLC, Uri.fromFile(oggFile))

        //val media = Media(libVLC, ogg3.toUri())
        mediaPlayer.media = media
        media.release()

        Log.d("VLCPLAYER", "MEDIA DATA = ${media.duration}  ---- ${mediaPlayer.isSeekable}")
        seekBar.max = 100

        mediaPlayer.setEventListener { event ->
            Log.d("VLCPLAYER", "${event.type}")
            when (event.type) {
                MediaPlayer.Event.Opening -> {
                    Log.d("VLCPLAYER", "Opening")
                }
                MediaPlayer.Event.LengthChanged -> {
                    Log.d("VLCPLAYER", "LengthChanged ${event.lengthChanged}")
                    runOnUiThread {
                        durationText.text = formatTime(mediaPlayer.length)
                    }
                }

                MediaPlayer.Event.TimeChanged -> {
                    Log.d("VLCPLAYER", "TimeChanged")
                    Log.d("VLCPLAYER", "MEDIA DATA = ${media.duration}  ---- ${mediaPlayer.isSeekable}")

                    Log.d("VLCPLAYER", "PLAYER TIME = ${mediaPlayer.time}")
                    Log.d("VLCPLAYER", "PLAYER LENGTH = ${media.duration}")
                    runOnUiThread {
                        /**
                         * Normalize.
                         * a = player position
                         * b = player duration/length
                         * c = 100
                         * progress = (a * b) / c
                         */

                        seekBar.progress = ((mediaPlayer.time.toFloat() / mediaPlayer.length.toFloat()) * 100).toInt()
                        Log.d("VLCPLAYER", "PLAYER TIME PROGRESS = ${seekBar.progress}")
                        currentTimeText.text = formatTime(mediaPlayer.time)
                    }
                }

                MediaPlayer.Event.EndReached -> {
                    Log.d("VLCPLAYER", "EndReached ${mediaPlayer.media}")
                    playPauseButton.text = "Play"
                    mediaPlayer.stop()
                }

                MediaPlayer.Event.EncounteredError -> {
                    Log.d("VLCPLAYER", "EncounteredError")
                    Log.d("VLCPLAYER", "Error Occured!")
                }
            }
        }

        playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                playPauseButton.text = "Play"
            } else {
                try {
                    mediaPlayer.play()

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                playPauseButton.text = "Pause"
            }
        }


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val timeInMs = ((seekBar.progress / 100f) * mediaPlayer.length).toLong()
                mediaPlayer.time = timeInMs

                Log.d("VLCPLAYER", "onStopTrackingTouch = ${mediaPlayer.time}")
                isSeeking = false
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("VLCPLAYER", "seekbar max = ${seekBar.max}")
                Log.d("VLCPLAYER", "seekbar min = ${seekBar.min}")

                Log.d("VLCPLAYER", "onProgressChanged1 = ${seekBar.progress}")

                /**
                 * Normalize.
                 * a = progress
                 * b = 100f
                 * c = player duration/length
                 *
                 * ms = (a * b) / c
                 */
                val timeInMs = ((progress / 100f) * mediaPlayer.length).toLong()
                Log.d("VLCPLAYER", "onProgressChanged2 = $timeInMs")

                if (fromUser) {
                    currentTimeText.text = formatTime(timeInMs)
                }
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

    private fun copyAssetToInternalStorage(context: Context, assetName: String): File {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVLC.release()
    }
}
