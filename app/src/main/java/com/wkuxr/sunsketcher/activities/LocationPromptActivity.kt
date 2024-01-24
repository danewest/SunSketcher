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
        if(v.id == binding.locationTutorialButton.id){
            //TODO: go to countdown
        } else {
            var intent = Intent(this, LocationWarningActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}