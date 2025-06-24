package com.example.tp_mobile

import java.io.File
import java.io.FileOutputStream
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.session.MediaSession
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.graphics.BitmapFactory
import android.content.ContentUris
import androidx.annotation.NonNull

class AudioService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentDuration: Long = 0

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "AudioServiceChannel"
        const val ACTION_PLAY = "com.example.tp_mobile.PLAY"
        const val ACTION_PAUSE = "com.example.tp_mobile.PAUSE"
        const val ACTION_TOGGLE = "com.example.tp_mobile.TOGGLE"

    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initMediaSession()
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setOnCompletionListener {
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                stopForeground(true)
                stopSelf()
            }
            setOnPreparedListener {
                start()
                isPlaying = true
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                updateMediaSessionMetadata()
                startForeground(NOTIFICATION_ID, createNotification())
                updatePlaybackStateUI(true)
            }
            setOnErrorListener { _, what, extra ->
                Log.e("AudioService", "Media error: $what, $extra")
                true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> if (isPlaying) pauseAudio() else playAudio(intent)
            ACTION_PLAY -> playAudio(intent)
            ACTION_PAUSE -> pauseAudio()
        }
        return START_STICKY
    }

    private fun playAudio(intent: Intent) {
        intent.getStringExtra("uri")?.let { uriString ->
            val uri = Uri.parse(uriString)
            currentTitle = intent.getStringExtra("title") ?: ""
            currentArtist = intent.getStringExtra("artist") ?: ""
            currentDuration = intent.getLongExtra("duration", 0L)

            mediaPlayer?.apply {
                reset()
                try {
                    setDataSource(applicationContext, uri)
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e("AudioService", "Error setting data source: ${e.message}")
                    abandonAudioFocus()
                }
            }
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        startForeground(NOTIFICATION_ID, createNotification())
        updatePlaybackStateUI(false)
        abandonAudioFocus()
    }

    private fun initMediaSession() {
        Log.d("AudioService", "Initializing MediaSession")

        mediaSession = MediaSessionCompat(this, "AudioService")

        // Set session activity (for when notification is clicked)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        mediaSession.setSessionActivity(sessionActivityPendingIntent)

        // Set media session callback
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                Log.d("AudioService", "MediaSession callback: onPlay")
                playAudio()
            }

            override fun onPause() {
                Log.d("AudioService", "MediaSession callback: onPause")
                pauseAudio()
            }

            override fun onStop() {
                Log.d("AudioService", "MediaSession callback: onStop")
                stopSelf()
            }

            override fun onSkipToNext() {
                Log.d("AudioService", "MediaSession callback: onSkipToNext")
                // Implement if you have playlist functionality
            }

            override fun onSkipToPrevious() {
                Log.d("AudioService", "MediaSession callback: onSkipToPrevious")
                // Implement if you have playlist functionality
            }
        })

        // Set flags for media buttons and transport controls
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        // Start the session
        mediaSession.isActive = true
    }

    private fun requestAudioFocus(): Boolean {
        Log.d("AudioService", "Requesting audio focus")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            Log.d("AudioService", "Audio focus loss")
                            pauseAudio()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.d("AudioService", "Audio focus loss transient")
                            pauseAudio()
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.d("AudioService", "Audio focus gain")
                            playAudio()
                        }
                    }
                }
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            Log.d("AudioService", "Audio focus loss")
                            pauseAudio()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.d("AudioService", "Audio focus loss transient")
                            pauseAudio()
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.d("AudioService", "Audio focus gain")
                            playAudio()
                        }
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        Log.d("AudioService", "Abandoning audio focus")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun updatePlaybackState(state: Int) {
        Log.d("AudioService", "Updating playback state to: $state")

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, mediaPlayer?.currentPosition?.toLong() ?: 0, 1.0f)

        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateMediaSessionMetadata() {
        Log.d("AudioService", "Updating media session metadata")

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Your Audio Title")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist Name")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Album Name")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer?.duration?.toLong() ?: 0)

        // Add album art if available
        try {
            // You can replace this with actual album art
            val bitmap = BitmapFactory.decodeResource(resources, android.R.drawable.ic_media_play)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        } catch (e: Exception) {
            Log.e("AudioService", "Error setting album art: ${e.message}")
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun updatePlaybackStateUI(playing: Boolean) {
        // Use broadcast to update the UI
        val intent = Intent("com.example.tp_mobile.PLAYBACK_STATE_CHANGED")
        intent.putExtra("isPlaying", playing)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Audio Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for Audio Player"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)

            Log.d("AudioService", "Notification channel created")
        }
    }

    private fun createNotification(): Notification {
        Log.d("AudioService", "Creating notification")

        // Create content intent (when notification is clicked)
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Create play/pause action
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AudioService::class.java).setAction(playPauseAction),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Create previous action
        val previousIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AudioService::class.java).setAction(ACTION_PREVIOUS),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Create next action
        val nextIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AudioService::class.java).setAction(ACTION_NEXT),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Create stop action
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AudioService::class.java).setAction(ACTION_STOP),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Create media style
        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2) // Previous, Play/Pause, Next

        // Build the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Your Audio Title")
            .setContentText("Artist Name")
            .setSubText("Album Name")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(mediaStyle)
            .setShowWhen(false)

            // Add actions
            .addAction(android.R.drawable.ic_media_previous, "Previous", previousIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)

        // Add album art if available
        try {
            // You can replace this with actual album art
            val bitmap = BitmapFactory.decodeResource(resources, android.R.drawable.ic_media_play)
            builder.setLargeIcon(bitmap)
        } catch (e: Exception) {
            Log.e("AudioService", "Error setting large icon: ${e.message}")
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("AudioService", "onDestroy called")

        // Release MediaPlayer resources
        mediaPlayer?.release()
        mediaPlayer = null

        // Release MediaSession
        mediaSession.release()

        // Abandon audio focus
        abandonAudioFocus()

        stopSelf()



        super.onDestroy()
    }
}