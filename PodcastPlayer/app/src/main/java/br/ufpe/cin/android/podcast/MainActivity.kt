package br.ufpe.cin.android.podcast

import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.IntentService
import android.content.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


//Room
@Dao
interface PodcastDao {
    @Query("SELECT * FROM ItemFeed")
    fun all(): List<ItemFeed>

    @Insert
    fun insertAll(vararg Items: ItemFeed)

    @Query("UPDATE ItemFeed SET uri = :uri WHERE title = :title")
    fun updateURI(title: String, uri: String)
}

@Database(entities = [ItemFeed::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
}

// Main
class MainActivity : AppCompatActivity() {
    private var myAdapter:PodcastAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val link = "https://s3-us-west-1.amazonaws.com/podcasts.thepolyglotdeveloper.com/podcast.xml"
        LoadXML().execute(link)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadCastReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))


        val button = findViewById<Button>(R.id.settingsButton)
        button.setOnClickListener {
            startActivity(Intent(applicationContext,PrefsMenuActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadCastReceiver)
    }

    private inner class LoadXML : AsyncTask<String, Void, List<ItemFeed>>() {
        override fun doInBackground(vararg urls: String?): List<ItemFeed> {
            Log.d("debug", "HEY")

            // Step 0 - Check the Database
            val dao: PodcastDao = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "podcasts"
            ).build().podcastDao()

            val prePodcasts = dao.all()
            if (prePodcasts.isNotEmpty()) {
                fillRecyclerView(prePodcasts)
                Log.d("Debug", "Loading from Database")
                return prePodcasts
            }

            // Step 1 - Get the XML String
            Log.d("Debug", "Loading from internet")
            val urlObject = URL(urls[0])
            val connection = urlObject.openConnection() as HttpURLConnection
            val XMLString = connection.inputStream.bufferedReader().readText()

            // Step 2 -  Parse the XML String
            val parsedList = Parser.parse(XMLString)

            // Step 3 - Add it to the Database
            dao.insertAll(*parsedList.toTypedArray())

            return parsedList
        }

        override fun onPostExecute(result: List<ItemFeed>?) {
            super.onPostExecute(result)
            fillRecyclerView(result!!)
        }
    }

    fun fillRecyclerView(podcastList: List<ItemFeed>){
        // Fill out the RecyclerView
        myAdapter = PodcastAdapter(podcastList)
        findViewById<RecyclerView>(R.id.my_recycler_view).apply{
            adapter = myAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private inner class PodcastAdapter(var podcastList: List<ItemFeed>):
        RecyclerView.Adapter<PodcastViewHolder>() {

        var mp:MediaPlayer? =  null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.itemlista, parent, false)
            return PodcastViewHolder(view)
        }

        override fun getItemCount(): Int {
            return podcastList.size
        }

        override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
            Log.d("debug", "On Bind: $position")
            val podcast = this.podcastList.get(position)
            holder.Title.text = podcast.title

            holder.Date.text = podcast.pubDate

            holder.Action.setOnClickListener {
                Log.d("debug", "Click")
                val intent = Intent(this@MainActivity, DownloadService::class.java)
                intent.putExtra("url", podcast.downloadLink)
                intent.putExtra("title", podcast.title)
                startService(intent)
            }

            holder.Title.setOnClickListener {
                val intent = Intent(this@MainActivity, EpisodeDetailActivity::class.java)

                intent.putExtra("title", podcast.title)
                intent.putExtra("description", podcast.description)
                intent.putExtra("link", podcast.link)
                startActivityForResult(intent, 1)
            }

            if (podcast.uri == ""){
                holder.Play.alpha = 0.2F
                Log.d("debug", "empty")
            }
            else{
                holder.Play.alpha = 1F
                Log.d("debug", "not empty")
                holder.Play.setOnClickListener {
                    if (!podcast.isPlaying){
                        Log.d("debug", "play")
                        val uri = Uri.parse(podcast.uri)
                        Log.d("debug", uri.toString())

                        if (mp == null) {
                            mp = MediaPlayer.create(this@MainActivity, uri)
                            mp!!.setOnPreparedListener {
                                mp!!.start()
                            }
                            mp!!.setOnCompletionListener {
                                // Erase the podcast once it'' finished playing
                                Uri.parse(podcast.uri).toFile().delete()
                            }

                        }

                        else{
                            mp!!.start()
                        }
                        mp!!.seekTo(podcast.secondsIn)

                        holder.Play.setImageResource(R.drawable.pause)

                        podcastList[position].isPlaying = true;
                    }
                    else{
                        Log.d("debug", "pause")

                        mp!!.pause()
                        podcastList[position].secondsIn = mp!!.currentPosition

                        holder.Play.setImageResource(R.drawable.play)

                        podcastList[position].isPlaying = false;
                    }
                }
            }
        }
        fun makePlayable(title:String, uri:String){
            var index = podcastList.indexOfFirst{
                Log.d("iter", "in: " + it.title + " - looking for: " + title)
                it.title == title
            }
            Log.d("debug", "index: $index")
            podcastList[index].uri = uri;
            notifyDataSetChanged()
            Log.d("debug", "SHoyld Change")
        }
    }

    private inner class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var Title:TextView = itemView.findViewById(R.id.item_title)
        var Action:Button = itemView.findViewById(R.id.item_action)
        var Date:TextView = itemView.findViewById(R.id.item_date)
        var Play:ImageButton = itemView.findViewById(R.id.item_play)
    }

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DOWNLOAD_COMPLETE){
                handleFinishedDownload(
                    intent.getStringExtra("title"),
                    intent.getStringExtra("uri")
                )

            }
        }
    }

    fun handleFinishedDownload(title:String, uri:String){
        Log.d("debug", "Finished Download")
        myAdapter!!.makePlayable(title, uri)
    }
}

class DownloadService: IntentService("DownloadService") {
    override fun onHandleIntent(intent: Intent?) {
        Log.d("debug","Starting Download")

        val url = intent!!.getStringExtra("url")
        val title = intent.getStringExtra("title")
        val fileName = title!!.split(":")[0]

        //checar se tem permissao... Android 6.0+
        val root = getExternalFilesDir(DIRECTORY_DOWNLOADS)
        root?.mkdirs()
        val output = File(root, fileName+".mp3")

        val uri = output.toURI()
        Log.d("debug", uri.toString())

        if (output.exists()) {
            output.delete()
        }

        val c = URL(url).openConnection() as HttpURLConnection
        val fos = FileOutputStream(output.path)
        val out = BufferedOutputStream(fos)
        try {
            val `in` = c.inputStream
            val buffer = ByteArray(8192)
            var len = `in`.read(buffer)
            while (len >= 0) {
                out.write(buffer, 0, len)
                len = `in`.read(buffer)
            }
            out.flush()
        } finally {
            fos.fd.sync()
            out.close()
            c.disconnect()
        }

        // Save Podcast Location
        val dao: PodcastDao = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "podcasts"
        ).build().podcastDao()
        dao.updateURI(title, uri.toString())

        val broadcastIntent = Intent(ACTION_DOWNLOAD_COMPLETE)
        broadcastIntent.putExtra("title", title)
        broadcastIntent.putExtra("uri", uri.toString())
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }
}