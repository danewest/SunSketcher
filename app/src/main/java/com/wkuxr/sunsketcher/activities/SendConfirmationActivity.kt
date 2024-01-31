package com.wkuxr.sunsketcher.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.database.Metadata
import com.wkuxr.sunsketcher.database.MetadataDB
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.createDB
import com.wkuxr.sunsketcher.database.MetadataDB.Companion.db
import com.wkuxr.sunsketcher.databinding.ActivitySendConfirmationBinding
import com.wkuxr.sunsketcher.networking.UploadScheduler
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File


class SendConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivitySendConfirmationBinding
    lateinit var recyclerView: RecyclerView

    companion object {
        lateinit var prefs: SharedPreferences
        lateinit var singleton: SendConfirmationActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        singleton = this

        recyclerView = binding.imageRecycler

        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)
        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val hasConfirmDeny = prefs.getInt("upload", -1)
        var intent: Intent? = null
        when (hasConfirmDeny) {
            0 -> intent = Intent(this, FinishedInfoDenyActivity::class.java)
            1 -> intent = if (prefs.getBoolean("uploadSuccessful", false)) { //allowed upload and upload already finished
                Intent(this, FinishedCompleteActivity::class.java)
            } else {
                Intent(this, FinishedInfoActivity::class.java)
            }

            else -> {}
        }
        if (intent != null) {
            this.startActivity(intent)
        }

        if(!prefs.getBoolean("cropped", false)){
            cropImages()
        }

        displayImageList()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("eclipseDetails", MODE_PRIVATE)
        val intent = when (prefs.getInt("upload", -1)) {
            0 -> {
                Intent(this, FinishedInfoDenyActivity::class.java)
            }
            1 -> {
                if (prefs.getBoolean("uploadSuccessful", false)) { //allowed upload and upload already finished
                    Intent(this, FinishedCompleteActivity::class.java)
                } else {
                    Intent(this, FinishedInfoActivity::class.java)
                }
            }
            else -> {
                null
            }
        }
        if (intent != null) {
            this.startActivity(intent)
        }
    }

    fun cropImages(){
        System.loadLibrary("opencv_java4")

        // initialize database as an object
        val db : MetadataDB = createDB(this)

        // get list of all rows (user's images) in metadata
        val metadataList : List<Metadata> = db.getMetadata()

        // get crop box from the center image
        val centerMetadata = metadataList[metadataList.size / 2]

        // create bitmap from image
        val imgBitmap = BitmapFactory.decodeFile(centerMetadata.filepath)

        // convert bitmap to mat
        val imgMat = Mat()
        Utils.bitmapToMat(imgBitmap, imgMat)

        // find the correct crop box using the center image

        val cropBox = getEclipseBox(imgMat)

        // create folder for cropped images to be stored on the phone
        // reference to folder with all cropped images
        val mCropImageFolder: File = createCroppedImageFolder()

        if (mCropImageFolder.exists()) {

            // cropped image folder successfully made!

            // iterate through all images (rows) in database and apply cropping w/ crop box to them
            // then save the cropped image to the cropped image folder and replace db filepath
            for (metadataRow in metadataList) {

                // create bitmap from current image
                val imgOriginal = File(metadataRow.filepath)
                val newImgBitmap = BitmapFactory.decodeFile(metadataRow.filepath)

                var imgMatCropped = Mat()
                Utils.bitmapToMat(newImgBitmap, imgMatCropped)

                if (!cropBox.empty()) {
                    // crop the image using crop box
                    imgMatCropped = Mat(imgMatCropped, cropBox)

                    // create image file in the crop image folder with original image's name
                    val imgCroppedFile = File(mCropImageFolder, imgOriginal.name)

                    // write the cropped image to the cropped image file
                    Imgcodecs.imwrite(imgCroppedFile.absolutePath, imgMatCropped)

                    val exif = ExifInterface(imgOriginal.absolutePath)
                    var fstop = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.toDouble()
                    val iso = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS))
                    val whiteBalance = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE))
                    var exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDouble()
                    var focalDistance = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);

                    if (fstop == null){
                        fstop = 0.0
                    }
                    if (exposure == null){
                        exposure = 0.0
                    }
                    if (focalDistance == null){
                        focalDistance = ""
                    }
                    // save crop img file path to img metadata in database
                    db.updateRowFilepath(metadataRow.id, imgCroppedFile.absolutePath, fstop, iso, whiteBalance, exposure, focalDistance)


                } else {
                    // TODO if for some reason the crop box ends up empty should we just keep the original image in db?
                    continue
                }

            }
        } else {
            Toast.makeText(this@SendConfirmationActivity, "Error creating directory", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putBoolean("cropped", true).apply()
    }

    // creates folder in SunSketchers directory for cropped images. Returns created folder
    private fun createCroppedImageFolder(): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        // reference to original image folder in pictures directory (should already be made)
        val mImageFolder: File = File(picturesDir, "SunSketcher")

        // create the cropped image folder in Pictures/SunSketcher/CroppedImages
        val mCropImageFolder = File(mImageFolder, "CroppedImages")
        mCropImageFolder.mkdirs()

        return mCropImageFolder

    }

    private fun makeImageGreyScale(img: Mat): Mat {
        val imgGrey = Mat()
        Imgproc.cvtColor(img, imgGrey, Imgproc.COLOR_RGB2GRAY)
        return imgGrey
    }

    private fun getEclipseBox(img: Mat): Rect {

        // make image grey scale
        val imgGrey: Mat = makeImageGreyScale(img)

        // apply gaussian blur to image (reduces noise)
        Imgproc.GaussianBlur(imgGrey, imgGrey, Size(5.0, 5.0), 0.0)

        // find brightest spot using minMaxLoc
        val result: Core.MinMaxLocResult = Core.minMaxLoc(imgGrey)
        val maxLocX = result.maxLoc.x.toInt()
        val maxLocY = result.maxLoc.y.toInt()
        println("Max Location- X: $maxLocX Y: $maxLocY")

        // get the image's dimensions based off mat rows and columns
        val imgMaxX: Int = img.cols()
        val imgMaxY: Int = img.rows()

        // side length of box relative to image size, ~2% of original image size
        val side = (Math.sqrt((imgMaxX * imgMaxY).toDouble()) * 0.02)

        // create the coords for rectangle and check if starting/ending coordinates for rectangle are out of image bounds
        val startCoord: Point = boundaryCheck(Point(maxLocX - side, maxLocY - side), imgMaxX, imgMaxY)
        val endCoord: Point = boundaryCheck(Point(maxLocX + side, maxLocY + side), imgMaxX, imgMaxY)

        // create the region of interest (roi) rectangle around brightest spot
        val roi = Rect(startCoord, endCoord)

//        // to check rectangle coordinates and side length
//        System.out.println("""
//    ${"rectangle start x coordinate = " + roi.x}
//    rectangle start y coordinate = ${roi.y}
//    """.trimIndent())
//        System.out.println("""
//    ${"rectangle height = " + roi.height}
//    rectangle width = ${roi.width}
//    """.trimIndent())

        // create the actual crop box to be used on all of the user's images
        val boxStartCoord: Point = boundaryCheck(Point((roi.x - roi.width).toDouble(), (roi.y - roi.width).toDouble()), imgMaxX, imgMaxY)
        val boxEndCoord: Point = boundaryCheck(Point((roi.x + 2 * roi.width).toDouble(), (roi.y + 2 * roi.width).toDouble()), imgMaxX, imgMaxY)

        return Rect(boxStartCoord, boxEndCoord)
    }

    // checks if a coordinate's x and y are out of bounds of a given area
    fun boundaryCheck(pt: Point, maxX: Int, maxY: Int): Point {
        if (pt.x < 0) {
            pt.x = 0.0
        }
        if (pt.x > maxX) {
            pt.x = maxX.toDouble()
        }
        if (pt.y < 0) {
            pt.y = 0.0
        }
        if (pt.y > maxY) {
            pt.y = maxY.toDouble()
        }

        return pt
    }

    fun onClick(v: View) {
        val intent: Intent = if (v.id == binding.sendConfirmationYesBtn.id) {
            prefs.edit().putInt("upload", 1).apply()
            /*if(!foregroundServiceRunning()) { //TODO: add for actual releases
                if(App.getContext() == null)
                    App.setContext(this)
                val uploadSchedulerIntent = Intent(this, UploadScheduler::class.java)
                startService(uploadSchedulerIntent)
            }*/
            Intent(this, FinishedInfoActivity::class.java)
        } else {
            Intent(this, UploadDenyConfirmationActivity::class.java)
        }
        this.startActivity(intent)
    }

    private fun displayImageList() {
        db = createDB(this)
        val metadata = db.getMetadata()
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ItemAdapter(metadata)
    }

    class ItemAdapter(private val metadataList: List<Metadata>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycle_list_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return metadataList.size
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = metadataList[position]
            holder.itemView.tag = item.id
            Log.d("IMAGEFILEPATHS", item.filepath)
            val imgFile = File(item.filepath)

            //create bitmap from image
            val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)

            holder.imgView.setImageBitmap(imgBitmap)
        }

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imgView: ImageView = itemView.findViewById(R.id.itemImage)
        }
    }

    fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (UploadScheduler::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}