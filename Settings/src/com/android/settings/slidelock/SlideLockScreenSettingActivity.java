package com.android.settings.slidelock;

import android.os.Bundle;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap.Config;

import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import com.android.settings.R;

import com.android.featureoption.FeatureOption;


public class SlideLockScreenSettingActivity extends Activity implements OnClickListener{
	protected static final String TAG = "SecuritySelectorView";
	
	private final int TOP_VIEW_ID = 1;
	private final int LEFT_VIEW_ID = 2;
	private final int RIGHT_VIEW_ID = 3;
	private final int BOTTON_VIEW_ID = 4;
	private static final String LOCK_SCREEN_ITEM_SHARED_PREFERENCES_KEY = "lockscreenitemprefs";
	private static final String SETTING_LOCK_ITEM_PREFERENCES = "settinglockitemprefs";
	private static final String LEFT_ITEM = "left";
	private static final String TOP_ITEM = "top";
	private static final String RIGHT_ITEM = "right";
	private static final String BOTTON_ITEM = "botton";

	ImageView topView;
	ImageView leftView;
	ImageView rightView;
	ImageView bottonView;
	SharedPreferences itemSP;
	SharedPreferences settingPre;
	
	LockScreenItemType leftItemType;
	LockScreenItemType topItemType;
	LockScreenItemType rightItemType;
	LockScreenItemType bottonItemType;
	
	LockScreenItemType itemType;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lock_pad_setting);
		leftItemType = new LockScreenItemType();
		topItemType = new LockScreenItemType();
		rightItemType = new LockScreenItemType();
		bottonItemType = new LockScreenItemType();
		
		init();
	}

	private void init(){
		
		settingPre = getSharedPreferences(SETTING_LOCK_ITEM_PREFERENCES, 
				Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor msettingPreEditor = settingPre.edit();
		topView = (ImageView)findViewById(R.id.top_button);
		leftView = (ImageView)findViewById(R.id.left_button);
		rightView = (ImageView)findViewById(R.id.right_button);
		bottonView = (ImageView)findViewById(R.id.bottom_button);
		
		try {
			Context otherAppContext = createPackageContext("com.android.keyguard", 
					Context.CONTEXT_IGNORE_SECURITY);
			itemSP = otherAppContext.getSharedPreferences(LOCK_SCREEN_ITEM_SHARED_PREFERENCES_KEY, 
					Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);
			if(settingPre.getString(TOP_ITEM, "false").equals("false")
					&&itemSP.getString(TOP_ITEM, "false").equals("false")){
				loadDefaultItem(settingPre,otherAppContext);
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(!settingPre.getString(TOP_ITEM, "false").equals("false")){
			//setting SharedPreferences
			if(!settingPre.getString(TOP_ITEM, "false").equals("false")){
				String[] mString = split(settingPre.getString(TOP_ITEM, "false"));
				if(mString[0].equals("unlock")||mString[1].equals("unlock")){
					
				}else{
					topView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					topView.setImageDrawable(drawable);
				}
			}
	
			if(!settingPre.getString(LEFT_ITEM, "false").equals("false")){
				String[] mString = split(settingPre.getString(LEFT_ITEM, "false"));
				if(mString[0].equals("unlock")||mString[1].equals("unlock")){
					
				}else{
					leftView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					leftView.setImageDrawable(drawable);
				}
			}
	
			if(!settingPre.getString(RIGHT_ITEM, "false").equals("false")){
				String[] mString = split(settingPre.getString(RIGHT_ITEM, "false"));
				if(!(mString[0].equals("unlock")||mString[1].equals("unlock"))){
					rightView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					rightView.setImageDrawable(drawable);
				}
				
			}
			if(!settingPre.getString(BOTTON_ITEM, "false").equals("false")){
				String[] mString = split(settingPre.getString(BOTTON_ITEM, "false"));
				if(!(mString[0].equals("unlock")||mString[1].equals("unlock"))){
					bottonView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					bottonView.setImageDrawable(drawable);
				}
			}
			
		}else{
			//keyguard SharedPreferences
			if(!itemSP.getString(TOP_ITEM, "false").equals("false")){
				String[] mString = split(itemSP.getString(TOP_ITEM, "false"));
				msettingPreEditor.putString(TOP_ITEM, mString[0] + ";" + mString[1]);
				if(mString[0].equals("unlock")||mString[1].equals("unlock")){
					
				}else{
					topView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					topView.setImageDrawable(drawable);
				}
			}
	
			if(!itemSP.getString(LEFT_ITEM, "false").equals("false")){
				String[] mString = split(itemSP.getString(LEFT_ITEM, "false"));
				msettingPreEditor.putString(LEFT_ITEM, mString[0] + ";" + mString[1]);
				if(mString[0].equals("unlock")||mString[1].equals("unlock")){
					
				}else{
					leftView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					leftView.setImageDrawable(drawable);
				}
			}
	
			if(!itemSP.getString(RIGHT_ITEM, "false").equals("false")){
				String[] mString = split(itemSP.getString(RIGHT_ITEM, "false"));
				msettingPreEditor.putString(RIGHT_ITEM, mString[0] + ";" + mString[1]);
				if(!(mString[0].equals("unlock")||mString[1].equals("unlock"))){
					rightView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					rightView.setImageDrawable(drawable);
				}
				
			}
			if(!itemSP.getString(BOTTON_ITEM, "false").equals("false")){
				String[] mString = split(itemSP.getString(BOTTON_ITEM, "false"));
				msettingPreEditor.putString(BOTTON_ITEM, mString[0] + ";" + mString[1]);
				if(!(mString[0].equals("unlock")||mString[1].equals("unlock"))){
					bottonView.setOnClickListener(this);
					Drawable drawable = getPackageDrawable(mString[0], mString[1]);
					bottonView.setImageDrawable(drawable);
				}
			}
			
			msettingPreEditor.commit();
		}
		Button cancel = (Button)findViewById(R.id.button_cancel);
		cancel.setOnClickListener(this);
		
		Button done = (Button)findViewById(R.id.button_done);
		done.setOnClickListener(this);
	}

	private ArrayList<Drawable> loadDefaultItem(SharedPreferences itemSP,Context mContext){
    	ArrayList<Drawable> drawab = new ArrayList<Drawable>(4);
    	try {
    		XmlPullParser xmlPullParser = Xml.newPullParser();
			if(FeatureOption.PRJ_FEATURE_MULTI_PRJ_CUSTOMER_ADVAN_BASE){
				xmlPullParser.setInput(mContext.getAssets().open("default_lock_advan_item.xml"), "UTF-8");
			}else{
				xmlPullParser.setInput(mContext.getAssets().open("default_lock_item.xml"), "UTF-8");
			}

            int eventType=xmlPullParser.getEventType();

            
            while (eventType!=XmlPullParser.END_DOCUMENT) {
            	
            	String nodeName=xmlPullParser.getName();
            	String key;
            	String packageName;
            	String className;
            	
                switch(eventType){
                	case XmlPullParser.START_TAG:
                		if("item".equals(nodeName)){
                			itemType = new LockScreenItemType();
                		}else if("packageName".equals(nodeName)){
                			itemType.setPackageName(xmlPullParser.nextText());
                		}else if("className".equals(nodeName)){
                			itemType.setClassName(xmlPullParser.nextText());
                		}else if("key".equals(nodeName)){
                			itemType.setKey(xmlPullParser.nextText());
                		}
                		break;
            		case XmlPullParser.END_TAG:
                        if("item".equals(nodeName)){
//                        	Log.d(TAG,"packageName = " + itemType.getPackageName());
//                        	Log.d(TAG,"className = " + itemType.getClassName());
//                        	Log.d(TAG,"key = " + itemType.getKey());
                        	addToSharedPreferences(itemType.getKey(),
                        			itemType.getPackageName(),
                        			itemType.getClassName(),
                        			itemSP);

                        	drawab.add(getDrawable(itemType.getPackageName(),itemType.getClassName()));
                            itemType=null;
                        }
                        break;
                }
                eventType=xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Got exception parsing favorites.", e);
        } catch (IOException e) {
            Log.w(TAG, "Got exception parsing favorites.", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Got exception parsing favorites.", e);
        }
    	Log.d(TAG,"drawab = " + drawab);
    	return drawab;
    }
	
	private Drawable getDrawable(String pagName, String className){
    	if(pagName.equals("unlock")||className.equals("unlock")){
    		return getResources().getDrawable(R.drawable.ic_lockscreen_unlock_normal);
    	}
    	Intent intent = new Intent();
		intent.setComponent(new ComponentName(pagName, className));
		PackageManager pm = getPackageManager();
		ResolveInfo app = pm.resolveActivity(intent, 0);
		Drawable mDrawable = null;
		if(app != null){
			mDrawable = app.loadIcon(pm);
		}

		BitmapDrawable bd = (BitmapDrawable) mDrawable;
		Bitmap bm = bd.getBitmap();
		
		return toConformBitmap(bm);
    }
	
	private void addToSharedPreferences(String key,
    		String pkgName,
    		String ClassName,
    		SharedPreferences sp){
    	SharedPreferences.Editor mEditor = sp.edit();
    	mEditor.putString(key, pkgName + ";" + ClassName);
    	mEditor.commit();
    }
	
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		boolean isTouchIcon = false;
		int id = 0;
		switch(v.getId()){
			case R.id.top_button:
				id = TOP_VIEW_ID;
				isTouchIcon = true;
				break;
			case R.id.left_button:
				id = LEFT_VIEW_ID;
				isTouchIcon = true;
				break;
			case R.id.right_button:
				id = RIGHT_VIEW_ID;
				isTouchIcon = true;
				break;
			case R.id.bottom_button:
				id = BOTTON_VIEW_ID;
				isTouchIcon = true;
				break;
			case R.id.button_cancel:
				setResult(RESULT_CANCELED);
				finish();
				break;
			case R.id.button_done:
				saveItem();
				setResult(RESULT_OK);
				finish();
				break;
			default:
				break;
		}
		if(isTouchIcon){
			Intent mIntent = new Intent(this,ShowAppActivity.class);
			startActivityForResult(mIntent, id);
		}
	}
	private void setItemType(LockScreenItemType item,String key,String pagName,String className){
		item.setKey(key);
		item.setPackageName(pagName);
		item.setClassName(className);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(data == null){
			return;
		}
		String pagName = data.getStringExtra(ShowAppActivity.EXTRA_PACKAGE);
		String className = data.getStringExtra(ShowAppActivity.EXTRA_ACTIVITY);
//		Intent intent = new Intent();
//		intent.setComponent(new ComponentName(pagName, className));
//		PackageManager pm = getPackageManager();
//		ResolveInfo app = pm.resolveActivity(intent, 0);
		
		Drawable mDrawable = getPackageDrawable(pagName,className);
		switch(requestCode){
			case TOP_VIEW_ID:
				setItemType(topItemType , TOP_ITEM,pagName,className);
				topView.setImageDrawable(mDrawable);
				break;
			case LEFT_VIEW_ID:
				setItemType(leftItemType , LEFT_ITEM,pagName,className);
				leftView.setImageDrawable(mDrawable);
				break;
			case RIGHT_VIEW_ID:
				setItemType(rightItemType , RIGHT_ITEM,pagName,className);
				rightView.setImageDrawable(mDrawable);
				break;
			case BOTTON_VIEW_ID:
				setItemType(bottonItemType , BOTTON_ITEM , pagName , className);
				bottonView.setImageDrawable(mDrawable);
				break;
			default:
				break;
		}
	}
	
	private Drawable getPackageDrawable(String pagName,String className){
		if(pagName.equals("unlock")||className.equals("unlock")){
    		return getResources().getDrawable(R.drawable.ic_lockscreen_unlock_normal);
    	}
		Drawable mDrawable;
		
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(pagName, className));
		PackageManager pm = getPackageManager();
		ResolveInfo app = pm.resolveActivity(intent, 0);
		
		mDrawable = app.loadIcon(pm);
		
		BitmapDrawable bd = (BitmapDrawable) mDrawable;
		Bitmap bm = bd.getBitmap();
				
		return toConformBitmap(bm);
	}
	
	private void saveItem(){
		SharedPreferences.Editor mEditor = settingPre.edit();
		if(leftItemType.getKey()!=null && leftItemType.getKey().equals(LEFT_ITEM)){
			mEditor.remove(LEFT_ITEM);
			mEditor.putString(LEFT_ITEM, 
					leftItemType.getPackageName() + ";" + leftItemType.getClassName());
		}
		if(topItemType.getKey()!=null && topItemType.getKey().equals(TOP_ITEM)){
			mEditor.remove(TOP_ITEM);
			mEditor.putString(TOP_ITEM, 
					topItemType.getPackageName() + ";" + topItemType.getClassName());
		}
		if(rightItemType.getKey()!=null && rightItemType.getKey().equals(RIGHT_ITEM)){
			mEditor.remove(RIGHT_ITEM);
			mEditor.putString(RIGHT_ITEM, 
					rightItemType.getPackageName() + ";" + rightItemType.getClassName());
		}
		if(bottonItemType.getKey()!=null && bottonItemType.getKey().equals(BOTTON_ITEM)){
			mEditor.remove(BOTTON_ITEM);
			mEditor.putString(BOTTON_ITEM, 
					bottonItemType.getPackageName() + ";" + bottonItemType.getClassName());
		}
		mEditor.commit();
	}
	
	private String[] split(String mString){
    	return mString.split(";");
    }
	
	private Drawable toConformBitmap(Bitmap foreground) {
		Drawable d = getResources().getDrawable(R.drawable.ic_lockscreen_setting_bg);
		BitmapDrawable bd = (BitmapDrawable) d;
		Bitmap background = bd.getBitmap();
		
		int bgWidth = background.getWidth();
		int bgHeight = background.getHeight();
		int fgWidth = foreground.getWidth();
		int fgHeight = foreground.getHeight();
		float fgEndWidth ;
		float fgEndHeight;
		
		fgEndWidth = (float) (bgWidth * 0.7);
		fgEndHeight = (float) (bgHeight * 0.7);
		
		
		float scale = fgEndWidth/fgWidth;
		Matrix matrix = new Matrix();
		matrix.postScale(scale,scale);
		Bitmap resizeBmp = Bitmap.createBitmap(foreground,0,0,fgWidth,fgHeight,matrix,true);
		
		Bitmap newbmp = Bitmap.createBitmap(bgWidth, bgHeight, Config.ARGB_8888);
		Canvas cv = new Canvas(newbmp);
		cv.drawBitmap(background, 0, 0, null);
		cv.drawBitmap(resizeBmp, (bgWidth - fgEndWidth)/2, (bgHeight - fgEndHeight)/2, null);
		cv.save(Canvas.ALL_SAVE_FLAG);
		cv.restore();
		
		
		return new BitmapDrawable(newbmp);
   }
}
