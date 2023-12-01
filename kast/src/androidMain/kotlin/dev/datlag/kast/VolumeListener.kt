package dev.datlag.kast

fun interface VolumeListener {
    operator fun invoke(fixed: Boolean, current: Int, max: Int)
}