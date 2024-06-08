package dev.datlag.kast

import dev.datlag.kast.common.fromCapabilityValue
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlin.shl

actual data class Device(
    private val dns: JmDNS,
    private val serviceName: String?
) {
    private val serviceInfo: ServiceInfo? = dns.getServiceInfo(Kast.SERVICE_TYPE, serviceName)

    actual val name: String = serviceInfo?.getPropertyString("fn")?.ifBlank {
        null
    } ?: serviceName ?: "Unknown"

    actual val type: DeviceType = serviceInfo?.getPropertyString("ca")?.ifBlank {
        null
    }?.trim()?.toIntOrNull()?.let {
        DeviceType.fromCapabilityValue(it)
    } ?: DeviceType.UNKNOWN

    actual val connectionState: ConnectionState = ConnectionState.DISCONNECTED
    actual val isSelected: Boolean = connectionState == ConnectionState.CONNECTED

    internal sealed interface Capability {
        val mask: Int

        val canPlayVideo: Boolean
            get() = false

        val canPlayAudio: Boolean
            get() = false

        fun isIn(value: Int): Boolean {
            return (value and mask) == mask
        }

        /** The cast device has no known capabilities  */
        data object None : Capability {
            override val mask: Int = 0
        }

        sealed interface Video : Capability {
            /** The cast device can provide video media  */
            data object In : Video {
                override val mask: Int = 1 shl 1
            }

            /** The cast device can play video media  */
            data object Out : Video {
                override val mask: Int = 1
                override val canPlayVideo: Boolean = true
            }
        }

        sealed interface Audio : Capability {
            /** The cast device can provide audio media  */
            data object In : Audio {
                override val mask: Int = 1 shl  3
            }

            /** The cast device can play audio media  */
            data object Out : Audio {
                override val mask: Int = 1 shl 4
                override val canPlayAudio: Boolean = true
            }
        }

        companion object {
            val allExisting = setOf(
                Video.In,
                Video.Out,
                Audio.In,
                Audio.Out
            )

            fun capabilities(value: Int): Set<Capability> {
                if (value <= 0) {
                    return setOf(Capability.None)
                }

                val result = mutableSetOf<Capability>()
                for (c in allExisting) {
                    if (c == None) {
                        continue
                    }
                    if ((c.mask and value) == c.mask) {
                        result.add(c)
                    }
                }
                return result
            }
        }
    }
}
