package com.wkuxr.sunsketcher.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.databinding.FragmentLearnMoreBinding

class LearnMoreFragment : Fragment() {
    private lateinit var binding: FragmentLearnMoreBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentLearnMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun changeScreen(screenNum: Int){
        when (screenNum){
            0 -> {
                binding.learnMoreFragmentTitle.text = "What is SunSketcher?"
                binding.learnMoreFragmentImage.setImageResource(R.drawable.bailys_bead_learn_more)
                binding.learnMoreFragmentText.text = "SunSketcher is a collaborative, crowd-sourced experiment to collect observations during the 2024 total solar eclipse that will determine the exact shape of the sun."
            }
            1 -> {
                binding.learnMoreFragmentTitle.text = "How does the app help?"
                binding.learnMoreFragmentImage.setImageResource(R.drawable.eclipse_timelapse_learn_more)
                binding.learnMoreFragmentText.text = "Using the app, your phone will capture images of the Sun during the eclipse as it forms Baily's Beads, small flashes of light at the rim of the solar disc."
            }
            2 -> {
                binding.learnMoreFragmentTitle.text = "Why capture Baily's Beads?"
                binding.learnMoreFragmentImage.setImageResource(R.drawable.moon_learn_more)
                binding.learnMoreFragmentText.text = "Solar physicists use Baily's Beads in conjunction with a detailed model of the moon in order to triangulate points on the Sun's surface to determine its shape and size."
            }
            3 -> {
                binding.learnMoreFragmentTitle.text = "Why study the Sun's shape?"
                binding.learnMoreFragmentImage.visibility = View.VISIBLE
                binding.learnMoreFragmentImage.setImageResource(R.drawable.flowers_learn_more)
                binding.learnMoreFragmentText.text = "A more accurate model for the size and shape of the sun would help astrophysicists learn more about the flow of the Sun's interior and its implications for popular models of gravity."
            }
            else -> {
                binding.learnMoreFragmentTitle.text = "Curious about the finer details?"
                binding.learnMoreFragmentImage.visibility = View.INVISIBLE
                binding.learnMoreFragmentText.text = "Awesome! You can check out our full website at sunsketcher.org, or press the button above!"
            }
        }
    }
}