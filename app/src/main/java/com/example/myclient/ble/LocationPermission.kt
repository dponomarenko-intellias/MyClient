package com.example.myclient.ble

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

//private const val REQUEST_PERMISSION_BLE_SCAN = 101
//
//fun Activity.requestBlePermissions(permissions: Array<String>) =
//    ActivityCompat.requestPermissions(
//        this,
//        permissions,
//        REQUEST_PERMISSION_BLE_SCAN
//    )
//
//fun areBlePermissionsGranted(requestCode: Int, grantResults: IntArray) =
//    requestCode == REQUEST_PERMISSION_BLE_SCAN && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
//
//fun Activity.areBlePermissionsGranted(permissions: Array<String>) =
//    permissions.map { ContextCompat.checkSelfPermission(this, it) }
//        .all { it == PackageManager.PERMISSION_GRANTED }
