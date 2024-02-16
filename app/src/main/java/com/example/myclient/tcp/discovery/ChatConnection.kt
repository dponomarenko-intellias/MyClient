package com.example.myclient.tcp.discovery

import android.os.Bundle
import android.os.Handler
import android.os.Message
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


class ChatConnection(private val mUpdateHandler: Handler) {
    private val mChatServer: ChatServer = ChatServer(mUpdateHandler)
    private var mChatClient: ChatClient? = null
    private var mSocket: Socket? = null
    var localPort = -1

    fun tearDown() {
        mChatServer.tearDown()
        if (mChatClient != null) {
            mChatClient!!.tearDown()
        }
    }

    fun connectToServer(address: InetAddress, port: Int) {
        Timber.d("connectToServer: address = " + address.hostAddress + " port: " + port)
        mChatClient = ChatClient(address, port)
    }

    fun sendMessage(msg: String) {
        Thread {
            if (mChatClient != null) {
                mChatClient!!.sendMessage(msg)
            }
        }.start()
    }

    @Synchronized
    fun updateMessages(msg: String, local: Boolean) {
        var msg = msg
        Timber.e("Updating message: $msg")
        msg = if (local) {
            "me: $msg"
        } else {
            "them: $msg"
        }
        val messageBundle = Bundle()
        messageBundle.putString("msg", msg)
        val message = Message()
        message.data = messageBundle
        mUpdateHandler.sendMessage(message)
    }

    @set:Synchronized
    private var socket: Socket?
        private get() = mSocket
        private set(socket) {
            Timber.d("setSocket being called.")
            if (socket == null) {
                Timber.d("Setting a null socket.")
            }
            if (mSocket != null) {
                if (mSocket!!.isConnected) {
                    try {
                        mSocket!!.close()
                    } catch (e: IOException) {
                        // TODO(alexlucas): Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            }
            mSocket = socket
        }

    private inner class ChatServer(handler: Handler?) {
        var mServerSocket: ServerSocket? = null
        var mThread: Thread? = null

        init {
            mThread = Thread(ServerThread())
            mThread!!.start()
        }

        fun tearDown() {
            mThread!!.interrupt()
            try {
                mServerSocket!!.close()
            } catch (ioe: IOException) {
                Timber.e("Error when closing server socket.")
            }
        }

        internal inner class ServerThread : Runnable {
            override fun run() {
                try {
                    // Since discovery will happen via Nsd, we don't need to care which port is
                    // used.  Just grab an available one  and advertise it via Nsd.
                    mServerSocket = ServerSocket(0)
                    localPort = mServerSocket!!.localPort
                    while (!Thread.currentThread().isInterrupted) {
                        Timber.d("ServerSocket Created, awaiting connection")
                        socket = mServerSocket!!.accept()
                        Timber.d("Connected.")
                        if (mChatClient == null) {
                            val port = mSocket!!.port
                            val address = mSocket!!.inetAddress
                            connectToServer(address, port)
                        }
                    }
                } catch (e: IOException) {
                    Timber.e(e, "Error creating ServerSocket: ")
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class ChatClient(address: InetAddress, port: Int) {
        private val mAddress: InetAddress
        private val PORT: Int
        private val CLIENT_TAG = "ChatClient"
        private val mSendThread: Thread
        private var mRecThread: Thread? = null

        init {
            Timber.d("Creating chatClient")
            mAddress = address
            PORT = port
            mSendThread = Thread(SendingThread())
            mSendThread.start()
        }

        internal inner class SendingThread : Runnable {
            var mMessageQueue: BlockingQueue<String>
            private val QUEUE_CAPACITY = 10

            init {
                mMessageQueue = ArrayBlockingQueue(QUEUE_CAPACITY)
            }

            override fun run() {
                try {
                    if (socket == null) {
                        socket = Socket(mAddress, PORT)
                        Timber.d("Client-side socket initialized.")
                    } else {
                        Timber.d("Socket already initialized. skipping!")
                    }
                    mRecThread = Thread(ReceivingThread())
                    mRecThread!!.start()
                } catch (e: UnknownHostException) {
                    Timber.d(e, "Initializing socket failed, UHE")
                } catch (e: IOException) {
                    Timber.d(e, "Initializing socket failed, IOE.")
                }
                while (true) {
                    try {
                        val msg = mMessageQueue.take()
                        sendMessage(msg)
                    } catch (ie: InterruptedException) {
                        Timber.d("Message sending loop interrupted, exiting")
                    }
                }
            }
        }

        internal inner class ReceivingThread : Runnable {
            override fun run() {
                val input: BufferedReader
                try {
                    input = BufferedReader(
                        InputStreamReader(
                            mSocket!!.getInputStream()
                        )
                    )
                    while (!Thread.currentThread().isInterrupted) {
                        var messageStr: String? = null
                        messageStr = input.readLine()
                        if (messageStr != null) {
                            Timber.d("Read from the stream: $messageStr")
                            updateMessages(messageStr, false)
                        } else {
                            Timber.d("The nulls! The nulls!")
                            break
                        }
                    }
                    input.close()
                } catch (e: IOException) {
                    Timber.e(e, "Server loop error: ")
                }
            }
        }

        fun tearDown() {
            try {
                socket?.close()
            } catch (ioe: IOException) {
                Timber.e("Error when closing server socket.")
            }
        }

        fun sendMessage(msg: String) {
            try {
                val socket: Socket = mSocket!!
                if (socket == null) {
                    Timber.d("Socket is null, wtf?")
                } else if (socket.getOutputStream() == null) {
                    Timber.d("Socket output stream is null, wtf?")
                }
                val out = PrintWriter(
                    BufferedWriter(
                        OutputStreamWriter(mSocket!!.getOutputStream())
                    ), true
                )
                out.println(msg)
                out.flush()
                updateMessages(msg, true)
            } catch (e: UnknownHostException) {
                Timber.d(e, "Unknown Host")
            } catch (e: IOException) {
                Timber.d(e, "I/O Exception")
            } catch (e: Exception) {
                Timber.d(e, "Error3")
            }
            Timber.d("Client sent message: $msg")
        }
    }

    companion object {
        private const val TAG = "ChatConnection"
    }
}
