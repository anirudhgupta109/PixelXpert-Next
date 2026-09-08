package sh.siava.pixelxpert.service.tileServices;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.lang.ref.WeakReference;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.utils.PXPreferences;

public class HighBrightnessFlashlightTileService extends TileService {
	private boolean mLastEnabled = false;
	private static WeakReference<HighBrightnessFlashlightTileService> instance;

	public HighBrightnessFlashlightTileService()
	{
		instance = new WeakReference<>(this);
	}
	
	@Override
	public void onStartListening() {
		super.onStartListening();
		updateTile();
	}

	@Override
	public void onClick() {
		setTile(!mLastEnabled); //Isn't mandatory, but without it tile click will take time to reflect on UI

		new Thread(() -> {
			PXPreferences.putBoolean("HighBrightnessFlashlightEnabled", mLastEnabled);
		}).start(); //otherwise click will be blocked until pref is saved
	}

	private void updateTile()
	{
		boolean enabled = PXPreferences.getBoolean("HighBrightnessFlashlightEnabled", false);
		setTile(enabled);
	}

	private void setTile(boolean enabled) {
		mLastEnabled = enabled;

		Tile thisTile = getQsTile();
		if (thisTile == null) {
			return;
		}

		try {
			thisTile.setIcon(Icon.createWithResource(getApplicationContext(), enabled
					? R.drawable.qs_flashlight_on
					: R.drawable.qs_fashlight_on_outline));

			thisTile.setState(enabled
					? STATE_ACTIVE
					: STATE_INACTIVE);

			thisTile.setSubtitle(getString(enabled
					? R.string.general_on
					: R.string.general_off));

			thisTile.setLabel("High Brightness Flashlight");

			thisTile.updateTile();
		} catch (Exception ignored) {
		}
	}

	public static void onPrefsChanged()
	{
		try
		{
			if (instance != null && instance.get() != null) {
				instance.get().updateTile();
			}
		}
		catch (Throwable ignored){
		}
	}
}
