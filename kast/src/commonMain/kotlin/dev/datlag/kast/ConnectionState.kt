package dev.datlag.kast

import kotlinx.serialization.Serializable

@Serializable
sealed interface ConnectionState {

    @Serializable
    data object DISCONNECTED : ConnectionState

    @Serializable
    data object CONNECTING : ConnectionState

    @Serializable
    data object CONNECTED : ConnectionState
}