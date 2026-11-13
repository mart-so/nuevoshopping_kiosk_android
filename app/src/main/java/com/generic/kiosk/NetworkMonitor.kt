package com.generic.kiosk

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class NetworkMonitor(context: Context, private val onChanged: (Boolean) -> Unit) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { onChanged(true) }
        override fun onLost(network: Network) { onChanged(isConnected()) }
    }
    fun start() { manager.registerDefaultNetworkCallback(callback); onChanged(isConnected()) }
    fun stop() { runCatching { manager.unregisterNetworkCallback(callback) } }
    private fun isConnected(): Boolean = manager.activeNetwork?.let { manager.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } == true
}
