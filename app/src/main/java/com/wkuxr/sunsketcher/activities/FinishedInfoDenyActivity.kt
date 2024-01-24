package com.wkuxr.sunsketcher.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.databinding.ActivityFinishedInfoDenyBinding

class FinishedInfoDenyActivity : AppCompatActivity() {
    lateinit var binding: ActivityFinishedInfoDenyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishedInfoDenyBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


}