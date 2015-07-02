/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ListView;

import com.android.internal.util.ArrayUtils;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.ToggleAccessibilityServicePreferenceFragment;
import com.android.settings.accessibility.ToggleCaptioningPreferenceFragment;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.AuthenticatorHelper;
import com.android.settings.accounts.ManageAccountsSettings;
import com.android.settings.applications.AppOpsSummary;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.bluetooth.BluetoothEnabler;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.deviceinfo.Memory;
import com.android.settings.deviceinfo.UsbSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintServiceSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.HotspotSettings;
import com.android.settings.wifi.WifiEnabler;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;

//revo lyq 2014 for advan settings
import android.bluetooth.BluetoothAdapter;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.featureoption.FeatureOption;

/* smart_wake {@ */
import static com.sprd.android.config.OptConfig.SMART_WAKE;
import static android.provider.Settings.System;

/* @} */
/*lyx 20150320 power_saving */
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.provider.Settings.SettingNotFoundException;
import static android.provider.Settings.Secure;
import android.os.BatteryManager;
/**
 * Top-level settings activity to handle single pane and double pane UI layout.
 */
public class Settings extends PreferenceActivity
        implements ButtonBarHandler, OnAccountsUpdateListener {

    private static final String LOG_TAG = "Settings";

    private static final String META_DATA_KEY_HEADER_ID =
        "com.android.settings.TOP_LEVEL_HEADER_ID";
    private static final String META_DATA_KEY_FRAGMENT_CLASS =
        "com.android.settings.FRAGMENT_CLASS";
    private static final String META_DATA_KEY_PARENT_TITLE =
        "com.android.settings.PARENT_FRAGMENT_TITLE";
    private static final String META_DATA_KEY_PARENT_FRAGMENT_CLASS =
        "com.android.settings.PARENT_FRAGMENT_CLASS";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private static final String SAVE_KEY_CURRENT_HEADER = "com.android.settings.CURRENT_HEADER";
    private static final String SAVE_KEY_PARENT_HEADER = "com.android.settings.PARENT_HEADER";

    // if we do not support backup,the text "backup and reset" change.
    private static final String GSETTINGS_PROVIDER = "com.google.settings";
    public static boolean UNIVERSEUI_SUPPORT = SystemProperties.getBoolean("universe_ui_support",false);
    public static final boolean CU_SUPPORT = SystemProperties.get("ro.operator").equals("cucc");

    static final int DIALOG_ONLY_ONE_HOME = 1;

    private static boolean sShowNoHomeNotice = false;

    private String mFragmentClass;
    private int mTopLevelHeaderId;
    private Header mFirstHeader;
    private Header mCurrentHeader;
    private Header mParentHeader;
    private boolean mInLocalHeaderSwitch;
    /* SPRD: add for tab style @{ */
    private ActionBar mActionBar;
    private int mCurrentTabIndex = 0;
    private View mView;
    public static LayoutInflater mInflater;
    int mHeadersCategory = R.xml.settings_headers_uui;
    private int[] mTabTitle = new int[] {
            R.string.network_settings_tab,
            R.string.device_settings_tab,
            R.string.personal_settings_tab,
            R.string.more_settings_tab
    };
    /* @} */
    // Show only these settings for restricted users
    private int[] SETTINGS_FOR_RESTRICTED = {
            R.id.wireless_section,
            R.id.wifi_settings,
            R.id.bluetooth_settings,
            R.id.data_usage_settings,
            R.id.wireless_settings,
            R.id.device_section,
            R.id.sound_settings,
            R.id.display_settings,
            R.id.storage_settings,
            R.id.application_settings,
            R.id.battery_settings,
            R.id.personal_section,
            R.id.location_settings,
            R.id.security_settings,
            R.id.language_settings,
            R.id.user_settings,
            R.id.account_settings,
            R.id.account_add,
            R.id.system_section,
            R.id.date_time_settings,
            R.id.about_settings,
            R.id.accessibility_settings,
            R.id.print_settings,
            R.id.nfc_payment_settings,
            R.id.home_settings,
            /* SPRD: add for uui style 335009 @{ */
            R.id.uninstall_settings
            /* @} */
    };

    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;

    // TODO: Update Call Settings based on airplane mode state.

    protected HashMap<Integer, Integer> mHeaderIndexMap = new HashMap<Integer, Integer>();

    private AuthenticatorHelper mAuthenticatorHelper;
    private Header mLastHeader;
    private boolean mListeningToAccountUpdates;
	
/*lyx 20150320 power_saving */
    private  static int batteryLevel;
	
    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean batteryPresent = Utils.isBatteryPresent(intent);
				
		/*lyx 20150320 power_saving */		
		    batteryLevel = intent.getIntExtra("level", 0);
		
                if (mBatteryPresent != batteryPresent) {
                    mBatteryPresent = batteryPresent;
                    invalidateHeaders();
                }
            }
        }
    };

    //revo lyq 2014 for advan settings
    private static final boolean is_advan_settings =FeatureOption.PRJ_FEATURE_MULTI_PRJ_CUSTOMER_ADVAN_BASE;
	
    private  BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
	            Log.w(LOG_TAG, "WIFI_STATE_CHANGED_ACTION" );
					
	            ListAdapter listAdapter = getListAdapter();
	             if (listAdapter instanceof HeaderAdapter) {
		            ((HeaderAdapter) listAdapter).setWifiIcon(intent.getIntExtra(
		                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
	            }				
            }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
	            Log.w(LOG_TAG, "ACTION_STATE_CHANGED" );
					
	            ListAdapter listAdapter = getListAdapter();
	             if (listAdapter instanceof HeaderAdapter) {
		            ((HeaderAdapter) listAdapter).setBtIcon(intent.getIntExtra(
						BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
	            }	
			
            } 
        }
    };

    private boolean mVoiceCapable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mVoiceCapable = getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
        if (getIntent().hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(getIntent().getIntExtra(EXTRA_UI_OPTIONS, 0));
        }
        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(this);
        mAuthenticatorHelper.onAccountsUpdated(this, null);

        mDevelopmentPreferences = getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);

        getMetaData();
        mInLocalHeaderSwitch = true;
        super.onCreate(savedInstanceState);
        mInLocalHeaderSwitch = false;
        /**
     * SPRD:Optimization to erase the animation on click. @{
    */
        ListView list = getListView();
        list.setSelector(R.drawable.list_selector_holo_dark);
	 //revo lyq 2014 for advan settings
	 if(FeatureOption.PRJ_FEATURE_MULTI_PRJ_CUSTOMER_ADVAN_BASE){	 	
	 list.is_no_padding=true;
 	 }
	/** @} */
        if (!onIsHidingHeaders() && onIsMultiPane()) {
            highlightHeader(mTopLevelHeaderId);
            // Force the title so that it doesn't get overridden by a direct launch of
            // a specific settings screen.
            setTitle(R.string.settings_label);
        }

        // Retrieve any saved state
        if (savedInstanceState != null) {
            mCurrentHeader = savedInstanceState.getParcelable(SAVE_KEY_CURRENT_HEADER);
            mParentHeader = savedInstanceState.getParcelable(SAVE_KEY_PARENT_HEADER);
        }

        // If the current header was saved, switch to it
        if (savedInstanceState != null && mCurrentHeader != null) {
            //switchToHeaderLocal(mCurrentHeader);
            showBreadCrumbs(mCurrentHeader.title, null);
        }

        if (mParentHeader != null) {
            setParentTitle(mParentHeader.title, null, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToParent(mParentHeader.fragment);
                }
            });
        }

        // Override up navigation for multi-pane, since we handle it in the fragment breadcrumbs
        if (onIsMultiPane()) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setHomeButtonEnabled(false);
        }
        /* SPRD: add for tab style @{ */
        mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (UNIVERSEUI_SUPPORT) {
            if (this.getClass().equals(Settings.class)) {
                int index = getIntent().getIntExtra("tab_index", mCurrentTabIndex);
                setupTab();
                chooseTab(index);
            }
        }
if(FeatureOption.PRJ_FEATURE_MULTI_PRJ_TIANRUIXIANG_BASE || FeatureOption.PRJ_FEATURE_SHOW_MENU_FOR_DEVOLOPMENT_SETTINGS){
		getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW, true).apply();
}
        /* @} */
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current fragment, if it is the same as originally launched
        if (mCurrentHeader != null) {
            outState.putParcelable(SAVE_KEY_CURRENT_HEADER, mCurrentHeader);
        }
        if (mParentHeader != null) {
            outState.putParcelable(SAVE_KEY_PARENT_HEADER, mParentHeader);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                invalidateHeaders();
            }
        };
        mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);
        // SPRD:ADD register receiver.
        ListAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof HeaderAdapter) {
            // add for tab style
            ((HeaderAdapter) listAdapter).flushViewCache();
            // add for tab style
            ((HeaderAdapter) listAdapter).resume();
        }
        // SPRD:ADD update Header
        invalidateHeaders();

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //revo lyq 2014 for advan settings
        if(is_advan_settings){
          IntentFilter  mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
          mIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
          registerReceiver(mReceiver, mIntentFilter);
        }


    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(mBatteryInfoReceiver);
		
		//revo lyq 2014 for advan settings   
        if(is_advan_settings){     
            unregisterReceiver(mReceiver);	
        }

        ListAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof HeaderAdapter) {
            ((HeaderAdapter) listAdapter).pause();
        }

        mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);
        mDevelopmentPreferencesListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mListeningToAccountUpdates) {
            AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    private static final String[] ENTRY_FRAGMENTS = {
        WirelessSettings.class.getName(),
        WifiSettings.class.getName(),
        AdvancedWifiSettings.class.getName(),
        BluetoothSettings.class.getName(),
        TetherSettings.class.getName(),
        HotspotSettings.class.getName(),
        WifiP2pSettings.class.getName(),
        VpnSettings.class.getName(),
        DateTimeSettings.class.getName(),
        LocalePicker.class.getName(),
        InputMethodAndLanguageSettings.class.getName(),
        SpellCheckersSettings.class.getName(),
        UserDictionaryList.class.getName(),
        UserDictionarySettings.class.getName(),
        SoundSettings.class.getName(),
        DisplaySettings.class.getName(),
        DeviceInfoSettings.class.getName(),
        ManageApplications.class.getName(),
        ProcessStatsUi.class.getName(),
        NotificationStation.class.getName(),
        LocationSettings.class.getName(),
        SecuritySettings.class.getName(),
        PrivacySettings.class.getName(),
        DeviceAdminSettings.class.getName(),
        AccessibilitySettings.class.getName(),
        ToggleCaptioningPreferenceFragment.class.getName(),
        TextToSpeechSettings.class.getName(),
        Memory.class.getName(),
        DevelopmentSettings.class.getName(),
        UsbSettings.class.getName(),
        AndroidBeam.class.getName(),
        WifiDisplaySettings.class.getName(),
        PowerUsageSummary.class.getName(),
        AccountSyncSettings.class.getName(),
        CryptKeeperSettings.class.getName(),
        DataUsageSummary.class.getName(),
        DreamSettings.class.getName(),
        UserSettings.class.getName(),
        NotificationAccessSettings.class.getName(),
        ManageAccountsSettings.class.getName(),
        PrintSettingsFragment.class.getName(),
        PrintJobSettingsFragment.class.getName(),
        TrustedCredentialsSettings.class.getName(),
        PaymentSettings.class.getName(),
        KeyboardLayoutPickerFragment.class.getName()
    };

    @Override
    protected boolean isValidFragment(String fragmentName) {
        // Almost all fragments are wrapped in this,
        // except for a few that have their own activities.
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }
        return false;
    }

    private void switchToHeaderLocal(Header header) {
        mInLocalHeaderSwitch = true;
        switchToHeader(header);
        mInLocalHeaderSwitch = false;
    }

    @Override
    public void switchToHeader(Header header) {
        if (!mInLocalHeaderSwitch) {
            mCurrentHeader = null;
            mParentHeader = null;
        }
        super.switchToHeader(header);
    }

    /**
     * Switch to parent fragment and store the grand parent's info
     * @param className name of the activity wrapper for the parent fragment.
     */
    private void switchToParent(String className) {
        final ComponentName cn = new ComponentName(this, className);
        try {
            final PackageManager pm = getPackageManager();
            final ActivityInfo parentInfo = pm.getActivityInfo(cn, PackageManager.GET_META_DATA);

            if (parentInfo != null && parentInfo.metaData != null) {
                String fragmentClass = parentInfo.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);
                CharSequence fragmentTitle = parentInfo.loadLabel(pm);
                Header parentHeader = new Header();
                parentHeader.fragment = fragmentClass;
                parentHeader.title = fragmentTitle;
                mCurrentHeader = parentHeader;

                switchToHeaderLocal(parentHeader);
                highlightHeader(mTopLevelHeaderId);

                mParentHeader = new Header();
                mParentHeader.fragment
                        = parentInfo.metaData.getString(META_DATA_KEY_PARENT_FRAGMENT_CLASS);
                mParentHeader.title = parentInfo.metaData.getString(META_DATA_KEY_PARENT_TITLE);
            }
        } catch (NameNotFoundException nnfe) {
            Log.w(LOG_TAG, "Could not find parent activity : " + className);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // If it is not launched from history, then reset to top-level
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            if (mFirstHeader != null && !onIsHidingHeaders() && onIsMultiPane()) {
                switchToHeaderLocal(mFirstHeader);
            }
            getListView().setSelectionFromTop(0, 0);
        }
    }

    private void highlightHeader(int id) {
        if (id != 0) {
            Integer index = mHeaderIndexMap.get(id);
            if (index != null) {
                getListView().setItemChecked(index, true);
                if (isMultiPane()) {
                    getListView().smoothScrollToPosition(index);
                }
            }
        }
    }

    @Override
    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        // This is called from super.onCreate, isMultiPane() is not yet reliable
        // Do not use onIsHidingHeaders either, which relies itself on this method
        if (startingFragment != null && !onIsMultiPane()) {
            Intent modIntent = new Intent(superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, startingFragment);
            Bundle args = superIntent.getExtras();
            if (args != null) {
                args = new Bundle(args);
            } else {
                args = new Bundle();
            }
            args.putParcelable("intent", superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, superIntent.getExtras());
            return modIntent;
        }
        return superIntent;
    }

    /**
     * Checks if the component name in the intent is different from the Settings class and
     * returns the class name to load as a fragment.
     */
    protected String getStartingFragmentClass(Intent intent) {
        if (mFragmentClass != null) return mFragmentClass;

        String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) return null;

        if ("com.android.settings.ManageApplications".equals(intentClass)
                || "com.android.settings.RunningServices".equals(intentClass)
                || "com.android.settings.applications.StorageUse".equals(intentClass)) {
            // Old names of manage apps.
            intentClass = com.android.settings.applications.ManageApplications.class.getName();
        }

        return intentClass;
    }

    /**
     * Override initial header when an activity-alias is causing Settings to be launched
     * for a specific fragment encoded in the android:name parameter.
     */
    @Override
    public Header onGetInitialHeader() {
        String fragmentClass = getStartingFragmentClass(super.getIntent());
        if (fragmentClass != null) {
            Header header = new Header();
            header.fragment = fragmentClass;
            header.title = getTitle();
            header.fragmentArguments = getIntent().getExtras();
            mCurrentHeader = header;
            return header;
        }

        return mFirstHeader;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args,
            int titleRes, int shortTitleRes) {
        Intent intent = super.onBuildStartFragmentIntent(fragmentName, args,
                titleRes, shortTitleRes);

        // Some fragments want split ActionBar; these should stay in sync with
        // uiOptions for fragments also defined as activities in manifest.
        if (WifiSettings.class.getName().equals(fragmentName) ||
                WifiP2pSettings.class.getName().equals(fragmentName) ||
                BluetoothSettings.class.getName().equals(fragmentName) ||
                DreamSettings.class.getName().equals(fragmentName) ||
                LocationSettings.class.getName().equals(fragmentName) ||
                ToggleAccessibilityServicePreferenceFragment.class.getName().equals(fragmentName) ||
                PrintSettingsFragment.class.getName().equals(fragmentName) ||
                PrintServiceSettingsFragment.class.getName().equals(fragmentName)) {
            intent.putExtra(EXTRA_UI_OPTIONS, ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        }

        if (DataUsageSummary.class.getName().equals(fragmentName)) {
            intent.setClass(this, DataUsageSummaryActivity.class);
        } else {
            intent.setClass(this, SubSettings.class);
        }
        return intent;
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> headers) {
        if (!onIsHidingHeaders()) {
            /* SPRD: changed for tab style @{ */
            if (UNIVERSEUI_SUPPORT) {
                ListAdapter listAdapter = getListAdapter();
                loadHeadersFromResource(mHeadersCategory, headers);
                if (listAdapter instanceof HeaderAdapter) {
                    ((HeaderAdapter) listAdapter).flushViewCache();
                    ((HeaderAdapter) listAdapter).resume();
                    ((HeaderAdapter) listAdapter).notifyDataSetChanged();
                }
                /* @} */
            }else if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME){
                loadHeadersFromResource(R.xml.settings_headers_setsimname, headers);
            }else {
                loadHeadersFromResource(R.xml.settings_headers, headers);
            }
        }
        updateHeaderList(headers);
    }

    private void updateHeaderList(List<Header> target) {
        final boolean showDev = mDevelopmentPreferences.getBoolean(
                DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng"));
        int i = 0;
        boolean IsSupVoice = Settings.this.getResources().getBoolean(com.android.internal.R.bool.
                config_voice_capable);
        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        mHeaderIndexMap.clear();
        while (i < target.size()) {
            Header header = target.get(i);
            // Ids are integers, so downcasting
            int id = (int) header.id;
            if (id == R.id.operator_settings || id == R.id.manufacturer_settings) {
                Utils.updateHeaderToSpecificActivityFromMetaDataOrRemove(this, target, header);
            } else if (id == R.id.wifi_settings) {
                // Remove WiFi Settings if WiFi service is not available.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                    target.remove(i);
                }
            } else if (id == R.id.bluetooth_settings) {
                // Remove Bluetooth Settings if Bluetooth service is not available.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    target.remove(i);
                }
            } else if (id == R.id.data_usage_settings) {
                // Remove data usage when kernel module not enabled
                final INetworkManagementService netManager = INetworkManagementService.Stub
                        .asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
               try {
                    /* SPRD：ADD to delete the data usage item of settings @{ */
                boolean support_cmcc = SystemProperties.get("ro.operator").equals("cmcc");
                    if (!netManager.isBandwidthControlEnabled() || support_cmcc) {
                        target.remove(i);
                    }
                } catch (RemoteException e) {
                    // ignored
                }
            } else if (id == R.id.battery_settings) {
                // Remove battery settings when battery is not available. (e.g. TV)

                if (!mBatteryPresent) {
                    target.remove(i);
                }
                /* @} */
            } else if (id == R.id.account_settings) {
                int headerIndex = i + 1;
                i = insertAccountsHeaders(target, headerIndex);
                // SPRD: clear viewCache
                ListAdapter listAdapter = getListAdapter();
                if (listAdapter instanceof HeaderAdapter) {
                    // add for tab style
                    ((HeaderAdapter) listAdapter).flushViewCache();
                }
            } else if (id == R.id.home_settings) {
                if (!updateHomeSettingHeaders(header)) {
                    target.remove(i);
                }
            } else if (id == R.id.user_settings) {
                if (!UserHandle.MU_ENABLED
                        || !UserManager.supportsMultipleUsers()
                        || Utils.isMonkeyRunning()) {
                    target.remove(i);
                }
            } else if (id == R.id.nfc_payment_settings) {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
                    target.remove(i);
                } else {
                    // Only show if NFC is on and we have the HCE feature
                    NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
                    if (!adapter.isEnabled() || !getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
                        target.remove(i);
                    }
                }
            } else if (id == R.id.development_settings) {
                if (!showDev) {
                    target.remove(i);
                }
            } else if (id == R.id.account_add) {
                if (um.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
                    target.remove(i);
                }
            }

            /* SPRD: modified for cucc feature @{ */
            else if (id == R.id.network_preference_settings) {
                //if (!("cucc".equals(WifiManager.SUPPORT_VERSION))) {
                if (!CU_SUPPORT) { //modify for CUCC support  2013-11-22
                    target.remove(header);
                }
            }
            /* @} */
            /* SPRD: add AudioProfile  @{ */
            else if (id == R.id.sound_settings && IsSupVoice)
            {
                target.remove(header);
            }
            else if (id == R.id.audio_profiles && !IsSupVoice)
            {
                target.remove(header);
            }
            /* @} */
            /* SPRD: for multi-sim @{ */
            else if (id == R.id.dual_sim_settings) {
                if (!TelephonyManager.isMultiSim()||(!mVoiceCapable)) {
                    target.remove(header);
                }
            }
            /* @} */
            /* SPRD: add for uui style 335009 @{ */
            else if (id == R.id.uninstall_settings) {
                if (!UNIVERSEUI_SUPPORT) {
                    target.remove(header);
                }
            }
            /* @} */
            /* smart_wake {@*/
            else if (id == R.id.smart_wake){
                if(!SMART_WAKE){
                    target.remove(header);                    
                }
            }
            /* @} */
          /*lyx 20150320 power_saving */
            else if (id == R.id.power_saving){
                if(!FeatureOption.PRJ_FEATURE_POWER_SAVING){
                    target.remove(header);                    
                }
            }
            /* @} */
            if (i < target.size() && target.get(i) == header
                    && UserHandle.MU_ENABLED && UserHandle.myUserId() != 0
                    && !ArrayUtils.contains(SETTINGS_FOR_RESTRICTED, id)) {
                target.remove(i);
            }

            // Increment if the current one wasn't removed by the Utils code.
            if (i < target.size() && target.get(i) == header) {
                // Hold on to the first header, when we need to reset to the top-level
                if (mFirstHeader == null &&
                        HeaderAdapter.getHeaderType(header) != HeaderAdapter.HEADER_TYPE_CATEGORY) {
                    mFirstHeader = header;
                }
                mHeaderIndexMap.put(id, i);
                i++;
            }
        }
    }

    private int insertAccountsHeaders(List<Header> target, int headerIndex) {
        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
        List<Header> accountHeaders = new ArrayList<Header>(accountTypes.length);
        for (String accountType : accountTypes) {
            /**
             * SPRD:
             * hide sprd account in settings.
             * @{
             */
            if (accountType.startsWith("sprd")) {
                continue;
            }
            /**
             * @}
             */
            CharSequence label = mAuthenticatorHelper.getLabelForType(this, accountType);
            if (label == null) {
                continue;
            }

            Account[] accounts = AccountManager.get(this).getAccountsByType(accountType);
            boolean skipToAccount = accounts.length == 1
                    && !mAuthenticatorHelper.hasAccountPreferences(accountType);
            Header accHeader = new Header();
            accHeader.title = label;
            if (accHeader.extras == null) {
                accHeader.extras = new Bundle();
            }
            if (skipToAccount) {
                accHeader.breadCrumbTitleRes = R.string.account_sync_settings_title;
                accHeader.breadCrumbShortTitleRes = R.string.account_sync_settings_title;
                accHeader.fragment = AccountSyncSettings.class.getName();
                accHeader.fragmentArguments = new Bundle();
                // Need this for the icon
                accHeader.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accHeader.extras.putParcelable(AccountSyncSettings.ACCOUNT_KEY, accounts[0]);
                accHeader.fragmentArguments.putParcelable(AccountSyncSettings.ACCOUNT_KEY,
                        accounts[0]);
            } else {
                accHeader.breadCrumbTitle = label;
                accHeader.breadCrumbShortTitle = label;
                accHeader.fragment = ManageAccountsSettings.class.getName();
                accHeader.fragmentArguments = new Bundle();
                accHeader.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accHeader.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE,
                        accountType);
                if (!isMultiPane()) {
                    accHeader.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_LABEL,
                            label.toString());
                }
            }
            accountHeaders.add(accHeader);
            mAuthenticatorHelper.preloadDrawableForType(this, accountType);
        }

        // Sort by label
        Collections.sort(accountHeaders, new Comparator<Header>() {
            @Override
            public int compare(Header h1, Header h2) {
                return h1.title.toString().compareTo(h2.title.toString());
            }
        });

        for (Header header : accountHeaders) {
            target.add(headerIndex++, header);
        }
        if (!mListeningToAccountUpdates) {
            AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);
            mListeningToAccountUpdates = true;
        }
        return headerIndex;
    }

    private boolean updateHomeSettingHeaders(Header header) {
        // Once we decide to show Home settings, keep showing it forever
        SharedPreferences sp = getSharedPreferences(HomeSettings.HOME_PREFS, Context.MODE_PRIVATE);
        if (sp.getBoolean(HomeSettings.HOME_PREFS_DO_SHOW, false)) {
            return true;
        }

        try {
            final ArrayList<ResolveInfo> homeApps = new ArrayList<ResolveInfo>();
            getPackageManager().getHomeActivities(homeApps);
            if (homeApps.size() < 2) {
                // When there's only one available home app, omit this settings
                // category entirely at the top level UI.  If the user just
                // uninstalled the penultimate home app candidiate, we also
                // now tell them about why they aren't seeing 'Home' in the list.
                if (sShowNoHomeNotice) {
                    sShowNoHomeNotice = false;
                    NoHomeDialogFragment.show(this);
                }
                return false;
            } else {
                // Okay, we're allowing the Home settings category.  Tell it, when
                // invoked via this front door, that we'll need to be told about the
                // case when the user uninstalls all but one home app.
                if (header.fragmentArguments == null) {
                    header.fragmentArguments = new Bundle();
                }
                header.fragmentArguments.putBoolean(HomeSettings.HOME_SHOW_NOTICE, true);
            }
        } catch (Exception e) {
            // Can't look up the home activity; bail on configuring the icon
            Log.w(LOG_TAG, "Problem looking up home activity!", e);
        }

        sp.edit().putBoolean(HomeSettings.HOME_PREFS_DO_SHOW, true).apply();
        return true;
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mTopLevelHeaderId = ai.metaData.getInt(META_DATA_KEY_HEADER_ID);
            mFragmentClass = ai.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);

            // Check if it has a parent specified and create a Header object
            final int parentHeaderTitleRes = ai.metaData.getInt(META_DATA_KEY_PARENT_TITLE);
            String parentFragmentClass = ai.metaData.getString(META_DATA_KEY_PARENT_FRAGMENT_CLASS);
            if (parentFragmentClass != null) {
                mParentHeader = new Header();
                mParentHeader.fragment = parentFragmentClass;
                if (parentHeaderTitleRes != 0) {
                    mParentHeader.title = getResources().getString(parentHeaderTitleRes);
                }
            }
        } catch (NameNotFoundException nnfe) {
            // No recovery
        }
    }

    @Override
    public boolean hasNextButton() {
        return super.hasNextButton();
    }

    @Override
    public Button getNextButton() {
        return super.getNextButton();
    }

    public static class NoHomeDialogFragment extends DialogFragment {
        public static void show(Activity parent) {
            final NoHomeDialogFragment dialog = new NoHomeDialogFragment();
            dialog.show(parent.getFragmentManager(), null);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.only_one_home_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        static final int HEADER_TYPE_CATEGORY = 0;
        static final int HEADER_TYPE_NORMAL = 1;
        static final int HEADER_TYPE_SWITCH = 2;
        static final int HEADER_TYPE_BUTTON = 3;
        private static final int HEADER_TYPE_COUNT = HEADER_TYPE_BUTTON + 1;

        private final WifiEnabler mWifiEnabler;
        private final BluetoothEnabler mBluetoothEnabler;
        private AuthenticatorHelper mAuthHelper;
        private DevicePolicyManager mDevicePolicyManager;

        //revo lyq 2014 for advan settings
        HeaderViewHolder wifi_holder;
        HeaderViewHolder bt_holder;
        HeaderViewHolder dsim_holder;
        HeaderViewHolder smartwake_holder;
	
        /* SPRD: add for tab style @{ */
        private View[] mViewCache;
        private int mViewCacheSize = 0;

        /* @} */
        // SPRD: for Bug258772, no instance of class of DevicePolicyManager
        private Context mContext;

        private static class HeaderViewHolder {
            ImageView icon;
            TextView title;
            TextView summary;
            Switch switch_;
            ImageButton button_;
            View divider_;
        }

        private LayoutInflater mInflater;

        static int getHeaderType(Header header) {
            if (header.fragment == null && header.intent == null) {
                return HEADER_TYPE_CATEGORY;
            } else if (header.id == R.id.wifi_settings || header.id == R.id.bluetooth_settings || header.id == R.id.power_saving) {
                return HEADER_TYPE_SWITCH;           /*lyx 20150320 power_saving */
            } else if (header.id == R.id.security_settings) {
                return HEADER_TYPE_BUTTON;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false; // because of categories
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != HEADER_TYPE_CATEGORY;
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public HeaderAdapter(Context context, List<Header> objects,
                AuthenticatorHelper authenticatorHelper, DevicePolicyManager dpm) {
            super(context, 0, objects);

            mAuthHelper = authenticatorHelper;
            if (null == mInflater) {
                mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }

            // Temp Switches provided as placeholder until the adapter replaces these with actual
            // Switches inflated from their layouts. Must be done before adapter is set in super
            mWifiEnabler = new WifiEnabler(context, new Switch(context));
            mBluetoothEnabler = new BluetoothEnabler(context, new Switch(context));
            /* SPRD: add for tab style @{ */
            mViewCacheSize = objects.size();
            mViewCache = new View[mViewCacheSize];
            /* @} */
            // SPRD: for Bug258772, no instance of class of DevicePolicyManager
            mContext = context;
        }

        /* SPRD: add for tab style @{ */
        public boolean flushViewCache() {
            int currentCount = getCount();

            mViewCacheSize = currentCount;
            mViewCache = null;
            mViewCache = new View[mViewCacheSize];
            return true;
        }

        /* @} */

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            /* SPRD: add for tab style @{ */
            if (position >= mViewCacheSize) {
                flushViewCache();
            }
            /* @} */
            HeaderViewHolder holder;
            Header header = getItem(position);
            int headerType = getHeaderType(header);
            View view = null;
            /* SPRD: add for tab style @{ */
            convertView = mViewCache[position];
            /* @} */

            if (convertView == null) {
                holder = new HeaderViewHolder();
                switch (headerType) {
                    case HEADER_TYPE_CATEGORY:
                        view = new TextView(getContext(), null,
                                android.R.attr.listSeparatorTextViewStyle);
                        holder.title = (TextView) view;
                        break;

                    case HEADER_TYPE_SWITCH:
                        view = mInflater.inflate(R.layout.preference_header_switch_item, parent,
                                false);
                        holder.icon = (ImageView) view.findViewById(R.id.icon);
                        holder.title = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.summary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        holder.switch_ = (Switch) view.findViewById(R.id.switchWidget);
                        break;

                    case HEADER_TYPE_BUTTON:
                        view = mInflater.inflate(R.layout.preference_header_button_item, parent,
                                false);
                        holder.icon = (ImageView) view.findViewById(R.id.icon);
                        holder.title = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.summary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        holder.button_ = (ImageButton) view.findViewById(R.id.buttonWidget);
                        holder.divider_ = view.findViewById(R.id.divider);
                        break;

                    case HEADER_TYPE_NORMAL:
                        view = mInflater.inflate(
                                R.layout.preference_header_item, parent,
                                false);
                        holder.icon = (ImageView) view.findViewById(R.id.icon);
                        holder.title = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.summary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        break;
                }
                view.setTag(holder);
                /* SPRD: add for tab style @{ */
                mViewCache[position] = view;
                /* @} */
            } else {
                view = convertView;
                /* SPRD: changed for tab style @{ */
                 holder = (HeaderViewHolder) view.getTag();
//                return view;
                /* @} */
            }

            // All view fields must be updated every time, because the view may be recycled
            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    break;

                case HEADER_TYPE_SWITCH:
                    // Would need a different treatment if the main menu had more switches
                    if (header.id == R.id.wifi_settings) {
                        mWifiEnabler.setSwitch(holder.switch_);
                    } else if (header.id == R.id.bluetooth_settings) {
                        mBluetoothEnabler.setSwitch(holder.switch_);
                    } else if (header.id == R.id.power_saving) {
                    /*lyx 20150320 power_saving */
		              boolean mEnabler =  getSecureBoolean(Secure.POWERSAVING_MODE_ON) ;
				holder.switch_.setChecked(mEnabler);
			       holder.switch_.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			            @Override
			            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                        putSecureBoolean(Secure.POWERSAVING_MODE_ON,isChecked);
                                        Intent intent = new Intent("com.sprd.intent.action.BATTERY_CHANGED");
                                              intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                                              intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                                              intent.putExtra(BatteryManager.EXTRA_LEVEL, batteryLevel);
                                              mContext.sendBroadcast(intent);
			            }
			        });
			}
                    updateCommonHeaderView(header, holder);
                     
                      //revo lyq 2014 for advan settings 
                      if(is_advan_settings){ 
	                      if (header.id == R.id.wifi_settings) {
	                        wifi_holder=	holder;	
	                        setWifiIcon(mWifiEnabler.getWifiState());
	                      } else {
	                        bt_holder=holder;
	                        setBtIcon(mBluetoothEnabler.getBluetoothState());
	                      }
                      }
					
                    break;

                case HEADER_TYPE_BUTTON:
                    if (header.id == R.id.security_settings) {
                        boolean hasCert = DevicePolicyManager.hasAnyCaCertsInstalled();
                        if (hasCert) {
                            holder.button_.setVisibility(View.VISIBLE);
                            holder.divider_.setVisibility(View.VISIBLE);
                            /* SPRD: for Bug258772, no instance of class of DevicePolicyManager @{*/
                            if(mDevicePolicyManager==null){
                                mDevicePolicyManager = DevicePolicyManager.create(mContext, null);
                            }
                            /* @} */
                            boolean isManaged = mDevicePolicyManager.getDeviceOwner() != null;
                            if (isManaged) {
                                holder.button_.setImageResource(R.drawable.ic_settings_about);
                            } else {
                                holder.button_.setImageResource(
                                        android.R.drawable.stat_notify_error);
                            }
                            holder.button_.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(
                                            android.provider.Settings.ACTION_MONITORING_CERT_INFO);
                                    getContext().startActivity(intent);
                                }
                            });
                        } else {
                            holder.button_.setVisibility(View.GONE);
                            holder.divider_.setVisibility(View.GONE);
                        }
                    }
                    updateCommonHeaderView(header, holder);
                    break;

                case HEADER_TYPE_NORMAL:
                    updateCommonHeaderView(header, holder);
					
                    //revo lyq 2014 for advan settings
                      if(is_advan_settings){                       
	                      if (header.id == R.id.dual_sim_settings) {
	                        dsim_holder=holder;
	                        setDualSimIcon();	
	                      }else if(header.id == R.id.smart_wake){
							smartwake_holder=holder;
							setsmartwakeIcon();
						  }
                      }
					
                    break;
            }

            return view;
        }

        private void updateCommonHeaderView(Header header, HeaderViewHolder holder) {
                if (header.extras != null
                        && header.extras.containsKey(ManageAccountsSettings.KEY_ACCOUNT_TYPE)) {
                    String accType = header.extras.getString(
                            ManageAccountsSettings.KEY_ACCOUNT_TYPE);
                    Drawable icon = mAuthHelper.getDrawableForType(getContext(), accType);
                    setHeaderIcon(holder, icon);
                } else {
                    holder.icon.setImageResource(header.iconRes);
                }
                holder.title.setText(header.getTitle(getContext().getResources()));
                // if we do not support backup,the text "backup and reset" change.
                if(header.id == R.id.privacy_settings) {
                    if (getContext().getPackageManager().resolveContentProvider(GSETTINGS_PROVIDER, 0) == null) {
                        holder.title.setText(getContext().getResources().getText(R.string.master_clear_title));
                    }
                }
                CharSequence summary = header.getSummary(getContext().getResources());
                if (!TextUtils.isEmpty(summary)) {
                    holder.summary.setVisibility(View.VISIBLE);
                    holder.summary.setText(summary);
                } else {
                    holder.summary.setVisibility(View.GONE);
                }
            }

        private void setHeaderIcon(HeaderViewHolder holder, Drawable icon) {
            ViewGroup.LayoutParams lp = holder.icon.getLayoutParams();
            lp.width = getContext().getResources().getDimensionPixelSize(
                    R.dimen.header_icon_width);
            lp.height = lp.width;
            holder.icon.setLayoutParams(lp);
            holder.icon.setImageDrawable(icon);
        }

        public void resume() {
            mWifiEnabler.resume();
            mBluetoothEnabler.resume();
        }

        public void pause() {
            mWifiEnabler.pause();
            mBluetoothEnabler.pause();
        }

   //revo lyq 2014 for advan settings                
   public void  setWifiIcon(int state) {
      try{
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
            case WifiManager.WIFI_STATE_ENABLED:
            Log.w(LOG_TAG, "setWifiIcon  111" );
		wifi_holder.icon.setImageResource(R.drawable.ic_settings_wireless_on);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
            case WifiManager.WIFI_STATE_DISABLED:
	    Log.w(LOG_TAG, "setWifiIcon  222" );
		wifi_holder.icon.setImageResource(R.drawable.ic_settings_wireless);
	
                break;				
            default:
                break;
        }
      	}catch(Exception e) {
		Log.w(LOG_TAG, "setWifiIcon  333" +e.toString());
	}
    }

   public void  setBtIcon(int state) {
      try{
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
            case BluetoothAdapter.STATE_ON:
				 Log.w(LOG_TAG, "setBtIcon  111" );
		bt_holder.icon.setImageResource(R.drawable.ic_settings_bluetooth2_on);// 
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
            case BluetoothAdapter.STATE_OFF:
				 Log.w(LOG_TAG, "setBtIcon  222" );
		bt_holder.icon.setImageResource(R.drawable.ic_settings_bluetooth2);
                break;
            default:
                break;
        }
      	}catch(Exception e) {
		Log.w(LOG_TAG, "setBtIcon  333" +e.toString());
	}
    }   

          /*lyx 20150320 power_saving */
	private boolean getSecureBoolean(String key) {
		int opened = 0;
	       int default_on = mContext.getResources().getInteger(com.android.internal.R.integer.powersaving_default_on);	
		opened = Secure.getInt(mContext.getContentResolver(), key,default_on)  ;
		return (opened == 0 ? false : true);
	}
	
	private void putSecureBoolean(String key,boolean val ) {
		Secure.putInt(mContext.getContentResolver(), key, val ? 1 : 0);		
	}
          /*lyx 20150320 power_saving  end*/

	public void  setDualSimIcon() {   
      try{		
         int activeSimCount =0;
	  TelephonyManager tm_1 = TelephonyManager.getDefault(0);
	  TelephonyManager tm_2 = TelephonyManager.getDefault(1); 
	  
         if(tm_1.getSimState() == TelephonyManager.SIM_STATE_READY){
		activeSimCount=1;		
	   }
		 	
	  if(tm_2.getSimState() == TelephonyManager.SIM_STATE_READY) {
		activeSimCount=2;		
  	  }
			  
	 if(activeSimCount==0){
		dsim_holder.icon.setImageResource(R.drawable.ic_dualsettings_sim);		
 	 }else{
		dsim_holder.icon.setImageResource(R.drawable.ic_dualsettings_sim_on);
	 }
      	}catch(Exception e) {
		Log.w(LOG_TAG, "setDualSimIcon  333" +e.toString());
	}
	}   


  public void  setsmartwakeIcon() {
      try{
	  	 
		 boolean opened = false;
              opened = System.getInt(mContext.getContentResolver(), System.SMART_WAKE_STATE) == System.SMART_WAKE_ON ;
		 if(opened){
			smartwake_holder.icon.setImageResource(R.drawable.ic_settings_gesture_control_on);
		 }else{
			smartwake_holder.icon.setImageResource(R.drawable.ic_settings_gesture_control);
		 }
      	}catch(Exception e) {
		Log.w(LOG_TAG, "setsmartwakeIcon  " +e.toString());
	}
    }
	
  //revo lyq 2014 for advan settings	
		
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        boolean revert = false;
        if (header.id == R.id.account_add) {
            revert = true;
        }

        super.onHeaderClick(header, position);

        if (revert && mLastHeader != null) {
            highlightHeader((int) mLastHeader.id);
        } else {
            mLastHeader = header;
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        // Override the fragment title for Wallpaper settings
        int titleRes = pref.getTitleRes();
        if (pref.getFragment().equals(WallpaperTypeSettings.class.getName())) {
            titleRes = R.string.wallpaper_settings_fragment_title;
        } else if (pref.getFragment().equals(OwnerInfoSettings.class.getName())
                && UserHandle.myUserId() != UserHandle.USER_OWNER) {
            if (UserManager.get(this).isLinkedUser()) {
                titleRes = R.string.profile_info_settings_title;
            } else {
                titleRes = R.string.user_info_settings_title;
            }
        }
        startPreferencePanel(pref.getFragment(), pref.getExtras(), titleRes, pref.getTitle(),
                null, 0);
        return true;
    }

    @Override
    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return super.shouldUpRecreateTask(new Intent(this, Settings.class));
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            super.setListAdapter(new HeaderAdapter(this, getHeaders(), mAuthenticatorHelper, dpm));
        }
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // TODO: watch for package upgrades to invalidate cache; see 7206643
        mAuthenticatorHelper.updateAuthDescriptions(this);
        mAuthenticatorHelper.onAccountsUpdated(this, accounts);
        invalidateHeaders();
    }

    public static void requestHomeNotice() {
        sShowNoHomeNotice = true;
    }

    /*
     * Settings subclasses for launching independently.
     */
    public static class BluetoothSettingsActivity extends Settings { /* empty */ }
    public static class WirelessSettingsActivity extends Settings { /* empty */ }
    public static class TetherSettingsActivity extends Settings { /* empty */ }
    public static class VpnSettingsActivity extends Settings { /* empty */ }
    public static class DateTimeSettingsActivity extends Settings { /* empty */ }
    public static class StorageSettingsActivity extends Settings { /* empty */ }
    public static class WifiSettingsActivity extends Settings { /* empty */ }
    public static class HotspotSettingsActivity extends Settings { /* empty */ }
    public static class WifiP2pSettingsActivity extends Settings { /* empty */ }
    public static class InputMethodAndLanguageSettingsActivity extends Settings { /* empty */ }
    public static class KeyboardLayoutPickerActivity extends Settings { /* empty */ }
    public static class InputMethodAndSubtypeEnablerActivity extends Settings { /* empty */ }
    public static class SpellCheckersSettingsActivity extends Settings { /* empty */ }
    public static class LocalePickerActivity extends Settings { /* empty */ }
    public static class UserDictionarySettingsActivity extends Settings { /* empty */ }
    public static class SoundSettingsActivity extends Settings { /* empty */ }
    public static class DisplaySettingsActivity extends Settings { /* empty */ }
    public static class DeviceInfoSettingsActivity extends Settings { /* empty */ }
    public static class ApplicationSettingsActivity extends Settings { /* empty */ }
    public static class ManageApplicationsActivity extends Settings { /* empty */ }
    public static class AppOpsSummaryActivity extends Settings {
        @Override
        public boolean isValidFragment(String className) {
            if (AppOpsSummary.class.getName().equals(className)) {
                return true;
            }
            return super.isValidFragment(className);
        }
    }
    public static class StorageUseActivity extends Settings { /* empty */ }
    public static class DevelopmentSettingsActivity extends Settings { /* empty */ }
    public static class AccessibilitySettingsActivity extends Settings { /* empty */ }
    public static class CaptioningSettingsActivity extends Settings { /* empty */ }
    public static class SecuritySettingsActivity extends Settings { /* empty */ }
    public static class LocationSettingsActivity extends Settings { /* empty */ }
    public static class PrivacySettingsActivity extends Settings { /* empty */ }
    public static class RunningServicesActivity extends Settings { /* empty */ }
    public static class ManageAccountsSettingsActivity extends Settings { /* empty */ }
    public static class PowerUsageSummaryActivity extends Settings { /* empty */ }
    public static class AccountSyncSettingsActivity extends Settings { /* empty */ }
    public static class AccountSyncSettingsInAddAccountActivity extends Settings { /* empty */ }
    public static class CryptKeeperSettingsActivity extends Settings { /* empty */ }
    public static class DeviceAdminSettingsActivity extends Settings { /* empty */ }
    public static class DataUsageSummaryActivity extends Settings { /* empty */ }
    public static class AdvancedWifiSettingsActivity extends Settings { /* empty */ }
    public static class TextToSpeechSettingsActivity extends Settings { /* empty */ }
    public static class AndroidBeamSettingsActivity extends Settings { /* empty */ }
    public static class WifiDisplaySettingsActivity extends Settings { /* empty */ }
    public static class DreamSettingsActivity extends Settings { /* empty */ }
    public static class NotificationStationActivity extends Settings { /* empty */ }
    public static class UserSettingsActivity extends Settings { /* empty */ }
    public static class NotificationAccessSettingsActivity extends Settings { /* empty */ }
    public static class UsbSettingsActivity extends Settings { /* empty */ }
    public static class TrustedCredentialsSettingsActivity extends Settings { /* empty */ }
    public static class PaymentSettingsActivity extends Settings { /* empty */ }
    public static class PrintSettingsActivity extends Settings { /* empty */ }
    public static class PrintJobSettingsActivity extends Settings { /* empty */ }
    /* SPRD: Modify 20140118 Spread of bug269951, TaskManager and ApplicationManager perfomance optimization @{ */
    public static class SprdManageApplicationsActivity extends ManageApplicationsActivity { /* empty */ }
    public static class SprdManageTasksActivity extends ManageApplicationsActivity { /* empty */ }
    /* @} */
    /* SPRD: add beatch uninstall function for bug 283586 @{ */
    public static class UninstallApplicationsActivity extends Settings { /* empty */ }
    /* @} */
        /* SPRD：ADD to make Settings to Tab style @{ */
    private void setupTab() {
        mActionBar = getActionBar();
        mActionBar.setAlternativeTabStyle(true);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        int tabHeight = (int) getResources().getDimensionPixelSize(R.dimen.uui_tab_height);
        mActionBar.setTabHeight(tabHeight);

        setupNetWork();
        setupDevice();
        setupPersonal();
        setupMore();
        setCurrentTab(mCurrentTabIndex);
    }

    private void setupNetWork() {
        final Tab tab = mActionBar.newTab();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.setting_tab_new_ui, null);
        ImageView dialView = (ImageView) view.findViewById(R.id.tab_icon);
        if (dialView != null) {
            dialView.setImageResource(R.drawable.tab_network);
        }
        TextView dialText = (TextView) view.findViewById(R.id.tab_text);
        if (dialText != null) {
            dialText.setText(R.string.network_settings_tab);
        }
        tab.setCustomView(view);
        tab.setTag(R.xml.settings_headers_network);
        tab.setTabListener(mTabListener);
        mActionBar.addTab(tab);
    }
    private void setupDevice() {
        final Tab tab = mActionBar.newTab();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.setting_tab_new_ui, null);
        ImageView dialView = (ImageView) view.findViewById(R.id.tab_icon);
        if (dialView != null) {
            dialView.setImageResource(R.drawable.tab_device);
        }
        TextView dialText = (TextView) view.findViewById(R.id.tab_text);
        if (dialText != null) {
            dialText.setText(R.string.device_settings_tab);
        }
        tab.setCustomView(view);
        tab.setTag(R.xml.settings_headers_device);
        tab.setTabListener(mTabListener);
        mActionBar.addTab(tab);
    }
    private void setupPersonal() {
        final Tab tab = mActionBar.newTab();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.setting_tab_new_ui, null);
        ImageView dialView = (ImageView) view.findViewById(R.id.tab_icon);
        if (dialView != null) {
            dialView.setImageResource(R.drawable.tab_personal);
        }
        TextView dialText = (TextView) view.findViewById(R.id.tab_text);
        if (dialText != null) {
            dialText.setText(R.string.personal_settings_tab);
        }
        tab.setCustomView(view);
        tab.setTag(R.xml.settings_headers_personal);
        tab.setTabListener(mTabListener);
        mActionBar.addTab(tab);
    }
    private void setupMore() {
        final Tab tab = mActionBar.newTab();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.setting_tab_new_ui, null);
        ImageView dialView = (ImageView) view.findViewById(R.id.tab_icon);
        if (dialView != null) {
            dialView.setImageResource(R.drawable.tab_more);
        }
        TextView dialText = (TextView) view.findViewById(R.id.tab_text);
        if (dialText != null) {
            dialText.setText(R.string.more_settings_tab);
        }
        tab.setCustomView(view);
        tab.setTag(R.xml.settings_headers_more);
        tab.setTabListener(mTabListener);
        mActionBar.addTab(tab);
    }

    /* SPRD: DELETE unUseless.
    private PagerAdapter mPagerAdapter = new PagerAdapter() {
        final ArrayList<View> views = new ArrayList<View>(4);

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getCount() {
            return mTabTitle.length;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView(views.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(mTabTitle[position]);
        }

        @Override
        public Object instantiateItem(View container, int position) {
            LayoutInflater inflater = LayoutInflater.from(Settings.this);
            View view1 = inflater.inflate(R.xml.settings_headers_network, null);
            View view2 = inflater.inflate(R.xml.settings_headers_device, null);
            View view3 = inflater.inflate(R.xml.settings_headers_personal, null);
            View view4 = inflater.inflate(R.xml.settings_headers_more, null);
            views.add(view1);
            views.add(view2);
            views.add(view3);
            views.add(view4);
            ((ViewPager) container).addView(views.get(position));
            return views.get(position);
        }
    };

    private class TabsPagerListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            setCurrentTab(position);
        }

    }
    */

    public void setCurrentTab(int position) {
        /* SPRD: tab title changed,bug 258557 @{ */
        //mActionBar.setTitle(mTabTitle[position]);
        /* @} */
        mCurrentTabIndex = position;
        if ((mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS)
                && (mCurrentTabIndex != mActionBar.getSelectedNavigationIndex())) {
            mActionBar.setSelectedNavigationItem(mCurrentTabIndex);
        }
    }

    private final TabListener mTabListener = new TabListener() {
        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            int tag = (Integer) tab.getTag();

            if (mHeadersCategory != tag) {
                mCurrentTabIndex = tab.getPosition();
                Log.i("TabSettings", "mCurrentTab = " + mCurrentTabIndex);
                getIntent().putExtra("tab_index", mCurrentTabIndex);
                mHeadersCategory = tag;

                /* SPRD: tab title changed,bug 258557 @{ */
                //mActionBar.setTitle(mTabTitle[mCurrentTabIndex]);
                /* @} */

                /* mViewPager.setCurrentItem(tab.getPosition(), true); */
                invalidateHeaders();
            }
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    };

    /**
     * SPRD：ADD for tab style
     */
    private void chooseTab(int index) {
        getActionBar().setSelectedNavigationItem(index);
        mCurrentTabIndex = index;
        getIntent().putExtra("tab_index", mCurrentTabIndex);
    }
    /* @} */
}
