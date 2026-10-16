package sh.anirudhgupta109.pxn.xposed.modpacks.systemui.compat

import android.content.pm.ApplicationInfo
import dalvik.system.DexFile
import io.github.libxposed.api.XposedInterface
import sh.anirudhgupta109.pxn.xposed.utils.toolkit.Logger
import sh.anirudhgupta109.pxn.xposed.utils.reflection.ReflectedClass

/** Compatibility with both direct Compose calls and independently recomposed render lambdas. */
internal object ShortcutComposeHooks {
	@Suppress("DEPRECATION") // DexFile enumeration is available on the supported Android versions.
	fun install(
		xposed: XposedInterface,
		provider: Class<*>?,
		applicationInfo: ApplicationInfo,
		scope: ShortcutRenderScope,
	) {
		// Older SystemUI variants use the existing ImageView hooks instead.
		if (provider == null) return
		val loader = provider.classLoader
		// Do not use Class.forName with a literal here: R8 rewrites it to the
		// module's obfuscated Composer name, even with SystemUI's class loader.
		val composer = ReflectedClass.ofIfPossible("androidx.compose.runtime.Composer", loader).clazz
			?: return

		// Generated lambdas are not necessarily returned by getDeclaredClasses().
		// Discover only this provider's class family, once, including split APKs.
		// Do not assume an Android release, compiler mangling, or lambda number.
		val names = linkedSetOf(provider.name)
		val paths = listOf(applicationInfo.sourceDir) + applicationInfo.splitSourceDirs.orEmpty()
		for (path in paths.distinct()) {
			try {
				val dex = DexFile(path)
				try {
					for (name in dex.entries()) {
						if (ShortcutRenderTargets.belongsToProvider(name, provider.name)) names.add(name)
					}
				} finally {
					dex.close()
				}
			} catch (e: Exception) {
				Logger.log("Shortcut Compose: could not inspect $path", e)
			}
		}

		var hooked = 0
		for (name in names) {
			try {
				val target = Class.forName(name, false, loader)
				for (method in target.declaredMethods) {
					if (!ShortcutRenderTargets.isRenderMethod(method, provider.name, composer)) continue
					try {
						// ART can inline BackgroundKt.background into the caller. Hooking
						// BackgroundKt alone then does nothing; deoptimize these narrow
						// callers so their original bodies also go through the color hook.
						if (!xposed.deoptimize(method)) {
							Logger.log("Shortcut Compose: could not deoptimize $method")
						}
						xposed.hook(method).intercept { chain ->
							if (chain.args.any { composer.isInstance(it) }) {
								scope.render { chain.proceed() }
							} else {
								chain.proceed()
							}
						}
						hooked++
					} catch (e: Exception) {
						Logger.log("Shortcut Compose: could not hook $method", e)
					}
				}
			} catch (e: ReflectiveOperationException) {
				Logger.log("Shortcut Compose: could not inspect $name", e)
			} catch (e: LinkageError) {
				Logger.log("Shortcut Compose: incompatible class $name", e)
			}
		}
		Logger.log("Shortcut Compose: scoped $hooked render methods")
	}
}
