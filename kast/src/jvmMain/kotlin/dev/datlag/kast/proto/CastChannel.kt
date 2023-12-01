@file:OptIn(ExperimentalSerializationApi::class)

package dev.datlag.kast.proto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class CastMessage(
    @ProtoNumber(1) @SerialName("protocol_version") val protocolVersion: ProtocolVersion,
    @ProtoNumber(2) @SerialName("source_id") val sourceId: String,
    @ProtoNumber(3) @SerialName("destination_id") val destinationId: String,
    @ProtoNumber(4) @SerialName("namespace") val namespace: String,
    @ProtoNumber(5) @SerialName("payload_type") val payloadType: PayloadType,
    @ProtoNumber(6) @SerialName("payload_utf8") val payloadUtf8: String? = null,
    @ProtoNumber(7) @SerialName("payload_binary") val payloadBinary: ByteArray? = null
) {

    @Serializable
    enum class ProtocolVersion {
        @ProtoNumber(0) CASTV2_1_0;
    }

    @Serializable
    enum class PayloadType {
        @ProtoNumber(0) STRING,
        @ProtoNumber(1) BINARY;
    }
}

@Serializable
internal enum class SignatureAlgorithm {
    @ProtoNumber(0) UNSPECIFIED,
    @ProtoNumber(1) RSASSA_PKCS1v15,
    @ProtoNumber(2) RSASSA_PSS;
}

@Serializable
internal enum class HashAlgorithm {
    @ProtoNumber(0) SHA1,
    @ProtoNumber(1) SHA256;
}

@Serializable
internal class AuthChallenge(
    @ProtoNumber(1) @SerialName("signature_algorithm") val signatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.RSASSA_PKCS1v15,
    @ProtoNumber(2) @SerialName("sender_nonce") val senderNonce: ByteArray,
    @ProtoNumber(3) @SerialName("hash_algorithm") val hashAlgorithm: HashAlgorithm = HashAlgorithm.SHA1
)

@Serializable
internal class AuthResponse(
    @ProtoNumber(1) @SerialName("signature") val signature: ByteArray,
    @ProtoNumber(2) @SerialName("client_auth_certificate") val clientAuthCertificate: ByteArray,
    @ProtoNumber(3) @SerialName("intermediate_certificate") val intermediateCertificate: List<ByteArray>,
    @ProtoNumber(4) @SerialName("signature_algorithm") val signatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.RSASSA_PKCS1v15,
    @ProtoNumber(5) @SerialName("sender_nonce") val senderNonce: ByteArray? = null,
    @ProtoNumber(6) @SerialName("hash_algorithm") val hashAlgorithm: HashAlgorithm = HashAlgorithm.SHA1,
    @ProtoNumber(7) @SerialName("crl") val crl: ByteArray? = null
)

@Serializable
internal data class AuthError(
    @ProtoNumber(1) @SerialName("error_type") val errorType: ErrorType
) {
    @Serializable
    enum class ErrorType {
        @ProtoNumber(0) INTERNAL_ERROR,
        @ProtoNumber(1) NO_TLS,
        @ProtoNumber(2) SIGNATURE_ALGORITHM_UNAVAILABLE;
    }
}

@Serializable
internal data class DeviceAuthMessage(
    @ProtoNumber(1) @SerialName("challenge") val challenge: AuthChallenge? = null,
    @ProtoNumber(2) @SerialName("response") val response: AuthResponse? = null,
    @ProtoNumber(3) @SerialName("error") val error: AuthError? = null
)