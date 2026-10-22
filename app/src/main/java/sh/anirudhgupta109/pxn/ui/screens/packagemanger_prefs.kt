package sh.anirudhgupta109.pxn.ui.screens

import sh.anirudhgupta109.pxn.R
import sh.anirudhgupta109.pxn.ui.prefs.PrefScreen
import sh.anirudhgupta109.pxn.ui.prefs.node.Illustration
import sh.anirudhgupta109.pxn.ui.prefs.node.SwitchPref

val packagemanger_prefs: PrefScreen = PrefScreen(
	key = "packagemanger_prefs",
	nodes = listOf(
		Illustration(key = "package_manager_illustration", lottieRawRes = R.raw.package_manager, ignore = true),
		SwitchPref(
			key = "PM_AllowMismatchedSignature",
			title = R.string.pm_bypass_signature_title,
			default = "false",
			summaryOn = R.string.auto_off_five_minutes,
			summaryOff = R.string.general_off,
		),
		SwitchPref(
			key = "PM_AllowDowngrade",
			title = R.string.pm_allow_downgrade_title,
			default = "false",
			summaryOn = R.string.auto_off_five_minutes,
			summaryOff = R.string.general_off,
		),
	),
)
