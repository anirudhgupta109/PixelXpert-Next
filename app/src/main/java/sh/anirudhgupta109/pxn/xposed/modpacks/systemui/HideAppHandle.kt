package sh.anirudhgupta109.pxn.xposed.modpacks.systemui

import android.content.Context
import android.view.View
import io.github.libxposed.api.XposedModuleInterface
import sh.anirudhgupta109.pxn.xposed.XPrefs.Xprefs
import sh.anirudhgupta109.pxn.xposed.XposedModPack
import sh.anirudhgupta109.pxn.xposed.annotations.SystemUIModPack
import sh.anirudhgupta109.pxn.xposed.utils.reflection.ReflectedClass
import sh.anirudhgupta109.pxn.xposed.utils.reflection.Reflection.getObjectField
import sh.anirudhgupta109.pxn.xposed.utils.reflection.Reflection.setBooleanField

/**
 * Hides the WM Shell desktop-windowing app handle pill drawn at the top of
 * windows while keeping desktop header bars (`AppHeaderViewHolder`) untouched.
 *
 * Clearing `RelayoutParams.isCaptionVisible` on [AppHandleController.relayout]
 * turns off touchable-region allocation, disposes the status bar input layer,
 * and reports `CaptionState.NoCaption` so SystemUI does not carve a dead touch
 * zone out of the status bar. The `AppHandleViewHolder` and
 * `DesktopModeWindowDecoration` hooks cover initial inflation and legacy paths.
 */
@SystemUIModPack
class HideAppHandle(context: Context) : XposedModPack(context) {

	private var hideAppHandle = false

	override fun onPreferenceUpdated(vararg key: String?) {
		hideAppHandle = Xprefs.getBoolean("HideAppHandle", false)
	}

	override fun onPackageLoaded(PRParam: XposedModuleInterface.PackageReadyParam) {
		val appHandleControllerClass = ReflectedClass.ofIfPossible(
			"com.android.wm.shell.windowdecor.caption.apphandle.AppHandleController",
		)
		val appHandleViewHolderClass = ReflectedClass.ofIfPossible(
			"com.android.wm.shell.windowdecor.caption.apphandle.AppHandleViewHolder",
		)
		val appHandleViewHolderFactoryClass = ReflectedClass.ofIfPossible(
			"com.android.wm.shell.windowdecor.caption.apphandle.AppHandleViewHolder\$Factory",
		)
		val desktopModeWindowDecorationClass = ReflectedClass.ofIfPossible(
			"com.android.wm.shell.windowdecor.DesktopModeWindowDecoration",
		)

		appHandleControllerClass
			.before("relayout")
			.run { param ->
				if (!hideAppHandle) return@run
				param.args.getOrNull(0)?.let { setBooleanField(it, "isCaptionVisible", false) }
			}

		appHandleViewHolderFactoryClass
			.after("create")
			.run { param ->
				if (!hideAppHandle) return@run
				val holder = param.getResult<Any?>() ?: return@run
				(getObjectField(holder, "rootView") as? View)?.visibility = View.GONE
			}

		appHandleViewHolderClass
			.before("bindData")
			.run { param ->
				if (!hideAppHandle) return@run
				val data = param.args.getOrNull(0) ?: return@run
				setBooleanField(data, "isCaptionVisible", false)
				setBooleanField(data, "showInputLayer", false)
			}

		desktopModeWindowDecorationClass
			.after("isCaptionVisible")
			.run { param ->
				if (!hideAppHandle) return@run
				val holder = getObjectField(param.thisObject, "mWindowDecorViewHolder") ?: return@run
				if (appHandleViewHolderClass.clazz?.isInstance(holder) == true) {
					param.setResult(false)
				}
			}
	}
}
