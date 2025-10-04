package com.example.shogi

/*****************************************************************************************
 * The pieces are the real meat of the game. This abstract class contains the basic logic
 * that allows all pieces to function.
 ****************************************************************************************/
abstract class Piece(var pos: Position, var isWhite: Boolean) {
    abstract val symbol: String
    abstract val canPromote: Boolean
    abstract val isPromoted: Boolean
    abstract fun promote(board: Board)
    abstract fun demote(board: Board): Piece
    fun getPosition(): Position {return pos}
    fun setPosition(position: Position) {this.pos = position}
    abstract fun getMoves(moves: MutableList<Move>, board: Board)

    /*****************************************************************************************
     * This function handles any continuous piece movement (like bishops' or lances').
     ****************************************************************************************/
    fun slide(pos: Position, board: Board, moves: MutableList<Move>, verticalSlide: Int, horizontalSlide: Int){
        var newPos = Position(pos.column, pos.row)
        var blocked: Boolean = false
        do {
            newPos.adjustCol(horizontalSlide)
            newPos.adjustRow(verticalSlide)
            if (newPos.isValid()) {
                if (board.board[newPos.row][newPos.column].isWhite != this.isWhite || board.board[newPos.row][newPos.column] is Empty) {
                    moves.add(Move(this.pos, Position(newPos.column, newPos.row), board.board[newPos.row][newPos.column]))
                    if (board.board[newPos.row][newPos.column] !is Empty) {
                        blocked = true
                    }
                } else {
                    blocked = true
                }
            }
        } while (newPos.isValid() && !blocked)
    }

    /*****************************************************************************************
     * This function executes a given Move object, modifying both the board and the piece so
     * that they both know where the piece is now.
     ****************************************************************************************/
    fun executeMove(move: Move, board: Board): Boolean {
        board.board[move.posTo.row][move.posTo.column].capture(isWhite, board)
        board.board[pos.row][pos.column] = Empty()
        board.board[move.posTo.row][move.posTo.column] = this
        pos = move.posTo
        board.numMoves++
        if (move.mustPromote(this)) {
            promote(board)
            return false
        }
        if (move.canPromote(this))
        {
            return true
        }
        return false
    }

    /*****************************************************************************************
     * This function allows a piece to be captured. I named it intrasnitively for some reason,
     * so make sure you realize that the piece that calls it is the piece being captured, not
     * the piece doing tha capturing.
     ****************************************************************************************/
    protected open fun capture(isWhite: Boolean, board: Board) {
        if (isWhite) { // The capturing player is white
            pos.setInvalid()
            val capturedPiece = this.demote(board)
            capturedPiece.isWhite = true // This piece will belong to the black player for dropping
            board.whiteCaptured.add(capturedPiece)
        } else { // The capturing player is black
            pos.setInvalid()
            val capturedPiece = this.demote(board)
            capturedPiece.isWhite = false // This piece will belong to the white player for dropping
            board.blackCaptured.add(capturedPiece)
        }
    }

    /*****************************************************************************************
     * Dropping is a little simpler than moving, as you don't have to worry about where the
     * piece is coming from.
     ****************************************************************************************/
    fun drop(drop: Drop, board: Board) {
        if (isWhite)
        {
            board.whiteCaptured.remove(this)
        }
        else
        {
            board.blackCaptured.remove(this)
        }
        board.board[drop.posTo.row][drop.posTo.column] = this
        pos = drop.posTo
        board.numMoves++
    }
}