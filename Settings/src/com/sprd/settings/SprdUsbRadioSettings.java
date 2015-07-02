package com.sprd.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.Utils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.provider.Settings;
import com.android.settings.R;

public class SprdUsbRadioSettings extends PreferenceActivity
      implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private final String TAG = "SprdUsbRadioSettings";

    private static final String KEY_CHARGE_ONLY = "usb_charge_only";
    private static final String KEY_UMS = "usb_storage";
    private static final String KEY_CDROM = "usb_virtual_drive";
    private static final String KEY_MTP = "usb_mtp";
    private static final String KEY_PTP = "usb_ptp";
    private static final String KEY_ADB = "enable_adb";

    private UsbRadioPreference mMtp;
    private UsbRadioPreference mPtp;
    private UsbRadioPreference mUsbChargeOnly;
    private UsbRadioPreference mUms;
    private UsbRadioPreference mCdrom;
    private CheckBoxPreference mAdb;

    private UsbManager mUsbManager = null;
    private BroadcastReceiver mPowerDisconnectReceiver = null;

    private boolean mDialogClicked;
    private Dialog mAdbDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sprd_usb_radio_settings);

        mUsbChargeOnly = (UsbRadioPreference) findPreference(KEY_CHARGE_ONLY);
        mUms = (UsbRadioPreference) findPreference(KEY_UMS);
        mCdrom = (UsbRadioPreference) findPreference(KEY_CDROM);
        mMtp = (UsbRadioPreference) findPreference(KEY_MTP);
        mPtp = (UsbRadioPreference) findPreference(KEY_PTP);
        mAdb = (CheckBoxPreference) findPreference(KEY_ADB);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mPowerDisconnectReceiver = new PowerDisconnectReceiver();
        registerReceiver(mPowerDisconnectReceiver, new IntentFilter(
                Intent.ACTION_POWER_DISCONNECTED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPowerDisconnectReceiver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        if (Utils.isMonkeyRunning()) {
            return false;
        }

        String mCurrentfunction = getCurrentFunction();
        Log.d(TAG, "onPreferenceTreeClick, mCurrentfunction = " + mCurrentfunction);

        if (preference == mUsbChargeOnly) {
            if(!UsbManager.USB_FUNCTION_NONE.equals(mCurrentfunction)) {
                mUsbChargeOnly.setChecked(true);
                mUms.setChecked(false);
                mCdrom.setChecked(false);
                mMtp.setChecked(false);
                mPtp.setChecked(false);
                boolean mUsbCharged = mUsbChargeOnly.isChecked();
                mUsbManager.setCurrentFunction(
                        UsbManager.USB_FUNCTION_NONE, true);
            }
        } else if (preference == mUms) {
            if(!UsbManager.USB_FUNCTION_MASS_STORAGE.equals(mCurrentfunction)) {
                mUsbChargeOnly.setChecked(false);
                mUms.setChecked(true);
                mCdrom.setChecked(false);
                mMtp.setChecked(false);
                mPtp.setChecked(false);
                boolean mUmsSelected = mUms.isChecked();
                mUsbManager.setCurrentFunction(
                        UsbManager.USB_FUNCTION_MASS_STORAGE, true);
             }
        } else if (preference == mCdrom) {
            if(!UsbManager.USB_FUNCTION_CDROM.equals(mCurrentfunction)) {
                mUsbChargeOnly.setChecked(false);
                mUms.setChecked(false);
                mCdrom.setChecked(true);
                mMtp.setChecked(false);
                mPtp.setChecked(false);
                boolean mCdromSelected = mCdrom.isChecked();
                mUsbManager.setCurrentFunction(
                        UsbManager.USB_FUNCTION_CDROM, true);
            }
        } else if (preference == mMtp) {
            if(!UsbManager.USB_FUNCTION_MTP.equals(mCurrentfunction)) {
                mUsbChargeOnly.setChecked(false);
                mUms.setChecked(false);
                mCdrom.setChecked(false);
                mMtp.setChecked(true);
                mPtp.setChecked(false);
                boolean mMtpSelected = mMtp.isChecked();
                mUsbManager.setCurrentFunction(
                        UsbManager.USB_FUNCTION_MTP, true);
            }
        } else if (preference == mPtp) {
            if(!UsbManager.USB_FUNCTION_PTP.equals(mCurrentfunction)) {
                mUsbChargeOnly.setChecked(false);
                mUms.setChecked(false);
                mCdrom.setChecked(false);
                mMtp.setChecked(false);
                mPtp.setChecked(true);
                boolean mPtpSelected = mPtp.isChecked();
                mUsbManager.setCurrentFunction(
                        UsbManager.USB_FUNCTION_PTP, true);
            }
        }else if (preference == mAdb) {
            if (mAdb.isChecked()) {
                mDialogClicked = false;
                if (mAdbDialog != null) dismissDialogs();
                mAdbDialog = new AlertDialog.Builder(this).setMessage(
                        getResources().getString(R.string.adb_warning_message))
                        .setTitle(R.string.adb_warning_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show();
                mAdbDialog.setOnDismissListener(this);
            } else {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.ADB_ENABLED, 0);
            }
        }

        return true;
    }

    public String getCurrentFunction() {
        String functions = SystemProperties.get("sys.usb.config", "");
        int commaIndex = functions.indexOf(',');
        if (commaIndex > 0) {
            return functions.substring(0, commaIndex);
        } else {
            return functions;
        }
    }

    private void updateUI() {
        String mCurrentfunction = getCurrentFunction();
        Log.d(TAG, "updateUI, mCurrentfunction = " + mCurrentfunction);

        mUsbChargeOnly.setChecked(UsbManager.USB_FUNCTION_NONE.equals(mCurrentfunction));
        mUms.setChecked(UsbManager.USB_FUNCTION_MASS_STORAGE.equals(mCurrentfunction));
        mCdrom.setChecked(UsbManager.USB_FUNCTION_CDROM.equals(mCurrentfunction));
        mMtp.setChecked(UsbManager.USB_FUNCTION_MTP.equals(mCurrentfunction));
        mPtp.setChecked(UsbManager.USB_FUNCTION_PTP.equals(mCurrentfunction));

        boolean adb_enable = (Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) != 0);
        mAdb.setChecked(adb_enable);
    }

    private class PowerDisconnectReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            SprdUsbRadioSettings.this.finish();
        }
    }

    private void dismissDialogs() {
        if (mAdbDialog != null) {
            mAdbDialog.dismiss();
            mAdbDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mAdbDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mDialogClicked = true;
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.ADB_ENABLED, 1);
                // SPRD: checked the adb box
                mAdb.setChecked(true);
            } else {
                // Reset the toggle
                mAdb.setChecked(false);
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (dialog == mAdbDialog) {
            if (!mDialogClicked) {
                mAdb.setChecked(false);
            }
            mAdbDialog = null;
        }
    }
}
