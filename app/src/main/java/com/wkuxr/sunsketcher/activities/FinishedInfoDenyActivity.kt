package com.wkuxr.sunsketcher.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.databinding.ActivityFinishedInfoDenyBinding

class FinishedInfoDenyActivity : AppCompatActivity() {
    lateinit var binding: ActivityFinishedInfoDenyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishedInfoDenyBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun onSciStarterClick(v: View){
        val uri = Uri.parse("https://scistarter.org/form/180")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}