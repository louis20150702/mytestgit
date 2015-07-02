package com.android.settings;

import com.android.settings.accounts.AuthenticatorHelper;
import android.app.admin.DevicePolicyManager;

public class HWAllSettingsFragment extends BasePreferenceFragment
{
	public HWAllSettingsFragment(){
		super();
	}
	
	public HWAllSettingsFragment(AuthenticatorHelper aHelper, DevicePolicyManager dpm){
		super(aHelper, dpm);
	}
	
  protected int getHeadersResourceId()
  {
    return R.xml.settings_headers_all;
  }
}

/* Location:           classes_dex2jar.jar
 * Qualified Name:     com.android.settings.HWAllSettingsFragment
 * JD-Core Version:    0.6.2
 */