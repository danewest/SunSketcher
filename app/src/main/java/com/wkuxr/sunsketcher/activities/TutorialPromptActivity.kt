package com.wkuxr.sunsketcher.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wkuxr.sunsketcher.databinding.ActivityTutorialPromptBinding

class TutorialPromptActivity : AppCompatActivity() {
    lateinit var binding: ActivityTutorialPromptBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //open the tutorial
    fun viewTutorial(v: View) {
        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        //specify that it needs to go to the LocationPromptActivity next
        val prefEdit = prefs.edit()
        prefEdit.putInt("next", 1)
        prefEdit.apply()
        
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
        finish()
    }

    //skip the tutorial and go straight to the LocationPromptActivity
    fun skipTutorial(v: View) {
        val intent = Intent(this, LocationPromptActivity::class.java)
        this.startActivity(intent)
        finish()
    }
}