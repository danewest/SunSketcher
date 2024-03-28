package com.wkuxr.sunsketcher.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.wkuxr.sunsketcher.App
import com.wkuxr.sunsketcher.activities.SendConfirmationActivity.Companion.prefs
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.createDB
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.db
import com.wkuxr.sunsketcher.databinding.ActivityFinishedInfoBinding
import com.wkuxr.sunsketcher.networking.UploadScheduler
import java.io.File
import java.io.FileWriter


class FinishedInfoActivity : AppCompatActivity() {

    lateinit var binding: ActivityFinishedInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishedInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get the necessary preferences to fill UI text
        prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val uploadReady = prefs.getInt("upload", 0)

        //display the client ID for debugging
        //val clientID = prefs.getLong("clientID", -1)
        //binding.clientIDText.text = "ClientID: $clientID"

        //dump database values to a csv file in documents folder
        /*if(!prefs.getBoolean("DBIsDumped", false)) {
            dumpDBtoCSV()
        }*/

        if (prefs.getBoolean("uploadSuccessful", false)) { //upload already finished
            Intent(this, FinishedCompleteActivity::class.java)
        } else if (!foregroundServiceRunning()){
            if(App.getContext() == null)
                App.setContext(this)
            val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
            startService(uploadSchedulerIntent)
        }
    }

    //called any time the user re-focuses the app
    override fun onResume() {
        super.onResume()
        //checks if the user's data has been uploaded
        prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        if (prefs.getBoolean("uploadSuccessful", false)) { //if uploaded, switch to the FinishedCompleteActivity
            Intent(this, FinishedCompleteActivity::class.java)
        } else if (!foregroundServiceRunning()){ //if not uploaded, check to see if the UploadScheduler service is running; if not, start it
            if(App.getContext() == null) //check if the App class has a global context set; if not, set it to this activity
                App.setContext(this)

            //start the upload scheduler service
            val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
            startService(uploadSchedulerIntent)
        }
    }


    //dump database to a CSV file (saved in documents folder of device)
    private fun dumpDBtoCSV(){
        db = createDB(this)
        db.initialize()
        val metas = db.getMetadata()

        val prefEdit = prefs.edit()

        var out = "Filepath,Latitude,Longitude,Altitude,Saved Time,f-stop,iso,white balance,exposure,focal distance\n"

        for(meta in metas){
            //val splitFilepath = meta.filepath.split("/")
            out += meta.filepath + "," + meta.latitude + "," + meta.longitude + "," + meta.altitude + "," + meta.captureTime + "," + meta.fstop + "," + meta.iso + "," + meta.whiteBalance + "," + meta.exposure + "," + meta.focalDistance + "\n"
        }

        //save the file to the documents folder
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath

        val csvFileName = "SunSketcher Database Dump ${System.currentTimeMillis()}.csv"
        Log.d("CSVWriter", "Writing timing data to $documents/$csvFileName")

        val csv = File("$documents/$csvFileName")
        val writer = FileWriter(csv)
        writer.write(out)
        writer.close()

        prefEdit.putBoolean("DBIsDumped", true)
        prefEdit.apply()
    }

    //manually starts the upload scheduler service; the button is hidden in the layout currently, so this is technically not used
    fun onUploadClick(v: View) {
        val btn: Button = v as Button
        try {
            prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)
            val clientID = prefs.getLong("clientID", -1)

            if(!foregroundServiceRunning()) {
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }

            btn.text = "The upload service is running."
            btn.isEnabled = false
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    //check if the upload scheduler service is running
    private fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (UploadScheduler::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    //open the scistarter form for SunSketcher in browser
    fun onSciStarterClick(v: View){
        val uri = Uri.parse("https://scistarter.org/form/180")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    //open the LearnMoreActivity
    fun onLearnMoreClick(v: View){
        val intent = Intent(this, LearnMoreActivity::class.java)
        startActivity(intent)
    }
}