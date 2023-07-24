package com.wkuxr.eclipsetotality.activities

import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.wkuxr.eclipsetotality.activities.SendConfirmationActivity.Companion.prefs
import com.wkuxr.eclipsetotality.databinding.ActivityFinishedInfoBinding
import com.wkuxr.eclipsetotality.networking.ClientRunOnTransfer.clientTransferSequence

class FinishedInfoActivity : AppCompatActivity() {

    lateinit var binding: ActivityFinishedInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishedInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val uploadReady = prefs.getInt("upload", 0)

        var text = binding.infoText

        if(uploadReady == 0){
            text.text = "Thank you for using the SunSketcher app. You have chosen not to upload your images for analysis. You no longer need the app installed and can now uninstall it. Even after uninstalling, your eclipse photos can still be found in the `Photos/SunSketcher/` directory in your device storage, or in your gallery."
        } else {
            text.text = "Thank you for using the SunSketcher app. You have chosen to upload your images for analysis. Please keep the app installed and do not delete the photos in the `Pictures/SunSketcher/` directory, or the SunSketcher album in your gallery, until further notice. You will receive a notification when your images have been uploaded, at which point you can freely delete the app and images from your device. This may take more than a month, so we appreciate your patience."
        }
    }

    fun onUploadClick(v: Button){
        try {
            prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)
            clientTransferSequence()
            v.text = "uploading, please wait..."

            while(prefs.getInt("finishedUpload", 0) == 0);
            v.text = "upload complete"
        } catch (e: Exception){
            e.printStackTrace();
        }
    }
}