package com.example.shogi

import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class GameServer(
    private val onMoveReceived: (NetworkMove) -> Unit,
    private val onConnected: (Boolean) -> Unit // Boolean: true if I am White (Host), false if Black (Client)
) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var running = false
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // HOST A GAME
    fun startHost() {
        scope.launch {
            try {
                val serverSocket = ServerSocket(8888) // Port 8888
                socket = serverSocket.accept() // Wait for client
                setupStreams()
                withContext(Dispatchers.Main) { onConnected(true) } // Host is White
                listenForMoves()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // JOIN A GAME
    fun joinGame(ipAddress: String) {
        scope.launch {
            try {
                socket = Socket(ipAddress, 8888)
                setupStreams()
                withContext(Dispatchers.Main) { onConnected(false) } // Client is Black
                listenForMoves()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupStreams() {
        socket?.let {
            writer = PrintWriter(it.getOutputStream(), true)
        }
    }

    fun sendMove(move: NetworkMove) {
        scope.launch {
            try {
                val json = gson.toJson(move)
                writer?.println(json)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun listenForMoves() {
        running = true
        try {
            val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            while (running) {
                val line = reader.readLine() ?: break
                val move = gson.fromJson(line, NetworkMove::class.java)
                withContext(Dispatchers.Main) {
                    onMoveReceived(move)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        running = false
        socket?.close()
    }
}
