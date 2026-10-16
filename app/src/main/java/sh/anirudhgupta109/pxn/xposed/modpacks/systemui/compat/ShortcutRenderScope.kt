package sh.anirudhgupta109.pxn.xposed.modpacks.systemui.compat

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** A nested render or a throwing callback must not leak shortcut styling into other UI. */
internal class ShortcutRenderScope {
	private val depth = ThreadLocal.withInitial { 0 }
	val isActive: Boolean get() = depth.get() > 0

	fun <T> render(block: () -> T): T {
		val previous = depth.get()
		depth.set(previous + 1)
		try {
			return block()
		} finally {
			if (previous == 0) depth.remove() else depth.set(previous)
		}
	}
}

internal object ShortcutRenderTargets {
	fun belongsToProvider(name: String, providerName: String): Boolean =
		name == providerName || name.startsWith(providerName + "$")

	fun isRenderMethod(method: Method, providerName: String, composer: Class<*>): Boolean {
		if (!belongsToProvider(method.declaringClass.name, providerName) ||
			Modifier.isAbstract(method.modifiers) || Modifier.isNative(method.modifiers)) return false

		// The provider entry point and typed Kotlin restart/render lambdas.
		if (method.name != "invoke" && !method.name.startsWith("KeyguardShortcut")) return false
		if (method.parameterTypes.any { it == composer }) return true

		// R8 erases FunctionN.invoke parameters to Object. Check for a real
		// SystemUI Composer at invocation time before entering the scope.
		return method.name == "invoke" && method.declaringClass.name != providerName &&
			method.parameterCount >= 2 && method.parameterTypes.all { it == Any::class.java }
	}
}
