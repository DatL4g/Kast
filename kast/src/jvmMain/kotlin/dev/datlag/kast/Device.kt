package dev.datlag.kast

import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

actual data class Device(
    private val dns: JmDNS,
    private val serviceName: String?
) {
    private val serviceInfo: ServiceInfo? = dns.getServiceInfo(Kast.SERVICE_TYPE, serviceName)
    actual val name: String = serviceInfo?.getPropertyString("fn")?.ifBlank { null } ?: serviceName ?: "Unknown"
    actual val type: DeviceType = DeviceType.UNKNOWN
    actual val connectionState: ConnectionState = ConnectionState.DISCONNECTED
}
