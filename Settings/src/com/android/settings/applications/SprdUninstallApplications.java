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

package com.android.settings.applications;

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.NetworkPolicyManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFrameLayout;
import android.provider.Settings;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.app.IMediaContainerService;
import com.android.internal.content.PackageHelper;
import com.android.settings.R;
import com.android.settings.Settings.ManageApplicationsActivity;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.deviceinfo.StorageMeasurement;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.widget.CheckBox;
import android.widget.Button;
import android.net.Uri;
import android.view.View.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import java.util.Iterator;
import java.util.Map;
import com.android.settings.applications.ApplicationsState.AppEntry;

import android.widget.ImageButton;
import android.app.ActionBar;
import android.view.Gravity;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.ProgressDialog;

/**
 * Activity to pick an application that will be used to display installation
 * information and options to uninstall/delete user data for system
 * applications. This activity can be launched through Settings or via the
 * ACTION_MANAGE_PACKAGE_STORAGE intent.
 */
public class SprdUninstallApplications extends Fragment implements
        DialogInterface.OnDismissListener {

    static final String TAG = "SprdUninstallApplications";
    static final boolean DEBUG = true;

    private static final int MENU_OPTIONS_BASE = 0;

    public static final int SORT_ORDER_ALPHA = MENU_OPTIONS_BASE + 1;
    public static final int SORT_ORDER_SIZE = MENU_OPTIONS_BASE;
    public static final int SORT_ORDER_TIME = MENU_OPTIONS_BASE + 2;
    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;
    private static final String FILE_SORT_KEY = "sort_key";
    private static final String EXTRA_SORT_ORDER = "sortOrder";
    
    private static final int UNINSTALL_COMPLETE_MESSAGE = 1;

    private ApplicationsState mApplicationsState;
    public ApplicationsAdapter mApplications;
    
    private ViewGroup mRelative;
    private CheckBox mSelectAllBox;
    private TextView mSelectText;
    private Button mAllUninstallButton;
    private ImageButton mActionBarSort;

    private View mRootView;
    private View mListContainer;
    private ListView mListView;
    
    public static LayoutInflater mInflater;
    private Context mContext;
    
    ArrayList<ApplicationsState.AppEntry> mUninstallInfos;
    private PackageDeleteObserver mDeleterobserver;
    private boolean isCancleUninstall = false;
    private int mScussUninstallNum = 0, mFailUninstallNum = 0;
    private int mUnistallNum = 0;

    private AlertDialog mUnistallConfirmDialog,mSortDialog;
    private ProgressDialog mUninstallDialog;
    
    private SharedPreferences mSharedPreference;


    class ApplicationsAdapter extends BaseAdapter implements
            ApplicationsState.Callbacks, AbsListView.RecyclerListener {
        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastSortMode = -1;
        private LayoutInflater inflater = mInflater;
        SprdAppViewHolder holder;
        public final CharSequence mInvalidSizeStr = getActivity().getText(
                R.string.invalid_size_value);

        private HashMap<Long, Long> mCheckMap = new HashMap<Long, Long>();

        public void setCheckedArray(HashMap<Long, Long> checkedArray) {
            mCheckMap = checkedArray;
        }

        public void setChecked(int position, boolean checked) {
            long id = getItemId(position);
            if (checked) {
                mCheckMap.put(id, id);
            } else {
                mCheckMap.remove(id);
            }
        }

        public boolean hasCheckedItem() {
            return mCheckMap.size() > 0;
        }

        public boolean isChecked(int position) {
            long id = getItemId(position);
            return mCheckMap.containsValue(id);
        }

        public int getCheckedCount() {
            return mCheckMap.size();
        }

        public void updateCheckedMap() {
            if (mCheckMap.size() == 0 || mEntries == null) {
                return;
            }

            List<Long> delList = new ArrayList<Long>();
            for (Long id : mCheckMap.keySet()) {
                boolean isChecked = false;
                for (int j = 0; j < mEntries.size(); j++) {
                    if (mEntries.get(j).id == id) {
                        isChecked = true;
                        break;
                    }
                }
                if (isChecked == false) {
                    delList.add(id);
                }

            }
            for (int i = 0; i < delList.size(); i++) {
                mCheckMap.remove(delList.get(i));
            }
            Log.d(TAG, "mCheckMap.size() = " + mCheckMap.size());
        }

        public ArrayList<ApplicationsState.AppEntry> getCheckedAppInfo() {
            ArrayList<ApplicationsState.AppEntry> ids = new ArrayList<ApplicationsState.AppEntry>(
                    mCheckMap.size());
            int pos = 0;
            ApplicationsState.AppEntry appInfo;
            for (int i = 0; i < getCount(); i++) {
                if (isChecked(i)) {
                    appInfo = mEntries.get(i);
                    ids.add(appInfo);
                }
            }
            return ids;
        }

        public ApplicationsAdapter(ApplicationsState state) {
            mState = state;
            mSession = state.newSession(this);
        }

        public void resume(int sort) {
            if (DEBUG)
                Log.i(TAG, "Resume!  mResumed=" + mResumed);
            if (!mResumed) {
                mResumed = true;
                mSession.resume();
                mLastSortMode = sort;
                rebuild(true);
            } else {
                rebuild(sort);
            }
        }

        public void pause() {
            if (mResumed) {
                mResumed = false;
                mSession.pause();
            }
        }

        public void rebuild(int sort) {
            if (sort == mLastSortMode) {
                return;
            }
            mLastSortMode = sort;
            rebuild(true);
        }

        /**
         * SPRD:ADD release the session.
         */
        public void release() {
            mSession.release();
        }

        public void rebuild(boolean eraseold) {
            if (DEBUG)
                Log.i(TAG, "Rebuilding app list...");
            ApplicationsState.AppFilter filterObj = ApplicationsState.THIRD_PARTY_FILTER;
            Comparator<AppEntry> comparatorObj;
            Log.d(TAG, " mLastSortMode = " + mLastSortMode);
            switch (mLastSortMode) {
                case SORT_ORDER_SIZE:
                    comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                    break;
                case SORT_ORDER_TIME:
                    comparatorObj = ApplicationsState.TIME_COMPARATOR;
                    break;
                default:
                    comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                    break;
            }
            ArrayList<ApplicationsState.AppEntry> entries = mSession.rebuild(filterObj,
                    comparatorObj);
            if (entries == null && !eraseold) {
                // Don't have new list yet, but can continue using the old one.
                return;
            }
            if (entries != null) {
                mEntries = entries;
            } else {
                mEntries = null;
            }
            notifyDataSetChanged();

            if (entries == null) {
                mListContainer.setVisibility(View.INVISIBLE);
            } else {
                mListContainer.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onRunningStateChanged(boolean running) {
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            mListContainer.setVisibility(View.VISIBLE);
            mEntries = apps;
            notifyDataSetChanged();
        }

        @Override
        public void onPackageListChanged() {
            rebuild(false);
        }

        @Override
        public void onPackageIconChanged() {
            // We ensure icons are loaded when their item is displayed, so
            // don't care about icons loaded in the background.
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            for (int i = 0; i < mActive.size(); i++) {
                SprdAppViewHolder holder = (SprdAppViewHolder) mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        holder.updateSizeText(mInvalidSizeStr);
                    }
                    return;
                }
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (mLastSortMode == SORT_ORDER_SIZE) {
                rebuild(false);
            }
        }

        public int getCount() {
            return mEntries != null ? mEntries.size() : 0;
        }

        public Object getItem(int position) {
            return mEntries.get(position);
        }

        public ApplicationsState.AppEntry getAppEntry(int position) {
            return mEntries.get(position);
        }

        public long getItemId(int position) {
            return mEntries.get(position).id;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            
            holder = SprdAppViewHolder.createOrRecycle(mInflater, convertView);
            convertView = holder.rootView;
            ApplicationsState.AppEntry entry = mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                }
                mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }
                holder.updateSizeText(mInvalidSizeStr);
                holder.checkBox.setChecked(isChecked(position));
                final String pkg = entry.info.packageName;
                final String appName = entry.label;
                final boolean isChecked = holder.checkBox.isChecked();
                holder.checkBox.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setChecked(position,!isChecked);
                        updateViews(true);
                    }
                } );
                holder.button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Log.d(TAG,"holder.button onClick");
                        buildConfirmUninstallDialog(false,pkg,appName);
                    }
                } );
                
            }
            mActive.remove(convertView);
            mActive.add(convertView);
            return convertView;
        }

        @Override
        public void onMovedToScrapHeap(View view) {
            mActive.remove(view);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
        
        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mApplications = new ApplicationsAdapter(mApplicationsState);
        
        mSharedPreference = mContext.getSharedPreferences(FILE_SORT_KEY, 0);
        mSortOrder = mSharedPreference.getInt(FILE_SORT_KEY, SORT_ORDER_ALPHA);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;

        View rootView = mInflater.inflate(R.layout.uninstall_multi_select,
                container, false);

        mRootView = rootView;
        initialViews(container);
        
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        View customActionBarView = mInflater.inflate(R.layout.action_bar_custom_uui, null);
        View ationBar = (ViewGroup) customActionBarView.findViewById(R.id.ationBar);
        if (com.android.settings.Settings.UNIVERSEUI_SUPPORT) {
            ationBar.setBackground(mContext.getResources().getDrawable(R.drawable.ab_background));
        }
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.LEFT);
        actionBar.setCustomView(customActionBarView, params);
        ImageButton ationBarBack = (ImageButton) customActionBarView.findViewById(R.id.ationBarBack);
        mActionBarSort = (ImageButton) customActionBarView.findViewById(R.id.actionbar_sort);
        ationBarBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });
        /* SPRD: add for 306676 @{ */
        TextView uninstallView = (TextView) customActionBarView.findViewById(R.id.uninstall);
        uninstallView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });
        /* @} */
        mActionBarSort.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buildSortDialog();
            }
        });
    
        return rootView;
    }

    public void onBackPressed() {
        getActivity().finish();
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mApplications != null) {
            mApplications.resume(mSortOrder);
            mApplications.updateCheckedMap();
            updateViews(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mUnistallConfirmDialog != null) {
            mUnistallConfirmDialog.dismiss();
            mUnistallConfirmDialog = null;
        }
        if (mUninstallDialog != null) {
            mUninstallDialog.dismiss();
            mUninstallDialog = null;
        }
        if (mSortDialog != null) {
            mSortDialog.dismiss();
            mSortDialog = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void initialViews(ViewGroup contentParent) {
        if (mRootView == null) {
            Log.d(TAG, "mRootView == null ");
            return;
        }
        mRelative = (ViewGroup) mRootView.findViewById(R.id.select);
        mSelectAllBox = (CheckBox) mRootView.findViewById(R.id.select_all);
        mAllUninstallButton = (Button) mRootView.findViewById(R.id.all_delete);
        mSelectText = (TextView) mRootView.findViewById(R.id.select_text);
        mListContainer = mRootView.findViewById(R.id.list_container);
        mSelectAllBox.setVisibility(View.VISIBLE);

        if (mListContainer != null) {
            // Create adapter and list view here
            View emptyView = mListContainer.findViewById(com.android.internal.R.id.empty);
            ListView lv = (ListView) mListContainer.findViewById(android.R.id.list);
            if (emptyView != null) {
                lv.setEmptyView(emptyView);
            }
            lv.setSaveEnabled(true);
            lv.setItemsCanFocus(true);
            lv.setTextFilterEnabled(true);
            mListView = lv;
            mListView.setAdapter(mApplications);
            mListView.setRecyclerListener(mApplications);

            Utils.prepareCustomPreferencesList(contentParent, mRootView, mListView, false);
        }
    }
    private void updateViews(boolean isResume) {
        if (!isVisible() && !isResume) {
            Log.d(TAG, "updateViews, not  Visible");
            return;
        }

        if (mApplications.mEntries != null && mApplications.mEntries.size() == 0) {
            mRelative.setVisibility(View.GONE);
            mActionBarSort.setVisibility(View.GONE);
        } else {
            mRelative.setVisibility(View.VISIBLE);
            mActionBarSort.setVisibility(View.VISIBLE);
        }
       
        int selectNum = mApplications.getCheckedCount();
        int allNum = mApplications.getCount();
        Log.d(TAG, "selectNum =" + selectNum + " allNum = " + allNum
                + " mApplications.mEntries.size() = " + mApplications.mEntries.size());
        mAllUninstallButton.setEnabled(true);
        if (selectNum == allNum) {
            mSelectAllBox.setChecked(true);
            mSelectText.setText(R.string.cancle_select_all);
        } else {
            mSelectAllBox.setChecked(false);
            mSelectText.setText(R.string.select_all);
            if (selectNum == 0) {
                mAllUninstallButton.setEnabled(false);
            }
        }

        updateUninstallSummary(selectNum);
        mSelectAllBox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int allNum = mApplications.getCount();
                for (int i = 0; i < allNum; i++) {
                    mApplications.setChecked(i, mSelectAllBox.isChecked());
                }
                updateUninstallSummary(mApplications.getCheckedCount());
                if (mSelectAllBox.isChecked()) {
                    mAllUninstallButton.setEnabled(true);
                    mSelectText.setText(R.string.cancle_select_all);
                } else {
                    mAllUninstallButton.setEnabled(false);
                    mSelectText.setText(R.string.select_all);
                }
                mApplications.notifyDataSetChanged();
            }
        });
        mAllUninstallButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                buildConfirmUninstallDialog(true, null, null);
            }
        });
        mApplications.notifyDataSetChanged();
    }
    private void updateUninstallSummary(int num) {
        String allUninstallSummary = getString(R.string.beatch_no_select_uninstall);
        if (num != 0) {
            allUninstallSummary = getString(R.string.beatch_uninstall, num);
        }
        mAllUninstallButton.setText(allUninstallSummary);
    }
    private void updateData() {
        if (!isVisible()) {
            Log.d(TAG, "updateData, not  Visible");
            return;
        }
        String message = getString(R.string.uninstall_done, mScussUninstallNum);
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        mScussUninstallNum = 0;
        mFailUninstallNum = 0;
        isCancleUninstall = false;
        mApplications.updateCheckedMap();
        updateViews(false);
        if (mUninstallDialog != null) {
            mUninstallDialog.dismiss();
        }
        return;
    }
    private void uninstallOneApp(String packageName, boolean allUsers, boolean andDisable) {
        if (mDeleterobserver == null) {
            mDeleterobserver = new PackageDeleteObserver();
        }
        mContext.getPackageManager().deletePackage(packageName, mDeleterobserver,
                allUsers ? PackageManager.DELETE_ALL_USERS : 0);
    }
    
    void buildConfirmUninstallDialog(final boolean isBeatch, final String packageName, String name) {
        if (mUnistallConfirmDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.delete_applications);
            String message;
            if (isBeatch) {
                message = getString(R.string.confirm_beatch_uninstall_message,
                        mApplications.getCheckedCount());
            } else {
                message = getString(R.string.confirm_one_uninstall_message, name);
            }
            builder.setMessage(message);
            builder.setPositiveButton(R.string.dlg_ok,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (isBeatch) {
                                mUninstallInfos = mApplications.getCheckedAppInfo();
                                mUnistallNum = mUninstallInfos.size();
                                Log.d(TAG, "uninstallInfo.size() = " + mUnistallNum);
                                if (mUninstallInfos != null && mUninstallInfos.size() > 0) {
                                    uninstallOneApp(mUninstallInfos.get(0).info.packageName, true,
                                            false);
                                    mUninstallInfos.remove(0);
                                    mApplications.updateCheckedMap();
                                    updateViews(false);
                                }
                            } else {
                                mUnistallNum = 1;
                                uninstallOneApp(packageName, true, false);
                            }
                            buildUninstallDialog();
                        }
                    });
            builder.setNegativeButton(R.string.dlg_cancel,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mUnistallConfirmDialog.dismiss();
                        }
                    });
            mUnistallConfirmDialog = builder.show();
            mUnistallConfirmDialog.setOnDismissListener(this);
        }
    }

    void buildUninstallDialog() {
        if (mUninstallDialog == null) {
            mUninstallDialog = new ProgressDialog(mContext);
            mUninstallDialog.setMessage(getString(R.string.uninstalling_message));
            mUninstallDialog.setCancelable(false);
            mUninstallDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    mContext.getText(R.string.dlg_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isCancleUninstall = true;
                            if (mUninstallDialog != null) {
                                mUninstallDialog.dismiss();
                            }
                        }
                    });
            mUninstallDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mUninstallDialog.show();
            mUninstallDialog.setOnDismissListener(this);
        }

    }
    
    void buildSortDialog() {
        if (mSortDialog == null) {
            AlertDialog.Builder mSortDialog = new AlertDialog.Builder(mContext);
            mSortDialog.setTitle(R.string.sort_type);
            final int selectItem = mSortOrder;
            Log.d("order","selectItem = "+selectItem);
            mSortDialog.setSingleChoiceItems(R.array.sort_type, selectItem,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mSortOrder = whichButton;
                        }
                    });
            mSortDialog.setNegativeButton(R.string.dlg_cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mSortOrder = selectItem;
                        }
                    });
            mSortDialog.setPositiveButton(R.string.dlg_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton){
                SharedPreferences.Editor editor = mSharedPreference.edit();
                Log.d(TAG,"setPositiveButton mSortOrder = "+mSortOrder);
                editor.putInt(FILE_SORT_KEY, mSortOrder).commit();   
                if (mApplications != null) {
                    mApplications.resume(mSortOrder);
                }
                }
            });
            mSortDialog.show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mUnistallConfirmDialog == dialog) {
            mUnistallConfirmDialog = null;
        }
        if (mUninstallDialog == dialog) {
            mUninstallDialog = null;
        }
        if (mSortDialog == dialog) {
            mSortDialog = null;
        }
    }
   
    private Handler mUninstallHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UNINSTALL_COMPLETE_MESSAGE:
                    if (mUnistallNum > 0) {
                        mUnistallNum--;
                    }

                    final String packageName = (String) msg.obj;
                    switch (msg.arg1) {
                        case PackageManager.DELETE_SUCCEEDED:
                            mScussUninstallNum++;
                            break;
                        case PackageManager.DELETE_FAILED_DEVICE_POLICY_MANAGER:
                            mFailUninstallNum++;
                            Log.d(TAG, "Uninstall failed because " + packageName
                                    + " is a device admin");
                            break;
                        default:
                            mFailUninstallNum++;
                            Log.d(TAG, "Uninstall failed for " + packageName + " with code "
                                    + msg.arg1);
                            break;
                    }
                    if (isCancleUninstall) {
                        updateData();
                    }else {
                        if (mUninstallInfos != null && mUninstallInfos.size() > 0) {
                            uninstallOneApp(mUninstallInfos.get(0).info.packageName, true, false);
                            mUninstallInfos.remove(0);
                            mApplications.updateCheckedMap();
                            updateViews(false);
                        }
                        if (mUnistallNum == 0) {
                            updateData();
                        }
                    }

                    break;
                default:
                    break;
            }
        }
    };

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(String packageName, int returnCode) {
            Message msg = mUninstallHandler.obtainMessage(UNINSTALL_COMPLETE_MESSAGE);
            msg.arg1 = returnCode;
            msg.obj = packageName;
            mUninstallHandler.sendMessage(msg);
        }
    }

}
