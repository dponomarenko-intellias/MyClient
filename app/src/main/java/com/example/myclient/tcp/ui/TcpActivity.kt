package com.example.myclient.tcp.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myclient.databinding.ActivityTcpBinding
import com.example.myclient.tcp.TcpClient4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TcpActivity : AppCompatActivity() {

    private lateinit var tcpClient4: TcpClient4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityTcpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.isConnected.text = "Disconnected"
        binding.isConnected.setTextColor(Color.RED)

        binding.hostInput.setText("tcpbin.com")
        binding.portInput.setText("4242")
        binding.sendInput.setText("Hello TCP Client Test!")

        tcpClient4 = TcpClient4(
            binding.hostInput.text.toString(),
            binding.portInput.text.toString().toInt()
        )
        tcpClient4.addConnectedListener(object : TcpClient4.OnConnectedListener {
            override fun onConnected() {
                binding.isConnected.text = "Connected"
                binding.isConnected.setTextColor(Color.GREEN)
                lifecycleScope.launch(Dispatchers.IO) {
                    tcpClient4.observe()
                }
//                Thread {
//                    tcpClient4.receiveMessages()
//                }.start()
            }
        })
        tcpClient4.addDisconnectedListener(object : TcpClient4.OnDisconnectedListener {
            override fun onDisconnected(reason: String) {
                runOnUiThread {
                    binding.isConnected.text = "Disconnected"
                    binding.isConnected.setTextColor(Color.RED)
                    Toast.makeText(applicationContext, "Disconnected: $reason", Toast.LENGTH_LONG)
                        .show()
                }
            }
        })
        tcpClient4.addReceiveDataListener(object : TcpClient4.OnReceiveDataListener {
            override fun onReceive(data: String) {
                runOnUiThread {
                    binding.receivedMessage.text = data
                }
            }
        })

        //we create a TCPClient object and
//        tcpClient2 = TcpClient3(object : OnMessageReceived {
//            override fun messageReceived(message: String?) {
//                Log.v("MY_TEST", "messageReceived: $message")
//            }
//        })

        binding.connectButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
//                tcpClient.connect()
//                tcpClient2.run()
                tcpClient4.connect()
            }
        }

        binding.sendButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val message = binding.sendInput.text.toString()
//                tcpClient.sendMessage(message)
//                tcpClient2.sendMessage(message);
                tcpClient4.write(message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.receiveButton.setOnClickListener {
//            Thread {
//                tcpClient4.receiveMessages()
//            }.start()
            lifecycleScope.launch(Dispatchers.IO) {
                tcpClient4.observe()
            }
        }

        binding.disconnectButton.setOnClickListener {
            lifecycleScope.launch {
                tcpClient4.disconnect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            tcpClient4.disconnect()
        }
    }
}
