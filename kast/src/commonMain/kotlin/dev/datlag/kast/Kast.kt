package dev.datlag.kast

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.StateFlow

expect object Kast {

    val isSupported: Boolean
    val connectionState: StateFlow<ConnectionState>
    val allAvailableDevices: StateFlow<ImmutableSet<Device>>

    @JvmStatic
    fun dispose(): Kast

    @JvmStatic
    fun select(device: Device): Kast

    @JvmStatic
    fun unselect(reason: UnselectReason): Kast

    /**
     * Android specific options mapped for common module.
     */
    object Android {
        @JvmStatic
        fun activeDiscovery()

        @JvmStatic
        fun passiveDiscovery()
    }
}