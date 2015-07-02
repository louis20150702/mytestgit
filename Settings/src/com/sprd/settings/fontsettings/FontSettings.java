/** Create by Spreadst */
package com.sprd.settings.fontsettings;

import static com.android.settings.Utils.prepareCustomPreferencesList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.android.settings.applications.ApplicationsState;

import android.app.IActivityManager;
import android.app.ActivityManagerNative;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import com.android.settings.R;
import com.android.featureoption.FeatureOption;

public class FontSettings extends Fragment implements OnItemClickListener,
        TabHost.TabContentFactory, TabHost.OnTabChangeListener {

    private TabHost mTabHost;
    private LayoutInflater mInflater;
    static final String TAB_SYSTEM = "system";
    static final String TAB_USER = "user";
    private String mCurrentTab = null;
    private View mRootView = null;
    private ListView mListView = null;
    private View mListContainer;
    private FontAdapter mAdapter = null;
    List<FontEntry> sysFonts = new ArrayList<FontEntry>();
    List<FontEntry> usrFonts = new ArrayList<FontEntry>();
    List<FontEntry> data = new ArrayList<FontEntry>();
    public static final String SYS_FONT_PATH = "system/user_fonts";
    public static final String USR_FONT_PATH = "data/fonts";
    private static final int MENU_ADD_FONT = Menu.FIRST;
    private static final int MENU_SET_DEFAULT = Menu.FIRST + 1;
    private ProgressDialog pDialog = null;
    private String mImportPath;
    private String mImportName;
    private MenuItem setDefaultItem;
    private String deletePath;

    public void onTabChanged(String tabId) {

        if (tabId.equals(TAB_SYSTEM)) {
            mAdapter.setEntries(sysFonts);
            mCurrentTab = TAB_SYSTEM;
        } else if (tabId.equals(TAB_USER)) {
            mAdapter.setEntries(usrFonts);
            mCurrentTab = TAB_USER;
        }
        mAdapter.notifyDataSetChanged();
    }

    public View createTabContent(String tag) {
        return mRootView;
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        final String path = data.get(arg2).path;
        OnClickListener l = new OnClickListener(){

            public void onClick(DialogInterface dialog, int which) {

                try {
                    IActivityManager am = ActivityManagerNative.getDefault();
                    Configuration config = am.getConfiguration();
                    config.bUserSetTypeface = true;
                    config.sUserTypeface = path;
                    am.updateConfiguration(config);
                    updateDefaultMenu();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        if(Typeface.mUserSetTfPath != null && Typeface.mUserSetTfPath.equals(path)){
            Toast.makeText(getActivity(), R.string.font_setting_is_current, Toast.LENGTH_SHORT).show();
        }else{
            showAlertDialog(R.string.font_setting_dialog_set_font,R.string.font_setting_dialog_set_font_confirmation,l);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentTab = TAB_SYSTEM;
        if( savedInstanceState !=null && savedInstanceState.getString("tab") != null){
            mCurrentTab = savedInstanceState.getString("tab");
        }
        setHasOptionsMenu(true);
        File usr = new File(USR_FONT_PATH);
        if (!usr.exists()) {
            usr.mkdir();
            setPermission(usr.getAbsolutePath(), 755);
        }
        fillFontList(SYS_FONT_PATH, sysFonts);
        fillFontList(USR_FONT_PATH, usrFonts);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mCurrentTab);
        super.onSaveInstanceState(outState);
    }

    private void updateDefaultMenu(){
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();
            if(setDefaultItem != null){
                if (!config.bUserSetTypeface && config.sUserTypeface == null) {
                    setDefaultItem.setEnabled(false);
                } else {
                    setDefaultItem.setEnabled(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static class FontEntry {
        public String fileName;
        public String path;

        public FontEntry(String name, String p) {
            fileName = name;
            path = p;
        }
    }

    static class FontViewHolder {
        TextView fontName;
        ImageView delete;
    }

    private void fillFontList(String path, List<FontEntry> entries) {
        entries.clear();
        File[] files = new File(path).listFiles();
        if(files != null && files.length > 0){
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".ttf") || file.getName().endsWith(".TTF"))) {
                    String name = file.getName().substring(0, file.getName().lastIndexOf("."));
                    FontEntry entry = new FontEntry(name, file.getAbsolutePath());
                    entries.add(entry);
                }
            }
        }
    }

    private class FontAdapter extends BaseAdapter {

        public FontAdapter() {
            data = sysFonts;
        }

        public int getCount() {
            return data.size();
        }

        public void setEntries(List<FontEntry> da) {
            data = da;
        }

        public Object getItem(int position) {
            return data.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            FontViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.font_settings_item,
                        null);

                holder = new FontViewHolder();
                holder.fontName = (TextView) convertView.findViewById(R.id.font_name);
                holder.delete = (ImageView) convertView.findViewById(R.id.delete);
                convertView.setTag(holder);
            } else {
                holder = (FontViewHolder) convertView.getTag();
            }
            holder.fontName.setText(data.get(position).fileName);
            holder.delete.setOnClickListener(new View.OnClickListener(){

                public void onClick(View v) {
                    deletePath = data.get(position).path;
                    deleteFont();
                }
            });
            if (mCurrentTab == TAB_SYSTEM){
                holder.delete.setVisibility(View.GONE);
            } else if (mCurrentTab == TAB_USER){
                holder.delete.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        onTabChanged(mCurrentTab);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mInflater = inflater;
        mRootView = inflater.inflate(R.layout.font_settings_content, null);
        mListContainer = mRootView.findViewById(R.id.list_container);
        ListView lv = (ListView) mListContainer.findViewById(android.R.id.list);
        View emptyView = mListContainer
                .findViewById(com.android.internal.R.id.empty);
        if (emptyView != null) {
            lv.setEmptyView(emptyView);
        }
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setOnItemClickListener(this);
        lv.setTextFilterEnabled(true);
        mListView = lv;
        mAdapter = new FontAdapter();
        mListView.setAdapter(mAdapter);

        mTabHost = (TabHost) mInflater.inflate(
                R.layout.manage_apps_tab_content, container, false);
        mTabHost.setup();
        final TabHost tabHost = mTabHost;
        tabHost.addTab(tabHost
                .newTabSpec(TAB_SYSTEM)
                .setIndicator(
                        getActivity().getString(
                                R.string.font_setting_tab_sys),
                        getActivity().getResources().getDrawable(
                                R.drawable.ic_tab_download)).setContent(this));
	//lyx remove User Font menu	
	if(!FeatureOption.PRJ_FEATURE_MULTI_PRJ_TRX_VID_Z40NOVA){		
	        tabHost.addTab(tabHost
	                .newTabSpec(TAB_USER)
	                .setIndicator(
	                        getActivity().getString(R.string.font_setting_tab_usr),
	                        getActivity().getResources().getDrawable(
	                                R.drawable.ic_tab_sdcard)).setContent(this));
	}
        tabHost.setCurrentTabByTag(mCurrentTab);
        tabHost.setOnTabChangedListener(this);

        prepareCustomPreferencesList(container, mTabHost, mListView, false);

        return mTabHost;
    }

    @Override

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_ADD_FONT, 0,
                R.string.font_setting_menu_add);

        setDefaultItem = menu.add(0, MENU_SET_DEFAULT, 0,
                R.string.font_setting_menu_reset);
        updateDefaultMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_ADD_FONT) {
            // SPRD: if sd card and internal storage is not avalible,show toast
            if(!Environment.getExternalStoragePathState().equals(Environment.MEDIA_MOUNTED)
                    && !Environment.getInternalStoragePathState().equals(Environment.MEDIA_MOUNTED)){
                 Toast.makeText(getActivity(), R.string.stroage_not_mounted, Toast.LENGTH_LONG).show();
            }else{
                Intent i = new Intent(getActivity(),FontImportEditor.class);
                startActivityForResult(i , 1);
            }
            return true;
        } else if (itemId == MENU_SET_DEFAULT) {
            setDefault();
            return true;
        }else {
            return super.onOptionsItemSelected(item);
        }
    }

    public static final int MAX_NAME_LENGTH = 75; //91-path.length(data/fonts/)-5(extra)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data != null && resultCode == Activity.RESULT_OK){
            mImportPath = data.getStringExtra("path");
            mImportName = data.getStringExtra("name");
            if(isContant(mImportName)){
                Toast.makeText(getActivity(), R.string.font_setting_is_contant, Toast.LENGTH_LONG).show();
                return;
            }
            if(mImportName.length() > MAX_NAME_LENGTH){
                Toast.makeText(getActivity(), R.string.font_setting_name_too_long, Toast.LENGTH_LONG).show();
                return;
            }
            if(Typeface.isTypefaceOk(mImportPath)){
                showProgressDialog(getString(R.string.font_setting_dialog_import),getString(R.string.font_setting_dialog_importing),importR);
            }else {
                Toast.makeText(getActivity(), R.string.font_setting_not_enable, Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isContant(String mImportName){
        String mImport = mImportName.substring(0, mImportName.lastIndexOf("."));
        for(int i=0;i<usrFonts.size();i++){
            if(mImport.equals(usrFonts.get(i).fileName)){
                return true;
            }
        }
        return false;
    }

    public boolean copyFile(String oldPath, String newPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) {
                inStream = new FileInputStream(oldPath);
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
            }
            setPermission(newPath ,666);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }finally{
            if(inStream != null){
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(fs != null){
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private void setPermission(String path, int perm) {
        // SPRD: Modify 20131206 Spreadst of bug247489, permission of font file
        // cannot be changed successfully if file path contains special character
        String cmd[] = {"chmod", String.valueOf(perm), path};
        Runtime r = Runtime.getRuntime();
        try {
            r.exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setDefault(){
        OnClickListener l = new OnClickListener(){

            public void onClick(DialogInterface dialog, int which) {

                try{
                    IActivityManager am = ActivityManagerNative.getDefault();
                    Configuration config = am.getConfiguration();
                    config.bUserSetTypeface = false;
                    config.sUserTypeface = null;
                    am.updateConfiguration(config);
                    updateDefaultMenu();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        };
        showAlertDialog(R.string.font_setting_dialog_set_default,R.string.font_setting_dialog_set_default_confirmation,l);

    }
    Runnable deleteR = new Runnable(){

        public void run() {
            File f = new File(deletePath);
            boolean isCurrent = false;
            if (f.exists()) {
                try{
                    IActivityManager am = ActivityManagerNative.getDefault();
                    Configuration config = am.getConfiguration();
                    if(config.bUserSetTypeface && deletePath.equals(config.sUserTypeface)){
                        isCurrent = true;
                        config.bUserSetTypeface = false;
                        config.sUserTypeface = null;
                        am.updateConfiguration(config);
                        updateDefaultMenu();
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                f.delete();
            }
            if(!isCurrent){
                mHandler.sendEmptyMessage(DELETE_COMPLITE);
                isCurrent = false;
            }
        }

    };
    Runnable importR = new Runnable(){

        public void run() {
            boolean success = copyFile(mImportPath,USR_FONT_PATH + "/" + mImportName);
            if(success){
                mHandler.sendEmptyMessage(IMPORT_SUCCESS);
            }else {
                mHandler.sendEmptyMessage(IMPORT_FAIL);
            }
        }

    };
    private void deleteFont(){
        OnClickListener l = new OnClickListener(){

            public void onClick(DialogInterface dialog, int which) {

                showProgressDialog(getString(R.string.font_setting_dialog_delete),getString(R.string.font_setting_dialog_deleting),deleteR);
            }

        };
        if (deletePath.equals(Typeface.mUserSetTfPath)) {
            showAlertDialog(R.string.font_setting_dialog_delete,R.string.font_setting_dialog_delete_confirmation_and_reset,l);
        } else {
            showAlertDialog(R.string.font_setting_dialog_delete,R.string.font_setting_dialog_delete_confirmation,l);
        }
    }

    private static final int DELETE_COMPLITE = 0;
    private static final int IMPORT_SUCCESS = 1;
    private static final int IMPORT_FAIL = 2;
    Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
            case DELETE_COMPLITE:
                DissMissDlgAndSwitchToUsr();
                break;
            case IMPORT_SUCCESS:
                DissMissDlgAndSwitchToUsr();
                break;
            case IMPORT_FAIL:
                DissMissDlgAndSwitchToUsr();
                break;
            }
        }

    };
    private void DissMissDlgAndSwitchToUsr(){
        if(pDialog != null && pDialog.isShowing()){
            pDialog.dismiss();
        }
        fillFontList(USR_FONT_PATH, usrFonts);
        if(mCurrentTab != TAB_USER){
            mTabHost.setCurrentTabByTag(TAB_USER);
        }else{
            mAdapter.notifyDataSetChanged();
        }
    }
    public void showProgressDialog(String title, String msg, Runnable r){
        pDialog = ProgressDialog.show(getActivity(), title, msg, true, false);
        new Thread(r).start();
    }
    private void showAlertDialog(int titleId, int msgId, OnClickListener listener){
        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setMessage(msgId)
        .setTitle(titleId)
        .setPositiveButton(R.string.dlg_ok, listener)
        .setNegativeButton(R.string.dlg_cancel, new OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        ab.create().show();
    }
}
