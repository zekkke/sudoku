package com.example.sudokugame

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min

class GameActivity : ComponentActivity() {
    private lateinit var sudokuContainer: GridLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var hintButton: Button
    private lateinit var checkButton: Button
    private lateinit var menuButton: Button
    private lateinit var notesButton: Button
    private lateinit var settingsButton: Button
    private var numberButtons: MutableList<Button> = mutableListOf()
    private var gameBoard: Array<IntArray> = Array(9) { IntArray(9) }
    private var initialBoard: Array<IntArray> = Array(9) { IntArray(9) }
    private var notes: Array<Array<MutableSet<Int>>> = Array(9) { Array(9) { mutableSetOf() } }
    private var selectedRow: Int = -1
    private var selectedCol: Int = -1
    private var selectedNumber: Int = -1
    private var isGameInitialized: Boolean = false
    private var isGameCompleted: Boolean = false
    private var difficulty: String = "Medium"
    private var hintsUsed: Int = 0
    private var maxHints: Int = 5
    private var isNotesMode: Boolean = false
    private var isPaused: Boolean = false
    private var gridSize: Int = 9
    private var isLightningMode = false
    private var selectedNumberForLightning: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ініціалізація UI елементів на самому початку
        sudokuContainer = findViewById(R.id.sudokuGrid)
        progressBar = findViewById(R.id.progressBar)
        hintButton = findViewById(R.id.hintButton)
        checkButton = findViewById(R.id.checkButton)
        menuButton = findViewById(R.id.menuButton)
        notesButton = findViewById(R.id.notesButton)
        settingsButton = findViewById(R.id.settingsButton)

        setupNumberButtons()

        // Встановлення обробників подій
        hintButton.setOnClickListener { provideHint() }
        checkButton.setOnClickListener { checkSolution() }
        menuButton.setOnClickListener { finish() }
        notesButton.setOnClickListener { toggleNotesMode() }
        settingsButton.setOnClickListener { openSettings() }

        // Отримання рівня складності
        val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        difficulty = sharedPref.getString("difficulty", "Medium") ?: "Medium"
        setGridSize()
        adjustNumberButtonsVisibility()

        // Перевірка, чи потрібно завантажити збережену гру
        val loadSavedGame = intent.getBooleanExtra("loadSavedGame", false)
        val hasSavedGame = sharedPref.getBoolean("hasSavedGame", false)

        android.util.Log.d("SudokuGame", "loadSavedGame: $loadSavedGame, hasSavedGame: $hasSavedGame")

        if (!loadSavedGame && hasSavedGame) {
            android.widget.Toast.makeText(this, R.string.saved_game_warning, android.widget.Toast.LENGTH_LONG).show()
        }
        if (loadSavedGame) {
            if (hasSavedGame) {
                android.util.Log.d("SudokuGame", "Loading saved game")
                loadSavedGame()
                android.widget.Toast.makeText(this, R.string.game_loaded, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.util.Log.d("SudokuGame", "No saved game to load")
                android.widget.Toast.makeText(this, R.string.no_saved_game, android.widget.Toast.LENGTH_LONG).show()
                initializeGame()
            }
        } else {
            android.util.Log.d("SudokuGame", "Initializing new game")
            initializeGame()
        }

        setupLightningMode()
        setupLightningSwitch()
    }

    private fun setGridSize() {
        gridSize = if (difficulty == "SixBySix") 6 else 9
        gameBoard = Array(gridSize) { IntArray(gridSize) }
        initialBoard = Array(gridSize) { IntArray(gridSize) }
        notes = Array(gridSize) { Array(gridSize) { mutableSetOf() } }
    }

    private fun adjustNumberButtonsVisibility() {
        val maxNumber = if (difficulty == "SixBySix") 6 else 9
        for (i in numberButtons.indices) {
            numberButtons[i].visibility = if (i < maxNumber) View.VISIBLE else View.GONE
        }
    }

    private fun initializeGame() {
        progressBar.visibility = View.VISIBLE
        disableAllButtons()
        android.util.Log.d("SudokuGame", "Initializing new game started")

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val emptyCells = when (difficulty) {
                    "Easy" -> if (gridSize == 9) 30 else 10
                    "Medium" -> if (gridSize == 9) 40 else 15
                    "Hard" -> if (gridSize == 9) 50 else 20
                    "VeryHard" -> if (gridSize == 9) 60 else 25
                    "SixBySix" -> 6 // Зменшуємо кількість порожніх клітинок для 6x6
                    else -> if (gridSize == 9) 40 else 15
                }
                val generator = SudokuGenerator()
                // Додаємо тайм-аут 5 секунд для генерації головоломки (зменшено для швидшого виявлення проблем)
                android.util.Log.d("SudokuGame", "Starting puzzle generation with gridSize=$gridSize, emptyCells=$emptyCells")
                val generatedBoard = withTimeoutOrNull(5000) {
                    generator.generatePuzzle(emptyCells, gridSize)
                }
                gameBoard = generatedBoard ?: generator.getFallbackPuzzle(emptyCells, gridSize)
                android.util.Log.d("SudokuGame", "Puzzle generation completed, board created")
                initialBoard = gameBoard.map { it.copyOf() }.toTypedArray()
                notes = Array(gridSize) { Array(gridSize) { mutableSetOf() } }
                selectedRow = -1
                selectedCol = -1

                withContext(Dispatchers.Main) {
                    updateGridUI()
                    progressBar.visibility = View.GONE
                    isGameInitialized = true
                    hintsUsed = 0 // Скидаємо кількість використаних підказок для нової гри
                    saveHintsUsed()
                    enableAllButtons()
                    android.util.Log.d("SudokuGame", "Initializing new game completed, UI updated")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("SudokuGame", "Error during game initialization: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    android.widget.Toast.makeText(this@GameActivity, R.string.game_initialization_error, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateGridUI() {
        sudokuContainer.removeAllViews()

        sudokuContainer.post {
            val totalSize = min(sudokuContainer.width, sudokuContainer.height)
            if (totalSize <= 0) {
                android.util.Log.e("SudokuGame", "Container size is 0, cannot update grid UI")
                return@post
            }
            val cellSize = totalSize / (if (gridSize == 9) 11f else 7f) // Для 9x9: 11, для 6x6: 7 (6 клітинок + 1 роздільник)

            // Встановлюємо кількість рядків і стовпців, враховуючи проміжки для ліній
            sudokuContainer.columnCount = if (gridSize == 9) 11 else 5 // Для 6x6: 4 клітинки по горизонталі + 1 роздільник
            sudokuContainer.rowCount = if (gridSize == 9) 11 else 7 // Для 6x6: 6 клітинок по вертикалі + 1 роздільник

            // Додаємо клітинки
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    // Пропускаємо клітинки, які не входять у видиму область 4x6
                    if (gridSize == 6 && (j >= 4)) continue

                    val cellView = TextView(this).apply {
                        val lp = GridLayout.LayoutParams().apply {
                            width = 0
                            height = 0
                            // Враховуємо зміщення через проміжки для ліній
                            var rowIndex = i + (if (gridSize == 9 && i >= 3) 1 else 0) + (if (gridSize == 9 && i >= 6) 1 else 0)
                            var colIndex = j + (if (gridSize == 9 && j >= 3) 1 else 0) + (if (gridSize == 9 && j >= 6) 1 else 0)
                            if (gridSize == 6) {
                                rowIndex = i + (if (i >= 3) 1 else 0)
                                colIndex = j + (if (j >= 2) 1 else 0) // Для 6x6 роздільник після 2 клітинок по горизонталі
                            }
                            rowSpec = GridLayout.spec(rowIndex, 1f)
                            columnSpec = GridLayout.spec(colIndex, 1f)
                            setMargins(0, 0, 0, 0)
                        }
                        layoutParams = lp

                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, 0)
                        includeFontPadding = false

                        // Встановлення фону
                        if (selectedRow == i && selectedCol == j) {
                            setBackgroundResource(R.drawable.selected_cell)
                        } else if (selectedNumber != -1 && gameBoard[i][j] == selectedNumber) {
                            setBackgroundResource(R.drawable.highlighted_cell)
                        } else if (selectedRow != -1 && selectedCol != -1 &&
                            (i == selectedRow || j == selectedCol || (i / (gridSize/3) == selectedRow / (gridSize/3) && j / (gridSize/3) == selectedCol / (gridSize/3)))) {
                            setBackgroundResource(R.drawable.highlighted_cell)
                        } else {
                            setBackgroundResource(R.drawable.cell_background)
                        }

                        val value = gameBoard[i][j]
                        if (value != 0) {
                            text = value.toString()
                            setTextColor(Color.WHITE)
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, cellSize * 0.6f)
                        } else if (notes[i][j].isNotEmpty()) {
                            text = buildNotesText(notes[i][j])
                            setTextColor(Color.LTGRAY)
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, cellSize * 0.25f)
                            setLineSpacing(0f, 0.8f)
                        } else {
                            text = ""
                        }
                        setOnClickListener { onCellClick(i, j) }
                    }
                    sudokuContainer.addView(cellView)
                }
            }

            // Додаємо лінії між блоками
            addBlockSeparators()
        }
    }

    private fun addBlockSeparators() {
        val lineThickness = 2.dpToPx()
        val lineColor = Color.RED
        val blockSize = if (gridSize == 9) 3 else 2 // Для 6x6 роздільник після 2 клітинок по горизонталі
        val separatorPositionsHorizontal = if (gridSize == 9) listOf(3, 7) else listOf(2)
        val separatorPositionsVertical = if (gridSize == 9) listOf(3, 7) else listOf(3)

        // Горизонтальні лінії між блоками
        for (row in separatorPositionsVertical) {
            val lineView = View(this).apply {
                setBackgroundColor(lineColor)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.MATCH_PARENT
                    height = lineThickness
                    rowSpec = GridLayout.spec(row, 0f) // Вага 0, щоб лінія займала лише свою товщину
                    columnSpec = GridLayout.spec(0, if (gridSize == 9) 11 else 5) // Від 0 до кінця (для 6x6: 5 стовпців)
                    setMargins(0, 0, 0, 0)
                }
            }
            sudokuContainer.addView(lineView)
        }

        // Вертикальні лінії між блоками
        for (col in separatorPositionsHorizontal) {
            val lineView = View(this).apply {
                setBackgroundColor(lineColor)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = lineThickness
                    height = GridLayout.LayoutParams.MATCH_PARENT
                    rowSpec = GridLayout.spec(0, if (gridSize == 9) 11 else 7) // Від 0 до кінця
                    columnSpec = GridLayout.spec(col, 0f) // Вага 0, щоб лінія займала лише свою товщину
                    setMargins(0, 0, 0, 0)
                }
            }
            sudokuContainer.addView(lineView)
        }
    }

    private fun buildNotesText(notesSet: Set<Int>): String {
        val notesGrid = Array(3) { Array(3) { " " } }
        for (num in notesSet) {
            val index = num - 1
            val row = index / 3
            val col = index % 3
            notesGrid[row][col] = num.toString()
        }
        return notesGrid.joinToString("\n") { row -> row.joinToString(" ") }
    }

    private fun onCellClick(row: Int, col: Int) {
        if (!isGameInitialized) return
        if (isLightningMode) {
            if (gameBoard[row][col] == 0 && selectedNumberForLightning != -1) {
                gameBoard[row][col] = selectedNumberForLightning
                updateGridUI()
                checkWinCondition()
            } else if (gameBoard[row][col] != 0) {
                Toast.makeText(this, "Ця клітинка вже заповнена!", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (initialBoard[row][col] == 0) {
                selectedRow = row
                selectedCol = col
            } else {
                selectedRow = -1
                selectedCol = -1
            }
            selectedNumber = if (gameBoard[row][col] != 0) gameBoard[row][col] else -1
            updateGridUI()
        }
    }

    private fun onNumberClick(number: Int) {
        if (!isGameInitialized) return
        if (isLightningMode) {
            selectedNumberForLightning = number
            updateNumberButtonsUI()
        } else {
            if (selectedRow == -1 || selectedCol == -1) {
                Toast.makeText(this, R.string.select_cell_first, Toast.LENGTH_SHORT).show()
                selectedNumber = number
                updateGridUI()
                return
            }
            if (isNotesMode) {
                if (notes[selectedRow][selectedCol].contains(number)) {
                    notes[selectedRow][selectedCol].remove(number)
                } else {
                    notes[selectedRow][selectedCol].add(number)
                }
            } else {
                gameBoard[selectedRow][selectedCol] = number
                notes[selectedRow][selectedCol].clear()
                selectedRow = -1
                selectedCol = -1
                selectedNumber = number
            }
            updateGridUI()
        }
    }

    private fun provideHint() {
        if (!isGameInitialized) return
        if (hintsUsed >= maxHints) {
            android.widget.Toast.makeText(this, String.format(getString(R.string.all_hints_used), hintsUsed, maxHints), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        // Логіка для підказки: заповнення однієї рандомної порожньої клітинки правильним значенням
        val generator = SudokuGenerator()
        val solution = generator.solveSudoku(gameBoard.map { it.copyOf() }.toTypedArray())
        // Перевірка, чи розв'язок коректний
        val isSolutionValid = generator.isValidSolution(solution)
        android.util.Log.d("SudokuGame", "Solution for hint is valid: $isSolutionValid")
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (gameBoard[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        android.util.Log.d("SudokuGame", "Number of empty cells for hint: ${emptyCells.size}")
        if (emptyCells.isNotEmpty()) {
            val randomCell = emptyCells.random()
            val row = randomCell.first
            val col = randomCell.second
            gameBoard[row][col] = solution[row][col]
            android.util.Log.d("SudokuGame", "Hint added: number ${solution[row][col]} to cell ($row, $col)")
            hintsUsed++
            saveHintsUsed()
            // Примусове оновлення UI
            sudokuContainer.invalidate()
            sudokuContainer.requestLayout()
            updateGridUI()
            android.widget.Toast.makeText(this, String.format(getString(R.string.hint_used), hintsUsed, maxHints), android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(this, R.string.no_empty_cells_for_hint, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkSolution() {
        if (!isGameInitialized) return
        val generator = SudokuGenerator()
        val isCorrect = generator.isValidSolution(gameBoard)
        android.util.Log.d("SudokuGame", "Solution check: isCorrect = $isCorrect")
        if (isCorrect) {
            // Гра завершена
            isGameCompleted = true
            // Приховуємо кнопку Check
            checkButton.visibility = View.GONE
            checkButton.invalidate()
            checkButton.requestLayout()
            android.util.Log.d("SudokuGame", "Check button hidden")
            // Показуємо діалогове вікно з повідомленням про перемогу та пропозицією нової гри
            showWinDialog()
            disableAllButtons()
        } else {
            // Додаткова перевірка шляхом порівняння з розв'язком
            val solution = generator.solveSudoku(gameBoard.map { it.copyOf() }.toTypedArray())
            var isManuallyCorrect = true
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    if (gameBoard[i][j] != solution[i][j]) {
                        isManuallyCorrect = false
                        break
                    }
                }
                if (!isManuallyCorrect) break
            }
            android.util.Log.d("SudokuGame", "Manual solution check: isManuallyCorrect = $isManuallyCorrect")
            if (isManuallyCorrect) {
                // Гра завершена за ручною перевіркою
                isGameCompleted = true
                checkButton.visibility = View.GONE
                checkButton.invalidate()
                checkButton.requestLayout()
                android.util.Log.d("SudokuGame", "Check button hidden (manual check)")
                showWinDialog()
                disableAllButtons()
            } else {
                android.widget.Toast.makeText(this, R.string.incorrect_solution, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showWinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_win, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<TextView>(R.id.winMessage).text = getString(R.string.win_message)
        dialogView.findViewById<Button>(R.id.newGameButton).setOnClickListener {
            dialog.dismiss()
            // Скидаємо стан гри
            isGameCompleted = false
            checkButton.visibility = View.VISIBLE
            initializeGame()
        }

        dialog.show()
    }

    private fun disableAllButtons() {
        hintButton.isEnabled = false
        checkButton.isEnabled = false
        for (button in numberButtons) {
            button.isEnabled = false
        }
    }

    private fun enableAllButtons() {
        hintButton.isEnabled = true
        checkButton.isEnabled = true
        for (button in numberButtons) {
            button.isEnabled = true
        }
    }

    private fun saveGame() {
        val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("hasSavedGame", true)
            putInt("gridSize", gridSize)
            // Зберігаємо поточний стан гри
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    putInt("gameBoard_" + i + "_" + j, gameBoard[i][j])
                    putInt("initialBoard_" + i + "_" + j, initialBoard[i][j])
                    putStringSet("notes_" + i + "_" + j, notes[i][j].map { it.toString() }.toSet())
                }
            }
            putInt("hintsUsed", hintsUsed) // Зберігаємо кількість використаних підказок
            apply()
        }
        android.util.Log.d("SudokuGame", "Game saved successfully")
    }

    private fun loadSavedGame() {
        android.util.Log.d("SudokuGame", "Method loadSavedGame called")
        val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        gridSize = sharedPref.getInt("gridSize", 9)
        setGridSize()
        adjustNumberButtonsVisibility()
        gameBoard = Array(gridSize) { i ->
            IntArray(gridSize) { j ->
                sharedPref.getInt("gameBoard_" + i + "_" + j, 0)
            }
        }
        initialBoard = Array(gridSize) { i ->
            IntArray(gridSize) { j ->
                sharedPref.getInt("initialBoard_" + i + "_" + j, 0)
            }
        }
        notes = Array(gridSize) { i ->
            Array(gridSize) { j ->
                sharedPref.getStringSet("notes_" + i + "_" + j, emptySet())?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()
            }
        }
        hintsUsed = sharedPref.getInt("hintsUsed", 0) // Завантажуємо кількість використаних підказок
        android.util.Log.d("SudokuGame", "Game data loaded, updating UI")
        updateGridUI() // Оновлюємо UI напряму
        isGameInitialized = true
        enableAllButtons()
        android.widget.Toast.makeText(this, R.string.game_loaded, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun saveHintsUsed() {
        val sharedPref = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("hintsUsed", hintsUsed)
            apply()
        }
    }

    private fun toggleNotesMode() {
        isNotesMode = !isNotesMode
        notesButton.text = if (isNotesMode) "Notes ON" else "Notes OFF"
        android.widget.Toast.makeText(this, if (isNotesMode) R.string.notes_mode_on else R.string.notes_mode_off, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun setupLightningMode() {
        isLightningMode = intent.getBooleanExtra("LIGHTNING_MODE", false)
        if (isLightningMode) {
            Toast.makeText(this, "Режим Блискавка активовано! Вибирайте число для заповнення.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupLightningSwitch() {
        val lightningSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.lightningSwitch)
        lightningSwitch.setOnCheckedChangeListener { _, isChecked ->
            isLightningMode = isChecked
            if (isChecked) {
                Toast.makeText(this, "Режим Блискавка увімкнено! Вибирайте будь-яке число.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Режим Блискавка вимкнено.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onPause() {
        super.onPause()
        if (isGameInitialized) {
            saveGame()
            android.util.Log.d("SudokuGame", "onPause: Game save triggered")
        } else {
            android.util.Log.d("SudokuGame", "onPause: Game not initialized, save skipped")
        }
    }

    private fun updateNumberButtonsUI() {
        for (i in 0 until numberButtons.size) {
            val button = numberButtons[i]
            button.setTextColor(Color.parseColor("#00CED1")) // Бірюзовий колір тексту
            if (isLightningMode && selectedNumberForLightning == i + 1) {
                button.setBackgroundResource(R.drawable.number_button_highlighted)
            } else {
                button.setBackgroundResource(R.drawable.number_button_background)
            }
        }
    }

    private fun getNumberDrawable(number: Int): Int {
        return R.drawable.number_button_background
    }

    private fun checkWinCondition() {
        val generator = SudokuGenerator()
        val isCorrect = generator.isValidSolution(gameBoard)
        android.util.Log.d("SudokuGame", "Solution check: isCorrect = $isCorrect")
        if (isCorrect) {
            // Гра завершена
            isGameCompleted = true
            // Приховуємо кнопку Check
            checkButton.visibility = View.GONE
            checkButton.invalidate()
            checkButton.requestLayout()
            android.util.Log.d("SudokuGame", "Check button hidden")
            // Показуємо діалогове вікно з повідомленням про перемогу та пропозицією нової гри
            showWinDialog()
            disableAllButtons()
        } else {
            android.widget.Toast.makeText(this, R.string.incorrect_solution, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNumberButtons() {
        numberButtons.add(findViewById(R.id.num1))
        numberButtons.add(findViewById(R.id.num2))
        numberButtons.add(findViewById(R.id.num3))
        numberButtons.add(findViewById(R.id.num4))
        numberButtons.add(findViewById(R.id.num5))
        numberButtons.add(findViewById(R.id.num6))
        numberButtons.add(findViewById(R.id.num7))
        numberButtons.add(findViewById(R.id.num8))
        numberButtons.add(findViewById(R.id.num9))
        for (i in 0 until numberButtons.size) {
            numberButtons[i].setOnClickListener { onNumberClick(i + 1) }
            numberButtons[i].text = (i + 1).toString()
            numberButtons[i].setTextColor(Color.parseColor("#00CED1")) // Бірюзовий колір тексту
            numberButtons[i].setBackgroundResource(R.drawable.number_button_background)
        }
    }
} 