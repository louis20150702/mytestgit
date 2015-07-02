package com.android.settings.slidelock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;

public class ShowAppActivity extends Activity implements Runnable,OnItemClickListener {
	
	private static final int SEARCH_APP = 0;
	GridView gv;
	
	public  static String ARG_CURR_SETTINGNAME = "currsettingname";
	public static final String EXTRA_SETTING_NAME = "settingname";
	public static final String EXTRA_ACTIVITY = "activityname";
	public static final String EXTRA_PACKAGE = "packagename";
	public static final String EXTRA_APPNAME = "appname";
	public static String CONFIG_SPLIT = ";";
	
	private List<ApplicationInfo> apps;  //安装的应用程序信息
	private ProgressDialog pd;          	
	
	private boolean isListView = false;
	//func for current config item 
	private String currStName;
	private ComponentName currFunc;
	private Map<ComponentName, String> stMap = new HashMap<ComponentName, String>();
	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			//查询完安装的应用程序信息后，取消对话框
			if(msg.what == SEARCH_APP) {
				gv.setAdapter(new GridViewAdapter(ShowAppActivity.this,apps));
				pd.dismiss();
				setProgressBarIndeterminateVisibility(false);
			}					
		}
		
		
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.show_app_grid);
        setProgressBarIndeterminateVisibility(true);
   
        ActionBar actionBar = this.getActionBar(); 
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true); 
        actionBar.setDisplayUseLogoEnabled(false);
        
        currStName = getIntent().getStringExtra(ARG_CURR_SETTINGNAME);
        if(currStName != null){
            String func = Settings.System.getString(getContentResolver(), currStName);
            if(func != null){
                currFunc = ComponentName.unflattenFromString(func);
            }
        }
        gv = (GridView) this.findViewById(R.id.gv_apps);    
        gv.setOnItemClickListener(this);
        pd = ProgressDialog.show(this, this.getString(R.string.waiting), this.getString(R.string.rearching),true,false);
        Thread t = new Thread(this);
        t.start();
    }
    
    class GridViewAdapter extends BaseAdapter {
    	 private class GridViewHolder {
   		  TextView appName;
   		  ImageView appIcon;
   		  }
    	LayoutInflater inflater;
    	List<ApplicationInfo> appInfos;
    	GridViewHolder  viewHolder;
    	public GridViewAdapter(Context context,List<ApplicationInfo> appInfos) {
    		inflater = LayoutInflater.from(context);
    		this.appInfos = appInfos;
    	}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return appInfos.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return appInfos.get(arg0);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView!=null){
			    viewHolder = (GridViewHolder) convertView.getTag();
			}else{
				viewHolder = new GridViewHolder();
			}
			viewHolder = new GridViewHolder();
			convertView = inflater.inflate(R.layout.gv_item, null);
			viewHolder.appName = (TextView) convertView.findViewById(R.id.gv_item_appname);
			viewHolder.appIcon = (ImageView) convertView.findViewById(R.id.gv_item_icon);
			
			ApplicationInfo appInfo = appInfos.get(position);
			String appname = appInfo.getName();
			String stName = appInfo.getStName();
//			if(stName != null && !stName.equals("")){
//			    String action = BasePreferenceFragment.getGestureName(stName);
//			    appname = appname + "(" + action  + ")";
//			}
			viewHolder.appName.setText(appname);
			
			viewHolder.appIcon.setImageDrawable(appInfos.get(position).getIcon());
			
			return convertView;
		}
    	
    }

	public  void getApp() {
	    //load current settings
	    
		//查询所有应用程序的信息
		List<ApplicationInfo> allApps = new ArrayList<ApplicationInfo>();
		PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        Collections.sort(infos, new ResolveInfo.DisplayNameComparator(pm));
        if(infos != null) {
        	allApps.clear();
            for(int i=0; i<infos.size(); i++) {
                ApplicationInfo app = new ApplicationInfo();
                ResolveInfo info = infos.get(i);
                app.setName(info.loadLabel(pm).toString());
                app.setIcon(info.loadIcon(pm));
                app.setPackName(info.activityInfo.packageName);
                app.setActivityName(info.activityInfo.name);
                ComponentName cn = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                String stname = stMap.get(cn);
                if(stname != null){
                    app.setStName(stname);
                }else{
                    app.setStName("");
                }
				//revo:stvelzhang20150309
                if(!info.activityInfo.packageName.equals("com.google.android.googlequicksearchbox") && 
                    !info.activityInfo.name.equals("com.google.android.googlequicksearchbox.SearchActivity") ){
                     allApps.add(app);
                }
            }
        }
       apps = allApps;
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		final ApplicationInfo appInfo = apps.get(position);
	
		Intent intent = new Intent();
		String packageName = appInfo.getPackName();
		String activityName = appInfo.getActivityName();		
		
		ComponentName cn = new ComponentName(packageName, activityName);
		if(currFunc != null && currFunc.equals(cn)){
		    return;
		}
		intent.putExtra(EXTRA_ACTIVITY, activityName);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        String stName = appInfo.getStName();
        if(stName != null && !stName.equals("")){
            intent.putExtra(EXTRA_APPNAME, appInfo.getName());
            intent.putExtra(EXTRA_SETTING_NAME, stName);
        }
		setResult(Activity.RESULT_OK, intent);
		ShowAppActivity.this.finish();
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case android.R.id.home:
       finish();
        break;
   
      default:
        break;
      }
      return super.onOptionsItemSelected(item);
    }
	@Override
	protected void onResume() {
		super.onResume();
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		 getApp();
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 mHandler.sendEmptyMessage(SEARCH_APP);
	}
	
}