package com.example.shogi

/*****************************************************************************************
 * Pawns can only move forward, and they promote into tokins. Not sure why they get to
 * keep their Japanese name when everything else is translated, or even just described.
 ****************************************************************************************/
class Pawn(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "歩兵"
    override val canPromote: Boolean = true
    override val isPromoted: Boolean = false
    override fun promote(board: Board)
    {
        board.board[pos.row][pos.column] = Tokin(pos, isWhite)
    }
    override fun demote(board: Board): Piece {return this}
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        var p1: Position = Position(pos.column, pos.row)
        if (this.isWhite) {
            p1.adjustRow(1)
        }
        else {
            p1.adjustRow(-1)
        }
        if (p1.isValid()) {
            if (board.board[p1.row][p1.column].isWhite != this.isWhite || board.board[p1.row][p1.column] is Empty) {
                moves.add(Move(this.pos, p1, board.board[p1.row][p1.column]))
            }
        }
    }
}