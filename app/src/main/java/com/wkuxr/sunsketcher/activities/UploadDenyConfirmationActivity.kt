package com.wkuxr.sunsketcher.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.activities.SendConfirmationActivity.Companion.prefs
import com.wkuxr.sunsketcher.databinding.ActivityUploadDenyConfirmationBinding

class UploadDenyConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivityUploadDenyConfirmationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadDenyConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    override fun onResume() {
        super.onResume()
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
        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)
        val intent: Intent = if (v.id == binding.uploadDenyConfirmationSendButton.id) {
            prefs.edit().putInt("upload", 1).apply()
            /*if(!foregroundServiceRunning()) { //TODO: add for actual releases
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }*/
            Intent(this, FinishedInfoActivity::class.java)
        } else {
            prefs.edit().putInt("upload", 0).apply()
            Intent(this, FinishedInfoDenyActivity::class.java)
        }
        this.startActivity(intent)
    }
}