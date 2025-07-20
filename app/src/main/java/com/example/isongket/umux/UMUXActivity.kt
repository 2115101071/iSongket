package com.example.isongket.umux

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.isongket.R

class UMUXActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_umux)

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()

            // Simpan rating ke SharedPreferences (atau bisa ke file lokal)
            val sharedPref = getSharedPreferences("umux_feedback", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt("user_rating", rating)
                apply()
            }

            Toast.makeText(this, "Terima kasih atas penilaian Anda!", Toast.LENGTH_SHORT).show()

            // Menutup aplikasi
            finishAffinity()
        }
    }
}