package dev.datlag.kast

sealed interface DeviceType {
    data object UNKNOWN : DeviceType
    data object TV : DeviceType
    data object SPEAKER : DeviceType

    companion object
}