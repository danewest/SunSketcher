package com.wkuxr.sunsketcher.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.databinding.ActivityLocationPromptBinding
import kotlin.system.exitProcess

class LocationPromptActivity : AppCompatActivity() {
    lateinit var binding: ActivityLocationPromptBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //called when a button on screen is pressed
    fun onClick(v: View){
        val intent: Intent = if(v.id == binding.locationTutorialButton.id){
            //go to countdown if user says they are generally at the location their device will be at during totality
            Intent(this, CountdownActivity::class.java)
        } else {
            //go to the LocationWarningActivity if the user says they are not at the location their device will be at during totality
            Intent(this, LocationWarningActivity::class.java)
        }
        startActivity(intent)

        //remove this Activity from the Activity stack (so if the next activity is closed, the app returns to the one before this
        finish()
    }
}