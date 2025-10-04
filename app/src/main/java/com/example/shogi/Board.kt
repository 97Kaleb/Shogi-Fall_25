package com.example.shogi

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/*****************************************************************************************
 * The board is where all the pieces are stored.
 ****************************************************************************************/
class Board {
//  getters
    fun whiteTurn(): Boolean {if (numMoves % 2 == 0) {return true} else {return false}}

//  setters
    fun initializeBoard()
    {
        board[0][0] = Lance(Position(0, 0), true)
        board[0][1] = Knight(Position(1, 0), true)
        board[0][2] = Silver(Position(2, 0), true)
        board[0][3] = Gold(Position(3, 0), true)
        board[0][4] = King(Position(4, 0), true)
        board[0][5] = Gold(Position(5, 0), true)
        board[0][6] = Silver(Position(6, 0), true)
        board[0][7] = Knight(Position(7, 0), true)
        board[0][8] = Lance(Position(8, 0), true)
        board[1][7] = Bishop(Position(7, 1), true)
        board[1][1] = Rook(Position(1, 1), true)
        for (col in 0..8) {board[2][col] = Pawn(Position(col, 2), true)}
        for (col in 0..8) {board[6][col] = Pawn(Position(col, 6), false)}
        board[7][7] = Rook(Position(7, 7), false)
        board[7][1] = Bishop(Position(1, 7), false)
        board[8][0] = Lance(Position(0, 8), false)
        board[8][1] = Knight(Position(1, 8), false)
        board[8][2] = Silver(Position(2, 8), false)
        board[8][3] = Gold(Position(3, 8), false)
        board[8][4] = Opposing_King(Position(4, 8), false)
        board[8][5] = Gold(Position(5, 8), false)
        board[8][6] = Silver(Position(6, 8), false)
        board[8][7] = Knight(Position(7, 8), false)
        board[8][8] = Lance(Position(8, 8), false)
    }

    /*****************************************************************************************
     * Returns a list of positions where a piece can legally be dropped.
     ****************************************************************************************/
    fun getValidDropPositions(piece: Piece): List<Position> {
        val validPositions = mutableListOf<Position>()
        for (r in 0..8) {
            for (c in 0 .. 8) {
                val currentPos = Position(c, r)
                if (board[r][c] is Empty) {
                    var isValid = true
                    if (piece is Pawn) {
                        for (row in 0 until 9) {
                            val p = board[row][c]
                            if (p is Pawn && p.isWhite == piece.isWhite) {
                                isValid = false
                                break
                            }
                        }
                    }

                    val lastRank = if (piece.isWhite) 0 else 8
                    val secondLastRank = if (piece.isWhite) 1 else 7
                    if ( (piece is Pawn || piece is Lance) && r == lastRank) {
                        isValid = false
                    }
                    if (piece is Knight && (r == lastRank || r == secondLastRank)) {
                        isValid = false
                    }

                    if (isValid) {
                        validPositions.add(currentPos)
                    }
                }
            }
        }
        return validPositions
    }
//  attributes
    var numMoves: Int = 0
    val board: Array<Array<Piece>> = Array(9) { Array(9) { Empty() } }
    // Stores the captured pieces for each player so that they can be dropped later.
    val whiteCaptured: SnapshotStateList<Piece> = mutableStateListOf()
    val blackCaptured: SnapshotStateList<Piece> = mutableStateListOf()
}