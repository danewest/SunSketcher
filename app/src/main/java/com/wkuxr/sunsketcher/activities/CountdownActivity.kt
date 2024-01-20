package com.wkuxr.sunsketcher.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wkuxr.sunsketcher.databinding.ActivityCountdownBinding

class CountdownActivity : AppCompatActivity() {
    lateinit var binding: ActivityCountdownBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountdownBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //TODO: make countdown functionality
}