package com.example.shogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.shogi.ui.theme.ShogiTheme

/*****************************************************************************************
 * MainActivity is the main entry point for the Shogi application.
 * It sets up the Jetpack Compose content and initializes the game board.
 ****************************************************************************************/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge allows the app to draw behind the system bars.
        enableEdgeToEdge()
        setContent {
            ShogiTheme {
                // Initialize the game board and its pieces.
                val board = Board().apply { initializeBoard() }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Display the main Shogi board UI.
                    ShogiBoard(
                        board = board,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/*****************************************************************************************
 * The main composable for the Shogi game UI.
 * It manages the game state, including piece selection, moves, drops, and promotion.
 * It also renders the board, the pieces, and the captured pieces displays.
 *
 * @param board The current state of the game board.
 * @param modifier The modifier to be applied to the composable.
 ****************************************************************************************/
@Composable
fun ShogiBoard(board: Board, modifier: Modifier = Modifier) {
    // State for tracking the currently selected piece on the board.
    var selectedPiecePosition by remember { mutableStateOf<Position?>(null) }

    // State to trigger recomposition when the number of captured pieces changes.
    var capturedPieceCount by remember { mutableStateOf(board.whiteCaptured.size + board.blackCaptured.size) }

    // State to hold a piece that is eligible for promotion, triggering the promotion dialog.
    var pieceToPromote by remember { mutableStateOf<Piece?>(null) }

    // State for tracking a selected piece from the captured pieces area.
    var selectedCapturedPiece by remember { mutableStateOf<Piece?>(null) }

    // Calculates the list of possible moves for the currently selected piece.
    val possibleMoves: List<Move> by remember(selectedPiecePosition, board) {
        mutableStateOf(
            selectedPiecePosition?.let { pos ->
                val piece = board.board[pos.row][pos.column]
                if (piece !is Empty) {
                    val movesList = mutableListOf<Move>()
                    piece.getMoves(movesList, board)
                    movesList
                } else {
                    emptyList()
                }
            } ?: emptyList()
        )
    }

    // Extracts the destination positions from the list of possible moves.
    val possibleDestinationPositions = possibleMoves.map { it.posTo }

    // Calculates the list of valid positions where a selected captured piece can be dropped.
    val possibleDropPositions: List<Position> by remember(selectedCapturedPiece, board) {
        mutableStateOf(
            selectedCapturedPiece?.let { piece ->
                board.getValidDropPositions(piece)
            } ?: emptyList()
        )
    }

    // Show the promotion dialog if there is a piece waiting for a promotion decision.
    pieceToPromote?.let { piece ->
        PromotionDialog(
            onConfirm = {
                piece.promote(board)
                capturedPieceCount = board.whiteCaptured.size + board.blackCaptured.size
                pieceToPromote = null
                selectedPiecePosition = null
            },
            onDismiss = {
                // If the user declines promotion, just reset the state.
                capturedPieceCount = board.whiteCaptured.size + board.blackCaptured.size
                pieceToPromote = null
                selectedPiecePosition = null
            }
        )
    }

    // The main layout for the game screen.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display for White's (top player's) captured pieces.
        CapturedPiecesDisplay(
            capturedPieces = board.whiteCaptured.toList(),
            onPieceSelected = { piece ->
                // Allow selecting a captured piece only if it's White's turn.
                if (board.whiteTurn()) {
                    selectedCapturedPiece = board.whiteCaptured.find { it == piece }
                    selectedPiecePosition = null // Deselect any piece on the board.
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Renders the 9x9 Shogi board grid.
        Column {
            for (rowIndex in board.board.indices) {
                Row {
                    for (colIndex in board.board[rowIndex].indices) {
                        val currentIteratedPosition = Position(colIndex, rowIndex)
                        val piece = board.board[rowIndex][colIndex]
                        val isSelected = selectedPiecePosition == currentIteratedPosition

                        // Determine if the current square is a valid target for a move or a drop.
                        val isPossibleMoveTarget = possibleDestinationPositions.contains(currentIteratedPosition) ||
                                possibleDropPositions.contains(currentIteratedPosition)

                        // Set the background color based on the square's state.
                        val backgroundColor = when {
                            isSelected -> Color.Yellow // Selected piece
                            isPossibleMoveTarget -> Color.Green.copy(alpha = 0.5f) // Possible destination
                            else -> Color.LightGray // Default
                        }


                        // Represents a single square on the board.
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(backgroundColor)
                                .padding(4.dp)
                                .clickable {
                                    // Handle dropping a captured piece.
                                    if (selectedCapturedPiece != null && possibleDropPositions.contains(currentIteratedPosition)) {
                                        val pieceToDrop = selectedCapturedPiece!!
                                        val dropMove = Drop(currentIteratedPosition, pieceToDrop)
                                        pieceToDrop.drop(dropMove, board)
                                        selectedCapturedPiece = null
                                        capturedPieceCount = board.whiteCaptured.size + board.blackCaptured.size
                                    }
                                    // Handle moving a piece on the board.
                                    else if (possibleDestinationPositions.contains(currentIteratedPosition)) {
                                        val move = possibleMoves.first { it.posTo == currentIteratedPosition }
                                        val movingPiece = board.board[selectedPiecePosition!!.row][selectedPiecePosition!!.column]

                                        // Execute the move and check if promotion is an option.
                                        val promotionIsOptional = movingPiece.executeMove(move, board)
                                        selectedPiecePosition = null

                                        // If promotion is possible, set the piece to trigger the dialog.
                                        if (promotionIsOptional) {
                                            pieceToPromote = movingPiece
                                        }
                                        capturedPieceCount = board.whiteCaptured.size + board.blackCaptured.size
                                    }
                                    // Handle selecting a piece or deselecting.
                                    else {
                                        val pieceAtClickedSquare = board.board[currentIteratedPosition.row][currentIteratedPosition.column]
                                        // Check if there is a piece on the clicked square.
                                        if (pieceAtClickedSquare.symbol != "") {
                                            // Check if the piece belongs to the current player.
                                            if (board.whiteTurn() == pieceAtClickedSquare.isWhite) {
                                                // Toggle selection of the piece.
                                                selectedPiecePosition =
                                                    if (selectedPiecePosition == currentIteratedPosition) null else currentIteratedPosition
                                            }
                                            selectedCapturedPiece = null // Deselect any captured piece.
                                        } else if (selectedPiecePosition != null) {
                                            // If an empty square is clicked, deselect the current piece.
                                            selectedPiecePosition = null
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center

                        ) {
                            Text(
                                text = piece.symbol,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display for Black's (bottom player's) captured pieces.
        CapturedPiecesDisplay(
            capturedPieces = board.blackCaptured.toList(),
            onPieceSelected = { piece ->
                // Allow selecting a captured piece only if it's Black's turn.
                if (!board.whiteTurn()) {
                    selectedCapturedPiece = board.blackCaptured.find { it == piece }
                    selectedPiecePosition = null // Deselect any piece on the board.
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/*****************************************************************************************
 * A composable that displays a list of captured pieces for a player.
 * The pieces are arranged in up to three rows.
 *
 * @param capturedPieces The list of pieces to display.
 * @param onPieceSelected A callback function invoked when a piece is tapped.
 * @param modifier The modifier to be applied to the composable.
 ****************************************************************************************/
@Composable
fun CapturedPiecesDisplay(
    capturedPieces: List<Piece>,
    onPieceSelected: (Piece) -> Unit,
    modifier: Modifier = Modifier
) {
    // Organizes pieces into chunks to be displayed in rows.
    val piecesPerRow = (capturedPieces.size / 3) + 1
    val rowsOfPieces = if (capturedPieces.isEmpty()) {
        emptyList()
    } else {
        capturedPieces.chunked(piecesPerRow)
    }

    // A container for the captured pieces display.
    Box(
        modifier = modifier
            .height(90.dp)
            .background(Color.LightGray.copy(alpha = 0.4f))
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Display up to 3 rows of captured pieces.
            for (i in 0 until 3) {
                Row {
                    if (i < rowsOfPieces.size) {
                        rowsOfPieces[i].forEach { piece ->
                            CapturedPieceItem(piece = piece, onPieceSelected = onPieceSelected)
                        }
                    }
                }
            }
        }
    }
}

/*****************************************************************************************
 * A composable that represents a single captured piece item.
 *
 * @param piece The piece to display.
 * @param onPieceSelected A callback function invoked when the piece is tapped.
 * @param modifier The modifier to be applied to the composable.
 ****************************************************************************************/
@Composable
fun CapturedPieceItem(
    piece: Piece,
    onPieceSelected: (Piece) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(Color.LightGray)
            .clickable { onPieceSelected(piece) }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = piece.symbol, fontSize = 18.sp)
    }
}

/*****************************************************************************************
 * A dialog that asks the user whether they want to promote a piece.
 *
 * @param onConfirm A callback function invoked when the "Yes" button is pressed.
 * @param onDismiss A callback function invoked when the "No" button is pressed or the dialog is dismissed.
 ****************************************************************************************/
@Composable
fun PromotionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote Piece") },
        text = { Text("Do you want to promote this piece?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

/****************************************************************************************
 * A preview composable for displaying the Shogi board in Android Studio's preview panel.
 ****************************************************************************************/
@Preview(showBackground = true)
@Composable
fun ShogiBoardPreview() {
    ShogiTheme {
        // Creates a board with the initial piece setup for the preview.
        val board = Board().apply { initializeBoard() }
        ShogiBoard(board = board)
    }
}
