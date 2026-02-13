package com.example.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.music.data.Data
import com.example.music.util.SharedPref

class MediaService : Service(){

    private var isPLaying = false
    private var mediaPlayer: MediaPlayer? =null
    private var currtrackUrl : String? = null
    private var title : String = ""
    private var artist : String = ""

    private val binder  =  LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService() : MediaService = this@MediaService
    }

    private var handler = Handler(Looper.getMainLooper())
    private var updateRunnable = object  : Runnable{
        override fun run() {
            notifyUIUpdate()
            handler.postDelayed(this,500)
        }
    }
    // static as java
    companion object{
        const val ACTION_PLAY = "com.example.music.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.music.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.music.ACTION_STOP"
        const val ACTION_SEEK = "com.example.music.ACTION_SEEK"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_PLAY -> {
                val url = intent.getStringExtra("track_url")
                if (url != null && url != currtrackUrl){
                  currtrackUrl = url
                  title  = intent.getStringExtra("title") ?: ""
                  artist = intent.getStringExtra("artist") ?: ""
                  playMusic(url)
                }else{
                    resumeMusic()
                }

            }

            ACTION_SEEK -> {
                val pos = intent.getIntExtra("seek_pos",0)
                mediaPlayer?.seekTo(pos)
            }

            ACTION_PAUSE -> pauseMusic()
            ACTION_STOP   -> stopMusic()
        }

        return START_NOT_STICKY
    }

//    Even if the Runnable is the same, the queue doesn’t care — it treats it as a new scheduled execution.
    private fun playMusic(url: String){
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                it.start()
                isPLaying = true
                handler.removeCallbacks(updateRunnable)
                handler.post(updateRunnable)
            }
            prepareAsync()
        }
        val isLoop = SharedPref.getBoolean(SharedPref.Keys.IS_LOOPING)
        mediaPlayer?.isLooping = isLoop

        mediaPlayer?.setOnCompletionListener {
            isPLaying = false
        }

        updateNotification()
    }

     fun pauseMusic(){
        mediaPlayer?.pause()
        isPLaying = false
        updateNotification()
    }

     fun resumeMusic(){
        mediaPlayer?.start()
        isPLaying = true
        updateNotification()
    }

    fun playNewSong(track : Data){
        currtrackUrl = track.preview
        title  = track.title
        artist = track.artist.name
        playMusic(track.preview)
    }

    fun setLoop(){
        mediaPlayer?.isLooping = SharedPref.getBoolean(SharedPref.Keys.IS_LOOPING)
    }
// for stoping the music and notification
    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPLaying = false
        stopSelf()
        handler.removeCallbacks(updateRunnable)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    private fun updateNotification() {
        val mainIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this, 0,mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // for service in notification
        val playpauseIntent = Intent(this, MediaService::class.java).apply {
            action = if (isPLaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playpausePendingIntent = PendingIntent.getService(this
        ,1,playpauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // preparing an action on clicking button
        val playpauseAction = NotificationCompat.Action.Builder(
            if (isPLaying) R.drawable.baseline_pause_circle_24 else R.drawable.baseline_play_circle_24,
            if (isPLaying) ACTION_PAUSE else ACTION_PLAY,
            playpausePendingIntent
        ).build()

        val notification = NotificationCompat.Builder(this,"music_channel")
            .setContentTitle(MusicSession.currentTrack?.title)
            .setContentText(MusicSession.currentTrack?.artist?.name)
            .setSmallIcon(R.mipmap.app_logo_round)
            .setContentIntent(contentIntent)
            .addAction(playpauseAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(1,notification)

    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "music_channel",
            "music_playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "handle music playback controls"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

// is the broadcaster
    private fun notifyUIUpdate() {
       val updateIntent = Intent("com.example.music.PLAYER_UPDATE").apply {
           putExtra("is_playing",isPLaying)
           putExtra("position",mediaPlayer?.currentPosition ?: 0)
           putExtra("duration",mediaPlayer?.duration ?: 0)
       }
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopMusic()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopMusic()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

}