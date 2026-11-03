package sh.anirudhgupta109.pxn.xposed.modpacks.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.DisplayCutout
import io.github.libxposed.api.XposedModuleInterface
import sh.anirudhgupta109.pxn.xposed.XPrefs.Xprefs
import sh.anirudhgupta109.pxn.xposed.XposedModPack
import sh.anirudhgupta109.pxn.xposed.annotations.FrameworkModPack
import sh.anirudhgupta109.pxn.xposed.annotations.SystemUIModPack
import sh.anirudhgupta109.pxn.xposed.utils.reflection.ReflectedClass
import sh.anirudhgupta109.pxn.xposed.utils.reflection.Reflection.getObjectField
import sh.anirudhgupta109.pxn.xposed.utils.reflection.Reflection.getStaticObjectField
import java.util.IdentityHashMap

/**
 * Gives the module its own answer for "how tall is the status bar?", by
 * scaling the `status_bar_height` dimen measured in a portrait-only
 * configuration by `statusbarHeightFactor` and pushing the result at every
 * place the framework reports it - the `DisplayCutout` the window manager
 * derives, plus `SystemBarUtils.getStatusBarHeight` and its per-rotation
 * sibling. With `noCutoutEnabled` set the before-hook replaces
 * `WmDisplayCutout.getDisplayCutout` outright with `DisplayCutout.NO_CUTOUT`,
 * which is what hides the cutout area from the rest of the UI.
 *
 * Every hook body sits in a `Throwable` swallow because these are framework
 * internals: a build that lacks one of the classes must leave the status bar
 * exactly as the OS drew it rather than crash system server. The height is
 * only recomputed when the factor moved, when this instance already touched
 * it, or when a forced re-apply is pending - otherwise the original value is
 * left alone even at 100%.
 *
 * Kotlin has no class-level `@SuppressWarnings("RedundantThrows")`
 * equivalent, so it is absent here; `onPackageLoaded` does not declare `throws`
 * either way, because the base class already carries `@Throws`.
 */
// We are playing in system framework. should be extra cautious..... many try-catchs, still not enough!
@FrameworkModPack
@SystemUIModPack
class StatusbarSize(context: Context) : XposedModPack(context) {

	companion object {
		private const val BOUNDS_POSITION_TOP = 1

		private var sizeFactor = 100 // % of normal
		private var noCutoutEnabled = true
	}

	private var currentHeight = 0

	// if we touched it once during this instance, we'll have to continue setting it even if it's the original value
	private var edited = false
	private var mForceApplyHeight = false

	// `WmDisplayCutout.getDisplayCutout` hands out a live, cached object and the
	// hook shrinks its bounds in place. Remember the untouched values so the
	// shrink can be undone when the factor goes back to 100% - otherwise only
	// a process restart would restore the stock status bar height.
	// Identity, not equality: `DisplayCutout` hashes and compares its insets,
	// and those are exactly what this hook mutates. A `WeakHashMap` would
	// lose its own entries as soon as the bounds changed.
	private val originalCutouts = IdentityHashMap<DisplayCutout, IntArray>()

	@SuppressLint("DiscouragedApi", "InternalInsetResource")
	@Suppress("SENSELESS_COMPARISON")
	override fun onPreferenceUpdated(vararg key: String?) {
		noCutoutEnabled = Xprefs.getBoolean("noCutoutEnabled", false)

		mForceApplyHeight = Xprefs.getBoolean("allScreenRotations", false) || // Particularly used for rotation Status bar
			noCutoutEnabled ||
			Xprefs.getBoolean("systemIconsMultiRow", false) ||
			Xprefs.getBoolean("notificationAreaMultiRow", false)

		sizeFactor = Xprefs.getSliderInt("statusbarHeightFactor", 100)
		if (sizeFactor >= 100) restoreCutouts()
		if (sizeFactor != 100 || edited || mForceApplyHeight) {
			val conf = Configuration()
			conf.updateFrom(mContext.resources.configuration)

			conf.orientation = Configuration.ORIENTATION_PORTRAIT
			val portraitContext = mContext.createConfigurationContext(conf)

			currentHeight = Math.round(
				portraitContext.resources.getDimensionPixelSize(
					portraitContext.resources.getIdentifier(
						"status_bar_height",
						"dimen",
						"android",
					),
				) * sizeFactor / 100f,
			)
		}
	}

	override fun onPackageLoaded(PRParam: XposedModuleInterface.PackageReadyParam) {
		try {
			try {
				val WmDisplayCutoutClass = ReflectedClass.of("com.android.server.wm.utils.WmDisplayCutout")
				val DisplayCutoutClass = ReflectedClass.of("android.view.DisplayCutout")

				val NO_CUTOUT = getStaticObjectField(DisplayCutoutClass.clazz, "NO_CUTOUT")

				WmDisplayCutoutClass
					.before("getDisplayCutout")
					.run { param ->
						if (noCutoutEnabled) {
							param.setResult(NO_CUTOUT)
						}
					}

				WmDisplayCutoutClass
					.after("getDisplayCutout")
					.run { param ->
						if (sizeFactor >= 100 && !edited) return@run

						val displayCutout: DisplayCutout = param.getResult()

						val bounds = getObjectField(displayCutout, "mBounds") ?: return@run
						val rects = getObjectField(bounds, "mRects") ?: return@run
						val boundTop = java.lang.reflect.Array.get(rects, BOUNDS_POSITION_TOP) as Rect
						val mSafeInsets = getObjectField(
							displayCutout,
							"mSafeInsets",
						) as Rect

						val original = synchronized(originalCutouts) {
							originalCutouts.getOrPut(displayCutout) {
								intArrayOf(boundTop.bottom, mSafeInsets.top)
							}
						}

						boundTop.bottom = Math.min(original[0], currentHeight)
						mSafeInsets.top = Math.min(original[1], currentHeight)
					}
			} catch (ignored: Throwable) {
			}

			val resizedResultConsumer = ReflectedClass.ReflectionConsumer { param ->
				try {
					if (sizeFactor == 100 && !edited && !mForceApplyHeight) return@ReflectionConsumer
					edited = true
					param.setResult(currentHeight)
				} catch (ignored: Throwable) {
				}
			}

			try {
				val SystemBarUtilsClass = ReflectedClass.of("com.android.internal.policy.SystemBarUtils")

				SystemBarUtilsClass.before("getStatusBarHeight").run(resizedResultConsumer)
				SystemBarUtilsClass.before("getStatusBarHeightForRotation").run(resizedResultConsumer)
			} catch (ignored: Throwable) {
			}
		} catch (ignored: Throwable) {
		}
	}

	private fun restoreCutouts() {
		if (originalCutouts.isEmpty()) return

		synchronized(originalCutouts) {
			for ((displayCutout, original) in originalCutouts) {
				try {
					val bounds = getObjectField(displayCutout, "mBounds") ?: continue
					val rects = getObjectField(bounds, "mRects") ?: continue
					val boundTop = java.lang.reflect.Array.get(rects, BOUNDS_POSITION_TOP) as Rect
					val mSafeInsets = getObjectField(displayCutout, "mSafeInsets") as Rect

					boundTop.bottom = original[0]
					mSafeInsets.top = original[1]
				} catch (ignored: Throwable) {
				}
			}
			originalCutouts.clear()
		}
	}

	override fun onSaveHotReloadState(outState: MutableMap<String, Any?>) {
		if (originalCutouts.isEmpty()) return

		// The cutouts are framework objects and the arrays are bootstrap
		// types, so a plain HashMap is safe to hand to the next generation.
		val saved = HashMap<Any, IntArray>()
		synchronized(originalCutouts) {
			for ((displayCutout, original) in originalCutouts) {
				saved[displayCutout] = original
			}
		}
		outState["originalCutouts"] = saved
	}

	@Suppress("UNCHECKED_CAST")
	override fun onRestoreHotReloadState(savedState: Map<String, Any?>) {
		val saved = savedState["originalCutouts"] as? Map<DisplayCutout, IntArray> ?: return
		synchronized(originalCutouts) {
			for ((displayCutout, original) in saved) {
				originalCutouts[displayCutout] = original
			}
		}
	}
}
