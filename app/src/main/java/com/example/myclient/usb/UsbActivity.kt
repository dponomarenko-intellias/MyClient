package com.example.myclient.usb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myclient.databinding.ActivityUsbBinding

class UsbActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityUsbBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

