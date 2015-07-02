package com.android.settings.wifi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
// revo lyq 20140318
import com.android.featureoption.FeatureOption;

import com.android.settings.R;

public class WifiConnectionPolicy extends BroadcastReceiver {
    // Const variables
    public static final int MANUAL_CONNECT_DISABLED = 0;
    public static final int MANUAL_CONNECT_ENABLED = 1;
    public static final int MOBILE_TO_WLAN_AUTO = 0;
    public static final int MOBILE_TO_WLAN_MANUAL = 1;
    public static final int MOBILE_TO_WLAN_ASK = 2;
    private static final String TAG = "WifiConnectionPolicy";
    private static final int DIALOG_INTERVAL_MS = 60 * 60 * 1000;

    private static Resources mResources = null;

    private static WifiManager mWifiManager = null;
    private static long mTimer = -1;
    private static boolean mDialogIsShowing = false;
    private static boolean mPromptIsShowing = false;
    private static boolean isWpsRunning = false;
    private static boolean supportCMCC = SystemProperties.get("ro.operator").equals("cmcc");
    private static boolean manulDisconnect = false;

    private AlertDialog wlan2MobileDialog = null;
    ConnectivityManager mConnectivityManager;

    public static void init(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (supportCMCC == false) {
            return;
        }
        // initialize values
        if(mWifiManager == null) mWifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        if(mResources == null) mResources = context.getResources();

        // skip all the CMCC processing is wps is running
        if(isWpsRunning) {
            Log.i(TAG, "WPS is running");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Received action = " + action);
        // apply wifi policy after wifi is turned on
        if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            if(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED) {
                manulDisconnect = false;
                applyWifiDisConnectPolicy(context, true);
            }
            if(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_DISABLED) {
                // todo: do we need clear the dialog timer(by resetTimer()) after wifi is turned off?
                resetTimer();
            }
        } else if(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            // apply wifi policy after connection is lost
            SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (state == SupplicantState.DISCONNECTED) {
                Log.v(TAG, "before applyWifiDisConnectPolicy(context, true)");
                applyWifiDisConnectPolicy(context, true);
            }
        } else if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            // show dialogs according to CMCC's specification after scan results is received
            showMobileToWlanDialog(context);
        } else if ("android.net.wifi.wifi_disconnect_ap".equals(action)) {
            // only care about wifi connection lost, for cmcc case 4.10
            Log.v(TAG, "received wifi_disconnect_ap");
            WifiInfo mWifiInfo = (WifiInfo) intent.getExtra("xtra_networkInfo");

            if (isNeedPopUpWlanToMobileDialog(context, mWifiInfo) == true &&
                    mPromptIsShowing == false && mDialogIsShowing == false) {
                disableAutoConnect(true);
                popUpWlanToMobileDialog(context);
            } else {
                int manualConnect = getManualConnectPolicy(context);
                int wifiState = mWifiManager.getWifiState();

                if(isWifiConnectingOrConnected() || wifiState != WifiManager.WIFI_STATE_ENABLED || mPromptIsShowing == true) {
                    Log.d(TAG, "showMobileToWlanDialog() returned because of connecting or connected");
                    return; // do not show dialog when connection is under processing or etablished
                }
                if(manualConnect == MANUAL_CONNECT_ENABLED) {
                    // For cmcc case 4.6, other SSIDs promotion when disconnect with one AP,
                    // now instead show when disconnect with ap
                    disableAutoConnect(true);
                    if(!isTimerRunning() && !mDialogIsShowing && !mPromptIsShowing && manulDisconnect == false)
                        showMatchedAccessPoints(context, mWifiInfo);
                }
            }
        }
    }

    boolean isNeedPopUpWlanToMobileDialog(Context mContext, WifiInfo mWifiInfo) {
        //TODO Mobile Data network is available
        boolean dataEnabled = isMobileDataConnected(mContext);

        Log.d(TAG, "isNeedPopUpWlanToMobileDialog dataEnabled = " + dataEnabled + " mWifiInfo.ssid = "
                + mWifiInfo.getSSID() + " mWifiInfo.networkId = " + mWifiInfo.getNetworkId());
        if (dataEnabled) {
            Collection<AccessPoint> accessPoints = getMatchedAccessPoints(mContext);
            Log.d(TAG, " accessPoints.size() = " + accessPoints.size());
            if (mWifiInfo != null && accessPoints.size() > 0) {
                for (AccessPoint ap : accessPoints) {
                    Log.d(TAG, "ap.ssid = " + ap.ssid);
                    if (ap.networkId == mWifiInfo.getNetworkId() ||
                            (ap.ssid != null && ap.ssid.equals(AccessPoint.removeDoubleQuotes(mWifiInfo.getSSID())))) {
                        accessPoints.remove(ap);
                        break;
                    }
                }
            }
            if(accessPoints.size() == 0) {
                Log.d(TAG, "accessPoints.size() equals zero");
                return true;
            }
        } else {
            Log.d(TAG, "isNeedPopUpWlanToMobileDialog() mobile data is disabled");
        }
        Log.d(TAG, "isNeedPopUpWlanToMobileDialog() returns false");
        return false;
    }

    void popUpWlanToMobileDialog(Context mContext) {
        // do not show dialog when connection is under processing or established
        int wifiState = mWifiManager.getWifiState();
        if(isWifiConnectingOrConnected() || wifiState != WifiManager.WIFI_STATE_ENABLED) {
            Log.d(TAG, "popUpWlanToMobileDialog, ap is connecting then return");
            return;
        }
        if ((wlan2MobileDialog != null) && wlan2MobileDialog.isShowing()) {
            Log.d(TAG, "popUpWlanToMobileDialog is already showing");
            return;
        }

        mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        b.setCancelable(false);
        b.setTitle(R.string.network_disconnect_title);
        b.setMessage(R.string.network_disconnect_message);
        b.setPositiveButton(R.string.mobile_data_connect_enable, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mConnectivityManager.setMobileDataEnabled(true);
                mPromptIsShowing = false;
            }
        });

        b.setNegativeButton(R.string.mobile_data_connect_disable, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                mConnectivityManager.setMobileDataEnabled(false);
                mPromptIsShowing = false;
            }
        });
        wlan2MobileDialog = b.create();
        wlan2MobileDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        wlan2MobileDialog.show();
        Log.d(TAG, "wlan2MobileDialog is displayed");
        mPromptIsShowing = true;
    }

    public static void setManualConnectPolicy(Context context, int value) {
        Global.putInt(context.getContentResolver(),  Global.WIFI_MANUAL_CONNECT, value);
        applyWifiConnectionPolicy(context, false);
        resetTimer();
    }

    public static int getManualConnectPolicy(Context context) {
        int value = Global.getInt(context.getContentResolver(), Global.WIFI_MANUAL_CONNECT, MANUAL_CONNECT_DISABLED);
        return value;
    }

    public static void setMobileToWlanPolicy(Context context, int value) {
        Global.putInt(context.getContentResolver(), Global.WIFI_MOBILE_TO_WLAN_POLICY, value);
        applyWifiConnectionPolicy(context, false);
        resetTimer();
    }

    public static void setManulDisconnectFlags(boolean flag) {
        Log.d(TAG, "setManulDisconnectFlags flags = " + flag);
        manulDisconnect = flag;
    }
    public static int getMobileToWlanPolicy(Context context) {
        int value = Global.getInt(context.getContentResolver(), Global.WIFI_MOBILE_TO_WLAN_POLICY, MOBILE_TO_WLAN_AUTO);
        return value;
    }

    public static void setWpsIsRunning(boolean value) {
        isWpsRunning = value;
    }

    private static boolean isMobileDataConnected(Context context) {
        /* SPRD: Modify Bug 312556 for do not show top info when disable wifi ap and dataconnect is enable @{ */
        if (TelephonyManager.getDefault(TelephonyManager.getDefaultDataPhoneId(context))
                .getSimState()
                == TelephonyManager.SIM_STATE_READY) {
            int simState = 0;
            if (TelephonyManager.isMultiSim()) {
			    //revo lyq 20140910
                if(FeatureOption.PRJ_FEATURE_MOBILE_DATA){
                    simState = Settings.Global.getIntAtIndex(context.getContentResolver(),Settings.Global.MOBILE_DATA,
                            TelephonyManager.getDefaultDataPhoneId(context), 1);
                }else{
                    simState = Settings.Global.getIntAtIndex(context.getContentResolver(),Settings.Global.MOBILE_DATA,
                            TelephonyManager.getDefaultDataPhoneId(context), 0);
                }
            } else {
			    //revo lyq 20140910
                if(FeatureOption.PRJ_FEATURE_MOBILE_DATA){
                    simState = Settings.Global.getInt(context.getContentResolver(),
                            Settings.Global.MOBILE_DATA, 1);
                }else{
                    simState = Settings.Global.getInt(context.getContentResolver(),
                            Settings.Global.MOBILE_DATA, 0);
                }
            }
            Log.d(TAG, "simState = " + simState);
            if (simState == 1) {
                return true;
            }
        }
        /* @} */
        return false;
    }

    public static boolean isWifiConnectingOrConnected() {
        if(mWifiManager != null && mWifiManager.getConnectionInfo() != null) {
            SupplicantState state = mWifiManager.getConnectionInfo().getSupplicantState();
            if(state == SupplicantState.AUTHENTICATING) return true;
            if(state == SupplicantState.ASSOCIATING) return true;
            if(state == SupplicantState.ASSOCIATED) return true;
            if(state == SupplicantState.FOUR_WAY_HANDSHAKE) return true;
            if(state == SupplicantState.GROUP_HANDSHAKE) return true;
            if(state == SupplicantState.COMPLETED) return true;
        }
        return false;
    }

    private static void enableAutoConnect() {
        if (mPromptIsShowing == false && mDialogIsShowing == false/* && manulDisconnect == false*/) {
            Log.d(TAG, "enableAutoConnect() mWifiManager.reconnect()");
            mWifiManager.reconnect();
        }
    }

    private static void disableAutoConnect(boolean forceDisconnect) {
        Log.i(TAG, "disableAutoConnect");
        if(!forceDisconnect && isWifiConnectingOrConnected()) {
            Log.i(TAG, "do not disconnect when connection is under processing or etablished");
        } else {
            mWifiManager.disconnect();
        }
    }

    private static void setTimer(long value) {
        Log.i(TAG, "setTimer:" + value);
        mTimer = value;
    }

    private static void resetTimer() {
        Log.i(TAG, "resetTimer");
        mTimer = -1;
    }

    private static boolean isTimerRunning() {
        boolean isTimerRunning;
        if(mTimer < 0) {
            isTimerRunning = false;
        } else {
            isTimerRunning = System.currentTimeMillis() - mTimer < DIALOG_INTERVAL_MS;
        }
        Log.i(TAG, "isTimerRunning: " + isTimerRunning);
        return isTimerRunning;
    }

    private static void applyWifiConnectionPolicy(Context context, boolean forceDisconnect) {
        boolean isMobileDataConnected = isMobileDataConnected(context);
        int manualConnect = getManualConnectPolicy(context);
        int mobileToWlan = getMobileToWlanPolicy(context);
        Log.i(TAG, "applyWifiConnectionPolicy() isMobileDataConnected: "
                + isMobileDataConnected + " manualConnect: " + manualConnect + " mobileToWlan: " + mobileToWlan);

        if(manualConnect == MANUAL_CONNECT_DISABLED) {
            if(isMobileDataConnected) {
                if(mobileToWlan == MOBILE_TO_WLAN_AUTO) {
                    enableAutoConnect();
                } else if(mobileToWlan == MOBILE_TO_WLAN_MANUAL) {
                    disableAutoConnect(forceDisconnect);
                } else if(mobileToWlan == MOBILE_TO_WLAN_ASK) {
                    disableAutoConnect(forceDisconnect);
                }
            } else {
                enableAutoConnect();
            }
        } else if(manualConnect == MANUAL_CONNECT_ENABLED) {
            //non-auto connect
            disableAutoConnect(forceDisconnect);
        }
    }

    private static void applyWifiDisConnectPolicy(Context context, boolean forceDisconnect) {
        boolean isMobileDataConnected = isMobileDataConnected(context);
        int manualConnect = getManualConnectPolicy(context);
        int mobileToWlan = getMobileToWlanPolicy(context);
        Log.i(TAG, "applyWifiDisConnectPolicy() isMobileDataConnected: "
                + isMobileDataConnected + " manualConnect: " + manualConnect + " mobileToWlan: " + mobileToWlan);

        if(manualConnect == MANUAL_CONNECT_DISABLED) {
            if(isMobileDataConnected) {
                if(mobileToWlan == MOBILE_TO_WLAN_AUTO) {
                    enableAutoConnect();
                } else if(mobileToWlan == MOBILE_TO_WLAN_MANUAL) {
                    disableAutoConnect(forceDisconnect);
                } else if(mobileToWlan == MOBILE_TO_WLAN_ASK) {
                    disableAutoConnect(forceDisconnect);
                }
            } else {
                enableAutoConnect();
            }
        } else if(manualConnect == MANUAL_CONNECT_ENABLED) {
            //non-auto connect
            disableAutoConnect(forceDisconnect);
        }
    }

    private static void showMobileToWlanDialog(Context context) {
        boolean isMobileDataConnected = isMobileDataConnected(context);
        int manualConnect = getManualConnectPolicy(context);
        int mobileToWlan = getMobileToWlanPolicy(context);
        int wifiState = mWifiManager.getWifiState();

        if(isWifiConnectingOrConnected() || wifiState != WifiManager.WIFI_STATE_ENABLED || mPromptIsShowing == true) {
            Log.d(TAG, "showMobileToWlanDialog() returned because of connecting or connected");
            return; // do not show dialog when connection is under processing or etablished
        }

        if(manualConnect == MANUAL_CONNECT_DISABLED) {
            if(isMobileDataConnected) {
                if (mobileToWlan == MOBILE_TO_WLAN_MANUAL) {
                    // For cmcc case 4.8, manual connect
                    // Only pop up Recommend ap dialog when passive disconnect
                    disableAutoConnect(true);
                    if(!isTimerRunning() && !mDialogIsShowing && !mPromptIsShowing /*&& manulDisconnect == false*/)
                        showMatchedAccessPoints(context, null);
                } else if(mobileToWlan == MOBILE_TO_WLAN_ASK) {
                    // For cmcc case 4.9, Always ask
                    // Only pop up dialog when passive disconnect
                    disableAutoConnect(true);
                    if(!isTimerRunning() && !mDialogIsShowing && !mPromptIsShowing /*&& manulDisconnect == false*/)
                        showRecommendAccessPoint(context);
                } else  if(mobileToWlan == MOBILE_TO_WLAN_AUTO) {
                    // For cmcc case 4.7 auto connect trust aps, it's has relationship with auto connect functions
                    enableAutoConnect();
                    // do nothing
                }
            } else {
                // auto connect do nothing
                enableAutoConnect();
            }
        } else if(manualConnect == MANUAL_CONNECT_ENABLED) {
            // For cmcc case 4.6, other SSIDs promotion when disconnect with one AP,
            // now instead show when disconnect with ap
            disableAutoConnect(true);
            // if(!isTimerRunning() && !mDialogIsShowing && !mPromptIsShowing && manulDisconnect == false)
            // showMatchedAccessPoints(context, null);
        }
    }

    private static void showRecommendAccessPoint(Context context) {
        int wifiState = mWifiManager.getWifiState();
        if(isWifiConnectingOrConnected() || wifiState != WifiManager.WIFI_STATE_ENABLED) {
            Log.i(TAG, "showRecommendAccessPoint in connecting state, then return");
            return;
        }
        final AccessPoint mRecommendAccessPoint = getRecommendAccessPoint(context);
        if(mRecommendAccessPoint != null) {
            DialogInterface.OnClickListener mRecommendApListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mWifiManager.connect( mRecommendAccessPoint.networkId, null);
                    mDialogIsShowing = false;
                }
            };
            DialogInterface.OnClickListener mCancelListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setTimer(System.currentTimeMillis());
                    mDialogIsShowing = false;
                }
            };
            AlertDialog mRecommendApDialog = new AlertDialog.Builder(context)
                .setTitle(mResources.getText(R.string.mobile_to_wlan_popup_title))
                .setMessage(mResources.getText(R.string.mobile_to_wlan_popup_prefix) + mRecommendAccessPoint.ssid + "," + mResources.getText(R.string.mobile_to_wlan_popup_suffix))
                .setPositiveButton(mResources.getText(R.string.gps_to_wifi_yes), mRecommendApListener)
                .setNegativeButton(mResources.getText(R.string.gps_to_wifi_no), mCancelListener)
                .setCancelable(false)
                .create();
            mRecommendApDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
            mRecommendApDialog.show();
            mDialogIsShowing = true;
            Log.d(TAG, "mobile2wlan recommend dialog is displayed.");
        }
    }

    private static boolean showMatchedAccessPoints(Context context, WifiInfo mWifiInfo) {
        int wifiState = mWifiManager.getWifiState();

        if(isWifiConnectingOrConnected() || wifiState != WifiManager.WIFI_STATE_ENABLED) {
            Log.i(TAG, "showMatchedAccessPoints in connecting state, then return");
            return false;
        }
        Collection<AccessPoint> accessPoints = getMatchedAccessPoints(context);
        boolean dialogDisplayed = false;
        if (mWifiInfo != null && accessPoints.size() > 0) {
            for (AccessPoint ap : accessPoints) {
                if (ap.networkId == mWifiInfo.getNetworkId() &&
                        (ap.ssid != null && ap.ssid.equals(AccessPoint.removeDoubleQuotes(mWifiInfo.getSSID())))) {
                    accessPoints.remove(ap);
                    break;
                }
            }
        }
        if(accessPoints.size() > 0) {
            String[] mApSSIDs = new String[accessPoints.size()];
            final int mApIDs[] = new int[accessPoints.size()];
            int i=0;
            for(AccessPoint accessPoint : accessPoints) {
                mApSSIDs[i] = accessPoint.ssid;
                mApIDs[i++] = accessPoint.networkId;
            }

            DialogInterface.OnClickListener mMatchedApsListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mWifiManager.connect( mApIDs[which], null);
                    mDialogIsShowing = false;
                }
            };
            DialogInterface.OnClickListener mCancelListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setTimer(System.currentTimeMillis());
                    mDialogIsShowing = false;
                }
            };
            AlertDialog mMatchedApsDialog= new AlertDialog.Builder(context)
                .setTitle(mResources.getText(R.string.select_trusted_ap_access))
                .setItems(mApSSIDs, mMatchedApsListener)
                .setNegativeButton(mResources.getText(R.string.gps_to_wifi_no), mCancelListener)
                .setCancelable(false)
                .create();
            mMatchedApsDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
            mMatchedApsDialog.show();
            mDialogIsShowing = true;
            dialogDisplayed = true;
        }
        return dialogDisplayed;
    }

    private static AccessPoint getRecommendAccessPoint(Context context) {
        Log.i(TAG, "getRecommendAccessPoint");
        final Collection<AccessPoint> accessPoints = getMatchedAccessPoints(context);
        AccessPoint recommendAccessPoint = null;
        int priority = -1;
        for(AccessPoint accessPoint : accessPoints) {
            if(priority < accessPoint.getConfig().priority) {
                priority = accessPoint.getConfig().priority;
                recommendAccessPoint = accessPoint;
            }
        }
        if(recommendAccessPoint != null) Log.i(TAG, "getRecommendAccessPoint " + recommendAccessPoint.ssid);
        return recommendAccessPoint;
    }

    private static List<AccessPoint> getMatchedAccessPoints(Context context) {
        ArrayList<AccessPoint> configuredAPs = new ArrayList<AccessPoint>();
        ArrayList<AccessPoint> scannedAPs = new ArrayList<AccessPoint>();
        ArrayList<AccessPoint> matchedAPs = new ArrayList<AccessPoint>();

        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                AccessPoint accessPoint = new AccessPoint(context, config);
                configuredAPs.add(accessPoint);
            }
        }
        final List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                AccessPoint accessPoint = new AccessPoint(context, result);
                scannedAPs.add(accessPoint);
            }
        }
        for (AccessPoint configuredAP : configuredAPs) {
            for (AccessPoint scannedAP : scannedAPs) {
                if (configuredAP.ssid != null && scannedAP.ssid != null) {
                    if(configuredAP.ssid.equals(scannedAP.ssid) && configuredAP.security == scannedAP.security) {
                        matchedAPs.add(configuredAP);
                        break;
                    }
                }
            }
        }
        Log.i(TAG, "getMatchedAccessPoints matchedAPs.size = " + matchedAPs.size());
        return matchedAPs;
    }
}
