package dev.datlag.kast

import android.content.Context
import android.media.MediaRoute2Info
import androidx.core.content.ContextCompat
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.RouteInfo
import com.google.android.gms.cast.framework.CastContext
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

    private var volumeListener: VolumeListener? = null
    private var routeListener: DeviceListener? = null
    private var connectionListener: ConnectionStateListener? = null

    var isSetupFinished: Boolean = false
        private set

    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    @JvmStatic
    @JvmOverloads
    fun setup(context: Context, castContextUnavailable: CastContextUnavailable? = null) = apply {
        val mediaRouter = ContextCompat.getSystemService(context, MediaRouter::class.java)
            ?: MediaRouter.getInstance(context)

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
        this.isSetupFinished = true

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

    private fun update() {
        val router = mediaRouter ?: return
        val selectedRoute = router.selectedRoute.let {
            if (it.isDefault) {
                null
            } else {
                it
            }
        }

        val routes = router.routes.filterNotNull().filterNot {
            it.isDefault
        }.sortedWith(RouteComparator)

        connectionState = when (selectedRoute?.connectionState) {
            MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING -> ConnectionState.CONNECTING
            MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED -> ConnectionState.CONNECTED
            else -> ConnectionState.DISCONNECTED
        }

        routeListener?.invoke(
            selected = selectedRoute?.let { Device(it) },
            available = routes.map { Device(it) }
        )

        connectionListener?.invoke(
            device = selectedRoute?.let { Device(it) },
            state = connectionState
        )
    }

    private fun updateVolume() {
        val router = mediaRouter ?: return

        val selectedRoute = router.selectedRoute
        if (selectedRoute.volumeHandling == MediaRoute2Info.PLAYBACK_VOLUME_FIXED) {
            volumeListener?.invoke(true, selectedRoute.volume, selectedRoute.volumeMax)
        } else {
            volumeListener?.invoke(false, selectedRoute.volume, selectedRoute.volumeMax)
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

    data object Listener {
        @JvmStatic
        fun setVolumeListener(listener: VolumeListener?) = apply {
            volumeListener = listener
            updateVolume()
        }

        @JvmStatic
        fun setRouteListener(listener: DeviceListener?) = apply {
            routeListener = listener
            update()
        }

        @JvmStatic
        fun setConnectionStateListener(listener: ConnectionStateListener?) = apply {
            connectionListener = listener
            listener?.invoke(mediaRouter?.selectedRoute?.let {
                if (it.isDefault) {
                    null
                } else {
                    it
                }
            }?.let { Device(it) }, connectionState)
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

            update()
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            super.onRouteChanged(router, route)

            update()
        }

        override fun onRouteRemoved(router: MediaRouter, route: RouteInfo) {
            super.onRouteRemoved(router, route)

            update()
        }

        override fun onRouteSelected(router: MediaRouter, route: RouteInfo, reason: Int) {
            super.onRouteSelected(router, route, reason)

            update()
        }

        override fun onRouteUnselected(router: MediaRouter, route: RouteInfo, reason: Int) {
            super.onRouteUnselected(router, route, reason)

            update()
        }

        override fun onRouteVolumeChanged(router: MediaRouter, route: RouteInfo) {
            super.onRouteVolumeChanged(router, route)

            updateVolume()
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