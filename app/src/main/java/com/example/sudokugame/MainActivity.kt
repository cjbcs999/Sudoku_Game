package com.example.sudokugame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SudokuScreen()
            }
        }
    }
}

@Composable
fun SudokuScreen() {
    // State for displaying Snackbar messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Create a 9x9 Sudoku board (81 cells in total)
    // Initial state: The first row (index 0-8) is randomly shuffled from 1 to 9, while the rest remain empty
    val board = remember { mutableStateListOf<Int?>() }
    fun resetBoard() {
        board.clear()
        val firstRow = (1..9).shuffled()
        firstRow.forEach { board.add(it) }
        repeat(81 - 9) { board.add(null) }
    }
    // Reset the board when the composable is first launched
    LaunchedEffect(Unit) {
        resetBoard()
    }

    // Stores the index of the selected cell where the user wants to input a number (only for empty cells)
    var selectedCellIndex by remember { mutableStateOf<Int?>(null) }
    // Stores the input text in the dialog
    var inputText by remember { mutableStateOf("") }

    // If a cell is selected, display an AlertDialog for user input
    if (selectedCellIndex != null) {
        AlertDialog(
            onDismissRequest = {
                selectedCellIndex = null
                inputText = ""
            },
            title = { Text("Enter a number (1-9)") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val index = selectedCellIndex!!
                    // If input is empty, clear the cell
                    if (inputText.isBlank()) {
                        board[index] = null
                    } else {
                        val number = inputText.toIntOrNull()
                        if (number == null || number !in 1..9) {
                            scope.launch { snackbarHostState.showSnackbar("Please enter a number between 1-9") }
                        } else {
                            // Check if the move is valid
                            if (isValidMove(board, index, number)) {
                                board[index] = number
                                // Check for win condition
                                if (checkWin(board)) {
                                    scope.launch { snackbarHostState.showSnackbar("You won!") }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Invalid number") }
                            }
                        }
                    }
                    selectedCellIndex = null
                    inputText = ""
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = {
                    selectedCellIndex = null
                    inputText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Use Scaffold to structure the UI, including the AppBar, Snackbar, and Reset button
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { CustomTopAppBar(title = ("Sudoku")) },
        floatingActionButton = {
            FloatingActionButton(onClick = { resetBoard() }) {
                Text("Reset")
            }
        }
    ) { paddingValues ->
        // Use LazyVerticalGrid to create a 9-row Sudoku board
        LazyVerticalGrid(
            columns = GridCells.Fixed(9),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(board.size) { index ->
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.Black)
                        .clickable {
                            if (index >= 9) {
                                selectedCellIndex = index
                                // If the cell already has a number, display it in the dialog, otherwise show empty
                                inputText = board[index]?.toString() ?: ""
                            }
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = board[index]?.toString() ?: "")
                }
            }
        }
    }
}

@Composable
fun CustomTopAppBar(
    title: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor
        )
    }
}

/** Checks whether placing `value` in `board[index]` follows Sudoku rules */
fun isValidMove(board: List<Int?>, index: Int, value: Int): Boolean {
    val row = index / 9
    val col = index % 9

    // Check row
    for (c in 0 until 9) {
        val cell = board[row * 9 + c]
        if (cell == value) return false
    }
    // Check column
    for (r in 0 until 9) {
        val cell = board[r * 9 + col]
        if (cell == value) return false
    }
    // Check 3x3 block
    val blockRowStart = (row / 3) * 3
    val blockColStart = (col / 3) * 3
    for (r in blockRowStart until blockRowStart + 3) {
        for (c in blockColStart until blockColStart + 3) {
            val cell = board[r * 9 + c]
            if (cell == value) return false
        }
    }
    return true
}

/** Checks if the board is fully filled and each row, column, and block contains unique numbers 1-9 */
fun checkWin(board: List<Int?>): Boolean {
    if (board.any { it == null }) return false
    // Check rows
    for (r in 0 until 9) {
        val set = mutableSetOf<Int>()
        for (c in 0 until 9) {
            val cell = board[r * 9 + c] ?: return false
            if (!set.add(cell)) return false
        }
        if (set.size != 9) return false
    }
    // Check columns
    for (c in 0 until 9) {
        val set = mutableSetOf<Int>()
        for (r in 0 until 9) {
            val cell = board[r * 9 + c] ?: return false
            if (!set.add(cell)) return false
        }
        if (set.size != 9) return false
    }
    // Check 3x3 blocks
    for (blockRow in 0 until 3) {
        for (blockCol in 0 until 3) {
            val set = mutableSetOf<Int>()
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    val cell = board[(blockRow * 3 + r) * 9 + (blockCol * 3 + c)] ?: return false
                    if (!set.add(cell)) return false
                }
            }
            if (set.size != 9) return false
        }
    }
    return true
}

@Preview(showBackground = true)
@Composable
fun PreviewSudokuScreen() {
    SudokuScreen()
}
