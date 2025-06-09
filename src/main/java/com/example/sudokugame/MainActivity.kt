package com.example.sudokugame

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : ComponentActivity() {
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Ініціалізація UI елементів
        val newGameButton: Button = findViewById(R.id.newGameButton)
        continueButton = findViewById(R.id.continueButton)
        val levelButton: Button = findViewById(R.id.levelButton)
        val settingsButton: Button = findViewById(R.id.settingsButton)
        val exitButton: Button = findViewById(R.id.exitButton)

        newGameButton.setOnClickListener {
            val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
            val difficulty = sharedPref.getString("difficulty", "Medium") ?: "Medium"
            startNewGame(difficulty)
        }

        continueButton.setOnClickListener {
            val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
            val hasSavedGame = sharedPref.getBoolean("hasSavedGame", false)
            if (hasSavedGame) {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("loadSavedGame", true)
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.no_saved_game, Toast.LENGTH_LONG).show()
            }
        }

        levelButton.setOnClickListener {
            showDifficultyDialog()
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        exitButton.setOnClickListener {
            finish()
        }
        // Перевірка, чи є збережена гра
        updateContinueButtonState()

        // Додаємо анімацію до кнопок
        setupButtonAnimation(findViewById(R.id.newGameButton))
        setupButtonAnimation(findViewById(R.id.continueButton))
        setupButtonAnimation(findViewById(R.id.levelButton))
        setupButtonAnimation(findViewById(R.id.settingsButton))
        setupButtonAnimation(findViewById(R.id.exitButton))
    }

    override fun onResume() {
        super.onResume()
        // Оновлюємо стан кнопки "Продовжити гру" при поверненні до активності
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        val hasSavedGame = sharedPref.getBoolean("hasSavedGame", false)
        continueButton.isEnabled = hasSavedGame
    }

    private fun showDifficultyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_difficulty, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.easyButton).setOnClickListener {
            saveDifficulty("Easy")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.mediumButton).setOnClickListener {
            saveDifficulty("Medium")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.hardButton).setOnClickListener {
            saveDifficulty("Hard")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.veryHardButton).setOnClickListener {
            saveDifficulty("VeryHard")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.sixBySixButton).setOnClickListener {
            saveDifficulty("SixBySix")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveDifficulty(difficulty: String) {
        val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("difficulty", difficulty)
            putBoolean("hasSavedGame", false)
            apply()
        }
        Toast.makeText(this, String.format(getString(R.string.level_set), difficulty), Toast.LENGTH_SHORT).show()
    }

    private fun showNewGameConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_game_confirmation, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.yesButton).setOnClickListener {
            val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("hasSavedGame", false)
                apply()
            }
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("loadSavedGame", false)
            startActivity(intent)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.noButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Функція для налаштування анімації кнопки
    private fun setupButtonAnimation(button: Button) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    val anim = AnimationUtils.loadAnimation(this, R.anim.button_scale)
                    v.startAnimation(anim)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.clearAnimation()
                }
            }
            false
        }
    }

    private fun startNewGame(difficulty: String) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("DIFFICULTY", difficulty)
            putExtra("GRID_SIZE", if (difficulty == "SixBySix") 6 else 9)
            putExtra("loadSavedGame", false)
        }
        startActivity(intent)
    }
} 