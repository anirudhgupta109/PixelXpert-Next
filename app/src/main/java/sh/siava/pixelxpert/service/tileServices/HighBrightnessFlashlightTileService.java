package sh.siava.pixelxpert.service.tileServices;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.lang.ref.WeakReference;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.utils.PXPreferences;

public class HighBrightnessFlashlightTileService extends TileService {
	private static final String TAG = "PixelXpert-HBTorchTile";
	private boolean mLastEnabled = false;
	private static WeakReference<HighBrightnessFlashlightTileService> instance;

	public HighBrightnessFlashlightTileService()
	{
		Log.d(TAG, "HighBrightnessFlashlightTileService instantiated");
		instance = new WeakReference<>(this);
	}
	
	@Override
	public void onStartListening() {
		super.onStartListening();
		Log.d(TAG, "onStartListening");
		updateTile();
	}

	@Override
	public void onClick() {
		Log.d(TAG, "onClick, currently enabled: " + mLastEnabled);
		setTile(!mLastEnabled); //Isn't mandatory, but without it tile click will take time to reflect on UI

		new Thread(() -> {
			PXPreferences.putBoolean("HighBrightnessFlashlightEnabled", mLastEnabled);
			Log.d(TAG, "Preference saved: " + mLastEnabled);
		}).start(); //otherwise click will be blocked until pref is saved
	}

	private void updateTile()
	{
		boolean enabled = PXPreferences.getBoolean("HighBrightnessFlashlightEnabled", false);
		Log.d(TAG, "updateTile called, preference enabled: " + enabled);
		setTile(enabled);
	}

	private void setTile(boolean enabled) {
		Log.d(TAG, "setTile called with: " + enabled);
		if(mLastEnabled == enabled) {
			Log.d(TAG, "Skipping setTile because state hasn't changed");
			// return;  <-- WAIT, I shouldn't return here if the tile hasn't been initialized yet in the UI!
		}

		mLastEnabled = enabled;

		Tile thisTile = getQsTile();
		if (thisTile == null) {
			Log.d(TAG, "getQsTile returned null!");
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
			Log.d(TAG, "Tile updated successfully");
		} catch (Exception e) {
			Log.e(TAG, "Error updating tile", e);
		}
	}

	public static void onPrefsChanged()
	{
		try
		{
			if (instance != null && instance.get() != null) {
				Log.d(TAG, "onPrefsChanged called");
				instance.get().updateTile();
			}
		}
		catch (Throwable e){
			Log.e(TAG, "Error in onPrefsChanged", e);
		}
	}
}
