package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.app.ActionBar;
import android.widget.Switch;
import android.app.ActionBar.LayoutParams;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import static android.provider.Settings.System;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.content.BroadcastReceiver;
import android.view.MenuItem;

import com.android.settings.R;

public class PowerSavingSettings extends PreferenceActivity implements OnPreferenceChangeListener {

	private static final String TAG = "PowerSaving";

	private static final String KEY_POWER_SAVING_ON_AT = "power_saving_on_at";
	private static final String KEY_WIFI = "turn_off_wifi";
	private static final String KEY_BLUETOOTH = "turn_off_bluetooth";
	private static final String KEY_GPS = "turn_off_gps";
	private static final String KEY_SYNC = "turn_off_sync";
	private static final String KEY_DATA_CONNECTION = "turn_off_data";
	private static final String KEY_SLEEP = "turn_off_sleep";
	private static final String KEY_BRIGHTNESS = "adjust_brightness";
	private static final String KEY_INACTIVITY_SLEEP = "inactivity_sleep";

	private ListPreference mAutoRestrictListPref;
	private CheckBoxPreference mWifiCheckBox;
	private CheckBoxPreference mBtCheckBox;
	private CheckBoxPreference mGpsCheckBox;
	private CheckBoxPreference mSyncCheckBox;
	private CheckBoxPreference mDataCheckBox;
	private CheckBoxPreference mSleepCheckBox;
	private CheckBoxPreference mBrightnessCheckBox;
	
    	private  static int batteryLevel;
	
	private Preference mInactivitySleepPref;
       private ActionBar mActionBar;
       private Switch mSwitch;

	private PowerSavingSecure mSecure;
    private IntentFilter   mIntentFilter;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.power_saving_settings);

		mAutoRestrictListPref = (ListPreference) this.findPreference(KEY_POWER_SAVING_ON_AT);
		mWifiCheckBox = (CheckBoxPreference) this.findPreference(KEY_WIFI);
		mBtCheckBox = (CheckBoxPreference) this.findPreference(KEY_BLUETOOTH);
		mGpsCheckBox = (CheckBoxPreference) this.findPreference(KEY_GPS);
		mSyncCheckBox = (CheckBoxPreference) this.findPreference(KEY_SYNC);
		mDataCheckBox = (CheckBoxPreference) this.findPreference(KEY_DATA_CONNECTION);
		//mSleepCheckBox = (CheckBoxPreference) this.findPreference(KEY_SLEEP);
		mBrightnessCheckBox = (CheckBoxPreference) this.findPreference(KEY_BRIGHTNESS);
		//mInactivitySleepPref = this.findPreference(KEY_INACTIVITY_SLEEP);

		mAutoRestrictListPref.setOnPreferenceChangeListener(this);
		mWifiCheckBox.setOnPreferenceChangeListener(this);
		mBtCheckBox.setOnPreferenceChangeListener(this);
		mGpsCheckBox.setOnPreferenceChangeListener(this);
		mSyncCheckBox.setOnPreferenceChangeListener(this);
		mDataCheckBox.setOnPreferenceChangeListener(this);
		//mSleepCheckBox.setOnPreferenceChangeListener(this);
		mBrightnessCheckBox.setOnPreferenceChangeListener(this);
		//mInactivitySleepPref.setOnPreferenceChangeListener(this);
		
		mSecure = new PowerSavingSecure(this);
		updateActionBar();

		refreshStatus();
	}


    private void updateActionBar(){
        mActionBar = getActionBar();
        mActionBar.setTitle(R.string.power_saving);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP 
                | ActionBar.DISPLAY_SHOW_TITLE
                | ActionBar.DISPLAY_SHOW_CUSTOM);
        mSwitch = new Switch(this);
        mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	    		mSecure.setPowerSavingOn(isChecked);
			if(mSecure.isRestricted() != isChecked){	
	                     Intent intent = new Intent("com.sprd.intent.action.BATTERY_CHANGED");
	                      intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
	                      intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
				 intent.putExtra(BatteryManager.EXTRA_LEVEL, batteryLevel);
	                      sendBroadcast(intent);
			}
			disable(!isChecked);
            }
        });

	mActionBar.setCustomView(mSwitch, new ActionBar.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.END));
       mSwitch.setChecked(mSecure.isPowerSavingOn());

}

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            this.finish();
        }
        // TODO Auto-generated method stub
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mBatteryInfoReceiver, mIntentFilter);	
	}

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryInfoReceiver);
  }
	
	private void 	disable(boolean isChecked){
		 mWifiCheckBox.setEnabled(isChecked);
		 mBtCheckBox.setEnabled(isChecked);
		 mAutoRestrictListPref.setEnabled(isChecked);
		 mGpsCheckBox.setEnabled(isChecked);
		 mSyncCheckBox.setEnabled(isChecked);
		 mDataCheckBox.setEnabled(isChecked);
		 mBrightnessCheckBox.setEnabled(isChecked);
	}
     
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
		    batteryLevel = intent.getIntExtra("level", 0);
            }
        }
    };
	private void refreshStatus() {	
		mAutoRestrictListPref.setValue(String.valueOf(mSecure.getRestrictLevel()));
		mAutoRestrictListPref.setSummary(getResources().getString(R.string.power_saving_percent_summary,mAutoRestrictListPref.getValue(),"%%"));
		mWifiCheckBox.setChecked(mSecure.isWifiChecked());
		mBtCheckBox.setChecked(mSecure.isBluetoothChecked());
		mGpsCheckBox.setChecked(mSecure.isGpsChecked());
		mSyncCheckBox.setChecked(mSecure.isSyncChecked());
		mDataCheckBox.setChecked(mSecure.isDataConnectionChecked());
		//mSleepCheckBox.setChecked(mSecure.isSleepChecked());
		mBrightnessCheckBox.setChecked(mSecure.isBrightnessChecked());
       	 mIntentFilter = new IntentFilter();
        	mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onPreferenceChange:" + preference.toString() +" newValue:"+newValue);
		if (preference == mAutoRestrictListPref) {
			mSecure.setRestrictLevel(Integer.parseInt(newValue.toString()));
			String str = getResources().getString(R.string.power_saving_percent_summary,newValue.toString(),"%%");
			mAutoRestrictListPref.setSummary(str);
			return true;
		}

		if (preference == mWifiCheckBox) {
			mSecure.setWifiChecked((Boolean) newValue);
			return true;
		}
		if (preference == mBtCheckBox) {
			mSecure.setBluetoothChecked((Boolean) newValue);
			return true;
		}
		if (preference == mGpsCheckBox) {
			mSecure.setGpsChecked((Boolean) newValue);
			return true;
		}
		if (preference == mSyncCheckBox) {
			mSecure.setSyncChecked((Boolean) newValue);
			return true;
		}
		if (preference == mDataCheckBox) {
			mSecure.setDataConnectionChecked((Boolean) newValue);
			return true;
		}
		if (preference == mSleepCheckBox) {
			mSecure.setSleepChecked((Boolean) newValue);
			return true;
		}
		if (preference == mBrightnessCheckBox) {
			mSecure.setBrightnessChecked((Boolean) newValue);
			return true;
		}
		return false;

	}
}
