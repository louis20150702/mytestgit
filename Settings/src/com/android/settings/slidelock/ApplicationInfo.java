package com.android.settings.slidelock;

import android.graphics.drawable.Drawable;

public class ApplicationInfo {
    
   private  String name;          //Ӧ�ó�����
   private  Drawable icon;        //Ӧ��ͼ��
   private String packName;       //����
   private String activityName;   //��Activity��
   private String stName; //��Ӧ����������
   
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
