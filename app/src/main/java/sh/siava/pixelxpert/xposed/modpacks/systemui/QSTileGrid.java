package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class QSTileGrid extends XposedModPack {
	private static final int NOT_SET = 0;
	private static final int QS_COL_NOT_SET = 1;

	private static int QSRowQty = NOT_SET;
	private static int QSRowQtyL = NOT_SET;
	private static int QSColQty = QS_COL_NOT_SET;
	private static int QSColQtyL = QS_COL_NOT_SET;
	private static int QQSTileRows = NOT_SET;
	private static int QQSTileRowsL = NOT_SET;

	public QSTileGrid(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return;

		QSRowQty = Xprefs.getInt("QSRowQty", NOT_SET);
		QSRowQtyL = Xprefs.getInt("QSRowQtyL", NOT_SET);
		QSColQty = Xprefs.getInt("QSColQty", QS_COL_NOT_SET);
		QSColQtyL = Xprefs.getInt("QSColQtyL", QS_COL_NOT_SET);

		QQSTileRows = Xprefs.getInt("QQSTileRows", NOT_SET);
		QQSTileRowsL = Xprefs.getInt("QQSTileRowsL", NOT_SET);
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		int qsColumnsId = mContext.getResources().getIdentifier("quick_settings_infinite_grid_num_columns", "integer", mContext.getPackageName());
		int qqsRowsId = mContext.getResources().getIdentifier("quick_qs_paginated_grid_num_rows", "integer", mContext.getPackageName());
		int qsRowsId = mContext.getResources().getIdentifier("quick_settings_paginated_grid_num_rows", "integer", mContext.getPackageName());

		XposedBridge.hookAllMethods(Resources.class, "getInteger", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (param.args == null || param.args.length == 0 || !(param.args[0] instanceof Integer)) return;
				int id = (int) param.args[0];
				
				if (id == qsColumnsId && qsColumnsId != 0) {
					boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
					if (isLandscape && QSColQtyL != QS_COL_NOT_SET) {
						param.setResult(QSColQtyL);
					} else if (!isLandscape && QSColQty != QS_COL_NOT_SET) {
						param.setResult(QSColQty);
					}
				} else if (id == qqsRowsId && qqsRowsId != 0) {
					boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
					if (isLandscape && QQSTileRowsL != NOT_SET) {
						param.setResult(QQSTileRowsL);
					} else if (!isLandscape && QQSTileRows != NOT_SET) {
						param.setResult(QQSTileRows);
					}
				} else if (id == qsRowsId && qsRowsId != 0) {
					boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
					if (isLandscape && QSRowQtyL != NOT_SET) {
						param.setResult(QSRowQtyL);
					} else if (!isLandscape && QSRowQty != NOT_SET) {
						param.setResult(QSRowQty);
					}
				}
			}
		});
	}
}
