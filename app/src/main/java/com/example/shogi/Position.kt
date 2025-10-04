package com.example.shogi

/*****************************************************************************************
 * This class is a convenient way to trak a piece's position or a position on the board.
 ****************************************************************************************/
data class Position(var column: Int, var row: Int)
{
//  getters
    fun isInvalid(): Boolean
    {
        if (this.row < 0 || this.row > 8 || this.column < 0 || this.column > 8) {return true}
        else {return false}
    }
    fun isValid(): Boolean {return !isInvalid()}

//  setters
    fun setValid() {column = 0; row = 0}
    fun setInvalid() {column = 9; row = 9}
    fun updateRow(r: Int) {row = r}
    fun updateCol(c: Int) {column = c}
    fun set(r: Int, c: Int) {row = r; column = c}
    fun adjustRow(r: Int) { if (row + r < 0 || row + r > 8) {setInvalid()} else row += r}
    fun adjustCol(c: Int) {if (column + c < 0 || column + c > 8) {setInvalid()} else column += c}

}