
package com.sprd.settings.sim;


import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.sim.Sim;
import android.sim.SimManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.WirelessSettings;
import com.android.featureoption.FeatureOption;
import com.android.internal.telephony.TelephonyIntents;
import android.os.SystemProperties;
import android.util.Log;

public class SimInfoSetActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private EditTextPreference mName;

    // add for SPC0132986 begin
    private EditTextPreference mNumber;
    // add for SPC0132986 end
    
    private ColorPreference mColor;
    
    private Preference mOperator;

    private Context mContext;

    private int mPhoneId = -1;
    
    private Dialog colorDialog;

    private int mColorIndexSelected = 0;

    private int mColorIndexUsed = -1;

    private static final String KEY_NAME = "name_setting";
		
    private static final String KEY_NUMBER = "number_setting";// add for SPC0132986 begin
    
    private static final String KEY_COLOR = "color_setting";
    
    private static final String KEY_OPERATOR = "operator_setting";

    private static final int INPUT_MAX_LENGTH = 7;
		
    private static final int INPUT_SIM_NUMBER_MAX_LENGTH = 11;// add for SPC0132986
    
    private int[] SIM_IMAGES_BIG = {
            R.drawable.sim_color_1_big_sprd, R.drawable.sim_color_2_big_sprd,
            R.drawable.sim_color_3_big_sprd, R.drawable.sim_color_4_big_sprd,
            R.drawable.sim_color_5_big_sprd, R.drawable.sim_color_6_big_sprd,
            R.drawable.sim_color_7_big_sprd, R.drawable.sim_color_8_big_sprd,
    };
 
    public void onCreate(Bundle savedInstanceState) {
        ActionBar actionBar =  getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,ActionBar.DISPLAY_HOME_AS_UP);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        super.onCreate(savedInstanceState);

        mContext = this;
        mPhoneId = this.getIntent().getIntExtra("phoneId", 0);
        SimManager simManager = SimManager.get(mContext);
        String simName = simManager.getName(mPhoneId);
        Sim[] sims = simManager.getSims();
        if (sims == null || sims.length == 0) {
            return;
        }

        mColorIndexSelected = simManager.getColorIndex(mPhoneId);
        for (int i = 0; i < sims.length; i++) {
            if (sims[i].getPhoneId() != mPhoneId) {
                mColorIndexUsed = simManager.getColorIndex(sims[i].getPhoneId());
                break;
            }
        }

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        mName = new EditTextPreference(this);
        mName.setPersistent(false);
        mName.setDialogTitle(R.string.sim_name_setting_title);
        mName.setKey(KEY_NAME + mPhoneId);
        mName.setTitle(R.string.sim_name_setting_title);
        mName.setText(simName);
        mName.getEditText().setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        /* SPRD: modify for the limit of Chinese or English character and cursor position @{ */
        mName.getEditText().addTextChangedListener(new TextWatcher() {
            private int editStart;
            private EditText mEditText = mName.getEditText();

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
                editStart = mEditText.getSelectionStart();
                mEditText.removeTextChangedListener(this);
                while (calculateLength(s.toString()) > INPUT_MAX_LENGTH) {
                    s.delete(editStart - 1, editStart);
                    editStart--;
                }
                mEditText.setText(s);
                mEditText.setSelection(editStart);
                mEditText.addTextChangedListener(this);
                Dialog dialog = mName.getDialog();
                if (dialog instanceof AlertDialog) {
                    Button btn = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    btn.setEnabled(s.toString().trim().length() > 0);
                }
            }

            private int calculateLength(CharSequence c) {
                double len = 0;
                for (int i = 0; i < c.length(); i++) {
                    int tmp = (int) c.charAt(i);
                    if (tmp > 0 && tmp < 127) {
                        len += 0.5;
                    } else {
                        len++;
                    }
                }
                return (int) Math.round(len);
            }
        });
        /* @} */
        mName.setOnPreferenceChangeListener(this);
        root.addPreference(mName);

        // add for SPC0132986 begin
         if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME)	{		
        	    addPrefSimName(root);
         }		
        // add for SPC0132986 end
				
        if (!Settings.CU_SUPPORT || FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME) {
        mColor = new ColorPreference(mContext, null);
        mColor.setKey(KEY_COLOR + mPhoneId);
        mColor.setTitle(R.string.sim_color_setting_title);
        mColor.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                pickColorDialog();
                return true;
            }
        });
        root.addPreference(mColor);
        }
        if (Settings.CU_SUPPORT) {
            mOperator = new Preference(mContext, null, 0);
            mOperator.setKey(KEY_OPERATOR + mPhoneId);
            mOperator.setTitle(R.string.device_status);
            mOperator.setSummary(R.string.device_status_summary);
            mOperator.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings",
                            "com.android.settings.deviceinfo.StatusSim");
                    intent.putExtra(WirelessSettings.SUB_ID, mPhoneId);
                    SimInfoSetActivity.this.startActivity(intent);
                    return true;
                }
            });
            root.addPreference(mOperator);
        }

        setPreferenceScreen(root);

        refreshSimInfo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if (item.getItemId() == android.R.id.home) {
            finish();
            return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    // add for SPC0132986 begin   diwei add for set sim name 
    private void addPrefSimName(PreferenceScreen parent){
        if(parent!=null){
            SimManager simManager = SimManager.get(mContext);
            String simNumber = simManager.getNumber(mPhoneId);
            mNumber = new EditTextPreference(this);
            mNumber.setPersistent(false);
            mNumber.setDialogTitle(R.string.sim_number_setting_title);
            mNumber.setKey(KEY_NUMBER + mPhoneId);
            mNumber.setTitle(R.string.sim_number_setting_title);
            mNumber.setText(simNumber);
            mNumber.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            mNumber.getEditText().addTextChangedListener(new TextWatcher() {
                private int editStart;
                private EditText mEditText = mNumber.getEditText();

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void afterTextChanged(Editable s) {
                    editStart = mEditText.getSelectionStart();
                    mEditText.removeTextChangedListener(this);
                    while (s.length() > INPUT_SIM_NUMBER_MAX_LENGTH) {
                        s.delete(editStart - 1, editStart);
                        editStart--;
                    }
                    mEditText.setText(s);
                    mEditText.setSelection(editStart);
                    mEditText.addTextChangedListener(this);
                    Dialog dialog = mNumber.getDialog();
                    if (dialog instanceof AlertDialog) {
                        Button btn = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        btn.setEnabled(s.toString().length() >= 0);
                    }
                }
            });

            mNumber.setOnPreferenceChangeListener(this);
            parent.addPreference(mNumber);
        }
    }
    // add for SPC0132986 end


    void refreshSimInfo() {
        SimManager simManager = SimManager.get(mContext);
        String name = simManager.getName(mPhoneId);
        mName.setSummary(name);

        // add for SPC0132986 begin
        if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME){
	        String number = simManager.getNumber(mPhoneId);
	        mNumber.setSummary(number);
        }		
        // add for SPC0132986 end
        
        if (!Settings.CU_SUPPORT || FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME) {
            mColor.notifyColorUpdated();
        }

				
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mName) {
            SimManager simManager = SimManager.get(mContext);
            simManager.setName(mPhoneId, (String) newValue);
            refreshSimInfo();
        } 
	// add for SPC0132986 begin
        else if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME && preference == mNumber ){
            SimManager simManager = SimManager.get(mContext);
            simManager.setNumber(mPhoneId, (String) newValue);
            refreshSimInfo();
        }
        // add for SPC0132986 end
        return true;
    }

    void pickColorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View colorSelectView = inflater.inflate(R.layout.sim_color_pick, null);
        final GridView gridView = (GridView) colorSelectView.findViewById(R.id.color_gridview);

        ColorAdapter adapter = new ColorAdapter(mColorIndexSelected);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                SimManager simManager = SimManager.get(mContext);
                simManager.setColorIndex(mPhoneId, position);
                mColorIndexSelected = position;
                refreshSimInfo();
                if(FeatureOption.PRJ_FEATURE_SIM_SETTING_NAME){				
                    Intent intent = new Intent();			
                    intent.setAction(TelephonyIntents.ACTION_SIM_COLOR_CHANGED);
                    intent.putExtra(TelephonyIntents.EXTRA_PHONE_ID, mPhoneId);
                    sendBroadcast(intent); 
                }
                if (colorDialog.isShowing())
                    colorDialog.dismiss();

            }
        });

        builder.setTitle(getResources().getString(R.string.sim_color_setting_title));
        builder.setView(colorSelectView);
        builder.setNegativeButton(getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        colorDialog = builder.create();
        colorDialog.show();
    }

    public class ColorPreference extends Preference {

        public ColorPreference(final Context context, final AttributeSet attrs, final int defStyle) {
            super(context, attrs);
            this.setLayoutResource(R.layout.sim_color_setting);
        }

        public ColorPreference(final Context context, final AttributeSet attrs) {
            this(context, attrs, 0);
        }


        protected void onBindView(final View view) {
            super.onBindView(view);
            final ImageView colorImage = (ImageView) view.findViewById(R.id.sim_color);
            SimManager simManager = SimManager.get(mContext);
            Sim sim = simManager.getSimById(mPhoneId);
            colorImage.setImageResource(SimManager.COLORS_IMAGES[sim.getColorIndex()]);
        }

        public void notifyColorUpdated() {
            this.notifyChanged();
        } 
    }

    public class ColorAdapter extends BaseAdapter {

        private int mSelected;
        private int mColorItemWidth;

        public ColorAdapter(int selected) {
            this.mSelected = selected;
            mColorItemWidth = getResources().getDimensionPixelOffset(
                    R.dimen.uui_sim_color_item_width);
        }

        public int getCount() {
            return SimManager.COLORS.length;
        }

        public Object getItem(int position) {
            return SimManager.COLORS[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View colorItem = inflater.inflate(R.layout.sim_color_item, null);
            final ImageView imageView = (ImageView) colorItem.findViewById(R.id.color_image);

            colorItem.setLayoutParams(new GridView.LayoutParams(mColorItemWidth, mColorItemWidth));
            colorItem.setPadding(6, 6, 6, 6);

            imageView.setImageResource(SIM_IMAGES_BIG[position]);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            if (position == mSelected) {
                colorItem.setBackgroundResource(R.drawable.sim_color_selected_sprd);
            }else if (position == mColorIndexUsed) {
                colorItem.setBackgroundResource(R.drawable.sim_color_used_sprd);
            }
            return colorItem;

        }
    }
}