package com.pronotes.app.data

import android.content.ComponentCallbacks2
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Collections

/**
 * Centralized atomic file persistence helper.
 * Always writes to a sibling temporary file, flushes/syncs the descriptor, and atomically replaces
 * [target], checking move errors rather than ignoring `File.renameTo` results.
 */
object AtomicFiles {
    fun writeTextAtomic(target: File, content: String) {
        writeBytesAtomic(target) { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun writeBytesAtomic(target: File, writer: (OutputStream) -> Unit) {
        val parent = target.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmp = File(parent ?: File("."), "${target.name}.${System.nanoTime()}.tmp")
        try {
            FileOutputStream(tmp).use { fos ->
                writer(fos)
                fos.flush()
                runCatching { fos.fd.sync() }
            }
            moveAtomically(tmp, target)
        } catch (t: Throwable) {
            runCatching { tmp.delete() }
            throw t
        }
    }

    fun moveAtomically(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}

/**
 * Global registry of evictable bitmap/PDF/thumbnail caches notified by [android.app.Application.onTrimMemory]
 * and [android.app.Application.onLowMemory].
 */
object MemoryTrimRegistry {
    fun interface Listener {
        fun onTrimMemory(level: Int)
    }

    private val listeners = Collections.synchronizedList(ArrayList<WeakReference<Listener>>())

    fun register(listener: Listener) {
        synchronized(listeners) {
            listeners.removeAll { it.get() == null }
            if (listeners.none { it.get() === listener }) {
                listeners.add(WeakReference(listener))
            }
        }
    }

    @Suppress("DEPRECATION")
    fun dispatch(level: Int) {
        val active = synchronized(listeners) {
            listeners.removeAll { it.get() == null }
            listeners.mapNotNull { it.get() }
        }
        for (l in active) {
            runCatching { l.onTrimMemory(level) }
        }
    }

    @Suppress("DEPRECATION")
    fun isCriticalLevel(level: Int): Boolean =
        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE
}
