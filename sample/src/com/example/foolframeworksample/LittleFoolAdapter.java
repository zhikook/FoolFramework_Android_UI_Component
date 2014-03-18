package com.example.foolframeworksample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LittleFoolAdapter extends BaseAdapter{

	private LayoutInflater mInflater;
	
	private static final String[] GENRES = new String[] {
		 "[--A--]",
		 "[--B--]",
		 "[--C--]",
		 "[--D--]",
		 "[--E--]",
		 "[--F--]",
		 "[--G--]",
		 "[--H--]",
		 "[--I--]",
		 "[--J--]", 
		 "[--K--]", 
		 "[--L--]", 
		 "[--M--]", 
		 "[--N--]",
	};
	
	public LittleFoolAdapter(Context context){
		mInflater =LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 14;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		
		
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LinearLayout mView;
        
		if (convertView == null) {
            mView = (LinearLayout)mInflater.inflate(R.layout.m, parent, false);
        } else {
        	mView = (LinearLayout)convertView;
        }

		TextView tv = (TextView)mView.findViewById(R.id.textView1);
		tv.setText(GENRES[position]);
		
        return mView;
	}

}
