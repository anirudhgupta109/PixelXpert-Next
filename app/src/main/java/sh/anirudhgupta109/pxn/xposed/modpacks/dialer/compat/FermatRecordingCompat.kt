package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat

import android.annotation.SuppressLint
import android.content.Context
import io.github.libxposed.api.XposedInterface
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Properties
import java.util.function.BooleanSupplier
import sh.anirudhgupta109.pxn.xposed.utils.toolkit.Logger

object FermatRecordingCompat {
	private data class Hooks(val feature: Field, val flag: Field, val recording: Class<*>,
		val methods: List<Method>, val skipValue: Boolean)

	@SuppressLint("DiscouragedApi")
	@JvmStatic
	fun install(context: Context, loader: ClassLoader,
		removeAnnouncement: BooleanSupplier, xposed: XposedInterface) {
		if (!removeAnnouncement.asBoolean) return
		val hooks = mutableListOf<XposedInterface.HookHandle>()
		try {
			val resources = listOf("fermat_call_recording_settings_title",
				"fermat_call_recording_auto_start_prompt_content_title",
				"incall_snackbar_call_recording_start_error").map {
				context.resources.getIdentifier(it, "string", "com.google.android.dialer")
			}
			if (resources.any { it == 0 }) return
			val app = context.applicationInfo
			val apks = (listOf(app.sourceDir) + app.splitSourceDirs.orEmpty()).map { File(it) }
			fun load(type: String) = Class.forName(type.substring(1, type.length - 1).replace('/', '.'), false, loader)
			val target = DialerDiscoveryCache.resolve(context, apks, "recording-prompts-v1", decode = { p ->
				val session = load(p.getProperty("session"))
				val feature = session.getDeclaredField(p.getProperty("feature")).apply { isAccessible = true }
				val flag = session.getDeclaredField(p.getProperty("flag")).apply { isAccessible = true }
				val recording = load(p.getProperty("recording"))
				val parameter = load(p.getProperty("parameter"))
				val methods = p.getProperty("methods").split(',').map { session.getDeclaredMethod(it, parameter) }
				require(feature.type.isAssignableFrom(recording) && flag.type == Boolean::class.javaPrimitiveType)
				require(methods.distinct().size == 3 && methods.all {
					!Modifier.isStatic(it.modifiers) && it.returnType == Any::class.java
				})
				Hooks(feature, flag, recording, methods, p.getProperty("skip").toBooleanStrict())
			}, discover = {
				val targets = FermatRecordingTargets.resolve(apks, resources)
				Properties().apply {
					setProperty("session", targets.playPrompts.definingClass)
					setProperty("feature", targets.feature.name)
					setProperty("flag", targets.playPrompts.name)
					setProperty("recording", targets.recordingClass)
					setProperty("parameter", targets.prompts.first().parameterTypes.single().toString())
					setProperty("methods", targets.prompts.joinToString(",") { it.name })
					setProperty("skip", targets.skipValue.toString())
				}
			})
			target.methods.forEach { check(xposed.deoptimize(it)) }
			target.methods.forEach { method ->
				hooks += xposed.hook(method).intercept { chain ->
					if (removeAnnouncement.asBoolean && target.recording.isInstance(target.feature.get(chain.thisObject))) {
						// Startup resets this flag, so change it at each prompt entry.
						target.flag.setBoolean(chain.thisObject, target.skipValue)
					}
					chain.proceed()
				}
			}
			Logger.log("PixelXpert: recording prompt hooks installed")
		} catch (error: Throwable) {
			hooks.forEach { runCatching { it.unhook() } }
			Logger.log("PixelXpert: recording prompt lookup failed; keeping legacy hooks: $error")
		}
	}
}
