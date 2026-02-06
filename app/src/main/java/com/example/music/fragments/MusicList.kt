package com.example.music.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.music.AdapterR
import com.example.music.ApiInterface
import com.example.music.R
import com.example.music.data.Data
import com.example.music.data.MyData
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MusicList : Fragment() {

    private lateinit var recycleView: RecyclerView
    private lateinit var miniPlayer: View
    private lateinit var miniTitle: TextView
    private lateinit var miniArtist: TextView
    private lateinit var miniArt: ImageView
    private lateinit var miniPlayBtn: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_music_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        recycleView = view.findViewById(R.id.recycleViewF1)
        recycleView.layoutManager = LinearLayoutManager(requireContext())

        miniPlayer = view.findViewById(R.id.miniPlayer)
        miniTitle = view.findViewById(R.id.miniTitle)
        miniArtist = view.findViewById(R.id.miniArtist)
        miniArt = view.findViewById(R.id.miniArt)
        miniPlayBtn = view.findViewById(R.id.miniPlayBtn)

        // Restore mini player state if song is playing
        MusicSession.currentTrack?.let {
            updateMiniPlayer(it)
        }

        // Mini player click -> open MusicDisplay again
        miniPlayer.setOnClickListener {
            MusicSession.fullTrackList?.let { list ->
                val bundle = Bundle().apply {
                    putSerializable("trackList", list)
                    putInt("currentIndex", MusicSession.currentIndex)
                }
                val musicDisplay = MusicDisplay().apply {
                    arguments = bundle
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.containerFragment, musicDisplay)
                    .addToBackStack(null)
                    .commit()
            }
        }

        val api = Retrofit.Builder()
            .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)

        val retrofitData = api.getData("eminem")

        retrofitData.enqueue(object : Callback<MyData?> {
            override fun onResponse(call: Call<MyData?>, response: Response<MyData?>) {
                val datalist = response.body()?.data ?: return
                if (datalist.isEmpty()) {
                    Log.d("Tag: onEmpty", "data List is empty")
                }

                val adapter = AdapterR(requireActivity(), datalist) { selectedTrack ->
                    val index = datalist.indexOf(selectedTrack)

                    // Save session
                    MusicSession.currentTrack = selectedTrack
                    MusicSession.currentIndex = index
                    MusicSession.fullTrackList = ArrayList(datalist)

                    updateMiniPlayer(selectedTrack)

                    val bundle = Bundle().apply {
                        putSerializable("trackList", ArrayList(datalist))
                        putInt("currentIndex", index)
                    }

                    val musicDisplay = MusicDisplay().apply {
                        arguments = bundle
                    }

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.containerFragment, musicDisplay)
                        .addToBackStack(null)
                        .commit()
                }

                recycleView.adapter = adapter
            }

            override fun onFailure(call: Call<MyData?>, t: Throwable) {
                Log.d("Tag: onFailure", "onFailure: " + t.message)
            }
        })
    }

    private fun updateMiniPlayer(track: Data) {
        miniTitle.text = track.title
        miniArtist.text = track.artist.name
        Picasso.get().load(track.album.cover).into(miniArt)
        miniPlayer.visibility = View.VISIBLE
    }
}
