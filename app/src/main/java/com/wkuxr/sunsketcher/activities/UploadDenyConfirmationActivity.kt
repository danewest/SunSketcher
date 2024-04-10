package com.wkuxr.sunsketcher.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.wkuxr.sunsketcher.App
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.activities.SendConfirmationActivity.Companion.prefs
import com.wkuxr.sunsketcher.databinding.ActivityUploadDenyConfirmationBinding
import com.wkuxr.sunsketcher.networking.UploadScheduler

class UploadDenyConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivityUploadDenyConfirmationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadDenyConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = when (prefs.getInt("upload", -1)) {
            0 -> { //0 means upload was denied
                Intent(this, FinishedInfoDenyActivity::class.java)
            }
            1 -> {//1 means the user allowed upload
                if (prefs.getBoolean("uploadSuccessful", false)) { //uploadSuccessful means all data has been uploaded if true
                    Intent(this, FinishedCompleteActivity::class.java)
                } else { //uploadSuccessful is false if not all data has been uploaded (this is also the default if the variable isn't found)
                    Intent(this, FinishedInfoActivity::class.java)
                }
            }
            //upload was neither confirmed nor denied yet
            else -> {
                null
            }
        }
        //go to the respective Activity or do nothing
        if (intent != null) {
            this.startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = when (prefs.getInt("upload", -1)) {
            0 -> { //0 means upload was denied
                Intent(this, FinishedInfoDenyActivity::class.java)
            }
            1 -> {//1 means the user allowed upload
                if (prefs.getBoolean("uploadSuccessful", false)) { //uploadSuccessful means all data has been uploaded if true
                    Intent(this, FinishedCompleteActivity::class.java)
                } else { //uploadSuccessful is false if not all data has been uploaded (this is also the default if the variable isn't found)
                    Intent(this, FinishedInfoActivity::class.java)
                }
            }
            //upload was neither confirmed nor denied yet
            else -> {
                null
            }
        }
        //go to the respective Activity or do nothing
        if (intent != null) {
            this.startActivity(intent)
        }
    }

    //called when a button is pressed
    fun onClick(v: View) {
        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)
        //if the send button was pressed (the view's name is confusing, it's UploadDenyConfirmationActivity's layout's send button)
        val intent: Intent = if (v.id == binding.uploadDenyConfirmationSendButton.id) {
            //update the upload prefs variable
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
            //update the upload prefs variable
            prefs.edit().putInt("upload", 0).apply()
            //move to FinishedInfoDenyActivity, as upload was denied
            Intent(this, FinishedInfoDenyActivity::class.java)
        }
        this.startActivity(intent)
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