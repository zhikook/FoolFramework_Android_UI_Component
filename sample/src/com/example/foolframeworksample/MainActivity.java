package com.example.foolframeworksample;
import zy.fool.widget.FoolListView;


import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

//public class MainActivity extends Activity  implements FoolAdapterView.OnItemClickListener,OnSlideListener {
public class MainActivity extends Activity{

	private FoolListView mView ;
	private LittleFoolAdapter mFoolAdapter ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mView = (FoolListView)findViewById(R.id.foolview);
		
		mFoolAdapter = new LittleFoolAdapter(this);
		
		mView.setAdapter(mFoolAdapter);
		
		mView.setLongClickable(true);
		
		//mView.setOnSlideListener(this);
		//mView.setOnItemClickListener(this);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

//	@Override
//	public void onItemClick(FoolAdapterView<?> parent, View view, int position,	long id) {
//		if(view!=null)
//		this.setTitle(GENRES[position]);
//	}
//
//	@Override
//	public boolean onSlide(FoolAdapterView<?> parent, View view, int position,
//			long id) {
//		if(view!=null)
//			this.setTitle(GENRES[position]);
//		
//		return true;
//	}
}
