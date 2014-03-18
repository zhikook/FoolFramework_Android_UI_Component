package zy.fool.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class ItemInnerView extends ViewGroup{

	public ItemInnerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		canvas.drawColor(Color.BLACK);
		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		canvas.drawLine(20, 20,20, getHeight()-20, paint);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e){
		return true;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
	}
	
	//可写更多的内容，让APP复写

}
