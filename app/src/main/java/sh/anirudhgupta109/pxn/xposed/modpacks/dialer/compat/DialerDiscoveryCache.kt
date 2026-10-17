package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat

import android.content.Context
import android.util.AtomicFile
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import java.io.File
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.Properties

/** Caches only resolved names; bytecode is eligible for collection after discovery. */
internal object DialerDiscoveryCache {
    private var indexKey = ""
    private var index = WeakReference<Map<String, ClassDef>>(null)

    private fun identity(apks: List<File>): String {
        // Installed APK paths are immutable and change on replacement. Include split
        // paths and file metadata as well, so a downgrade cannot reuse newer targets.
        val files = apks.sortedBy { it.absolutePath }.joinToString("\n") {
            "${it.absolutePath}:${it.length()}:${it.lastModified()}"
        }
        return MessageDigest.getInstance("SHA-256").digest(files.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun classes(apks: List<File>): Map<String, ClassDef> {
        val key = identity(apks)
        if (indexKey == key) index.get()?.let { return it }
        val classes = apks.flatMap { apk ->
            val dex = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault())
            dex.dexEntryNames.flatMap { requireNotNull(dex.getEntry(it)).dexFile.classes }
        }.associateBy { it.type }
        indexKey = key
        index = WeakReference(classes)
        return classes
    }

    fun <T> resolve(context: Context, apks: List<File>, schema: String,
        decode: (Properties) -> T, discover: () -> Properties): T {
        val key = identity(apks)
        // Bump the schema when discovery rules or serialized fields change.
        // Credential-protected storage may be unavailable before first unlock.
        val file = runCatching { AtomicFile(File(context.codeCacheDir, "pixelxpert-$schema.properties")) }.getOrNull()
        val cached = runCatching {
            val values = Properties().apply { requireNotNull(file).openRead().use { load(it) } }
            require(values.getProperty("apk") == key)
            decode(values)
        }
        if (cached.isSuccess) return cached.getOrThrow()
        val values = discover()
        val resolved = decode(values)
        values.setProperty("apk", key)
        // Cache failures must not prevent valid hooks from being installed.
        runCatching {
            val cache = file ?: return@runCatching
            val stream = cache.startWrite()
            try {
                values.store(stream, null)
                cache.finishWrite(stream)
            } catch (error: Throwable) {
                cache.failWrite(stream)
                throw error
            }
        }
        return resolved
    }
}
