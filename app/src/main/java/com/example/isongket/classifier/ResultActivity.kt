package com.example.isongket.classifier

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.isongket.R
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class ResultActivity : AppCompatActivity() {

    private lateinit var imgResult: ImageView
    private lateinit var tvTopResult: TextView
    private lateinit var layoutOtherProbs: LinearLayout
    private lateinit var btnRetry: Button
    private lateinit var btnExit: Button
    private lateinit var labels: List<String>
    private lateinit var currentPhotoPath: String

    private val REQUEST_CAMERA = 200
    private val REQUEST_GALLERY = 201
    private val REQUEST_CAMERA_PERMISSION = 202

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        Log.d("WhiteboxTest", "ResultActivity started")

        imgResult = findViewById(R.id.imgResult)
        tvTopResult = findViewById(R.id.tvTopResult)
        layoutOtherProbs = findViewById(R.id.layoutOtherProbs)
        btnRetry = findViewById(R.id.btnRetry)
        btnExit = findViewById(R.id.btnExit)

        labels = "labels.txt".loadLabels()
        Log.d("WhiteboxTest", "Label list loaded: ${labels.joinToString()}")

        // --- Ambil data dari Intent ---
        val topLabel = intent.getStringExtra("label") ?: "Unknown"
        val confidence = intent.getFloatExtra("confidence", 0f)
        val probs = intent.getFloatArrayExtra("all_probs") ?: floatArrayOf()

        Log.d("WhiteboxTest", "Top label: $topLabel, Confidence: $confidence")
        Log.d("WhiteboxTest", "All probabilities: ${probs.joinToString()}")

        val imagePath = intent.getStringExtra("imagePath")
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imgResult.setImageBitmap(bitmap)
            Log.d("WhiteboxTest", "Image loaded from file: $imagePath")
        } else {
            imgResult.setImageResource(R.drawable.ic_no_image)
            Log.e("WhiteboxTest", "No imagePath found in intent!")
        }

        tvTopResult.text = "Motif: $topLabel\nAkurasi: ${(confidence * 100).toInt()}%"
        Log.d("WhiteboxTest", "Top result displayed on UI")

        layoutOtherProbs.removeAllViews()
        for (i in probs.indices) {
            if (labels[i] != topLabel) {
                val textView = TextView(this)
                textView.text = "${labels[i]}: ${(probs[i] * 100).toInt()}%"
                layoutOtherProbs.addView(textView)
                Log.d("WhiteboxTest", "Added alternative prediction: ${labels[i]} - ${probs[i]}")
            }
        }

        btnRetry.setOnClickListener {
            showRetryDialog()
            Log.d("WhiteboxTest", "Retry button clicked")
        }

        btnExit.setOnClickListener {
            finishAffinity()
            Log.d("WhiteboxTest", "Exit button clicked, closing app")
        }
    }

    private fun showRetryDialog() {
        val options = arrayOf("Kamera", "Galeri")
        AlertDialog.Builder(this)
            .setTitle("Pilih Metode")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }

    @SuppressLint("IntentReset")
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir!!).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            Log.d(
                "WhiteboxTest",
                "Result not OK. requestCode: $requestCode, resultCode: $resultCode"
            )
            return
        }

        when (requestCode) {
            REQUEST_CAMERA -> {
                Log.d(
                    "WhiteboxTest",
                    "Camera image received, decoding from path: $currentPhotoPath"
                )
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                if (bitmap != null) {
                    Log.d("WhiteboxTest", "Bitmap created from camera capture")
                    classifyAndReload(bitmap)
                } else {
                    Log.e("WhiteboxTest", "Failed to decode bitmap from camera")
                }
            }

            REQUEST_GALLERY -> {
                val uri = data?.data ?: run {
                    Log.e("WhiteboxTest", "No data returned from gallery")
                    return
                }
                Log.d("WhiteboxTest", "Gallery image selected: $uri")
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    Log.d("WhiteboxTest", "Bitmap created from gallery image")
                    classifyAndReload(bitmap)
                } else {
                    Log.e("WhiteboxTest", "Failed to decode bitmap from gallery")
                }
            }

            else -> {
                Log.d("WhiteboxTest", "Unknown requestCode: $requestCode")
            }
        }
    }

    private fun classifyAndReload(bitmap: Bitmap) {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val input = convertBitmapToByteBuffer(resized)
        val output = Array(1) { FloatArray(labels.size) }

        val interpreter = Interpreter("fixberhasilcuy.tflite".loadModelFile())
        interpreter.run(input, output)

        val probs = output[0]
        val maxIndex = probs.indices.maxByOrNull { probs[it] } ?: -1
        val resultLabel = labels[maxIndex]
        val confidence = probs[maxIndex]

        // ✅ Simpan bitmap ke file
        val tempFile = File(cacheDir, "classified_result_image.png")
        val outStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream)
        outStream.close()

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("label", resultLabel)
            putExtra("confidence", confidence)
            putExtra("all_probs", probs)
            putExtra("imagePath", tempFile.absolutePath) // ✅ Kirim path gambar
        }

        startActivity(intent)
        finish()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        Log.d("WhiteboxTest", "convertBitmapToByteBuffer() called")

        val inputSize = 224
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        Log.d("WhiteboxTest", "Bitmap pixel values extracted")

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        Log.d("WhiteboxTest", "ByteBuffer successfully filled with normalized RGB values")
        return byteBuffer
    }


    private fun String.loadModelFile(): MappedByteBuffer {
        Log.d("WhiteboxTest", "loadModelFile() called with filename: $this")

        return try {
            val fileDescriptor = assets.openFd(this)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val mappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
            Log.d("WhiteboxTest", "Model file loaded successfully: $this")
            mappedByteBuffer
        } catch (e: Exception) {
            Log.e("WhiteboxTest", "Error loading model file: ${e.message}")
            throw RuntimeException("Model loading failed", e)
        }
    }

    private fun String.loadLabels(): List<String> {
        return assets.open(this).bufferedReader().useLines { it.toList() }
    }
}