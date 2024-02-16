package com.example.myclient.tcp.discovery

import timber.log.Timber

class TcpConnection(
    private val host: String,
    private val port: Int,
) {

    private var tcpClient: TcpClient = TcpClient(host, port)

    init {
        tcpClient.addConnectedListener {
            Timber.d("TcpConnection, onConnected")
            Thread {
                tcpClient.receiveMessages()
            }.start()
        }
        tcpClient.addDisconnectedListener { reason ->
            Timber.d("TcpConnection, onDisconnected, reason: $reason")
        }
        tcpClient.addReceiveDataListener { data ->
            Timber.d("TcpConnection, onReceive, data: $data")
        }
    }

    suspend fun connect() {
        tcpClient.connect()
    }

    suspend fun disconnect() {
        if (tcpClient.isConnected()) {
            tcpClient.disconnect()
        }
    }
}
