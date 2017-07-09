package com.oymotion.gforcedev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.oymotion.gforcedev.R;
import com.oymotion.gforcedev.global.OymotionApplication;


/**
 * MemuAdapter
 *
 * @author MouMou
 */
public class MemuAdapter extends BaseAdapter {
    private String[] menuLists;
    private Integer[] iconLists;

    public MemuAdapter(String[] sCheeseStrings, Integer[] sCheeseIcons) {
        menuLists = sCheeseStrings;
        iconLists = sCheeseIcons;
    }

    @Override
    public int getCount() {
        return menuLists.length;
    }

    @Override
    public Object getItem(int position) {
        return menuLists[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            final LayoutInflater inflater = (LayoutInflater) OymotionApplication.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_list_menu, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.iv_menu);
            holder.text = (TextView) convertView.findViewById(R.id.tv_menu);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.icon.setImageResource(iconLists[position]);
        holder.text.setText(menuLists[position]);
        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView text;
    }
}
