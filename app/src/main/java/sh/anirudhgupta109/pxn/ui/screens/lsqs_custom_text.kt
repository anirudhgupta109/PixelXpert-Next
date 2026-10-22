package sh.anirudhgupta109.pxn.ui.screens

import sh.anirudhgupta109.pxn.R
import sh.anirudhgupta109.pxn.ui.prefs.PrefScreen
import sh.anirudhgupta109.pxn.ui.prefs.node.ColorPref
import sh.anirudhgupta109.pxn.ui.prefs.node.Illustration
import sh.anirudhgupta109.pxn.ui.prefs.node.ListPref
import sh.anirudhgupta109.pxn.ui.prefs.node.SliderPref
import sh.anirudhgupta109.pxn.ui.prefs.node.SwitchPref
import sh.anirudhgupta109.pxn.ui.prefs.node.TimePref

val lsqs_custom_text: PrefScreen = PrefScreen(
	key = "lsqs_custom_text",
	nodes = listOf(
		Illustration(key = "statistics_illustration", lottieRawRes = R.raw.statistics, ignore = true),
		ListPref(
			key = "NetworkStatsStartBase",
			title = R.string.network_stat_type,
			default = "0",
			entries = R.array.network_stat_start_names,
			entryValues = R.array.network_stat_start_codes,
		),
		TimePref(key = "NetworkStatsStartTime", title = R.string.network_stat_day, default = "00:00"),
		ListPref(
			key = "NetworkStatsWeekStart",
			title = R.string.network_stat_week,
			default = "1",
			entries = R.array.days_of_week_names,
			entryValues = R.array.days_of_week_codes,
		),
		SliderPref(
			key = "NetworkStatsMonthStart",
			title = R.string.network_stat_month,
			default = "1",
			min = 1f,
			max = 28f,
			step = 1f,
			valueCount = 1,
		),
		SwitchPref(
			key = "networkStatsColorful",
			title = R.string.network_colorful_title,
			default = "false",
			summaryOn = R.string.general_on,
			summaryOff = R.string.general_off,
		),
		ColorPref(key = "networkStatDLColor", title = R.string.network_download_color_title, default = "-7617718"),
		ColorPref(key = "networkStatULColor", title = R.string.network_upload_color_title, default = "-351845"),
	),
)
