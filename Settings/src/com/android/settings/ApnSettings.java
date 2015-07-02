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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import java.util.ArrayList;
import android.sim.SimManager;

public class ApnSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static boolean mRestoreDefaultApnMode;

    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;

    private String mSelectedKey;

    public static int mPhoneId = 0; // SPRD: add for multi-sim

    private IntentFilter mMobileStateFilter;
    
    private SimManager mSimManager;     //diwei add for set sim name

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    } else {
                        showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
            }
        }
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPhoneId = getIntent().getIntExtra(WirelessSettings.SUB_ID,
                               TelephonyManager.getDefaultDataPhoneId(getApplicationContext())); // SPRD
                                                                                                 // :
                                                                                                 // add
                                                                                                 // by
                                                                                                 // spreadst
        Log.d(TAG, "onCreate phoneId = " + mPhoneId);
        addPreferencesFromResource(R.xml.apn_settings);
        getListView().setItemsCanFocus(true);

        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);

        //mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        mSimManager= SimManager.get(getApplicationContext());   //diwei add for set sim name
   
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mMobileStateReceiver, mMobileStateFilter);

        /** SPRD: Bug 327811 title add phoneId @{ */
        if (TelephonyManager.isMultiSim()) {
            //diwei add for set sim name 
            this.setTitle(mSimManager.getSimName(mPhoneId,getResources().getString(R.string.apn_settings_ex, mPhoneId + 1)));
        }
        /** @} */

        if (!mRestoreDefaultApnMode) {
            fillList();
        } else {
            showDialog(DIALOG_RESTORE_DEFAULTAPN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mMobileStateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
    }

    private void fillList() {
        /* SPRD: for multi-sim @{ */
        // String where = "numeric=\""
        // +
        // android.os.SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC,
        // "")
        // + "\"";
        //
        // Cursor cursor =
        // getContentResolver().query(Telephony.Carriers.CONTENT_URI, new
        // String[] {
        // "_id", "name", "apn", "type"}, where, null,
        // Telephony.Carriers.DEFAULT_SORT_ORDER);

        // Uri contentUri = Telephony.Carriers.CONTENT_URI;
        String where;
        Uri contentUri = Telephony.Carriers.getContentUri(mPhoneId,null);

        if (TelephonyManager.isMultiSim()) {
            // APNs are listed based on the MCC+MNC.
            // Get the value from appropriate Telephony Property based on the
            // subscription.
            where = "numeric=\""
                        + android.os.SystemProperties.get(TelephonyManager.getProperty(
                                TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, mPhoneId), "")
                        + "\"";
        } else {
            where = "numeric=\""
                            + android.os.SystemProperties.get(
                                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "")
                            + "\"";
        }
        where += " and name!='CMCC DM'";

        Log.d(TAG,"where = " + where);
        Cursor cursor = getContentResolver().query(contentUri, new String[] {
                                "_id", "name", "apn", "type"
        }, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);
        /* @} */

        if (cursor != null) {
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();

            ArrayList<Preference> mmsApnList = new ArrayList<Preference>();

            /* SPRD: add by spreadst @{ */
            String firstKey = null;
            boolean hasKey = false;
            ApnPreference firstPref = new ApnPreference(this);
            /* @} */

            mSelectedKey = getSelectedApnKey();
            Log.d(TAG, "mSelectedKey = " + mSelectedKey);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                Log.d(TAG, "name = " + name + "apn = " + apn + "key = " + key + "type = " + type);

                ApnPreference pref = new ApnPreference(this);

                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);

                /* SPRD: for multi-sim @{*/
                // boolean selectable = ((type == null) || !type.equals("mms"));
                boolean selectable = ((type == null) || (type.indexOf("default") != -1)
                                        || (type.equals("*")));
                /* @} */
                pref.setSelectable(selectable);
                if (selectable) {
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                        hasKey = true;
                        Log.d(TAG, "mSelectedKey has a value: firstKey = " + firstKey + " hasKey = " + hasKey + " firstPref = " + firstPref);
                    } else if (mSelectedKey == null) {
                        pref.setChecked();
                        hasKey = true;
                        Log.d(TAG, "mSelectedKey is null: firstKey = " + firstKey + " hasKey = " + hasKey + " firstPref = " + firstPref);
                        setSelectedApnKey(key);
                    }
                    apnList.addPreference(pref);
                    // if mSelectedKey dose not match with the operator,
                    // remember the first key as firstKey
                    if (firstKey == null) {
                        firstPref = pref;
                        firstKey = key;
                    }
                } else {
                    mmsApnList.add(pref);
                }
                cursor.moveToNext();
            }
            cursor.close();

            for (Preference preference : mmsApnList) {
                apnList.addPreference(preference);
            }

            /* SPRD: add by spreadst @{ */
            // set firstKey to be SelectedApnKey
            if (!hasKey) {
                firstPref.setChecked();
                setSelectedApnKey(firstKey);
            }
            Log.d(TAG, "Final: firstKey = " + firstKey + " hasKey = " + hasKey + " firstPref = " + firstPref);
            Log.d(TAG,"apnList = " + apnList);
            /* @} */
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_NEW, 0,
                getResources().getString(R.string.menu_new))
                .setIcon(android.R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;

        case MENU_RESTORE:
            restoreDefaultApn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        /* SPRD: for multi-sim @{ */
        // startActivity(new Intent(Intent.ACTION_INSERT,
        // Telephony.Carriers.CONTENT_URI));
        Uri uri;
        uri = Telephony.Carriers.getContentUri(mPhoneId,null);
        Intent intent = new Intent(Intent.ACTION_INSERT, uri);
        if (TelephonyManager.isMultiSim()) {
            intent.putExtra(WirelessSettings.SUB_ID, mPhoneId);
        }
        startActivity(intent);
        /* @} */
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        // Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        Uri url;
        url = ContentUris.withAppendedId(Telephony.Carriers.getContentUri(mPhoneId,null), pos);
        startActivity(new Intent(Intent.ACTION_EDIT, url));
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        /* SPRD: for multi-sim @{ */
        // values.put(APN_ID, mSelectedKey);
        // resolver.update(PREFERAPN_URI, values, null, null);
        values.put(getApnIdByPhoneId(mPhoneId), mSelectedKey);
        resolver.update(
                Telephony.Carriers.getContentUri(mPhoneId, Telephony.Carriers.PATH_PREFERAPN),
                values, null, null);
        /* @} */
    }

    private String getSelectedApnKey() {
        String key = null;

        /* SPRD: for multi-sim @{ */
        // Cursor cursor = getContentResolver().query(PREFERAPN_URI, new
        // String[] {"_id"},
        // null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        Cursor cursor = getContentResolver().query(
                Telephony.Carriers.getContentUri(mPhoneId, Telephony.Carriers.PATH_PREFERAPN),
                new String[] {
                    "_id"
                },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        /* @} */
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    private boolean restoreDefaultApn() {
        // SPRD: modified for bug270422 start
        mRestoreDefaultApnMode = true;
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        // SPRD: modified for bug270422 end

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    dismissDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        ApnSettings.this,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    /* SPRD: for multi-sim @{ */
                    // resolver.delete(DEFAULTAPN_URI, null, null);
                    resolver.delete(Telephony.Carriers.getContentUri(mPhoneId,
                            Telephony.Carriers.PATH_RESTORE),null,null);
                    /* @} */
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // SPRD: modified for bug270422
        if (id == DIALOG_RESTORE_DEFAULTAPN && mRestoreDefaultApnMode) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            getPreferenceScreen().setEnabled(false);
        }
    }

    /** SPRD: for multi-sim @{ */
    private String getApnIdByPhoneId(int phoneId) {
        switch (phoneId) {
            case 0:
                return APN_ID;
            default:
                return APN_ID + "_sim" + (phoneId + 1);
        }
    }
    /** @} */

    /** SPRD: Bug 327811 onNewIntent listen Home Button @{ */
    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
        mPhoneId = intent
                .getIntExtra(WirelessSettings.SUB_ID, TelephonyManager
                        .getDefaultDataPhoneId(getApplicationContext()));
        Log.i(TAG, "onNewIntent --> mPhoneId = " + mPhoneId);
    }
    /** @} */
}
