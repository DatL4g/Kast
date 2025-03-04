package dev.datlag.kast

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmStatic

expect object Kast {

    val isSupported: Boolean

    @NativeCoroutinesState
    val connectionState: StateFlow<ConnectionState>

    @NativeCoroutinesState
    val allAvailableDevices: StateFlow<Collection<Device>>

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