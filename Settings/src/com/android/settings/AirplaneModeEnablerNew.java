/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
//import android.preference.CheckBoxPreference;
//import android.preference.Preference;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import android.util.Log;
import android.os.UserHandle;
import android.telephony.TelephonyManager;


public class AirplaneModeEnablerNew implements CompoundButton.OnCheckedChangeListener  {

    private final Context mContext;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    
    //private final CheckBoxPreference mCheckBoxPref;
    private Switch mSwitch;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;
    protected static final int EVENT_SERVICE_CHANGE_WAIT_TIMEOUT = 4;// message
    protected static final int DELAY_AIRPLANE_SET_TIME = 30000; // time (msec)

    private AirplanModeChange mAirplanModeChange;

    private boolean isCheckBottomAllowed = true;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onAirplaneModeChanged();
                    break;
		case EVENT_SERVICE_CHANGE_WAIT_TIMEOUT:
                    onAirplaneModeChangedTimedout();
		    break;
            }
        }
    };

	/* SPRD: for airplanemode optimization @{ */
	private void onAirplaneModeChangedTimedout() {
		mSwitch.setEnabled(true);
		TelephonyManager.setRadioBusy(mContext, false);
	}
	

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onAirplaneModeChanged();
        }
    };

    public AirplaneModeEnablerNew(Context context, Switch switch_) {
        
        mContext = context;
        mSwitch = switch_;
        mAirplanModeChange = new AirplanModeChange();
        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
    }

    public void resume() {
        mSwitch.setChecked(isAirplaneModeOn(mContext));
        mPhoneStateReceiver.registerIntent();
        mSwitch.setOnCheckedChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true,
                mAirplaneModeObserver);
    }
    
    public void pause() {
        mPhoneStateReceiver.unregisterIntent();
        mSwitch.setOnCheckedChangeListener(null);
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
    }
    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.setChecked(isAirplaneModeOn(mContext));
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		setAirplaneModeOn(isChecked);
    }
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {
       /* delay 2 sec to handle airplane mode change to fix bug about phone crash when we enable and disable
        * among wifi,wifi direct and airplane mode
        * */
        // Update the UI to reflect system setting
        mSwitch.setEnabled(false);
        mSwitch.setChecked(enabling);

	mHandler.removeMessages(EVENT_SERVICE_CHANGE_WAIT_TIMEOUT);
        mHandler.sendEmptyMessageDelayed(EVENT_SERVICE_CHANGE_WAIT_TIMEOUT, DELAY_AIRPLANE_SET_TIME);		
        
        Message msg = Message.obtain();
        msg.obj = enabling;
        mAirplanModeChange.sendMessageDelayed(msg, 2000);//send msg after 2000 msec
	
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified
     * that mobile radio is powered up/down. We should not have dependency
     * on one radio alone. We need to do the following:
     * - handle the case of wifi/bluetooth failures
     * - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
	  if(mSwitch.isEnabled())
        {		
        	mSwitch.setChecked(isAirplaneModeOn(mContext));
	  }
    }
    
    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }

    /* delay 2 sec to handle airplane mode change to fix bug about phone crash when we enable and disable
     * among wifi,wifi direct and airplane mode
     * */
    private class AirplanModeChange extends Handler{
        @Override
        public void handleMessage(Message message){
            // Change the system setting
            // Change the system setting
            TelephonyManager.setRadioBusy(mContext, true);
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                                    (Boolean) message.obj ? 1 : 0);
            mSwitch.setEnabled(true);
            // Post the intent
            // Post the intent
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", (Boolean) message.obj);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            // SPRD: modify by bug 259541
            //if (!needWaitResponse()) {
            //    onAirplaneModeChangedTimedout();
            //}
        }
    }


}
