package dev.datlag.kast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

data object Kast {

    internal const val SERVICE_TYPE = "_googlecast._tcp.local."
    private var dns: JmDNS? = null

    private val _allAvailableDevices: MutableStateFlow<List<Device>> = MutableStateFlow(emptyList())
    val allAvailableDevices: StateFlow<List<Device>> = _allAvailableDevices

    @JvmStatic
    @JvmOverloads
    fun startDiscovery(address: InetAddress? = null) = apply {
        if (dns == null) {
            _allAvailableDevices.update { emptyList() }
            dns = if (address == null) {
                JmDNS.create()
            } else {
                JmDNS.create(address)
            }
            dns?.addServiceListener(SERVICE_TYPE, CastServiceListener)
        }
    }

    @JvmStatic
    fun stopDiscovery() = apply {
        dns?.unregisterAllServices()
        dns?.close()
        dns = null
    }

    @JvmStatic
    @JvmOverloads
    fun restartDiscovery(address: InetAddress? = null) = apply {
        stopDiscovery()
        startDiscovery(address)
    }

    @JvmStatic
    fun dispose() = apply {
        stopDiscovery()
    }

    internal data object CastServiceListener : ServiceListener {
        override fun serviceAdded(event: ServiceEvent?) {
            val device = dns?.let {
                Device(
                    dns = it,
                    serviceName = event?.info?.name ?: event?.name
                )
            }

            device?.let { newDevice ->
                _allAvailableDevices.update {
                    listOf(*it.toTypedArray(), newDevice)
                }
            }
        }

        override fun serviceRemoved(event: ServiceEvent?) {
            if (event?.type == SERVICE_TYPE) {
                val deviceName: String? = event.info?.name ?: event.name

                deviceName?.let {
                    _allAvailableDevices.update { list ->
                        list.filterNot {
                            it.name == deviceName
                        }
                    }
                }
            }
        }

        override fun serviceResolved(event: ServiceEvent?) { }
    }
}