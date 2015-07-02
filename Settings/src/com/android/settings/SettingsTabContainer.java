package com.android.settings;

import java.util.ArrayList;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.util.Log;


public class SettingsTabContainer extends LinearLayout implements OnClickListener{
	
	ViewGroup mContainer;
	int mCurrentSelectedIndex;
	ArrayList<TextView> mTabs;
	ArrayList<String> mTags;
	int mTabCount;
	TabClickListener mClickListener;
	
	public SettingsTabContainer(Context context, AttributeSet attr) {
		// TODO Auto-generated constructor stub
		super(context, attr);
		mContainer = (((ViewGroup)((ViewGroup)((LayoutInflater)context.getSystemService("layout_inflater"))
				.inflate(R.layout.settings_tab_container, this, true)).getChildAt(0)));

		mCurrentSelectedIndex = -1;
		mTabs = new ArrayList<TextView>();
		mTags = new ArrayList<String>();
	}
	
	public void createTabs(int tabCount, ArrayList<String> tabs, ArrayList<String> tags) {

		if(tabs == null || tabCount != tabs.size())
			throw new IllegalStateException("Tab Count does not match Tab title count");
		else if(tabCount < 1 || tabCount > 4) 
			throw new IllegalStateException("Tab count must be in the range of 1 to 4 !");
		else if(tags != null && tags.size() != tabCount)
			throw new IllegalStateException("Tag count does not match tab count");
		
		mTabCount = tabCount;
		//int i = 0;
		//if(i < tabCount) {
			//for(TextView localTextView = (TextView)mContainer.getChildAt(i);; localTextView = (TextView)mContainer.getChildAt(mTabCount-1)) {
			//	localTextView.setVisibility(View.VISIBLE);
			//	Log.i("wangxiang", "==> tabs.get(i) :" + tabs.get(i));
			//	localTextView.setText(tabs.get(i));
			//	localTextView.setOnClickListener(this);
			//	mTabs.add(localTextView);
			//	if(tags != null) {
			//		mTags.add(tags.get(i));
			//	}
				
				//i++;
				//if(i == tabCount) {
				//	break;
				//}
			//}

			for(int i = 0; i < tabCount; i++) {
				TextView localTextView = (TextView)mContainer.getChildAt(i);
				localTextView.setVisibility(View.VISIBLE);
				localTextView.setText(tabs.get(i));
				localTextView.setOnClickListener(this);
				mTabs.add(localTextView);
				if(tags != null) {
					mTags.add(tags.get(i));
				}
			}
		//}
		selectTab(0);
	}
	
	public void createTabs(int tabCount, ArrayList<String> tabs) {
		createTabs(tabCount, tabs, null);
	}
	
	public String getTag(int index) {
		if(index < 0 || index > mTabCount-1) 
			return "";
		return (String)mTags.get(index);
	}
	
	public String getCurrentTag() {
		return getTag(mCurrentSelectedIndex);
	}
	
	public void selectTab(int index) {
		if(index < 0 || index > mTabCount-1) 
			return;
		//do {
			//if(mCurrentSelectedIndex == -1) {
			//	((TextView)mTabs.get(mCurrentSelectedIndex)).setSelected(false);
				
				
			//} 
			((TextView)mTabs.get(index)).setSelected(true);
			
			mCurrentSelectedIndex = index;
			//for background
			for(int i = 0; i < mTabCount; i++) {
				if(i != index)  {
					((TextView)mTabs.get(i)).setSelected(false);
				}
			}
			
		//} while (mClickListener == null);
		if(mClickListener != null)
			mClickListener.onTabClicked(index);
	}
	
	public void selectTab(String tag) {
		selectTab(mTags.indexOf(tag));
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		TextView localTextView =  (TextView)v;
		if(mClickListener != null) 
			selectTab(mTabs.indexOf(localTextView));
	}
	
	public void setTabClickListener(TabClickListener listener) {
		mClickListener = listener;
	}
	
	public static abstract interface TabClickListener {
	    public abstract void onTabClicked(int paramInt);
	}
}
