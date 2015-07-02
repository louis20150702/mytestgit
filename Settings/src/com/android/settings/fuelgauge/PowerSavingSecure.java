package com.android.settings.fuelgauge;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class PowerSavingSecure {

    private Context mContext;

    private int mDefaultStatus;
    private int mDefaultRestrictLevel;

    public PowerSavingSecure(Context context) {
        this.mContext = context;
        mDefaultStatus = mContext.getResources().getInteger(
                com.android.internal.R.integer.powersaving_default_on);
        mDefaultRestrictLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.powersaving_default_restrict_level);
    }

    public boolean isWifiChecked() {
        return getSecureInt(Settings.Secure.POWERSAVING_WIFI_STATUS) != 0;
    }

    public boolean isBluetoothChecked() {
        return getSecureInt(Settings.Secure.POWERSAVING_BLUETOOTH_STATUS) != 0;
    }

    public boolean isGpsChecked() {
        return getSecureInt(Settings.Secure.POWERSAVING_GPS_STATUS) != 0;
    }

    public boolean isSyncChecked() {
        return getSecureInt(Settings.Secure.POWERSAVING_SYNC_STATUS) != 0;
    }

    public boolean isSleepChecked() {
        return getSecureInt(Settings.Secure.POWERSAVING_SLEEP_STATUS) != 0;
    }

    public boolean isBrightnessChecked() {
        return getSecureInt(Settings.Secure.POWERSAVING_BRIGHTNESS_STATUS) != 0;
    }
    public boolean isDataConnectionChecked(){
    	return getSecureInt(Settings.Secure.POWERSAVING_DATA_CONNECTION_STATUS) != 0;
    }

    public void setWifiChecked(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_WIFI_STATUS, b);
    }

    public void setBluetoothChecked(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_BLUETOOTH_STATUS, b);
    }

    public void setGpsChecked(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_GPS_STATUS, b);
    }

    public void setSyncChecked(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_SYNC_STATUS, b);
    }

    public void setSleepChecked(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_SLEEP_STATUS, b);
    }

    public void setBrightnessChecked(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_BRIGHTNESS_STATUS, b);
    }
    public void setDataConnectionChecked(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_DATA_CONNECTION_STATUS, b);
    }
    

    public void setRestrictLevel(int level) {
        updateSecureString(Settings.Secure.POWERSAVING_AUTO_RESTRICT_LEVEL, String.valueOf(level));
    }

    public int getRestrictLevel() {
        String level = getSecureString(Settings.Secure.POWERSAVING_AUTO_RESTRICT_LEVEL);
        if (level == null) {
            return mDefaultRestrictLevel;
        }
        return Integer.parseInt(level);
    }

    public void setPowerSavingOn(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_MODE_ON, b);
    }

    public boolean isPowerSavingOn() {
        return getSecureInt(Settings.Secure.POWERSAVING_MODE_ON) != 0;
    }

    public void setRestrictStatus(boolean b) {
        updateSecureInt(Settings.Secure.POWERSAVING_RESTRICT_STATUS, b);
    }

    public boolean isRestricted() {
        try {
            int value = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.POWERSAVING_RESTRICT_STATUS);
            return value == 0 ? false : true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    private void updateSecureInt(String key, boolean b) {
        Settings.Secure.putInt(mContext.getContentResolver(), key, b ? 1 : 0);
    }

    private int getSecureInt(String key) {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(), key);
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return mDefaultStatus;
        }
    }

    private void updateSecureString(String key, String value) {
        Settings.Secure.putString(mContext.getContentResolver(), key, value);
    }

    private String getSecureString(String key) {
        return Settings.Secure.getString(mContext.getContentResolver(), key);
    }

}
