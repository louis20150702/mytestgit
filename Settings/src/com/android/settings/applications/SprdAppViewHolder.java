package com.android.settings.applications;

import com.android.settings.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import com.android.settings.applications.ManageApplications;

// View Holder used when displaying views
public class SprdAppViewHolder {
    public ApplicationsState.AppEntry entry;
    public View rootView;
    public TextView appName;
    public ImageView appIcon;
    public TextView appSize;
    public CheckBox checkBox;
    public Button button;

    static public SprdAppViewHolder createOrRecycle(LayoutInflater inflater, View convertView) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.uninstall_filelist_item, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            SprdAppViewHolder holder = new SprdAppViewHolder();
            holder.rootView = convertView;
            holder.appName = (TextView) convertView.findViewById(R.id.app_name);
            holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
            holder.appSize = (TextView) convertView.findViewById(R.id.app_size);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.select_checkbox);
            holder.button = (Button) convertView.findViewById(R.id.delete);
            convertView.setTag(holder);
            return holder;
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            return (SprdAppViewHolder)convertView.getTag();
        }
    }

    void updateSizeText(CharSequence invalidSizeStr) {
        if (entry.sizeStr != null) {
            appSize.setText(entry.sizeStr);
        } else if (entry.size == ApplicationsState.SIZE_INVALID) {
            appSize.setText(invalidSizeStr);
        }
    }
}
