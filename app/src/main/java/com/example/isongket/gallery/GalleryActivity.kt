package com.example.isongket.gallery

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.isongket.R
import com.example.isongket.loading.LoadingActivity
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@Suppress("DEPRECATION")
class GalleryActivity : AppCompatActivity() {
    private val SELECT_IMAGE_REQUEST = 100
    private lateinit var imageView: ImageView
    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        imageView = findViewById(R.id.previewImage)
        val button: Button = findViewById(R.id.btnSelectImage)

        // Load model dan label
        interpreter = Interpreter("fixberhasilcuy.tflite".loadModelFile())
        labels = "labels.txt".loadLabels()

        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_IMAGE_REQUEST)
        }
    }

    private val REQUEST_CAMERA = 100
    private val REQUEST_GALLERY = 101
    private lateinit var currentPhotoPath: String  // pastikan nilai ini di-set saat ambil foto

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
                    classifyImage(bitmap)
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
                    classifyImage(bitmap)
                } else {
                    Log.e("WhiteboxTest", "Failed to decode bitmap from gallery")
                }
            }

            else -> {
                Log.d("WhiteboxTest", "Unknown requestCode: $requestCode")
            }
        }
    }


    private fun classifyImage(bitmap: Bitmap) {
        Log.d("WhiteboxTest", "classifyImage() called with image")

        // Resize gambar ke ukuran input model
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val input = convertBitmapToByteBuffer(resized)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)

        val probs = output[0]
        val maxIndex = probs.indices.maxByOrNull { probs[it] } ?: -1
        val resultLabel = labels[maxIndex]
        val confidence = probs[maxIndex]

        Log.d("WhiteboxTest", "Prediction result: $resultLabel with confidence $confidence")

        // Simpan gambar ke file sementara
        val tempFile = File(cacheDir, "classified_image.png")
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        outputStream.close()
        Log.d("WhiteboxTest", "Image saved to cache: ${tempFile.absolutePath}")

        // Kirim ke LoadingActivity
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("label", resultLabel)
            putExtra("confidence", confidence)
            putExtra("all_probs", probs)
            putExtra("imagePath", tempFile.absolutePath)
        }

        Log.d("WhiteboxTest", "Sending intent to LoadingActivity")
        startActivity(intent)
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

}
