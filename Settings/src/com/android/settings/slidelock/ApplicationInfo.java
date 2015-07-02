package com.android.settings.slidelock;

import android.graphics.drawable.Drawable;

public class ApplicationInfo {
    
   private  String name;          //应用程序名
   private  Drawable icon;        //应用图标
   private String packName;       //包名
   private String activityName;   //主Activity名
   private String stName; //对应设置项名称
   
   public String getActivityName() {
	return activityName;
}

public void setActivityName(String activityName) {
	this.activityName = activityName;
}

	public String getPackName() {
		return packName;
	}

	public void setPackName(String packName) {
		this.packName = packName;
	}

	public String getName () {
        return name;
    }
    
    public void setName (String name) {
        this.name = name;
    }
    
    
    public Drawable getIcon () {
        return icon;
    }
    
    public void setIcon (Drawable icon) {
        this.icon = icon;
    }

    public String getStName() {
        return stName;
    }

    public void setStName(String stName) {
        this.stName = stName;
    }
    
}
