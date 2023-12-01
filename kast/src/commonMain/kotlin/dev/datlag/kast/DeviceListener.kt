package dev.datlag.kast

fun interface DeviceListener {
    operator fun invoke(selected: Device?, available: List<Device>)
}