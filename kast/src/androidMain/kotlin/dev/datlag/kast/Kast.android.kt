package dev.datlag.kast

import android.content.Context
import android.media.MediaRoute2Info
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors
import kotlin.math.abs

actual object Kast {

    @Suppress("StaticFieldLeak")
    private var _castContext: CastContext? = null

    @Suppress("StaticFieldLeak")
    private var _mediaRouter: MediaRouter? = null

    val castContext: CastContext?
        get() = _castContext

    val mediaRouter: MediaRouter?
        get() = _mediaRouter

    private var selector: MediaRouteSelector? = null

    actual val isSupported: Boolean = true

    private val _connectionState: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.DISCONNECTED)

    @NativeCoroutinesState
    actual val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _selectedDevice: MutableStateFlow<Device?> = MutableStateFlow(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _allAvailableDevices: MutableStateFlow<ImmutableSet<Device>> = MutableStateFlow(persistentSetOf())

    @NativeCoroutinesState
    actual val allAvailableDevices: StateFlow<Collection<Device>> = _allAvailableDevices.asStateFlow()

    private val _volumeInfo: MutableStateFlow<VolumeInfo?> = MutableStateFlow(null)
    val volumeInfo: StateFlow<VolumeInfo?> = _volumeInfo.asStateFlow()

    @JvmStatic
    @JvmOverloads
    fun setup(context: Context, castContextUnavailable: CastContextUnavailable? = null) = apply {
        val mediaRouter = MediaRouter.getInstance(context)

        CastContext.getSharedInstance(context, Executors.newSingleThreadExecutor()).addOnSuccessListener {
            val cast = it ?: CastContext.getSharedInstance() ?: return@addOnSuccessListener castContextUnavailable?.invoke(null) ?: Unit
            setup(cast, mediaRouter)
        }.addOnFailureListener {
            val cast = CastContext.getSharedInstance() ?: return@addOnFailureListener castContextUnavailable?.invoke(it) ?: Unit
            setup(cast, mediaRouter)
        }.addOnCanceledListener {
            val cast = CastContext.getSharedInstance() ?: return@addOnCanceledListener castContextUnavailable?.invoke(null) ?: Unit
            setup(cast, mediaRouter)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun setup(
        castContext: CastContext,
        mediaRouter: MediaRouter,
        mediaRouteSelector: MediaRouteSelector = runCatching { castContext.mergedSelector }.getOrNull() ?: MediaRouteSelector.EMPTY
    ) = apply {
        this._castContext = castContext
        this._mediaRouter = mediaRouter
        this.selector = mediaRouteSelector

        mediaRouter.addCallback(mediaRouteSelector, MediaCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
    }

    @JvmStatic
    actual fun dispose() = apply {
        this._castContext = null
        this._mediaRouter = null
    }

    @JvmStatic
    actual fun select(device: Device) = apply {
        val router = mediaRouter ?: return@apply device.route.select()
        router.selectRoute(device.route)
    }

    @JvmStatic
    actual fun unselect(reason: UnselectReason) = apply {
        val router = mediaRouter ?: return@apply
        val mappedReason = when (reason) {
            is UnselectReason.Unknown -> MediaRouter.UNSELECT_REASON_UNKNOWN
            is UnselectReason.Disconnected -> MediaRouter.UNSELECT_REASON_DISCONNECTED
            is UnselectReason.Stopped -> MediaRouter.UNSELECT_REASON_STOPPED
            is UnselectReason.Device_Changed -> MediaRouter.UNSELECT_REASON_ROUTE_CHANGED
        }
        router.unselect(mappedReason)
    }

    private fun update(
        router: MediaRouter? = mediaRouter,
        select: MediaRouter.RouteInfo? = null
    ) {
        val usingRouter = router ?: mediaRouter ?: return
        val selectedRoute = (select ?: usingRouter.selectedRoute).let {
            if (it.isSystemRoute) {
                null
            } else {
                it
            }
        }

        val routes = usingRouter.routes.filterNotNull().filterNot {
            it.isSystemRoute
        }.sortedWith(RouteComparator)

        _connectionState.update {
            when (selectedRoute?.connectionState) {
                MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING -> ConnectionState.CONNECTING
                MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED -> ConnectionState.CONNECTED
                else -> ConnectionState.DISCONNECTED
            }
        }

        _selectedDevice.update {
            selectedRoute?.let(::Device)
        }
        _allAvailableDevices.update {
            routes.map(::Device).toImmutableSet()
        }
    }

    private fun updateVolume(router: MediaRouter? = mediaRouter, route: MediaRouter.RouteInfo? = null) {
        val usingRouter = router ?: mediaRouter ?: return

        val selectedRoute = route ?: usingRouter.selectedRoute
        if (selectedRoute.volumeHandling == MediaRoute2Info.PLAYBACK_VOLUME_FIXED) {
            _volumeInfo.value = VolumeInfo(
                fixed = true,
                current = selectedRoute.volume,
                max = selectedRoute.volumeMax
            )
        } else {
            _volumeInfo.value = VolumeInfo(
                fixed = false,
                current = selectedRoute.volume,
                max = selectedRoute.volumeMax
            )
        }
    }

    data object Router {
        @JvmStatic
        @JvmOverloads
        fun activeDiscovery(
            routeSelector: MediaRouteSelector = selector ?: MediaRouteSelector.EMPTY
        ) = apply {
            mediaRouter?.removeCallback(MediaCallback)
            mediaRouter?.addCallback(routeSelector, MediaCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        }

        @JvmStatic
        @JvmOverloads
        fun passiveDiscovery(
            routeSelector: MediaRouteSelector = selector ?: MediaRouteSelector.EMPTY
        ) = apply {
            mediaRouter?.removeCallback(MediaCallback)
            mediaRouter?.addCallback(routeSelector, MediaCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        }

        @JvmStatic
        fun addProvider(provider: MediaRouteProvider) = apply {
            mediaRouter?.addProvider(provider)
        }

        @JvmStatic
        fun removeProvider(provider: MediaRouteProvider) = apply {
            mediaRouter?.removeProvider(provider)
        }
    }

    data object Options {
        @JvmStatic
        @JvmOverloads
        fun increaseVolume(value: Int = 1) = apply {
            val newVolume = abs(value)

            changeVolume(newVolume)
        }

        @JvmStatic
        @JvmOverloads
        fun decreaseVolume(value: Int = 1) = apply {
            val newVolume = -abs(value)

            changeVolume(newVolume)
        }

        @JvmStatic
        fun changeVolume(value: Int) = apply {
            val selectedRoute = mediaRouter?.selectedRoute ?: return@apply
            if (selectedRoute.volumeHandling == MediaRoute2Info.PLAYBACK_VOLUME_VARIABLE) {
                selectedRoute.requestUpdateVolume(value)
            }
        }

        @JvmStatic
        fun setVolume(value: Int) = apply {
            val selectedRoute = mediaRouter?.selectedRoute ?: return@apply
            if (selectedRoute.volumeHandling == MediaRoute2Info.PLAYBACK_VOLUME_VARIABLE) {
                selectedRoute.requestSetVolume(value)
            }
        }
    }

    actual data object Android {
        @JvmStatic
        actual fun activeDiscovery() {
            Router.activeDiscovery()
        }

        @JvmStatic
        actual fun passiveDiscovery() {
            Router.passiveDiscovery()
        }
    }

    private object MediaCallback : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            super.onRouteAdded(router, route)

            update(router)
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            super.onRouteChanged(router, route)

            update(router)
        }

        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
            super.onRouteRemoved(router, route)

            update(router)
        }

        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
            super.onRouteSelected(router, route, reason)

            update(router, route)
        }

        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
            super.onRouteUnselected(router, route, reason)

            update(router)
        }

        override fun onRouteVolumeChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            super.onRouteVolumeChanged(router, route)

            updateVolume(router, route)
        }
    }

    private object RouteComparator : Comparator<MediaRouter.RouteInfo?> {
        override fun compare(lhs: MediaRouter.RouteInfo?, rhs: MediaRouter.RouteInfo?): Int {
            return when {
                lhs == null && rhs == null -> 0
                lhs == null || rhs?.isSelected == true -> 1
                rhs == null || lhs.isSelected -> -1
                else -> lhs.name.compareTo(rhs.name)
            }
        }
    }

}