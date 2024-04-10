package com.wkuxr.sunsketcher.activities

import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wkuxr.sunsketcher.databinding.ActivityTutorialBinding
import com.wkuxr.sunsketcher.fragments.TutorialFragment


class TutorialActivity : AppCompatActivity() {
    lateinit var binding: ActivityTutorialBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //keep track of the current page of the tutorial in order to properly switch pages
    var currentScreen = 0

    fun changeScreen(v: View) {
        //this function is only called by two buttons, so we can use this check to determine
        // whether to go forward or back a page based on the view that is passed in
        var increment: Int = if(v.id == binding.tutorialNext.id){
            1
        } else {
            -1
        }

        //update the screen value, limiting to 0-4 (because there are only 5 tutorial pages)
        currentScreen += increment
        if (currentScreen < 0){
            currentScreen = 0
        }
        else if (currentScreen > 4){
            currentScreen = 4
        }

        //set the interactive buttons visible or gone based on what page is currently being displayed
        if (currentScreen == 4){    //last page, next is gone, done is visible, previous is visible by implication
            binding.tutorialNext.visibility = View.GONE
            binding.tutorialDone.visibility = View.VISIBLE
        }
        else if (currentScreen == 0){   //first page, next is visible, previous is gone, done is gone by implication
            binding.tutorialPrevious.visibility = View.GONE
        }
        else {  //any other page, next and previous are visible, done is gone
            binding.tutorialPrevious.visibility = View.VISIBLE
            binding.tutorialNext.visibility = View.VISIBLE
            binding.tutorialDone.visibility = View.GONE
        }

        //change the screen via the implementation created in the fragment
        binding.tutorialFragmentContainer.getFragment<TutorialFragment>().changeScreen(currentScreen)
    }

    fun tutorialCompleted(v: View){
        //set a preference variable so that the app keeps track of the fact that the tutorial has been completed
        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val prefEdit = prefs.edit()
        prefEdit.putBoolean("completedTutorial", true)
        prefEdit.apply()

        //prefs value of int next is 0 when tutorial was selected from main screen, and completion will return to main screen
        //prefs value of int next is 1 when tutorial was selected from prompt after pressing start button, and completion will send to countdown activity
        when (prefs.getInt("next", 0)) {
            1 -> {
                var intent = Intent(this, LocationPromptActivity::class.java)
                startActivity(intent)
                finish()
            }
            else -> {
                finish()
            }
        }
    }
}