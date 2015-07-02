
package com.sprd.settings.sim;

import com.android.internal.telephony.PhoneFactory;

import android.provider.Settings.System;
import android.sim.Sim;
import android.sim.SimManager;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.sprd.settings.sim.SimManagerActivity;
import com.android.featureoption.FeatureOption;
import android.text.TextUtils;

public class SimListAdapter extends BaseAdapter {
    

    public final class ViewHolder {

        public RelativeLayout colorImage;

        public TextView name;
		
        public TextView sim_bg_color;  //diwei add 
        
        public TextView sim_number;	//diwei add 
        
        public RadioButton viewBtn;
    }

    private LayoutInflater mInflater;
    private Sim[] mData;
    SimManager mSimManager;    //diwei add FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME
    private Context mContext;
    private OnClickListener mListener;
    private int mLayoutId;
    private int mode = -1;

    public SimListAdapter(Context context, Sim[] data, OnClickListener listener, int layoutId) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.mListener = listener;
        this.mLayoutId = layoutId;

        if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME){
 	    mSimManager = SimManager.get(context);
        }
    }


    //diwei add for FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME
    private String getCallSimNameLabel(int phoneId) {
        String sim_msg = " ";
		
        if(mSimManager != null){
            sim_msg=mSimManager.getName(phoneId);
            /*
            Sim sim = mSimManager.getSimById(phoneId);
            if(sim != null ){
            	sim_msg = sim.getName();
            } else {
                sim_msg = "sim "+phoneId ;
            }*/
        } 
        
         //Log.d("donelle ","SimListAdapter.  getCallSimNameLabel      sim_msg="+sim_msg);

        
        return sim_msg;
    }


	private String  getCallSimTextLabel(int phoneId){
        	String sim_number = " ";
		
          if(mSimManager != null){
              Sim sim = mSimManager.getSimById(phoneId);
              if(sim != null ){
					 	sim_number = sim.getNumber();
              } else {
                  sim_number = " " ;
              }
          } 
        return sim_number;
		
	}
	
   private int getCallSimBgLabel(int phoneId) {

		  int  imageViewResource[] = {com.android.internal.R.drawable.mtk_sim_light_red,
												com.android.internal.R.drawable.mtk_sim_light_tangerine,
												com.android.internal.R.drawable.mtk_sim_light_linghtblue,
												com.android.internal.R.drawable.mtk_sim_light_pink,
												com.android.internal.R.drawable.mtk_sim_light_orange,
												com.android.internal.R.drawable.mtk_sim_light_green,
												com.android.internal.R.drawable.mtk_sim_light_blue,
												com.android.internal.R.drawable.mtk_sim_light_violet};
			
        	String sim_msg = " ";
			int imgResource=0;
          if(mSimManager != null){
					 	imgResource = mSimManager.getColorIndex(phoneId);
            } else {
                  imgResource=0;
            }
						
	  //Log.d("donelle ","MsmsDialerActivity   ,getCallSimImageLabel    imgResource="+imgResource);

        return imageViewResource[imgResource];
    }




    public int getCount() {

        return mData.length;
    }

    public Object getItem(int position) {
       
        return mData[position];
    }
    public int getMode() {
        return mode;
    }
    public void setMode(int values) {
        mode = values;
    }
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Sim sim = (Sim) mData[position];       
        int val = -1;
        if (convertView == null) {

            holder = new ViewHolder();

            convertView = mInflater.inflate(mLayoutId, null);           
            holder.colorImage = (RelativeLayout) convertView.findViewById(com.android.internal.R.id.sim_color);
            holder.name = (TextView) convertView.findViewById(com.android.internal.R.id.sim_name);
            holder.viewBtn = (RadioButton) convertView.findViewById(com.android.internal.R.id.btn);          
	    //diwei add 	
           if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME){
              holder.sim_bg_color = (TextView) convertView.findViewById(com.android.internal.R.id.sim_bg_color);
              holder.sim_number = (TextView) convertView.findViewById(com.android.internal.R.id.sim_number);
           }
           
            if (holder.viewBtn != null)
                holder.viewBtn.setId(position);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (sim == null) {
            return convertView;
        }
        boolean isPhoneEnabled = System.getInt(mContext.getContentResolver(),
                TelephonyManager.getSetting(System.SIM_STANDBY, mData[position].getPhoneId()), 1) == 1;       
        if (!isPhoneEnabled) {
            holder.name.setTextColor(Color.GRAY);
            if (holder.viewBtn != null) {
                holder.viewBtn.setEnabled(false);
            }
        }else{
            holder.name.setTextColor(Color.BLACK);
            if (holder.viewBtn != null) {
                holder.viewBtn.setEnabled(true);
            }
        }

        if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME){	

            holder.colorImage.setVisibility(View.VISIBLE);
            if (sim.getPhoneId() == SimManagerActivity.DUAL_SET_ALWAYS_PROMPT
            || sim.getPhoneId() == SimManagerActivity.MMS_SET_AUTO){		 
                holder.name.setText(mData[position].getName());
                holder.sim_bg_color.setVisibility(View.INVISIBLE); 
            }else{
                holder.name.setText(getCallSimNameLabel(position));
                holder.sim_bg_color.setBackgroundResource(getCallSimBgLabel(position));
                if(!TextUtils.isEmpty(getCallSimTextLabel(position).trim())){
                    holder.sim_bg_color.setText(getCallSimTextLabel(position).trim());
                    holder.sim_number.setText(getCallSimTextLabel(position).trim());
                    holder.sim_number.setVisibility(View.VISIBLE); 
                }else{
                    holder.sim_bg_color.setText(" ");
                    holder.sim_number.setVisibility(View.GONE); 
                }
                holder.sim_bg_color.setVisibility(View.VISIBLE); 
            }
        }else{
                    if (sim.getPhoneId() == SimManagerActivity.DUAL_SET_ALWAYS_PROMPT
                            || sim.getPhoneId() == SimManagerActivity.MMS_SET_AUTO) {
                        holder.colorImage.setVisibility(View.GONE);
                    } else {
                        holder.colorImage.setVisibility(View.VISIBLE);
                        holder.colorImage.setBackgroundResource(SimManager.COLORS_IMAGES[sim.getColorIndex()]);
                    }        
                    holder.name.setText(mData[position].getName());
        }

        
        if (holder.viewBtn != null && mListener != null) {
            if (getMode() == -1) {
                val = TelephonyManager.getDefaultDataPhoneId(mContext);
            } else if (getMode() == TelephonyManager.MODE_VOICE) {
                val = TelephonyManager.getDefaultSim(mContext, TelephonyManager.MODE_VOICE);
            } else if (getMode() == TelephonyManager.MODE_VEDIO) {
                val = TelephonyManager.getDefaultSim(mContext, TelephonyManager.MODE_VEDIO);
            } else if (getMode() == TelephonyManager.MODE_MMS) {
                val = TelephonyManager.getDefaultSim(mContext, TelephonyManager.MODE_MMS);
            }
            if (mData[position].getPhoneId() == val) {
                holder.viewBtn.setChecked(true);
            } else
                holder.viewBtn.setChecked(false);
            holder.viewBtn.setOnClickListener(mListener);
        }

        
        return convertView;
    }
}
