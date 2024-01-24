package com.wkuxr.sunsketcher.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.wkuxr.sunsketcher.databinding.ActivityLocationWarningBinding

class LocationWarningActivity : AppCompatActivity() {
    lateinit var binding: ActivityLocationWarningBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationWarningBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun onClick(v: View){
        finish()
    }
}