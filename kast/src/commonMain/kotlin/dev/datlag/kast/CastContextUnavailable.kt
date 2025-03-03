package dev.datlag.kast

fun interface CastContextUnavailable {
    operator fun invoke(throwable: Throwable?)
}