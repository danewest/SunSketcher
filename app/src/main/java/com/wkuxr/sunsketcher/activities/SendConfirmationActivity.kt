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
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.database.Metadata
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.createDB
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.db
import com.wkuxr.sunsketcher.databinding.ActivitySendConfirmationBinding
import com.wkuxr.sunsketcher.networking.UploadScheduler
import java.io.File
import java.util.Timer
import java.util.TimerTask


class SendConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivitySendConfirmationBinding
    lateinit var recyclerView: RecyclerView

    companion object {
        lateinit var prefs: SharedPreferences
        lateinit var singleton: SendConfirmationActivity
    }


    lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cam: Camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        singleton = this

        recyclerView = binding.imageRecycler

        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)
        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val hasConfirmDeny = prefs.getInt("upload", -1)
        var intent: Intent? = null
        when (hasConfirmDeny) {
            0 -> intent = Intent(this, FinishedInfoDenyActivity::class.java)
            1 -> if (prefs.getBoolean("uploadSuccessful", false)) { //allowed upload and upload already finished
                //intent = Intent(this, FinishedCompleteActivity::class.java)
            } else {
                intent = Intent(this, FinishedInfoActivity::class.java)
            }

            else -> {}
        }
        if (intent != null) {
            this.startActivity(intent)
        }

        if(!prefs.getBoolean("hasNotified", false)){
            cameraProvider = ProcessCameraProvider.getInstance(this).get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build()
            cam = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)

            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(true) // or false
            }

            val cameraActivitySchedulerTask = TimeTask()
            timer = Timer()
            timer.schedule(cameraActivitySchedulerTask, 1000)
        }

        displayImageList()
    }

    private lateinit var timer: Timer

    internal class TimeTask : TimerTask() {
        override fun run() {
            singleton.cam.cameraControl.enableTorch(false)
            singleton.cameraProvider.unbindAll()
        }
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
            /*if(!foregroundServiceRunning()) { //TODO: add for actual releases
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }*/
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