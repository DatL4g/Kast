package dev.datlag.kast

expect class Device {
    val name: String
    val type: DeviceType
    val connectionState: ConnectionState
    val isSelected: Boolean
}
