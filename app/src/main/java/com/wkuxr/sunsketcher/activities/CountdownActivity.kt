package com.wkuxr.sunsketcher.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.text.bold
import com.wkuxr.sunsketcher.databinding.ActivityCountdownBinding
import com.wkuxr.sunsketcher.location.LocToTime
import com.wkuxr.sunsketcher.location.LocationAccess
import com.wkuxr.sunsketcher.location.LocationAccess.LocationResultCallback
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class CountdownActivity : AppCompatActivity() {
    companion object {
        lateinit var singleton: CountdownActivity
    }
    lateinit var binding: ActivityCountdownBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountdownBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        singleton = this
        var str = SpannableStringBuilder("Please turn your ringer ").bold{append("off")}.append(" and Do Not Disturb ").bold{append("on!")}
        binding.countdownInfoText.text = str

        var str2 = SpannableStringBuilder("Set the phone down with the ").bold{append("REAR CAMERA")}.append(" facing the Sun. Enjoy totality, you can check your phone again 5 minutes after totality ends.")

        getLocation()

    }

    var timer: Timer? = null

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //request permission again if it wasn't given
            requestPermissions(arrayOf("android.permission.ACCESS_FINE_LOCATION"), 1)
        } else {
            binding.countdownLocationDetailsText.text = "Getting GPS Location"

            //prevent phone from automatically locking
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val locAccess = LocationAccess(this)
            locAccess.getCurrentLocation(object : LocationResultCallback {
                override fun onLocationResult(location: Location) {
                    var lat = location.latitude
                    var lon = location.longitude
                    val alt = location.altitude

                    //todo: for testing
                    lat = 47.6683
                    lon = -60.7450

                    //get actual device location for eclipse timing TODO: use for actual app releases
                    val eclipseData = LocToTime.calculatefor(lat, lon, alt)

                    //spoof location for eclipse testing; TODO: remove for actual app releases
                    //val eclipseData = LocToTime.calculatefor(37.60786, -91.02687, 0.0); //4/8/2024
                    //String[] eclipseData = LocToTime.calculatefor(31.86361, -102.37163, 0); //10/14/2023
                    //String[] eclipseData = LocToTime.calculatefor(36.98605, -86.45146, 0); //8/21/2017

                    //get actual device location for sunset timing (test stuff) TODO: remove for actual app releases
                    //val sunsetTime = Sunset.calcSun(lat, -lon) //make longitude negative as the sunset calculations use a positive westward latitude as opposed to the eclipse calculations using a positive eastward latitude

                    //make sure the user is actually in eclipse path before trying to do any scheduling stuff
                    if (!eclipseData[0].equals("N/A")) {
                        //val times = convertTimes(eclipseData)     //TODO: use for actual app releases
                        val times = testConvertTimes(eclipseData) //TODO: remove for actual app releases

                        //use the given times to create calendar objects to use in setting alarms
                        /*val timeCals = arrayOfNulls<Calendar>(2)
                        timeCals[0] = Calendar.getInstance()
                        timeCals[0]?.timeInMillis = times[0] * 1000
                        timeCals[1] = Calendar.getInstance()
                        timeCals[1]?.timeInMillis = times[1] * 1000*/

                        //for the final app, might want to add something that makes a countdown timer on screen tick down TODO: working on that right now, past me
                        //String details = "You are at lat: " + lat + ", lon: " + lon + "; The solar eclipse will start at the following time at your current location: " + timeCals[0].getTime(); //TODO: use for actual app releases
                        //val details = "The app will now swap to the camera, where you will have 45 seconds to adjust the phone's position before it starts taking photos." //TODO: remove for actual app releases
                        val details = "Your location:\nLatitude: $lat\nLongitude: $lon"
                        //String details = "lat: " + lat + "; lon: " + lon + "; Sunset Time: " + timeCals[0].getTime(); //TODO: remove for actual app releases
                        Log.d("Timing", details)
                        binding.countdownLocationDetailsText.text = details
                        //--------made it visible that something is happening--------

                        //store the unix time for the start and end of totality in SharedPreferences
                        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE).edit()
                        prefs.putLong("startTime", times[0] * 1000)
                        prefs.putLong("endTime", times[1] * 1000)
                        prefs.putFloat("lat", lat.toFloat())
                        prefs.putFloat("lon", lon.toFloat())
                        prefs.putFloat("alt", alt.toFloat())
                        prefs.apply()

                        //go to camera 60 seconds prior, start taking images 15 seconds prior to 5 seconds after, and then at end of eclipse 5 seconds before and 15 after TODO: also for the sunset timing
                        //val date = Date((times[0] - 60) * 1000); //TODO: use
                        //the next line is a testcase to make sure functionality works for eclipse timing
                        //val date = Date(System.currentTimeMillis() + 5000) //TODO: remove
                        //Log.d("SCHEDULE_CAMERA", date.toString())
                        if (timer == null) {
                            Log.d("Timing", "Creating timer.")
                            timer = Timer()
                            //val cameraActivitySchedulerTask = TimeTask()
                            //timer!!.schedule(cameraActivitySchedulerTask, date)
                            /*var countdownTimeDiff = ((times[0] * 1000) - 60 * 1000) - System.currentTimeMillis() //TODO: use
                            if(countdownTimeDiff <= 0){
                                countdownTimeDiff = 5000L;
                            }*/
                            val countdownTimeDiff = 5000L //TODO: remove
                            object : CountDownTimer(countdownTimeDiff, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    var seconds = millisUntilFinished / 1000
                                    var minutes = seconds / 60
                                    seconds %= 60
                                    val hours = minutes / 60
                                    minutes %= 60

                                    binding.countdownTimeText.text = "${if(hours > 0){"$hours:"} else {""}}${if(minutes > 0){"${if(minutes < 10){"0"}else{""} + "$minutes"}:"} else {"0:"}}${if(seconds < 10){"0"}else{""} + "$seconds"} UNTIL FIRST PHOTO IS TAKEN"
                                }

                                override fun onFinish() {
                                    val intent = Intent(singleton, CameraActivity::class.java)
                                    singleton.startActivity(intent)
                                }
                            }.start()

                        }
                    } else {
                        binding.countdownLocationDetailsText.text = "Not in eclipse path. Please enter the path of totality before pressing the start button."
                    }
                }

                override fun onLocationFailed() {
                    binding.countdownLocationDetailsText.text = "Unable to get location. Did you allow access permissions?"
                }
            })
        }
    }

    //TimerTask subclass that opens the CameraActivity at the specified time
    internal class TimeTask : TimerTask() {
        var context: Context = singleton

        override fun run() {
            val intent = Intent(context, CameraActivity::class.java)
            context.startActivity(intent)
        }
    }

    //convert `hh:mm:ss` format string to unix time (this version is specifically for Apr. 8, 2024 eclipse, the first number in startUnix and endUnix will need to be modified to the unix time for the start of Oct. 14, 2023 for that test
    fun convertTimes(data: Array<String>): LongArray {
        val start = data[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val end = data[1].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        //add actual time to unix time of UTC midnight for start of that day
        val startUnix = 1712534400 + start[0].toInt() * 3600L + start[1].toInt() * 60L + start[2].toInt() //todo: for april 8
        val endUnix = 1712534400 + end[0].toInt() * 3600L + end[1].toInt() * 60L + end[2].toInt()
        //long startUnix = 1697241600 + ((Integer.parseInt(start[0])) * 3600L) + (Integer.parseInt(start[1]) * 60L) + Integer.parseInt(start[2]);     //todo: for october 14
        //long endUnix = 1697241600 + ((Integer.parseInt(end[0])) * 3600L) + (Integer.parseInt(end[1]) * 60L) + Integer.parseInt(end[2]);
        return longArrayOf(startUnix, endUnix)
    }

    fun testConvertTimes(data: Array<String>): LongArray {
        //0 -> hour; 1 -> minute; 2 -> second
        val start = data[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val end = data[1].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        //get current time in seconds, remove a day if it is past UTC midnight for the date that your timezone is currently in
        var currentDateUnix = System.currentTimeMillis() / 1000
        val currentTimeUnix = currentDateUnix % 86400
        if (currentTimeUnix > 0 && currentTimeUnix < 5 * 60 * 60) {
            Log.d("testConvertTimes", "Current time is past UTC midnight; Subtracting a day from time estimate")
            currentDateUnix -= 86400
        }
        val currentDateTimezoneCorrectedUnix = currentDateUnix - (currentDateUnix % (60 * 60 * 24)) // - (-5 * 60 * 60); //add this +5 hours back for sunset tests

        //convert the given time to seconds, add it to the start of the day as calculated by
        val startUnix = currentDateTimezoneCorrectedUnix + start[0].toInt() * 3600L + start[1].toInt() * 60L + start[2].toInt()
        val endUnix = currentDateTimezoneCorrectedUnix + end[0].toInt() * 3600L + end[1].toInt() * 60L + end[2].toInt()
        return longArrayOf(startUnix, endUnix)
    }

    fun onArrowClick(v: View){
        if(v.id == binding.countdownArrowRight.id){
            binding.countdownArrowRight.visibility = View.GONE
            binding.countdownLocationDetailsText.visibility = View.GONE
            binding.countdownInfoText.visibility = View.GONE
            binding.countdownDoNotDisturbImage.visibility = View.GONE


            binding.countdownArrowLeft.visibility = View.VISIBLE
            binding.countdownPhoneStandImage.visibility = View.VISIBLE
            binding.countdownInfoPage2Text.visibility = View.VISIBLE
        } else {
            binding.countdownArrowLeft.visibility = View.GONE
            binding.countdownPhoneStandImage.visibility = View.GONE
            binding.countdownInfoPage2Text.visibility = View.GONE

            binding.countdownArrowRight.visibility = View.VISIBLE
            binding.countdownLocationDetailsText.visibility = View.VISIBLE
            binding.countdownInfoText.visibility = View.VISIBLE
            binding.countdownDoNotDisturbImage.visibility = View.VISIBLE
        }
    }
}