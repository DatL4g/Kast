package dev.datlag.kast

import cocoapods.GoogleCast.*
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class Device(internal val route: GCKDevice, internal val state: GCKConnectionState) {

    actual val name: String = route.friendlyName?.ifBlank { null }
        ?: route.modelName?.ifBlank { null }
        ?: route.deviceID

    actual val type: DeviceType = when(route.type) {
        GCKDeviceTypeTV -> DeviceType.TV
        GCKDeviceTypeSpeaker, GCKDeviceTypeSpeakerGroup -> DeviceType.SPEAKER
        else -> DeviceType.UNKNOWN
    }

    actual val connectionState: ConnectionState = when (state) {
        GCKConnectionStateConnecting -> ConnectionState.CONNECTING
        GCKConnectionStateConnected -> ConnectionState.CONNECTED
        else -> ConnectionState.DISCONNECTED
    }

    actual val isSelected: Boolean = connectionState == ConnectionState.CONNECTED
}