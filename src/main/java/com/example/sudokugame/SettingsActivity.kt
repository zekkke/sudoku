package com.example.sudokugame

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Коментую код, пов'язаний із themeSwitch і backButton, щоб уникнути помилок компіляції
        // val themeSwitch: SwitchCompat = findViewById(R.id.themeSwitch)
        // val backButton: Button = findViewById(R.id.backButton)

        // Завантаження збереженого стану темного режиму
        val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        // val isDarkMode = sharedPref.getBoolean("darkMode", false)
        // themeSwitch.isChecked = isDarkMode

        // Обробник для перемикача темного режиму
        // themeSwitch.setOnCheckedChangeListener { _, isChecked ->
        //     with(sharedPref.edit()) {
        //         putBoolean("darkMode", isChecked)
        //         apply()
        //     }
        //     // Тут можна додати логіку для зміни теми програми
        // }

        // Обробник для кнопки повернення
        // backButton.setOnClickListener {
        //     finish()
        // }
    }
} 