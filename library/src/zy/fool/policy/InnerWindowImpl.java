package zy.fool.policy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import zy.fool.R;
import zy.fool.internal.InnerBaseWindow;
import zy.fool.widget.ItemInnerView;

public class InnerWindowImpl extends InnerBaseWindow<ItemInnerView>{
	LinearLayout.LayoutParams innerLayoutParams;
	int innerMeasureSpecWidth;
	int innerMeasureSpecWidthMode;
	int innerMeasureSpecHeight;
	int innerMeasureSpecHeightMode;
	
	LinearLayout innerLinearLayout;
	ItemInnerView mItemInnerView;
	
	LayoutInflater mInnerInflater;
	
	static OnSlideToRefreshListener<ItemInnerView> mSlideListener = new OnSlideToRefreshListener<ItemInnerView>() {

		@Override
		public void onSlideToRefresh(InnerBaseWindow<ItemInnerView> innerWin) {
			try {
				Thread.sleep(4000);
				System.out.println("--><-- onSlideToRefresh");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	public InnerWindowImpl(Context context) {
		super(context);
		init(context, null);
	}
	

	public InnerWindowImpl(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	void init(Context context, AttributeSet attrs){
		mInnerInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		innerLinearLayout = (LinearLayout) mInnerInflater.inflate(R.layout.innerwindow_layout, null);
		
		innerLayoutParams = (LinearLayout.LayoutParams) innerLinearLayout.getLayoutParams();
		innerMeasureSpecWidth = innerLinearLayout.getMeasuredWidth();
		innerMeasureSpecHeight = innerLinearLayout.getMeasuredWidth();
		
		//mItemInnerView = (ItemInnerView)innerLinearLayout.findViewById(R.id.innerView);
		
		//mView.layout(10, 10, 20, 30);
	}
	
//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		measure(innerMeasureSpecWidth, innerMeasureSpecHeight);
//	}
//	
	
	
	@Override
	public LinearLayout.LayoutParams getInnerLayoutParams(){
		return innerLayoutParams;
	}
	
	@Override
	protected ItemInnerView createRefreshableView(Context context, AttributeSet attrs) {
		mInnerInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mItemInnerView = (ItemInnerView)mInnerInflater.inflate(R.layout.itemview_layout, null);	
		return mItemInnerView;
	}

	@Override
	protected boolean isReadyForSlideRight() {
		return true;
	}

	@Override
	protected boolean isReadyForSlideLeft() {
		return true;
	}

	@Override
	public int getInnerMeasureSpecWidth() {
		return innerMeasureSpecWidth;
	}


	@Override
	public int getInnerMeasureSpecHeight() {
		return innerMeasureSpecHeight;
	}

	
}
