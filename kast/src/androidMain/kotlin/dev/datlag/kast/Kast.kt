package dev.datlag.kast

import android.content.Context
import android.media.MediaRoute2Info
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.RouteInfo
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.properties.Delegates

data object Kast {

    private var _castContext = WeakReference<CastContext?>(null)
    private var _mediaRouter = WeakReference<MediaRouter?>(null)

    val castContext: CastContext?
        get() = _castContext.get()

    val mediaRouter: MediaRouter?
        get() = _mediaRouter.get()

    private var selector by Delegates.notNull<MediaRouteSelector>()

    private val _setupFinished: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val setupFinished: StateFlow<Boolean> = _setupFinished

    private val _connectionState: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _selectedDevice: MutableStateFlow<Device?> = MutableStateFlow(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice

    private val _allAvailableDevices: MutableStateFlow<List<Device>> = MutableStateFlow(emptyList())
    val allAvailableDevices: StateFlow<List<Device>> = _allAvailableDevices

    private val _volumeInfo: MutableStateFlow<VolumeInfo?> = MutableStateFlow(null)
    val volumeInfo: StateFlow<VolumeInfo?> = _volumeInfo

    @JvmStatic
    @JvmOverloads
    fun setup(context: Context, castContextUnavailable: CastContextUnavailable? = null) = apply {
        val mediaRouter = MediaRouter.getInstance(context)

        CastContext.getSharedInstance(context, Executors.newSingleThreadExecutor()).addOnSuccessListener {
            val cast = it ?: CastContext.getSharedInstance() ?: return@addOnSuccessListener castContextUnavailable?.invoke() ?: Unit
            setup(cast, mediaRouter)
        }.addOnFailureListener {
            val cast = CastContext.getSharedInstance() ?: return@addOnFailureListener castContextUnavailable?.invoke() ?: Unit
            setup(cast, mediaRouter)
        }.addOnCanceledListener {
            val cast = CastContext.getSharedInstance() ?: return@addOnCanceledListener castContextUnavailable?.invoke() ?: Unit
            setup(cast, mediaRouter)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun setup(
        castContext: CastContext,
        mediaRouter: MediaRouter,
        mediaRouteSelector: MediaRouteSelector = castContext.mergedSelector ?: MediaRouteSelector.EMPTY
    ) = apply {
        this._castContext = WeakReference(castContext)
        this._mediaRouter = WeakReference(mediaRouter)
        this.selector = mediaRouteSelector
        this._setupFinished.value = true

        mediaRouter.addCallback(mediaRouteSelector, MediaCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
    }

    @JvmStatic
    fun dispose() = apply {
        this._castContext.enqueue()
        this._castContext.clear()

        this._mediaRouter.enqueue()
        this._mediaRouter.clear()
    }

    @JvmStatic
    fun select(device: Device) = apply {
        val router = mediaRouter ?: return this
        router.selectRoute(device.route)
    }

    @JvmStatic
    fun unselect(reason: UnselectReason) = apply {
        val router = mediaRouter ?: return@apply
        val mappedReason = when (reason) {
            is UnselectReason.UNKNOWN -> MediaRouter.UNSELECT_REASON_UNKNOWN
            is UnselectReason.DISCONNECTED -> MediaRouter.UNSELECT_REASON_DISCONNECTED
            is UnselectReason.STOPPED -> MediaRouter.UNSELECT_REASON_STOPPED
            is UnselectReason.DEVICE_CHANGED -> MediaRouter.UNSELECT_REASON_ROUTE_CHANGED
        }
        router.unselect(mappedReason)
    }

    private fun update(
        router: MediaRouter? = mediaRouter,
        select: RouteInfo? = null
    ) {
        val usingRouter = router?.also { updateRouter(it) } ?: mediaRouter ?: return
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

        _connectionState.value =  when (selectedRoute?.connectionState) {
            RouteInfo.CONNECTION_STATE_CONNECTING -> ConnectionState.CONNECTING
            RouteInfo.CONNECTION_STATE_CONNECTED -> ConnectionState.CONNECTED
            else -> ConnectionState.DISCONNECTED
        }

        _selectedDevice.value = selectedRoute?.let { Device(it) }
        _allAvailableDevices.value = routes.map { Device(it) }
    }

    private fun updateRouter(router: MediaRouter) {
        _mediaRouter.enqueue()
        _mediaRouter.clear()
        _mediaRouter = WeakReference(router)
    }

    private fun updateVolume(router: MediaRouter? = mediaRouter, route: RouteInfo? = null) {
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
        fun activeDiscovery(routeSelector: MediaRouteSelector = selector) = apply {
            mediaRouter?.removeCallback(MediaCallback)
            mediaRouter?.addCallback(routeSelector, MediaCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        }

        @JvmStatic
        @JvmOverloads
        fun passiveDiscovery(routeSelector: MediaRouteSelector = selector) = apply {
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

    private object MediaCallback : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            super.onRouteAdded(router, route)

            update(router)
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            super.onRouteChanged(router, route)

            update(router)
        }

        override fun onRouteRemoved(router: MediaRouter, route: RouteInfo) {
            super.onRouteRemoved(router, route)

            update(router)
        }

        override fun onRouteSelected(router: MediaRouter, route: RouteInfo, reason: Int) {
            super.onRouteSelected(router, route, reason)

            update(router, route)
        }

        override fun onRouteUnselected(router: MediaRouter, route: RouteInfo, reason: Int) {
            super.onRouteUnselected(router, route, reason)

            update(router)
        }

        override fun onRouteVolumeChanged(router: MediaRouter, route: RouteInfo) {
            super.onRouteVolumeChanged(router, route)

            updateVolume(router, route)
        }
    }

    private object RouteComparator : Comparator<RouteInfo?> {
        override fun compare(lhs: RouteInfo?, rhs: RouteInfo?): Int {
            return when {
                lhs == null && rhs == null -> 0
                lhs == null || rhs?.isSelected == true -> 1
                rhs == null || lhs.isSelected -> -1
                else -> lhs.name.compareTo(rhs.name)
            }
        }
    }
}