package dev.datlag.kast

import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

data object Kast {

    internal const val SERVICE_TYPE = "_googlecast._tcp.local."
    private var dns: JmDNS? = null
    private val devices: MutableList<Device> = mutableListOf()

    @JvmStatic
    @JvmOverloads
    fun startDiscovery(address: InetAddress? = null) = apply {
        if (dns == null) {
            devices.clear()
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
        devices.clear()
    }

    internal data object CastServiceListener : ServiceListener {
        override fun serviceAdded(event: ServiceEvent?) {
            val device = dns?.let { Device(it, event?.info?.name ?: event?.name) }
            device?.let { devices.add(it) }

            println(devices.map { it.name })
        }

        override fun serviceRemoved(event: ServiceEvent?) {
            if (event?.type == SERVICE_TYPE) {
                val deviceName: String? = event.info?.name ?: event.name
                if (deviceName != null) {
                    val removed = devices.removeAll { it.name == deviceName }
                }
            }
        }

        override fun serviceResolved(event: ServiceEvent?) { }
    }
}