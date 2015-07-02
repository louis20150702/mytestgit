package com.android.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import java.util.ArrayList;
import java.util.List;
import com.android.settings.accounts.AuthenticatorHelper;
import android.app.admin.DevicePolicyManager;

public class BasePreferenceFragment extends SettingsPreferenceFragment{
  private AuthenticatorHelper mAHelper;
  private DevicePolicyManager mDPM;
  protected List<PreferenceActivity.Header> mHeaders;
  private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener()
  {
    public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, int paramAnonymousInt, long paramAnonymousLong)
    {
      PreferenceActivity.Header localHeader = (PreferenceActivity.Header)BasePreferenceFragment.this.mHeaders.get(paramAnonymousInt);
      BasePreferenceFragment.this.onHeaderClick(localHeader, paramAnonymousInt);
    }
  };

	public BasePreferenceFragment(){
		super();
	}
	
	public BasePreferenceFragment(AuthenticatorHelper aHelper, DevicePolicyManager dpm){
		mAHelper = aHelper;
		mDPM = dpm;
	}
	
  public void buildAdapter()
  {
    buildHeaders();
    Log.d("dingjingliang","buildAdapter");
    if (this.mHeaders == null)
    {
      this.mHeaders = new ArrayList();
      ListAdapter localListAdapter = getListView().getAdapter();
      Log.d("dingjingliang","liscount: "+localListAdapter.getCount());
      if (localListAdapter != null)
        for (int i = 0; i < localListAdapter.getCount(); i++)
          this.mHeaders.add((PreferenceActivity.Header)localListAdapter.getItem(i));
    }
    HWSettings.HeaderAdapter localHeaderAdapter = (HWSettings.HeaderAdapter)getListView().getAdapter();
    if (localHeaderAdapter != null)
      localHeaderAdapter.pause();
    HWSettings localSettings = (HWSettings)getActivity();
    getListView().setAdapter(new HWSettings.HeaderAdapter(localSettings, this.mHeaders, mAHelper, mDPM));
  }

  public void buildHeaders()
  {
    ArrayList localArrayList = new ArrayList();
    int i = getHeadersResourceId();
	Log.d("dingjingliang", "buildHeaders: i="+i);
    if (i > 0)
    {
      HWSettings localSettings = (HWSettings)getActivity();
      if (localSettings != null)
      {
        localSettings.loadHeadersFromResource(i, localArrayList);
        localSettings.updateHeaderList(localArrayList);
        this.mHeaders = localArrayList;
      }
    }
  }

  protected int getHeadersResourceId()
  {
    return 0;
  }

  public void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent)
  {
    ListAdapter localListAdapter = getListView().getAdapter();
    if (((localListAdapter instanceof HWSettings.HeaderAdapter)) && (paramInt1 == 1))
    {
//      Boolean localBoolean = Boolean.valueOf(paramIntent.getBooleanExtra("exit_ecm_result", false));
//      AirplaneModeEnabler localAirplaneModeEnabler = ((HWSettings.HeaderAdapter)localListAdapter).getAirplaneModeEnabler();
//      if (localAirplaneModeEnabler != null)
//        localAirplaneModeEnabler.setAirplaneModeInECM(localBoolean.booleanValue(), localAirplaneModeEnabler.getSwitch().isChecked());
    }
  }

  public void onHeaderClick(PreferenceActivity.Header paramHeader, int paramInt)
  {
    /*if (paramHeader.id == 2131296882L)
    {
      paramHeader.fragment = null;
      return;
    }
    if ((paramHeader.id == 2131296883L) && (AirplaneModeEnabler.isAirplaneModeOn(getActivity())))
    {
      paramHeader.intent.setAction(null);
      return;
    }*/
    ((HWSettings)getActivity()).onHeaderClick(paramHeader, paramInt);
  }

  public void onPause()
  {
    super.onPause();
    ListAdapter localListAdapter = getListView().getAdapter();
    if ((localListAdapter instanceof HWSettings.HeaderAdapter))
      ((HWSettings.HeaderAdapter)localListAdapter).pause();
  }

  public void onResume()
  {
    super.onResume();
    ListAdapter localListAdapter = getListView().getAdapter();
    if ((localListAdapter instanceof HWSettings.HeaderAdapter))
      ((HWSettings.HeaderAdapter)localListAdapter).resume();
  }

  public void onViewCreated(View paramView, Bundle paramBundle)
  {
	  Log.d("dingjingliang", "onViewCreated");
    buildAdapter();
    getListView().setOnItemClickListener(this.mOnItemClickListener);
    super.onViewCreated(paramView, paramBundle);
  }

  public void updateHeaders()
  {
    buildHeaders();
    HWSettings.HeaderAdapter localHeaderAdapter = (HWSettings.HeaderAdapter)getListView().getAdapter();
    localHeaderAdapter.setHeaders(this.mHeaders);
	localHeaderAdapter.flushViewCache();
    localHeaderAdapter.notifyDataSetChanged();
  }
}

/* Location:           classes_dex2jar.jar
 * Qualified Name:     com.android.settings.BasePreferenceFragment
 * JD-Core Version:    0.6.2
 */