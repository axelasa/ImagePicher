package com.example.imagepicher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {


    private val FILE_NAME ="photo.jpg"
    val MyPREFERENCES = "MyPrefs"
    var sharedpreferences: SharedPreferences? = null

    lateinit var camera: Button
    private lateinit var storage: Button

    private lateinit var photoFile: File

    var member:String = ""

    private val REQUEST_CODE = 42

    private lateinit var cPhoto: Bitmap

    var cPhotoRotated: Bitmap?=null

    private lateinit var selfi : ShapeableImageView
    companion object
    {
        private val SELECT_IMAGE_CODE=100

        // private val CAMERA_REQUEST = 123
    }
    //face detection using Ml kit here

    //var detector: FirebaseVisionFaceDetector? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE)
        member = sharedpreferences?.getString("member", "null").toString()
        Log.e("####member", member)


        // initializeWidgets()

        selfi = findViewById(R.id.passport)


        val encoder = findViewById<FloatingActionButton>(R.id.done)

        encoder.setOnClickListener {


            val editor = sharedpreferences!!.edit()
            editor.putInt("status", 1)     //im saving the image in shared prefrences then display the same image in homepage
            editor.apply()

//            val intent = Intent(this@Profile, HomepageActivity::class.java)
//            startActivity(intent)

        }


        camera = findViewById(R.id.capture)


        camera.setOnClickListener {

            capturePhoto()
            //no camera permitions? how add them well it's not the permissions that are bringing this issue... ni URI add them just in case
        }



        storage = findViewById(R.id.gallery)

        storage.setOnClickListener {
            //check permissions at runtime
            openGallery()
        }

    }



    // Real-time contour detection
//    val realTimeOpts = FaceDetectorOptions.Builder()
//        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
//        .build()



    private fun capturePhoto(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = photoFile(FILE_NAME)

        val fileProvider = FileProvider.getUriForFile(
            this@MainActivity,
            "com.example.imagepicher.fileprovider",
            photoFile
        )
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
        if (takePictureIntent.resolveActivity(this.packageManager)!=null) {
            startActivityForResult(takePictureIntent, REQUEST_CODE)
            Log.i("### camera intent", REQUEST_CODE.toString())

        }else{

            Toast.makeText(this@MainActivity, "Unable to Open Camera", Toast.LENGTH_SHORT).show()

        }
    }

    private fun photoFile(fileName: String): File {

        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpeg", storageDirectory)

    }


    private fun openGallery(){

        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"

        intent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(Intent.createChooser(intent, "Select Picture..."), SELECT_IMAGE_CODE)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        //var cPhotoRotated: Bitmap? = null

        // Gallery Image
        if(requestCode == SELECT_IMAGE_CODE && resultCode == Activity.RESULT_OK){

            if(data!= null)
            {
                try {
                    cPhoto = MediaStore.Images.Media.getBitmap(
                        application.contentResolver,
                        data.data
                    )

//                    if (cPhoto != null)
//                        cPhotoRotated = rotateImageIfRequired(cPhoto!!)    check whatsappp, seen what is the data type of an image in kotlin?? its processed in bitmap

                    //  Set photo to imageView
                    selfi.setImageBitmap(cPhoto) // here: this is when image is picked from the gallarey

//                    val editor = sharedpreferences!!.edit()
//                    editor.putString("image" , cPhoto.toString())     //im saving the image in shared prefrences then display the same image in homepage
//                    editor.apply()

                }
                catch (exp: IOException){
                    exp.printStackTrace()
                }

            } else if(resultCode == Activity.RESULT_CANCELED)
            {
                Toast.makeText(applicationContext, "Cancelled", Toast.LENGTH_LONG).show()
            }

            // Camera Image
        } else if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            cPhoto = BitmapFactory.decodeFile(photoFile.absolutePath)

            Timber.e("CHECk")

            if (cPhoto!=null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                cPhotoRotated = imageRotation(cPhoto!!)
                selfi.setImageBitmap(cPhotoRotated) // here too : so this is for the camera

                val base64ImageString = cPhotoRotated?.let { it1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    encode(it1)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                }
                Log.e("####", "Base64ImageString = $base64ImageString")

            }

            // High-accuracy landmark detection and face classification
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                //.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()

            val image = InputImage.fromBitmap(cPhoto,0)

            val detector = FaceDetection.getClient()
            detector.process(image)
                .addOnSuccessListener { faces->
                    Toast.makeText(this, "successful", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e->
                    Toast.makeText(this, "PLEASE TRY AGAIN!", Toast.LENGTH_LONG).show()
                }

// Or, to use the default option:
// val detector = FaceDetection.getClient();


            // set photo to imageView

        } else{
            super.onActivityResult(requestCode, resultCode, data)
        }


    }
    // encoding image to base 64
    @RequiresApi(Build.VERSION_CODES.O)
    private fun encode(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()

        val reuslt : String = Base64.encodeToString(b, Base64.NO_WRAP)

        val editor = sharedpreferences!!.edit()
        editor.putString("image", reuslt)     //im saving the image in shared prefrences then display the same image in homepage
        editor.apply()

        // post details to the back end
        // HERE

        var imageObject = JSONObject ()
        imageObject.put("memberPic", cPhotoRotated)
        imageObject.put("memberNo", member)


//        fuel library used to post to the backend

//        try {
//            Fuel.put(
//                pictureURL
//            ).jsonBody(imageObject.toString()).responseJson { request , response , result ->
//                Log.d("####response: " , result.get().content)
//
//            }
//        } catch (e: Exception) {
//
//        } finally {
//
//            return reuslt
//        }
//         //PROGRESS
        return reuslt
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun imageRotation(bitmap: Bitmap): Bitmap? {
        val ei = ExifInterface(photoFile)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        var rotatedBitmap: Bitmap? = null
        rotatedBitmap =
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                ExifInterface.ORIENTATION_NORMAL -> bitmap
                else -> bitmap
            }
        return rotatedBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }



}
