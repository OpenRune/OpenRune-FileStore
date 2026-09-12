package dev.openrune.cache.tools.tasks.impl.defs

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.ITEM
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.autocert.AutoCertSettings
import dev.openrune.cache.tools.autocert.CertCandidateScanner
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

class PackAutoCert(private val settings: AutoCertSettings) : CacheTask() {

    private val logger = InlineLogger()

    override val priority: TaskPriority
        get() = TaskPriority.END

    override fun init(cache: Cache) {
        val codec = ItemCodec(revision)

        val templateId = ConstantProvider.getMappingOrNull(settings.template)
            ?: error("Auto-cert template '${settings.template}' has no gameval mapping")
        val template = cache.load(templateId, codec)
            ?: error("Auto-cert template '${settings.template}' ($templateId) is not in the cache")
        require(template.noteTemplateId > 0) {
            "Auto-cert template '${settings.template}' ($templateId) is not a cert item: it has no " +
                "note template. Point `template` at something like obj.cert_shark."
        }

        val candidates = CertCandidateScanner(settings).scan(cache, revision)
        if (candidates.isEmpty()) return

        val bar = progress.begin("Packing Auto Certs", candidates.size)
        var packed = 0

        candidates.forEach { candidate ->
            bar.message(candidate.certKey)

            val itemId = ConstantProvider.getMappingOrNull("${settings.table}.${candidate.itemKey}")
            val certId = ConstantProvider.getMappingOrNull("${settings.table}.${candidate.certKey}")

            if (itemId == null || certId == null) {
                logger.warn { "Auto-cert has no reserved id for '${candidate.certKey}', skipping" }
                bar.step()
                return@forEach
            }

            val item = cache.load(itemId, codec)
            if (item == null) {
                logger.warn {
                    "Auto-cert skipped '${candidate.certKey}': item '${candidate.itemKey}' " +
                        "($itemId) was not packed"
                }
                bar.step()
                return@forEach
            }

            val cert = template.copy(
                id = certId,
                name = item.name,
                noteLinkId = itemId,
                noteTemplateId = template.noteTemplateId,
            )
            cache.store(cert, codec)

            if (item.noteLinkId != certId) {
                cache.store(item.copy(noteLinkId = certId), codec)
            }

            CacheTool.addGameValMapping(
                GameValGroupTypes.OBJTYPES,
                GameValElement(candidate.certKey, certId),
            )

            packed++
            bar.step()
        }

        bar.close()
        logger.info { "Packed $packed auto-cert item(s) from '${settings.template}'" }
    }

    private fun Cache.load(id: Int, codec: ItemCodec): ItemType? =
        data(CONFIGS, ITEM, id)?.let { codec.loadData(id, it) }

    private fun Cache.store(item: ItemType, codec: ItemCodec) {
        val writer = Unpooled.buffer(4096)
        with(codec) { writer.encode(item) }
        write(CONFIGS, ITEM, item.id, writer.toArray())
    }
}
