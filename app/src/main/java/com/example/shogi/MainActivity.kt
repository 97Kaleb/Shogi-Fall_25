package com.example.shogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.data.position
import androidx.compose.ui.tooling.preview.Preview
import com.example.shogi.ui.theme.ShogiTheme
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.rotate

class PentagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)                 // Tip (Top Center)
            lineTo(size.width, size.height * 0.35f)     // Top Right Shoulder
            lineTo(size.width * 0.85f, size.height)     // Bottom Right
            lineTo(size.width * 0.15f, size.height)     // Bottom Left
            lineTo(0f, size.height * 0.35f)             // Top Left Shoulder
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun ShogiPieceView(piece: Piece) {
    val rotationAngle = if (piece.isWhite) 180f else 0f
    val textRotation = if (piece.isWhite) 180f else 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(1.dp)
            .fillMaxSize()
            .rotate(rotationAngle)
            .background(Color(0xFFE1C699), shape = PentagonShape())
            .border(1.dp, Color.Black, shape = PentagonShape())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = piece.symbol,
                fontSize = 18.sp,
                color = Color.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, // For some reason I couldn't get this to work any other way.
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(textRotation)
            )
        }
    }
}


/*****************************************************************************************
 * MainActivity is the main entry point for the Shogi application.
 * It sets up the Jetpack Compose content and initializes the game board.
 ****************************************************************************************/
class MainActivity : ComponentActivity() {

    // Network Helpers
    private var gameServer: GameServer? = null
    private var networkDiscovery: NetworkDiscovery? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize discovery helper
        networkDiscovery = NetworkDiscovery(this)

        setContent {
            ShogiTheme {
                // --- STATE VARIABLES ---
                // We use 'remember' so the board doesn't reset every time the screen updates
                val board = remember { Board().apply { initializeBoard() } }

                var isMultiplayer by remember { mutableStateOf(false) }
                var amIWhite by remember { mutableStateOf(true) } // Host=White, Client=Black
                var showConnectionDialog by remember { mutableStateOf(true) }

                // Helper state to show connection status ("Scanning...", "Waiting for player...")
                var connectionStatus by remember { mutableStateOf("") }

                // Trigger to force UI to redraw when a move comes in
                var refreshTrigger by remember { mutableStateOf(0) }

                // --- 1. HANDLE INCOMING MOVES ---
                fun handleRemoteMove(move: NetworkMove) {
                    // Run on UI thread just in case
                    runOnUiThread {
                        if (move.type == "MOVE") {
                            val piece = board.board[move.fromRow][move.fromCol]
                            val targetPos = Position(move.toCol, move.toRow)
                            val gameMove = Move(piece.pos, targetPos, piece)

                            piece.executeMove(gameMove, board)
                            if (move.promote) piece.promote(board)
                        }
                        else if (move.type == "DROP") {
                            val captureList = if (board.whiteTurn()) board.whiteCaptured else board.blackCaptured
                            val pieceToDrop = captureList.firstOrNull { it.symbol == move.pieceSymbol }
                            if (pieceToDrop != null) {
                                val targetPos = Position(move.toCol, move.toRow)
                                pieceToDrop.drop(Drop(targetPos, pieceToDrop), board)
                            }
                        }
                        // Update trigger to refresh Compose UI
                        refreshTrigger++
                    }
                }

                // --- 2. THE UI ---
                if (showConnectionDialog) {
                    // === MAIN MENU / CONNECTION SCREEN ===
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Shogi", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(30.dp))

                        // PLAY LOCAL
                        Button(onClick = {
                            isMultiplayer = false
                            showConnectionDialog = false
                        }) { Text("Play Local (Hotseat)") }

                        Spacer(modifier = Modifier.height(20.dp))

                        // HOST GAME
                        Button(onClick = {
                            connectionStatus = "Hosting... Waiting for player..."

                            // 1. Start Server
                            gameServer = GameServer(
                                onMoveReceived = { move -> handleRemoteMove(move) },
                                onConnected = { isWhite ->
                                    amIWhite = isWhite // Host is White
                                    isMultiplayer = true
                                    showConnectionDialog = false
                                    // Stop broadcasting service once connected
                                    networkDiscovery?.stopService()
                                }
                            )
                            gameServer?.startHost()

                            // 2. Broadcast presence so Client can find us
                            networkDiscovery?.registerService(8888)
                        }) { Text("Host Game (As White)") }

                        Spacer(modifier = Modifier.height(20.dp))

                        // JOIN GAME (AUTO SCAN)
                        Button(onClick = {
                            connectionStatus = "Scanning for hosts..."

                            // 1. Start Scanning
                            networkDiscovery?.discoverServices { hostAddress, port ->
                                networkDiscovery?.stopServiceDiscovery()

                                runOnUiThread {
                                    connectionStatus = "Host found! Connecting..."
                                }

                                // 2. Connect (only if we haven't already started connecting)
                                if (gameServer == null) {
                                    gameServer = GameServer(
                                        onMoveReceived = { move -> handleRemoteMove(move) },
                                        onConnected = { isWhite ->
                                            amIWhite = isWhite
                                            isMultiplayer = true
                                            showConnectionDialog = false
                                        }
                                    )
                                    gameServer?.joinGame(hostAddress.hostAddress!!)
                                }
                            }
                        }) { Text("Join Game (As Black)") }

                        // Status Text
                        if (connectionStatus.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(connectionStatus)
                        }
                    }
                } else {
                    // === GAME SCREEN ===
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        // This empty text reads the trigger to force a recompose when moves happen
                        Text(text = "$refreshTrigger", modifier = Modifier.size(0.dp))

                        ShogiBoard(
                            board = board,
                            isMultiplayer = isMultiplayer,
                            myPlayerColorIsWhite = amIWhite,
                            onNetworkMoveMade = { move ->
                                gameServer?.sendMove(move)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameServer?.close()
        networkDiscovery?.stopService()
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
fun ShogiBoard(
    board: Board,
    isMultiplayer: Boolean,
    myPlayerColorIsWhite: Boolean, // True if we are White, False if Black
    onNetworkMoveMade: (NetworkMove) -> Unit, // Callback when we make a move
    modifier: Modifier = Modifier
) {
    // State for tracking the currently selected piece on the board.
    var selectedPiecePosition by remember { mutableStateOf<Position?>(null) }

    // State to track if the game has ended and who won
    var winner by remember { mutableStateOf<String?>(null) }

    // State to track move/drop details in preparation for sending the networking signal
    var pendingNetworkMove by remember { mutableStateOf<NetworkMove?>(null) }


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
                    movesList.filter { move ->
                        isMoveSafe(
                            move = move,
                            fromPos = pos,
                            board = board,
                            playerColor = piece.isWhite
                        )
                    }
                } else {
                    emptyList()
                }
            } ?: emptyList()
        )
    }

    // Extracts the destination positions from the list of possible moves.
    val possibleDestinationPositions = possibleMoves.map { it.posTo }

    // Calculates the list of valid positions where a selected captured piece can be dropped.
    // Calculates the list of valid positions where a selected captured piece can be dropped.
    val possibleDropPositions: List<Position> by remember(selectedCapturedPiece, board) {
        mutableStateOf(
            selectedCapturedPiece?.let { piece ->
                val rawDropSpots = board.getValidDropPositions(piece)

                rawDropSpots.filter { pos ->
                    // Ensure drop doesn't cause Self-Check
                    val isSelfSafe = isDropSafe(
                        dropPos = pos,
                        pieceToDrop = piece,
                        board = board,
                        playerColor = piece.isWhite
                    )

                    // Ensure a pawn drop isn't a checkmate
                    val isNotUchifuzume = !isUchifuzume(
                        dropPos = pos,
                        pieceToDrop = piece,
                        board = board
                    )
                    isSelfSafe && isNotUchifuzume
                }
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

    // Show Game Over Dialog
    winner?.let { w ->
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal or reset game */ },
            title = { Text(text = "Game Over") },
            text = { Text(text = "Checkmate! $w Wins!") },
            confirmButton = {
                Button(onClick = {
                    // Optional: Add logic to reset board here
                    winner = null
                }) {
                    Text("OK")
                }
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

                        val isKingInCheck = (piece is King || piece is Opposing_King) && isPlayerInCheck(piece.isWhite, board)

                        // Set the background color based on the square's state.
                        val backgroundColor = when {
                            isSelected -> Color.Yellow // Selected piece
                            isKingInCheck -> Color.Red.copy(alpha = 0.6f) // King is in check
                            isPossibleMoveTarget -> Color.Green.copy(alpha = 0.5f) // Possible destination
                            else -> Color.LightGray // Default
                        }


                        // Represents a single square on the board.
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(backgroundColor)
                                .border(1.dp, Color.Black)
                                .padding(4.dp)
                                .clickable {    // 1. BLOCK INPUT IF NOT YOUR TURN
                                    val isMyTurn =
                                        if (isMultiplayer) board.whiteTurn() == myPlayerColorIsWhite else true
                                    if (!isMyTurn) return@clickable

                                    // 2. HANDLE DROP LOGIC
                                    if (selectedCapturedPiece != null && possibleDropPositions.contains(
                                            currentIteratedPosition
                                        )
                                    ) {
                                        val pieceToDrop = selectedCapturedPiece!!
                                        val dropMove = Drop(currentIteratedPosition, pieceToDrop)

                                        // Capture data for network BEFORE the object references change
                                        val netMove = NetworkMove(
                                            type = "DROP",
                                            toRow = currentIteratedPosition.row,
                                            toCol = currentIteratedPosition.column,
                                            pieceSymbol = pieceToDrop.symbol,
                                            promote = false
                                        )

                                        // Execute Game Logic
                                        pieceToDrop.drop(dropMove, board)

                                        // Finalize Turn immediately (Drops never trigger promotion dialogs)
                                        selectedCapturedPiece = null
                                        capturedPieceCount =
                                            board.whiteCaptured.size + board.blackCaptured.size

                                        if (isMultiplayer) onNetworkMoveMade(netMove) // SEND NOW

                                        // Check Win Condition
                                        val nextPlayerColor = board.whiteTurn()
                                        if (checkForCheckmate(nextPlayerColor, board)) {
                                            winner = if (!nextPlayerColor) "White" else "Black"
                                        }
                                    }
                                    // 3. HANDLE MOVE LOGIC
                                    else if (possibleDestinationPositions.contains(
                                            currentIteratedPosition
                                        )
                                    ) {
                                        val move =
                                            possibleMoves.first { it.posTo == currentIteratedPosition }
                                        val movingPiece =
                                            board.board[selectedPiecePosition!!.row][selectedPiecePosition!!.column]

                                        // Capture data for network
                                        var netMove = NetworkMove(
                                            type = "MOVE",
                                            fromRow = selectedPiecePosition!!.row,
                                            fromCol = selectedPiecePosition!!.column,
                                            toRow = currentIteratedPosition.row,
                                            toCol = currentIteratedPosition.column,
                                            promote = false // Default, might update later
                                        )

                                        // Execute Game Logic
                                        val promotionIsOptional =
                                            movingPiece.executeMove(move, board)
                                        selectedPiecePosition = null

                                        if (promotionIsOptional) {
                                            // DO NOT SEND YET. Wait for Dialog.
                                            pieceToPromote = movingPiece
                                            pendingNetworkMove = netMove // Store it for later
                                        } else {
                                            // No promotion needed? Send immediately.
                                            if (isMultiplayer) onNetworkMoveMade(netMove)
                                        }

                                        capturedPieceCount =
                                            board.whiteCaptured.size + board.blackCaptured.size

                                        // Check Win Condition
                                        val nextPlayerColor = board.whiteTurn()
                                        if (checkForCheckmate(nextPlayerColor, board)) {
                                            winner = if (!nextPlayerColor) "White" else "Black"
                                        }
                                    }
                                    // 4. HANDLE SELECTION (Normal click on a piece)
                                    else {
                                        if (piece !is Empty && piece.isWhite == board.whiteTurn()) {
                                            selectedPiecePosition = currentIteratedPosition
                                            selectedCapturedPiece = null
                                        } else {
                                            selectedPiecePosition = null
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (piece !is Empty) {
                                ShogiPieceView(piece = piece)
                            }
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

fun getAllMoves(playerColor: Boolean, board: Board): List<Position> {
    val allMovesTo = mutableListOf<Position>()
    for (row in board.board.indices) {
        for (col in board.board[row].indices) {
            val piece = board.board[row][col]
            if (piece !is Empty && piece.isWhite == playerColor) {
                val moves = mutableListOf<Move>()
                piece.getMoves(moves, board)
                for (move in moves) {
                    allMovesTo.add(move.posTo)
                }
            }
        }
    }
    return allMovesTo
}

/**
 * Locates the position of the King for the specified player.
 */
fun getKingPosition(playerColor: Boolean, board: Board): Position? {
    for (row in board.board.indices) {
        for (col in board.board[row].indices) {
            val piece = board.board[row][col]
            // Assumes you have a 'King' class.
            // If you identify pieces by symbol, use: piece.symbol == "K" (or your specific symbol)
            if ((piece is King || piece is Opposing_King) && piece.isWhite == playerColor) {
                return Position(col, row)
            }
        }
    }
    return null // Should not happen in a valid game, but good for safety
}

/**
 * Determines if the specified player's King is in check.
 * Returns true if the King is under attack.
 */
fun isPlayerInCheck(playerColor: Boolean, board: Board): Boolean {
    // 1. Find the player's King
    val kingPos = getKingPosition(playerColor, board) ?: return false

    // 2. Get all valid moves of the OPPONENT (!playerColor)
    // Note: This calls the function you just fixed
    val opponentAttacks = getAllMoves(!playerColor, board)

    // 3. Return true if the opponent can move to the King's square
    return opponentAttacks.contains(kingPos)
}

/**
 * Simulates a move to see if it leaves the player's King in check.
 * 1. Performs the move temporarily.
 * 2. Checks if the King is safe.
 * 3. Reverts the board to its original state.
 */
fun isMoveSafe(
    move: Move,
    fromPos: Position,
    board: Board,
    playerColor: Boolean
): Boolean {
    val toRow = move.posTo.row
    val toCol = move.posTo.column
    val fromRow = fromPos.row
    val fromCol = fromPos.column

    // 1. Snapshot the current state
    val movingPiece = board.board[fromRow][fromCol]
    val capturedPiece = board.board[toRow][toCol] // Could be Empty or an opponent piece

    // 2. Apply the Move Temporarily
    board.board[toRow][toCol] = movingPiece
    // Assuming you have an Empty class. If Empty requires arguments, adjust here.
    board.board[fromRow][fromCol] = Empty()

    // 3. Check if the King is safe in this new configuration
    val isSafe = !isPlayerInCheck(playerColor, board)

    // 4. Undo the Move (Restore State)
    board.board[fromRow][fromCol] = movingPiece
    board.board[toRow][toCol] = capturedPiece

    return isSafe
}

/**
 * Simulates a drop to ensure it doesn't leave the player in check.
 */
fun isDropSafe(
    dropPos: Position,
    pieceToDrop: Piece,
    board: Board,
    playerColor: Boolean
): Boolean {
    val r = dropPos.row
    val c = dropPos.column

    // 1. Snapshot the current state (it should be Empty, but we store it to be safe)
    val originalSquareContent = board.board[r][c]
    val originalPiecePosition = pieceToDrop.pos // Store original pos (likely off-board)

    // 2. Apply Drop Temporarily
    board.board[r][c] = pieceToDrop
    // CRITICAL: Update the piece's internal position so logic that relies on 'this.position' works
    pieceToDrop.pos = dropPos

    // 3. Check if this configuration is safe for the King
    val isSafe = !isPlayerInCheck(playerColor, board)

    // 4. Undo the Drop (Restore State)
    board.board[r][c] = originalSquareContent
    pieceToDrop.pos = originalPiecePosition

    return isSafe
}

/**
 * Checks if the specified player is in Checkmate.
 * Returns true if the player is in Check AND has no valid moves or drops to escape.
 */
fun checkForCheckmate(playerColor: Boolean, board: Board): Boolean {// 1. If not in check, it's not checkmate (could be stalemate, but Shogi rarely has stalemate)
    if (!isPlayerInCheck(playerColor, board)) {
        return false
    }

    // 2. Check if any on-board piece can move to save the King
    for (row in board.board.indices) {
        for (col in board.board[row].indices) {
            val piece = board.board[row][col]
            if (piece !is Empty && piece.isWhite == playerColor) {
                val moves = mutableListOf<Move>()
                piece.getMoves(moves, board)

                for (move in moves) {
                    if (isMoveSafe(move, Position(col, row), board, playerColor)) {
                        return false // Found a way out!
                    }
                }
            }
        }
    }

    // 3. Check if any captured piece can be dropped to save the King
    val capturedPieces = if (playerColor) board.whiteCaptured else board.blackCaptured

    // Optimization: Only check distinct piece types (e.g., checking one Pawn is enough)
    val distinctPieces = capturedPieces.distinctBy { it.symbol }

    for (piece in distinctPieces) {
        val validDrops = board.getValidDropPositions(piece)
        for (pos in validDrops) {
            if (isDropSafe(pos, piece, board, playerColor)) {
                return false // Found a way out!
            }
        }
    }

    // 4. If we are here: Player is in check, has no moves, and no drops.
    return true
}

/**
 * Uchifuzume is a rule that states that checkmate cannot be achieved via a pawn drop.
 */
fun isUchifuzume(dropPos: Position, pieceToDrop: Piece, board: Board): Boolean {
    // 1. This rule ONLY applies to Pawns.
    // (If you have a separate class for Opposing_Pawn, add checks for that here too)
    if (pieceToDrop !is Pawn) {
        return false
    }

    val r = dropPos.row
    val c = dropPos.column

    // 2. Snapshot state
    val originalSquareContent = board.board[r][c]
    val originalPiecePosition = pieceToDrop.pos

    // 3. Apply Drop Temporarily
    board.board[r][c] = pieceToDrop
    pieceToDrop.pos = dropPos

    // 4. Check if the OPPONENT is now in Checkmate
    val opponentColor = !pieceToDrop.isWhite
    val isMate = checkForCheckmate(opponentColor, board)

    // 5. Revert State
    board.board[r][c] = originalSquareContent
    pieceToDrop.pos = originalPiecePosition

    return isMate
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
        ShogiBoard(
            board = board,
            isMultiplayer = false,          // Dummy value: Preview is always local
            myPlayerColorIsWhite = true,    // Dummy value
            onNetworkMoveMade = {}          // Dummy value: Do nothing on move
        )
    }
}

