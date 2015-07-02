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
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.CheckBoxPreference;
import android.provider.Settings;


import com.android.internal.widget.LockPatternUtils;
import android.app.admin.DevicePolicyManager;



import java.util.List;

public class ScreenLockSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener{

    //screnn lock
    private LockPatternUtils mLockPatternUtils;	
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;	
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";	
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING = "biometric_weak_improve_matching";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String KEY_LOCK_WITH_POWER = "lock_with_power";// bug 216086
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST = 124;
    private static final int CONFIRM_EXISTING_REQUEST = 100; // bug 131372

    //show password
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private CheckBoxPreference mShowPassword;	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	mLockPatternUtils = new LockPatternUtils(getActivity());	
	mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        //addPreferencesFromResource(R.xml.screen_lock_settings);
       
    }

	private PreferenceScreen createPreferenceHierarchy() {
	        PreferenceScreen root = getPreferenceScreen();
	        if (root != null) {
	            root.removeAll();
	        }
	        addPreferencesFromResource(R.xml.screen_lock_settings);
	        root = getPreferenceScreen();

	        // Add options for lock/unlock screen
	        int resid = 0;
	        if (!mLockPatternUtils.isSecure()) {
	            if (mLockPatternUtils.isLockScreenDisabled()) {
	                resid = R.xml.security_settings_lockscreen_hw;
	            } else {
	                resid = R.xml.security_settings_chooser_hw;
	            }
	        } else if (mLockPatternUtils.usingBiometricWeak() &&
	                mLockPatternUtils.isBiometricWeakInstalled()) {
	            resid = R.xml.security_settings_biometric_weak;
	        } else {
	            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
	                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
	                    resid = R.xml.security_settings_pattern_hw;
	                    break;
	                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
	                    resid = R.xml.security_settings_pin_hw;
	                    break;
	                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
	                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
	                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
	                    resid = R.xml.security_settings_password_hw;
	                    break;
	            }
	        }
	        addPreferencesFromResource(resid);	 

		// Show password
		addPreferencesFromResource(R.xml.screen_lock_settings_other);
	        mShowPassword = (CheckBoxPreference) root.findPreference(KEY_SHOW_PASSWORD);
		

	        return root;
    }	

    @Override
    public void onResume() {
        super.onResume();	

	createPreferenceHierarchy();

	mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        ComponentName targetComponent = null;
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            // check 'launch confirmation activity' need to display or not for
            // bug 131372 start
            ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST, null, null)) {
                startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                        SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
            }
            // bug 131372 end

        } else if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }


   public boolean onPreferenceChange(Preference preference, Object value) {
       /* if (preference == mLockAfter) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        }*/
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        } else if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
            // handle confirm request result, to enter 'ChooseLockGeneric'.
            // bug 131372 start
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
	    
            return;
            // bug 131372 end
        }
        createPreferenceHierarchy();
    }

    public void startBiometricWeakImprove(){
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }	
	
}

