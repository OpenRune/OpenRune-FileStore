package dev.openrune.cache.tools.progress

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.util.progress

/**
 * Receives all progress reporting from a cache build.
 *
 * The default renders one bar per section, which is noisy when a build has many small sections — a config
 * directory per content plugin, say. Supply your own through `cacheTool { progress = ... }` to render them
 * however you like, for example by holding a single bar for the whole build and relabelling it.
 */
interface CacheProgress {

    /**
     * Starts a section. Called once per task, or once per task scope for tasks split across several
     * directories. Returning [ProgressTracker.None] renders nothing.
     */
    fun begin(label: String, total: Long): ProgressTracker

    fun begin(label: String, total: Int): ProgressTracker = begin(label, total.toLong())

    /**
     * Reports how a section resolved once it is finished. [skipped] is non-zero only for tasks that support
     * incremental packing, and is what tells you the build actually saved work.
     */
    fun summary(label: String, packed: Int, skipped: Int) {}

    fun buildStarted(revision: Int, serverPass: Boolean) {}

    fun buildFinished() {}
}

/** A single section's progress. Closed by the reporter when the section finishes. */
interface ProgressTracker : AutoCloseable {

    fun step()

    fun step(message: String) {
        message(message)
        step()
    }

    fun stepTo(current: Long) {}

    /** Detail shown alongside the bar, typically the item being packed. */
    fun message(message: String) {}

    override fun close() {}

    /** Tracker that renders nothing; return it from [CacheProgress.begin] to hide a section. */
    object None : ProgressTracker {
        override fun step() {}
    }
}

/**
 * Renders a `me.tongfei.progressbar` bar per section.
 *
 * Section summaries are accumulated by label and logged once at [buildFinished] rather than per section: a
 * build has a config section per source directory, so logging each one buries the build in near-identical
 * "up to date" lines. Only labels that actually skipped work are reported — when everything in a section is
 * packed the bar already said so.
 */
open class DefaultCacheProgress : CacheProgress {

    private val logger = InlineLogger()

    private data class Tally(var packed: Int = 0, var skipped: Int = 0, var sections: Int = 0)

    private val tallies = LinkedHashMap<String, Tally>()

    override fun begin(label: String, total: Long): ProgressTracker {
        if (total <= 0) return ProgressTracker.None
        return BarTracker(progress(label, total))
    }

    override fun summary(label: String, packed: Int, skipped: Int) {
        val tally = tallies.getOrPut(label) { Tally() }
        tally.packed += packed
        tally.skipped += skipped
        tally.sections++
    }

    override fun buildStarted(revision: Int, serverPass: Boolean) {
        tallies.clear()
    }

    override fun buildFinished() {
        tallies.forEach { (label, tally) ->
            if (tally.skipped <= 0) return@forEach
            val sections = if (tally.sections > 1) " across ${tally.sections} sections" else ""
            if (tally.packed == 0) {
                logger.info { "$label: up to date [${tally.skipped} unchanged]$sections" }
            } else {
                logger.info { "$label: [${tally.packed} packed, ${tally.skipped} unchanged]$sections" }
            }
        }
        tallies.clear()
    }

    private class BarTracker(private val bar: me.tongfei.progressbar.ProgressBar) : ProgressTracker {
        override fun step() {
            bar.step()
        }

        override fun stepTo(current: Long) {
            bar.stepTo(current)
        }

        override fun message(message: String) {
            bar.extraMessage = message
        }
        override fun close() = bar.close()
    }
}

/** Reports nothing at all. Useful for tests and for embedding a build in another tool's output. */
object SilentCacheProgress : CacheProgress {
    override fun begin(label: String, total: Long) = ProgressTracker.None
}
