package dev.datlag.kast

import cocoapods.GoogleCast.GCKError
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
data class KastError(val error: GCKError) : Throwable(error.localizedDescription)
