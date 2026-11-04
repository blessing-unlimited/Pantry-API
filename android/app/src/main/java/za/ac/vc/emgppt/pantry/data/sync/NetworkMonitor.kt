package za.ac.vc.emgppt.pantry.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the device has a network that can carry the API sync.
 * A callback replays the outbox when connectivity returns (Android Developers, 2024).
 */
class NetworkMonitor(context: Context) {
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _online = MutableStateFlow(readOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    fun isOnline(): Boolean = _online.value

    fun start(onAvailable: () -> Unit) {
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _online.value = true
                Log.i("Pantry", "Network available; sync will run")
                onAvailable()
            }

            override fun onLost(network: Network) {
                _online.value = readOnline()
                Log.i("Pantry", "Network lost; offline=${!_online.value}")
            }
        })
    }

    private fun readOnline(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
