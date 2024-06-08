package dev.datlag.kast

import kotlinx.serialization.Serializable

@Serializable
data class VolumeInfo(
    val fixed: Boolean,
    val current: Int,
    val max: Int
)
