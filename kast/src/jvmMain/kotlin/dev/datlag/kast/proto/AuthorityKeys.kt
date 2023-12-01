@file:OptIn(ExperimentalSerializationApi::class)

package dev.datlag.kast.proto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class AuthorityKeys(
    @ProtoNumber(1) @SerialName("keys") val keys: List<Key>
) {
    @Serializable
    class Key(
        @ProtoNumber(1) @SerialName("fingerprint") val fingerprint: ByteArray,
        @ProtoNumber(2) @SerialName("public_key") val publicKey: ByteArray
    )
}
