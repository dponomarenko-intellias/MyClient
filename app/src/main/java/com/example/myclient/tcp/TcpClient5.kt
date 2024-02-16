package com.example.myclient.tcp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

class TcpClient5(private val host: String, private val port: Int) {

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var output: OutputStream? = null

    fun connect() {
        try {
            socket = Socket(host, port)
            reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
            output = socket?.getOutputStream()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessage(message: String) {
        try {
            output?.write(message.toByteArray())
            output?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun receiveMessage(): String? {
        try {
            return reader?.readLine()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun disconnect() {
        try {
            output?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
