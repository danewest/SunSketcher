package com.wkuxr.sunsketcher.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wkuxr.sunsketcher.databinding.ActivityTutorialPromptBinding

class TutorialPromptActivity : AppCompatActivity() {
    lateinit var binding: ActivityTutorialPromptBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun viewTutorial() {
        val intent = Intent(this, TutorialActivity::class.java)
        this.startActivity(intent)
    }

    fun skipTutorial() {
        //val intent = Intent(this, CountdownActivity::class.java)
        //this.startActivity(intent)
    }
}