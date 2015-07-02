package com.sprd.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.preference.Preference;
import android.widget.RadioButton;
import com.android.settings.R;

public class UsbRadioPreference extends Preference {
    private boolean mChecked;

    public UsbRadioPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public UsbRadioPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UsbRadioPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.radio_button);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View radiobuttonView = view.findViewById(R.id.radiobutton);
        if (radiobuttonView != null && radiobuttonView instanceof RadioButton) {
            ((RadioButton) radiobuttonView).setChecked(mChecked);
        }
    }

    public void setChecked(boolean checked) {
        final boolean changed = mChecked != checked;
        if (changed) {
            mChecked = checked;
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    public boolean isChecked() {
        return mChecked;
    }
}

