package com.example.myclient.ble.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothDevice.BOND_NONE
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES
import android.bluetooth.le.ScanSettings.CALLBACK_TYPE_FIRST_MATCH
import android.bluetooth.le.ScanSettings.CALLBACK_TYPE_MATCH_LOST
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myclient.databinding.ActivityBleBinding
import timber.log.Timber
import java.nio.charset.Charset
import java.util.Locale


private const val REQUEST_PERMISSIONS_ALL = 99
private const val REQUEST_PERMISSION_BLE_SCAN = 101
private const val REQUEST_PERMISSION_BLUETOOTH_ADVERTISE = 102
private const val REQUEST_ENABLE_BT = 201
private const val REQUEST_ENABLE_BT_SCAN = 202

class BleActivity : AppCompatActivity() {

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    @SuppressLint("MissingPermission")
    private val adapter = BleDevicesAdapter(connectClickListener = {
//        val device = it.device
        val bleHandler = Handler(Looper.getMainLooper())
        val bluetoothAdapter =
            (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = bluetoothAdapter.getRemoteDevice(it.address)
        val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
            // Implement callback methods for GATT events
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                Toast.makeText(
                    applicationContext,
                    "onConnectionStateChange status = $status, gatt: " + gatt.toString(),
                    Toast.LENGTH_LONG
                ).show()
                if (status == GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        val bondstate = device.bondState
                        if (bondstate == BOND_NONE || bondstate == BOND_BONDED) {
                            var discoverServicesRunnable = Runnable {
                                Timber.d(
                                    String.format(
                                        Locale.ENGLISH,
                                        "discovering services of '%s' with delay of %d ms",
                                        "Name",
                                        0
                                    )
                                );
                                val result = gatt?.discoverServices()
                                if (result == false) {
                                    Timber.e("discoverServices failed to start");
                                }
//                                this.discoverServicesRunnable = null
                            }
                            bleHandler.post(discoverServicesRunnable)
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            gatt?.close()
                        }
                    } else {
                        gatt?.close()
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, value, status)
                Timber.v("BLE onCharacteristicRead")
//                Base64.getDecoder()?.decode(value)
//                decode(value)
            }
        }


//        // Get device object for a mac address
//        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(peripheralAddress)
//        // Check if the peripheral is cached or not
        val deviceType = device.type
        if (deviceType != BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            val bluetoothGatt = device.connectGatt(this@BleActivity, false, gattCallback, TRANSPORT_LE)
//            bluetoothGatt.readCharacteristic()
            Timber.v("BLE connected gatt: " + bluetoothGatt.toString())
        }
    })

    //    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(this@BleActivity, permissions, REQUEST_PERMISSIONS_ALL)

        binding.scanButton.setOnClickListener {
            Timber.d("BLE: scanButton clicked")
            if (ActivityCompat.checkSelfPermission(
                    this@BleActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.d("BLE permissions: request Manifest.permission.BLUETOOTH_SCAN")
                ActivityCompat.requestPermissions(
                    this@BleActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                    REQUEST_PERMISSION_BLE_SCAN
                )
            } else {
                val bluetoothManager =
                    getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter

                if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_SCAN)
                } else {
                    // Bluetooth is already enabled, you can perform Bluetooth operations here
                    bluetoothScan()
                }
            }
        }
        binding.advertiseButton.setOnClickListener {
            Timber.d("BLE: advertiseButton clicked")

            if (ActivityCompat.checkSelfPermission(
                    this@BleActivity,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.d("BLE permissions: request Manifest.permission.BLUETOOTH_ADVERTISE")
                ActivityCompat.requestPermissions(
                    this@BleActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE),
                    REQUEST_PERMISSION_BLUETOOTH_ADVERTISE
                )
            } else {
                bleAdvertise()
            }
        }
        binding.stopAdvertiseButton.setOnClickListener {
            stopAdvertising()
//            startActivity(Intent(applicationContext, DeviceControlActivity::class.java))
        }
//        binding.connectButton.setOnClickListener {
//            startActivity(Intent(applicationContext, DeviceScanActivity::class.java))
//        }
        binding.devicesList.layoutManager = LinearLayoutManager(applicationContext)
        binding.devicesList.adapter = adapter
    }

    override fun onStop() {
        super.onStop()
        stopAdvertising()
        stopScan()
    }

    @SuppressLint("MissingPermission")
    private fun bleAdvertise() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            startAdvertising()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val parameters = AdvertisingSetParameters.Builder()
//            .setLegacyMode(true) // True by default, but set here as a reminder.
            .setConnectable(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()

        val readUUID = "49535343-1e4d-4bd9-ba61-23c647249616"
        val pUuid = ParcelUuid.fromString(readUUID)

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
//            .addServiceUuid(ParcelUuid(GattService.MyServiceProfile.MY_SERVICE_UUID))
            .addServiceUuid(pUuid)
            .addServiceData( pUuid, "TestData".toByteArray(Charset.forName("UTF-8") ) )
            .build()

        val callback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet,
                txPower: Int,
                status: Int
            ) {
                Timber.v("onAdvertisingSetStarted(): txPower: $txPower, status: $status")
                // Keep track of the advertising set.
//                currentAdvertisingSet = advertisingSet
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
                Timber.v("onAdvertisingDataSet(): status: $status")
                // Advertising data has been set.


            } // Other callback methods...
        }

//        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        bluetoothLeAdvertiser?.startAdvertisingSet(parameters, data, null, null, null, callback);
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            // Advertising started successfully
            Toast.makeText(applicationContext, "onStartSuccess settingsInEffect = " + settingsInEffect.toString(), Toast.LENGTH_LONG).show()
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            // Advertising failed
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }

    var scanCallback: ScanCallback? = null

    val availableDevices = mutableSetOf<BluetoothDevice>()

    @SuppressLint("MissingPermission")
    private fun bluetoothScan() {
        Timber.d("BLE bluetoothScan")

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothAdapter = bluetoothManager.adapter

//        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//        } else {
//            // Bluetooth is already enabled, you can perform Bluetooth operations here
//        }

        val bluetoothAdapter = bluetoothManager.adapter
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                // Handle scanned device
                Timber.d("BLE SCAN onScanResult: callbackType = " + callbackType + ", result = " + result.toString())
                if (callbackType == CALLBACK_TYPE_FIRST_MATCH || callbackType == CALLBACK_TYPE_ALL_MATCHES) {
                    bluetoothLeScanner.stopScan(this)

                    result?.let {
//                    val device = it.device
//                    device.name
//                    val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
//                        // Implement callback methods for GATT events
//                    }
//                    val bluetoothGatt = device.connectGatt(this@BleActivity, false, gattCallback)
                        availableDevices.add(it.device)
                        Toast.makeText(
                            applicationContext,
                            "Found device: " + it.device.name,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (callbackType == CALLBACK_TYPE_MATCH_LOST) {
                    result?.let {
                        availableDevices.remove(it.device)
                        Toast.makeText(
                            applicationContext,
                            "Disappeared device: " + it.device.name,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                adapter.submitList(availableDevices.map { device ->
//                        device.type
                    BleDeviceItem(
                        address = device.address,
                        device.name ?: "null",
                        "Address:" + device.address + " Alias: " + (device.alias
                            ?: "") + " class: " + device.bluetoothClass.toString()
                                + "\ntype: " + deviceTypeToString(device.type),
                    )
                })
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                Timber.d("BLE SCAN onBatchScanResults: results size  = " + results?.size)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Timber.d("BLE SCAN onScanFailed errorCode = $errorCode")
            }
        }
        bluetoothLeScanner.startScan(scanCallback)
    }

    private fun deviceTypeToString(type: Int): String {
        return when (type) {
            1 -> "Classic"
            2 -> "Low Energy"
            3 -> "Dual"
            else -> "Unknown"
        }

//        val DEVICE_TYPE_UNKNOWN = 0
//        val DEVICE_TYPE_CLASSIC = 1
//        val DEVICE_TYPE_LE = 2
//        val DEVICE_TYPE_DUAL = 3
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        Timber.d("BLE stopScan")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        scanCallback?.let {
            bluetoothLeScanner?.stopScan(it)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_ALL
            && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            Timber.d("BLE all permissions granted")
        } else if (requestCode == REQUEST_PERMISSION_BLE_SCAN
            && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            Timber.d("BLE Permission android.permission.BLUETOOTH_SCAN is granted!")
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_SCAN)
            } else {
                // Bluetooth is already enabled, you can perform Bluetooth operations here
                bluetoothScan()
            }
        } else if (requestCode == REQUEST_PERMISSION_BLUETOOTH_ADVERTISE
            && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            Timber.d("BLE Permission android.permission.BLUETOOTH_ADVERTISE is granted!")
            bleAdvertise()
        }

//        else if (requestCode == REQUEST_ENABLE_BT
//            && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
//        ) {
//            startAdvertising()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            startAdvertising()
        } else if (requestCode == REQUEST_ENABLE_BT_SCAN && resultCode == RESULT_OK) {
            bluetoothScan()
        }
    }

    private fun encode(byte: ByteArray): String {
        return android.util.Base64.encodeToString(byte, android.util.Base64.DEFAULT).trim()
    }

    private fun decode(string: String): ByteArray {
        return android.util.Base64.decode(string, android.util.Base64.DEFAULT)
    }
}
