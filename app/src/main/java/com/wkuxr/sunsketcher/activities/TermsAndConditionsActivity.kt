package com.wkuxr.sunsketcher.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.bold
import com.wkuxr.sunsketcher.databinding.ActivityTermsAndConditionsBinding

class TermsAndConditionsActivity : AppCompatActivity() {
    lateinit var binding: ActivityTermsAndConditionsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsAndConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //build a formatted string for the terms and conditions
        var str = SpannableStringBuilder("").bold{append("I.\tThe Application")}
            .append("\n\tSunSketcher (“Licensed Application” or “App” hereinafter) is a piece of " +
                    "software created to photograph the 2024 Great North American Eclipse. The App " +
                    "is customized for iOS and Android mobile devices (“Devices”), and is owned by " +
                    "WKU. The App is used with phones located in the continental United States" +
                    "to take a series of photographs of the totality of the " +
                    "Great North American Eclipse on April 8, 2024. With user consent, the images " +
                    "collected by each user of the App (“User Content”) will be sent to a database " +
                    "along with the time and location (longitude, latitude, and altitude) at which " +
                    "the images were taken. Normal data rates may apply." +
                    "\n\nTo learn more about the SunSketcher App, and the " +
                    "Great North American Eclipse, see https://sunsketcher.org/faq.php.\n\n")
            .bold{append("II.\tConditions of Use")}
            .append("\n\tBy using this app, you certify that you have read and reviewed this " +
                    "Agreement and that you agree to comply with its terms. If you do not want to " +
                    "be bound by the terms of this Agreement, you are advised to stop using the " +
                    "App. Use and access to this App, its products, and its services is only " +
                    "available to those who have accepted the terms and conditions contained herein.\n\n")
            .bold{append("III.\tPrivacy Policy: Data Collection")}
            .append("\n\tThe App will, with your consent, take and collect a series of images from " +
                    "your Device during the Great North American Eclipse on April 8, 2024. The App " +
                    "will also, with your consent, collect your location (longitude, latitude, and " +
                    "altitude), when images of the Great North American Eclipse are collected from " +
                    "your Device on April 8, 2024. Other than location, this App will not collect " +
                    "or store any personal information. See https://sunsketcher.org/privacy.php." +
                    "\n\nYou may elect to register as a SunSketcher (https://sunsketcher.org/register.php), " +
                    "by providing your email address to receive updates from SunSketcher; however, " +
                    "providing your email address is optional, meaning that you are not required to " +
                    "provide the data in order for the App to function.\n\n")
            .bold{append("IV.\tAge Restriction")}
            .append("\n\tIf you are under the age of 13, you should not use this App. If you are a " +
                    "minor between the ages of 13-18, you may use this App only in conjunction with " +
                    "your parent, legal guardian, or other responsible adult.\n\nThe above stated " +
                    "age restrictions are necessary to ensure compliance with the Children’s Online " +
                    "Privacy Protection Act (COPPA). 15 U.S.C §§ 6501-6506.\n\n")
            .bold{append("V.\tIntellectual Property")}
            .append("\n\tThe App and its original content (which does not include User Content), are " +
                    "and will remain the exclusive property of SunSketcher. User hereby grants to " +
                    "WKU a perpetual, irrevocable, non-exclusive, royalty free license to use, " +
                    "reproduce, display and publish the User Content uploaded through the App, for " +
                    "any purpose determined by WKU.\n\n")
            .bold{append("VI.\tUpdates and Modifications")}
            .append("\n\tWe reserve the right to modify or change the terms contained herein at any " +
                    "time.\n\n")
            .bold{append("VII.\tApplicable Law")}
            .append("\n\tThis Agreement shall be governed by, construed, and enforced in accordance " +
                    "with the laws of the state of Kentucky (without giving effect to principles of " +
                    "conflicts of law).\n\n")
            .bold{append("VIII.\tSeverability")}
            .append("\n\tIf any provision of these Terms of Use is deemed unlawful, void, or " +
                    "unenforceable for any reason, then that provision will be deemed severable " +
                    "from the remainder of this Agreement and will not affect the validity or " +
                    "enforceability of the remainder of this Agreement.\n\n")
            .bold{append("IX.\tLiability")}
            .append("\n\tYou expressly understand and agree that SunSketcher, Western Kentucky " +
                    "University, its directors, officers, employees, and agents, will not be held " +
                    "liable for any direct, indirect, incidental, special, consequential or " +
                    "punitive damages resulting from the use of the App or inability to use the " +
                    "App.\n\n")
            .bold{append("X.\tContact")}
            .append("\n\tIf you have any questions about the SunSketcher App, please contact:" +
                    "\n\n\t\tWestern Kentucky University" +
                    "\n\t\tGreg Arbuckle" +
                    "\n\t\tgreg.arbuckle@wku.edu")
        //display it
        binding.tacText.text = str
    }

    //return to the previous Activity in the activity stack
    fun onClick(v: View){
        finish()
    }
}