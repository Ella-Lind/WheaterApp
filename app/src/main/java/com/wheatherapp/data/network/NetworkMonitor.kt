package com.wheatherapp.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Observes whether the device currently has a validated internet connection.
 */
class NetworkMonitor(
    context: Context? = null,
    private val testOnline: Boolean = false
) {
    private val connectivityManager =
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun observe(): Flow<Boolean> = callbackFlow {
        fun emitCurrentStatus() {
            trySend(isNetworkAvailable())
        }

        if (connectivityManager == null) {
            trySend(testOnline)
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                emitCurrentStatus()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }

            override fun onLost(network: Network) {
                emitCurrentStatus()
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        emitCurrentStatus()
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    fun isNetworkAvailable(): Boolean {
        val manager = connectivityManager ?: return testOnline
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
