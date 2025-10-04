package com.example.shogi

/*****************************************************************************************
 * Move contains the information that allows a piece to move. It also contains the logic that
 * determines if a piece can (or in the case of a pawn, knight, or lance that will no lnger have
 * legal moves, must) promote. Unlike in chess, promotion can take place when leaving  or passing
 * through the promotion zone as well as when entering it.
 ****************************************************************************************/
class Move(var posFrom: Position, var posTo: Position, var piece: Piece) {
    fun canPromote(movingPiece: Piece): Boolean {
        if (movingPiece.isWhite) {
            if (movingPiece.canPromote && (posTo.row == 6 || posTo.row == 7 || posTo.row == 8
                                        || posFrom.row == 6 || posFrom.row == 7 || posFrom.row == 8)) {
                return true
            }
        }
        else {
            if (movingPiece.canPromote && (posTo.row == 0 || posTo.row == 1 || posTo.row == 2
                                        || posFrom.row == 0 || posFrom.row == 1 || posFrom.row == 2)) {
                return true
            }
        }
        return false
    }

    fun mustPromote(movingPiece: Piece): Boolean {
        if ((movingPiece.isWhite && ((movingPiece is Pawn && posTo.row == 8) || (movingPiece is Lance &&
             posTo.row == 8) || (movingPiece is Knight && (posTo.row == 8 || posTo.row == 7))))
            || ((!movingPiece.isWhite) && ((movingPiece is Pawn && posTo.row == 0) || (movingPiece is Lance &&
             posTo.row == 0) || (movingPiece is Knight && (posTo.row == 0 || posTo.row == 1))))) {
            return true
        }
        return false
    }
}