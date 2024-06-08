package dev.datlag.kast

import androidx.mediarouter.media.MediaRouter

actual class Device(internal val route: MediaRouter.RouteInfo) {
    actual val name: String = route.name
    actual val type: DeviceType = when (route.deviceType) {
        MediaRouter.RouteInfo.DEVICE_TYPE_TV -> DeviceType.TV
        MediaRouter.RouteInfo.DEVICE_TYPE_BUILTIN_SPEAKER,
        MediaRouter.RouteInfo.DEVICE_TYPE_REMOTE_SPEAKER -> DeviceType.SPEAKER
        else -> DeviceType.UNKNOWN
    }
    actual val connectionState: ConnectionState = when (route.connectionState) {
        MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING -> ConnectionState.CONNECTING
        MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED -> ConnectionState.CONNECTED
        else -> ConnectionState.DISCONNECTED
    }
    actual val isSelected: Boolean = route.isSelected
}