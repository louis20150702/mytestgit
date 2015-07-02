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

package com.android.settings.wifi;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

class WifiDialog extends AlertDialog implements WifiConfigUiBase {
    //add by spreadst_lc for cmcc wifi feature start
    static int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
    static final int BUTTON_FORGET = DialogInterface.BUTTON_NEUTRAL;
    static int BUTTON_DISCONNECT = DialogInterface.BUTTON_POSITIVE;
    //add by spreadst_lc for cmcc wifi feature end
    private final boolean mEdit;
    private final DialogInterface.OnClickListener mListener;
    private final AccessPoint mAccessPoint;

    private View mView;
    private WifiConfigController mController;

    public WifiDialog(Context context, DialogInterface.OnClickListener listener,
            AccessPoint accessPoint, boolean edit) {
        super(context);
        mEdit = edit;
        mListener = listener;
        mAccessPoint = accessPoint;
    }

    @Override
    public WifiConfigController getController() {
        return mController;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.wifi_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);
        mController = new WifiConfigController(this, mView, mAccessPoint, mEdit);
        super.onCreate(savedInstanceState);
        /* During creation, the submit button can be unavailable to determine
         * visibility. Right after creation, update button visibility */
        mController.enableSubmitIfAppropriate();
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public boolean isEdit() {
        return mEdit;
    }

    @Override
    public Button getSubmitButton() {
        return getButton(BUTTON_SUBMIT);
    }

    @Override
    public Button getForgetButton() {
        return getButton(BUTTON_FORGET);
    }

    @Override
    public Button getCancelButton() {
        return getButton(BUTTON_NEGATIVE);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        //add by spreadst_lc for cmcc wifi feature start
        BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
        BUTTON_DISCONNECT = 0;
        //add by spreadst_lc for cmcc wifi feature end
        setButton(BUTTON_SUBMIT, text, mListener);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        setButton(BUTTON_FORGET, text, mListener);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(BUTTON_NEGATIVE, text, mListener);
    }

    //add by spreadst_lc for cmcc wifi feature start
    public void setDisconnectButton(CharSequence text) {
        BUTTON_DISCONNECT = DialogInterface.BUTTON_POSITIVE;
        BUTTON_SUBMIT = 0;
        setButton(BUTTON_DISCONNECT, text, mListener);
    }

    public Button getDisconnectButton() {
        return getButton(BUTTON_DISCONNECT);
    }
    //add by spreadst_lc for cmcc wifi feature end
}
