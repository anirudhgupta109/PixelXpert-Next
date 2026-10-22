package sh.anirudhgupta109.pxn.ui.screens

import sh.anirudhgupta109.pxn.R
import sh.anirudhgupta109.pxn.ui.prefs.PrefScreen
import sh.anirudhgupta109.pxn.ui.prefs.node.Illustration
import sh.anirudhgupta109.pxn.ui.prefs.node.SliderPref
import sh.anirudhgupta109.pxn.ui.prefs.node.SwitchPref

val hotspot_prefs: PrefScreen = PrefScreen(
	key = "hotspot_prefs",
	nodes = listOf(
		Illustration(key = "hotspot_illustration", lottieRawRes = R.raw.hotspot, ignore = true),
		SwitchPref(
			key = "hotSpotHideSSID",
			title = R.string.hotspot_hide_ssid_title,
			default = "false",
			summaryOn = R.string.general_on,
			summaryOff = R.string.general_off,
		),
		SliderPref(
			key = "hotSpotTimeoutSecs",
			title = R.string.hotspot_inactivity_timeout_title,
			default = "0",
			min = 0f,
			max = 7200f,
			step = 60f,
			valueCount = 1,
		),
		SliderPref(
			key = "hotSpotMaxClients",
			title = R.string.hotspot_max_client_title,
			default = "0",
			min = 0f,
			max = 15f,
			step = 1f,
			valueCount = 1,
		),
		SwitchPref(
			key = "hotspotDisableApproval",
			title = R.string.hotspot_disable_approval_title,
			summary = R.string.hotspot_disable_approval_summary,
			default = "false",
		),
		SwitchPref(
			key = "force_hotspot",
			title = R.string.hotspot_force_unsupported_regions,
			default = "false",
			summaryOn = R.string.general_on,
			summaryOff = R.string.general_off,
		),
	),
)
