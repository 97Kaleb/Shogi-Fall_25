package com.example.shogi

/*****************************************************************************************
 * The promoted knight, along with most of the other promoted pieces, moves in exactly the
 * same way the gold general does: one square in any orthogonal direction, or diagonally
 * forward. It is promoted from (you guessed it) a knight.
 ****************************************************************************************/
class PromotedKnight(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "成桂"
    override val canPromote: Boolean = false
    override val isPromoted: Boolean = true
    override fun promote(board: Board) {}
    override fun demote(board: Board): Piece
    {
        return Knight(pos, isWhite)
    }
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        var p1: Position = Position(pos.column, pos.row)
        var p2: Position = Position(pos.column, pos.row)
        var p3: Position = Position(pos.column, pos.row)
        var p4: Position = Position(pos.column, pos.row)
        var p5: Position = Position(pos.column, pos.row)
        var p6: Position = Position(pos.column, pos.row)
        if (this.isWhite) {
            p1.adjustRow(-1)
            p2.adjustCol(-1)
            p3.adjustCol(1)
            p4.adjustRow(1)
            p4.adjustCol(-1)
            p5.adjustRow(1)
            p6.adjustRow(1)
            p6.adjustCol(1)
        }
        else {
            p1.adjustRow(1)
            p2.adjustCol(-1)
            p3.adjustCol(1)
            p4.adjustRow(-1)
            p4.adjustCol(-1)
            p5.adjustRow(-1)
            p6.adjustRow(-1)
            p6.adjustCol(1)
        }
        var possibleSteps = listOf(p1, p2, p3, p4, p5, p6)
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