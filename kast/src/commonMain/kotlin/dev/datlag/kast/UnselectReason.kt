package dev.datlag.kast

import kotlinx.serialization.Serializable

@Serializable
sealed interface UnselectReason {

    @Serializable
    data object Unknown : UnselectReason

    @Serializable
    data object Disconnected : UnselectReason

    @Serializable
    data object Stopped : UnselectReason

    @Serializable
    data object Device_Changed : UnselectReason

    companion object {
        @JvmField
        val unknown = Unknown

        @JvmField
        val disconnected: UnselectReason = Disconnected

        @JvmField
        val stopped: UnselectReason = Stopped

        @JvmField
        val deviceChanged: UnselectReason = Device_Changed
    }
}