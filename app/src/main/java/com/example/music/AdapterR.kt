package com.example.music

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.music.data.Data
import com.squareup.picasso.Picasso


class AdapterR(val context : Activity,
               val datalist: List<Data>,
    val onItemClicked: (Data) -> Unit

): RecyclerView.Adapter<AdapterR.MyViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_layout,parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val currentData  = datalist[position]
        holder.title.text = currentData.title
        holder.artist.text = currentData.artist.name
        Picasso.get().load(currentData.album.cover).into(holder.image)

        holder.itemView.setOnClickListener {
            onItemClicked(currentData)
        }

    }

    override fun getItemCount(): Int {
        return datalist.size
    }


    class MyViewHolder(dataView: View) : RecyclerView.ViewHolder(dataView){

          val image: ImageView
          val title : TextView
          val artist : TextView

          init {
              image = dataView.findViewById(R.id.imageF1)
              title = dataView.findViewById(R.id.musicTitleF1)
              artist = dataView.findViewById(R.id.musicArtistF1)
          }
    }


}