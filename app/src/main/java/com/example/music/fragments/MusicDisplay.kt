package com.example.music.fragments

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.music.MediaService
import com.example.music.R
import com.example.music.data.Data
import com.example.music.util.SharedPref
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Runnable


class MusicDisplay : Fragment() {
    //    register for updating the ui
    private var seekBarMaxSet = false
    private val playReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.music.PLAYER_UPDATE") {
                val isplay = intent.getBooleanExtra("is_playing", false)
                val position = intent.getIntExtra("position", 0)
                val duration = intent.getIntExtra("duration", 0)


                if (!seekBarMaxSet && duration > 0) {
                    seekBar.max = duration
                    tvDurationView.text = formatMillis(duration)
                    seekBarMaxSet = true
                }

                // is position reaches seekbar max
                if (seekBarMaxSet && position == seekBar.max) {
                    if (isRepeatOneSong) {
                        loadTrack(trackList[currentIndex])
                    } else {
                        playRightSong()
                    }
                }

                if (!isUserSeeking) {
                    seekBar.progress = position
                    tvCurrentView.text = formatMillis(position)
                }

                playBtn.setImageResource(
                    if (isplay) R.drawable.baseline_pause_circle_24 else R.drawable.baseline_play_circle_24
                )
                this@MusicDisplay.isPlaying = isplay
            }
        }

    }

    //    views
    private lateinit var tvCurrentView: TextView
    private lateinit var tvDurationView: TextView
    private lateinit var titleView: TextView
    private lateinit var artistView: TextView
    private lateinit var imageView: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var playBtn: ImageButton
    private lateinit var leftplayBtn: ImageButton
    private lateinit var rightplayBtn: ImageButton
    private lateinit var repeatOneBtn: ImageButton

    // media player
    private var isPlaying = false
    private var handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private var isRepeatOneSong = false

    private var mediaService : MediaService? = null
    private var isBound  = false

    private val connection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?
        ) {
           val binder  = service as MediaService.LocalBinder
            mediaService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaService = null
            isBound = false
        }

    }


    private lateinit var trackList: ArrayList<Data>
    private var currentIndex: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_music_display, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //registering the ui update receiver to update the seekbar ans play button
        super.onViewCreated(view, savedInstanceState)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            playReceiver,
            IntentFilter("com.example.music.PLAYER_UPDATE")
        )
        // initialise the views
        titleView = view.findViewById(R.id.titleF2)
        artistView = view.findViewById(R.id.artistF2)
        imageView = view.findViewById(R.id.imageF2)
        seekBar = view.findViewById(R.id.seekBar)
        playBtn = view.findViewById(R.id.playBtn)
        leftplayBtn = view.findViewById(R.id.leftPlay)
        rightplayBtn = view.findViewById(R.id.rightPlay)
        tvCurrentView = view.findViewById(R.id.tvCurrentTime)
        tvDurationView = view.findViewById(R.id.tvDurationTime)
        repeatOneBtn = view.findViewById(R.id.loopBtnOne)

        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                trackList = (arguments?.getSerializable(
                    "trackList",
                    ArrayList::class.java
                ) as? ArrayList<Data>)!!
            } else {
                @Suppress("DEPRECATION")
                trackList = (arguments?.getSerializable("trackList") as? ArrayList<Data>)!!
            }

            currentIndex = arguments?.getInt("currentIndex")!!
        }

        setListener()

        isRepeatOneSong = SharedPref.getBoolean(SharedPref.Keys.IS_LOOPING)
        Log.d("KrishnaMk",isRepeatOneSong.toString())
        repeatOneBtn.alpha = if (isRepeatOneSong) 1f else 0.3f
        loadTrack(trackList[currentIndex])
    }


    override fun onStart() {
        super.onStart()

        val intent  = Intent(requireContext(), MediaService::class.java)
        requireContext().bindService(intent,connection , Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        if (isBound){
            requireContext().unbindService(connection)
            isBound = false
        }
    }

    private fun setListener(){
        playBtn.setOnClickListener {
            if (isPlaying) {
//                sendMusicCommand(MediaService.ACTION_PAUSE)
                mediaService?.pauseMusic()
                playBtn.setImageResource(R.drawable.baseline_play_circle_24)
            } else {
//                sendMusicCommand(MediaService.ACTION_PLAY)
                mediaService?.resumeMusic()
                playBtn.setImageResource(R.drawable.baseline_pause_circle_24)
            }
            isPlaying = !isPlaying
        }

        leftplayBtn.setOnClickListener {
            playLeftSong()
        }

        rightplayBtn.setOnClickListener {
            playRightSong()
        }


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var finalProgress = 0
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    seekBar.let {
                        finalProgress = progress
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                val intent = Intent(requireContext(), MediaService::class.java).apply {
                    action = MediaService.ACTION_SEEK
                    putExtra("seek_pos", finalProgress)
                }
                requireContext().startService(intent)
            }


        })

        repeatOneBtn.setOnClickListener {
            isRepeatOneSong = !isRepeatOneSong

            repeatOneBtn.alpha = if (isRepeatOneSong) 1f else 0.3f
            SharedPref.saveBoolean(SharedPref.Keys.IS_LOOPING,isRepeatOneSong)
            Log.d("KrishnaMk",isRepeatOneSong.toString())
            mediaService?.setLoop()
        }

    }
    private fun playLeftSong() {
        if (currentIndex > 0) {
            currentIndex--
            MusicSession.currentIndex = currentIndex
            MusicSession.fullTrackList = trackList
            MusicSession.currentTrack = trackList[currentIndex]
            loadTrack(trackList[currentIndex])
        }
    }

    private fun playRightSong() {
        if (currentIndex < trackList.size - 1) {
            currentIndex++
            MusicSession.currentIndex = currentIndex
            MusicSession.fullTrackList = trackList
            MusicSession.currentTrack = trackList[currentIndex]
            loadTrack(trackList[currentIndex])
        }
    }


    private fun loadTrack(track: Data) {
        sendMusicCommand(MediaService.ACTION_PLAY, track)
        titleView.text = track.title
        artistView.text = track.artist.name
        Picasso.get().load(track.album.cover).into(imageView)
        seekBarMaxSet = false
        isPlaying = true

        playBtn.setImageResource(R.drawable.baseline_pause_circle_24)
    }

    // optimisation :  this will be call only once for starting the foreground service
    private fun sendMusicCommand(action: String, track: Data? = null) {
        val intent = Intent(requireContext(), MediaService::class.java).apply {
            this.action = action
            track?.let {
                putExtra("track_url", track.preview)
                putExtra("title", track.title)
                putExtra("artist", track.artist.name)
            }
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(playReceiver)
    }

    fun formatMillis(millis: Int): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }


}