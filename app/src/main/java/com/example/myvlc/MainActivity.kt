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
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.myvlc.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var libVLC: LibVLC
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: Button
    private lateinit var currentTimeText: TextView
    private lateinit var durationText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isSeeking = false

    private lateinit var binding: ActivityMainBinding

    companion object {
        // OGG
        val ogg: String = "https://www.sfxlibrary.com/data/sounds/69/69.ogg"
        val ogg2: String = "https://dn720200.ca.archive.org/0/items/FREE_background_music_dhalius/BackgroundMusica2.ogg"
        val ogg3: String = "https://commondatastorage.googleapis.com/codeskulptor-demos/pyman_assets/ateapill.ogg"
        val ogg4: String = "https://storage.googleapis.com/nc_webrtc_recording/78974d5f-55a8-4469-85a8-e81002001b05/38e6718d-4fd2-4bd0-97e7-6d780faec7c5/audio.ogg?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=nativecamp-speech-to-text%40nativecamp-91104.iam.gserviceaccount.com%2F20250806%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20250806T014653Z&X-Goog-Expires=21600&X-Goog-SignedHeaders=host&X-Goog-Signature=8ff40076c6d810f96a1f84f697a27e8ff8194e94bf2f1572062433d5796c4161ab84c18190838c0186d172488052f66182ba50486b1061df0832b3d307dd9ff3195f227a0de2dcdd7a4f72960707437a9cdb029389c50659e16ba2817d4b6e7d1aa79bd0a247b8849adc56a07bf6b8f096370e801bd04f32e906c32726f1d62b10cb156d1f967ac17e4dbeec405b1c03f145ff35923af416db612ae5d26c2d9bfecee99b7099bd04b51efc620a011571559cc8a7cb48848293d52c51a51f4e997c1a285f852fedeb4d08e6740bb0a08f46f9dd76da77024fb9344075c82ff53765455d4df102e72857f134edc12434ba3123493f80803a7142ce24027e3b2e22"
        val ogg5: String = "https://storage.googleapis.com/nc_webrtc_recording/78974d5f-55a8-4469-85a8-e81002001b05/69613a02-297f-41a6-8df5-75843065d568/audio.ogg?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=nativecamp-speech-to-text%40nativecamp-91104.iam.gserviceaccount.com%2F20250807%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20250807T031319Z&X-Goog-Expires=21600&X-Goog-SignedHeaders=host&X-Goog-Signature=b49275cdc9d002be957b652ec36206742a96fe2e4b2a41452f69d30cc9e06722b383966ec004d618ca04e43aac20903abd63127cfadd4dde0e4d16fb1eddaa5929483f81b0189b871d1967aceabf9e30ee6c22ab5cda1be225c5387f6ca5c0014a743e400e3a90b0b346947f3f251783f115f8f3d2bae98c0add9dc4cb19edef57ff8d0d7ab90b620ae3b8729c284b7d5f2cd9cf11293afd9337c97e30b47e155020fbc6ce1cd04b09c96c976dc537a6485111375dd6ac997b2041ab3a3e99e1166b416b48540ca4094533ff8096738efcd454ffdc6514a80120637b2488eac8b0fb5f50c88b0f76e9af16d98c5e4fcb7833074f37b54d6a39e149d570aeacfd"
        val ogg6: String = "https://storage.googleapis.com/nc_webrtc_recording/78974d5f-55a8-4469-85a8-e81002001b05/408444dc-2393-4b80-9189-86d261a8e55a/audio.ogg?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=nativecamp-speech-to-text%40nativecamp-91104.iam.gserviceaccount.com%2F20250808%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20250808T023210Z&X-Goog-Expires=21600&X-Goog-SignedHeaders=host&X-Goog-Signature=752c9707b9cb0d26be7be41b643cd3a4fad184ff24a245298e266e7a9a9364858d7335e897357d66b3e03c0f98ba9da7431a8f9485ad3f34744dc69c794ee4eefa2d33fa8725f33a335a4665f2680c0a64e369538305dd58a620e53c7ffc6c020b8db93510ebb0b43ed4ded3f89b4b9bfac83f838331a54d10cbac65d2088d39402b3f7083c86facb0f6e939e5b6e376661f03bbcebdf351d5e137871acdd5bb1fabcb94e90e18b2d7117c64739f96381e855e662bb1457cb0d2e4db88c1b6c8a2952e83af5da5bfaab14d0a3a5d10970fa17b9650549c77785980ca094a7ad3d89913b2244c8e97c10ead8978641d416fb4bd25b6fd681c705f1cd64bb00465"

        // MP3
        val mp3: String = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.main)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        convertOggToWav(ogg6)

    }

    private fun init(filePath: String) {

        libVLC = LibVLC(this)
        mediaPlayer = MediaPlayer(libVLC)

        // Locally
//        val oggFile = copyAssetToInternalStorage(this, "input.wav")
//        val media = Media(libVLC, Uri.fromFile(oggFile))

        // Network
        val media = Media(libVLC, filePath.toUri().toString())
//        media.addOption(":input-fast-seek")
//        media.addOption(":no-audio-time-stretch")
//        media.addOption(":file-caching=1000")  // Increase caching time for smoother seeks
//        media.addOption(":no-sout-keep")
//        media.addOption(":no-drop-late-frames")

        mediaPlayer.media = media
//        media.release()

        Log.d("VLCPLAYER", "MEDIA DATA = ${media.duration}  ---- ${mediaPlayer.isSeekable}")
        binding.seekBar.max = 100

        mediaPlayer.setEventListener { event ->
            Log.d("VLCPLAYER", "${event.type}")
            when (event.type) {
                MediaPlayer.Event.Opening -> {
                    Log.d("VLCPLAYER", "Opening")
                }
                MediaPlayer.Event.LengthChanged -> {
                    Log.d("VLCPLAYER", "LengthChanged ${event.lengthChanged}")
                    runOnUiThread {
                        binding.duration.text = formatTime(mediaPlayer.length)
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

                        binding.seekBar.progress = ((mediaPlayer.time.toFloat() / mediaPlayer.length.toFloat()) * 100).toInt()
                        Log.d("VLCPLAYER", "PLAYER TIME PROGRESS = ${binding.seekBar.progress}")
                        binding.currentTime.text = formatTime(mediaPlayer.time)
                    }
                }
                //  Saved to /data/user/0/com.example.myvlc/files/output.mp3
                MediaPlayer.Event.EndReached -> {
                    Log.d("VLCPLAYER", "EndReached ${mediaPlayer.media}")
                    binding.playPauseButton.text = "Play"

                    runOnUiThread {

                        binding.currentTime.text = formatTime(mediaPlayer.time)
                    }
                }

                MediaPlayer.Event.EncounteredError -> {
                    Log.d("VLCPLAYER", "EncounteredError")
                    Log.d("VLCPLAYER", "Error Occured!")
                }
            }
        }

        binding.playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                binding.playPauseButton.text = "Play"
            } else {
                try {
                    mediaPlayer.play()

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                binding.playPauseButton.text = "Pause"
            }
        }

        binding.rewind.setOnClickListener {
            mediaPlayer.pause()
            val newTime = mediaPlayer.time - 3000L
//            mediaPlayer.position = (newTime.toFloat() / mediaPlayer.length)
            mediaPlayer.time = newTime
            mediaPlayer.play()
        }

        binding.forward.setOnClickListener {
            mediaPlayer.pause()
            val newTime = mediaPlayer.time + 3000L
//            mediaPlayer.position = (newTime.toFloat() / mediaPlayer.length)
            mediaPlayer.time = newTime
            mediaPlayer.play()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val timeInMs = ((binding.seekBar.progress / 100f) * mediaPlayer.length).toFloat()
                val posAsFloat = (binding.seekBar.progress / 100f)

                //mediaPlayer.pause()
                mediaPlayer.position = posAsFloat // fixes the weird seeking backward behavior of OGG file

                Handler(Looper.getMainLooper()).post {
                    mediaPlayer.play()
                }

                Log.d("VLCPLAYER", "onStopTrackingTouch = ${mediaPlayer.time}")
                isSeeking = false
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("VLCPLAYER", "seekbar max = ${binding.seekBar.max}")
                Log.d("VLCPLAYER", "seekbar min = ${binding.seekBar.min}")

                Log.d("VLCPLAYER", "onProgressChanged1 = ${binding.seekBar.progress}")

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
                    binding.currentTime.text = formatTime(timeInMs)
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

    private fun convertOggToWav(oggUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Download OGG file
                val oggFile = File(cacheDir, "input.ogg")
                downloadFile(oggUrl, oggFile)

                // /storage/emulated/0/Android/data/com.example.myvlc/files/output.mp3
                // Step 2: Output path
                val outputFile = File(getExternalFilesDir(null), "output.m4a")

                // Step 3: FFmpeg command
                //val ffmpegCommand = "-y -i ${oggFile.absolutePath} ${outputWav.absolutePath}"
                val ffmpegCommand = "-y -i ${oggFile.absolutePath} -codec:a libmp3lame -qscale:a 2 ${outputFile.absolutePath}"

                // Step 4: Run FFmpeg
//                val session = FFmpegKit.execute(ffmpegCommand)
//
//                if (session.returnCode.isValueSuccess) {
//                    Log.d("FFmpeg", "Conversion success! Saved to ${outputFile.absolutePath}")
//
//                    init(outputFile.absolutePath)
//                } else {
//                    Log.e("FFmpeg", "Conversion failed: ${session.failStackTrace}")
//                }

                val command = arrayOf(
                    "-i", oggFile.absolutePath,
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-fflags", "+genpts",
                    "-avoid_negative_ts", "make_zero",
                    "-y",
                    outputFile.absolutePath // must end with .m4a
                ).joinToString(" ")

                FFmpegKit.executeAsync(command) { session ->
                    val returnCode = session.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        Log.d("FFmpeg", "✅ Converted: ${outputFile.absolutePath}")
                        init(outputFile.absolutePath)
                    } else {
                        Log.e("FFmpeg", "❌ Failed: ${session.failStackTrace}")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun downloadFile(urlString: String, outputFile: File) {
        val url = URL(urlString)
        val connection = url.openConnection()
        connection.connect()

        val input: InputStream = connection.getInputStream()
        val output = FileOutputStream(outputFile)

        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }

        output.flush()
        output.close()
        input.close()
    }
}
