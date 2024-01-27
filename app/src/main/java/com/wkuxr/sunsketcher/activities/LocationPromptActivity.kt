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

    fun onClick(v: View){
        val intent: Intent = if(v.id == binding.locationTutorialButton.id){
            Intent(this, CountdownActivity::class.java)
        } else {
            Intent(this, LocationWarningActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}