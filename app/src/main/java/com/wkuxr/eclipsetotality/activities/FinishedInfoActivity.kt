package com.wkuxr.eclipsetotality.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wkuxr.eclipsetotality.databinding.ActivityFinishedInfoBinding

class FinishedInfoActivity : AppCompatActivity() {

    lateinit var binding: ActivityFinishedInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishedInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}