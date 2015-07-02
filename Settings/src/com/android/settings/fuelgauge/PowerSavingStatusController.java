package com.android.settings.fuelgauge;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class PowerSavingStatusController {

    public static final String POWERSAVER_ENABLE_PROPERTY = "persist.sys.powersaver.enable";

    /** Broadcast intent action when the location mode is about to change. */
    private static final String MODE_CHANGING_ACTION = "com.android.settings.location.MODE_CHANGING";
    private static final String CURRENT_MODE_KEY = "CURRENT_MODE";
    private static final String NEW_MODE_KEY = "NEW_MODE";

    private Context mContext;

    public PowerSavingStatusController(Context context) {
        this.mContext = context;
    }
	
    public boolean isAirplaneModeOn() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    public void updateWifiStatus() {
        if (isAirplaneModeOn()) {
            return;
        }
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        switch (wifiState) {
        case WifiManager.WIFI_STATE_ENABLED:
        case WifiManager.WIFI_STATE_ENABLING:
            wifiManager.setWifiEnabled(false);
            break;
        case WifiManager.WIFI_STATE_DISABLED:
        case WifiManager.WIFI_STATE_UNKNOWN:
        case WifiManager.WIFI_STATE_DISABLING:
        default:
            break;
        }
    }

    public void updateBluetoothStatus() {
        if (isAirplaneModeOn()) {
            return;
        }
        BluetoothAdapter localAdapter = BluetoothAdapter.getDefaultAdapter();
        int bluetoothState;

        if (localAdapter == null) {
            return;
        }

        bluetoothState = localAdapter.getState();
         if (localAdapter.isEnabled()) {
             localAdapter.disable();
         }
        switch (bluetoothState) {
        case BluetoothAdapter.STATE_TURNING_ON:
        case BluetoothAdapter.STATE_ON:
            // will open powersavermode
            localAdapter.disable();
            break;

        case BluetoothAdapter.STATE_TURNING_OFF:
        case BluetoothAdapter.STATE_OFF:
        default:
            break;
        }
    }

    public void updateGpsStatus() {

        int currentMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        Intent intent = new Intent(MODE_CHANGING_ACTION);
        intent.putExtra(CURRENT_MODE_KEY, currentMode);
        intent.putExtra(NEW_MODE_KEY, Settings.Secure.LOCATION_MODE_OFF);
        mContext.sendBroadcast(intent, android.Manifest.permission.WRITE_SECURE_SETTINGS);
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF);

    }

    public void updateSyncStatus() {
        // ConnectivityManager connManager = (ConnectivityManager) mContext
        // .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean sync = ContentResolver.getMasterSyncAutomatically();
        if (sync) {
            ContentResolver.setMasterSyncAutomatically(false);
        }
    }
    
    public void updateDataConnectionStatus(){
    	if (isAirplaneModeOn()) {
			return;
		}
    	
		int defaultId = TelephonyManager.getDefaultDataPhoneId(mContext);

		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (TelephonyManager.isMultiSim()) {
			if (cm.getMobileDataEnabledByPhoneId(defaultId))
				cm.setMobileDataEnabledByPhoneId(defaultId, false);
		} else {
			if (cm.getMobileDataEnabled())
				cm.setMobileDataEnabled(false);
		}

    }

    public void updateSleepStatus(boolean b) {

    }

    public void updateBrightnessStatus() {
        // boolean autoModeAvailable = mContext.getResources().getBoolean(
        // com.android.internal.R.bool.config_automatic_brightness_available);
        //
        int brightness = mContext.getResources().getInteger(
                com.android.internal.R.integer.powersaving_default_min_brightness);

        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        int mObrightness = getBrightness();
        mObrightness = (mObrightness * 100) / 255;
        if (brightness < mObrightness) {
            setBrightness(brightness);
        }

	
    }

    // return backlight brightness
    private int getBrightness() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 0);
    }

    private void setBrightness(int brightness) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
    }
}
