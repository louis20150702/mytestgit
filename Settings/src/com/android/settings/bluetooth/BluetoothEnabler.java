/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.bluetooth;

import java.util.concurrent.atomic.AtomicBoolean;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.WirelessSettings;

import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.net.wifi.WifiManager;

/**
 * BluetoothEnabler is a helper to manage the Bluetooth on/off checkbox
 * preference. It turns on/off Bluetooth and ensures the summary of the
 * preference reflects the current state.
 */
public final class BluetoothEnabler implements CompoundButton.OnCheckedChangeListener {
    private final Context mContext;
    private Switch mSwitch;
    private boolean mValidListener;
    private final LocalBluetoothAdapter mLocalAdapter;
    private AtomicBoolean mRegister = new AtomicBoolean(false);
    private final IntentFilter mIntentFilter;
    private boolean supportBtWifiSoftApCoexit = true;
    private static String TAG = "BluetoothEnabler";
    private SwitchUiHandler mSwitchHandler;
    private final int MSG_RECOVER_STATE = 1;
    private WifiManager mWifiManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
           //   int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
          //    handleStateChanged(state);

            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                handleStateChanged(state);
            }
        }
    };

    public BluetoothEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mValidListener = false;

        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
        if (manager == null) {
            // Bluetooth is not supported
            mLocalAdapter = null;
            mSwitch.setEnabled(false);
        } else {
            mLocalAdapter = manager.getBluetoothAdapter();
        }
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (SystemProperties.get("ro.btwifisoftap.coexist", "true").equals(
                "false")) {
            if (mWifiManager == null) {
                mWifiManager = (WifiManager) mContext
                        .getSystemService(Context.WIFI_SERVICE);
            }
            mSwitchHandler = new SwitchUiHandler();
            supportBtWifiSoftApCoexit = false;
        }
    }

    public void resume() {
        if (mLocalAdapter == null) {
            mSwitch.setEnabled(false);
            return;
        }

        // Bluetooth state is not sticky, so set it manually
        handleStateChanged(mLocalAdapter.getBluetoothState());

        if (!mRegister.get()) {
            mContext.registerReceiver(mReceiver, mIntentFilter);
            mRegister.set(true);
        }
        mSwitch.setOnCheckedChangeListener(this);
        mValidListener = true;
    }

    public void pause() {
        if (mLocalAdapter == null) {
            return;
        }

        if (mRegister.get()) {
            mContext.unregisterReceiver(mReceiver);
            mRegister.set(false);
        }
        mSwitch.setOnCheckedChangeListener(null);
        mValidListener = false;
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(mValidListener ? this : null);

        int bluetoothState = BluetoothAdapter.STATE_OFF;
        if (mLocalAdapter != null) bluetoothState = mLocalAdapter.getBluetoothState();
        boolean isOn = bluetoothState == BluetoothAdapter.STATE_ON;
        boolean isOff = bluetoothState == BluetoothAdapter.STATE_OFF;
        setChecked(isOn);
        mSwitch.setEnabled(isOn || isOff);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Show toast message if Bluetooth is not allowed in airplane mode
        if (isChecked &&
                !WirelessSettings.isRadioAllowed(mContext, Settings.Global.RADIO_BLUETOOTH)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off
            buttonView.setChecked(false);
        }

        if (!supportBtWifiSoftApCoexit) {
            if (isChecked && mWifiManager.isSoftapEnablingOrEnabled()) {
                mSwitchHandler.sendEmptyMessage(MSG_RECOVER_STATE);
                Toast.makeText(this.mContext,
                        R.string.bt_softap_cannot_coexist, Toast.LENGTH_SHORT)
                        .show();
                return;
            } else if (isChecked == false && mWifiManager.isSoftapEnablingOrEnabled()){
                return;
            }
        }
        Log.d(TAG,"Check bluetooth enable change");
        if (mLocalAdapter != null) {
            mLocalAdapter.setBluetoothEnabled(isChecked);
        }
        mSwitch.setEnabled(false);
    }

    class SwitchUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            mSwitch.setChecked(false);
        }
    }

    void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                setChecked(true);
                mSwitch.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                setChecked(false);
                mSwitch.setEnabled(true);
                break;
            default:
                setChecked(false);
                mSwitch.setEnabled(true);
        }
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != mSwitch.isChecked()) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(null);
            }
            mSwitch.setChecked(isChecked);
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(this);
            }
        }
    }

    //revo lyq 2014 for advan settings  
    public int getBluetoothState(){
        int bluetoothState = BluetoothAdapter.STATE_OFF;
        if (mLocalAdapter != null) bluetoothState = mLocalAdapter.getBluetoothState();
       return bluetoothState;		
    }
	
}
