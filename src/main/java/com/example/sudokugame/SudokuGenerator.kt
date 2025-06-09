package com.example.sudokugame

import android.util.Log
import java.util.Random

class SudokuGenerator {
    private val TAG = "SudokuGenerator"

    fun generatePuzzle(emptyCells: Int, gridSize: Int = 9): Array<IntArray> {
        Log.d(TAG, "Generating puzzle for gridSize=$gridSize with $emptyCells empty cells")
        val grid = Array(gridSize) { IntArray(gridSize) { 0 } }
        // Заповнюємо діагональні блоки (вони незалежні, тому це безпечно)
        fillDiagonal(grid, gridSize)
        Log.d(TAG, "Diagonal blocks filled, solving the rest of the puzzle")
        // Розв'язуємо решту головоломки з тайм-аутом
        val solved = solveWithTimeout(grid, gridSize)
        if (!solved) {
            Log.w(TAG, "Failed to solve puzzle within timeout, using fallback")
            return getFallbackPuzzle(emptyCells, gridSize)
        }
        Log.d(TAG, "Puzzle solved, removing $emptyCells cells")
        // Видаляємо задану кількість клітинок
        return removeNumbers(grid, emptyCells, gridSize)
    }

    private fun fillDiagonal(grid: Array<IntArray>, gridSize: Int) {
        val blockSize = if (gridSize == 9) 3 else if (gridSize == 6) 2 else 3
        for (i in 0 until gridSize step blockSize) {
            fillBox(grid, i, i, gridSize)
        }
    }

    private fun fillBox(grid: Array<IntArray>, row: Int, col: Int, gridSize: Int) {
        val blockSize = if (gridSize == 9) 3 else if (gridSize == 6) 2 else 3
        val numbers = (1..gridSize).shuffled()
        var index = 0
        for (i in 0 until blockSize) {
            for (j in 0 until blockSize) {
                if (row + i < gridSize && col + j < gridSize) {
                    grid[row + i][col + j] = numbers[index++]
                }
            }
        }
    }

    private fun solveWithTimeout(grid: Array<IntArray>, gridSize: Int): Boolean {
        val timeoutMillis = 10000L // 10 секунд тайм-аут
        val startTime = System.currentTimeMillis()
        val solved = solveSudoku(grid, 0, 0, gridSize)
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Solving took $elapsed ms")
        if (elapsed > timeoutMillis) {
            Log.w(TAG, "Solving timed out after $elapsed ms")
            return false
        }
        return solved
    }

    fun solveSudoku(grid: Array<IntArray>, row: Int, col: Int, gridSize: Int): Boolean {
        if (row == gridSize) {
            return true
        }
        if (col == gridSize) {
            return solveSudoku(grid, row + 1, 0, gridSize)
        }
        if (grid[row][col] != 0) {
            return solveSudoku(grid, row, col + 1, gridSize)
        }
        for (num in 1..gridSize) {
            if (isSafe(grid, row, col, num, gridSize)) {
                grid[row][col] = num
                if (solveSudoku(grid, row, col + 1, gridSize)) {
                    return true
                }
                grid[row][col] = 0
            }
        }
        return false
    }

    private fun isSafe(grid: Array<IntArray>, row: Int, col: Int, num: Int, gridSize: Int): Boolean {
        // Перевірка рядка і стовпця
        for (x in 0 until gridSize) {
            if (grid[row][x] == num || grid[x][col] == num) {
                return false
            }
        }
        // Перевірка блоку
        val blockRowSize = if (gridSize == 9) 3 else if (gridSize == 6) 3 else 3
        val blockColSize = if (gridSize == 9) 3 else if (gridSize == 6) 2 else 3
        val startRow = row - row % blockRowSize
        val startCol = col - col % blockColSize
        for (i in 0 until blockRowSize) {
            for (j in 0 until blockColSize) {
                if (startRow + i < gridSize && startCol + j < gridSize && grid[startRow + i][startCol + j] == num) {
                    return false
                }
            }
        }
        return true
    }

    private fun removeNumbers(grid: Array<IntArray>, emptyCells: Int, gridSize: Int): Array<IntArray> {
        val result = grid.map { it.copyOf() }.toTypedArray()
        var cellsToRemove = emptyCells
        val random = Random()
        while (cellsToRemove > 0) {
            val row = random.nextInt(gridSize)
            val col = random.nextInt(gridSize)
            if (result[row][col] != 0) {
                result[row][col] = 0
                cellsToRemove--
            }
        }
        return result
    }

    fun isValidSolution(grid: Array<IntArray>, gridSize: Int = 9): Boolean {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val num = grid[row][col]
                if (num == 0) return false
                grid[row][col] = 0
                if (!isSafe(grid, row, col, num, gridSize)) {
                    grid[row][col] = num
                    return false
                }
                grid[row][col] = num
            }
        }
        return true
    }

    fun getFallbackPuzzle(emptyCells: Int, gridSize: Int = 9): Array<IntArray> {
        Log.d(TAG, "Using fallback puzzle for gridSize=$gridSize")
        val fallbackGrid = if (gridSize == 9) {
            arrayOf(
                intArrayOf(5, 3, 0, 0, 7, 0, 0, 0, 0),
                intArrayOf(6, 0, 0, 1, 9, 5, 0, 0, 0),
                intArrayOf(0, 9, 8, 0, 0, 0, 0, 6, 0),
                intArrayOf(8, 0, 0, 0, 6, 0, 0, 0, 3),
                intArrayOf(4, 0, 0, 8, 0, 3, 0, 0, 1),
                intArrayOf(7, 0, 0, 0, 2, 0, 0, 0, 6),
                intArrayOf(0, 6, 0, 0, 0, 0, 2, 8, 0),
                intArrayOf(0, 0, 0, 4, 1, 9, 0, 0, 5),
                intArrayOf(0, 0, 0, 0, 8, 0, 0, 7, 9)
            )
        } else {
            arrayOf(
                intArrayOf(1, 2, 0, 0, 5, 6),
                intArrayOf(3, 4, 0, 0, 0, 0),
                intArrayOf(0, 0, 5, 6, 0, 0),
                intArrayOf(0, 0, 1, 2, 0, 0),
                intArrayOf(0, 0, 0, 0, 3, 4),
                intArrayOf(5, 6, 0, 0, 1, 2)
            )
        }
        // Розв'язуємо головоломку, щоб переконатися, що вона дійсна
        if (!solveWithTimeout(fallbackGrid, gridSize)) {
            Log.w(TAG, "Fallback puzzle could not be solved")
        }
        // Видаляємо потрібну кількість клітинок
        return removeNumbers(fallbackGrid, emptyCells, gridSize)
    }

    fun solveSudoku(grid: Array<IntArray>, gridSize: Int = 9): Array<IntArray> {
        solveSudoku(grid, 0, 0, gridSize)
        return grid
    }
} 