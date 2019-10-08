package br.ufpe.cin.android.podcast

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class EpisodeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode_detail)

        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        val link = intent.getStringExtra("link")

        val Title = findViewById<TextView>(R.id.podcast_title).setText(title)
        val Description = findViewById<TextView>(R.id.podcast_description).setText(description)
        val Link = findViewById<TextView>(R.id.podcast_link).setText(link)
        val Button = findViewById<TextView>(R.id.podcast_back).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivityForResult(intent, 1)
        }


    }
}
