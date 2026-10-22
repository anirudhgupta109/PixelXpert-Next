package sh.anirudhgupta109.pxn.xposed.modpacks.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.github.libxposed.api.XposedModuleInterface
import sh.anirudhgupta109.pxn.Constants
import sh.anirudhgupta109.pxn.util.AppUtils
import sh.anirudhgupta109.pxn.xposed.XposedModPack
import sh.anirudhgupta109.pxn.xposed.annotations.SystemUIModPack

/**
 * Lets the module app ask SystemUI - the process that is already alive in the
 * background on every screen - to bring up the KernelSU root prompt on its
 * behalf.
 *
 * The pack registers an exported receiver for
 * [Constants.ACTION_KSU_ACQUIRE_ROOT]; when that broadcast arrives,
 * [AppUtils.runKSURootActivity] runs from inside SystemUI so the request is
 * fired from a system process rather than from the app's own task.
 */
@SystemUIModPack
class KSURootReceiver(context: Context) : XposedModPack(context) {

	private var broadcastReceiver: BroadcastReceiver? = null

	/** Nothing this pack reads; declared because [XposedModPack] requires it. */
	override fun onPreferenceUpdated(vararg key: String?) {
	}

	override fun onPackageLoaded(PRParam: XposedModuleInterface.PackageReadyParam) {
		val receiver = object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent?) {
				AppUtils.runKSURootActivity(mContext, false)
			}
		}

		val filter = IntentFilter()
		filter.addAction(Constants.ACTION_KSU_ACQUIRE_ROOT)

		mContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
		broadcastReceiver = receiver
	}

	override fun onUnloaded() {
		broadcastReceiver?.let { runCatching { mContext.unregisterReceiver(it) } }
		broadcastReceiver = null
	}
}
