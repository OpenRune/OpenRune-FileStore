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
 * A summary is logged only when work was actually skipped: when everything in a section is packed the bar
 * already says so, and repeating it doubles the output of a build with many sections.
 */
open class DefaultCacheProgress : CacheProgress {

    private val logger = InlineLogger()

    override fun begin(label: String, total: Long): ProgressTracker {
        if (total <= 0) return ProgressTracker.None
        return BarTracker(progress(label, total))
    }

    override fun summary(label: String, packed: Int, skipped: Int) {
        if (skipped <= 0) return
        if (packed == 0) {
            logger.info { "$label: up to date ($skipped unchanged)" }
        } else {
            logger.info { "$label: packed $packed, skipped $skipped unchanged" }
        }
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
