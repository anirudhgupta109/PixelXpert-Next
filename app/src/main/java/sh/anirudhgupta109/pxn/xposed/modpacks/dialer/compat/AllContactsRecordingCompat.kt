package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat

import android.annotation.SuppressLint
import android.content.Context
import io.github.libxposed.api.XposedInterface
import java.io.File
import sh.anirudhgupta109.pxn.xposed.utils.toolkit.Logger

object AllContactsRecordingCompat {
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
            val targets = AutomaticRecordingTargets.resolve(
                (listOf(app.sourceDir) + app.splitSourceDirs.orEmpty()).map { File(it) }, ids)
            fun load(type: String) = Class.forName(type.substring(1, type.length - 1).replace('/', '.'), false, loader)
            val owner = load(targets.eligibility.definingClass)
            val eligibility = owner.getDeclaredMethod(targets.eligibility.name,
                load(targets.eligibility.parameterTypes.single().toString()))
            val contains = load(targets.contains.definingClass).getDeclaredMethod(targets.contains.name,
                Iterable::class.java, Any::class.java)
            val feature = owner.getDeclaredField(targets.feature.name).apply { isAccessible = true }
            val recording = load(targets.recordingClass)
            require(feature.type.isAssignableFrom(recording))
            val scope = ThreadLocal<Boolean>()
            check(xposed.deoptimize(eligibility))
            hooks += xposed.hook(contains).intercept { chain ->
                val candidate = chain.getArg(1)
                var directCaller = false
                if (scope.get() == true && candidate is String && candidate.isNotBlank()) {
                    val frames = Throwable().stackTrace
                    // The hook bridge replaces the callee class in the stack trace.
                    val index = frames.indexOfFirst { it.methodName == contains.name }
                    val caller = frames.getOrNull(index + 1)
                    directCaller = index >= 0 && caller?.className == owner.name && caller.methodName == eligibility.name
                }
                if (directCaller) true else chain.proceed()
            }
            hooks += xposed.hook(eligibility).intercept { chain ->
                val previous = scope.get()
                scope.set(recording.isInstance(feature.get(chain.thisObject)))
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
