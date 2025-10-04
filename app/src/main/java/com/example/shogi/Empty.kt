package com.example.shogi

/*****************************************************************************************
 * An empty space needs to be a subclass of Piece too for the program to function correctly.
 ****************************************************************************************/
class Empty : Piece(Position(9, 9), false) {
    override val symbol: String = ""
    override val canPromote: Boolean = false
    override val isPromoted: Boolean = false
    override fun promote(board: Board) {}
    override fun demote(board: Board): Piece {return this}
    override fun getMoves(moves: MutableList<Move>, board: Board) {}
    override fun capture(isWhite: Boolean, board: Board) {}
}
