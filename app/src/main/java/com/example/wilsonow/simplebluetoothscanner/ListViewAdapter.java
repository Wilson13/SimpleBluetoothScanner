package com.example.wilsonow.simplebluetoothscanner;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ListViewAdapter extends ArrayAdapter<ListViewItem> {

    Context context;
    List items;

    public ListViewAdapter(Context context, List items) {
        super(context, android.R.layout.simple_list_item_1, items);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        ListViewItem lvItems = getItem(position);
        if (null == convertView) {
            View returnView = inflater.inflate(R.layout.bluetooth_listview, parent, false);
            ((TextView) returnView.findViewById(R.id.tv_bt_name)).setText( lvItems.getName() );
            ((TextView) returnView.findViewById(R.id.tv_bt_addr)).setText( lvItems.getAddr() );
            return returnView;
        } else { // Reuse UI to avoid duplicates!
            ((TextView) convertView.findViewById(R.id.tv_bt_name)).setText( lvItems.getName() );
            ((TextView) convertView.findViewById(R.id.tv_bt_addr)).setText( lvItems.getAddr() + " " + lvItems.getState() );
            return convertView;
        }
    }
}
