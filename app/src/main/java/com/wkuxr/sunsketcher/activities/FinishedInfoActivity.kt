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
import com.wkuxr.sunsketcher.networking.ClientRunOnTransfer.clientTransferSequence
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
        val clientID = prefs.getLong("clientID", -1)

        //fill the UI text based on the preferences
        var text = binding.infoText
        if(uploadReady == 0){
            text.text = "Thank you for using the SunSketcher app. You have chosen not to upload your images for analysis. You can now uninstall the SunSketcher app. Even after uninstalling, your eclipse photos can still be found in the `Photos/SunSketcher/` directory in your device storage, or in your gallery."
            binding.uploadBtn.isEnabled = false
            binding.uploadBtn.text = "You have chosen not to upload photos."
        } else {
            text.text = "Thank you for using the SunSketcher app. You have chosen to upload your images for analysis. Please keep the app installed and do not delete the photos in the `Pictures/SunSketcher/` directory, or the SunSketcher album in your gallery, until further notice. The text at the bottom of the screen will change when your images have been uploaded, at which point you can freely delete the app and images from your device. This may take up to a week, so please be patient."
        }
        binding.clientIDText.text = "ClientID: $clientID"

        //disable the upload button if the UploadScheduler service is already running
        /*if(foregroundServiceRunning()){   //TODO: uncomment
            binding.uploadBtn.isEnabled = false
            binding.uploadBtn.text = "The upload service is running. Estimated finish time: ${(0.5 + clientID) * 15} minutes from time that upload was accepted. Please allow for extra time, as your upload time may be delayed without notice."
        }*/
        //TODO: remove the following two lines
        binding.uploadBtn.isEnabled = false
        binding.uploadBtn.text = "This is a test version of the app where upload is unnecessary."

        //dump database values to a csv file in documents folder
        /*if(!prefs.getBoolean("DBIsDumped", false)) {
            dumpDBtoCSV()
        }*/

        if (prefs.getBoolean("uploadSuccessful", false)) { //upload already finished
            Intent(this, FinishedCompleteActivity::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        var hasFinishedUpload = prefs.getBoolean("uploadSuccessful", false)
        if(hasFinishedUpload){
            binding.uploadBtn.isEnabled = false
            binding.uploadBtn.text = "Your images have been uploaded successfully. You can now uninstall the SunSketcher app."
        }

        if (prefs.getBoolean("uploadSuccessful", false)) { //upload already finished
            Intent(this, FinishedCompleteActivity::class.java)
        }
    }

    //dump timing data to a CSV
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

    fun onUploadClick(v: View) {
        val btn: Button = v as Button
        try {
            prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)
            val clientID = prefs.getLong("clientID", -1)

            //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            //var thread = NetworkThread(this) { updateUIOnUploadFinish() }
            //thread.start()
            if(!foregroundServiceRunning()) {
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }

            btn.text = "The upload service is running. Estimated finish time: ${(0.5 + clientID) * 15} minutes from time that upload was accepted. Please allow for extra time, as your upload time may be delayed without notice."
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

    fun onSciStarterClick(v: View){
        val uri = Uri.parse("https://scistarter.org/form/180")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    fun onLearnMoreClick(v: View){
        val intent = Intent(this, LearnMoreActivity::class.java)
        startActivity(intent)
    }
}