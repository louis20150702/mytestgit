
package com.sprd.settings.networkrate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;

//network spread rate
public class DataNetworkRate {

	private static final String TAG = "DataNetworkRate";
    // 未知网络类型
    private final static int NETWORK_U = -1;
    // W是4g网络
    private final static int NETWORK_W = 0;
    // E是2.75代网络
    private final static int NETWORK_E = 1;
    // G是2.5代网络
    private final static int NETWORK_G = 2;

    private final static String DEFAULT_RATE = "0 bps";
    private final Context mContext;
    // 理论峰值
    public class NetworkTrafficTheoryPeak {
        public String upLinkTheoryPeak;
        public String downLinkTheoryPeak;
    }

    // 当前的网络状态值
    public class NetworkTrafficCurrentRate {
        public long upLinkRate;
        public long downLinkRate;
    }

    public DataNetworkRate(Context context) {
        this.mContext = context;
    }

    private NetworkInfo getNetworkInfo(Context context) {
        NetworkInfo networkInfo = null;
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            networkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return networkInfo;
    }

    // 判断当前网络是否可用
    private boolean isNetworkConnected(NetworkInfo info) {

        return info != null && info.isAvailable();
    }

    // 判断当前手机网络是否可用
    private boolean isMobileConnected(NetworkInfo info) {
    	if(info != null){
    		Log.d(TAG,"info.getType() = "+info.getType());
    	}
        return info != null && info.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    // 判断当前wifi网络是否可用
    private boolean isWifiConnected(NetworkInfo info) {
        return info != null && info.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public NetworkTrafficTheoryPeak getCurrentNetworkTrafficTheoryPeak(Context context,
            boolean isMobile) {
        NetworkTrafficTheoryPeak networkTrafficTheoryPeak = null;

        // 获取网络的状态信息
        NetworkInfo networkInfo = getNetworkInfo(context);
        Log.d(TAG,"networkInfo = "+networkInfo);
        Log.d(TAG,"isMobile = "+isMobile);
        if (context != null && isNetworkConnected(networkInfo)) {
            // mobile connected
            if (isMobile) {
                if (isMobileConnected(networkInfo)) {

                    // 获得手机网络峰值，根据类型
                    networkTrafficTheoryPeak = getMobileTrafficTheoryPeak(getMobileNetworkType(networkInfo
                            .getSubtype()));
                }
            }
            // wifi
            else {
                if (isWifiConnected(networkInfo)) {
                    // 获得wifi的速度长值
                    networkTrafficTheoryPeak = getWifiTrafficTheoryPeak();
                }
            }
        }
        Log.d(TAG,"networkTrafficTheoryPeak = "+networkTrafficTheoryPeak);
        // 网络没有链接，返回空对象
        return networkTrafficTheoryPeak;
    }

    // mobile(2g,3g,4g) , or wifi , or ethernet
    public NetworkTrafficCurrentRate getCurrentNetworkTrafficRate(Context context, boolean isMobile) {
        NetworkTrafficCurrentRate networkTrafficCurrentRate = null;
        // 获取网络的状态信息
        NetworkInfo networkInfo = getNetworkInfo(context);
        if (context != null && isNetworkConnected(networkInfo)) {
            // mobile connected
            if (isMobile) {
                if (isMobileConnected(networkInfo)) {
                    networkTrafficCurrentRate = getMobileTrafficCurrentRate();
                }
            }
            // wifi
            else {
                if (isWifiConnected(networkInfo)) {
                    networkTrafficCurrentRate = getWifiTrafficCurrentRate();
                }
            }
        }
        // 网络没有链接，返回空对象
        return networkTrafficCurrentRate;
    }

    //获得当前网络的流量统计
    public NetworkTrafficCurrentRate getCurrentNetworkTrafficTotal(boolean isMobile) {
        if (isMobile) {
            return getMobileTrafficCurrentRate();
        } else {
            return getWifiTrafficCurrentRate();
        }
    }

    // 获得手机接收到的总数
    private long getMobileTrafficRxBytes() {
        return TrafficStats.getMobileRxBytes();
    }

    // 获得手机发送的总数
    private long getMobileTrafficTxBytes() {
        return TrafficStats.getMobileTxBytes();
    }

    // 获得wifi所有接收到的数据
    private long getWifiTrafficRxBytes() {
        return Math.max(0, TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes());
    }

    // 获得wifi发送的总数
    private long getWifiTrafficTxBytes() {
        return Math.max(0, TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes());
    }

    // 获得手机的当前流量
    private NetworkTrafficCurrentRate getMobileTrafficCurrentRate() {
        NetworkTrafficCurrentRate currentRate = new NetworkTrafficCurrentRate();
        currentRate.upLinkRate = getMobileTrafficTxBytes();
        currentRate.downLinkRate = getMobileTrafficRxBytes();
        return currentRate;
    }

    // 获得wifi的流量信息
    private NetworkTrafficCurrentRate getWifiTrafficCurrentRate() {
        NetworkTrafficCurrentRate currentRate = new NetworkTrafficCurrentRate();
        currentRate.upLinkRate = getWifiTrafficTxBytes();
        currentRate.downLinkRate = getWifiTrafficRxBytes();
        return currentRate;
    }

    private NetworkTrafficTheoryPeak getWifiTrafficTheoryPeak() {
        // 返回一个常量，需要LC-WIFI协助
        NetworkTrafficTheoryPeak networkTrafficTheoryPeak = new NetworkTrafficTheoryPeak();
        networkTrafficTheoryPeak.upLinkTheoryPeak = (mContext != null ? mContext
                .getString(R.string.network_wifi_peak_uplink) : DEFAULT_RATE);
        networkTrafficTheoryPeak.downLinkTheoryPeak = (mContext != null ? mContext
                .getString(R.string.network_wifi_peak_downlink) : DEFAULT_RATE);
        return networkTrafficTheoryPeak;
    }

    // 不存在的峰值返回空指针
    private NetworkTrafficTheoryPeak getMobileTrafficTheoryPeak(int network_type) {
        NetworkTrafficTheoryPeak networkTrafficTheoryPeak = null;
        Log.d(TAG,"network_type= "+network_type);
        switch (network_type) {
            case NETWORK_G: {
                networkTrafficTheoryPeak = new NetworkTrafficTheoryPeak();
                networkTrafficTheoryPeak.upLinkTheoryPeak = (mContext != null ? mContext
                        .getString(R.string.network_mobile_peak_g_uplink) : DEFAULT_RATE);
                networkTrafficTheoryPeak.downLinkTheoryPeak = (mContext != null ? mContext
                        .getString(R.string.network_mobile_peak_g_downlink) : DEFAULT_RATE);
            }
                break;
            case NETWORK_E: {
                networkTrafficTheoryPeak = new NetworkTrafficTheoryPeak();
                networkTrafficTheoryPeak.upLinkTheoryPeak = (mContext != null ? mContext
                        .getString(R.string.network_mobile_peak_e_uplink) : DEFAULT_RATE);
                networkTrafficTheoryPeak.downLinkTheoryPeak = (mContext != null ? mContext
                        .getString(R.string.network_mobile_peak_e_downlink) : DEFAULT_RATE);
            }
                break;
            case NETWORK_W: {
                networkTrafficTheoryPeak = new NetworkTrafficTheoryPeak();
                networkTrafficTheoryPeak.upLinkTheoryPeak = (mContext != null ? mContext
                        .getString(R.string.network_mobile_peak_w_uplink) : DEFAULT_RATE);
                networkTrafficTheoryPeak.downLinkTheoryPeak = (mContext != null ? mContext
                        .getString(R.string.network_mobile_peak_w_downlink) : DEFAULT_RATE);
            }
                break;
        }

        return networkTrafficTheoryPeak;
    }

    // 获得当前手机的网络类型
    // 用来显示区分最大的上行率和下行率，3g，gprs，4g
    // 网络好用的时候才能调用该接口
    private int getMobileNetworkType(int mobileSubType) {
        Log.d(TAG,"mobileSubType = "+mobileSubType);
        int mobileConnectionType = NETWORK_U;
        int type = mobileSubType;
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS: {
                mobileConnectionType = NETWORK_G;
            }
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE: {
                mobileConnectionType = NETWORK_E;
            }
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_UMTS:    
            case TelephonyManager.NETWORK_TYPE_HSUPA: {
                mobileConnectionType = NETWORK_W;
            }
                break;
        }
        // case TelephonyManager.NETWORK_TYPE_CDMA:
        // case TelephonyManager.NETWORK_TYPE_UMTS:
        // case TelephonyManager.NETWORK_TYPE_EVDO_0:
        // case TelephonyManager.NETWORK_TYPE_EVDO_A:
        // case TelephonyManager.NETWORK_TYPE_EVDO_B:
        // case TelephonyManager.NETWORK_TYPE_HSPA:
        // case TelephonyManager.NETWORK_TYPE_IDEN:
        // case TelephonyManager.NETWORK_TYPE_LTE:
        // case TelephonyManager.NETWORK_TYPE_HSPAP:
        return mobileConnectionType;
    }
}
