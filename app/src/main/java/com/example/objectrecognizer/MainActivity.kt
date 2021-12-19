package com.example.objectrecognizer

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.objectrecognizer.GraphicOverlay.GraphicOverlay
import com.example.objectrecognizer.data.BoxWithText
import com.example.objectrecognizer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1001
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var currentPhotoPath: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }


    override fun onResume() {
        super.onResume()

        binding.btnTakePic.setOnClickListener {
            pictureIntent()
        }

        binding.imgSampleOne.setOnClickListener {
            setViewAndDetect(getSampleImage(R.drawable.demo_img1))
        }
        binding.imgSampleTwo.setOnClickListener {
            setViewAndDetect(getSampleImage(R.drawable.demo_img2))
        }
        binding.imgSampleThree.setOnClickListener {
            setViewAndDetect(getSampleImage(R.drawable.demo_img3))
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE &&
            resultCode == Activity.RESULT_OK
        ) {
            setViewAndDetect(getCapturedImage())
        }
    }

    //set big image view to frame
    private fun getCapturedImage(): Bitmap {
        val width = binding.bigImgView.width
        val height = binding.bigImgView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW = outWidth
            val photoH = outHeight

            // Determine how much to scale down the image
            val scaleFactor = max(1, min(photoW / width, photoH / height))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inMutable = true
        }

        //calculate orientation
        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)


        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }


    private fun setViewAndDetect(bitmap: Bitmap) {

        binding.bigImgView.setImageBitmap(bitmap)
        binding.tvDescription.visibility = View.INVISIBLE

        runObjectDetection(bitmap)
    }


    private fun runObjectDetection(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

        val objectDetector = ObjectDetection.getClient(options)

        objectDetector.process(image).addOnSuccessListener { detectedObjects ->

            var boxes = mutableListOf<BoxWithText>()
            var category = "Unknown"

            for (obj in detectedObjects) {

                if (obj.labels.isNotEmpty()) {
                    val label = obj.labels.first()
                    category = "${label.text}, ${label.confidence.times(100).toInt()}%"
                }

                boxes.add(BoxWithText(obj.boundingBox, category))

            }
            val graphicOverlay = GraphicOverlay()
            val objectsDrawnBitmap = graphicOverlay.drawDetectionResults(bitmap, boxes)

            runOnUiThread {
                binding.bigImgView.setImageBitmap(objectsDrawnBitmap)
            }
        }.addOnFailureListener {
            Snackbar.make(binding.root, "Detection failed", Snackbar.LENGTH_LONG).show()
        }

    }

    private fun getSampleImage(drawable: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, drawable, BitmapFactory.Options().apply {
            inMutable = true
        })
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun pictureIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = try {
            createImageFile()
        } catch (e: IOException) {
            Snackbar.make(binding.root, "Failed to make image!", Snackbar.LENGTH_LONG).show()
            null
        }

        if (photoFile != null) {
            val photoURI =
                FileProvider.getUriForFile(this, "com.example.objectrecognizer", photoFile)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }

        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    


}







