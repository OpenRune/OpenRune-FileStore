package dev.openrune.decoder

import dev.openrune.cache.NPC
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.NPCCodec
import dev.openrune.cache.filestore.definition.data.NpcType

class NPCDecoder : DefinitionDecoderOSRS<NpcType>(NPCCodec(), NPC, ::NpcType)