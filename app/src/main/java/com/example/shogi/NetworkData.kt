package com.example.shogi

data class NetworkMove(
    val type: String,
   val fromRow: Int = -1,
   val fromCol: Int = -1,
   val toRow: Int,
   val toCol: Int,
   val pieceSymbol: String? = null,
   val promote: Boolean = false
)