package com.example.shogi

/*****************************************************************************************
 * Bishops can silde diagonally, and are promoted into horses.
 ****************************************************************************************/
class Bishop(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "角行"
    override val canPromote: Boolean = true
    override val isPromoted: Boolean = false
    override fun promote(board: Board)
    {
        board.board[pos.row][pos.column] = Horse(pos, isWhite)
    }
    override fun demote(board: Board): Piece {return this}
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        slide(pos, board, moves, -1, -1)
        slide(pos, board, moves, -1, 1)
        slide(pos, board, moves, 1, -1)
        slide(pos, board, moves, 1, 1)
    }
}