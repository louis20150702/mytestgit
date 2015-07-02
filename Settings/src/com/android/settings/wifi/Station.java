/** Created by Spreadst */

package com.android.settings.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.settings.R;

class Station extends Preference implements OnClickListener{

    private WifiManager mWifiManager;
    private Context mContext;

    private String stationName;
    private boolean isConnected;

    public Station(Context context, String string, boolean connected) {
        super(context);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mContext = context;
        stationName = string;
        isConnected = connected;
        setWidgetLayoutResource(R.layout.preference_hotspot_button);
        setTitle(stationName);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Button mBlockButton = (Button) view.findViewById(R.id.block_button);
        mBlockButton.setOnClickListener(this);
        mBlockButton.setText(isConnected ? R.string.block : R.string.unblock);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.block_button) {
            setBlockButton();
        }
    }

    void setBlockButton() {
        if (isConnected) {
            mWifiManager.softApBlockStation(stationName);
        } else {
            mWifiManager.softApUnblockStation(stationName);
        }
        mContext.sendBroadcast(new Intent(HotspotSettings.STATIONS_STATE_CHANGED_ACTION));
    }

}
