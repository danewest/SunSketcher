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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.exifinterface.media.ExifInterface
import com.wkuxr.sunsketcher.App
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
        lateinit var singleton: SendConfirmationActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        singleton = this
        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)

        //get a reference to the recyclerView which will hold the captured images
        recyclerView = binding.imageRecycler

        val hasConfirmDeny = prefs.getInt("upload", -1)
        var intent: Intent? = null
        when (hasConfirmDeny) {
            0 -> intent = Intent(this, FinishedInfoDenyActivity::class.java) //user denied upload
            1 -> intent = if (prefs.getBoolean("uploadSuccessful", false)) { //allowed upload and upload already finished
                Intent(this, FinishedCompleteActivity::class.java)
            } else { //allowed upload but upload is not finished
                Intent(this, FinishedInfoActivity::class.java)
            }

            else -> {} //user has neither confirmed nor denied upload
        }
        if (intent != null) {
            this.startActivity(intent)
        }

        //populate the recyclerView with the captured images
        displayImageList()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val intent = when (prefs.getInt("upload", -1)) {
            0 -> { //0 means upload was denied
                Intent(this, FinishedInfoDenyActivity::class.java)
            }
            1 -> { //1 means the user allowed upload
                if (prefs.getBoolean("uploadSuccessful", false)) { //uploadSuccessful means all data has been uploaded if true
                    Intent(this, FinishedCompleteActivity::class.java)
                } else { //uploadSuccessful is false if not all data has been uploaded (this is also the default if the variable isn't found)
                    Intent(this, FinishedInfoActivity::class.java)
                }
            }
            //default to doing nothing if the user has neither confirmed nor denied
            else -> {
                null
            }
        }

        //switch to whatever activity necessary, if applicable
        if (intent != null) {
            this.startActivity(intent)
        }
    }

    //call this whenever any button on screen is pressed
    fun onClick(v: View) {
        //check if the pressed button is the allow upload button
        val intent: Intent = if (v.id == binding.sendConfirmationYesBtn.id) {
            //modify the upload variable in prefs to say that upload was allowed
            prefs.edit().putInt("upload", 1).apply()

            //check if the UploadScheduler service is already running
            if(!foregroundServiceRunning()) {
                //if not, get the app context and start it
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }

            //move to FinishedInfoActivity, as upload has not been completed yet
            Intent(this, FinishedInfoActivity::class.java)
        } else { //user pressed the deny button
            //move to a screen that confirms that the user wants to deny upload
            Intent(this, UploadDenyConfirmationActivity::class.java)
        }
        this.startActivity(intent)
    }

    //populate the recyclerView with the image filepath references in the Metadata table of the DB
    private fun displayImageList() {
        db = createDB(this)
        val metadata = db.getMetadata()
        //display the images in a horizontally-scrolling list
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ItemAdapter(metadata)
    }

    //adapts a list of Metadata entities to the recycleView
    class ItemAdapter(private val metadataList: List<Metadata>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        //inflates the view of each individual recycleView item (allows its elements to be accessed programmatically, same as activity bindings)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycle_list_item, parent, false)
            return ItemViewHolder(view)
        }

        //must be overridden from RecyclerView.Adapter
        override fun getItemCount(): Int {
            return metadataList.size
        }

        //for each ItemViewHolder in the recycleView, given an index for an image in the Metadata table, put the image in the view holder
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = metadataList[position]
            holder.itemView.tag = item.id
            Log.d("IMAGEFILEPATHS", item.filepath)
            val imgFile = File(item.filepath)

            //create bitmap from image
            val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)

            //replace the image in the ItemViewHolder's ImageView with the given image
            holder.imgView.setImageBitmap(imgBitmap)
        }

        //definition for the ItemViewHolder
        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            //just has an ImageView. By default, references an unused image
            val imgView: ImageView = itemView.findViewById(R.id.itemImage)
        }
    }

    //checks if the UploadScheduler service is running, returns true if it is, and false if not
    fun foregroundServiceRunning(): Boolean {
        //get a reference to the ActivityManager (because a service is just a viewless Activity)
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        //check if the UploadScheduler is present in the list of services in the ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (UploadScheduler::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}