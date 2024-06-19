package dev.datlag.kast

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

actual object Kast {

    internal const val SERVICE_TYPE = "_googlecast._tcp.local."
    private var dns: JmDNS? = null

    actual val isSupported: Boolean = false
    actual val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.DISCONNECTED)

    private val _allAvailableDevices: MutableStateFlow<ImmutableSet<Device>> = MutableStateFlow(persistentSetOf())
    actual val allAvailableDevices: StateFlow<ImmutableSet<Device>> = _allAvailableDevices

    @JvmStatic
    @JvmOverloads
    fun startDiscovery(address: InetAddress? = null) = apply {
        if (dns == null) {
            _allAvailableDevices.update { persistentSetOf() }
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
    actual fun dispose() = apply {
        stopDiscovery()
    }

    @JvmStatic
    actual fun select(device: Device) = apply {
        // ToDo("Not yet implemented")
    }

    @JvmStatic
    actual fun unselect(reason: UnselectReason) = apply {
        // ToDo("Not yet implemented")
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
                    it.toMutableSet().apply {
                        add(newDevice)
                    }.toImmutableSet()
                }
            }
        }

        override fun serviceRemoved(event: ServiceEvent?) {
            if (event?.type == SERVICE_TYPE) {
                val deviceName: String? = event.info?.name ?: event.name

                deviceName?.let { oldDevice ->
                    _allAvailableDevices.update {
                        it.filterNot { d -> d.name == oldDevice }.toImmutableSet()
                    }
                }
            }
        }

        /**
         * Handled in [serviceAdded]
         */
        override fun serviceResolved(event: ServiceEvent?) { }
    }

    actual data object Android {
        @JvmStatic
        actual fun activeDiscovery() { }

        @JvmStatic
        actual fun passiveDiscovery() { }
    }
}