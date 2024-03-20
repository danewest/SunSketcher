package com.wkuxr.sunsketcher.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.databinding.FragmentTutorialBinding

class TutorialFragment : Fragment() {
    private lateinit var binding: FragmentTutorialBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun changeScreen(screenNum: Int){
        when (screenNum){
            0 -> {
                binding.tutorialFragmentTitle.text = "Before you start"
                binding.tutorialFragmentImage.setImageResource(R.drawable.do_not_disturb_tutorial)
                binding.tutorialFragmentText.text = "In order to ensure smooth operation, please enable the 'Do Not Disturb' feature of your phone. You can disable it again after your images have been captured, after the total eclipse ends."
            }
            1 -> {
                binding.tutorialFragmentTitle.text = "Step one"
                binding.tutorialFragmentImage.setImageResource(R.drawable.main_screen_tutorial)
                var str = SpannableStringBuilder("Approximately 5 minutes before totality, press the start button on the main screen. ").bold{append("Do not")}.append(" press start until you are at the location you will view the eclipse from!")
                binding.tutorialFragmentText.text = str
            }
            2 -> {
                binding.tutorialFragmentTitle.text = "Step two"
                binding.tutorialFragmentImage.setImageResource(R.drawable.phone_stand_tutorial)
                binding.tutorialFragmentText.text = "Once the countdown timer is visible, place your device against a hard surface or on a phone stand, with the screen facing away from the Sun, and the back camera(s) facing towards it.\nPlease do not touch your phone again until after totality has ended.\n\nPlease do not place a solar filter over the phone's camera(s)!\nWhile we humans have sensitive eyes that require a solar filter to be able to safely look at the Sun, rest assured that aiming a camera at the Sun will incur no damages to it, especially over such a short period of time. A phone's camera has nothing organic like the cells in our eyes do, and, as such, cannot be damaged by contact with sunlight unless over very long periods, about the same amount of time it might take for a piece of cloth to become sun-bleached. The SunSketcher team has performed extensive testing related to the effects of sunlight on phone cameras and even ran an early test during the October 14th, 2023 annular eclipse (which, due to being annular as opposed to total, results in more light hitting the camera's sensor) and saw no adverse effects, whether lasting or temporary, with any phone used."
            }
            3 -> {
                binding.tutorialFragmentTitle.text = "Step three"
                binding.tutorialFragmentImage.setImageResource(R.drawable.eclipse_tutorial)
                binding.tutorialFragmentText.text = "Enjoy the eclipse! Do NOT disturb the phone until one minute after totality ends. It continues to take photos for a few seconds even after totality."
            }
            else -> {
                binding.tutorialFragmentTitle.text = "Step four"
                binding.tutorialFragmentImage.setImageResource(R.drawable.confirm_deny_tutorial)
                binding.tutorialFragmentText.text = "Press \"yes\" when asked if you would like to upload your photos for data collection, in which case you will have contributed to our study of the Sun! To make sure we receive your data, please don't delete the app until you are notified that your images have been sent."
            }
        }
    }
}