package com.wkuxr.sunsketcher.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.databinding.ActivityTutorialBinding

class TutorialActivity : AppCompatActivity() {
    lateinit var binding: ActivityTutorialBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}