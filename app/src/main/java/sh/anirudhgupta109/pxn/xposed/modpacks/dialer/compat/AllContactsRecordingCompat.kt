package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat

import android.annotation.SuppressLint
import android.content.Context
import io.github.libxposed.api.XposedInterface
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Properties
import sh.anirudhgupta109.pxn.xposed.utils.toolkit.Logger

object AllContactsRecordingCompat {
    private data class Hooks(val eligibility: Method, val constructor: Constructor<*>,
        val contacts: Field, val factories: List<Method>, val feature: Field, val recording: Class<*>)

    @SuppressLint("DiscouragedApi")
    fun install(context: Context, loader: ClassLoader, xposed: XposedInterface) {
        val hooks = mutableListOf<XposedInterface.HookHandle>()
        try {
            val ids = listOf("fermat_call_recording_settings_title",
                "fermat_call_recording_auto_start_prompt_content_title",
                "incall_snackbar_call_recording_start_error").map {
                context.resources.getIdentifier(it, "string", "com.google.android.dialer")
            }
            if (ids.any { it == 0 }) return
            val app = context.applicationInfo
            val apks = (listOf(app.sourceDir) + app.splitSourceDirs.orEmpty()).map { File(it) }
            fun load(type: String) = Class.forName(type.substring(1, type.length - 1).replace('/', '.'), false, loader)
            fun method(value: String): Method {
                val parts = value.split('|')
                return load(parts[0]).getDeclaredMethod(parts[1], load(parts[2]))
            }
            val target = DialerDiscoveryCache.resolve(context, apks, "all-contacts-v1", decode = { p ->
                val eligibility = method(p.getProperty("eligibility"))
                val config = load(p.getProperty("config"))
                val constructor = config.getDeclaredConstructor(load(p.getProperty("configParameter")))
                val contacts = config.getDeclaredField(p.getProperty("contacts")).apply { isAccessible = true }
                val factories = p.getProperty("factories").split(',').map { method(it) }
                val feature = eligibility.declaringClass.getDeclaredField(p.getProperty("feature")).apply { isAccessible = true }
                val recording = load(p.getProperty("recording"))
                require(contacts.type == Set::class.java && feature.type.isAssignableFrom(recording))
                require((factories + eligibility).all { !Modifier.isStatic(it.modifiers) && it.returnType == Any::class.java })
                Hooks(eligibility, constructor, contacts, factories, feature, recording)
            }, discover = {
                val targets = AutomaticRecordingTargets.resolve(apks, ids)
                fun signature(m: com.android.tools.smali.dexlib2.iface.Method) =
                    "${m.definingClass}|${m.name}|${m.parameterTypes.single()}"
                Properties().apply {
                    setProperty("eligibility", signature(targets.eligibility))
                    setProperty("config", targets.configConstructor.definingClass)
                    setProperty("configParameter", targets.configConstructor.parameterTypes.single().toString())
                    setProperty("contacts", targets.contacts.name)
                    setProperty("factories", targets.configFactories.joinToString(",") { signature(it) })
                    setProperty("feature", targets.feature.name)
                    setProperty("recording", targets.recordingClass)
                }
            })
            val scope = ThreadLocal<Boolean>()
            check(xposed.deoptimize(target.eligibility))
            target.factories.forEach { check(xposed.deoptimize(it)) }
            hooks += xposed.hook(target.constructor).intercept { chain ->
                val result = chain.proceed()
                @Suppress("UNCHECKED_CAST")
                val selected = target.contacts.get(chain.thisObject) as? Set<Any?> ?: return@intercept result
                // Keep iteration and stored settings intact. Only this configuration's
                // membership check changes, and only during ordinary recording eligibility.
                target.contacts.set(chain.thisObject, object : java.util.AbstractSet<Any?>() {
                    override val size get() = selected.size
                    override fun iterator(): MutableIterator<Any?> =
                        java.util.Collections.unmodifiableSet(selected).iterator()
                    override fun contains(element: Any?): Boolean =
                        (scope.get() == true && element is String && element.isNotBlank()) || selected.contains(element)
                })
                result
            }
            hooks += xposed.hook(target.eligibility).intercept { chain ->
                val previous = scope.get()
                scope.set(target.recording.isInstance(target.feature.get(chain.thisObject)))
                try { chain.proceed() }
                finally { if (previous == null) scope.remove() else scope.set(previous) }
            }
            Logger.log("PixelXpert: all-contacts recording hooks installed")
        } catch (error: Throwable) {
            hooks.forEach { runCatching { it.unhook() } }
            Logger.log("PixelXpert: all-contacts recording lookup failed; keeping Google's settings: $error")
        }
    }
}
