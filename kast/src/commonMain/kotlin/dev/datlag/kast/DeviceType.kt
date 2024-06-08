package dev.datlag.kast

import kotlinx.serialization.Serializable

@Serializable
sealed interface DeviceType {

    @Serializable
    data object UNKNOWN : DeviceType

    @Serializable
    data object TV : DeviceType

    @Serializable
    data object SPEAKER : DeviceType

    companion object
}