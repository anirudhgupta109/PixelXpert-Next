package sh.anirudhgupta109.pxn.xposed.modpacks.dialer

import android.content.Context
import io.github.libxposed.api.XposedModuleInterface
import sh.anirudhgupta109.pxn.xposed.XPLauncher
import sh.anirudhgupta109.pxn.xposed.XPrefs.Xprefs
import sh.anirudhgupta109.pxn.xposed.XposedModPack
import sh.anirudhgupta109.pxn.xposed.annotations.DialerModPack
import sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat.AllContactsRecordingCompat
import sh.anirudhgupta109.pxn.xposed.utils.SystemUtils

@DialerModPack
class AutomaticRecording(context: Context) : XposedModPack(context) {
    private var enabled = false

    override fun onPreferenceUpdated(vararg key: String?) {
        if (key.any { it == "DialerRecordAllContacts" }) SystemUtils.killSelf()
        enabled = Xprefs.getBoolean("DialerRecordAllContacts", false)
    }

    override fun onPackageLoaded(PRParam: XposedModuleInterface.PackageReadyParam) {
        if (!enabled || PRParam.packageName != "com.google.android.dialer") return
        XPLauncher.instance?.let { xposed ->
            AllContactsRecordingCompat.install(mContext, PRParam.classLoader, xposed)
        }
    }
}
