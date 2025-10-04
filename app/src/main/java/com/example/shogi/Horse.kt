package com.example.shogi

/*****************************************************************************************
 * Horses can silde diagonally and move one space orthagonally, and are promoted from bishops.
 ****************************************************************************************/
class Horse(pos: Position, isWhite: Boolean) : Piece(pos, isWhite) {
    override val symbol: String = "竜馬"
    override val canPromote: Boolean = false
    override val isPromoted: Boolean = true
    override fun promote(board: Board) {}
    override fun demote(board: Board): Piece
    {
        return Bishop(pos, isWhite)
    }
    override fun getMoves(moves: MutableList<Move>, board: Board) {
        slide(pos, board, moves, -1, -1)
        slide(pos, board, moves, -1, 1)
        slide(pos, board, moves, 1, -1)
        slide(pos, board, moves, 1, 1)
        var p1: Position = Position(pos.column, pos.row)
        var p2: Position = Position(pos.column, pos.row)
        var p3: Position = Position(pos.column, pos.row)
        var p4: Position = Position(pos.column, pos.row)
        p1.adjustCol(-1)
        p2.adjustCol(1)
        p3.adjustRow(-1)
        p4.adjustRow(1)
        var possibleSteps = listOf(p1, p2, p3, p4)
        for (p in possibleSteps)
        {
            if (p.isValid()) {
                if (board.board[p.row][p.column].isWhite != this.isWhite || board.board[p.row][p.column] is Empty)
                {
                    moves.add(Move(this.pos, p, board.board[p.row][p.column]))
                }
            }
        }
    }
}