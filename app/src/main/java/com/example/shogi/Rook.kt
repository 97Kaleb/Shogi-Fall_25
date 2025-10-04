package com.example.shogi

/*****************************************************************************************
 * Rooks can silde orthogonally, and are promoted into dragons.
 ****************************************************************************************/
class Rook(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "飛車"
    override val canPromote: Boolean = true
    override val isPromoted: Boolean = false
    override fun promote(board: Board)
    {
        board.board[pos.row][pos.column] = Dragon(pos, isWhite)
    }
    override fun demote(board: Board): Piece {return this}
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        slide(pos, board, moves, 0, -1)
        slide(pos, board, moves, 0, 1)
        slide(pos, board, moves, -1, 0)
        slide(pos, board, moves, 1, 0)
    }
}