package com.example.myclient.tcp.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import timber.log.Timber


class NsdHelper(var mContext: Context) {
    var mNsdManager: NsdManager
    var mResolveListener: NsdManager.ResolveListener? = null
    var mDiscoveryListener: DiscoveryListener? = null
    var mRegistrationListener: RegistrationListener? = null
    var mServiceName = "NsdChat"
    var chosenServiceInfo: NsdServiceInfo? = null

    init {
        mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    fun initializeNsd() {
        initializeResolveListener()
        initializeDiscoveryListener()
        initializeRegistrationListener()

        //mNsdManager.init(mContext.getMainLooper(), this);
    }

    fun initializeDiscoveryListener() {
        mDiscoveryListener = object : DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.tag(TAG).d("Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.tag(TAG).d("Service discovery success: %s", service)
                if (service.serviceType != SERVICE_TYPE) {
                    Timber.tag(TAG).d("Unknown Service Type: %s", service.serviceType)
                    //                } else if (service.getServiceName().equals(mServiceName)) {
//                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.serviceName.contains(mServiceName)) {
                    mNsdManager.resolveService(service, mResolveListener)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Timber.tag(TAG).e("service lost%s", service)
                if (chosenServiceInfo == service) {
                    chosenServiceInfo = null
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.tag(TAG).i("Discovery stopped: %s", serviceType)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.tag(TAG).e("Discovery failed: Error code:%s", errorCode)
                mNsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.tag(TAG).e("Discovery failed: Error code:%s", errorCode)
                mNsdManager.stopServiceDiscovery(this)
            }
        }
    }

    fun initializeResolveListener() {
        mResolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.tag(TAG).e("Resolve failed%s", errorCode)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Timber.tag(TAG).e("Resolve Succeeded. %s", serviceInfo)

//                if (serviceInfo.getServiceName().equals(mServiceName)) {
//                    Log.d(TAG, "Same IP.");
//                    return;
//                }
                chosenServiceInfo = serviceInfo
            }
        }
    }

    fun initializeRegistrationListener() {
        mRegistrationListener = object : RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                mServiceName = nsdServiceInfo.serviceName
                //                nsdServiceInfo.
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
    }

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo()
        serviceInfo.port = port
        serviceInfo.serviceName = mServiceName
        serviceInfo.serviceType = SERVICE_TYPE
        mNsdManager.registerService(
            serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener
        )
    }

    fun discoverServices() {
        mNsdManager.discoverServices(
            SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener
        )
    }

    fun stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener)
    }

    fun tearDown() {
        try {
            mNsdManager.unregisterService(mRegistrationListener)
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).e(e)
        }
    }

    companion object {
        const val SERVICE_TYPE = "_incredibles._tcp."
        const val TAG = "NsdHelper"
    }
}

