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
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.util.Log;
import android.provider.Settings;
import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.List;

 //revo lyq 20141028  for lights		
public class LedsSettings extends SettingsPreferenceFragment {

    private static final String TAG = "LedsSettingsActivity";
    private CheckBoxPreference mCharging;
    private CheckBoxPreference mLowBattery;	
    private CheckBoxPreference mNotifications;	

    ContentResolver resolver;
    int flag;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	 resolver = getActivity().getContentResolver();	

        addPreferencesFromResource(R.xml.leds_settings);
	 mCharging = (CheckBoxPreference)findPreference("charging");	 	
	 mLowBattery = (CheckBoxPreference)findPreference("low_battery");	
	 mNotifications = (CheckBoxPreference)findPreference("notifications");	

	 flag= Settings.System.getInt(resolver, Settings.System.LIGHTS_CHARGING,1);
	 mCharging.setChecked(flag==1?true:false);
	 flag= Settings.System.getInt(resolver, Settings.System.LIGHTS_LOW_BATTERY,1);
	 mLowBattery.setChecked(flag==1?true:false);
	 flag= Settings.System.getInt(resolver, Settings.System.LIGHTS_NOTIFICATIONS,1);
	 mNotifications.setChecked(flag==1?true:false);	  
		
    }

   @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
         boolean value;
        //revo lyq 20140926 for battery percent
        if (preference == mCharging) {
             value = mCharging.isChecked();			
             Log.d(TAG, "mCharging value =="+value);
            Settings.System.putInt(resolver, Settings.System.LIGHTS_CHARGING,value?1:0);
        }else if(preference == mLowBattery)	{        
             value = mLowBattery.isChecked();			
             Log.d(TAG, "mLowBattery value =="+value);
	     Settings.System.putInt(resolver, Settings.System.LIGHTS_LOW_BATTERY,value?1:0);	 
    	 }else if(preference == mNotifications){
             value = mNotifications.isChecked();			
             Log.d(TAG, " mNotifications value =="+value);    
		Settings.System.putInt(resolver, Settings.System.LIGHTS_NOTIFICATIONS,value?1:0);	 	 
   	 }
		 
        return true;
    }	

   
}
