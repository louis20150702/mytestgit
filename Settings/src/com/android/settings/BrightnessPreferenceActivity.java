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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import android.app.Activity;
import android.view.LayoutInflater;
import android.app.Dialog;
import android.app.AlertDialog;
import android.widget.ScrollView;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.view.Window;
import android.view.WindowManager;
import com.android.featureoption.FeatureOption;


public class BrightnessPreferenceActivity extends Activity/*SeekBarDialogPreference*/ implements
        SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener {

    private SeekBar mSeekBar;
    private CheckBox mCheckBox;

    private int mOldBrightness;
    private int mOldAutomatic=0;

    private boolean mAutomaticAvailable=false;

    private boolean mRestoredOldState;

    
    private AlertDialog brightnessDialog;
    

    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck
    private int mScreenBrightnessDim = -1;

    private static final int MAXIMUM_BACKLIGHT = 255;//android.os.Power.BRIGHTNESS_ON;

    private ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessChanged();
        }
    };

    private ContentObserver mBrightnessModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessModeChanged();
        }
    };
/*
    public BrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
		//revo lyq 20130527 begin
        if(android.os.SystemProperties.get("ro.device.support.lsensor").equals("false"))
        {
           mAutomaticAvailable = false;
        }
		//revo lyq 20130527 end        
        setDialogLayoutResource(R.layout.preference_dialog_brightness);
        setDialogIcon(R.drawable.ic_settings_display);
    }
*/
    @Override
     protected void onCreate(Bundle savedInstanceState) {
	 super.onCreate(savedInstanceState);
	 requestWindowFeature(Window.FEATURE_NO_TITLE);
	 getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);  

	 mAutomaticAvailable = getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
		//revo lyq 20130527 begin
        if(android.os.SystemProperties.get("ro.device.support.lsensor").equals("false"))
        {
           mAutomaticAvailable = false;
        }
		//revo lyq 20130527 end        
        //setDialogLayoutResource(R.layout.preference_dialog_brightness);
        //setDialogIcon(R.drawable.ic_settings_display);

	mScreenBrightnessDim = this.getResources().getInteger(com.android.internal.R.integer.config_screenBrightnessDim);
	
	getWindow().setBackgroundDrawableResource(R.drawable.brightness_bg);
	 
	 showDialog();


	this.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                mBrightnessObserver);

        this.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
                mBrightnessModeObserver);
        mRestoredOldState = false;		
    }


	private void showDialog() {
		onCreateDialog();
		brightnessDialog.show();
	}
	
	private void onCreateDialog() {
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ScrollView localScrollView = (ScrollView)inflater.inflate(R.layout.preference_dialog_brightness_hw, null);
		mSeekBar = (SeekBar)localScrollView.findViewById(R.id.seekbar_hw);
        	mSeekBar.setMax(MAXIMUM_BACKLIGHT - mScreenBrightnessDim);
        	mOldBrightness = getBrightness(0);
        	mSeekBar.setProgress(mOldBrightness - mScreenBrightnessDim);
		mCheckBox = (CheckBox)localScrollView.findViewById(R.id.automatic_mode);
		if (mAutomaticAvailable) {
            		mCheckBox.setOnCheckedChangeListener(this);
            		mOldAutomatic = getBrightnessMode(0);
            		mCheckBox.setChecked(mOldAutomatic != 0);
        	} else {
           	 	mCheckBox.setVisibility(View.GONE);
        	}
        	mSeekBar.setOnSeekBarChangeListener(this);
	if(false/*FeatureOption.PRJ_FEATURE_HW_CONTACTS_PHONE*/){
		brightnessDialog = new AlertDialog.Builder(this)
							.setTitle(R.string.brightness)
							.setView(localScrollView)
							.setIcon(R.drawable.ic_settings_display)
							.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener(){
								public void onClick (DialogInterface dialog, int which){
									onDialogClosed(false);
								}
							})
							.setPositiveButton(getResources().getString(R.string.dlg_ok), new DialogInterface.OnClickListener(){
								public void onClick (DialogInterface dialog, int which){
									onDialogClosed(true);
								}
							})
							.create();
		}
	else
		{
		brightnessDialog = new AlertDialog.Builder(this)
							.setTitle(R.string.brightness)
							.setView(localScrollView)
							.setIcon(R.drawable.ic_settings_display_pop)
							.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener(){
								public void onClick (DialogInterface dialog, int which){
									onDialogClosed(false);
								}
							})
							.setPositiveButton(getResources().getString(R.string.dlg_ok), new DialogInterface.OnClickListener(){
								public void onClick (DialogInterface dialog, int which){
									onDialogClosed(true);
								}
							})
							.create();

		}
		brightnessDialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
			public void onDismiss (DialogInterface dialog) {			
				onDialogClosed(false);
			}
		});							
		
	}


	private void onDialogClosed(boolean pressOK) {
		final ContentResolver resolver = this.getContentResolver();
		resolver.unregisterContentObserver(mBrightnessObserver);
		resolver.unregisterContentObserver(mBrightnessModeObserver);
		//if (positiveResult) {
		if(pressOK) {
			setBrightness(mSeekBar.getProgress());
			Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS,  mSeekBar.getProgress() + mScreenBrightnessDim);
		}
		 else {
			restoreOldState();
		}
		finish();
	}
/*
	@Override
	public void onBackPressed() {
		finish();
		brightnessDialog.dismiss();
		

		super.onBackPressed();
	}
	*/
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		try{
			brightnessDialog.dismiss();
		}catch (Exception e) {

		}
		super.onDestroy();
	}


/*
    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        this.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                mBrightnessObserver);

        this.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
                mBrightnessModeObserver);
        mRestoredOldState = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(MAXIMUM_BACKLIGHT - mScreenBrightnessDim);
        mOldBrightness = getBrightness(0);
        mSeekBar.setProgress(mOldBrightness - mScreenBrightnessDim);

        mCheckBox = (CheckBox)view.findViewById(R.id.automatic_mode);
        if (mAutomaticAvailable) {
            mCheckBox.setOnCheckedChangeListener(this);
            mOldAutomatic = getBrightnessMode(0);
            mCheckBox.setChecked(mOldAutomatic != 0);
        } else {
            mCheckBox.setVisibility(View.GONE);
        }
        mSeekBar.setOnSeekBarChangeListener(this);
    }
	*/

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        setBrightness(progress + mScreenBrightnessDim);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setMode(isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (!isChecked) {
            setBrightness(mSeekBar.getProgress() + mScreenBrightnessDim);
        }
    }

    private int getBrightness(int defaultValue) {
        int brightness = defaultValue;
        try {
            brightness = Settings.System.getInt(this.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException snfe) {
        }
        return brightness;
    }

    private int getBrightnessMode(int defaultValue) {
        int brightnessMode = defaultValue;
        try {
            brightnessMode = Settings.System.getInt(this.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (SettingNotFoundException snfe) {
        }
        return brightnessMode;
    }

    private void onBrightnessChanged() {
        int brightness = getBrightness(MAXIMUM_BACKLIGHT);
        mSeekBar.setProgress(brightness - mScreenBrightnessDim);
    }

    private void onBrightnessModeChanged() {
        boolean checked = getBrightnessMode(0) != 0;
        mCheckBox.setChecked(checked);
    }

/*
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final ContentResolver resolver = this.getContentResolver();
        resolver.unregisterContentObserver(mBrightnessObserver);
        resolver.unregisterContentObserver(mBrightnessModeObserver);
        if (positiveResult) {
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    mSeekBar.getProgress() + mScreenBrightnessDim);
        } else {
            restoreOldState();
        }

        
    }
*/
    private void restoreOldState() {
        // if (mRestoredOldState) return; // bug 202293 begin

        if (mAutomaticAvailable) {
            setMode(mOldAutomatic);
        }
        if (!mAutomaticAvailable || mOldAutomatic == 0) {
            setBrightness(getBrightness(0));
        }
        mRestoredOldState = true;
    }

    private void setBrightness(int brightness) {
        try{
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if(power != null){
                power.setTemporaryScreenBrightnessSettingOverride(brightness);
            }
        }catch(Exception e){

        }
    }

    private void setMode(int mode) {
        if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            mSeekBar.setVisibility(View.GONE);
        } else {
            mSeekBar.setVisibility(View.VISIBLE);
        }
        Settings.System.putInt(this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

/*
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) return superState;

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.automatic = mCheckBox.isChecked();
        myState.progress = mSeekBar.getProgress();
        myState.oldAutomatic = mOldAutomatic == 1;
        myState.oldProgress = mOldBrightness;

        // Restore the old state when the activity or dialog is being paused
        restoreOldState();
        // bug 202293 begin
        mSeekBar.setProgress(mOldBrightness - mScreenBrightnessDim);
        if (mAutomaticAvailable) {
            mCheckBox.setChecked(mOldAutomatic != 0);
        }
        // bug 202293 end
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mOldBrightness = myState.oldProgress;
        mOldAutomatic = myState.oldAutomatic ? 1 : 0;
        setMode(myState.automatic ? 1 : 0);
        setBrightness(myState.progress + mScreenBrightnessDim);
        // bug 202293 begin
        mSeekBar.setProgress(myState.progress);
        if (mAutomaticAvailable) {
            mCheckBox.setChecked(myState.automatic);
        }
        // bug 202293 end
    }
    */
/*
    private static class SavedState extends BaseSavedState {

        boolean automatic;
        boolean oldAutomatic;
        int progress;
        int oldProgress;

        public SavedState(Parcel source) {
            super(source);
            automatic = source.readInt() == 1;
            progress = source.readInt();
            oldAutomatic = source.readInt() == 1;
            oldProgress = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(automatic ? 1 : 0);
            dest.writeInt(progress);
            dest.writeInt(oldAutomatic ? 1 : 0);
            dest.writeInt(oldProgress);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    */
    
	@Override
    public void onResume() {
		super.onResume();
		mSeekBar.setProgress(getBrightness(0));
	}
}

