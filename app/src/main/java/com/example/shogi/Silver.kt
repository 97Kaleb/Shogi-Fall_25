package com.example.shogi

/*****************************************************************************************
 * Silver generals can move diagonally in any direction, or forward. They promote into
 * promoted silver generals, which instead move orthogonally in any direction or diagonally
 * forward.
 ****************************************************************************************/
class Silver(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "銀将"
    override val canPromote: Boolean = true
    override val isPromoted: Boolean = false
    override fun promote(board: Board)
    {
        board.board[pos.row][pos.column] = PromotedSilver(pos, isWhite)
    }
    override fun demote(board: Board): Piece {return this}
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        var p1: Position = Position(pos.column, pos.row)
        var p2: Position = Position(pos.column, pos.row)
        var p3: Position = Position(pos.column, pos.row)
        var p4: Position = Position(pos.column, pos.row)
        var p5: Position = Position(pos.column, pos.row)
        if (this.isWhite) {
            p1.adjustRow(-1)
            p1.adjustCol(-1)
            p2.adjustRow(-1)
            p2.adjustCol(1)
            p3.adjustRow(1)
            p3.adjustCol(-1)
            p4.adjustRow(1)
            p5.adjustRow(1)
            p5.adjustCol(1)
        }
        else {
            p1.adjustRow(1)
            p1.adjustCol(-1)
            p2.adjustRow(1)
            p2.adjustCol(1)
            p3.adjustRow(-1)
            p3.adjustCol(-1)
            p4.adjustRow(-1)
            p5.adjustRow(-1)
            p5.adjustCol(1)
        }
        var possibleSteps = listOf(p1, p2, p3, p4, p5)
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