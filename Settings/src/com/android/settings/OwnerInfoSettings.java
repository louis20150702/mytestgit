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

package com.android.settings;

import android.app.Fragment;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.widget.LockPatternUtils;

/* SPRD: Modify Bug 258911,not display keyboard when edit box disabled @{ */
import android.view.WindowManager;
/* @} */

public class OwnerInfoSettings extends Fragment {

    public static final String EXTRA_SHOW_NICKNAME = "show_nickname";

    private View mView;
    private CheckBox mCheckbox;
    private int mUserId;
    private LockPatternUtils mLockPatternUtils;
    private EditText mOwnerInfo;
    private EditText mNickname;
    private boolean mShowNickname;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.containsKey(EXTRA_SHOW_NICKNAME)) {
            mShowNickname = args.getBoolean(EXTRA_SHOW_NICKNAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.ownerinfo, container, false);
        mUserId = UserHandle.myUserId();
        mLockPatternUtils = new LockPatternUtils(getActivity());
        initView(mView);
        return mView;
    }

    private void initView(View view) {
        final ContentResolver res = getActivity().getContentResolver();
        String info = mLockPatternUtils.getOwnerInfo(mUserId);
        boolean enabled = mLockPatternUtils.isOwnerInfoEnabled();
        mCheckbox = (CheckBox) mView.findViewById(R.id.show_owner_info_on_lockscreen_checkbox);
        mOwnerInfo = (EditText) mView.findViewById(R.id.owner_info_edit_text);
        mOwnerInfo.setText(info);
        mOwnerInfo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128)});
        /* SPRD: Modify Bug 208120,adjust the cursor to the end @{ */
        mOwnerInfo.setSelection(info==null?0:info.length());
        /* @} */
        mOwnerInfo.setEnabled(enabled);
        mNickname = (EditText) mView.findViewById(R.id.owner_info_nickname);
        if (!mShowNickname) {
            mNickname.setVisibility(View.GONE);
        } else {
            mNickname.setText(UserManager.get(getActivity()).getUserName());
            mNickname.setSelected(true);
        }
        mCheckbox.setChecked(enabled);
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            if (UserManager.get(getActivity()).isLinkedUser()) {
                mCheckbox.setText(R.string.show_profile_info_on_lockscreen_label);
            } else {
                mCheckbox.setText(R.string.show_user_info_on_lockscreen_label);
            }
        }
        
        /* SPRD: Modify Bug 258911,not display keyboard when edit box disabled @{ */
        if (!enabled) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        /* @} */ 
        
        mCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLockPatternUtils.setOwnerInfoEnabled(isChecked);
                mOwnerInfo.setEnabled(isChecked); // disable text field if not enabled
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
    }

    void saveChanges() {
        ContentResolver res = getActivity().getContentResolver();
        String info = mOwnerInfo.getText().toString();
        mLockPatternUtils.setOwnerInfo(info, mUserId);
        if (mShowNickname) {
            String oldName = UserManager.get(getActivity()).getUserName();
            CharSequence newName = mNickname.getText();
            if (!TextUtils.isEmpty(newName) && !newName.equals(oldName)) {
                UserManager.get(getActivity()).setUserName(UserHandle.myUserId(),
                        newName.toString());
            }
        }
    }

}
