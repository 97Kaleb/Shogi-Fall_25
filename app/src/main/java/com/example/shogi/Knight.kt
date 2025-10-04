package com.example.shogi

/*****************************************************************************************
 * Knights can either jump forward two spaces and one to the left, or forward two spaces and one to the right.
 * They are promoted into promoted knights (which I wish had a better English name.)
 ****************************************************************************************/
class Knight(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "桂馬"
    override val canPromote: Boolean = true
    override val isPromoted: Boolean = false
    override fun promote(board: Board)
    {
        board.board[pos.row][pos.column] = PromotedKnight(pos, isWhite)
    }
    override fun demote(board: Board): Piece {return this}
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        var p1: Position = Position(pos.column, pos.row)
        var p2: Position = Position(pos.column, pos.row)
        if (this.isWhite) {
            p1.adjustRow(2)
            p1.adjustCol(-1)
            p2.adjustRow(2)
            p2.adjustCol(1)
        }
        else {
            p1.adjustRow(-2)
            p1.adjustCol(-1)
            p2.adjustRow(-2)
            p2.adjustCol(1)
        }
        var possibleSteps = listOf(p1, p2)
        for (p in possibleSteps) {
            if (p.isValid()) {
                if (board.board[p.row][p.column].isWhite != this.isWhite || board.board[p.row][p.column] is Empty)
                {
                    moves.add(Move(this.pos, p, board.board[p.row][p.column]))
                }
            }
        }
    }
}