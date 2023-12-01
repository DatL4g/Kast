package dev.datlag.kast

import dev.datlag.kast.proto.CastMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class Channel {

    init {
        println(protobuf.encodeToHexString<CastMessage>(
            CastMessage(
                protocolVersion = CastMessage.ProtocolVersion.CASTV2_1_0,
                sourceId = "sourceId",
                destinationId = "destinationId",
                namespace = "namespace",
                payloadType = CastMessage.PayloadType.STRING,
                payloadUtf8 = "payloadData"
            )
        ))
    }

    companion object {
        val protobuf = ProtoBuf { }
    }
}