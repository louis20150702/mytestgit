/** Created by Spreadst */
/*
 * Copyright (C) 2011 The Android Open Source Project
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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.Debug;
import android.util.Log;

/**
 * Panel showing data usage history across various networks, including options
 * to inspect based on usage cycle and control through {@link NetworkPolicy}.
 */
public class DataUsageRestrictBackground extends Activity {
    private static final String TAG = "DataUsageRestrictBackground";
    private static final boolean DEBUG = Debug.isDebug();

    private boolean isDataUsageRestrict = false;
    private NetworkPolicyManager mPolicyManager;
    private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if (DEBUG) Log.d(TAG, "onReceive action:"+action);
            if(Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)){
                DataUsageRestrictBackground.this.finish();
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mPolicyManager = new NetworkPolicyManager(INetworkPolicyManager.Stub.asInterface(
//                ServiceManager.getService(NETWORK_POLICY_SERVICE)));
        mPolicyManager = NetworkPolicyManager.from(getApplicationContext());
        isDataUsageRestrict = mPolicyManager.getRestrictBackground();
        if (DEBUG) Log.d(TAG, "isDataUsageRestrict = " + isDataUsageRestrict);

        new AlertDialog.Builder(this).setCancelable(false)
        .setTitle(R.string.back_groud_data_usage_restrict_protocal)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setMessage(R.string.accept_back_groud_data_usage_restrict_protocal)
        .setPositiveButton(R.string.mobile_net_yes,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    if (isDataUsageRestrict != false) {
                        mPolicyManager.setRestrictBackground(false);
                        if (DEBUG) Log.d(TAG, "isDataUsageRestrict = " + isDataUsageRestrict + "  set restrict background to true !");
                    }
                    finish();
                }
            })
        .setNegativeButton(R.string.mobile_net_no,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    if (isDataUsageRestrict != true) {
                        mPolicyManager.setRestrictBackground(true);
                        if (DEBUG) Log.d(TAG, "isDataUsageRestrict = " + isDataUsageRestrict + "  set restrict background to false !");
                    }
                    finish();
                }
            }).create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter mIntentFilter=new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
