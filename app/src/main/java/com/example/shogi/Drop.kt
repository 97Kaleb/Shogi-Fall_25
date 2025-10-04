package com.example.shogi

/*****************************************************************************************
 * Contains the information necessary for a piece to be dropped. Unlike moving a piece,
 * a piece cannot promote off a drop, so there are no methods necessary in this class.
 ****************************************************************************************/
class Drop(var posTo: Position, var piece: Piece)