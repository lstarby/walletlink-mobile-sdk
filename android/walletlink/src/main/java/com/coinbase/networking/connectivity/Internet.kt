package com.coinbase.networking.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Internet : BroadcastReceiver() {
    private val disposeBag = CompositeDisposable()
    private val serialScheduler = Schedulers.single()
    private var networkUpdatesSubject = PublishSubject.create<Context>()
    private val changes = BehaviorSubject.createDefault<ConnectionStatus>(ConnectionStatus.Unknown)

    /**
     * Get the current network status
     */
    var status: ConnectionStatus = ConnectionStatus.Unknown
        private set

    /**
     * Observer for new network status changes
     */
    val statusChanges = changes.hide()

    /**
     * Determine whether network is online or not
     */
    val isOnline: Boolean get() = status.isOnline

    /**
     * Start monitoring network changes
     */
    fun startMonitoring() {
        networkUpdatesSubject
            .observeOn(serialScheduler)
            .map { context ->
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = cm.activeNetwork
                val activeNetworkInfo = cm.activeNetworkInfo

                if (activeNetwork == null) {
                    changes.onNext(ConnectionStatus.Unknown)
                    return@map
                }

                status = if (activeNetworkInfo?.isConnected == true && isServerReachable()) {
                    val capabilities = cm.getNetworkCapabilities(activeNetwork)
                    val hasCell = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

                    when (Pair(hasCell, hasWifi)) {
                        Pair(first = true, second = false) -> ConnectionStatus.Connected(ConnectionKind.CELL)
                        Pair(first = false, second = true) -> ConnectionStatus.Connected(ConnectionKind.WIFI)
                        else -> ConnectionStatus.Connected(ConnectionKind.UNKNOWN)
                    }
                } else {
                    ConnectionStatus.Offline
                }

                changes.onNext(status)
            }
            .subscribe()
            .let { disposeBag.add(it) }

        // networkUpdatesSubject.onNext(appContext)
    }

    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring() {
        disposeBag.clear()
    }

    override fun onReceive(context: Context, intent: Intent) {
        Schedulers.io().scheduleDirect {
            if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                networkUpdatesSubject.onNext(context)
            }
        }
    }

    private fun isServerReachable(): Boolean = try {
        URL("https://google.com")
            .openConnection()
            .let { it as HttpURLConnection }
            .apply {
                setRequestProperty("Connection", "close")
                connectTimeout = 1500
                connect()
            }
            .responseCode == 200
    } catch (e: IOException) {
        false
    }
}
