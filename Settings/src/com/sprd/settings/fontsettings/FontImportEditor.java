/** Create by Spreadst */
package com.sprd.settings.fontsettings;
/*
 * Copyright (C) 2006 The Android Open Source Project
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.io.IOException;
import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.provider.Settings;
import android.provider.MediaStore;
import android.widget.Toast;
import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.content.ContentResolver;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.EditText;
import com.android.settings.R;

public class FontImportEditor extends ListActivity {

    /* SPRD: dual T policy porting@{ */
    // SPRD：MODIFY for double T card
    private static final String STORAGE_PATH_DOUBLE_T = "/storage/";
    private File mCurrentDir = new File(STORAGE_PATH_DOUBLE_T);
    /* @} */
    //menu item constant
    private static final int MENU_ID_PATH_SAVE = Menu.FIRST ;
    private static final int MENU_ID_PATH_PROP = Menu.FIRST + 1;
    private FileListAdapter mOnlyAdapter;
    boolean fileDisplay = true;
    boolean backActivity = true;
    //20121026 - For bug 78847
    /* SPRD: modify actionbar title for bug 264804 @{ */
    //TextView mTitleText;
    ActionBar mActionBar;
    /* @} */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.path_file_manager);
        //20121026 - For bug 78847
        /* SPRD: modify actionbar title for bug 264804 @{ */
        /*
         * ActionBar actionBar = getActionBar();
         * actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
         * actionBar.setCustomView(R.layout.custom_title);
         * mTitleText = (TextView) actionBar.getCustomView().findViewById(R.id.custom_title);
         */
        mActionBar = getActionBar();
        /* @} */
        // SPRD: dual T policy porting
        // SPRD：MODIFY for double T card
        mCurrentDir = new File(STORAGE_PATH_DOUBLE_T);
        mOnlyAdapter = new FileListAdapter(this);
        mOnlyAdapter.sortImpl( new File(STORAGE_PATH_DOUBLE_T), "ttf");
        setListAdapter(mOnlyAdapter);

        updateTitle();
    }

    public void updateListView()
    {
        mOnlyAdapter.sortImpl(mCurrentDir, "ttf");
        updateTitle();
    }

    private static final int MAX_FILENAME_LEN = 60;
    public void updateTitle(){
        if(mCurrentDir == null){
            /* SPRD: modify actionbar title for bug 264804 @{ */
            //mTitleText.setText(getString(R.string.settings_label_launcher));
            mActionBar.setTitle(getString(R.string.settings_label_launcher));
            /* @} */
            return;
        }
        /* SPRD: modify actionbar title for bug 264804 @{ */
        //mTitleText.setText(mCurrentDir.getPath());
        mActionBar.setTitle(mCurrentDir.getPath());
        /* @} */
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        String itemStr = ((FileListAdapter.ViewHolder)v.getTag()).filename.getText().toString();
        final File file = new File(mCurrentDir, itemStr);

        if(file.isDirectory())
        {
            // Open the file folder
            mCurrentDir = new File(mCurrentDir.getPath() + "/" + itemStr);
            updateListView();
            getListView().setSelection(0);
        } else {
            // SPRD: if sd card and internal storage is not avalible,show toast
            if (!Environment.getExternalStoragePathState().equals(Environment.MEDIA_MOUNTED)
                    && !Environment.getInternalStoragePathState().equals(Environment.MEDIA_MOUNTED))
            {
                Toast.makeText(getApplicationContext(), R.string.stroage_not_mounted,
                        Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent();
                intent.putExtra("path", file.getAbsolutePath());
                intent.putExtra("name", file.getName());
                setResult(RESULT_OK, intent);
            }
            finish();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
            {
                if(mCurrentDir == null)
                    break;
                String str = mCurrentDir.getParentFile().getPath();
                String currentFileName = mCurrentDir.getName();
                // SPRD:MODIFY the root dir is "/" instead of "/storage".
                if(!str.equals("/"))
                {
                    mCurrentDir = new File(str);
                    updateListView();

                    int index = ((FileListAdapter)getListAdapter()).getItemIndex(currentFileName);
                    getListView().setSelection(index);

                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public static String getFileExtendName(String filename)
    {
        int index = filename.lastIndexOf('.');
        return index == -1? null : filename.substring(index + 1);
    }

}
