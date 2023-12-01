package dev.datlag.kast

sealed interface UnselectReason {
    data object UNKNOWN : UnselectReason
    data object DISCONNECTED : UnselectReason
    data object STOPPED : UnselectReason
    data object DEVICE_CHANGED : UnselectReason

    companion object {
        @JvmField
        val unknown = UNKNOWN

        @JvmField
        val disconnected: UnselectReason = DISCONNECTED

        @JvmField
        val stopped: UnselectReason = STOPPED

        @JvmField
        val deviceChanged: UnselectReason = DEVICE_CHANGED
    }
}