package com.example.myclient.tcp.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import timber.log.Timber
import java.net.InetAddress


private const val SERVICE_TYPE = "_cricut._tcp."

class TcpDiscovery(
    context: Context,
) {

    private var nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

//    private val _availableDevices = MutableStateFlow<Set<NsdServiceInfo>>(emptySet())

    private val availableDevicesListeners: MutableList<((List<NsdServiceInfo>) -> Unit)> = mutableListOf()

//    private val availableDevicesFlow: Flow<Set<NsdServiceInfo>>
//        get() = _availableDevices

    private val availableDevices: MutableList<NsdServiceInfo> = mutableListOf()

    private val discoveryListener =  object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Timber.d("TcpDiscovery: Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Timber.d("TcpDiscovery: Service discovery success: %s", service)
//            val devices =  _availableDevices.value.toMutableSet().apply { this.add(service) }
//            availableDevices.clear()
//            availableDevices.addAll(devices)
//            _availableDevices.value = devices
            availableDevices.add(service)
            availableDevicesListeners.forEach { it.invoke(availableDevices.toList()) }

            nsdManager.resolveService(service, resolveListener)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Timber.d("TcpDiscovery: service lost %s", service)

//            val list = _availableDevices.value.toMutableSet()
            val serviceToRemove = availableDevices.find { it.serviceName == service.serviceName }
            availableDevices.remove(serviceToRemove)
//            _availableDevices.value = list.apply { this.remove(serviceToRemove) }
            availableDevicesListeners.forEach { it.invoke(availableDevices.toList()) }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Timber.d("TcpDiscovery: Discovery stopped: %s", serviceType)
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.e("TcpDiscovery: Discovery failed: Error code: %s", errorCode)
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.e("TcpDiscovery: Discovery failed: Error code :%s", errorCode)
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Timber.e("TcpConnection: Resolve failed: Error code: %s", errorCode)
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Timber.d("TcpConnection: Resolve Succeeded. %s", serviceInfo)
            val service = availableDevices.find { it.serviceName == serviceInfo.serviceName }
            availableDevices.remove(service)
            availableDevices.add(serviceInfo)
            availableDevicesListeners.forEach { it.invoke(availableDevices.toList()) }
        }
    }

    suspend fun startDiscoverServices() {
        Timber.d("TcpDiscovery: Start Discovery localHost: " + InetAddress.getLocalHost())

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscoverServices() {
        Timber.d("TcpDiscovery: Stop Discovery")
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    fun addDeviceListener(listener: (List<NsdServiceInfo>) -> Unit) {
        availableDevicesListeners.add(listener)
        listener.invoke(availableDevices.toList())
    }

    fun removeDeviceListener(listener: (List<NsdServiceInfo>) -> Unit) {
        availableDevicesListeners.remove(listener)
    }

//    fun getService(name: String): NsdServiceInfo? {
//        return availableDevices.find { it.serviceName == name }
//    }
//    fun subscribeDevicesUpdates(): Flow<Set<NsdServiceInfo>> {
//        return availableDevicesFlow
//    }
}
