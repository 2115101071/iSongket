package com.example.isongket.loading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.isongket.R
import com.example.isongket.classifier.ResultActivity

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        Log.d("WhiteboxTest", "LoadingActivity started. Showing loading animation...")

        Handler(Looper.getMainLooper()).postDelayed({
            val resultIntent = Intent(this, ResultActivity::class.java)

            val extras = intent.extras
            if (extras != null) {
                resultIntent.putExtras(extras)
                Log.d("WhiteboxTest", "Extras received and passed to ResultActivity")
            } else {
                Log.e("WhiteboxTest", "No extras found in intent!")
            }

            Log.d("WhiteboxTest", "Navigating to ResultActivity")
            startActivity(resultIntent)
            finish()
        }, 2000) // delay 2 detik
    }
}

