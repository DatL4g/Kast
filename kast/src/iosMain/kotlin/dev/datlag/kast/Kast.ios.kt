package dev.datlag.kast

import cocoapods.GoogleCast.GCKCastContext
import cocoapods.GoogleCast.GCKCastOptions
import cocoapods.GoogleCast.GCKError
import kotlinx.cinterop.*
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.StateFlow

actual object Kast {

    @OptIn(ExperimentalForeignApi::class)
    private var _castContext: GCKCastContext? = null

    actual val isSupported: Boolean
        get() = TODO("Not yet implemented")
    actual val connectionState: StateFlow<ConnectionState>
        get() = TODO("Not yet implemented")
    actual val allAvailableDevices: StateFlow<ImmutableSet<Device>>
        get() = TODO("Not yet implemented")

    @OptIn(ExperimentalForeignApi::class)
    fun setup(
        options: GCKCastOptions,
        castContextUnavailable: CastContextUnavailable? = null
    ) = apply {
        val cast = if (GCKCastContext.isSharedInstanceInitialized()) {
            GCKCastContext.sharedInstance()
        } else {
            val error = nativeHeap.alloc<ObjCObjectVar<GCKError?>>()

            if (GCKCastContext.setSharedInstanceWithOptions(options, error.ptr)) {
                GCKCastContext.sharedInstance().also {
                    nativeHeap.free(error)
                }
            } else {
                nativeHeap.free(error)
                return@apply castContextUnavailable?.invoke() ?: Unit
            }
        }

        setup(cast)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setup(
        castContext: GCKCastContext,
    ) = apply {
        this._castContext = castContext
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun dispose() = apply {
        this._castContext = null
    }

    actual fun select(device: Device): Kast {
        TODO("Not yet implemented")
    }

    actual fun unselect(reason: UnselectReason): Kast {
        TODO("Not yet implemented")
    }

    /**
     * Android specific options mapped for common module.
     */
    actual object Android {
        actual fun activeDiscovery() { }

        actual fun passiveDiscovery() { }
    }

    object Apple {
        @OptIn(ExperimentalForeignApi::class)
        fun discovery() {

        }
    }

}