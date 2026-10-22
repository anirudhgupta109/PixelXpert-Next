package sh.anirudhgupta109.pxn.ui.screens

import sh.anirudhgupta109.pxn.R
import sh.anirudhgupta109.pxn.ui.prefs.PrefScreen
import sh.anirudhgupta109.pxn.ui.prefs.node.Illustration
import sh.anirudhgupta109.pxn.ui.prefs.node.NavRow
import sh.anirudhgupta109.pxn.ui.prefs.node.SwitchPref

val theming_prefs: PrefScreen = PrefScreen(
	key = "theming_prefs",
	nodes = listOf(
		Illustration(key = "theme_illustration", lottieRawRes = R.raw.theme, ignore = true),
		NavRow(key = "icon_packs", title = R.string.icon_packs_title, route = "icon_pack"),
		SwitchPref(
			key = "ForceThemedLauncherIcons",
			title = R.string.force_themed_icons_title,
			summary = R.string.force_themed_icons_summary,
			default = "false",
		),
	),
)
