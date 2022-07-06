package com.dmc.demosaveimagebase64

import android.Manifest
import android.R.attr.rotation
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val handleException =
        CoroutineExceptionHandler { coroutineContext, throwable -> throwable.printStackTrace() }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageURI = result.data?.data
            resolve(imageURI)
        }
    }

    private var requestFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        } else {
            //deny
            finish()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
        } else {
            // Explain to the user that the feature is unavailable because the
            // features requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionRequest()
        findViewById<Button>(R.id.button).setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            resultLauncher.launch(gallery)
        }
    }

    private fun resolve(imageURI: Uri?) = CoroutineScope(Dispatchers.IO + handleException).launch {
        val encodedString = Base64.encodeToString(readBytesFromFile(imageURI), Base64.NO_WRAP)
        val bytesArray = Base64.decode(encodedString, Base64.NO_WRAP)
//        val bytesArray = BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.size)

        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
        val myDir = File(root)
        myDir.mkdirs()

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")
        val formatted = current.format(formatter)

        val fname = "base64_from_image_${formatted}.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        Log.i("LOAD", root + fname)
        file.writeBytes(bytesArray)
//        file.writeBitmap(bitmapByteArray, Bitmap.CompressFormat.JPEG, 100)

        withContext(Dispatchers.Main) {
            val bitmap = getBitmapFromUri(imageURI)
            findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
            Toast.makeText(this@MainActivity, "Save image success", Toast.LENGTH_SHORT).show()
        }
    }

    private fun permissionRequest() {
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            } else {
//                val enableBtIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")).apply {
//                    addCategory(Intent.CATEGORY_DEFAULT)
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                }
//                requestFile.launch(enableBtIntent)
//            }
//        }
    }

    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }

    private fun encodeToBase64(image: Bitmap): String {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.NO_WRAP)
    }

    private fun decodeBase64(input: String): Bitmap? {
        val decodedByte = Base64.decode(input, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
    }

    private fun exifToDegrees(exifOrientation: Int): Int = when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private fun getBitmapFromUri(uri: Uri?): Bitmap? {
        val exifInterface = ExifInterface(contentResolver.openInputStream(uri!!)!!)
        val orientation: Int = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotationInDegrees = exifToDegrees(orientation)
        val matrix = Matrix()
        if (rotation != 0) {
            matrix.preRotate(rotationInDegrees.toFloat())
        }
        var exif = ""
        exif += "\nTAG_IMAGE_WIDTH: ${exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)}"
        exif += "\nTAG_IMAGE_LENGTH: ${exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)}"
        exif += "\nTAG_GPS_LATITUDE : ${exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)}"
        exif += "\nTAG_GPS_LONGITUDE: ${exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)}"
        Log.d("chinhdm", exif)

        val bitmap = contentResolver.openFileDescriptor(uri, "r").use {
            val fileDescriptor = it!!.fileDescriptor
            val image: Bitmap? = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            image
        }
//        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
//        val bitmap = BitmapFactory.decodeStream(assets.open("a1.jpg"))
        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun readBytesFromFile(uri: Uri?): ByteArray? {
        return contentResolver.openInputStream(uri!!)?.buffered().use { it?.readBytes() }
    }

    private fun readUri(context: Context, uri: Uri?): ByteArray {
        val pdf = context.contentResolver.openFileDescriptor(uri!!, "r")!!
        assert(pdf.statSize <= Int.MAX_VALUE)
        val data = ByteArray(pdf.statSize.toInt())
        val fd = pdf.fileDescriptor
        val fileStream = FileInputStream(fd)
        fileStream.read(data)
        return data
    }
}