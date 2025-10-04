package com.example.shogi

/*****************************************************************************************
 * Dragons can silde orthogonally and move one square diagonally, and are promoted from rooks.
 ****************************************************************************************/
class Dragon(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "竜王"
    override val canPromote: Boolean = false
    override val isPromoted: Boolean = true
    override fun promote(board: Board) {}
    override fun demote(board: Board): Piece
    {
        return Rook(pos, isWhite)
    }
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        slide(pos, board, moves, 0, -1)
        slide(pos, board, moves, 0, 1)
        slide(pos, board, moves, -1, 0)
        slide(pos, board, moves, 1, 0)
        var p1: Position = Position(pos.column, pos.row)
        var p2: Position = Position(pos.column, pos.row)
        var p3: Position = Position(pos.column, pos.row)
        var p4: Position = Position(pos.column, pos.row)
        p1.adjustCol(-1)
        p1.adjustRow(-1)
        p2.adjustCol(-1)
        p2.adjustRow(1)
        p3.adjustCol(1)
        p3.adjustRow(-1)
        p4.adjustCol(1)
        p4.adjustRow(1)
        var possibleSteps = listOf(p1, p2, p3, p4)
        for (p in possibleSteps)
        {
            if (p.isValid())
            {
                if (board.board[p.row][p.column].isWhite != this.isWhite || board.board[p.row][p.column] is Empty)
                {
                    moves.add(Move(this.pos, p, board.board[p.row][p.column]))
                }
            }
        }
    }
}