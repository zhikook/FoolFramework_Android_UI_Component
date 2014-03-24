/*******************************************************************************
 * Copyright 2013, 2014 David Lau.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package zy.fool.widget;

import java.util.ArrayList;

import zy.android.collect.Lists;
import zy.android.widget.Checkable;
import zy.android.widget.SpaceViewListAdapter;
import zy.fool.R;
import zy.fool.policy.IInnerWin;
import zy.fool.widget.AbsFoolView.LayoutParams;
import android.R.integer;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

public class FoolListView extends AbsFoolView {

	static final int NO_POSITION = -1;
	
	private boolean mAreAllItemsSelectable = true;

	private boolean mItemsCanFocus = false;
	
    Drawable mOverScrollHeader;
    Drawable mOverScrollFooter;

    private boolean mIsCacheColorOpaque;
    private boolean mDividerIsOpaque;
	
    Drawable mDivider;
    int mDividerHeight = 0;//test
    
    // used for temporary calculations.
    private final Rect mTempRect = new Rect();
    private Paint mDividerPaint;
  
    /**
     * A class that represents a fixed view in a list, for example a header at the top
     * or a footer at the bottom.
     */
    public class FixedViewInfo {
        /** The view to add to the list */
        public View view;
        /** The data backing the view. This is returned from {@link android.widget.ListAdapter#getItem(int)}. */
        public Object data;
        /** <code>true</code> if the fixed view should be selectable in the list */
        public boolean isSelectable;
    }
    
    private ArrayList<FixedViewInfo> mHeaderViewInfos = Lists.newArrayList();
    private ArrayList<FixedViewInfo> mFooterViewInfos = Lists.newArrayList();


	public FoolListView(Context context) {
		super(context);
	}

	public FoolListView(Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}
	
	public FoolListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.FoolListView);
		final Drawable d = a.getDrawable(R.styleable.FoolListView_divider);
    	if (d != null) {
		    // If a divider is specified use its intrinsic height for divider height
    		System.out.println("divider "+ d.toString());
	        setDivider(d);
	    }
    	
    	final int dividerHeight = a.getDimensionPixelSize(R.styleable.FoolListView_dividerHeight, 0);
	    if (dividerHeight != 0) {
	        setDividerHeight(dividerHeight);
	    }
	    
	    final int innerHeight = a.getDimensionPixelSize(R.styleable.FoolListView_innerWindowHeight, 0);
	    if (innerHeight != 0) {
	        setInnerWindowHeight(innerHeight);
	    }
	    
	    final int innerWidth = a.getDimensionPixelSize(R.styleable.FoolListView_innerWindowWidth, 0);
	    if (innerHeight != 0) {
	        setInnerWindowWidth(innerWidth);
	    }
	    
		a.recycle();
	}
    
	/*
     * (non-Javadoc)
     *
     * Children specified in XML are assumed to be header views. After we have
     * parsed them move them out of the children list and into mAbovePulledViews.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int count = getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; ++i) {
                addFooterView(getChildAt(i));
            }
            removeAllViews();
        }
    }

    @Override
	public void setAdapter(ListAdapter adapter) {
		// TODO Auto-generated method stub
		    
		if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver((DataSetObserver) mDataSetObserver);
        }
		
		resetList();
		mRecycler.clear();
		
		if (mHeaderViewInfos.size() > 0|| mFooterViewInfos.size() > 0) {
	         mAdapter = new SpaceViewListAdapter(mHeaderViewInfos, mFooterViewInfos, adapter);
	     } else {
	         mAdapter = adapter;
	    }
	
	    mOldSelectedPosition = INVALID_POSITION;
	    mOldSelectedRowId = INVALID_ROW_ID;
	    
		super.setAdapter(adapter);
		
		if (mAdapter != null) {
            mAreAllItemsSelectable = mAdapter.areAllItemsEnabled();
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
            checkFocus();

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            mRecycler.setViewTypeCount(mAdapter.getViewTypeCount());

            int position;
            if (mStackFromBottom) {
                position = lookForSelectablePosition(mItemCount - 1, false);
            } else {
                position = lookForSelectablePosition(0, true);
            }
            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);

            if (mItemCount == 0) {
                // Nothing selected
                checkSelectionChanged();
            }
        } 
		 
        requestLayout();
	}
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
    	
    	if (mCachingStarted) {
            mCachingActive = true;
        }

    	//Draw the dividers
    	final int dividerHeight = mDividerHeight;
    	final boolean drawDividers = dividerHeight >0 && mDivider!=null;
    	
    	if(drawDividers){
    		//only modify the top and bottom in the loop, we set the left and right there
    		final Rect bounds = mTempRect;
    		bounds.left = mPaddingLeft;
    		bounds.right = mRight -mLeft -mPaddingRight;
    		
    		final int count = getChildCount();
    		final int itemCount = mItemCount;
    		final int first = mFirstPosition;
    		final boolean areAllItemsSelectable = mAreAllItemsSelectable;
    		final ListAdapter adapter = mAdapter;
    		
    		//If the list is opaque *and* the background is not, we want to
    		//fill a rect where the dividers would be for non-selectable items
    		//If the list is opaque and the background is also opaque, we don't
    		//need to draw anything since the background will do it for us.
    		final boolean fillForMissingDividers = isOpaque()&&!super.isOpaque();
    	
    		if(fillForMissingDividers&&mDividerPaint == null&&mIsCacheColorOpaque){
    			mDividerPaint = new Paint();
    			mDividerPaint.setColor(getCacheColorHint());
    		}
    		final Paint paint = mDividerPaint;
    		
    		int effectivePaddingTop = 0;
    		int effectivePaddingBottom = 0;
    		if((mGroupFlags&CLIP_TO_PADDING_MASK)==CLIP_TO_PADDING_MASK){
    			effectivePaddingTop = mListPadding.top;
    			effectivePaddingBottom = mListPadding.bottom;
    		}
    		
    		final int listBottom = mBottom - mTop -effectivePaddingBottom +mScrollY;
    		
    		if(!mStackFromBottom){
    			int bottom = 0;
	
    			for(int i = 0;i<count;i++){
    				View childView = getChildAt(i);
    				bottom = childView.getBottom();
    					
    				bounds.top = bottom;
					bounds.bottom = bottom + dividerHeight;
					drawDivider(canvas, bounds, i);
				}

    		}else {
				int top;
	
				for(int i = 0;i<count;i++){
						View childView = getChildAt(i);
						top = childView.getTop();
						//Don't draw dividers next to items that are not enabled
						if(top>effectivePaddingTop){
							if(areAllItemsSelectable||(adapter.isEnabled(first+i)&&(i==count-1||adapter.isEnabled(first+i+1)))){
								System.out.println("top>effectivePaddingTop");
								
								bounds.top = top - dividerHeight;
								bounds.bottom = top;
								
								//Give the method the child Above the divider,so we
								//subtract one from our child position. Give -1 when there
								//is no child above the divider.
								drawDivider(canvas, bounds, i-1);
							}
							else if(fillForMissingDividers){
								bounds.top = top-dividerHeight;
								bounds.bottom = top;
								canvas.drawRect(bounds, paint);
							}
					}
				}
    		}
    	}
        // Draw the indicators (these should be drawn above the dividers) and children
        super.dispatchDraw(canvas);
    }

    
    
    @Override
	public boolean isOpaque() {
		boolean retValue = (mCachingActive && mIsCacheColorOpaque && mDividerIsOpaque ) || super.isOpaque();
	        //hasOpaqueScrollbars delete
	        
	    if (retValue) {
	            // only return true if the list items cover the entire area of the view
            final int listTop = mListPadding != null ? mListPadding.top : mPaddingTop;
            View first = getChildAt(0);
	        if (first == null || first.getTop() > listTop) {
	            return false;
	            }
	        
	        final int listBottom = getHeight() -
	               (mListPadding != null ? mListPadding.bottom : mPaddingBottom);
	        View last = getChildAt(getChildCount() - 1);
	        if (last == null || last.getBottom() < listBottom) {
	            return false;
            }
	     }
	     return retValue;
	}
    
    @TargetApi(19)
	@Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean more = super.drawChild(canvas, child, drawingTime);
        if (mCachingActive && !child.isDrawingCacheEnabled()) {
            mCachingActive = false;
        }    
        return more;
    }

    /**
     * Draws a divider for the given child in the given bounds.
     *
     * @param canvas The canvas to draw to.
     * @param bounds The bounds of the divider.
     * @param childIndex The index of child (of the View) above the divider.
     *            This will be -1 if there is no child above the divider to be
     *            drawn.
     */
    void drawDivider(Canvas canvas, Rect bounds, int childIndex) {
        // This widget draws the same divider for all children
        final Drawable divider = mDivider;
        divider.setBounds(bounds);
        divider.draw(canvas); 
    }

    /**
     * Returns the drawable that will be drawn between each item in the list.
     *
     * @return the current drawable drawn between list elements
     */
    public Drawable getDivider() {
        return mDivider;
    }

    /**
     * Sets the drawable that will be drawn between each item in the list. If the drawable does
     * not have an intrinsic height, you should also call {@link #setDividerHeight(int)}
     *
     * @param divider The drawable to use.
     */
    public void setDivider(Drawable divider) {
    	System.out.println("SetDivider");
    	
        if (divider != null) {
            mDividerHeight = divider.getIntrinsicHeight();
        } else {
            mDividerHeight = 0;
        }
        mDivider = divider;
        mDividerIsOpaque = divider == null || divider.getOpacity() == PixelFormat.OPAQUE;
        requestLayout();
        invalidate();
    }
    

    /**
     * @return Returns the height of the divider that will be drawn between each item in the list.
     */
    public int getDividerHeight() {
        return mDividerHeight;
    }
    
    /**
     * Sets the height of the divider that will be drawn between each item in the list. Calling
     * this will override the intrinsic height as set by {@link #setDivider(android.graphics.drawable.Drawable)}
     *
     * @param height The new height of the divider in pixels.
     */
    public void setDividerHeight(int height) {
        mDividerHeight = height;
        requestLayout();
        invalidate();
    }
    
    /**
     * 
     * Add a fixed view to appear at the top of the list. If addAbovePulledView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p>
     * NOTE: Call this before calling setAdapter. This is so ListView can wrap
     * the supplied cursor with one that will also account for header and footer
     * views.
     *
     * @param v The view to add.
     * @param data Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    public void addHeaderView(View v, Object data, boolean isSelectable) {

        if (mAdapter != null && ! (mAdapter instanceof SpaceViewListAdapter)) {
            throw new IllegalStateException(
                    "Cannot add header view to list -- setAdapter has already been called.");
        }

        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        mHeaderViewInfos.add(info);

        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (mAdapter != null && mDataSetObserver != null) {
            mDataSetObserver.onChanged();
        }
    }
     

    /**
     * Add a fixed view to appear at the top of the list. If addAbovePulledView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p>
     * NOTE: Call this before calling setAdapter. This is so ListView can wrap
     * the supplied cursor with one that will also account for header and footer
     * views.
     *
     * @param v The view to add.
     */
    public void addHeaderView(View v) {
    	addHeaderView(v, null, true);
    }

    @Override
    public int getHeaderViewCount() {
        return mHeaderViewInfos.size();
    }

    /**
     * Removes a previously-added header view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a header
     *         view
     */
    public boolean removeHeaderView(View v) {
        if (mHeaderViewInfos.size() > 0) {
            boolean result = false;
            if (mAdapter != null && ((SpaceViewListAdapter) mAdapter).removeAbover(v)) {
                if (mDataSetObserver != null) {
                    mDataSetObserver.onChanged();
                }
                result = true;
            }
            removeFixedViewInfo(v, mHeaderViewInfos);
            return result;
        }
        return false;
    }

    private void removeFixedViewInfo(View v, ArrayList<FixedViewInfo> where) {
        int len = where.size();
        for (int i = 0; i < len; ++i) {
            FixedViewInfo info = where.get(i);
            if (info.view == v) {
                where.remove(i);
                break;
            }
        }
    }

    /**
     * Add a fixed view to appear at the bottom of the list. If addBelowPulledView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p>
     * NOTE: Call this before calling setAdapter. This is so ListView can wrap
     * the supplied cursor with one that will also account for header and footer
     * views.
     *
     * @param v The view to add.
     * @param data Data to associate with this view
     * @param isSelectable true if the footer view can be selected
     */
    public void addFooterView(View v, Object data, boolean isSelectable) {

        // NOTE: do not enforce the adapter being null here, since unlike in
        // addAbovePulledView, it was never enforced here, and so existing apps are
        // relying on being able to add a footer and then calling setAdapter to
        // force creation of the AbovePulledViewListAdapter wrapper

        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        mFooterViewInfos.add(info);

        // in the case of re-adding a footer view, or adding one later on,
        // we need to notify the observer
        if (mAdapter != null && mDataSetObserver != null) {
            mDataSetObserver.onChanged();
        }
    }

    /**
     * Add a fixed view to appear at the bottom of the list. If addBelowPulledView is called more
     * than once, the views will appear in the order they were added. Views added using
     * this call can take focus if they want.
     * <p>NOTE: Call this before calling setAdapter. This is so ListView can wrap the supplied
     * cursor with one that will also account for header and footer views.
     *
     *
     * @param v The view to add.
     */
    public void addFooterView(View v) {
        addFooterView(v, null, true);
    }

    @Override
    public int getFooterViewsCount() {
        return mFooterViewInfos.size();
    }

    /**
     * Removes a previously-added footer view.
     *
     * @param v The view to remove
     * @return
     * true if the view was removed, false if the view was not a footer view
     */
    public boolean removeFooterView(View v) {
        if (mFooterViewInfos.size() > 0) {
            boolean result = false;
            if (mAdapter != null && ((SpaceViewListAdapter) mAdapter).removeBelower(v)) {
                if (mDataSetObserver != null) {
                    mDataSetObserver.onChanged();
                }
                result = true;
            }
            removeFixedViewInfo(v, mFooterViewInfos);
            return result;
        }
        return false;
    }

    //================================================================================================================
    
    protected void  addAboverPulledView(View v,int seltop) {	
    	addAboverPulledView(v, seltop,false);
	}
    
    protected void  addBelowerPulledView(View v,int seltop) {	
    	addBelowerPulledView(v, seltop,false);
	}
    
    
    /**
     * add abover
     * 
     * @param v The view to add.
     * @param data Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    protected void addAboverPulledView(View v, Object data, boolean isSelectable) {
        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        mAboverPulledViewInfos.add(info);
    }
    
    /**
     * Removes a previously-pulled abover view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a abover
     *         view
     */
    protected boolean removeAboverPulledView(View v) {
        if (mAboverPulledViewInfos.size() > 0) {
            boolean result = false;
            if (mAdapter != null && ((SpaceViewListAdapter) mAdapter).removeAbover(v)) {
                if (mDataSetObserver != null) {
                    mDataSetObserver.onChanged();
                }
                result = true;
            }
            removeFixedViewInfo(v, mAboverPulledViewInfos);
            return result;
        }
        return false;
    }

    protected void addAboverPulledView(View v) {
    	addAboverPulledView(v, null, true);
    }
  
    protected int getAboverPulledViewCount() {
        return mAboverPulledViewInfos.size();
    }
    
    /**
     * add belower
     * 
     * @param v The view to add.
     * @param data Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    protected void addBelowerPulledView(View v, Object data, boolean isSelectable) {
    	int pullPosition = mPulledPosistion;
        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        mBelowerPulledViewInfos.add(info);
    }
    
    /**
     * @param v The view to add.
     */
    public void addBelowerPulledView(View v) {
    	addBelowerPulledView(v, null, true);
    }

    public int getBelowerPulledViewsCount() {
        return mBelowerPulledViewInfos.size();
    }

    /**
     * Removes a previously-pulled belower view.
     *
     * @param v The view to remove
     * @return
     * true if the view was removed, false if the view was not a belower view
     */
    public boolean removeBelowerPulledView(View v) {
        if (mBelowerPulledViewInfos.size() > 0) {
            boolean result = false;
            if (mAdapter != null && ((SpaceViewListAdapter) mAdapter).removeBelower(v)) {
                if (mDataSetObserver != null) {
                    mDataSetObserver.onChanged();
                }
                result = true;
            }
            removeFixedViewInfo(v, mBelowerPulledViewInfos);
            return result;
        }
        return false;
    }

    //===========================================================================================================
    	
	@Override
	public ListAdapter getAdapter() {
		// TODO Auto-generated method stub
		return mAdapter;
	}
	
	@Override
	void resetList(){
		// The parent's resetList() will remove all views from the layout so we need to
        // cleanup the state of our footers and headers
        clearRecycledState(mHeaderViewInfos);
        clearRecycledState(mFooterViewInfos);

		super.resetList();
		if(mLayoutMode!=LAYOUT_SPOT)
		mLayoutMode = LAYOUT_PREVIEW;
	}
	
	private void clearRecycledState(ArrayList<FixedViewInfo> infos) {
        if (infos != null) {
            final int count = infos.size();

            for (int i = 0; i < count; i++) {
                final View child = infos.get(i).view;
                final LayoutParams p = (LayoutParams) child.getLayoutParams();
                if (p != null) {
                    p.recycledHeaderFooter = false;
                }
            }
        }
    }

	@Override
	public void setSelection(int position) {
		// TODO Auto-generated method stub
		setSelectionFromTop(position, 0);	
	}
	
	/**
     * Sets the selected item and positions the selection y pixels from the top edge
     * of the FoolListView. (If in touch mode, the item will not be selected but it will
     * still be positioned appropriately.)
     *
     * @param position Index (starting at 0) of the data item to be selected.
     * @param y The distance from the top edge of the FoolListView (plus padding) that the
     *        item will be positioned.
     */
    public void setSelectionFromTop(int position, int y) {
        if (mAdapter == null) {
            return;
        }

        if (!isInTouchMode()) {
            position = lookForSelectablePosition(position, true);
            if (position >= 0) {
                setNextSelectedPositionInt(position);
            }
        } else {
            mResurrectToPosition = position;
        }

        if (position >= 0) {
            mLayoutMode = LAYOUT_SPECIFIC;
            mSpecificTop = mListPadding.top + y;

            //Dont sync in 0.47
            if (mNeedSync) {
            	mSyncPosition = position;
            	mSyncRowId = mAdapter.getItemId(position);
            }

//            if (mPositionScroller != null) {
//                mPositionScroller.stop();
//            }
            requestLayout();
        }
    }
    
    /**
     * Makes the item at the supplied position selected.
     * 
     * @param position the position of the item to select
     */
    @Override
    void setSelectionInt(int position) {
        setNextSelectedPositionInt(position);
        final int selectedPosition = mSelectedPosition;

        if (selectedPosition >= 0) {
            if (position == selectedPosition - 1) {
               // awakeScrollbars = true;
            } else if (position == selectedPosition + 1) {
               // awakeScrollbars = true;
            }
        }

        //layoutChildren();
    }
    

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Sets up mListPadding
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int childWidth = 0;
        int childHeight = 0;
        int childState = 0;

        mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
        
        if (mItemCount > 0 && (widthMode == MeasureSpec.UNSPECIFIED ||heightMode == MeasureSpec.UNSPECIFIED)) {
            final View child = obtainMeasuredAndUnusedViewToUse(0, mIsMeasuredAndUnused);

            measureMeasuredAndUnusedChild(child, 0, widthMeasureSpec);

            childWidth = child.getMeasuredWidth();
            childHeight = child.getMeasuredHeight();
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            if (recycleOnMeasure() && mRecycler.shouldRecycleViewType(((LayoutParams) child.getLayoutParams()).viewType)) {
                mRecycler.addMeasuredAndUnusedView(child, -1);
            }
        }

        if (widthMode == MeasureSpec.UNSPECIFIED) {
        	//no scroll bar ,need to remove....
            widthSize = mListPadding.left + mListPadding.right + childWidth + getVerticalScrollbarWidth();
        } else {
            widthSize |= (childState&MEASURED_STATE_MASK);
        }

        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = mListPadding.top + mListPadding.bottom + childHeight + getVerticalFadingEdgeLength() * 2;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            // TODO: after first layout we should maybe start at the first visible position, not 0
            heightSize = measureHeightOfChildren(widthMeasureSpec, 0, NO_POSITION, heightSize, -1);
        }

        setMeasuredDimension(widthSize , heightSize);
        mWidthMeasureSpec = widthMeasureSpec;
    }
	
	/**
	 * 
	 * @param child
	 * @param position
	 * @param widthMeasureSpec
	 */
	private void measureMeasuredAndUnusedChild(View child, int position, int widthMeasureSpec) {
		// TODO Auto-generated method stub
		ViewGroup.LayoutParams pg =  (ViewGroup.LayoutParams) child.getLayoutParams();
		AbsFoolView.LayoutParams p =(AbsFoolView.LayoutParams)pg;
        if (p == null) {
            p = (AbsFoolView.LayoutParams) generateDefaultLayoutParams();
            child.setLayoutParams(p);
        }
        p.viewType = mAdapter.getItemViewType(position);
        p.forceAdd = true;

        int childWidthSpec = getChildMeasureSpec(widthMeasureSpec, mListPadding.left + mListPadding.right, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
	}
	
	/**
	 * 
	 * 							NO Focus function 
	 * 
	 ***********************************************************************************************
	 * When selection changes, it is possible that the previously selected or the
     * next selected item will change its size.  If so, we need to offset some folks,
     * and re-layout the items as appropriate.
     *
     * @param selectedView The currently selected view (before changing selection).
     *   should be <code>null</code> if there was no previous selection.
     * @param direction Either {@link android.view.View#FOCUS_UP} or
     *        {@link android.view.View#FOCUS_DOWN}.
     * @param newSelectedPosition The position of the next selection.
     * @param newFocusAssigned whether new focus was assigned.  This matters because
     *        when something has focus, we don't want to show selection (ugh).
     ***********************************************************************************************
     */
    private void handleNewSelectionChange(View selectedView, int direction, int newSelectedPosition,
            boolean newFocusAssigned) {
        if (newSelectedPosition == INVALID_POSITION) {
            throw new IllegalArgumentException("newSelectedPosition needs to be valid");
        }

        // whether or not we are moving down or up, we want to preserve the
        // top of whatever view is on top:
        // - moving down: the view that had selection
        // - moving up: the view that is getting selection
        View topView;
        View bottomView;
        int topViewIndex, bottomViewIndex;
        boolean topSelected = false;
        final int selectedIndex = mSelectedPosition - mFirstPosition;
        final int nextSelectedIndex = newSelectedPosition - mFirstPosition;
        if (direction == View.FOCUS_UP) {
            topViewIndex = nextSelectedIndex;
            bottomViewIndex = selectedIndex;
            topView = getChildAt(topViewIndex);
            bottomView = selectedView;
            topSelected = true;
        } else {
            topViewIndex = selectedIndex;
            bottomViewIndex = nextSelectedIndex;
            topView = selectedView;
            bottomView = getChildAt(bottomViewIndex);
        }

        final int numChildren = getChildCount();

        // start with top view: is it changing size?
        if (topView != null) {
            topView.setSelected(!newFocusAssigned && topSelected);
            measureAndAdjustDown(topView, topViewIndex, numChildren);
        }

        // is the bottom view changing size?
        if (bottomView != null) {
            bottomView.setSelected(!newFocusAssigned && !topSelected);
            measureAndAdjustDown(bottomView, bottomViewIndex, numChildren);
        }
    }

    /**
     * Re-measure a child, and if its height changes, lay it out preserving its
     * top, and adjust the children below it appropriately.
     * @param child The child
     * @param childIndex The view group index of the child.
     * @param numChildren The number of children in the view group.
     */
    private void measureAndAdjustDown(View child, int childIndex, int numChildren) {
        int oldHeight = child.getHeight();
        measureItem(child);
        if (child.getMeasuredHeight() != oldHeight) {
            // lay out the view, preserving its top
            relayoutMeasuredItem(child);

            // adjust views below appropriately
            final int heightDelta = child.getMeasuredHeight() - oldHeight;
            for (int i = childIndex + 1; i < numChildren; i++) {
                getChildAt(i).offsetTopAndBottom(heightDelta);
            }
        }
    }
    
	/**
     * Measure a particular list child.
     * TODO: unify with setUpChild.
     * @param child The child.
     */
    private void measureItem(View child) {
    	
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = getChildMeasureSpec(mWidthMeasureSpec,
                mListPadding.left + mListPadding.right, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }
    
    /**
     * Layout a child that has been measured, preserving its top position.
     * TODO: unify with setUpChild.
     * @param child The child.
     */
    private void relayoutMeasuredItem(View child) {
        final int w = child.getMeasuredWidth();
        final int h = child.getMeasuredHeight();
        final int childLeft = mListPadding.left;
        final int childRight = childLeft + w;
        final int childTop = child.getTop();
        final int childBottom = childTop + h;
        child.layout(childLeft, childTop, childRight, childBottom);
    }
    
    @Override
	protected void layoutChildren() {
		 final boolean blockLayoutRequests = mBlockLayoutRequests;
	        if (!blockLayoutRequests) {
	            mBlockLayoutRequests = true;
	        } else {
	            return;
	        }

	        try {
	            super.layoutChildren();

	            invalidate();

	            if (mAdapter == null) {
	                resetList();
	                //invokeOnItemScrollListener();
	                //invoke pull
	                //=================================================
	                
	                return;
	            }

	            int childrenTop = mListPadding.top;
	            int childrenBottom = mBottom - mTop - mListPadding.bottom;

	            int childCount = getChildCount();
	            int index = 0;
	            int delta = 0;

	            View sel= null;
	            View oldSel = null;
	            View oldFirst = null;
	            View newSel = null;
          
	            // Remember stuff we will need down below
	            switch (mLayoutMode) {
	            case LAYOUT_SET_SELECTION:
	                index = mNextSelectedPosition - mFirstPosition;
	                if (index >= 0 && index < childCount) {
	                    newSel = getChildAt(index);
	                }
	                break;
	            case LAYOUT_FORCE_TOP:
	            case LAYOUT_FORCE_BOTTOM:
	            case LAYOUT_SYNC:
	            case LAYOUT_PULLING:
	            	break;
	            case LAYOUT_SPOT:
	            case LAYOUT_SLIDE:
	                break;
	            case LAYOUT_MOVE_SELECTION:
	 	        default:
	                // Remember the previously selected view
	 	            index = mSelectedPosition - mFirstPosition;
	                
	                if (index >= 0 && index < childCount) {
	                    oldSel = getChildAt(index);
	                }
	                // Remember the previous first child
	                oldFirst = getChildAt(0);

	                if (mNextSelectedPosition >= 0) {
	                    delta = mNextSelectedPosition - mSelectedPosition;	                  
	                }
	                // Caution: newSel might be null
	                newSel = getChildAt(index + delta);
	            }
            
	            boolean dataChanged = mDataChanged;
	            if (dataChanged) {
	                handleDataChanged();
	            }

	            // Handle the empty set by removing all views that are visible
	            // and calling it a day
	            if (mItemCount == 0) {
	                resetList();
	                //invokeOnItemScrollListener();                
	                return;
	            } else if (mItemCount != mAdapter.getCount()) {
	                throw new IllegalStateException("The content of the adapter has changed but "
	                        + "FoolListView did not receive a notification. Make sure the content of "
	                        + "your adapter is not modified from a background thread, but only "
	                        + "from the UI thread. [in FoolListView(" + getId() + ", " + getClass() 
	                        + ") with Adapter(" + mAdapter.getClass() + ")]");
	            }

	            //setSelectedPositionInt(mNextSelectedPosition);

	            // Pull all children into the RecycleBin.These views will be reused if possible
	            final int firstPosition = mFirstPosition;
	            final RecycleBin recycleBin = mRecycler;

	            // Don't put Abover or belower views into the Recycler. Those are
	            // already cached in mAbovePulledViews;	            
	           
	            if (dataChanged) {
	                for (int i = 0; i < childCount; i++) {
	                    recycleBin.addMeasuredAndUnusedView(getChildAt(i), firstPosition+i);
	                }
	            } else {
	                recycleBin.fillActiveViews(childCount, firstPosition);
	            }

	            // Clear out old views
	            detachAllViewsFromParent();
	            recycleBin.removeSkippedMeasuredAndUnused();
	            
	            switch (mLayoutMode) {
	            	case LAYOUT_SET_SELECTION:
		                if (newSel != null) {
		                    sel = fillFromSelection(newSel.getTop(), childrenTop, childrenBottom);
		                } else {
		                    sel = fillFromMiddle(childrenTop, childrenBottom);
		                }
		                break;
		            case LAYOUT_PULLING:
		            	//hideSelector();
		            	
		            	break;
		            case LAYOUT_SPOT:
		            	sel = fillSwapDown(mFirstPosition, childrenTop, childCount);
		            	break;
		            	
		            case LAYOUT_SLIDE:
		            	sel = fillAfterSlide(mFirstPosition,childrenTop);
		            	break;
		            	
		            default:
		                if (childCount == 0) {
		                    if (!mStackFromBottom) {
		                        final int position = lookForSelectablePosition(0, true);
		                        setSelectedPositionInt(position);
		                        sel = fillFromTop(childrenTop);
		                    } else {
		                        final int position = lookForSelectablePosition(mItemCount - 1, false);
		                        setSelectedPositionInt(position);
		                        sel = fillUp(mItemCount - 1, childrenBottom);
		                    }
		                } else {
		                	if (mSelectedPosition >= 0 && mSelectedPosition < mItemCount) {
		                		sel = startfill(mSelectedPosition,oldSel == null ? childrenTop : oldSel.getTop());
		                    } else if (mFirstPosition < mItemCount) {
		                    	sel = startfill(mFirstPosition,oldFirst == null ? childrenTop : oldFirst.getTop());
		                    } else {
		                    	sel = startfill(0, childrenTop);
		                    }
		                }
		                break;
	            }

	            // Flush any cached views that did not get reused above
	            recycleBin.moveMeasuredAndUnusedToActiveViews();

	            if (sel != null) {
	                positionSelector(INVALID_POSITION, sel);
	                mSelectedTop = sel.getTop();
	            } else {
	                if (mTouchMode > TOUCH_MODE_DOWN && mTouchMode < TOUCH_MODE_PULL) {
	                    View child = getChildAt(mMotionPosition - mFirstPosition);
	                    if (child != null) positionSelector(mMotionPosition, child);
	                } else {
	                    mSelectedTop = 0;
	                    mSelectorRect.setEmpty();
	                }
	            }

	            mLayoutMode = LAYOUT_PREVIEW;
	            mDataChanged = false;
	            
	            mNeedSync = false;
	            setNextSelectedPositionInt(mSelectedPosition);

	            if (mItemCount > 0) {
	                checkSelectionChanged();
	            }
	            //invokeOnItemScrollListener();
	        } finally {
	            if (!blockLayoutRequests) {
	                mBlockLayoutRequests = false;
	            }
	        }
	}
    
    /**
     * @param child a direct child of this list.
     * @return Whether child is a Abover or Belower view.
     * 
     */
    
    private boolean isDirectChildAboverOrBelower(View child) {

        final ArrayList<FixedViewInfo> abovers = mHeaderViewInfos;
        final int numAbovers = abovers.size();
        for (int i = 0; i < numAbovers; i++) {
            if (child == abovers.get(i).view) {
                return true;
            }
        }
        
        final ArrayList<FixedViewInfo> belowers = mFooterViewInfos;
        final int numBelowers = belowers.size();
        for (int i = 0; i < numBelowers; i++) {
            if (child == belowers.get(i).view) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtain the view and add it to our list of children. The view can be made
     * fresh, converted from an unused view, or used as is if it was in the
     * recycle bin.
     *
     * @param position Logical position in the list
     * @param y Top or bottom edge of the view to add
     * @param flow If flow is true, align top edge to y. If false, align bottom
     *        edge to y.
     * @param childrenLeft Left edge where children should be positioned
     * @param selected Is this position selected?
     * @return View that was added
     */
    private View makeAndAddView(int position, int y, boolean flow, int childrenLeft,
            boolean selected) {
    	
    	 View child;

         if (!mDataChanged) {
             // Try to use an existing view for this position
             child = mRecycler.getActiveView(position);
             if (child != null) {
                 // Found it -- we're using an existing child
                 // This just needs to be positioned
                 setupChild(child, position, y, flow, childrenLeft, selected, true);
                 return child;
             }
         }

         // Make a new view for this position, or convert an unused view if possible
         child = obtainMeasuredAndUnusedViewToUse(position, mIsMeasuredAndUnused);

         // This needs to be positioned and measured
         setupChild(child, position, y, flow, childrenLeft, selected, mIsMeasuredAndUnused[0]);

         return child;
    }
    
    /**
     * Obtain the view and add it to our list of children. The view can be made
     * fresh, converted from an unused view, or used as is if it was in the
     * recycle bin.
     *
     * @param position Logical position in the list
     * @param swapposition
     * @param y Top or bottom edge of the view to add
     * @param flow If flow is true, align top edge to y. If false, align bottom
     *        edge to y.
     * @param childrenLeft Left edge where children should be positioned
     * @param selected Is this position selected?
     * @return View that was added
     */
    private View makeAndAddSpecialView(int position,int swapposition,int y, boolean flow, int childrenLeft,
            boolean selected,boolean swaped) {
    	
    	 View child;

         if (!mDataChanged) {
             // Try to use an existing view for this position
        	 
        	 if(swapposition>0){
        		 child = mRecycler.getActiveView(swapposition);
        		 if (child != null) {
                     // Found it -- we're using an existing child
                     // This just needs to be positioned
        			 
                     setupChild(child, position, y, flow, childrenLeft, selected, true);
                     return child;
                 }
                  
//        		 // Make a new view for this position, or convert an unused view if possible
//                 child = obtainMeasuredAndUnusedViewToUse(position, mIsMeasuredAndUnused);
//
//                 // This needs to be positioned and measured
//                 setupChild(child, position, y, flow, childrenLeft, selected, mIsMeasuredAndUnused[0]);

        	 }else {
        		 child = mRecycler.getActiveView(position);
        		 if (child != null) {
                     // Found it -- we're using an existing child
                     // This just needs to be positioned
                    setupChild(child, position, y, flow, childrenLeft, selected, true);
                    return child;
                 }     		 
			}
            
         }
         
         if(swapposition>0){
        	    // Make a new view for this position, or convert an unused view if possible
             child = obtainMeasuredAndUnusedViewToUse(swapposition, mIsMeasuredAndUnused);

             // This needs to be positioned and measured
             setupChild(child, position, y, flow, childrenLeft, selected, mIsMeasuredAndUnused[0]);
             return child;

         }else {
        	   // Make a new view for this position, or convert an unused view if possible
             child = obtainMeasuredAndUnusedViewToUse(position, mIsMeasuredAndUnused);

             // This needs to be positioned and measured
             setupChild(child, position, y, flow, childrenLeft, selected, mIsMeasuredAndUnused[0]);
             return child;

		}
    }
    
    /**
     * Obtain the view and add it to our list of children. The view can be made
     * fresh, converted from an unused view, or used as is if it was in the
     * recycle bin.
     *
     * @param position Logical position in the list
     * @param y Top or bottom edge of the view to add
     * @param flow If flow is true, align top edge to y. If false, align bottom
     *        edge to y.
     * @param childrenLeft Left edge where children should be positioned
     * @param selected Is this position selected?
     * @return View that was added
     */
    private View makeAndMoveView(int position, int y, boolean flow, int childrenLeft,
            boolean selected) {
    	
    	 View child;

         if (!mDataChanged) {
             // Try to use an existing view for this position
             child = mRecycler.getActiveView(position);
             if (child != null) {
                 // Found it -- we're using an existing child
                 // This just needs to be positioned
                 setupChild(child, position, y, flow, childrenLeft, selected, true);
                 return child;
             }
         }

         // Make a new view for this position, or convert an unused view if possible
         child = obtainMeasuredAndUnusedViewToUse(position, mIsMeasuredAndUnused);

         // This needs to be positioned and measured
         setupChild(child, position, y, flow, childrenLeft, selected, mIsMeasuredAndUnused[0]);

         return child;
    }
    /**
     * Add a view as a child and make sure it is measured (if necessary) and
     * positioned properly.
     *
     * @param child The view to add
     * @param position The position of this child
     * @param y The y position relative to which this view will be positioned
     * @param flowDown If true, align top edge to y. If false, align bottom
     *        edge to y.
     * @param childrenLeft Left edge where children should be positioned
     * @param selected Is this position selected?
     * @param recycled Has this view been pulled from the recycle bin? If so it
     *        does not need to be remeasured.
     */
    private void setupChild(View child, int position, int y, boolean flowDown, int childrenLeft,boolean selected, boolean recycled) {
    	final boolean isSelected = selected && shouldShowSelector();
        final boolean updateChildSelected = isSelected != child.isSelected();
        final int mode = mTouchMode;
        final boolean isPressed = mode > TOUCH_MODE_DOWN && mode < TOUCH_MODE_PULL && mMotionPosition == position;
        final boolean updateChildPressed = isPressed != child.isPressed();
        final boolean needToMeasureAndLayout = !recycled || updateChildSelected || child.isLayoutRequested();

        // Respect layout params that are already in the view. Otherwise make some up...
        // noinspection unchecked
        
        AbsFoolView.LayoutParams p = (AbsFoolView.LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (AbsFoolView.LayoutParams) generateDefaultLayoutParams();
        }
        p.viewType = mAdapter.getItemViewType(position);

        if ((recycled && !p.forceAdd) || (p.recycledHeaderFooter && p.viewType == FoolAdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER)) {
            attachViewToParent(child, flowDown ? -1 : 0, p);
        } else {
            p.forceAdd = false;
            if (p.viewType == FoolAdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                p.recycledHeaderFooter = true;
            }
            addViewInLayout(child, flowDown ? -1 : 0, p, true);
        }

        if (updateChildSelected) {
            child.setSelected(isSelected);
        }

        if (updateChildPressed) {
            child.setPressed(isPressed);
        }

        //used in select and pull 
        if ( mCheckStates != null) {
            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(mCheckStates.get(position));
            } else if (getContext().getApplicationInfo().targetSdkVersion
                    >= Build.VERSION_CODES.HONEYCOMB) {
                child.setActivated(mCheckStates.get(position));
            }
        }
        
        if (needToMeasureAndLayout) {
        	int childWidthSpec = getChildMeasureSpec(mWidthMeasureSpec,mListPadding.left + mListPadding.right, p.width);
            int lpHeight = p.height;
            int childHeightSpec;
            if (lpHeight > 0) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            
            child.measure(childWidthSpec, childHeightSpec);
            
        } else {
            cleanupLayoutState(child);
        }

        final int w = child.getMeasuredWidth();
        final int h = child.getMeasuredHeight();
        final int childTop = flowDown ? y : y - h;

        if (needToMeasureAndLayout) {
            final int childRight = childrenLeft + w;
            final int childBottom = childTop + h;
            child.layout(childrenLeft, childTop, childRight, childBottom);
        } else {
            child.offsetLeftAndRight(childrenLeft - child.getLeft());
            child.offsetTopAndBottom(childTop - child.getTop());
        }

        if (mCachingStarted && !child.isDrawingCacheEnabled()) {
            child.setDrawingCacheEnabled(true);
        }

        if (recycled && (((AbsFoolView.LayoutParams)child.getLayoutParams()).measuredAndUnusedFromPosition)!= position) {
            child.jumpDrawablesToCurrentState();
        }
        
    }
    
    protected boolean recycleOnMeasure() {
        return true;
    }
	
	/**
	 * Measures the height of the given range of children (inclusive) and
     * returns the height with this FoolListView's padding and divider heights
     * included. If maxHeight is provided, the measuring will stop when the
     * current height reaches maxHeight.
     *
     * @param widthMeasureSpec The width measure spec to be given to a child's
     *            {@link android.view.View#measure(int, int)}.
     * @param startPosition The position of the first child to be shown.
     * @param endPosition The (inclusive) position of the last child to be
     *            shown. Specify {@link #NO_POSITION} if the last child should be
     *            the last available child from the adapter.
     * @param maxHeight The maximum height that will be returned (if all the
     *            children don't fit in this value, this value will be
     *            returned).
     * @param disallowPartialChildPosition In general, whether the returned
     *            height should only contain entire children. This is more
     *            powerful--it is the first inclusive position at which partial
     *            children will not be allowed. Example: it looks nice to have
     *            at least 3 completely visible children, and in portrait this
     *            will most likely fit; but in landscape there could be times
     *            when even 2 children can not be completely shown, so a value
     *            of 2 (remember, inclusive) would be good (assuming
     *            startPosition is 0).
     * @return The height of this FoolListView with the given children.
     */
    final int measureHeightOfChildren(int widthMeasureSpec, int startPosition, int endPosition,
            final int maxHeight, int disallowPartialChildPosition) {

        final ListAdapter adapter = mAdapter;
        if (adapter == null) 
            return mListPadding.top + mListPadding.bottom;
        
        // Include the padding of the list
        int returnedHeight = mListPadding.top + mListPadding.bottom;
        final int dividerHeight = ((mDividerHeight > 0) && mDivider != null) ? mDividerHeight : 0;
        
        // The previous height value that was less than maxHeight and contained
        // no partial children
        int prevHeightWithoutPartialChild = 0;
        int i;
        View child;

        // mItemCount - 1 since endPosition parameter is inclusive
        endPosition = (endPosition == NO_POSITION) ? adapter.getCount() - 1 : endPosition;
        final AbsFoolView.RecycleBin recycleBin = mRecycler;
        final boolean recyle = recycleOnMeasure();
        final boolean[] isMeasuredAndUnused = mIsMeasuredAndUnused;

        for (i = startPosition; i <= endPosition; ++i) {
            child = obtainMeasuredAndUnusedViewToUse(i, isMeasuredAndUnused);

            measureMeasuredAndUnusedChild(child, i, widthMeasureSpec);

            if (i > 0) {
             	//Count the divider for all but one child
             	returnedHeight += dividerHeight;
             }

            // Recycle the view before we possibly return from the method
            if (recyle && recycleBin.shouldRecycleViewType(
                    ((LayoutParams) child.getLayoutParams()).viewType)) {
                recycleBin.addMeasuredAndUnusedView(child, -1);
            }

            returnedHeight += child.getMeasuredHeight();
            
            if (returnedHeight >= maxHeight) {
                // We went over, figure out which height to return.  If returnedHeight > maxHeight,
                // then the i'th position did not fit completely.
                return (disallowPartialChildPosition >= 0) // Disallowing is enabled (> -1)
                            && (i > disallowPartialChildPosition) // We've past the min pos
                            && (prevHeightWithoutPartialChild > 0) // We have a prev height
                            && (returnedHeight != maxHeight) // i'th child did not fit completely
                        ? prevHeightWithoutPartialChild
                        : maxHeight;
            }

            if ((disallowPartialChildPosition >= 0) && (i >= disallowPartialChildPosition)) {
                prevHeightWithoutPartialChild = returnedHeight;
            }
        }
        
        // At this point, we went through the range of children, and they each
        // completely fit, so return the returnedHeight
        return returnedHeight;
        
    }

    /**
     * Fills the list from top to bottom, starting with mFirstPosition
     *
     * @param nextTop The location where the top of the first item should be
     *        drawn
     *
     * @return The view that is currently selected
     */
    private View fillFromTop(int nextTop) {
        mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
        mFirstPosition = Math.min(mFirstPosition, mItemCount - 1);
        if (mFirstPosition < 0) {
            mFirstPosition = 0;
        }
        return fillDown(mFirstPosition, nextTop);
    }
    
    /**
     * Check if we have dragged the bottom of the list too high (we have pushed the
     * top element off the top of the screen when we did not need to). Correct by sliding
     * everything back down.
     *
     * @param childCount Number of children
     */
    private void correctTooHigh(int childCount) {
        // First see if the last item is visible. If it is not, it is OK for the
        // top of the list to be pushed up.
        int lastPosition = mFirstPosition + childCount - 1;
        if (lastPosition == mItemCount - 1 && childCount > 0) {

            // Get the last child ...
            final View lastChild = getChildAt(childCount - 1);

            // ... and its bottom edge
            final int lastBottom = lastChild.getBottom();

            // This is bottom of our drawable area
            final int end = (mBottom - mTop) - mListPadding.bottom;

            // This is how far the bottom edge of the last view is from the bottom of the
            // drawable area
            int bottomOffset = end - lastBottom;
            View firstChild = getChildAt(0);
            final int firstTop = firstChild.getTop();

            // Make sure we are 1) Too high, and 2) Either there are more rows above the
            // first row or the first row is scrolled off the top of the drawable area
            if (bottomOffset > 0 && (mFirstPosition > 0 || firstTop < mListPadding.top))  {
                if (mFirstPosition == 0) {
                    // Don't pull the top too far down
                    bottomOffset = Math.min(bottomOffset, mListPadding.top - firstTop);
                }
                
                // Move everything down
                //offsetChildrenTopAndBottom(bottomOffset);
                offsetTopAndBottom(bottomOffset);
                
                if (mFirstPosition > 0) {
                    // Fill the gap that was opened above mFirstPosition with more rows, if
                    // possible
                    fillUp(mFirstPosition - 1, firstChild.getTop() - mDividerHeight);
                    // Close up the remaining gap
                    adjustViewsUpOrDown();
                }

            }
        }
    }

    /**
     * Check if we have dragged the bottom of the list too low (we have pushed the
     * bottom element off the bottom of the screen when we did not need to). Correct by sliding
     * everything back up.
     *
     * @param childCount Number of children
     */
    private void correctTooLow(int childCount) {
        // First see if the first item is visible. If it is not, it is OK for the
        // bottom of the list to be pushed down.
        if (mFirstPosition == 0 && childCount > 0) {

            // Get the first child ...
            final View firstChild = getChildAt(0);

            // ... and its top edge
            final int firstTop = firstChild.getTop();

            // This is top of our drawable area
            final int start = mListPadding.top;

            // This is bottom of our drawable area
            final int end = (mBottom - mTop) - mListPadding.bottom;

            // This is how far the top edge of the first view is from the top of the
            // drawable area
            int topOffset = firstTop - start;
            View lastChild = getChildAt(childCount - 1);
            final int lastBottom = lastChild.getBottom();
            int lastPosition = mFirstPosition + childCount - 1;

            // Make sure we are 1) Too low, and 2) Either there are more rows below the
            // last row or the last row is scrolled off the bottom of the drawable area
            if (topOffset > 0) {
                if (lastPosition < mItemCount - 1 || lastBottom > end)  {
                    if (lastPosition == mItemCount - 1) {
                        // Don't pull the bottom too far up
                        topOffset = Math.min(topOffset, lastBottom - end);
                    }
                    // Move everything up
                    offsetTopAndBottom(-topOffset);
                    if (lastPosition < mItemCount - 1) {
                        // Fill the gap that was opened below the last position with more rows, if
                        // possible
                        fillDown(lastPosition + 1, lastChild.getBottom() + mDividerHeight);
                        // Close up the remaining gap
                        adjustViewsUpOrDown();
                    }
                } else if (lastPosition == mItemCount - 1) {
                    adjustViewsUpOrDown();                    
                }
            }
        }
    }
    /**
     * Fills the list from pos down to the end of the list view.
     *
     * @param pos The first position to put in the list
     *
     * @param nextTop The location where the top of the item associated with pos
     *        should be drawn
     *
     * @return The view that is currently selected, if it happens to be in the
     *         range that we draw.
     */
    private View fillDown(int pos, int nextTop) {
    	View selectedView = null;
        int end = getBottom() - getTop();        
        if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            end -= mListPadding.bottom;
        }
        while (nextTop < end && pos < mItemCount) {
            // is this the selected item?
        	
        	boolean selected = pos == mSelectedPosition;
            View child = makeAndAddView(pos, nextTop, true, mListPadding.left, selected);
            nextTop = child.getBottom() + mDividerHeight;
            
            if (selected) {
                selectedView = child;
            }
            pos++;
        }

        //setVisibleRangeHint(mFirstPosition, mFirstPosition + getChildCount() - 1);
        
        return selectedView;
    }
    
    /**
     * Put a specific item at a specific location on the screen and then build
     * up and down from there.
     *
     * @param position The reference view to use as the starting point
     * @param top Pixel offset from the top of this view to the top of the
     *        reference view.
     *
     * @return The selected view, or null if the selected view is outside the
     *         visible area.
     */
    private View startfill(int position, int top) {
        boolean tempIsSelected = position == mSelectedPosition;
        View temp = makeAndAddView(position, top, true, mListPadding.left, tempIsSelected);
        // Possibly changed again in fillUp if we add rows above this one.
        mFirstPosition = position;
        
        View above;
        View below;

        final int dividerHeight = mDividerHeight;
        
        if (!mStackFromBottom) {
        	above = fillUp(position - 1, temp.getTop() - dividerHeight);
            // This will correct for the top of the first view not touching the top of the list
            adjustViewsBothUpAndDown();//need to change the table ,maybe use scale
            below = fillDown(position + 1, temp.getBottom() + dividerHeight);
            int childCount = getChildCount();
            
            if (childCount > 0) {
            	correctTooHigh(childCount);
            }
        } else {
            below = fillDown(position + 1, temp.getBottom() + dividerHeight);
            // This will correct for the bottom of the last view not touching the bottom of the list
            adjustViewsBothUpAndDown();
            above = fillUp(position - 1, temp.getTop() - dividerHeight);
            int childCount = getChildCount();
            
            if (childCount > 0) {
            	correctTooLow(childCount);
            }
        }

        if (tempIsSelected) {
            return temp;
        } else if (above != null) {
            return above;
        } else {
            return below;
        }
    }
    
    private View fillSlide(int pos, int nextTop,int delta) {
    	
    	final int slidedPosition = mSlidedPosition;
    	int mlistLeft = mListPadding.left +delta;        
        View slidedView = null;
        int end = (getBottom() - getTop());
        
        if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            end -= mListPadding.bottom;
        } 
        
        while (nextTop < end && pos < mItemCount) {
            // is this the selected item?
        	boolean slided = pos == slidedPosition;
            if(slided){
            	mlistLeft = mListPadding.left +delta;
            }else {
            	mlistLeft = mListPadding.left;
			}
            
            View child = makeAndAddView(pos, nextTop, true, mlistLeft, slided);
            nextTop = child.getBottom() + mDividerHeight;
            if (slided) {
                slidedView = child;
            }
            pos++;
        }

        //setVisibleRangeHint(mFirstPosition, mFirstPosition + getChildCount() - 1);
        
        return slidedView;
    }
    

    //����������̴߳���������
    
  	private View fillAfterSlide(int pos,int nextTop) {
//  		if (mFirstPosition < 0) {
//  			mFirstPosition = 0;
//        }      
  		int delta = incrementalDeltaX;	
  		return fillSlide(pos, nextTop,delta);
  	}
  
    /**
     * Fills the list from pos up to the top of the list view.
     *
     * @param pos The first position to put in the list
     *
     * @param nextBottom The location where the bottom of the item associated
     *        with pos should be drawn
     *
     * @return The view that is currently selected
     */
    private View fillUp(int pos, int nextBottom) {
    	View selectedView = null;

        int end = 0;
        if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            end = mListPadding.top;
        }

        while (nextBottom > end && pos >= 0) {
            // is this the selected item?
            boolean selected = pos == mSelectedPosition;
            View child = makeAndAddView(pos, nextBottom, false, mListPadding.left, selected);
            nextBottom = child.getTop() - mDividerHeight;
            if (selected) {
                selectedView = child;
            }
            pos--;
        }

        mFirstPosition = pos + 1;
        
        //setVisibleRangeHint(mFirstPosition, mFirstPosition + getChildCount() - 1);
        return selectedView;
    }

    /**
     * Put mSelectedPosition in the middle of the screen and then build up and
     * down from there. This method forces mSelectedPosition to the center.
     *
     * @param childrenTop Top of the area in which children can be drawn, as
     *        measured in pixels
     * @param childrenBottom Bottom of the area in which children can be drawn,
     *        as measured in pixels
     * @return Currently selected view
     */
    private View fillFromMiddle(int childrenTop, int childrenBottom) {
        int height = childrenBottom - childrenTop;

        int position = reconcileSelectedPosition();

        View sel = makeAndAddView(position, childrenTop, true,
                mListPadding.left, true);
        mFirstPosition = position;

        int selHeight = sel.getMeasuredHeight();
        if (selHeight <= height) {
            sel.offsetTopAndBottom((height - selHeight) / 2);
        }

        fillAboveAndBelow(sel, position);

        if (!mStackFromBottom) {
            correctTooHigh(getChildCount());
        } else {
            correctTooLow(getChildCount());
        }

        return sel;
    }
    
    /**
     * Once the selected view as been placed, fill up the visible area above and
     * below it.
     *
     * @param sel The selected view
     * @param position The position corresponding to sel
     */
    private void fillAboveAndBelow(View sel, int position) {
        final int dividerHeight = mDividerHeight;
        if (!mStackFromBottom) {
            fillUp(position - 1, sel.getTop() - dividerHeight);
            adjustViewsUpOrDown();
            fillDown(position + 1, sel.getBottom() + dividerHeight);
        } else {
            fillDown(position + 1, sel.getBottom() + dividerHeight);
            adjustViewsUpOrDown();
            fillUp(position - 1, sel.getTop() - dividerHeight);
        }
    }
    
    /**
     * Fills the grid based on positioning the new selection at a specific
     * location. The selection may be moved so that it does not intersect the
     * faded edges. The grid is then filled upwards and downwards from there.
     *
     * @param selectedTop Where the selected item should be
     * @param childrenTop Where to start drawing children
     * @param childrenBottom Last pixel where children can be drawn
     * @return The view that currently has selection
     */
    private View fillFromSelection(int selectedTop, int childrenTop, int childrenBottom) {
        int fadingEdgeLength = getVerticalFadingEdgeLength();
        final int selectedPosition = mSelectedPosition;

        View sel;

        final int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength,
                selectedPosition);
        final int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength,
                selectedPosition);

        sel = makeAndAddView(selectedPosition, selectedTop, true, mListPadding.left, true);


        // Some of the newly selected item extends below the bottom of the list
        if (sel.getBottom() > bottomSelectionPixel) {
            // Find space available above the selection into which we can scroll
            // upwards
            final int spaceAbove = sel.getTop() - topSelectionPixel;

            // Find space required to bring the bottom of the selected item
            // fully into view
            final int spaceBelow = sel.getBottom() - bottomSelectionPixel;
            final int offset = Math.min(spaceAbove, spaceBelow);

            // Now offset the selected item to get it into view
            sel.offsetTopAndBottom(-offset);
        } else if (sel.getTop() < topSelectionPixel) {
            // Find space required to bring the top of the selected item fully
            // into view
            final int spaceAbove = topSelectionPixel - sel.getTop();

            // Find space available below the selection into which we can scroll
            // downwards
            final int spaceBelow = bottomSelectionPixel - sel.getBottom();
            final int offset = Math.min(spaceAbove, spaceBelow);

            // Offset the selected item to get it into view
            sel.offsetTopAndBottom(offset);
        }

        // Fill in views above and below
        fillAboveAndBelow(sel, selectedPosition);

        if (!mStackFromBottom) {
            correctTooHigh(getChildCount());
        } else {
            correctTooLow(getChildCount());
        }

        return sel;
    }
  
    /**
     * Calculate the bottom-most pixel we can draw the selection into
     *
     * @param childrenBottom Bottom pixel were children can be drawn
     * @param fadingEdgeLength Length of the fading edge in pixels, if present
     * @param selectedPosition The position that will be selected
     * @return The bottom-most pixel we can draw the selection into
     */
    private int getBottomSelectionPixel(int childrenBottom, int fadingEdgeLength,
            int selectedPosition) {
        int bottomSelectionPixel = childrenBottom;
        if (selectedPosition != mItemCount - 1) {
            bottomSelectionPixel -= fadingEdgeLength;
        }
        return bottomSelectionPixel;
    }

    /**
     * Calculate the top-most pixel we can draw the selection into
     *
     * @param childrenTop Top pixel were children can be drawn
     * @param fadingEdgeLength Length of the fading edge in pixels, if present
     * @param selectedPosition The position that will be selected
     * @return The top-most pixel we can draw the selection into
     */
    private int getTopSelectionPixel(int childrenTop, int fadingEdgeLength, int selectedPosition) {
        // first pixel we can draw the selection into
        int topSelectionPixel = childrenTop;
        if (selectedPosition > 0) {
            topSelectionPixel += fadingEdgeLength;
        }
        return topSelectionPixel;
    }

    private View fillSwapDown(int pos, int nextTop,int childCount) {
    	
    	View temp = null;
    	int m = 0;
    	
    	mSwapPosition= (mFirstPosition + childCount-1);
    	
    	if(mCheckedItemCount == 0 ){
    		for(int i =mFirstPosition;i<mFirstPosition+childCount-1;i++){
    			mItemSpotStates.put(i, true);
    		}
    	}
    	
    	while(isCheckSpottedPosition(mSwapPosition)&&mSwapPosition < (mItemCount-1)){
    		mSwapPosition = mSwapPosition + 1;  
    	}
			 	
//		int end = (mBottom - mTop);
//		
//		if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
//		    	end -= mListPadding.bottom;
//		} 
		    
		while (m < childCount) {
		    boolean swaped = mMotionPosition==pos?true:false;
		        	
		    // is this the selected item?            	
		    View child = makeAndAddSpecialView(pos,swaped?mSwapPosition:0, nextTop, true, mListPadding.left, false,swaped);
		    nextTop = child.getBottom() + mDividerHeight;
		    
		    if(swaped){
		    	mItemSpotStates.put(mSwapPosition, true); 
	        }
		           
		    temp = child;
		    pos++;
		    m++;
		}
    
		return temp;
    }
    
    /**
     * ��Ϊ�����»�����List,mStackFromBottom���жϲ���������Ҫ����
     * ���� @adjustViewsBothUpAndDown()
     * 
     * Make sure views are touching the top or bottom edge, as appropriate for
     * our gravity
     */
    private void adjustViewsUpOrDown() {
        final int childCount = getChildCount();
        int delta;

        if (childCount > 0) {
            View child;

            if (!mStackFromBottom) {
                // Uh-oh -- we came up short. Slide all views up to make them
                // align with the top
                child = getChildAt(0);
                delta = child.getTop() - mListPadding.top;
                if (mFirstPosition != 0) {
                    // It's OK to have some space above the first item if it is
                    // part of the vertical spacing
                    delta -= mDividerHeight ;
                }
                if (delta < 0) {
                    // We only are looking to see if we are too low, not too high
                    delta = 0;
                }
            } else {
                // we are too high, slide all views down to align with bottom
                child = getChildAt(childCount - 1);
                delta = child.getBottom() - (getHeight() - mListPadding.bottom);

                if (mFirstPosition + childCount < mItemCount) {
                    // It's OK to have some space below the last item if it is
                    // part of the vertical spacing
                    delta += mDividerHeight;
                }

                if (delta > 0) {
                    delta = 0;
                }
            }

            if (delta != 0) {
                this.offsetTopAndBottom(-delta);
            }
        }
    }
    
    /**
     * �� adjustViewsUpOrDown ��ͬ���˷�����Ҫ���ǵĳ�ӯ
     * Make sure views are touching the top or bottom edge, as appropriate for
     * our gravity
     */
    private void adjustViewsBothUpAndDown() {
        final int childCount = getChildCount();
        int delta;

        if (childCount > 0) {
            View child;

            if (!mStackFromBottom) {
                // Uh-oh -- we came up short. Slide all views up to make them
                // align with the top
                child = getChildAt(0);
                delta = child.getTop() - mListPadding.top;
                if (mFirstPosition != 0) {
                    // It's OK to have some space above the first item if it is
                    // part of the vertical spacing
                    delta -= mDividerHeight ;
                }
                if (delta < 0) {
                    // We only are looking to see if we are too low, not too high
                    delta = 0;
                }
            } else {
                // we are too high, slide all views down to align with bottom
                child = getChildAt(childCount - 1);
                delta = child.getBottom() - (getHeight() - mListPadding.bottom);

                if (mFirstPosition + childCount < mItemCount) {
                    // It's OK to have some space below the last item if it is
                    // part of the vertical spacing
                    delta += mDividerHeight;
                }

                if (delta > 0) {
                    delta = 0;
                }
            }

            if (delta != 0) {
                this.offsetTopAndBottom(-delta);
            }
        }
    }

//	@Override
//	void fillGap(boolean out) {
//        final int count = getChildCount();
//        if (out) {
//            int paddingTop = 0;
//            if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
//                paddingTop = getListPaddingTop();
//            }
//            final int startOffset = count > 0 ? getChildAt(count - 1).getBottom() + mDividerHeight : paddingTop;
//            //================================================================================
//                             
//            correctTooOut(getChildCount());
//        } else {
//            int paddingBottom = 0;
//            if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
//                paddingBottom = getListPaddingBottom();
//            }
//            final int startOffset = count > 0 ? getChildAt(0).getTop() - mDividerHeight :
//                    getHeight() - paddingBottom;
//            fillUp(mFirstPosition - 1, startOffset);
//            //correctTooLow(getChildCount());
//        }
//    }

	@Override
	int findMotionRow(int y) {
		int childCount = getChildCount();
        if (childCount > 0) {
            if (!mStackFromBottom) {
                for (int i = 0; i < childCount; i++) {
                    View v = getChildAt(i);
                    if (y <= v.getBottom()) {
                        return mFirstPosition + i;
                    }
                }
            } else {
                for (int i = childCount - 1; i >= 0; i--) {
                    View v = getChildAt(i);
                    if (y >= v.getTop()) {
                        return mFirstPosition + i;
                    }
                }
            }
        }
        return INVALID_POSITION;
	}
	
    /**
     * setSelectionAfterHeaderView set the selection to be the first list item
     * after the header views.
     */
//    public void setSelectionAfterHeaderView() {
//        final int count = mHeaderViewInfos.size();
//        if (count > 0) {
//            mNextSelectedPosition = 0;
//            return;
//        }
//
//        if (mAdapter != null) {
//            setSelection(count);
//        } else {
//            mNextSelectedPosition = count;
//            mLayoutMode = LAYOUT_SET_SELECTION;
//        }
//
//    }
	
	
	/**
	 * 2014.03.13
	 */
	@Override
	protected void prepareInnerWindow() {
		super.prepareInnerWindow();
		
		if(mAboverPulledViewInfos.size()>0&&mBelowerPulledViewInfos.size()>0){
			mAboverPulledViewInfos.clear();
			mBelowerPulledViewInfos.clear();
		}
		
		int pullPosition = mPulledPosistion;
		int count = getChildCount();
		
		if(pullPosition>=0&&!mDataChanged&&count>0){
			
			View tempView ;
			
			for(int i = mFirstPosition;i<mFirstPosition+count-1;i++){
				
				if(i<pullPosition){
					tempView = getChildAt(i);
					addAboverPulledView(tempView,tempView.getTop());
				}
				
				if(i== pullPosition){
					tempView = getChildAt(i);
					mPulledTop = tempView.getTop();
					mPulledItemLeft = tempView.getLeft();
					mPulledItemWidth = tempView.getWidth();
					mPulledItemHeight = tempView.getHeight();
					mPulledBottom = tempView.getBottom();
				}
				
				if(i>pullPosition){
					tempView = getChildAt(i);
					addBelowerPulledView(tempView,tempView.getTop());
				}		
			}
			
			if(mInnerWindowWidth == 0){
				mInnerWindowWidth = mPulledItemWidth;
			}
			
			if(mInnerWindowHeight == 0){
				mInnerWindowHeight = mPulledItemHeight + mDividerHeight;
			}else{
				mInnerWindowHeight = mInnerWindowHeight+ mDividerHeight;
			}
			
			mTouchMode = TOUCH_MODE_PULLING;
		}
	}
	
	/**
	 * 2014.03.13
	 */
	@Override
	protected void  closeInnerWindow() {
		super.closeInnerWindow();
	}

	/**
	 * @param deltaY
	 * @return
	 */
	@Override
	protected boolean pullTo(int deltaY) {
		/*test*/
		mFirstPosition = 0;
		
		int rawDeltaY = Math.abs(deltaY);
		System.out.println("pullTo" +rawDeltaY);
		final RecycleBin recycleBin = mRecycler;
		View tempView;
		int oldTempViewTop;
		int newTempViewTop;
		int childHeight;
		int innerWinTop = 0;
		int aboverAtBottomViewBottom = 0;
		
		FixedViewInfo tempFixedViewInfo;
		
		if(!mDataChanged){
			int aboverCount = mAboverPulledViewInfos.size();
			int belowerCount = mBelowerPulledViewInfos.size();
			detachAllViewsFromParent();
			recycleBin.removeSkippedMeasuredAndUnused();
			
			if(aboverCount >0||belowerCount>0){
				for(int index = 0;index<aboverCount;index++){
					tempFixedViewInfo = mAboverPulledViewInfos.get(index);
					tempView = tempFixedViewInfo.view;
					oldTempViewTop =(Integer) tempFixedViewInfo.data;
					newTempViewTop = oldTempViewTop-rawDeltaY;
					tempFixedViewInfo.data = newTempViewTop;

					setupPulledView(tempView,true,newTempViewTop,mPulledItemLeft,false);
					
					if(index == aboverCount-1){
						aboverAtBottomViewBottom = newTempViewTop+ mPulledItemHeight + mDividerHeight;
					}
				}

				if(mInnerWindow!=null){
					//固定在InnerItemView中间位置。
					innerWinTop = mPulledTop - (mInnerWindowHeight-mPulledItemHeight)/2;
					setupInnerWindow(mInnerWindow,true,innerWinTop,mPulledItemLeft,false);	
				}
			    	
				for(int index = 0;index<belowerCount;index++){
					tempFixedViewInfo = mBelowerPulledViewInfos.get(index);
					tempView = tempFixedViewInfo.view;
					childHeight = tempView.getHeight();
					oldTempViewTop =(Integer) tempFixedViewInfo.data;
					newTempViewTop = oldTempViewTop +rawDeltaY;
					tempFixedViewInfo.data = newTempViewTop;
					
					setupPulledView(tempView,true,newTempViewTop,mPulledItemLeft,false);

					if(index == belowerCount-1){
						int adjustHeight = mBottom-(newTempViewTop + childHeight);
						newTempViewTop = newTempViewTop + childHeight + mDividerHeight;
						
						if(Math.abs(adjustHeight)>0){
							int adjustPosistion = mFirstPosition+ aboverCount+belowerCount+1;
							tempView = makeAndAddView(adjustPosistion, newTempViewTop, true, mPulledItemLeft, false);
						}
					}
				}			
				mRawPullDistance = innerWinTop - aboverAtBottomViewBottom;
				System.out.println("pullto " + mRawPullDistance) ;
				//设置移动值 
				setPullY(deltaY);
			}					   	
		   	invalidate();
		}
		return true;
    }
	
	@Override
	protected boolean pullBack(int backDelta) {
		System.out.println("smoothBackTo " + backDelta) ;
		
		View tempView;
		int oldTempViewTop;
		int newTempViewTop;
		int childHeight;
		
		FixedViewInfo tempFixedViewInfo;
		
		if(!mDataChanged){
			int aboverCount = mAboverPulledViewInfos.size();
			int belowerCount = mBelowerPulledViewInfos.size();
			//detachAllViewsFromParent();
			//recycleBin.removeSkippedMeasuredAndUnused();
			
			if(aboverCount >0||belowerCount>0){
				for(int index = 0;index<aboverCount;index++){
					tempFixedViewInfo = mAboverPulledViewInfos.get(index);
					tempView = tempFixedViewInfo.view;
					oldTempViewTop =(Integer) tempFixedViewInfo.data;
					newTempViewTop = oldTempViewTop+backDelta;
					tempFixedViewInfo.data = newTempViewTop;
					
					setupPulledView(tempView,true,newTempViewTop,tempView.getLeft(),false);
				}

				for(int index = 0;index<belowerCount;index++){
					tempFixedViewInfo = mBelowerPulledViewInfos.get(index);
					tempView = tempFixedViewInfo.view;
					childHeight = tempView.getHeight();
					oldTempViewTop =(Integer) tempFixedViewInfo.data;
					newTempViewTop = oldTempViewTop - backDelta;
					tempFixedViewInfo.data = newTempViewTop;
					
					setupPulledView(tempView,true,newTempViewTop,tempView.getLeft(),false);

					if(index == belowerCount-1){
						int adjustHeight = mBottom-(tempView.getTop() + childHeight);
						newTempViewTop = tempView.getTop() + childHeight + mDividerHeight;
						
						if(Math.abs(adjustHeight)>0){
							int adjustPosistion = mFirstPosition+ aboverCount+belowerCount+1;
							tempView = makeAndAddView(adjustPosistion, newTempViewTop, true, tempView.getLeft(), false);
						}
					}
				}
				
			}					   	
		   	invalidate();
		}
		return true;
    }

	private void setupPulledView(View tempView,boolean flowDown, int y,int selLeft,boolean isTopOrBottom) {
		final boolean needToMeasureAndLayout =  tempView.isLayoutRequested();
        AbsFoolView.LayoutParams p = (AbsFoolView.LayoutParams) tempView.getLayoutParams();
        if (p == null) {
            p = (AbsFoolView.LayoutParams) generateDefaultLayoutParams();
        }

        if(needToMeasureAndLayout){
        	 int childWidthSpec = getChildMeasureSpec(mWidthMeasureSpec,mListPadding.left + mListPadding.right, p.width);
             int lpHeight = p.height;
             int childHeightSpec;
             if (lpHeight > 0) {
             	childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
             } else {
                 childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
             }
             tempView.measure(childWidthSpec, childHeightSpec);
        }

        final int w = tempView.getMeasuredWidth();
        final int h = tempView.getMeasuredHeight();
        final int childTop = flowDown ? y : y - h;
        final int childRight = selLeft + w;
        final int childBottom = childTop + h;
        
        tempView.layout(selLeft, childTop, childRight, childBottom);             
        addViewInLayout(tempView, flowDown ? -1 : 0, p, true);
        
        //attachViewToParent(tempView, flowDown ? -1 : 0, p);
	}
	
    public void setInnerWindowWidth(int innerWidth) {
		this.mInnerWindowWidth = innerWidth;
	}

	public void setInnerWindowHeight(int innerHeight) {
		this.mInnerWindowHeight = innerHeight;
	} 
	
	public int getInnerWindowWidth(){
		return mInnerWindowWidth;
	}
	
	public int getInnerWindowHeight(){
		return mInnerWindowHeight;
	}
	
	private void setupInnerWindow(IInnerWin tempWin,boolean flowDown, int y,int selLeft,boolean isTopOrBottom) {
		LinearLayout.LayoutParams p= tempWin.getInnerLayoutParams();
		if(p==null){
			 p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					 LinearLayout.LayoutParams.WRAP_CONTENT, 0);
		}
		
		int childWidthSize = mInnerWindowWidth;
		int childHeightSize = mInnerWindowHeight;
		int childWidthMeasureMode = MeasureSpec.EXACTLY;   
        int childHeightMeasureMode = MeasureSpec.AT_MOST;
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthMeasureMode,childWidthSize);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightMeasureMode,childHeightSize);
        
        View childView = (View)tempWin;
		childView.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        final int w = childWidthSize;
        final int h = childHeightSize;
        final int childTop = flowDown ? y : y - h;
        final int childRight = selLeft + w;
        final int childBottom = childTop + h;
        
        childView.layout(selLeft, childTop, childRight, childBottom);        
        
        addViewInLayout(childView, flowDown ? -1 : 0, p, true);
        
        //attachViewToParent(tempView, flowDown ? -1 : 0, p);
	}
	
}
