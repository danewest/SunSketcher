package com.wkuxr.sunsketcher.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wkuxr.sunsketcher.App
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.database.Metadata
import com.wkuxr.sunsketcher.database.MetadataDB
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.createDB
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.db
import com.wkuxr.sunsketcher.databinding.ActivitySendConfirmationBinding
import com.wkuxr.sunsketcher.networking.UploadScheduler
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File


class SendConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivitySendConfirmationBinding
    lateinit var recyclerView: RecyclerView

    companion object {

        lateinit var prefs: SharedPreferences
        lateinit var singleton: SendConfirmationActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        singleton = this
        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)

        recyclerView = binding.imageRecycler

        val hasConfirmDeny = prefs.getInt("upload", -1)
        var intent: Intent? = null
        when (hasConfirmDeny) {
            0 -> intent = Intent(this, FinishedInfoDenyActivity::class.java)
            1 -> intent = if (prefs.getBoolean("uploadSuccessful", false)) { //allowed upload and upload already finished
                Intent(this, FinishedCompleteActivity::class.java)
            } else {
                Intent(this, FinishedInfoActivity::class.java)
            }

            else -> {}
        }
        if (intent != null) {
            this.startActivity(intent)
        }
        
        /*if(!prefs.getBoolean("cropped", false)){
            cropImages()
        }*/

        displayImageList()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val intent = when (prefs.getInt("upload", -1)) {
            0 -> {
                Intent(this, FinishedInfoDenyActivity::class.java)
            }
            1 -> {
                if (prefs.getBoolean("uploadSuccessful", false)) { //allowed upload and upload already finished
                    Intent(this, FinishedCompleteActivity::class.java)
                } else {
                    Intent(this, FinishedInfoActivity::class.java)
                }
            }
            else -> {
                null
            }
        }
        if (intent != null) {
            this.startActivity(intent)
        }
    }

    fun onClick(v: View) {
        val intent: Intent = if (v.id == binding.sendConfirmationYesBtn.id) {
            prefs.edit().putInt("upload", 1).apply()
            if(!foregroundServiceRunning()) { //TODO: add for actual releases
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }
            Intent(this, FinishedInfoActivity::class.java)
        } else {
            Intent(this, UploadDenyConfirmationActivity::class.java)
        }
        this.startActivity(intent)
    }

    private fun displayImageList() {
        db = createDB(this)
        val metadata = db.getMetadata()
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ItemAdapter(metadata)
    }

    class ItemAdapter(private val metadataList: List<Metadata>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
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