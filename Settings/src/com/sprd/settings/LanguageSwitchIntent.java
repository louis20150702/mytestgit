/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sprd.settings;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.Fragment;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.res.Configuration;
import java.text.Collator;
import java.util.Locale;
import android.app.backup.BackupManager;
import android.os.RemoteException;
import android.net.Uri;
import android.widget.Toast;
import android.util.Log;
import com.android.internal.app.LocalePicker;
import com.android.settings.SettingsPreferenceFragment.SettingsDialogFragment;
import com.android.settings.DevelopmentSettings;
// revo lgz
import com.android.featureoption.FeatureOption;
import java.util.Locale;

public class LanguageSwitchIntent extends BroadcastReceiver {  
    public static String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
	    private static final String TAG="LanguageSwitchIntent";
    private static final int DLG_SHOW_GLOBAL_WARNING = 1;
    private SettingsDialogFragment mDialogFragment;

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
         Log.d(TAG," intentAction " +intentAction); 
		try {
	        if (intentAction.equals(SECRET_CODE_ACTION)) {
	            IActivityManager am = ActivityManagerNative.getDefault();
	            Configuration config = am.getConfiguration();
				Uri uri = intent.getData();
                String host = uri.getHost();
				if("231".equals(host)){// 阿姆哈拉语
	            	config.locale = new Locale("am", "ET");
				}				
				else if("0966".equals(host)){// 阿拉伯语
	            	config.locale = new Locale("ar", "IL");
				}
				else if("8181".equals(host)){// 阿拉伯语
	            	config.locale = new Locale("ar", "EG");
				}					
				else if("2031".equals(host)){ // 捷克语
	            	config.locale = new Locale("cs", "CZ");
				}				
				else if("208".equals(host)){ // 丹麦语
	            	config.locale = new Locale("da", "DK");
				}
				else if("756".equals(host)){ // 德语-瑞士
	            	config.locale = new Locale("de", "CH");
				}
				else if("438".equals(host)){ // 德语-列支敦士登
	            	config.locale = new Locale("de", "LI");
				}
				else if("0049".equals(host)){ // 德语
	            	config.locale = new Locale("de", "DE");
				}
				else if("40".equals(host)){ // 德语，奥地利
	            	config.locale = new Locale("de", "AT");
				}						
				else if("300".equals(host)){ // 希腊语
	            	config.locale = new Locale("el", "GR");
				}				
				else if("826".equals(host)){
	            	config.locale = new Locale("en", "GB");//英语
				}	
				else if("0044".equals(host)){
	            	config.locale = new Locale("en", "US");//英语
				}
				else if("0034".equals(host)){// 西班牙语
	            	config.locale = new Locale("es", "ES");
				}					
				else if("8401".equals(host)){// 西班牙语
	            	config.locale = new Locale("es", "US");
				}				
				else if("0098".equals(host)){// 波斯语
	            	config.locale = new Locale("fa", "IR");
				}							
				else if("0033".equals(host)){// 法语
	            	config.locale = new Locale("fr", "FR");
				}			
				else if("0091".equals(host)){// 印地语
	            	config.locale = new Locale("hi", "IN");
				}	
				else if("191".equals(host)){//克罗地亚语
	            	config.locale = new Locale("hr", "HR");
				}						
				else if("0062".equals(host)){//印度尼西亚语
	            	config.locale = new Locale("in", "ID");
				}
				else if("0039".equals(host)){//意大利语
	            	config.locale = new Locale("it", "IT");
				}
				else if("7562".equals(host)){//瑞士-意大利语
	            	config.locale = new Locale("it", "CH");
				}	
        else if("0082".equals(host)){//韩语
	            	config.locale = new Locale("ko", "KR");
				}
				else if("428".equals(host)){//拉脱维亚语
	            	config.locale = new Locale("lv", "LV");
				}					
				else if("458".equals(host)){// 马来西亚语
	            	config.locale = new Locale("ms", "MY");
				}
				else if("0048".equals(host)){// 荷兰语
	            	config.locale = new Locale("nl", "NL");
				}
				else if("561".equals(host)){// 比利时-荷兰语
	            	config.locale = new Locale("nl", "BE");
				}
				else if("616".equals(host)){// 波兰语
	            	config.locale = new Locale("pl", "PL");
				}				
				else if("03511".equals(host)){ // 葡萄牙语
	            	config.locale = new Locale("pt", "BR");
				}		
				else if("0351".equals(host)){//葡萄牙语
	            	config.locale = new Locale("pt", "PT");
				}
				else if("642".equals(host)){ //罗马尼亚语
	            	config.locale = new Locale("ro", "RO");
				}				
				else if("0007".equals(host)){// 俄语
	            	config.locale = new Locale("ru", "RU");
				}		
				else if("705".equals(host)){// 斯洛文尼亚语
	            	config.locale = new Locale("sl", "SI");
				}
				else if("834".equals(host)){// 斯瓦西里语
	            	config.locale = new Locale("sw", "TZ");
				}				
				else if("0066".equals(host)){// 泰语
	            	config.locale = new Locale("th", "TH");
				}		
				else if("0090".equals(host)){// 土耳其语
	            	config.locale = new Locale("tr", "TR");
				}				
				else if("804".equals(host)){//乌克兰语
	            	config.locale = new Locale("uk", "UA");
				}				
				else if("0084".equals(host)){// 越南语
	            	config.locale = new Locale("vi", "VN");
				}				
				else if("0086".equals(host)){//中文-简体
	            	config.locale = new Locale("zh", "CN");
				}	
				else if("0886".equals(host)){//中文-繁体
	            	config.locale = new Locale("zh", "TW");
				}
				else if("159".equals(host)){//中文-藏文
	            	config.locale = new Locale("bo", "CN");
				}					
				else if("9133".equals(host)){//孟加拉语
			config.locale = new Locale("bn", "BD");
				}
				else if("0855".equals(host)){//柬埔寨语
			config.locale = new Locale("km", "KH");
				}		
				else if("0095".equals(host)){//缅甸语
			config.locale = new Locale("my", "MM");
				}			
				else if("0092".equals(host)){//乌尔都语
			config.locale = new Locale("ur", "PK");
				}						
				else if("8042".equals(host)){//乌尔都语
			config.locale = new Locale("ur", "IN");
				}
				else if("398".equals(host)){//哈萨克斯坦
			config.locale = new Locale("kk", "KZ");
				}				
				else if("268".equals(host)){//格鲁吉亚语
			config.locale = new Locale("ka", "CE");
				}	
				else if("0856".equals(host)){//老挝
			config.locale = new Locale("lo", "LA");
				}
				else if("3564".equals(host)){//印度-旁遮普语
			config.locale = new Locale("pa", "IN");
				}		
				else if("586".equals(host)){//印度-旁遮普语
			config.locale = new Locale("pa", "PK");
				}		

				Toast.makeText(context, "language is switched", Toast.LENGTH_SHORT).show();
		              Log.d(TAG," config.locale  " +config.locale );

	            // indicate this isn't some passing default - the user wants this remembered
	         //   config.userSetLocale = true;
				
		     config.setLocale(config.locale);
                   LocalePicker.updateLocale(config.locale);

	           // am.updateConfiguration(config);
	            // Trigger the dirty bit for the Settings Provider.
	         //   BackupManager.dataChanged("com.android.providers.settings");
	        }
        } catch (RemoteException e) {
        		  Log.d(TAG," e " +e );
            // Intentionally left blank
        }
    }
}

