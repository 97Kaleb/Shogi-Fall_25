package com.example.shogi

/*****************************************************************************************
 * Lances slide forward. That's it. They promote into promoted lances, though in Japanese,
 * their name means "incense cart", which I think is much cooler. Alas, English.
 ****************************************************************************************/
class Lance(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "香車"
    override val canPromote: Boolean = true
    override val isPromoted: Boolean = false
    override fun promote(board: Board)
    {
        board.board[pos.row][pos.column] = PromotedLance(pos, isWhite)
    }
    override fun demote(board: Board): Piece {return this}
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        if (this.isWhite) {
            slide(pos, board, moves, 1, 0)
        }
        else {
            slide(pos, board, moves, -1, 0)
        }
    }
}