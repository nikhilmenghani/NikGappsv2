package com.nikgapps.v2

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ConnectionState { CHECKING, ONLINE, OFFLINE }

/** Tracks the active validated network and debounces loss without recreating navigation. */
internal class NetworkConnection(context: Context) : AutoCloseable {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var closed = false
    private val mutableState = MutableStateFlow(read())
    val state = mutableState.asStateFlow()
    private val recheck = Runnable { if (!closed) mutableState.value = read() }
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refresh()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refresh()
        override fun onLost(network: Network) = refresh()
    }
    private fun read(): ConnectionState {
        val caps = manager.getNetworkCapabilities(manager.activeNetwork)
        return if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) ConnectionState.ONLINE else ConnectionState.OFFLINE
    }
    private fun refresh() {
        handler.post {
            if (!closed) {
                handler.removeCallbacks(recheck)
                if (read() == ConnectionState.ONLINE) mutableState.value = ConnectionState.ONLINE
                else handler.postDelayed(recheck, 1500)
            }
        }
    }
    fun start() {
        if (!started && !closed) {
            manager.registerDefaultNetworkCallback(callback)
            started = true
            refresh()
        }
    }
    fun retry() {
        if (!closed) {
            mutableState.value = ConnectionState.CHECKING
            handler.removeCallbacks(recheck)
            handler.postDelayed(recheck, 150)
        }
    }
    override fun close() {
        if (closed) return
        closed = true
        handler.removeCallbacksAndMessages(null)
        if (started) { manager.unregisterNetworkCallback(callback); started = false }
    }
}
