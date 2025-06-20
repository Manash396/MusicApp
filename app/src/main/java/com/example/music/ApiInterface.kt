package com.example.music

import com.example.music.data.MyData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ApiInterface {

//    to access the interface we have to use unique key

    @Headers("x-rapidapi-key: 65f70ba6f0msh574cbe0c187b7b3p1cc77bjsna82930990322" ,
    "x-rapidapi-host: deezerdevs-deezer.p.rapidapi.com")

    @GET("search")
    fun getData(@Query("q") query: String): Call<MyData>
}