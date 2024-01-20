package com.wkuxr.sunsketcher.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.wkuxr.sunsketcher.databinding.ActivityLearnMoreBinding
import com.wkuxr.sunsketcher.fragments.LearnMoreFragment

class LearnMoreActivity : AppCompatActivity() {
    lateinit var binding: ActivityLearnMoreBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearnMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //keep track of the current page of the learn more section in order to properly switch pages
    var currentScreen = 0

    fun changeScreen(v: View) {
        //this function is only called by two buttons, so we can use this check to determine
        // whether to go forward or back a page based on the view that is passed in
        var increment: Int = if(v.id == binding.learnMoreNext.id){
            1
        } else {
            -1
        }

        //update the screen value, limiting to 0-4 (because there are only 5 learn more pages)
        currentScreen += increment
        if (currentScreen < 0){
            currentScreen = 0
        }
        else if (currentScreen > 4){
            currentScreen = 4
        }

        //set the interactive buttons visible or gone based on what page is currently being displayed
        if (currentScreen == 4){    //last page, next is gone, done is visible, previous is visible by implication
            binding.learnMoreNext.visibility = View.GONE
            binding.learnMoreDone.visibility = View.VISIBLE
        }
        else if (currentScreen == 0){   //first page, next is visible, previous is gone, done is gone by implication
            binding.learnMorePrevious.visibility = View.GONE
        }
        else {  //any other page, next and previous are visible, done is gone
            binding.learnMorePrevious.visibility = View.VISIBLE
            binding.learnMoreNext.visibility = View.VISIBLE
            binding.learnMoreDone.visibility = View.GONE
        }

        //change the screen via the implementation created in the fragment
        binding.learnMoreFragmentContainer.getFragment<LearnMoreFragment>().changeScreen(currentScreen)

        //if on the final page of the learn more screen, make the learn even more button visible
        if(currentScreen < 4) {
            binding.learnEvenMoreButton.visibility = View.GONE
        } else {
            binding.learnEvenMoreButton.visibility = View.VISIBLE
        }
    }

    fun learnEvenMore(v: View){
        val uri = Uri.parse("https://sunsketcher.org")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    fun learnMoreCompleted(v: View){
        finish() //return to previous activity
    }
}