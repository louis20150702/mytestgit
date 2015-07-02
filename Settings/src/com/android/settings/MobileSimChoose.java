package com.android.settings;
/** SPRD： Created by Spreadst */
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.provider.Settings;
import android.sim.SimManager;

public class MobileSimChoose extends PreferenceActivity {
    public static String PACKAGE_NAME = "package_name";
    public static String CLASS_NAME = "class_name";
    public static String CLASS_NAME_OTHER = "class_name_other";
    private Preference mSimPref[];
    private int mPhoneNumber = 0;
    private Preference mOtherPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhoneNumber = TelephonyManager.getPhoneCount();
        mSimPref = new Preference[mPhoneNumber];

        /* SPRD: modify for bug338542 @{ */
        addPreferencesFromResource(R.xml.mobile_sim_choose);
        PreferenceScreen prefSet = getPreferenceScreen();
        String strname=null;  //diwei add 
        for (int i = 0; i < mPhoneNumber; i++) {
            mSimPref[i] = new Preference(this, null);

                /*if(com.android.featureoption.FeatureOption.PRJ_FEATURE_SPICES_SIM_NAME){	
                    if(i==0){
                        mSimPref[i].setTitle(getResources().getString(R.string.mobile_network_settings_ex_1));
                        mSimPref[i].setKey(getResources().getString(R.string.mobile_network_settings_ex_1));
                    }else if(i==1){
                        mSimPref[i].setTitle(getResources().getString(R.string.mobile_network_settings_ex_2));
                        mSimPref[i].setKey(getResources().getString(R.string.mobile_network_settings_ex_2));
                    }else{
                        mSimPref[i].setTitle(getResources().getString(
                        getIntent().getIntExtra("title_name",
                        R.string.mobile_network_settings_ex), i + 1));
                        mSimPref[i].setKey(getResources().getString(
                        getIntent().getIntExtra("title_name",
                        R.string.mobile_network_settings_ex), i + 1));
                    }
                }else{		
                    mSimPref[i].setTitle(getResources().getString(
                    getIntent().getIntExtra("title_name",
                    R.string.mobile_network_settings_ex), i + 1));
                    mSimPref[i].setKey(getResources().getString(
                    getIntent().getIntExtra("title_name",
                    R.string.mobile_network_settings_ex), i + 1));
                }
                */
                //diwei add for set sim name 
                strname=getResources().getString(getIntent().getIntExtra("title_name", R.string.mobile_network_settings_ex), i + 1);
                mSimPref[i].setTitle(SimManager.get(getApplicationContext()).getSimName(i,strname));
                mSimPref[i].setKey(SimManager.get(getApplicationContext()).getSimName(i,strname));
  						
            prefSet.addPreference(mSimPref[i]);
        }

        if (getIntent().getStringExtra(MobileSimChoose.CLASS_NAME_OTHER) != null) {
            mOtherPref = new Preference(this, null);
            mOtherPref.setTitle(R.string.mobile_network_settings_other);
            prefSet.addPreference(mOtherPref);
        }
        /* @} */

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED_DONE);
        registerReceiver(myReceiver, intentFilter);
        //SPRD: modify for bug346260
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
           actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSimList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }

    private BroadcastReceiver  myReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.startsWith(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                updateSimList();
            }
        }
    };

    private void updateSimList() {
        boolean isAirplaneModeOn = Settings.Global.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        boolean isRadioBusy = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.RADIO_OPERATION, 0) != 0;
        for (int i = 0; i < mPhoneNumber; i++) {
            if (!isAirplaneModeOn && !isRadioBusy && isSimAvailable(i)) {
                mSimPref[i].setEnabled(true);
            } else {
                mSimPref[i].setEnabled(false);
            }
        }
    }

    private boolean isSimAvailable(int phoneId) {
        boolean isStandby = Settings.System.getInt(getContentResolver(),
                Settings.System.SIM_STANDBY + phoneId, 1) == 1;
        boolean isSimReady = TelephonyManager.getDefault(phoneId).getSimState()
                == TelephonyManager.SIM_STATE_READY;
        return isStandby && isSimReady;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(getIntent().getStringExtra(
                        MobileSimChoose.PACKAGE_NAME),
                        getIntent().getStringExtra(MobileSimChoose.CLASS_NAME)));
        for (int i = 0; i < mPhoneNumber; i++) {
            if (preference == mSimPref[i]) {
                intent.putExtra(WirelessSettings.SUB_ID, i);
                break;
            }
        }
        if (preference == mOtherPref) {
            intent.setComponent(new ComponentName(getIntent().getStringExtra(
                            MobileSimChoose.PACKAGE_NAME),
                            getIntent().getStringExtra(MobileSimChoose.CLASS_NAME_OTHER)));
        }
        startActivity(intent);
        return true;
    }
    //SPRD: modify for bug346260
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
