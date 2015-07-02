package com.android.settings.fuelgauge;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import android.provider.Settings;
import com.android.settings.R;

public class PowerSavingReceiver extends BroadcastReceiver {

    private static final String TAG = "PowerSaving";

    public static final String SPRD_ACTION_BATTERY_CHANGED = "com.sprd.intent.action.BATTERY_CHANGED";

    public static final int NOTIFICATION_ID = R.drawable.ic_settings_applications;
    private int mDefaultRestrictLevel;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        Log.d(TAG, "received action:" + action.toString());
        
        PowerSavingSecure secure = new PowerSavingSecure(context);
        if (SPRD_ACTION_BATTERY_CHANGED.equals(action)) {
		if (!secure.isPowerSavingOn()) {
                    dismissNotification(context);	
		      secure.setRestrictStatus(false);				
		      Log.i(TAG,"is power off!");
                    return;
                }	
		
              PowerSavingStatusController statusController = new PowerSavingStatusController(context);
	       final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
	   	if (!secure.isRestricted()) {        
			if(getRestrictLevel(context) < level){
	                return ;
			}
	       Log.i(TAG,"is power on do!");
		  secure.setRestrictStatus(true);		
                showNotification(context);
                if (secure.isWifiChecked()) {
                    statusController.updateWifiStatus();
                }
                if (secure.isBluetoothChecked()) {
                    statusController.updateBluetoothStatus();
                }
                if (secure.isGpsChecked()) {
                    statusController.updateGpsStatus();
                }
                if (secure.isBrightnessChecked()) {
                    statusController.updateBrightnessStatus();
                }
                if (secure.isSyncChecked()) {
                    statusController.updateSyncStatus();
                }
                if (secure.isDataConnectionChecked()){
                	statusController.updateDataConnectionStatus();
                }
            } else {
                dismissNotification(context);
	         Log.i(TAG,"is power on nodo!");					
		  secure.setRestrictStatus(false);				
            }
        }else if (Intent.ACTION_BOOT_COMPLETED.equals(action)){
            secure.setRestrictStatus(false);
        }
    }

    private void dismissNotification(Context context) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    private void showNotification(Context context) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_settings_applications);
        builder.setContentTitle(context.getString(R.string.power_saving));
        builder.setContentText(context.getString(R.string.power_saving_on));
        builder.setTicker(context.getString(R.string.power_saving_on));
	 builder.setWhen(0L);
	//builder.setWhen(System.currentTimeMillis());
       // builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setDefaults(0);
        builder.setOngoing(true);
        
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(PowerSavingSettings.class);
        
        Intent intent = new Intent(context,PowerSavingSettings.class);

        stackBuilder.addNextIntent(intent);
        
       // PendingIntent pendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, builder.build());
    }
	
    private int getRestrictLevel(Context context) {
        String level = getSecureString(context,Settings.Secure.POWERSAVING_AUTO_RESTRICT_LEVEL);
	mDefaultRestrictLevel = context.getResources().getInteger(com.android.internal.R.integer.powersaving_default_restrict_level);	
        if (level == null) {
            return mDefaultRestrictLevel;
        }
        return Integer.parseInt(level);
    }
	
    private String getSecureString(Context context , String key) {
        return Settings.Secure.getString(context.getContentResolver(), key);
    }


}
