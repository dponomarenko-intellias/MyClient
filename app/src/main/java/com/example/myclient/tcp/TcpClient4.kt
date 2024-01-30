package com.example.myclient.tcp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

class TcpClient4(
    private val host: String,
    private val port: Int,
    private val timeout: Long = 10000L,
    private val retryCount: Int = 3
) {

    private var socket: Socket? = null
    private var connected = false
    private var readJob: Job? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    // Callback interfaces for connection events
    interface OnConnectedListener {
        fun onConnected()
    }

    interface OnDisconnectedListener {
        fun onDisconnected(reason: String)
    }

    interface OnReceiveDataListener {
        fun onReceive(data: String)
    }

    private var connectedListeners: MutableList<OnConnectedListener> = mutableListOf()
    private var disconnectedListeners: MutableList<OnDisconnectedListener> = mutableListOf()
    private var receiveListeners: MutableList<OnReceiveDataListener> = mutableListOf()

    suspend fun connect(): Boolean {
        if (socket?.isConnected == true) return true

        for (i in 0 until retryCount) {
            try {
                val address = InetAddress.getByName(host)
                val socketAddress = InetSocketAddress(address, port)
                val socket = Socket()
                socket.connect(socketAddress, timeout.toInt())
                writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                this.socket = socket
                connected = true
                withContext(Dispatchers.Main) {
                    notifyConnected()
                }
                break
            } catch (e: SocketException) {
                if (i == retryCount - 1) throw e
                continue
            } catch (e: IOException) {
                throw e
            }
        }

        return connected
    }

    suspend fun disconnect() {
        if (!connected) return

        readJob?.cancelAndJoin()
        try {
            socket?.close()
            socket = null
            connected = false

            writer?.flush()
            writer?.close()
            writer = null
            reader?.close()
            reader = null
            notifyDisconnected("User disconnect")
        } catch (e: IOException) {
            notifyDisconnected("Disconnect error: ${e.message}")
            throw e
        }
    }

//    suspend fun read(): String = suspendCoroutine { continuation ->
//        if (!connected) {
//            continuation.resumeWithException(IOException("Not connected"))
//            return@suspendCoroutine
//        }
//
//        val inputStream = socket!!.inputStream
//        val buffer = ByteArray(1024)
//        val bytesRead = inputStream.read(buffer)
//        if (bytesRead == -1) {
//            notifyDisconnected("Server closed connection")
//            continuation.resumeWithException(IOException("Server closed connection"))
//        } else {
//            continuation.resume(String(buffer, 0, bytesRead))
//        }
//    }

    fun receiveMessages() {
        try {
            while (true) {
                val receivedMessage = reader?.readLine()
                Log.v("MY_TEST", "receiveMessages: $receivedMessage")
                if (receivedMessage != null) {
                    notifyDataReceived(receivedMessage)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            notifyDisconnected("Read error: ${e.message}")
        }
    }

    suspend fun observe() {
        withContext(Dispatchers.IO) {
            if (!connected) {
//                currentCoroutineContext().cancel()
//                cancel("Not connected", IllegalStateException("Not connected"))
                return@withContext
            }

            val inputStream = socket!!.inputStream
            val buffer = ByteArray(1024)

            while (true) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) {
                    cancel("Server closed connection", IOException("Server closed connection"))
                        withContext(Dispatchers.Main) {
                            notifyDisconnected("Server closed connection")
                        }
                    break
                    } else {
                        val received = String(buffer, 0, bytesRead)
                        Log.v("MY_TEST", "received = " + received)
                        withContext(Dispatchers.Main) {
                            notifyDataReceived(received)
                        }
                    }
                } catch (e: IOException) {
//                    cancel("Read error: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        notifyDisconnected("Read error: ${e.message}")
                    }
                    break
                }
            }
        }
    }

    suspend fun write(data: String) {
        if (!connected) throw IOException("Not connected")

        if (writer?.checkError() == false) {
            writer?.println(data)
            writer?.flush()
        }
    }

    fun addConnectedListener(listener: OnConnectedListener) {
        connectedListeners.add(listener)
    }

    fun addDisconnectedListener(listener: OnDisconnectedListener) {
        disconnectedListeners.add(listener)
    }

    fun addReceiveDataListener(listener: OnReceiveDataListener) {
        receiveListeners.add(listener)
    }

    private fun notifyConnected() {
        connectedListeners.forEach { it.onConnected() }
    }

    private fun notifyDisconnected(reason: String) {
        disconnectedListeners.forEach { it.onDisconnected(reason) }
    }

    private fun notifyDataReceived(data: String) {
        receiveListeners.forEach { it.onReceive(data) }
    }
}
