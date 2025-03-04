package dev.datlag.kast

import cocoapods.GoogleCast.GCKCastContext
import cocoapods.GoogleCast.GCKCastOptions
import cocoapods.GoogleCast.GCKError
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.cinterop.*
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object Kast {

    @OptIn(ExperimentalForeignApi::class)
    private var _castContext: GCKCastContext? = null

    @OptIn(ExperimentalForeignApi::class)
    val castContext: GCKCastContext?
        get() = _castContext

    actual val isSupported: Boolean = true

    private val _connectionState: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.DISCONNECTED)

    @NativeCoroutinesState
    actual val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _allAvailableDevices: MutableStateFlow<ImmutableSet<Device>> = MutableStateFlow(persistentSetOf())

    @NativeCoroutinesState
    actual val allAvailableDevices: StateFlow<Collection<Device>> = _allAvailableDevices.asStateFlow()

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
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
                val exception = error.value?.let(::KastError)
                nativeHeap.free(error)
                return@apply castContextUnavailable?.invoke(exception) ?: Unit
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

    @OptIn(ExperimentalForeignApi::class)
    actual fun select(device: Device): Kast = apply {
        castContext?.sessionManager?.startSessionWithDevice(device.route)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun unselect(reason: UnselectReason): Kast = apply {
        when (reason) {
            is UnselectReason.Disconnected -> {
                castContext?.sessionManager?.endSessionAndStopCasting(true)
            }
            is UnselectReason.Stopped -> {
                castContext?.sessionManager?.endSession()
            }
            is UnselectReason.Device_Changed -> {
                castContext?.sessionManager?.endSessionAndStopCasting(true)
            }
            is UnselectReason.Unknown -> {
                castContext?.sessionManager?.endSession()
            }
        }
    }

    /**
     * Android specific options mapped for common module.
     */
    actual object Android {
        actual fun activeDiscovery() { }

        actual fun passiveDiscovery() { }
    }

}