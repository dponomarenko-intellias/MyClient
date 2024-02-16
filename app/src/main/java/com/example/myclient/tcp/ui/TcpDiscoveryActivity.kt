package com.example.myclient.tcp.ui

import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myclient.databinding.ActivityTcpDiscoveryBinding
import com.example.myclient.tcp.discovery.TcpConnection
import com.example.myclient.tcp.discovery.TcpDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class TcpDiscoveryActivity : AppCompatActivity() {

    private lateinit var tcpDiscovery: TcpDiscovery
    private lateinit var tcpConnection: TcpConnection

    private val adapter = TcpDiscoveryListAdapter(connectClickListener = {
        Timber.v("connectClickListener item = $it")
//        val service = tcpDiscovery.getService(it.name)
        if (it.host != null && it.port > 0) {
            tcpConnection = TcpConnection(it.host, it.port)
            lifecycleScope.launch(Dispatchers.IO) {
                tcpConnection.connect()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "Service: " + it.name + " is not available",
                Toast.LENGTH_LONG
            ).show()
        }
    })

    private val deviceListener: (List<NsdServiceInfo>) -> Unit = {
        adapter.submitList(it.map { device ->
            DiscoveryItem(
                name = device.serviceName,
                host = device.host?.hostAddress,
                port = device.port,
                type = device.serviceType,
            )
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityTcpDiscoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.devicesList.layoutManager = LinearLayoutManager(applicationContext)
        binding.devicesList.adapter = adapter

        tcpDiscovery = TcpDiscovery(applicationContext)
//        tcpConnection = TcpConnection(applicationContext)
//        lifecycleScope.launch {
//            tcpDiscovery.availableDevicesFlow.collect {
//                adapter.submitList(it.map { device ->
//                    DiscoveryItem(
//                        name = device.serviceName,
//                        host = device.host?.hostAddress ?: "null",
//                        port = device.port,
//                        type = device.serviceType,
//                    )
//                })
//            }
//        }

    }

    override fun onStart() {
        super.onStart()
        tcpDiscovery.addDeviceListener(deviceListener)
        lifecycleScope.launch(Dispatchers.IO) {
            tcpDiscovery.startDiscoverServices()
        }
    }

    override fun onStop() {
        super.onStop()
        tcpDiscovery.removeDeviceListener(deviceListener)
        tcpDiscovery.stopDiscoverServices()
        if (this.isFinishing) {
            lifecycleScope.launch {
                tcpConnection.disconnect()
            }
        }
    }
}
