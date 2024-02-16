package com.example.myclient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myclient.ble.ui.BleActivity
import com.example.myclient.bluetooth.BluetoothActivity
import com.example.myclient.tcp.ui.TcpActivity
import com.example.myclient.tcp.ui.TcpDiscoveryActivity
import com.example.myclient.usb.UsbActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tcpClientButton = findViewById<Button>(R.id.tcpClientButton)
        tcpClientButton.setOnClickListener {
            startActivity(Intent(applicationContext, TcpActivity::class.java))
        }
        val bluetoothClientButton = findViewById<Button>(R.id.bluetoothClientButton)
        bluetoothClientButton.setOnClickListener {
            startActivity(Intent(applicationContext, BluetoothActivity::class.java))
        }
        val bleClientButton = findViewById<Button>(R.id.bleClientButton)
        bleClientButton.setOnClickListener {
            startActivity(Intent(applicationContext, BleActivity::class.java))
        }
        val usbClientButton = findViewById<Button>(R.id.usbClientButton)
        usbClientButton.setOnClickListener {
            startActivity(Intent(applicationContext, UsbActivity::class.java))
        }
    }
}
