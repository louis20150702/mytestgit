package com.android.settings;

import com.android.settings.accounts.AuthenticatorHelper;
import android.app.admin.DevicePolicyManager;

public class HWGeneralSettingsFragment extends BasePreferenceFragment
{
	public HWGeneralSettingsFragment(){
		super();
	}
	
	public HWGeneralSettingsFragment(AuthenticatorHelper aHelper, DevicePolicyManager dpm){
		super(aHelper, dpm);
	}
  protected int getHeadersResourceId()
  {
    return R.xml.settings_headers_general;
  }
}

/* Location:           classes_dex2jar.jar
 * Qualified Name:     com.android.settings.HWGeneralSettingsFragment
 * JD-Core Version:    0.6.2
 */