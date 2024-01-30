package com.example.myclient.bluetooth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myclient.databinding.ActivityBluetoothBinding

class BluetoothActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
