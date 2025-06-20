package com.example.music.data

import java.io.Serializable

data class MyData(
    val `data`: List<Data>,
    val next: String,
    val total: Int
): Serializable