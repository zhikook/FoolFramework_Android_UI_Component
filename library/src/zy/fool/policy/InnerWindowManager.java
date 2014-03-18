package zy.fool.policy;

import android.content.Context;
import android.util.AttributeSet;

public abstract class InnerWindowManager implements IPolicy {
	protected static IInnerWin mInnerWindow = null;//InnerWindow暂时为LinearLayout ，后续再做更多组件的补充
	
	protected Context mContext;
	protected AttributeSet mAttrs;
	
	public InnerWindowManager(Context context,AttributeSet attrs) {
		mContext = context;
		mAttrs = attrs;
	}
	
	public IInnerWin getInnerWindow(){
		return mInnerWindow;
	}

}
