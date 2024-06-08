package dev.datlag.kast.common

import dev.datlag.kast.Device
import dev.datlag.kast.DeviceType

internal fun DeviceType.Companion.fromCapabilities(vararg capabilities: Device.Capability): DeviceType {
    if (capabilities.isEmpty()) {
        return DeviceType.UNKNOWN
    }

    val canPlayVideo = capabilities.any { it.canPlayVideo }
    val canPlayAudio = capabilities.any { it.canPlayAudio }
    return when {
        canPlayVideo -> DeviceType.TV
        canPlayAudio -> DeviceType.SPEAKER
        else -> DeviceType.UNKNOWN
    }
}

internal fun DeviceType.Companion.fromCapabilityValue(value: Int): DeviceType {
    return fromCapabilities(*Device.Capability.capabilities(value).toTypedArray())
}