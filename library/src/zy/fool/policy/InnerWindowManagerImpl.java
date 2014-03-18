package zy.fool.policy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public final class InnerWindowManagerImpl extends InnerWindowManager{

	public InnerWindowManagerImpl(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void makeNewInnerWindow() {
		if(mInnerWindow==null)
		mInnerWindow = new InnerWindowImpl(mContext,mAttrs);
	}

}
