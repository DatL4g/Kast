package dev.datlag.kast

fun interface ConnectionStateListener {
    operator fun invoke(device: Device?, state: ConnectionState)
}