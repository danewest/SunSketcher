package com.wkuxr.eclipsetotality.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.wkuxr.eclipsetotality.App
import com.wkuxr.eclipsetotality.activities.SendConfirmationActivity.Companion.prefs
import com.wkuxr.eclipsetotality.database.MetadataDB.Companion.createDB
import com.wkuxr.eclipsetotality.database.MetadataDB.Companion.db
import com.wkuxr.eclipsetotality.databinding.ActivityFinishedInfoBinding
import com.wkuxr.eclipsetotality.networking.ClientRunOnTransfer.clientTransferSequence
import com.wkuxr.eclipsetotality.networking.UploadScheduler
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
        val clientID = prefs.getLong("clientID", -1)

        //fill the UI text based on the preferences
        var text = binding.infoText
        if(uploadReady == 0){
            text.text = "Thank you for using the SunSketcher app. You have chosen not to upload your images for analysis. You no longer need the app installed and can now uninstall it. Even after uninstalling, your eclipse photos can still be found in the `Photos/SunSketcher/` directory in your device storage, or in your gallery."
        } else {
            text.text = "Thank you for using the SunSketcher app. You have chosen to upload your images for analysis. Please keep the app installed and do not delete the photos in the `Pictures/SunSketcher/` directory, or the SunSketcher album in your gallery, until further notice. You will receive a notification when your images have been uploaded, at which point you can freely delete the app and images from your device. This may take more than a month, so we appreciate your patience."
        }
        binding.clientIDText.text = "ClientID: $clientID"

        //disable the upload button if the UploadScheduler service is already running
        if(foregroundServiceRunning()){
            binding.uploadBtn.isEnabled = false
                binding.uploadBtn.text = "UploadScheduler started. Estimated finish time: ${(1 + clientID) * 15} minutes from eclipse end."
        }

        //dump database values to a csv file in documents folder
        if(!prefs.getBoolean("DBIsDumped", false)) {
            dumpDBtoCSV()
        }
    }

    //dump timing data to a CSV
    private fun dumpDBtoCSV(){
        db = createDB(this)
        db.initialize()
        val metas = db.getMetadata()

        val prefEdit = prefs.edit()

        var out = "Filepath,Latitude,Longitude,Altitude,Saved Time\n"

        for(meta in metas){
            //val splitFilepath = meta.filepath.split("/")
            out += meta.filepath + "," + meta.latitude + "," + meta.longitude + "," + meta.altitude + "," + meta.captureTime + "\n"
        }

        //save the file to the same folder as the images, or to the documents folder if the image folder directory somehow wasn't saved to shared preferences
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

    fun onUploadClick(v: View) {
        val btn: Button = v as Button
        try {
            prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)

            //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            //var thread = NetworkThread(this) { updateUIOnUploadFinish() }
            //thread.start()
            if(!foregroundServiceRunning()) {
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }

            btn.text = "UploadScheduler started."
            btn.isEnabled = false
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun updateUIOnUploadFinish(){
        runOnUiThread {
            Runnable {
                Log.d("NetworkThread", "Upload is complete. Changing button text.")
                binding.uploadBtn.text = "Upload complete."
            }
        }
    }

    //old manual upload method
    class NetworkThread(context: Context, uiUpdate: () -> Unit) : Thread() {
        val uiUpdateFun = uiUpdate
        val context = context
        override fun run() {
            try {
                Log.d("NetworkThread", "Beginning Upload. Please wait...")
                clientTransferSequence()
                uiUpdateFun()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (UploadScheduler::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}