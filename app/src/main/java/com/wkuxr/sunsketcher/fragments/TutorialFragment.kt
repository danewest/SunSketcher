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
                binding.tutorialFragmentText.text = "Place your device against a hard surface or on a phone stand, with the rear camera facing the Sun.\nAt this point, please do not touch your phone again until after totality has ended."
            }
            3 -> {
                binding.tutorialFragmentTitle.text = "Step three"
                binding.tutorialFragmentImage.setImageResource(R.drawable.eclipse_tutorial)
                binding.tutorialFragmentText.text = "Enjoy the eclipse! Do NOT disturb the phone until one minute after totality ends. The phone will make a sound and flash when it is done and can be used again."
            }
            else -> {
                binding.tutorialFragmentTitle.text = "Step four"
                binding.tutorialFragmentImage.setImageResource(R.drawable.confirm_deny_tutorial)
                binding.tutorialFragmentText.text = "Press \"yes\" when asked if you would like to upload your photos for data collection, in which case you will have contributed to our study of the Sun! To make sure we receive your data, please don't delete the app until you are notified that your images have been sent."
            }
        }
    }
}