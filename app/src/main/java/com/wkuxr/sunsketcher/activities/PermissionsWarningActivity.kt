package com.wkuxr.sunsketcher.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.wkuxr.sunsketcher.databinding.ActivityPermissionsWarningBinding

class PermissionsWarningActivity : AppCompatActivity() {
    lateinit var binding: ActivityPermissionsWarningBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsWarningBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //return to the previous activity when clicked
    fun onClick(v: View){
        finish()
    }
}