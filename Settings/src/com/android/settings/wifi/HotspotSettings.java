/** Created by Spreadst */

package com.android.settings.wifi;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class HotspotSettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener {

    private static final String TAG = "HotspotSettings";

    private static final String HOTSPOT_SSID_AND_SECURITY = "hotspot_ssid_and_security";
    private static final String HOTSPOT_CONNECTED_STATIONS = "hotspot_connected_stations";
    private static final String HOTSPOT_NO_CONNECTED_STATION = "hotspot_no_connected_station";
    private static final String HOTSPOT_BLOCKED_STATIONS = "hotspot_blocked_stations";
    private static final String HOTSPOT_NO_BLOCKED_STATION = "hotspot_no_blocked_station";
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;
    private static final int DIALOG_AP_SETTINGS = 1;

    public static final String STATIONS_STATE_CHANGED_ACTION = "com.sprd.settings.STATIONS_STATE_CHANGED";

    private String[] mSecurityType;
    private Preference mCreateNetwork;
    private PreferenceCategory mConnectedStationsCategory;
    private Preference mHotspotNoConnectedStation;
    private PreferenceCategory mBlockedStationsCategory;
    private Preference mHotspotNoBlockedStations;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig;
    private StateReceiver mStateReceiver;

    private HotspotEnabler mHotspotEnabler;

    private boolean supportBtWifiSoftApCoexit = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.hotspot_settings);

        mConnectedStationsCategory = (PreferenceCategory) findPreference(HOTSPOT_CONNECTED_STATIONS);
        mBlockedStationsCategory = (PreferenceCategory) findPreference(HOTSPOT_BLOCKED_STATIONS);
        mHotspotNoConnectedStation = (Preference) findPreference(HOTSPOT_NO_CONNECTED_STATION);
        mHotspotNoBlockedStations = (Preference) findPreference(HOTSPOT_NO_BLOCKED_STATION);

        final Activity activity = getActivity();
        Switch actionBarSwitch = new Switch(activity);
        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders()
                    || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources()
                        .getDimensionPixelSize(
                                R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(
                        ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(
                        actionBarSwitch,
                        new ActionBar.LayoutParams(
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
        }
        mHotspotEnabler = new HotspotEnabler(activity, actionBarSwitch);

        if (SystemProperties.get("ro.btwifisoftap.coexist", "true").equals(
                "false")) {
            supportBtWifiSoftApCoexit = false;
        }

        initWifiTethering();
    }

    @Override
    public void onResume() {
        super.onResume();
        mHotspotEnabler.resume();
        updateStations();

        mStateReceiver = new StateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_AP_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiManager.SOFTAP_BLOCKLIST_AVAILABLE_ACTION);
        filter.addAction(STATIONS_STATE_CHANGED_ACTION);
        getActivity().registerReceiver(mStateReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHotspotEnabler.pause();
        getActivity().unregisterReceiver(mStateReceiver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen,
            Preference preference) {
        if (preference == mCreateNetwork) {
            showDialog(DIALOG_AP_SETTINGS);
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            return mDialog;
        }

        return null;
    }

    private void initWifiTethering() {
        final Activity activity = getActivity();
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);

        mCreateNetwork = findPreference(HOTSPOT_SSID_AND_SECURITY);

        if (mWifiConfig == null) {
             String s = activity
                    .getString(com.android.internal.R.string.wifi_tether_configure_ssid_default);
            //revo lyq 20140418  for wifi_tether_configure_ssid_default
            //lyx wifiap model
            s=android.os.SystemProperties.get("ro.macro.wifiap_model");           
            if(s.equals("")){
 	            s=android.os.SystemProperties.get("ro.product.model"); 
            }            
            
            mCreateNetwork.setSummary(String.format(
                    activity.getString(CONFIG_SUBTEXT), s,
                    mSecurityType[WifiApDialog.OPEN_INDEX]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            mCreateNetwork.setSummary(String.format(
                    activity.getString(CONFIG_SUBTEXT), mWifiConfig.SSID,
                    mSecurityType[index]));
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is stopped, bring up else restart with new config
                 * TODO: update config on a running access point when framework
                 * support is added
                 */
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    if (!supportBtWifiSoftApCoexit) {
                        Settings.Global.putInt(getContentResolver(),
                                Settings.Global.SOFTAP_REENABLING,
                                1);
                    }
                    mWifiManager.setWifiApEnabled(null, false);
                    mWifiManager.setWifiApEnabled(mWifiConfig, true);
                } else {
                    mWifiManager.setWifiApConfiguration(mWifiConfig);
                }
                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                mCreateNetwork.setSummary(String.format(getActivity()
                        .getString(CONFIG_SUBTEXT), mWifiConfig.SSID,
                        mSecurityType[index]));
            }
        }
    }

    private void updateStations() {
        addConnectedStations();
        addBlockedStations();
    }

    private void addConnectedStations() {
        String mConnectedStationsStr = mWifiManager.softApGetConnectedStations();
        Log.d(TAG, "mConnectedStationsStr = " + mConnectedStationsStr);
        mConnectedStationsCategory.removeAll();
        if (mConnectedStationsStr == null || mConnectedStationsStr.length() == 0) {
            mConnectedStationsCategory.addPreference(mHotspotNoConnectedStation);
            return;
        }

        String[] mConnectedStations = mConnectedStationsStr.split(" ");
        for (String station : mConnectedStations) {
            mConnectedStationsCategory.addPreference(new Station(getActivity(), station, true));
        }
    }

    private void addBlockedStations() {
        String mBlockedStationsStr = mWifiManager.softApGetBlockedStations();
        Log.d(TAG, "mBlockedStationsStr = " + mBlockedStationsStr);
        mBlockedStationsCategory.removeAll();
        if (mBlockedStationsStr == null || mBlockedStationsStr.length() == 0) {
            mBlockedStationsCategory.addPreference(mHotspotNoBlockedStations);
            return;
        }

        String[] mBlockedStations = mBlockedStationsStr.split(" ");
        for (String station : mBlockedStations) {
            mBlockedStationsCategory.addPreference(new Station(getActivity(), station, false));
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.WIFI_AP_CONNECTION_CHANGED_ACTION)
                    || action.equals(WifiManager.SOFTAP_BLOCKLIST_AVAILABLE_ACTION)
                    || action.equals(STATIONS_STATE_CHANGED_ACTION)) {
                updateStations();
            } else if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                int hotspotState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE,
                        WifiManager.WIFI_AP_STATE_FAILED);
                if (hotspotState != WifiManager.WIFI_AP_STATE_ENABLED) {
                    mConnectedStationsCategory.removeAll();
                    mConnectedStationsCategory.addPreference(mHotspotNoConnectedStation);
                    mBlockedStationsCategory.removeAll();
                    mBlockedStationsCategory.addPreference(mHotspotNoBlockedStations);
                } else {
                    updateStations();
                }
            }
        }
    }
}
