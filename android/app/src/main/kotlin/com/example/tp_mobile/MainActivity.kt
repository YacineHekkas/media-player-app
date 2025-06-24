package com.example.tp_mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

import android.content.ContentUris
import androidx.annotation.NonNull
import java.io.File
class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.tp_mobile/audio"
    private var broadcastReceiver: BroadcastReceiver? = null
    private lateinit var mediaRepository: MediaRepository

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        mediaRepository = MediaRepository(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector {
            val toggleIntent = Intent(this, AudioService::class.java).apply {
                action = AudioService.ACTION_TOGGLE
            }
            startService(toggleIntent)
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).apply {
            setMethodCallHandler { call, result ->
                when (call.method) {
                    "getAudioFiles" -> {
                        result.success(mediaRepository.getAudioFiles().map { audioFile ->
                            mapOf(
                                "id" to audioFile.id,
                                "title" to audioFile.title,
                                "artist" to audioFile.artist,
                                "duration" to audioFile.duration,
                                "uri" to audioFile.uri.toString()
                            )
                        })
                    }
                    "playAudio" -> {
                        val uri = call.argument<String>("uri") ?: ""
                        val title = call.argument<String>("title") ?: ""
                        val artist = call.argument<String>("artist") ?: ""
                        val duration = call.argument<Long>("duration") ?: 0L

                        Intent(this@MainActivity, AudioService::class.java).apply {
                            action = AudioService.ACTION_PLAY
                            putExtra("uri", uri)
                            putExtra("title", title)
                            putExtra("artist", artist)
                            putExtra("duration", duration)
                            startService(this)
                        }
                        result.success("Playing audio")
                    }
                    "pauseAudio" -> {
                        Intent(this@MainActivity, AudioService::class.java).apply {
                            action = AudioService.ACTION_PAUSE
                            startService(this)
                        }
                        result.success("Paused audio")
                    }
                    "registerBroadcastReceiver" -> {
                        registerBroadcastReceiver(this)
                        result.success(null)
                    }
                    "unregisterBroadcastReceiver" -> {
                        unregisterBroadcastReceiver()
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause()   {
        super.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            shakeDetector.onSensorChanged(event)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private class ShakeDetector(private val onShake: () -> Unit) {
        private var lastUpdate: Long = 0
        private var lastX: Float = 0.0f
        private var lastY: Float = 0.0f
        private var lastZ: Float = 0.0f

        companion object {
            private const val SHAKE_THRESHOLD = 1000
            private const val UPDATE_INTERVAL = 100
        }

        fun onSensorChanged(event: SensorEvent) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > UPDATE_INTERVAL) {
                val diffTime = (currentTime - lastUpdate)
                lastUpdate = currentTime

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

                if (speed > SHAKE_THRESHOLD) {
                    onShake()
                }

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    private fun registerBroadcastReceiver(methodChannel: MethodChannel) {
        if (broadcastReceiver == null) {
            Log.d("MainActivity", "Registering broadcast receiver")

            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "com.example.tp_mobile.PLAYBACK_STATE_CHANGED") {
                        val isPlaying = intent.getBooleanExtra("isPlaying", false)
                        Log.d("MainActivity", "Received broadcast: isPlaying=$isPlaying")

                        // Invoke method on Flutter side
                        methodChannel.invokeMethod("onPlaybackStateChanged", isPlaying)
                    }
                }
            }

            val filter = IntentFilter("com.example.tp_mobile.PLAYBACK_STATE_CHANGED")
            registerReceiver(broadcastReceiver, filter)
        }
    }

    private fun unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            Log.d("MainActivity", "Unregistering broadcast receiver")
            unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
        }
    }

    override fun onDestroy() {
        unregisterBroadcastReceiver()

        val intent = Intent(this, AudioService::class.java)
        stopService(intent)


        super.onDestroy()
    }
}

class MediaRepository(private val context: Context) {
    data class AudioFile(
        val id: Long,
        val title: String,
        val artist: String,
        val duration: Long,
        val uri: Uri
    )

    fun getAudioFiles(): List<AudioFile> {
        val audioList = mutableListOf<AudioFile>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                audioList.add(AudioFile(id, title, artist, duration, contentUri))
            }
        }
        return audioList
    }
}