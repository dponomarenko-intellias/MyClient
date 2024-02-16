package com.example.myclient.tcp.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import timber.log.Timber
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

class TcpClient(
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
//    listener: (List<NsdServiceInfo>) -> Unit
    private var connectedListeners: MutableList<() -> Unit> = mutableListOf()
    private var disconnectedListeners: MutableList<(String) -> Unit> = mutableListOf()
    private var receiveListeners: MutableList<(String) -> Unit> = mutableListOf()

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

    suspend fun disconnect(reason: String = "User disconnect") {
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

    fun isConnected(): Boolean {
        return connected
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
                if (socket!!.isConnected || socket!!.isClosed || socket!!.isInputShutdown) {
                    val receivedMessage = reader?.readLine()
                    Timber.d("TcpClient, receiveMessages: $receivedMessage")
                    if (receivedMessage != null) {
                        notifyDataReceived(receivedMessage)
                    }
                } else {
                    notifyDisconnected("Read error: Socket was closed")
                    break
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            notifyDisconnected("Read error: ${e.message}")
        }
    }


    suspend fun receiveMessages2() = withContext(Dispatchers.IO) {
        Timber.v("TcpClient: receiveMessages")
        val socket = socket
        try {
            while (true) {
                if (socket != null && (socket.isConnected || !socket.isClosed)) {
                    val receivedMessage = reader?.readLine()
                    Timber.d("TcpClient, receiveMessages: $receivedMessage")
                    //Received data is null means Socked was closed from outside.

                    if (receivedMessage != null) {
                        notifyDataReceived(receivedMessage)
                    } else {
                        disconnect("Read error: Socket was closed")
                        break
                    }
                } else {
                    disconnect()
                    break
                }
            }
        } catch (e: IOException) {
            disconnect("Read error: ${e.message}")
//            notifyDisconnected("Read error: ${e.message}")
        } catch (e: SocketException) {
//            notifyDisconnected("Read error: ${e.message}")
            disconnect("Read error: ${e.message}")
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

//            socket.
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
                        Timber.v("MY_TEST, received: $received")
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

    fun addConnectedListener(listener: () -> Unit) {
        connectedListeners.add(listener)
    }

    fun addDisconnectedListener(listener: (String) -> Unit) {
        disconnectedListeners.add(listener)
    }

    fun addReceiveDataListener(listener: (String) -> Unit) {
        receiveListeners.add(listener)
    }

    private fun notifyConnected() {
        connectedListeners.forEach { it.invoke() }
    }

    private fun notifyDisconnected(reason: String) {
        disconnectedListeners.forEach { it.invoke(reason) }
    }

    private fun notifyDataReceived(data: String) {
        receiveListeners.forEach { it.invoke(data) }
    }
}
