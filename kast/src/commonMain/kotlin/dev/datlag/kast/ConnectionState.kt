package dev.datlag.kast

sealed interface ConnectionState {
    data object DISCONNECTED : ConnectionState
    data object CONNECTING : ConnectionState
    data object CONNECTED : ConnectionState
}