package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;





import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.toolkit.ComposeFontUtils;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class QSTileGrid extends XposedModPack {
	private static final int NOT_SET = 0;
	private static final int QS_COL_NOT_SET = 1;

	private static int QSRowQty = NOT_SET;
	private static int QSColQty = QS_COL_NOT_SET;

	private static int QSRowQtyL = NOT_SET;
	private static int QSColQtyL = QS_COL_NOT_SET;

	private static float QSLabelScaleFactor = 1, QSSecondaryLabelScaleFactor = 1;

	private static int QQSTileRows = NOT_SET;
	private static int QQSTileRowsL = NOT_SET;

	public QSTileGrid(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return;

		if(Key.length > 0 && Key[0].equals("VerticalQSTile"))
		{
			SystemUtils.doubleToggleDarkMode();
		}

		QSRowQty = Xprefs.getSliderInt( "QSRowQty", NOT_SET);
		QSColQty = Xprefs.getSliderInt( "QSColQty", QS_COL_NOT_SET);
		if(QSColQty < QS_COL_NOT_SET) QSColQty = QS_COL_NOT_SET;

		QSRowQtyL = Xprefs.getSliderInt( "QSRowQtyL", NOT_SET);
		QSColQtyL = Xprefs.getSliderInt( "QSColQtyL", QS_COL_NOT_SET);
		if(QSColQtyL < QS_COL_NOT_SET) QSColQtyL = QS_COL_NOT_SET;

		QQSTileRows = Xprefs.getSliderInt( "QQSRows", NOT_SET);
		QQSTileRowsL = Xprefs.getSliderInt( "QQSRowsL", NOT_SET);

		QSLabelScaleFactor = (Xprefs.getSliderFloat( "QSLabelScaleFactor", 0) + 100) / 100f;
		QSSecondaryLabelScaleFactor = (Xprefs.getSliderFloat( "QSSecondaryLabelScaleFactor", 0) + 100) / 100f;

		if (Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty") || Key[0].equals("QQSTileQty") || Key[0].equals("QSRowQtyL") || Key[0].equals("QSColQtyL") || Key[0].equals("QQSTileQtyL") || Key[0].equals("QQSRows"))) {
			SystemUtils.doubleToggleDarkMode();
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		ReflectedClass PaginatedGridLayoutClass = ReflectedClass.ofIfPossible("com.android.systemui.qs.panels.ui.compose.PaginatedGridLayout");
		ReflectedClass.of(Resources.class).before("getInteger").run(param -> {
			Resources resources = param.getThisObject();
			int id = param.getArg(0);
			String resourceName;
			try {
				resourceName = resources.getResourceName(id);
			} catch (Resources.NotFoundException ignored) {
				return;
			}
			boolean isLandscape = resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
			if (resourceName.endsWith("quick_settings_infinite_grid_num_columns")) {
				int columns = isLandscape ? QSColQtyL : QSColQty;
				if (columns != QS_COL_NOT_SET) param.setResult(columns);
			} else if (resourceName.endsWith("quick_qs_paginated_grid_num_rows")) {
				int rows = isLandscape ? QQSTileRowsL : QQSTileRows;
				if (rows != NOT_SET) param.setResult(rows);
			}
		});
		ReflectedClass CommonTileKtClass = ReflectedClass.ofIfPossible("com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileKt");

		//region expressive compose UI rows
		AtomicReference<Set<XposedInterface.HookHandle>> QSRowsHooks = new AtomicReference<>();

		PaginatedGridLayoutClass
				.before("TileGrid")
				.run(param ->
						QSRowsHooks.set(ReflectedClass.of(Resources.class)
								.before("getInteger")
								.run(param1 -> {
									if(param1.args[0].equals(mContext.getResources().getIdentifier("quick_settings_paginated_grid_num_rows", "integer", mContext.getPackageName()))) {
										boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

										if(isLandscape && QSRowQtyL != NOT_SET)
										{
											param1.setResult(QSRowQtyL);
										}
										if(!isLandscape && QSRowQty != NOT_SET)
										{
											param1.setResult(QSRowQty);
										}
									}
								})));

		//region expressive compose tile label size
		CommonTileKtClass
				.before("TileLabel")
				.run(param -> {
					Object spanStyle = getObjectField(param.args[2], "spanStyle");

					if(Boolean.valueOf(true).equals(getAdditionalInstanceField(param.args[2], "Scaled")))
					{
						return;
					}

					setAdditionalInstanceField(param.args[2], "Scaled", true);

					long longFontSize = (long) getObjectField(spanStyle, "fontSize");

					setObjectField(spanStyle, "fontSize", ComposeFontUtils.Companion.scaleTileFont(mContext, longFontSize, QSLabelScaleFactor, QSSecondaryLabelScaleFactor));
				});
		//endregion

		PaginatedGridLayoutClass
				.after("TileGrid")
				.run(param -> {
					Set<XposedInterface.HookHandle> handles = QSRowsHooks.getAndSet(null);
					if (handles != null) handles.forEach(XposedInterface.HookHandle::unhook);
				});
		//endregion
	}
}