package com.wkuxr.sunsketcher.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wkuxr.sunsketcher.R
import com.wkuxr.sunsketcher.database.Metadata
import com.wkuxr.sunsketcher.database.MetadataDB
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.util.concurrent.Executors

class ImageCroppingActivity : AppCompatActivity() {
    companion object {
        lateinit var prefs: SharedPreferences
        var numImages: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_cropping)

        //initialize the database if it isn't already and get the number of entries in it
        numImages = MetadataDB.createDB(this).getMetadata().size

        //prevent the screen from locking while on this activity
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //get the shared preferences
        prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE)

        //crop images on a background thread
        Executors.newSingleThreadExecutor().execute{
            while (!prefs.getBoolean("cropped", false)) {
                cropImages()
            }

            //once cropping has finished, update the cropped variable in shared preferences and move to the SendConfirmationActivity
            prefs.edit().putBoolean("cropped", true).apply()
            val intent = Intent(this, SendConfirmationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        //don't want things to be unrecoverably messed up, so move to the SendConfirmationActivity if the app is resumed
        if (!prefs.getBoolean("cropped", true)) {
            val intent = Intent(this, SendConfirmationActivity::class.java)
            startActivity(intent)
        }
    }

    var cropBox: Rect? = null

    private fun cropImages() {
        System.loadLibrary("opencv_java4")

        // initialize database as an object
        val db: MetadataDB = MetadataDB.createDB(this)

        // get list of all rows (user's images) in metadata
        val metadataList: List<Metadata> = db.getMetadata()

        // get crop box from the center image
        val centerMetadata = metadataList[metadataList.size / 2]

        // create bitmap from image
        val imgBitmap = BitmapFactory.decodeFile(centerMetadata.filepath)

        // convert bitmap to mat
        val imgMat = Mat()
        Utils.bitmapToMat(imgBitmap, imgMat)

        // find the correct crop box using the center image

        if(cropBox == null) {
            cropBox = getEclipseBox(imgMat)
        }

        // create folder for cropped images to be stored on the phone
        // reference to folder with all cropped images
        val mCropImageFolder: File = createCroppedImageFolder()

        Log.d("ImageCropping", "mCropImageFolder: " + mCropImageFolder.absolutePath + "; cropBox: " + cropBox)

        if(mCropImageFolder.exists()) {
            if (!cropBox!!.empty()) {

                // cropped image folder successfully made!

                // iterate through all images (rows) in database and apply cropping w/ crop box to them
                // then save the cropped image to the cropped image folder and replace db filepath
                var numCropped = 0
                val numToCrop = 20
                for (metadataRow in metadataList) {
                    if (metadataRow.isCropped) {
                        //don't crop images that have already been cropped
                        continue
                    }
                    if (numCropped >= numToCrop) {
                        //will only be true if we aren't on the final round of images to crop, so we say that not all of the images have been cropped (because we assume at the end of the for loop that this is the final round, and we need the app to know that it isn't)
                        prefs.edit().putBoolean("cropped", false).commit() //synchronously update the shared preferences
                        break
                    }

                    // create bitmap from current image
                    val imgOriginal = File(metadataRow.filepath)
                    val newImgBitmap = BitmapFactory.decodeFile(metadataRow.filepath)

                    var imgMatCropped = Mat()
                    Utils.bitmapToMat(newImgBitmap, imgMatCropped)

                    Imgproc.cvtColor(imgMatCropped, imgMatCropped, Imgproc.COLOR_BGR2RGB)


                    // crop the image using crop box
                    imgMatCropped = Mat(imgMatCropped, cropBox)

                    // create image file in the crop image folder with original image's name
                    val imgCroppedFile = File(mCropImageFolder, imgOriginal.name)

                    // write the cropped image to the cropped image file
                    Imgcodecs.imwrite(imgCroppedFile.absolutePath, imgMatCropped)

                    val exif = ExifInterface(imgOriginal.absolutePath)
                    var fstop = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.toDouble()
                    val iso =
                        Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS))
                    val whiteBalance =
                        Integer.parseInt(exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE))
                    var exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDouble()
                    var focalDistance = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);

                    if (fstop == null) {
                        fstop = 0.0
                    }
                    if (exposure == null) {
                        exposure = 0.0
                    }
                    if (focalDistance == null) {
                        focalDistance = ""
                    }
                    // save crop img file path to img metadata in database
                    db.updateRowFilepath(
                        metadataRow.id,
                        imgCroppedFile.absolutePath,
                        fstop,
                        iso,
                        whiteBalance,
                        exposure,
                        focalDistance,
                        true
                    )
                    numCropped++

                    Log.d(
                        "ImageCropping",
                        "Image ${metadataRow.id} has been cropped: ${metadataRow.filepath}"
                    )

                    //assume this call of the cropImages function is the final call to it
                    prefs.edit().putBoolean("cropped", true).commit()
                }

                Log.d("ImageCropping", "$numCropped images have been cropped successfully.")
            } else {
                //something's going horribly wrong, skip cropping
                Log.e("ImageCropping", "Failed to find crop bounding box.")
                prefs.edit().putBoolean("cropped", true).commit()

                val intent = Intent(this, SendConfirmationActivity::class.java)
                startActivity(intent)
            }
        } else {
            //something's going horribly wrong, skip cropping
            Log.e("ImageCropping", "Failed to get image folder.")
            prefs.edit().putBoolean("cropped", true).commit()

            val intent = Intent(this, SendConfirmationActivity::class.java)
            startActivity(intent)
        }


    }

    // creates folder in SunSketchers directory for cropped images. Returns created folder
    private fun createCroppedImageFolder(): File {
        //return the root directory of the app's internal files
        return filesDir
    }

    //convert the image material to greyscale
    private fun makeImageGreyScale(img: Mat): Mat {
        val imgGrey = Mat()
        Imgproc.cvtColor(img, imgGrey, Imgproc.COLOR_RGB2GRAY)
        return imgGrey
    }

    //get the bounding box for the given image material
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
        val imgMaxX: Int = img.rows()
        val imgMaxY: Int = img.cols()

        // side length of box relative to image size, ~2% of original image size
        val side = (Math.sqrt((imgMaxX * imgMaxY).toDouble()) * 0.02)

        // create the coords for rectangle and check if starting/ending coordinates for rectangle are out of image bounds
        val startCoord: Point =
            boundaryCheck(Point(maxLocX - side, maxLocY - side), imgMaxX, imgMaxY)
        val endCoord: Point = boundaryCheck(Point(maxLocX + side, maxLocY + side), imgMaxX, imgMaxY)

        // create the region of interest (roi) rectangle around brightest spot
        val roi = Rect(startCoord, endCoord)

        // create the actual crop box to be used on all of the user's images
        val boxStartCoord: Point = boundaryCheck(
            Point((roi.x - roi.width).toDouble(), (roi.y - roi.width).toDouble()),
            imgMaxX,
            imgMaxY
        )
        val boxEndCoord: Point = boundaryCheck(
            Point(
                (roi.x + 2 * roi.width).toDouble(),
                (roi.y + 2 * roi.width).toDouble()
            ), imgMaxX, imgMaxY
        )

        return Rect(boxStartCoord, boxEndCoord)
    }

    // checks if a coordinate's x and y are out of bounds of a given area
    private fun boundaryCheck(pt: Point, maxX: Int, maxY: Int): Point {
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
}