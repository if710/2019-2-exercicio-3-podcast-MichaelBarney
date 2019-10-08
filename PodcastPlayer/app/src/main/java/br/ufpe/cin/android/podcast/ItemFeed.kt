package br.ufpe.cin.android.podcast

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.URI

@Entity
data class ItemFeed(@PrimaryKey val title: String,
                    val link: String,
                    val pubDate: String,
                    val description: String,
                    val downloadLink: String,
                    var uri: String,
                    var isPlaying: Boolean,
                    var secondsIn: Int) {

    override fun toString(): String {
        return title
    }
}
