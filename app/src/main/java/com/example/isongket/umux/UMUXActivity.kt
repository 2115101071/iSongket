package com.example.isongket.umux

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.isongket.R

class UMUXActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_umux)

        val radioGroup1 = findViewById<RadioGroup>(R.id.radioGroup1)
        val radioGroup2 = findViewById<RadioGroup>(R.id.radioGroup2)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val selected1 = radioGroup1.checkedRadioButtonId
            val selected2 = radioGroup2.checkedRadioButtonId

            if (selected1 == -1 || selected2 == -1) {
                Toast.makeText(this, "Silakan isi semua pertanyaan.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nilai1 = findViewById<RadioButton>(selected1).text.toString().toInt()
            val nilai2 = findViewById<RadioButton>(selected2).text.toString().toInt()

            // Simpan nilai ke SharedPreferences
            val sharedPref = getSharedPreferences("umux_feedback", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt("umux_q1", nilai1)
                putInt("umux_q2", nilai2)
                apply()
            }

            Toast.makeText(this, "Terima kasih atas penilaian Anda!", Toast.LENGTH_SHORT).show()

            // Tutup aplikasi
            finishAffinity()
        }
    }
}