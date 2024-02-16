package com.example.myclient.tcp.discovery


import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.myclient.R
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket


class NsdChatActivity : Activity() {
    var mNsdHelper: NsdHelper? = null
    private var mStatusView: TextView? = null
    private lateinit var mUpdateHandler: Handler
    private var serverSocket: ServerSocket? = null
    private var reader: BufferedReader? = null
    var mConnection: ChatConnection? = null
    //    private TcpClient4 tcpClient4;
    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        mStatusView = findViewById<View>(R.id.status) as TextView
        val discoverButton = findViewById<View>(R.id.discover_btn) as Button
        discoverButton.setOnClickListener { v: View? ->
            clickDiscover(
                v
            )
        }
        mUpdateHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val chatLine = msg.data.getString("msg")
                addChatLine(chatLine)
            }
        }
        mConnection = ChatConnection(mUpdateHandler)
        mNsdHelper = NsdHelper(this)
        mNsdHelper!!.initializeNsd()
    }

    override fun onStop() {
        super.onStop()
        stopListenPort()
    }

    fun clickAdvertise(v: View?) {
        val port = mConnection!!.localPort
        // Register service
        if (port > -1) {
            mNsdHelper!!.registerService(port)
            Toast.makeText(applicationContext, "Register Service port: $port", Toast.LENGTH_LONG)
                .show()
            //            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            startListenPort(port)
        } else {
            Log.d(TAG, "ServerSocket isn't bound.")
        }
        //        tcpClient4 = new TcpClient4("", port, 10000, 3);
//        tcpClient4.addReceiveDataListener(data -> {
//            Timber.v("NSD: Data received: " + data);
//            Toast.makeText(getApplicationContext(), "NSD: Data received: " + data, Toast.LENGTH_LONG).show();
//        });
//
//            tcpClient4.observe();
    }

    private fun startListenPort(port: Int) {
        Timber.d("NSD: startListenPort = $port")
        try {
            val serverSocket = ServerSocket(port) // Listen on port 8080
            val clientSocket = serverSocket.accept() // Wait for connection
            reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            this.serverSocket = serverSocket
            Thread {
                while (true) {
                    try {
                        val receivedData = reader!!.readLine()
                        Timber.v("NSB: Data received: $receivedData")
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Data received: $receivedData",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: IOException) {
                        Timber.e(e)
                    }
                }
            }.start()
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    private fun stopListenPort() {
        try {
            if (serverSocket != null) {
                Timber.d("NSD: stopListenPort = " + serverSocket!!.localPort)
                serverSocket!!.close()
                serverSocket = null
            }
            if (reader != null) {
                reader!!.close()
                reader = null
            }
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    fun clickDiscover(v: View?) {
        mNsdHelper!!.discoverServices()
    }

    fun clickConnect(v: View?) {
        Timber.v("NsdChat: clickConnect")
        val service = mNsdHelper!!.chosenServiceInfo
        if (service != null) {
            Log.d(TAG, "Connecting.")
            //
            try {
                mConnection!!.connectToServer(service.host, service.port)
                //                mConnection.connectToServer(InetAddress.getByName("192.168.120.131"), 41859);
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }

//        } else {
//            Log.d(TAG, "No service to connect to!");
        }
    }

    fun clickSend(v: View?) {
        val messageView = findViewById<View>(R.id.chatInput) as EditText
        if (messageView != null) {
            val messageString = messageView.text.toString()
            if (!messageString.isEmpty()) {
                mConnection!!.sendMessage(messageString)
            }
            messageView.setText("")
        }
    }

    fun addChatLine(line: String?) {
        mStatusView!!.append(
            """
                
                $line
                """.trimIndent()
        )
    }

    override fun onPause() {
        if (mNsdHelper != null) {
            try {
                mNsdHelper!!.stopDiscovery()
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, e.message, e)
            }
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        //        if (mNsdHelper != null) {
//            mNsdHelper.discoverServices();
//        }
    }

    override fun onDestroy() {
        mNsdHelper!!.tearDown()
        mConnection!!.tearDown()
        super.onDestroy()
    }

    companion object {
        const val TAG = "NsdChat"
    }
}
