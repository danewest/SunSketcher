package com.wkuxr.sunsketcher.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.database.Metadata
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.createDB
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.db
import com.wkuxr.sunsketcher.databinding.ActivitySendConfirmationBinding
import com.wkuxr.sunsketcher.networking.UploadScheduler
import java.io.File


class SendConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivitySendConfirmationBinding
    lateinit var recyclerView: RecyclerView

    companion object {
        lateinit var prefs: SharedPreferences
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = binding.imageRecycler

        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)

        displayImageList()
    }

    fun onClick(v: View){
        if(v.id == binding.allowBtn.id){
            prefs.edit().putInt("upload", 1).apply()
            /*if(!foregroundServiceRunning()) { //TODO: add for actual releases
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }*/
        } else {
            prefs.edit().putInt("upload", 0).apply()
        }
        val intent = Intent(this, FinishedInfoActivity::class.java)
        this.startActivity(intent)
    }

    fun displayImageList(){
        db = createDB(this)
        val metadata = db.getMetadata()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ItemAdapter(metadata, supportFragmentManager, filesDir.absolutePath)
    }

    class ItemAdapter(private val metadataList: List<Metadata>, fragManager: FragmentManager, directory: String) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycle_list_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return metadataList.size
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = metadataList[position]
            holder.itemView.tag = item.id
            Log.d("IMAGEFILEPATHS", item.filepath)
            val imgFile = File(item.filepath)

            //create bitmap from image
            val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)

            holder.imgView.setImageBitmap(imgBitmap)
        }

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imgView: ImageView = itemView.findViewById(R.id.itemImage)
        }
    }

    fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (UploadScheduler::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}