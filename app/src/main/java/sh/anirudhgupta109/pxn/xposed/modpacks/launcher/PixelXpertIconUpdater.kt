package sh.anirudhgupta109.pxn.xposed.modpacks.launcher

import android.content.Context
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import io.github.libxposed.api.XposedModuleInterface
import sh.anirudhgupta109.pxn.BuildConfig
import sh.anirudhgupta109.pxn.xposed.XposedModPack
import sh.anirudhgupta109.pxn.xposed.annotations.LauncherModPack
import sh.anirudhgupta109.pxn.xposed.utils.reflection.ReflectedClass

/**
 * Asks the launcher to re-check this package's icon whenever an activity resumes.
 *
 * The pack captures the first `LauncherModel` instance through a constructor
 * hook, then fires `onAppIconChanged(APPLICATION_ID, user 0)` after every
 * `BaseActivity.onResume` so a module icon swap shows up without a reboot.
 * The call is wrapped in a catch-all because the resume hook is registered
 * before the model capture - a resume that beats the first `LauncherModel`
 * construction throws reflectively and is swallowed.
 */
@LauncherModPack
class PixelXpertIconUpdater(context: Context) : XposedModPack(context) {
	private var LauncherModel: Any? = null

	override fun onPreferenceUpdated(vararg key: String?) {}

	override fun onPackageLoaded(PRParam: XposedModuleInterface.PackageReadyParam) {
		val LauncherModelClass = ReflectedClass.of("com.android.launcher3.LauncherModel")
		val BaseActivityClass = ReflectedClass.of("com.android.launcher3.BaseActivity")

		BaseActivityClass
			.after("onResume")
			.run { param ->
				try {
					XposedHelpers.callMethod(
						LauncherModel,
						"onAppIconChanged",
						BuildConfig.APPLICATION_ID,
						UserHandle.getUserHandleForUid(0),
					)
				} catch (ignored: Throwable) {
				}
			}

		LauncherModelClass
			.afterConstruction()
			.run { param -> LauncherModel = param.thisObject }
	}
}
