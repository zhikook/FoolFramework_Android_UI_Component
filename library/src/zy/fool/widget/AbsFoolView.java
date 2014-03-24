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
import java.util.List;

import zy.android.collect.Lists;
import zy.android.view.EdgeEffect;
import zy.android.widget.Checkable;
import zy.fool.R;

import zy.fool.internal.ViewCompat;
import zy.fool.policy.IInnerWin;
import zy.fool.policy.IPolicy;
import zy.fool.policy.InnerWindowManagerImpl;
import zy.fool.widget.FoolListView.FixedViewInfo;
import zy.fool.widget.IFoolCallback.OnPullListener;
import zy.fool.widget.IFoolCallback.OnSlideListener;
import zy.fool.widget.IFoolCallback.OnSmoothPullFinishedListener;

import android.R.integer;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.StateSet;
import android.view.ActionMode;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ListAdapter;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public abstract class AbsFoolView extends FoolAdapterView<ListAdapter>{
	
	 /**
     * When set, this ViewGroup should not intercept touch events.
     * {@hide}
     */
    protected static final int FLAG_DISALLOW_INTERCEPT = 0x80000;
    
    /**
     * Indicates that this view was specifically invalidated, not just dirtied because some
     * child view was invalidated. The flag is used to determine when we need to recreate
     * a view's display list (as opposed to just returning a reference to its existing
     * display list).
     *
     * @hide
     */
    static final int PFLAG_INVALIDATED                 = 0x80000000;
    
	/**
     * When set, this ViewGroup tries to always draw its children using their drawing cache.
     */
    static final int FLAG_ALWAYS_DRAWN_WITH_CACHE      = 0x4000;

    // When set, ViewGroup invalidates only the child's rectangle
    // Set by default
    static final int FLAG_CLIP_CHILDREN                = 0x1;
    // When set, ViewGroup excludes the padding area from the invalidate rectangle
    // Set by default
    private static final int FLAG_CLIP_TO_PADDING      = 0x2;
   
    // When set, there is either no layout animation on the ViewGroup or the layout
    // animation is over
    // Set by default
    static final int FLAG_ANIMATION_DONE               = 0x10;
    
    // When set, this ViewGroup caches its children in a Bitmap before starting a layout animation
    // Set by default
    private static final int FLAG_ANIMATION_CACHE      = 0x40;
    
    /**
     * When set, this ViewGroup will split MotionEvents to multiple child Views when appropriate.
     */
    private static final int FLAG_SPLIT_MOTION_EVENTS  = 0x200000;
    
	 /**
     * Regular layout - usually an unsolicited layout from the view system
     */
    static final int LAYOUT_PREVIEW = 0;

    /**
     * Show the first item
     */
    static final int LAYOUT_FORCE_TOP = 1;

    /**
     * Force the selected item to be on somewhere on the screen
     */
    static final int LAYOUT_SET_SELECTION = 2;

    /**
     * Show the last item
     */
    static final int LAYOUT_FORCE_BOTTOM = 3;

    /**
     * Make a mSelectedItem appear in a specific location and build the rest of
     * the views from there. The top is specified by mSpecificTop.
     */
    static final int LAYOUT_SPECIFIC = 4;

    /**
     * Layout to sync as a result of a data change. Restore mSyncPosition to have its top
     * at mSpecificTop
     */
    static final int LAYOUT_SYNC = 5;

    /**
     * Layout as a result of using the navigation keys
     */
    static final int LAYOUT_MOVE_SELECTION = 6;
    
    static final int LAYOUT_SPOT = 7;
    
    static final int LAYOUT_SLIDE = 8; 
    
    static final int LAYOUT_PULLING = 9; 
    
	static final int SMOOTH_PULL_DURATION_MS = 200;
	
	static final int SMOOTH_PULL_LONG_DURATION_MS = 325;
	
	static final int DEMO_PULL_INTERVAL = 225;
	
	final float FRICTION  = 2.5F;

    /**
     * How many positions in either direction we will search to try to
     * find a checked item with a stable ID that moved position across
     * a data set change. If the item isn't found it will be unselected.
     */
    private static final int CHECK_POSITION_SEARCH_DISTANCE = 20;

    /**
     * Used to request a layout when we changed touch mode
     */
    private static final int TOUCH_MODE_UNKNOWN = -1;
    private static final int TOUCH_MODE_ON = 0;
    private static final int TOUCH_MODE_OFF = 1;

    private int mLastTouchMode = TOUCH_MODE_UNKNOWN;
    
    /**
     * Indicates that we are not in the middle of a touch gesture
     */
    static final int TOUCH_MODE_REST = -1;

    /**
     * Indicates we just received the touch event and we are waiting to see if the it is a tap or a
     * scroll gesture.
     */
    static final int TOUCH_MODE_DOWN = 0;

    /**
     * Indicates the touch has been recognized as a tap and we are now waiting to see if the touch
     * is a longpress
     */
    static final int TOUCH_MODE_TAP = 1;

    /**
     * Indicates we have waited for everything we can wait for, but the user's finger is still down
     */
    static final int TOUCH_MODE_DONE_WAITING = 2;
    
    /**
     * Indicates the view is being pulled into of normal content bounds
     * and will spring back.
     */
    static final int TOUCH_MODE_PULL = 3;
    
    /**
     * Indicates the view is being pulled into of normal content bounds
     * and will spring back.
     */
    static final int TOUCH_MODE_OVERPULL = 4;  
   
    /**
     * Indicates the view is being pulled into of normal content bounds
     * and will spring back.
     */
    static final int TOUCH_MODE_SLIDE = 5;  
   
    /**
     * Indicates the view is being slided into of normal content bounds
     * and will spring back.
     */
    static final int TOUCH_MODE_OVERSLIDE = 6;
    
    static final int TOUCH_MODE_PULLING = 7;
   
    /**
     * mPullMode
     */
    static final int PULL_OUT = 1;
    static final int PULL_IN = 2;
    static final int PULL_REST = -1;
     
   /**
	 * mSlideMode
	 */
	static final int TOUCH_SLIDE_LEFT = 1;
	
	static final int TOUCH_SLIDE_RIGHT = 2;
	
	static final int PULLING_TIME = 40; // milliseconds
	
	static final int SLIDING_TIME = 40; // milliseconds
	
    /**
     * One of TOUCH_MODE_REST, TOUCH_MODE_DOWN, TOUCH_MODE_TAP, TOUCH_MODE_SCROLL, or
     * TOUCH_MODE_DONE_WAITING
     */
    int mTouchMode = TOUCH_MODE_REST;
    
    /**
     * Normal list that does not indicate choices
     */
    public static final int CHOICE_MODE_NONE = 0;

    /**
     * The list allows up to one choice
     */
    public static final int CHOICE_MODE_SINGLE = 1;

    /**
     * The list allows multiple choices
     */
    public static final int CHOICE_MODE_MULTIPLE = 2;

    /**
     * The list allows multiple choices in a modal selection mode
     */
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;
    
    final boolean[] mIsMeasuredAndUnused = new boolean[1];
	   
	RecycleBin mRecycler = new RecycleBin();

	int mWidthMeasureSpec = 0;
	
	protected int mLeft;
	   
	protected int mRight;
	    
	protected int mTop;
	   
	protected int mBottom; 
	   
	protected int mPaddingLeft;
	    
	protected int mPaddingRight;
	    
	protected int mPaddingTop;
	    
	protected int mPaddingBottom;
	
	/**
     * This view's padding
     */
    Rect mListPadding = new Rect();
   
	protected int mGroupFlags;  
	
	/**
     * Controls how the next layout will happen
     */
    int mLayoutMode = LAYOUT_PREVIEW;

    /**
     * Controls if/how the user may choose/check items in the list
     */
    int mChoiceMode = CHOICE_MODE_NONE;
    
    /**
     * Controls CHOICE_MODE_MULTIPLE_MODAL. null when inactive.
     */
    ActionMode mChoiceActionMode;
    
    /**
     * Wrapper for the multiple choice mode callback; AbsFoolView needs to perform
     * a few extra actions around what application code does.
     */
    MultiChoiceModeWrapper mMultiChoiceModeCallback;

    private ContextMenuInfo mContextMenuInfo = null;
    
    /**
     * The last CheckForLongPress runnable we posted, if any
     */
    private CheckForLongPress mPendingCheckForLongPress;
    
    /**
     * The last CheckForTap runnable we posted, if any
     */
    private Runnable mPendingCheckForTap;
  
    private Runnable mClearPullingCache;
     
	private int mLastHandledItemCount;
	
    ViewConfiguration config;
	
	AdapterDataSetObserver mDataSetObserver;

	ListAdapter mAdapter;
	
    /**
     *  The remote adapter containing the data to be displayed by this view to be set
			
	    private BaseAdapter mRemoteAdapter;
	 */
	
	/**
     * Running count of how many items are currently checked
     */
    int mCheckedItemCount;

    /**
     * Running state of which positions are currently checked
     */
    SparseBooleanArray mCheckStates;

    /**
     * Running state of which IDs are currently checked.
     * If there is a value for a given key, the checked state for that ID is true
     * and the value holds the last known position in the adapter for that id.
     */
    LongSparseArray<Integer> mCheckedIdStates;
    
    /**
     * Running state of which positions are currently checked
     */
    SparseBooleanArray mItemSpotStates;

    /**
     * Running state of which IDs are currently checked.
     * If there is a value for a given key, the checked state for that ID is true
     * and the value holds the last known position in the adapter for that id.
     */
    LongSparseArray<Integer> mItemSpotedIdStates;
	
    /**
     * Indicates whether the list selector should be drawn on top of the children or behind
     */
    boolean mDrawSelectorOnTop = false;

    /**
     * The drawable used to draw the selector
     */
    Drawable mSelector;

    /**
     * The selection's left padding
     */
    int mSelectionLeftPadding = 0;

    /**
     * The selection's top padding
     */
    int mSelectionTopPadding = 0;

    /**
     * The selection's right padding
     */
    int mSelectionRightPadding = 0;

    /**
     * The selection's bottom padding
     */
    int mSelectionBottomPadding = 0;
	
	private SavedState mPendingSync;

    /**
     *					 mAbovePulledView
     *                   mBelowPulledView
     *                   
     *                   mPulledTop
     *                   mPulledItemLeft
     *                   mPulledItemWidth
     *                   mPulledItemHeight
     *                   mPulledBottom
     *                   
     *                   mInnerView ;replace the mpulledPositionitemview;
     */
    ArrayList<FixedViewInfo> mAboverPulledViewInfos = Lists.newArrayList();
    ArrayList<FixedViewInfo> mBelowerPulledViewInfos = Lists.newArrayList();

    IPolicy mInnerPolicy;    //InnerWindow Manager Interface
    
	IInnerWin mInnerWindow;  //InnerWindow Interface
	
	int mLastPulledDelta = Integer.MIN_VALUE;
    
    int mRawPullDistance = 0;
	
	int mPullY = 0;
	
    //InnerWindow measure and Layout
    int mInnerWindowWidth;
    int mInnerWindowHeight;
	
	int mCurrentPullMode = PULL_REST;
	
	int mLastPullMode = PULL_REST;
	
	RectF tempPulledRectF ;
	
	int mPulledTop;
    
    int mPulledItemLeft;
    
    int mPulledItemWidth;
    
    int mPulledItemHeight;
    
    int mPulledBottom;
    
    int mPulledPosistion = INVALID_POSITION;
    
    int mSecondActiveId = INVALID_POINTER;
    
    long mTouch1DownTime;
    
    /**
     * Tracks the state of the top edge glow.
     */
    private EdgeEffect mEdgeGlowTop;

    /**
     * Tracks the state of the bottom edge glow.
     */
    private EdgeEffect mEdgeGlowBottom;
    
    /**
     * If mAdapter != null, whenever this is true the adapter has stable IDs.
     */
    boolean mAdapterHasStableIds;
    
    //selected vars
    
    int mSelectorPosition = INVALID_POSITION;

    private int mPersistentDrawingCache;
    
    /**
     * Defines the selector's location and dimension at drawing time
     */
    Rect mSelectorRect = new Rect();

    /**
     * The select child's view (from the adapter's getView) is enabled.
     */
    private boolean mIsChildViewEnabled;
    
    int mScrollX, mScrollY;
    
    int incrementalDeltaX = 0;    

    int mItemPulledItemCount; 
    
    int mSwapPosition ;
     
    int mMotionX, mMotionY;
    
    int mInitX, mInitY;
    
    int mLastX,mLastY;
    
    /**
     * Used for determining when to cancel out of overscroll.
     */
    private int mDirection = 0;

    /**
     * The position of the view that received the down motion event
     */
    int mMotionPosition;

    /**
     * The offset to the top of the mMotionPosition view when the down motion event was received
     */
    int mMotionViewOriginalTop;

    /**
     * The desired offset to the top of the mMotionPosition view after a scroll
     */
    int mMotionViewNewTop;
	
	/**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    /**
     * Rectangle used for hit testing children
     */
    private Rect mTouchFrame = new Rect(0,0,1,1);
    
    float x = 0,y = 0;
    
    /**
     * Rectangle used for hit testing children
     */
    private RectF mFrame = new RectF(0.0F,0.0F,1.0F,1.0F);
   
    private RectF mStartPullRectF;
    
    int mTouchSlop;
    
    boolean isSliding = false;
    
    boolean canPull = false;    	
    	  
    /**
     * When set to true, the list automatically discards the children's
     * bitmap cache after scrolling.
     */
    boolean mPullingCacheEnabled;
    
    boolean itemNeedAlpha = true;
    
    private int mGlowPaddingLeft;
    private int mGlowPaddingRight;
    
    /**
     * How far the finger moved before we started scrolling
     */
    int mMotionCorrection;
    
    /**
     * The offset in pixels form the top of the AdapterView to the top
     * of the currently selected view. Used to save and restore state.
     */
    int mSelectedTop = 0;

    private int mMinimumVelocity,mMaximumVelocity;

    private float mVelocityScale = 1.0f;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
   
    /**
     * Acts upon click
     */
    private PerformClick mPerformClick;
    
    /**
     * Acts upon slide
     */
    private PerformItemSlide mPerformItemSlide;
    
    /**
     * Acts upon slide
     */
    private SpotSlide mSpotSlide;

    /**
     * Acts upon spot
     */
    private PerformSpot mSpotNext;

    /**
     * Delayed action for touch mode.
     */
    private Runnable mTouchModeReset;
    
    int mSlidedPosition;
    int mSpotPosition;  
    int mSlideMode;

    boolean isSpot;
	/**
     * Indicates that this list is always drawn on top of a solid, single-color, opaque
     * background
     */
    private int mCacheColorHint;

	boolean mIsAttached;

	boolean mCachingStarted;
	
	boolean mCachingActive;
	
	 /**
     * Indicates whether the list is stacked from the bottom edge or
     * the top edge.
     */
    boolean mStackFromBottom;
    
    /**
     * The position to resurrect the selected position to.
     */
    int mResurrectToPosition = INVALID_POSITION;
 
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }
    
	public AbsFoolView(Context context) {
		super(context);
		
	}
	
	public AbsFoolView(Context context, AttributeSet attrs) {
		this(context, attrs,0);
		initFoolAbsView(attrs);
	}

	public AbsFoolView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AbsFoolView);
		Drawable d = a.getDrawable(R.styleable.AbsFoolView_listSelector);
	    if (d != null) {
	        setSelector(d);
	    } 

	    boolean pullingCacheEnabled = a.getBoolean(R.styleable.AbsFoolView_pullingCache, true);
        
        setPullingCacheEnabled(pullingCacheEnabled);
        
        int color = a.getColor(R.styleable.AbsFoolView_cacheColorHint, 0);
        setCacheColorHint(color);
  		
		a.recycle();
		
		initFoolAbsView(attrs);
	}
	
	void initFoolAbsView(AttributeSet attrs){
		setPullingCacheEnabled(true);
		setWillNotDraw(false);
		
        mGroupFlags |= FLAG_CLIP_CHILDREN;
        mGroupFlags |= FLAG_CLIP_TO_PADDING;
        mGroupFlags |= FLAG_ANIMATION_DONE;
        mGroupFlags |= FLAG_ANIMATION_CACHE;
        mGroupFlags |= FLAG_ALWAYS_DRAWN_WITH_CACHE;

        if (getContext().getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.HONEYCOMB) {
            mGroupFlags |= FLAG_SPLIT_MOTION_EVENTS;
        }

		config = ViewConfiguration.get(getContext());		
		
		mTouchSlop = config.getScaledTouchSlop();
		mMinimumVelocity = config.getScaledMinimumFlingVelocity();
        mMaximumVelocity = config.getScaledMaximumFlingVelocity();
        
		mPersistentDrawingCache = getPersistentDrawingCache();
		mSlidedPosition = INVALID_POSITION;
		mSpotPosition = INVALID_POSITION;
		
		if(isSpot)initSpot();
		
		if(mInnerPolicy==null){
			mInnerPolicy = new InnerWindowManagerImpl(getContext(), attrs);
		}
	}

	/**
	 * @param can
	 */
	public void setSpotCan(boolean can){
		this.isSpot = can;
	}
	
	public boolean isSpotable(){
		return isSpot;
	}
	
	/**
	 * @param can
	 */
	public void setPullCan(boolean can){
		this.isAllowedPull = can;
	}
	
	public boolean isPullable(){
		return isAllowedPull;
	}
	
	protected boolean isAllowedPull = false;
	
	/**
     * List padding is the maximum of the normal view's padding and the padding of the selector.
     *
     * @see android.view.View#getPaddingTop()
     * @see #getSelector()
     *
     * @return The top list padding.
     */
    public int getListPaddingTop() {
        return mListPadding.top;
    }

    /**
     * List padding is the maximum of the normal view's padding and the padding of the selector.
     *
     * @see android.view.View#getPaddingBottom()
     * @see #getSelector()
     *
     * @return The bottom list padding.
     */
    public int getListPaddingBottom() {
        return mListPadding.bottom;
    }

    /**
     * List padding is the maximum of the normal view's padding and the padding of the selector.
     *
     * @see android.view.View#getPaddingLeft()
     * @see #getSelector()
     *
     * @return The left list padding.
     */
    public int getListPaddingLeft() {
        return mListPadding.left;
    }

    /**
     * List padding is the maximum of the normal view's padding and the padding of the selector.
     *
     * @see android.view.View#getPaddingRight()
     * @see #getSelector()
     *
     * @return The right list padding.
     */
    public int getListPaddingRight() {
        return mListPadding.right;
    }

    /**
     * Indicates whether the children's drawing cache is used during a scroll.
     * By default, the drawing cache is enabled but this will consume more memory.
     *
     * @return true if the scrolling cache is enabled, false otherwise
     *
     * @see #setScrollingCacheEnabled(boolean)
     * @see View#setDrawingCacheEnabled(boolean)
     */
    @ViewDebug.ExportedProperty
    public boolean isPullingCacheEnabled() {
        return mPullingCacheEnabled;
    }

    /**
     * Enables or disables the children's drawing cache during a pull.
     * By default, the drawing cache is enabled but this will use more memory.
     *
     * When the scrolling cache is enabled, the caches are kept after the
     * first scrolling. You can manually clear the cache by calling
     * {@link android.view.ViewGroup#setChildrenDrawingCacheEnabled(boolean)}.
     *
     * @param enabled true to enable the scroll cache, false otherwise
     *
     * @see #isPullingCacheEnabled()
     * @see View#setDrawingCacheEnabled(boolean)
     */
    public void setPullingCacheEnabled(boolean enabled) {
        if (mPullingCacheEnabled && !enabled) {
            clearPullingCache();
        }
        mPullingCacheEnabled = enabled;
    }
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
    public void setAdapter(ListAdapter adapter) {
        if (adapter != null) {
            mAdapterHasStableIds = mAdapter.hasStableIds();

            if ( mAdapterHasStableIds && mCheckedIdStates == null) {
                mCheckedIdStates = new LongSparseArray<Integer>();
            }
        }

        //----------------------------------------------------------------------
        
        if (mCheckStates != null) {
            mCheckStates.clear();
        }

        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }

        if (mItemSpotStates != null) {
        	mItemSpotStates.clear();
        }

        if (mItemSpotedIdStates != null) {
        	mItemSpotedIdStates.clear();
        }
    }
	
	@Override
    protected void handleDataChanged() {
    	
        int count = mItemCount;
        int lastHandledItemCount = mLastHandledItemCount;
        mLastHandledItemCount = mItemCount;

        if (mChoiceMode != CHOICE_MODE_NONE && mAdapter != null && mAdapter.hasStableIds()) {
            confirmCheckedPositionsById();
        }
        
        // TODO: In the future we can recycle these views based on stable ID instead.
        mRecycler.clearTransientStateViews();

        if (count > 0) {
            int newPos;
            int selectablePos;

            // Find the row we are supposed to sync to
            if (mNeedSync) {
                // Update this first, since setNextSelectedPositionInt inspects it
                mNeedSync = false;
                mPendingSync = null;
                

//                if (mTranscriptMode == TRANSCRIPT_MODE_ALWAYS_SCROLL) {
//                    mLayoutMode = LAYOUT_FORCE_BOTTOM;
//                    return;
//                } else if (mTranscriptMode == TRANSCRIPT_MODE_NORMAL) {
//                    if (mForceTranscriptScroll) {
//                        mForceTranscriptScroll = false;
//                        mLayoutMode = LAYOUT_FORCE_BOTTOM;
//                        return;
//                    }
//                    final int childCount = getChildCount();
//                    final int listBottom = getHeight() - getPaddingBottom();
//                    final View lastChild = getChildAt(childCount - 1);
//                    final int lastBottom = lastChild != null ? lastChild.getBottom() : listBottom;
//                    if (mFirstPosition + childCount >= lastHandledItemCount &&
//                            lastBottom <= listBottom) {
//                        mLayoutMode = LAYOUT_FORCE_BOTTOM;
//                        return;
//                    }
//                    // Something new came in and we didn't scroll; give the user a clue that
//                    // there's something new.
//                    awakenScrollBars();
//                }

                switch (mSyncMode) {
                case SYNC_SELECTED_POSITION:
                    if (isInTouchMode()) {
                        // We saved our state when not in touch mode. (We know this because
                        // mSyncMode is SYNC_SELECTED_POSITION.) Now we are trying to
                        // restore in touch mode. Just leave mSyncPosition as it is (possibly
                        // adjusting if the available range changed) and return.
                        mLayoutMode = LAYOUT_SYNC;
                        mSyncPosition = Math.min(Math.max(0, mSyncPosition), count - 1);

                        return;
                    } else {
                        // See if we can find a position in the new data with the same
                        // id as the old selection. This will change mSyncPosition.
                        newPos = findSyncPosition();
                        if (newPos >= 0) {
                            // Found it. Now verify that new selection is still selectable
                            selectablePos = lookForSelectablePosition(newPos, true);
                            if (selectablePos == newPos) {
                                // Same row id is selected
                                mSyncPosition = newPos;
                                if (mSyncHeight == getHeight()) {
                                    // If we are at the same height as when we saved state, try
                                    // to restore the scroll position too.
                                    mLayoutMode = LAYOUT_SYNC;
                                } else {
                                    // We are not the same height as when the selection was saved, so
                                    // don't try to restore the exact position
                                    mLayoutMode = LAYOUT_SET_SELECTION;
                                }

                                // Restore selection
                                setNextSelectedPositionInt(newPos);
                                return;
                            }
                        }
                    }
                    break;
                case SYNC_FIRST_POSITION:
                    // Leave mSyncPosition as it is -- just pin to available range
                    mLayoutMode = LAYOUT_SYNC;
                    mSyncPosition = Math.min(Math.max(0, mSyncPosition), count - 1);
                    return;
                }
             }
            
            if (!isInTouchMode()) {
                // We couldn't find matching data -- try to use the same position
                newPos = getSelectedItemPosition();

                // Pin position to the available range
                if (newPos >= count) {
                    newPos = count - 1;
                }
                if (newPos < 0) {
                    newPos = 0;
                }

                // Make sure we select something selectable -- first look down
                selectablePos = lookForSelectablePosition(newPos, true);

                if (selectablePos >= 0) {
                    setNextSelectedPositionInt(selectablePos);
                    return;
                } else {
                    // Looking down didn't work -- try looking up
                    selectablePos = lookForSelectablePosition(newPos, false);
                    if (selectablePos >= 0) {
                        setNextSelectedPositionInt(selectablePos);
                        return;
                    }
                }
            } else {

                // We already know where we want to resurrect the selection
                if (mResurrectToPosition >= 0) {
                    return;
                }
            }
        }

        // Nothing is selected. Give up and reset everything.
        mSelectedPosition = INVALID_POSITION;
        mSelectedRowId = INVALID_ROW_ID;
        mNextSelectedPosition = INVALID_POSITION;
        mNextSelectedRowId = INVALID_ROW_ID;
        mNeedSync = false;
        mPendingSync = null;
        mSelectorPosition = INVALID_POSITION;

        checkSelectionChanged();
    }
    
    static class SavedState extends BaseSavedState {
        long selectedId;
        long firstId;
        int viewTop;
        int position;
        int height;
        String filter;
        boolean inActionMode;
        int checkedItemCount;
        SparseBooleanArray checkState;
        LongSparseArray<Integer> checkIdState;

        /**
         * Constructor called from {@link FoolAbsView#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            selectedId = in.readLong();
            firstId = in.readLong();
            viewTop = in.readInt();
            position = in.readInt();
            height = in.readInt();
            filter = in.readString();
            inActionMode = in.readByte() != 0;
            checkedItemCount = in.readInt();
            checkState = in.readSparseBooleanArray();
            final int N = in.readInt();
            if (N > 0) {
                checkIdState = new LongSparseArray<Integer>();
                for (int i=0; i<N; i++) {
                    final long key = in.readLong();
                    final int value = in.readInt();
                    checkIdState.put(key, value);
                }
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(selectedId);
            out.writeLong(firstId);
            out.writeInt(viewTop);
            out.writeInt(position);
            out.writeInt(height);
            out.writeString(filter);
            out.writeByte((byte) (inActionMode ? 1 : 0));
            out.writeInt(checkedItemCount);
            out.writeSparseBooleanArray(checkState);
            final int N = checkIdState != null ? checkIdState.size() : 0;
            out.writeInt(N);
            for (int i=0; i<N; i++) {
                out.writeLong(checkIdState.keyAt(i));
                out.writeInt(checkIdState.valueAt(i));
            }
        }

        @Override
        public String toString() {
            return "AbsFoolView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " selectedId=" + selectedId
                    + " firstId=" + firstId
                    + " viewTop=" + viewTop
                    + " position=" + position
                    + " height=" + height
                    + " filter=" + filter
                    + " checkState=" + checkState + "}";
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }


    @Override
    public Parcelable onSaveInstanceState() {
        /*
         * This doesn't really make sense as the place to dismiss the
         * popups, but there don't seem to be any other useful hooks
         * that happen early enough to keep from getting complaints
         * about having leaked the window.
         */
        //dismissPopup();

        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        if (mPendingSync != null) {
            // Just keep what we last restored.
            ss.selectedId = mPendingSync.selectedId;
            ss.firstId = mPendingSync.firstId;
            ss.viewTop = mPendingSync.viewTop;
            ss.position = mPendingSync.position;
            ss.height = mPendingSync.height;
            ss.filter = mPendingSync.filter;
            ss.inActionMode = mPendingSync.inActionMode;
            ss.checkedItemCount = mPendingSync.checkedItemCount;
            ss.checkState = mPendingSync.checkState;
            ss.checkIdState = mPendingSync.checkIdState;
            return ss;
        }

        boolean haveChildren = getChildCount() > 0 && mItemCount > 0;
        long selectedId = getSelectedItemId();
        ss.selectedId = selectedId;
        ss.height = getHeight();

        if (selectedId >= 0) {
            // Remember the selection
            ss.viewTop = mSelectedTop;
            ss.position = getSelectedItemPosition();
            ss.firstId = INVALID_POSITION;
        } else {
            if (haveChildren && mFirstPosition > 0) {
                // Remember the position of the first child.
                // We only do this if we are not currently at the top of
                // the list, for two reasons:
                // (1) The list may be in the process of becoming empty, in
                // which case mItemCount may not be 0, but if we try to
                // ask for any information about position 0 we will crash.
                // (2) Being "at the top" seems like a special case, anyway,
                // and the user wouldn't expect to end up somewhere else when
                // they revisit the list even if its content has changed.
                View v = getChildAt(0);
                ss.viewTop = v.getTop();
                int firstPos = mFirstPosition;
                if (firstPos >= mItemCount) {
                    firstPos = mItemCount - 1;
                }
                ss.position = firstPos;
                ss.firstId = mAdapter.getItemId(firstPos);
            } else {
                ss.viewTop = 0;
                ss.firstId = INVALID_POSITION;
                ss.position = 0;
            }
        }

//        ss.filter = null;
//        if (mFiltered) {
//            final EditText textFilter = mTextFilter;
//            if (textFilter != null) {
//                Editable filterText = textFilter.getText();
//                if (filterText != null) {
//                    ss.filter = filterText.toString();
//                }
//            }
//        }

        ss.inActionMode = mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode != null;

        if (mCheckStates != null) {
            ss.checkState = mCheckStates.clone();
        }

        if (mCheckedIdStates != null) {
            final LongSparseArray<Integer> idState = new LongSparseArray<Integer>();
            final int count = mCheckedIdStates.size();
            for (int i = 0; i < count; i++) {
                idState.put(mCheckedIdStates.keyAt(i), mCheckedIdStates.valueAt(i));
            }
            ss.checkIdState = idState;
        }
        ss.checkedItemCount = mCheckedItemCount;

//        if (mRemoteAdapter != null) {
//            mRemoteAdapter.saveRemoteViewsCache();
//        }

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        mDataChanged = true;

        mSyncHeight = ss.height;

        if (ss.selectedId >= 0) {
            mNeedSync = true;
            mPendingSync = ss;
            mSyncRowId = ss.selectedId;
            mSyncPosition = ss.position;
            mSpecificTop = ss.viewTop;
            mSyncMode = SYNC_SELECTED_POSITION;
        } else if (ss.firstId >= 0) {
            setSelectedPositionInt(INVALID_POSITION);
            // Do this before setting mNeedSync since setNextSelectedPosition looks at mNeedSync
            setNextSelectedPositionInt(INVALID_POSITION);
            mSelectorPosition = INVALID_POSITION;
            mNeedSync = true;
            mPendingSync = ss;
            mSyncRowId = ss.firstId;
            mSyncPosition = ss.position;
            mSpecificTop = ss.viewTop;
            mSyncMode = SYNC_FIRST_POSITION;
        }

        //setFilterText(ss.filter);

        if (ss.checkState != null) {
            mCheckStates = ss.checkState;
        }

        if (ss.checkIdState != null) {
            mCheckedIdStates = ss.checkIdState;
        }

        mCheckedItemCount = ss.checkedItemCount;

        if (ss.inActionMode && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL &&
                mMultiChoiceModeCallback != null) {
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        requestLayout();
    }
    /**
     * The list is empty. Clear everything out.
     */
    void resetList() {
        removeAllViewsInLayout();
        mFirstPosition = 0;
        mDataChanged = false;
        mNeedSync = false;
        mPendingSync = null;
        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;
        setSelectedPositionInt(INVALID_POSITION);
        setNextSelectedPositionInt(INVALID_POSITION);
        mSelectedTop = 0;
        mSelectorPosition = INVALID_POSITION;
        mSelectorRect.setEmpty();
        invalidate();
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            // Data may have changed while we were detached. Refresh.
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
        }

        mIsAttached = true;
        if(mLayoutMode!=LAYOUT_SPOT){
			mLayoutMode = LAYOUT_PREVIEW;
		}
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        final int touchMode = isInTouchMode() ? TOUCH_MODE_ON : TOUCH_MODE_OFF;

        if (!hasWindowFocus) {
            setChildrenDrawingCacheEnabled(false);
            // Always hide the type filter
            //dismissPopup();

            if (touchMode == TOUCH_MODE_OFF) {
                // Remember the last selected element
                mResurrectToPosition = mSelectedPosition;
            }
        } else {
        	// if (mFiltered && !mPopupHidden) {
        	// Show the type filter only if a filter is in effect
        	// showPopup();
        	// }

            // If we changed touch mode since the last time we had focus
            if (touchMode != mLastTouchMode && mLastTouchMode != TOUCH_MODE_UNKNOWN) {
                // If we come back in trackball mode, we bring the selection back
                if (touchMode == TOUCH_MODE_OFF) {
                    // This will trigger a layout
                    resurrectSelection();

                // If we come back in touch mode, then we want to hide the selector
                } else {
                    hideSelector();
                    mLayoutMode = LAYOUT_PREVIEW;
                    layoutChildren();
                }
            }
        }

        mLastTouchMode = touchMode;
    }

    /**
     * If there is a selection returns false.
     * Otherwise resurrects the selection and returns true if resurrected.
     */
    boolean resurrectSelectionIfNeeded() {
        if (mSelectedPosition < 0 && resurrectSelection()) {
            updateSelectorState();
            return true;
        }
        return false;
    }
	
    /**
     * Attempt to bring the selection back if the user is switching from touch
     * to trackball mode
     * @return Whether selection was set to something.
     */
    boolean resurrectSelection() {
        final int childCount = getChildCount();

        if (childCount <= 0) {
            return false;
        }

        int selectedTop = 0;
        int selectedPos;
        int childrenTop = mListPadding.top;
        int childrenBottom = mBottom - mTop - mListPadding.bottom;
        final int firstPosition = mFirstPosition;
        final int toPosition = mResurrectToPosition;
        boolean down = true;

        if (toPosition >= firstPosition && toPosition < firstPosition + childCount) {
            selectedPos = toPosition;

            final View selected = getChildAt(selectedPos - mFirstPosition);
            selectedTop = selected.getTop();
            int selectedBottom = selected.getBottom();

            // We are scrolled, don't get in the fade
            if (selectedTop < childrenTop) {
                selectedTop = childrenTop + getVerticalFadingEdgeLength();
            } else if (selectedBottom > childrenBottom) {
                selectedTop = childrenBottom - selected.getMeasuredHeight()
                        - getVerticalFadingEdgeLength();
            }
        } else {
            if (toPosition < firstPosition) {
                // Default to selecting whatever is first
                selectedPos = firstPosition;
                for (int i = 0; i < childCount; i++) {
                    final View v = getChildAt(i);
                    final int top = v.getTop();

                    if (i == 0) {
                        // Remember the position of the first item
                        selectedTop = top;
                        // See if we are scrolled at all
                        if (firstPosition > 0 || top < childrenTop) {
                            // If we are scrolled, don't select anything that is
                            // in the fade region
                            childrenTop += getVerticalFadingEdgeLength();
                        }
                    }
                    if (top >= childrenTop) {
                        // Found a view whose top is fully visisble
                        selectedPos = firstPosition + i;
                        selectedTop = top;
                        break;
                    }
                }
            } else {
                final int itemCount = mItemCount;
                down = false;
                selectedPos = firstPosition + childCount - 1;

                for (int i = childCount - 1; i >= 0; i--) {
                    final View v = getChildAt(i);
                    final int top = v.getTop();
                    final int bottom = v.getBottom();

                    if (i == childCount - 1) {
                        selectedTop = top;
                        if (firstPosition + childCount < itemCount || bottom > childrenBottom) {
                            childrenBottom -= getVerticalFadingEdgeLength();
                        }
                    }

                    if (bottom <= childrenBottom) {
                        selectedPos = firstPosition + i;
                        selectedTop = top;
                        break;
                    }
                }
            }
        }

        mResurrectToPosition = INVALID_POSITION;
        
        mTouchMode = TOUCH_MODE_REST;
        clearPullingCache();
        mSpecificTop = selectedTop;
        selectedPos = lookForSelectablePosition(selectedPos, down);
        if (selectedPos >= firstPosition && selectedPos <= getLastVisiblePosition()) {
            mLayoutMode = LAYOUT_SPECIFIC;
            updateSelectorState();
            setSelectionInt(selectedPos);
            //invokeOnItemScrollListener();
             
        } else {
            selectedPos = INVALID_POSITION;
        }
        //reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        
        return selectedPos >= 0;
    }
    
    void confirmCheckedPositionsById() {
        // Clear out the positional check states, we'll rebuild it below from IDs.
        mCheckStates.clear();

        boolean checkedCountChanged = false;
        for (int checkedIndex = 0; checkedIndex < mCheckedIdStates.size(); checkedIndex++) {
            final long id = mCheckedIdStates.keyAt(checkedIndex);
            final int lastPos = mCheckedIdStates.valueAt(checkedIndex);

            final long lastPosId = mAdapter.getItemId(lastPos);
            if (id != lastPosId) {
                // Look around to see if the ID is nearby. If not, uncheck it.
                final int start = Math.max(0, lastPos - CHECK_POSITION_SEARCH_DISTANCE);
                final int end = Math.min(lastPos + CHECK_POSITION_SEARCH_DISTANCE, mItemCount);
                boolean found = false;
                for (int searchPos = start; searchPos < end; searchPos++) {
                    final long searchId = mAdapter.getItemId(searchPos);
                    if (id == searchId) {
                        found = true;
                        mCheckStates.put(searchPos, true);
                        mCheckedIdStates.setValueAt(checkedIndex, searchPos);
                        break;
                    }
                }

                if (!found) {
                    mCheckedIdStates.delete(id);
                    checkedIndex--;
                    mCheckedItemCount--;
                    checkedCountChanged = true;
                    if (mChoiceActionMode != null && mMultiChoiceModeCallback != null) {
                        mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode,
                                lastPos, id, false);
                    }
                }
            } else {
                mCheckStates.put(lastPos, true);
            }
        }

        if (checkedCountChanged && mChoiceActionMode != null) {
            mChoiceActionMode.invalidate();
        }
    }
    
	 /**
     * Subclasses should NOT override this method but
     *  {@link #layoutChildren()} instead.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        
        mTop = this.getTop();
		mLeft = this.getLeft();
		mRight = this.getRight();
		mBottom = this.getBottom();

		mPaddingLeft = this.getPaddingLeft();
		mPaddingRight = this.getPaddingRight();
		mPaddingTop = this.getPaddingTop();
		mPaddingBottom = this.getPaddingBottom();

		mScrollX = this.getScrollX();
		mScrollY = this.getScrollY();
		
        mInLayout = true;

        if (changed) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).forceLayout();
            }
            mRecycler.markChildrenDirty();
        }
        
        layoutChildren();
        mInLayout = false;

        //mOverscrollMax = (b - t) / OVERSCROLL_LIMIT_DIVISOR;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSelector == null) {
            useDefaultSelector();
        }
        final Rect listPadding = mListPadding;
        listPadding.left = mSelectionLeftPadding + mPaddingLeft;
        listPadding.top = mSelectionTopPadding + mPaddingTop;
        listPadding.right = mSelectionRightPadding + mPaddingRight;
        listPadding.bottom = mSelectionBottomPadding + mPaddingBottom;

    }

	protected void layoutChildren() {

	}
	
	private boolean startPullIfNeeded(RectF tempRectF){
        final int deltaY = (int) (y - mMotionY);
        boolean ispull = false;

		if(isPulledMotion(mStartPullRectF,tempRectF,mTouch1DownTime)){
			final Handler handler = getHandler();

			// Handler should not be null unless the FoolAbsView is not attached to a
			// window, which would make it very hard to scroll it... but the monkeys
			// say it's possible.
	        if (handler != null) {
	        	handler.removeCallbacks(mPendingCheckForLongPress);
			}
			
		    setPressed(false);
		    View motionView = getChildAt(mPulledPosistion - mFirstPosition);
		    if (motionView != null) {
		        motionView.setPressed(false);
			}			 
		    // Time to start stealing events! Once we've stolen them, don't let anyone
	        // steal from us
		    final ViewParent parent = getParent();
            if (parent != null) {
	            parent.requestDisallowInterceptTouchEvent(true);
	        }		   	
	        mTouchMode  = TOUCH_MODE_PULL;
	        mMotionCorrection = deltaY > 0 ? mTouchSlop : -mTouchSlop;
		    
	        //这种控制方式前提是move的不能提前break;
	        
		    if(mPulledPosistion>0){        	
		   
		       //setPullTo(Math.abs(mMotionY-mInitY));			        
		    }
		    //smoothPullIfNeeded((int) y);
		    ispull = true;
		}else{
            mTouchMode  = TOUCH_MODE_DONE_WAITING;
            ispull = false;
        }
		return ispull;
	}
	
	void pullIfNeeded(int y){
		
		final int rawPullDelta =Math.abs(y-mMotionY);
		final int pullDelta = rawPullDelta -mMotionCorrection;
		final int pullDistance = Math.abs(y-mInitY);
		
		boolean pullingTag = pullDistance<mInnerWindowHeight/FRICTION?true:false;
//		
//		System.out.println("smoothPullIfNeeded rawPullDelta" + rawPullDelta);
//		System.out.println("smoothPullIfNeeded pullDelta" + pullDelta);
//		System.out.println("smoothPullIfNeeded pullDistance" + pullDistance);
//		System.out.println("smoothPullIfNeeded mInnerWindowHeight" + mInnerWindowHeight);
//		
		int incremetalPullDelta = mLastY != Integer.MIN_VALUE?Math.abs(y-mLastY):pullDelta;
		
		if(pullingTag){
			pullTo(incremetalPullDelta);
		}else {
			mTouchMode = TOUCH_MODE_OVERPULL;
		}
		mLastY = y;
	}
		
	protected boolean pullBack(int backDelta) {
		System.out.println("super smoothBackTo");
		return false;
	}
	
	/**
	 * 
	 * @param deltaY pulledPosition-1 pulledPosition+1 
	 * @param selectedPosition 
	 * @param duration 
	 * @param delayMillis postdelay time
	 * 
	 */
	protected boolean pullTo(int deltaY){
		createPullingCache();
		return false;
	}
	
    @Override
    public View getSelectedView() {
        if (mItemCount > 0 && mSelectedPosition >= 0) {
            return getChildAt(mSelectedPosition - mFirstPosition);
        } else {
            return null;
        }
    }
    
	public interface SelectionBoundsAdjuster {
        /**
         * 
         * Called to allow the list item to adjust the bounds shown for its selection.
	     *
	     * @param bounds On call, this contains the bounds the list has
	     * selected for the item (that is the bounds of the entire view).  The
	     * values can be modified as desired.
	     */
	     public void adjustListItemSelectionBounds(Rect bounds);
    }
	 
	/**
	 * 
	 * @param position
	 * @param sel
	 */
	void positionSelector(int position, View sel) {
        if (position != INVALID_POSITION) {
        	mSelectorPosition = position;
	    }
	 
        final Rect selectorRect = mSelectorRect; 
        
	    selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
        if (sel instanceof SelectionBoundsAdjuster) {
        	((SelectionBoundsAdjuster)sel).adjustListItemSelectionBounds(selectorRect);
	    }
	        
        positionSelector(selectorRect.left, selectorRect.top, selectorRect.right,
	                selectorRect.bottom);
	    final boolean isChildViewEnabled = mIsChildViewEnabled;
	    if (sel.isEnabled() != isChildViewEnabled) {
            mIsChildViewEnabled = !isChildViewEnabled;
            if (getSelectedItemPosition() != INVALID_POSITION) {
	            refreshDrawableState();
	        }
        }
	    
	}

    private void positionSelector(int l, int t, int r, int b) {
    	mSelectorRect.set(l - mSelectionLeftPadding, t - mSelectionTopPadding, r
	                + mSelectionRightPadding, b + mSelectionBottomPadding);
	}

	private void useDefaultSelector() {
		setSelector(getResources().getDrawable(
	               android.R.drawable.list_selector_background));
	}
	    
	/**
	 * Indicates whether this view is in a state where the selector should be drawn. This will
	 * happen if we have focus but are not in touch mode, or we are in the middle of displaying
	 * the pressed state for an item.
	 *
	 * @return True if the selector should be shown
	 */
	boolean shouldShowSelector() {
        return (hasFocus() && !isInTouchMode()) || touchModeDrawsInPressedState();
    }

	private void drawSelector(Canvas canvas) {
	    if (!mSelectorRect.isEmpty()) {
	        final Drawable selector = mSelector;
            selector.setBounds(mSelectorRect);
            selector.draw(canvas);
	    }
	}

   /**
    *  Controls whether the selection highlight drawable should be drawn on top of the item or behind it.
    *  @param onTop If true, the selector will be drawn on the item it is highlighting. The default 
    *  is false.
	*
    * @attr ref android.R.styleable#AbsFoolView_drawSelectorOnTop
    **/
    public void setDrawSelectorOnTop(boolean onTop) {
    	mDrawSelectorOnTop = onTop;
    }

   /**
    * Set a Drawable that should be used to highlight the currently selected item.
    *
    * @param resID A Drawable resource to use as the selection highlight.
    * @attr ref android.R.styleable#AbsFoolView_listSelector
	*/
	public void setSelector(int resID) {
		setSelector(getResources().getDrawable(resID));
	}

	public void setSelector(Drawable sel) {
		if (mSelector != null) {
			mSelector.setCallback(null);
	        unscheduleDrawable(mSelector);
	    }
	    mSelector = sel;
	    Rect padding = new Rect();
	    sel.getPadding(padding);
	    mSelectionLeftPadding = padding.left;
	    mSelectionTopPadding = padding.top;
	    mSelectionRightPadding = padding.right;
	    mSelectionBottomPadding = padding.bottom;
	    sel.setCallback(this);
        updateSelectorState();
    }

	/*** Returns the selector {@link android.graphics.drawable.Drawable} that is used to draw the
	 * selection in the list.
     *
     * @return the drawable used to display the selector
	 */
	public Drawable getSelector() {
	    return mSelector;
	}
	    
	void updateSelectorState() {
        if (mSelector != null) {
        	if (shouldShowSelector()) {
	            mSelector.setState(getDrawableState());
	        } else {
                mSelector.setState(StateSet.NOTHING);
            }
	    }
	}

    /**
     * @return True if the current touch mode requires that we draw the selector in the pressed
     *         state.
     */
	boolean touchModeDrawsInPressedState() {
        // FIXME use isPressed for this
		switch (mTouchMode) {
	        case TOUCH_MODE_TAP:
	        case TOUCH_MODE_DONE_WAITING:
	            return true;
	        default:
	            return false;
	     }
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent ev) {
		if (!isEnabled()) {
	        // A disabled view that is clickable still consumes the touch
			// events, it just doesn't respond to them.
			return isClickable() || isLongClickable();
	    }
		return onTouchMultiSpecialHoverEvent(ev);
    }
	
	private boolean receiveMotionEvent(MotionEvent event){
		final int action = event.getActionMasked();
		float sumX = 0 ,sumY = 0;
		int pointCount = event.getPointerCount();

		if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {			
			if(pointCount>=0){
				final boolean pointerUp = action == MotionEvent.ACTION_POINTER_UP;

				final int skipIndex = pointerUp ?event.getActionIndex() : -1;
				final int count = event.getPointerCount();
				//Determine focal point
				for(int i = 0;i<count;i++){
					if(skipIndex == i)
						continue;
					sumX += event.getX(i);
					sumY += event.getY(i);
				}

				final int div = pointerUp?pointCount-1:pointCount;
				x = sumX/div;
				y = sumY/div;
				mMotionPosition = pointToPosition((int)x, (int)y);
			}
	        return true;
	    }else {
			return false;
		}
	}		
    
	private boolean onTouchMultiSpecialHoverEvent(MotionEvent event) {
    	final int action = event.getActionMasked();            
    	View view;
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        
    	switch (action) {
    	case  MotionEvent.ACTION_DOWN:{
    		
    		if(receiveMotionEvent(event)){
    			mInitX = (int)x;
    	        mInitY = (int)y;    
    		}

    		mTouch1DownTime = System.currentTimeMillis();
     		
    		switch (mTouchMode) {
	    		default: {
	    			
	    			int motionPosition = pointToPosition((int)x,(int) y);
	    			mActivePointerId = event.getPointerId(0);
	    			
	    			if (!mDataChanged) {
	    				//out of pull-status
	    		    	if ((mLastPullMode != 1)&&(mTouchMode != TOUCH_MODE_PULLING) && (motionPosition >= 0) && (getAdapter().isEnabled(motionPosition))) {
	    		           // User clicked on an actual view (and was not stopping a fling).
	    		           // It might be a click or a scroll. Assume it is a click until
	                       // proven otherwise
	    		    	   
	    		    	   mTouchMode = TOUCH_MODE_DOWN;
	
	    		    	   // FIXME Debounce
	    		           if (mPendingCheckForTap == null) {
	    		        	   mPendingCheckForTap = new CheckForTap();
	    		           }
	                       postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
	    		        }
	    		    }
	
	    		    if (motionPosition >= 0) {
	    		    	// Remember where the motion event started
	    		        view = getChildAt(motionPosition - mFirstPosition);
	    		        if(view!=null)
	    		        mMotionViewOriginalTop = view.getTop();
	    	        }
	    		    
	    		    mMotionX = (int)x;	    		    
	    	        mMotionY = (int)y;    	        
	    		    mMotionPosition = motionPosition;
	                mLastX = Integer.MIN_VALUE;                
	                mLastY = Integer.MIN_VALUE;	                               
	            }
	    	}
    		
    		mTouchMode = TOUCH_MODE_DOWN;
    		break;    		
    	}

    	case MotionEvent.ACTION_MOVE:{
    		
    		if (mDataChanged) {
    			// Re-sync everything if data has been changed
    			// since the scroll operation can query the adapter.
                layoutChildren();
            }

	    	receiveMotionEvent(event);
	    	
	    	if(event.getPointerCount()>1){
	    		//BUG
	    		tempPulledRectF = getRectF(event.getX(mSecondActiveId-1),event.getY(mSecondActiveId-1), event.getX(mSecondActiveId), event.getY(mSecondActiveId));
	    	}
	    	
    		switch (mTouchMode) {
    			case TOUCH_MODE_DOWN:
    			case TOUCH_MODE_TAP:
    			case TOUCH_MODE_DONE_WAITING:{
    				
    				if(!mDataChanged&&action!=MotionEvent.ACTION_CANCEL&&action!=MotionEvent.ACTION_UP){
    					boolean handlePulled = false;
        				
    					if(canPull&&tempPulledRectF!=null){
    						handlePulled = startPullIfNeeded(tempPulledRectF);
    					}
       				
        				//check if we have moved far enough that it looks more like a scroll than a tap item scroll
        				//in pull-status ,can not scroll item
    					if((mLastPullMode != 1)&&!handlePulled&&mLayoutMode!=LAYOUT_PULLING){
        					System.out.println("mPullMode " + mCurrentPullMode);
        					
    						startScrollItemIfNeeded((int)x,(int)y);
        				}
        			}
        			break;
    			}
    			case TOUCH_MODE_PULL:
    				switch (mCurrentPullMode) {
					case PULL_OUT:
						if(mAboverPulledViewInfos.isEmpty()&&mBelowerPulledViewInfos.isEmpty()&&mPulledPosistion<0){
							mPulledPosistion = pointToPosition(mInitX, mInitY);
							prepareInnerWindow();
						}else {
							mTouchMode = TOUCH_MODE_PULLING;
						}
					break;
					case PULL_IN:
						mTouchMode = TOUCH_MODE_OVERPULL;
					break;
					}
    			break;
    			case TOUCH_MODE_PULLING:
    				//No-repeat 
    				if(mCurrentPullMode != mLastPullMode){
    					System.out.println("mCurrentPullMode " + mCurrentPullMode);
    					System.out.println("mLastPullMode " + mLastPullMode);
    					
    					pullIfNeeded((int)y);
    				}
    	    	break;
    			case TOUCH_MODE_OVERPULL:
    				if(mCurrentPullMode != mLastPullMode&&mLastPullMode == PULL_OUT){
    					closeInnerWindow();
    				}
    				break;
    			case TOUCH_MODE_SLIDE:
    			case TOUCH_MODE_OVERSLIDE:
    				scrollItemIfNeed((int)x);
    			break;
    		}
    	}
    	break;
    	case MotionEvent.ACTION_UP:
    	{
    		/*test*/  	
    		System.out.println("ACTION_UP");
    		System.out.println("mTouchMode " +mTouchMode);
    		System.out.println("mLayoutMode " +mLayoutMode);
    		System.out.println("mCurrentPullMode " +mCurrentPullMode);
    		System.out.println("mLastPullMode " +mLastPullMode);
    		
    		switch (mTouchMode) {
    		
	    		case TOUCH_MODE_DOWN:
	    		case TOUCH_MODE_TAP:
	  			case TOUCH_MODE_DONE_WAITING:
	  				final int motionPosition = mMotionPosition;
	  				final View childView = getChildAt(motionPosition-mFirstPosition);
	  				
	  				final float x0 = event.getX();
	  				final boolean inList = x0 > mListPadding.left && x0 < getWidth() - mListPadding.right;

	    			if (childView != null && !childView.hasFocusable() && inList) {
	    				if (mTouchMode != TOUCH_MODE_DOWN) {
	    		       	   childView.setPressed(false);
	    				}
	    				if (mPerformClick == null) {
	    	               mPerformClick = new PerformClick();
	    	            }

	    			    final PerformClick performClick = mPerformClick;
	    			       performClick.mClickMotionPosition = motionPosition;
	    			       performClick.rememberWindowAttachCount();
	    			       mResurrectToPosition = motionPosition;

	    			       if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
	    			    	   final Handler handler = getHandler();
	    			           if (handler != null) {
	    			           	  handler.removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ?mPendingCheckForTap : mPendingCheckForLongPress);
	    			           }

	    			           if(mLayoutMode!=LAYOUT_SPOT){
	    							mLayoutMode = LAYOUT_PREVIEW;
	    			           }

	    			           if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
	    			        	   mTouchMode = TOUCH_MODE_TAP;
	    			               setSelectedPositionInt(mMotionPosition);
	    			               layoutChildren();
	    			               childView.setPressed(true);
	    			               positionSelector(mMotionPosition, childView);
	    			               setPressed(true);
	    			               if (mSelector != null) {
	    			            	   Drawable d = mSelector.getCurrent();
	    			                   if (d != null && d instanceof TransitionDrawable) {
	    			                      ((TransitionDrawable) d).resetTransition();
	    			                    }
	    			               }
	    			               if (mTouchModeReset != null) {
	    			            	   	removeCallbacks(mTouchModeReset);
	    			               }

		    			           if(mPendingCheckForLongPress!=null){
		    			       			mPendingCheckForLongPress.setAlphaRunnableState(STATE_ALPHA_EXIT);
		    			       	   }
	    			       		
	    			               mTouchModeReset = new Runnable() {
	    			                  @Override
	    			                  public void run() {
	    	                              mTouchModeReset = null;
	    	                              mTouchMode = TOUCH_MODE_REST;
	    			                      childView.setPressed(false);
	    			                      setPressed(false);
	    			                      if (!mDataChanged) {
	    			                     	performClick.run();
	    			                      }
	    			                  }
	    			               };
	    			               postDelayed(mTouchModeReset, ViewConfiguration.getPressedStateDuration());

	    			           } else {
	    			               mTouchMode = TOUCH_MODE_REST;
	    			               updateSelectorState();
	    			           }
	    			           return true;

	    			        } else if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
	    			        	performClick.run();
	    			        }
	    			}
	    			mTouchMode = TOUCH_MODE_REST;
	                updateSelectorState();
					break;
	  			case TOUCH_MODE_PULL:
	  			case TOUCH_MODE_PULLING:
	  			case TOUCH_MODE_OVERPULL:
	  			{
	  				if(mCurrentPullMode!=mLastPullMode) {
	  					if(mRawPullDistance>0){
		  					//大于，则返回
		  					pullBack(mRawPullDistance);
		  				}
		  				if(mRawPullDistance<0){
		  					//小于，不够，则直至
		  					pullBack(mRawPullDistance);
		  				}
	  				} 
	  				
	  				mLastPullMode = mCurrentPullMode;
	  				canPull = false;
	  				
	  				break;
                }
	  			
	  			case TOUCH_MODE_SLIDE:
	  			case TOUCH_MODE_OVERSLIDE:{
	  				if(mPerformItemSlide!=null){
	  					mPerformItemSlide.setSlideState(ItemSlide.SLIDE_EXIT);
	  					postDelayed(mPerformItemSlide, SLIDING_TIME);
	  				}
	  				
	  				if(mSpotSlide!=null){
	  					final SpotSlide spotSlide = mSpotSlide;
	  					spotSlide.mSpotSlidePosistion = mMotionPosition;
	  					mTouchModeReset = new Runnable() {
			                  @Override
			                  public void run() {
		                          mTouchModeReset = null;
		                          mTouchMode = TOUCH_MODE_REST;
				                  setPressed(false);
				                  if (!mDataChanged) {
				                      spotSlide.run();
				                  }
				                  removeCallbacks(mPerformItemSlide);
			                  }
			            };
			            postDelayed(mTouchModeReset,SLIDING_TIME);
	  				}
	  				break;
	  			 }
	  			default:
	  				if(isSpot&&mSpotPosition>=0){
	  					mTouchModeReset = new Runnable() {
			                  @Override
			                  public void run() {
	                        mTouchModeReset = null;
	                        mTouchMode = TOUCH_MODE_REST;
			                      setPressed(false);
			                      if (!mDataChanged) {
			                    	  if(mSpotNext!=null)
			                    	  postDelayed(mSpotNext,100);
			                      }
			                  }
			            };
			            postDelayed(mTouchModeReset, ViewConfiguration.getPressedStateDuration());	
	  				}
	  			break;
    		}
    		
    		
    		setPressed(false);
    		if (mEdgeGlowTop != null) {
    		    mEdgeGlowTop.onRelease();
    		    mEdgeGlowBottom.onRelease();
    		}
    		
            // Need to redraw since we probably aren't drawing the selector anymore
    		invalidate();
    		final Handler handler = getHandler();
    		if (handler != null) {
                handler.removeCallbacks(mPendingCheckForLongPress);
            }
    		
    		recycleVelocityTracker();
    		mActivePointerId = INVALID_POINTER;
    		mSecondActiveId = INVALID_POINTER;
    		//canPull = false;
    		break; 
    	}
    	case MotionEvent.ACTION_POINTER_UP:{
    		
    		onSecondaryPointerUp(event);	
            final int x = mMotionX;
            final int y = mMotionY;
            final int motionPosition = pointToPosition(x, y);
    		if (motionPosition >= 0) {
    			// Remember where the motion event started
    			view= getChildAt(motionPosition - mFirstPosition);
    			mMotionViewOriginalTop = view.getTop();
    			mMotionPosition = motionPosition;
    		}
    		mLastY = mMotionY;
    		break;
    	}

    	case MotionEvent.ACTION_POINTER_DOWN:{
    		
    		//For ACTION_POINTER_DOWN or ACTION_POINTER_UP as returned by getActionMasked(), 
    		//this returns the associated pointer index. The index may be used with 
    		//getPointerId(int), getX(int), getY(int), getPressure(int), and getSize(int) to 
    		//get information about the pointer that has gone down or up.
    		
    		final int index = event.getActionIndex();
    		mSecondActiveId = event.getPointerId(index)>0?1:event.getPointerId(index);
    						
			if(mSecondActiveId>0&&!canPull){
				canPull = true;
				mStartPullRectF = getRectF(event.getX(mSecondActiveId-1),event.getY(mSecondActiveId-1), event.getX(mSecondActiveId), event.getY(mSecondActiveId));		
			}
    		
    		//  no pull, New pointers take over dragging duties
    		
    		if(!canPull){
    			mActivePointerId = mSecondActiveId;
                mMotionX = (int)event.getX(mSecondActiveId);
                mMotionY = (int)event.getY(mSecondActiveId);	
                
                mMotionCorrection = 0;
        		
                final int motionPosition = pointToPosition(mMotionX, mMotionY);
        		if (motionPosition >= 0) {
        			// Remember where the motion event started
        			view = getChildAt(motionPosition - mFirstPosition);
        			mMotionViewOriginalTop = view.getTop();
        			mMotionPosition = motionPosition;
        		}
        		
                mLastY = mMotionY;
    		}
    		break;
    	}

    	case MotionEvent.ACTION_CANCEL: {
    		switch (mTouchMode) {	    			
	    		default:
	    			mTouchMode = TOUCH_MODE_REST;		
	    		    setPressed(false);
	    		    View motionView = this.getChildAt(mMotionPosition - mFirstPosition);
	    		    if (motionView != null) {
	    		    	motionView.setPressed(false);
	    		    }
	    		    clearPullingCache();
	    		    
	    		    final Handler handler = getHandler();
	    		    if (handler != null) {
	    		        handler.removeCallbacks(mPendingCheckForLongPress);
	    		    }
	
	    		    recycleVelocityTracker();
	    		 }
	
	    		 if (mEdgeGlowTop != null) {
	    			 mEdgeGlowTop.onRelease();
	    		     mEdgeGlowBottom.onRelease();
	    		 }

	    		 //mSecondActiveId = INVALID_POINTER;
	    		 mActivePointerId = INVALID_POINTER;
	    		 break;
	    	}
    	    //add case Touch_mode_OverPull and touch_mode_overslide
    		//clear cache and remove callback
    	}

    	return true;
    }
    
    /**
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return RectF
     */
    private RectF getRectF(float x1,float y1,float x2,float y2){
    	return new RectF(Math.min(x1,x2),Math.min(y1, y2),Math.max(x1, x2),Math.max(y1, y2));
    }

    private boolean isPulledMotion(RectF r0,RectF r,long startMotionTime){
		
		boolean ispull = false;
	    
		long now = System.currentTimeMillis();
   		
   		if(now>ViewConfiguration.getTapTimeout()+startMotionTime&&now>ViewConfiguration.getJumpTapTimeout()+startMotionTime){  			
   			//System.out.println("isPulledMotion");
   			// the specified rectangle r is inside or equal to this rectangle.
   			if(r.bottom>r0.bottom&&r.top<r0.top){
   				ispull = true;
   				mCurrentPullMode = PULL_OUT;
   			}else if(r.bottom<r0.bottom&&r.top>r0.top){
   				ispull = true;
   				mCurrentPullMode = PULL_IN;
   			}
   		}else {
   			//System.out.println("NO PulledMotion"); 			
		}
   		
		return ispull;
	}
	/**
	 * @return boolean
     */
    private boolean startScrollItemIfNeeded(int x,int y){
	
    	mScrollX = getScrollX();
    	
		final int deltaX = x -mMotionX;
		final int deltaY = y -mMotionY; //
		final int distanceX = Math.abs(deltaX);
		final int distanceY = Math.abs(deltaY);
		final boolean overscroll = mScrollX!=0;
		
		mMotionCorrection = deltaX>0?mTouchSlop:-mTouchSlop;
		if(overscroll){ mMotionCorrection = 0;}
				
		if(overscroll||(distanceX>mTouchSlop&&(distanceY<=mTouchSlop))){
			
			mTouchMode = TOUCH_MODE_SLIDE;
			
			if(deltaX>0){
				this.mSlideMode = TOUCH_SLIDE_LEFT;
			}else if(deltaX <0) {
				this.mSlideMode = TOUCH_SLIDE_RIGHT;
			}
			scrollItemIfNeed(x);
			return true;
		}else{
			mTouchMode = TOUCH_MODE_DONE_WAITING;
			return false;
		}
	} 

    /**
     * @return int
     */
    protected int getIncrementalDeltaX(){
        return incrementalDeltaX;
    }

    /**
     * @param x
     */
    protected void setIncrementalDeltaX(int x){
        this.incrementalDeltaX = x;
    }

    /**
     * child view scroll x
     * @param x
     */
	private void scrollItemIfNeed(int x){
		
		final int rawDeltaX = x - mMotionX;//
		final int deltaX3 = rawDeltaX - mMotionCorrection;
		int incrementalDeltaX = mLastX!=Integer.MIN_VALUE?x - mLastX:deltaX3;
		
		if(!mDataChanged&&mMotionPosition >= 0){
			if(mPerformItemSlide==null){
                mPerformItemSlide = new PerformItemSlide();
            }
			 
            if(mTouchMode == TOUCH_MODE_SLIDE){
            	
            	isSliding = true;
                       
                final Handler handler = getHandler();
                // Handler should not be null unless the AbsFoolView is not attached to a
                // window, which would make it very hard to scroll it... but the monkeys
                // say it's possible.
                if (handler != null) {
                    handler.removeCallbacks(mPendingCheckForLongPress);
                }
                setPressed(false);

                int	motionIndex = mMotionPosition - mFirstPosition;
                View motionView = getChildAt(motionIndex);
                
                if (motionView != null) {
                    motionView.setPressed(false);
                }                				               
            }else if(mTouchMode == TOUCH_MODE_OVERSLIDE){
                if (x != mLastX) {
                    final int oldScroll = mScrollX;
                    final int newScroll = oldScroll - incrementalDeltaX;
                    int newDirection = x > mLastX ? 1 : -1;

                    if (mDirection  == 0) {
                        mDirection = newDirection;
                    }

                    int overScrollDistance = -incrementalDeltaX;
                    if ((newScroll < 0 && oldScroll >= 0) || (newScroll > 0 && oldScroll <= 0)) {
                        overScrollDistance = -oldScroll;
                        incrementalDeltaX += overScrollDistance;
                    } else {
                        incrementalDeltaX = 0;
                    }                    
                }
            }
            
            if(mPerformItemSlide!=null){
            	mPerformItemSlide.deltaX = incrementalDeltaX;
            	mPerformItemSlide.setSlideState(ItemSlide.SLIDE_IN);
              	postDelayed(mPerformItemSlide, SLIDING_TIME);
            }

        }else{
            mTouchMode = TOUCH_MODE_DONE_WAITING;
        }
	}
	
	/**
	 */
	private void createPullingCache() {
	    if (mPullingCacheEnabled && !mCachingStarted && !isHardwareAccelerated()) {
	        setChildrenDrawnWithCacheEnabled(true);
	        setChildrenDrawingCacheEnabled(true);
	        mCachingStarted = mCachingActive = true;
	    }
	}
	
    /**
     */
    private void clearPullingCache() {
        if (!isHardwareAccelerated()) {
            if (mClearPullingCache == null) {
            	mClearPullingCache = new Runnable() {

					public void run() {
                        if (mCachingStarted) {
                            mCachingStarted = mCachingActive = false;
                            setChildrenDrawnWithCacheEnabled(false);
                            if ((mPersistentDrawingCache & PERSISTENT_SCROLLING_CACHE) == 0) {
                                setChildrenDrawingCacheEnabled(false);
                            }
                            if (!isAlwaysDrawnWithCacheEnabled()) {
                                invalidate();
                            }
                        }
                    }
                };
            }
            post(mClearPullingCache);
        }
    }
    
    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	 int action = ev.getAction();
         View v;

         if (!mIsAttached) {
             // Something isn't right.
             // Since we rely on being attached to get data set change notifications,
             // don't risk doing anything where we might try to resync and find things
             // in a bogus state.
             return false;
         }

         switch (action & MotionEvent.ACTION_MASK) {
         case MotionEvent.ACTION_DOWN: {
             int touchMode = mTouchMode;
             if (touchMode == TOUCH_MODE_PULL) {
            	 // if (touchMode == TOUCH_MODE_PULL || touchMode == TOUCH_MODE_OVERPULLING) {
                 mMotionCorrection = 0;
                 return true;
             }

             final int x = (int) ev.getX();
             final int y = (int) ev.getY();
             mActivePointerId = ev.getPointerId(0);
            
             int motionPosition = findMotionRow(y);
             //if (touchMode != TOUCH_MODE_OVERPULLING && motionPosition >= 0) {
             if ( motionPosition >= 0) {
                 // User clicked on an actual view (and was not stopping a fling).
                 // Remember where the motion event started
                 v = getChildAt(motionPosition - mFirstPosition);
                 mMotionViewOriginalTop = v.getTop();
                 mMotionX = x;
                 mMotionY = y;
                 mMotionPosition = motionPosition;
                 mTouchMode = TOUCH_MODE_DOWN;
                 clearPullingCache();
             }
             mLastY = Integer.MIN_VALUE;
             initOrResetVelocityTracker();
             mVelocityTracker.addMovement(ev);
             if (touchMode == TOUCH_MODE_PULL||touchMode == TOUCH_MODE_PULLING) {
                 return true;
             }
             break;
         }

         case MotionEvent.ACTION_MOVE: {
             switch (mTouchMode) {
             case TOUCH_MODE_DOWN:
                 int pointerIndex = ev.findPointerIndex(mActivePointerId);
                 if (pointerIndex == -1) {
                     pointerIndex = 0;
                     mActivePointerId = ev.getPointerId(pointerIndex);
                 }
                 
                 initVelocityTrackerIfNotExists();
                 mVelocityTracker.addMovement(ev);
                 
                 //=================================================================================
                
                 break;
             }
             break;
         }

         case MotionEvent.ACTION_CANCEL:
         case MotionEvent.ACTION_UP: {
             mTouchMode = TOUCH_MODE_REST;
             mActivePointerId = INVALID_POINTER;
             recycleVelocityTracker();
            
             //======================================================================================
             
             break;
         }

         case MotionEvent.ACTION_POINTER_UP: {
        	 //mActivePointsNum = ev.getPointerCount();
        	 onSecondaryPointerUp(ev);
             break;
         }

         }

         return false;
    }
    private void onSecondaryPointerUp(MotionEvent ev) {
    	
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);

        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            
            mMotionX = (int) ev.getX(newPointerIndex);
            mMotionY = (int) ev.getY(newPointerIndex);
            mMotionCorrection = 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }

        System.out.println("onSecondaryPointerUp");
        
    }

    /**
     * Get a view and have it show the data associated with the specified
     * position. This is called when we have already discovered that the view is
     * not available for reuse in the recycle bin. The only choices left are
     * converting an old view or making a new one.
     *
     * @param position The position to display
     * @param isMeasuredAndUnused Array of at least 1 boolean, the first entry will become true if
     *                the returned view was taken from the measuredAndUnused heap, false if otherwise.
     *
     * @return A view displaying the data associated with the specified position
     */
    View obtainMeasuredAndUnusedViewToUse(int position, boolean[] isMeasuredAndUnused) {

        isMeasuredAndUnused[0] = false;
        View measuredAndUnusedView;

        measuredAndUnusedView = mRecycler.getTransientStateView(position);
        if (measuredAndUnusedView != null) {
            return measuredAndUnusedView;
        }

        measuredAndUnusedView = mRecycler.getMeasuredAndUnusedView(position);

        View child;
        if (measuredAndUnusedView != null) {

            child = mAdapter.getView(position, measuredAndUnusedView, this);

            if (child.getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            if (child != measuredAndUnusedView) {
                mRecycler.addMeasuredAndUnusedView(measuredAndUnusedView, position);
                if (mCacheColorHint != 0) {
                    child.setDrawingCacheBackgroundColor(mCacheColorHint);
                }
            } else {
                isMeasuredAndUnused[0] = true;
                child.onStartTemporaryDetach();
            }
        } else {
            child = mAdapter.getView(position, null, this);

            if (child.getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            if (mCacheColorHint != 0) {
                child.setDrawingCacheBackgroundColor(mCacheColorHint);
            }
        }

        if (mAdapterHasStableIds) {
            final ViewGroup.LayoutParams vlp = child.getLayoutParams();
            LayoutParams lp;
            if (vlp == null) {
                lp = (LayoutParams) generateDefaultLayoutParams();
            } else if (!checkLayoutParams(vlp)) {
                lp = (LayoutParams) generateLayoutParams(vlp);
            } else {
                lp = (LayoutParams) vlp;
            }
            lp.itemId = mAdapter.getItemId(position);
            child.setLayoutParams(lp);
        }

        return child;
    }

    /**
     * Maps a point to a position in the list.
     *
     * @param x X in local coordinate
     * @param y Y in local coordinate
     * @return The position of the item which contains the specified point, or
     *         {@link #INVALID_POSITION} if the point does not intersect an item.
     */
    public int pointToPosition(int x, int y) {
        Rect frame = mTouchFrame;
        if (frame == null) {
            mTouchFrame = new Rect();
            frame = mTouchFrame;
        }

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            //System.out.println(child.toString());

            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return mFirstPosition + i;
                }
            }
        }
        return INVALID_POSITION;
    }

    /**
     * Maps a point to a the rowId of the item which intersects that point.
     *
     * @param x X in local coordinate
     * @param y Y in local coordinate
     * @return The rowId of the item which contains the specified point, or {@link #INVALID_ROW_ID}
     *         if the point does not intersect an item.
     */
    public long pointToRowId(int x, int y) {
        int position = pointToPosition(x, y);
        if (position >= 0) {
            return mAdapter.getItemId(position);
        }
        return INVALID_ROW_ID;
    }
    
    /**
     * Find the row closest to y. This row will be used as the motion row when scrolling
     *
     * @param y Where the user touched
     * @return The position of the first (or only) item in the row containing y
     */
    abstract int findMotionRow(int y);
    
    class MultiChoiceModeWrapper implements MultiChoiceModeListener {
        private MultiChoiceModeListener mWrapped;

        public void setWrapped(MultiChoiceModeListener wrapped) {
            mWrapped = wrapped;
        }

        public boolean hasWrappedCallback() {
            return mWrapped != null;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mWrapped.onCreateActionMode(mode, menu)) {
                // Initialize checked graphic state?
                setLongClickable(false);
                return true;
            }
            return false;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mChoiceActionMode = null;

            // Ending selection mode means deselecting everything.
            clearChoices();

            mDataChanged = true;
            rememberSyncState();
            requestLayout();

            setLongClickable(true);
        }

        public void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked) {
            mWrapped.onItemCheckedStateChanged(mode, position, id, checked);

            // If there are no items selected we no longer need the selection mode.
            if (getCheckedItemCount() == 0) {
                mode.finish();
            }
        }
    }

    /**
     * Returns the number of items currently selected. This will only be valid
     * if the choice mode is not {@link #CHOICE_MODE_NONE} (default).
     *
     * <p>To determine the specific items that are currently selected, use one of
     * the <code>getChecked*</code> methods.
     *
     * @return The number of items currently selected
     *
     */
    public int getCheckedItemCount() {
        return mCheckedItemCount;
    }
    
    /**
     * Clear any choices previously set
     */
    public void clearChoices() {
        if (mCheckStates != null) {
            mCheckStates.clear();
        }
        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
        mCheckedItemCount = 0;
    }

    /**
     * A MultiChoiceModeListener receives events for {@link android.widget.AbsFoolView#CHOICE_MODE_MULTIPLE_MODAL}.
     * It acts as the {@link android.view.ActionMode.Callback} for the selection mode and also receives
     * {@link #onItemCheckedStateChanged(android.view.ActionMode, int, long, boolean)} events when the user
     * selects and deselects list items.
     */
    public interface MultiChoiceModeListener extends ActionMode.Callback {
        /**
         * Called when an item is checked or unchecked during selection mode.
         *
         * @param mode The {@link android.view.ActionMode} providing the selection mode
         * @param position Adapter position of the item that was checked or unchecked
         * @param id Adapter ID of the item that was checked or unchecked
         * @param checked <code>true</code> if the item is now checked, <code>false</code>
         *                if the item is now unchecked.
         */
        public void onItemCheckedStateChanged(ActionMode mode,
                                              int position, long id, boolean checked);
    }
    
    /**
     * Set a {@link AbsFoolView.MultiChoiceModeListener} that will manage the lifecycle of the
     * selection {@link android.view.ActionMode}. Only used when the choice mode is set to
     * {@link #CHOICE_MODE_MULTIPLE_MODAL}.
     *
     * @param listener Listener that will manage the selection mode
     *
     */
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        if (mMultiChoiceModeCallback == null) {
            mMultiChoiceModeCallback = new MultiChoiceModeWrapper();
        }
        mMultiChoiceModeCallback.setWrapped(listener);
    }
    
    final class CheckForTap implements Runnable {
        public void run() {
        	System.out.println("CheckForTap -->");
        	System.out.println("CheckForTap " + mTouchMode );
        	
            if (mTouchMode == TOUCH_MODE_DOWN) {
                mTouchMode = TOUCH_MODE_TAP;
                final View child = getChildAt(mMotionPosition - mFirstPosition);
                System.out.println(mMotionPosition);
                if (child != null && !child.hasFocusable()) {
                    if(mLayoutMode!=LAYOUT_SPOT)
                	mLayoutMode = LAYOUT_PREVIEW;

                    if (!mDataChanged) {
                    	
                        child.setPressed(true);
                        setPressed(true);
                        layoutChildren();
                        positionSelector(mMotionPosition, child);
                        refreshDrawableState();

                        final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
                        final boolean longClickable = isLongClickable();

                        if (mSelector != null) {
                            Drawable d = mSelector.getCurrent();
                            if (d != null && d instanceof TransitionDrawable) {
                                if (longClickable) {
                                    ((TransitionDrawable) d).startTransition(longPressTimeout);
                                } else {
                                    ((TransitionDrawable) d).resetTransition();
                                }
                            }
                        }

                        if (longClickable) {
                        	if (mPendingCheckForLongPress == null) {
                                mPendingCheckForLongPress = new CheckForLongPress();
                            }
                            mPendingCheckForLongPress.rememberWindowAttachCount();
                            postDelayed(mPendingCheckForLongPress, longPressTimeout);
                            
                        } else {
                            mTouchMode = TOUCH_MODE_DONE_WAITING;
                        }
                    } else {
                        mTouchMode = TOUCH_MODE_DONE_WAITING;
                    }
                }
            }
        }
    }

    /**
     * A base class for Runnables that will check that their view is still attached to
     * the original window as when the Runnable was created.
     *
     */
    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return hasWindowFocus() && getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        int mClickMotionPosition;

        public void run() {
            // The data has changed since we posted this action in the event queue,
            // bail out before bad things happen
            if (mDataChanged) return;

            final ListAdapter adapter = mAdapter;
            final int motionPosition = mClickMotionPosition;
            if (adapter != null && mItemCount > 0 &&motionPosition != INVALID_POSITION && motionPosition < adapter.getCount() && sameWindow()) {
                final View view = getChildAt(motionPosition - mFirstPosition);
                // If there is no view, something bad happened (the view scrolled off the
                // screen, etc.) and we should cancel the click
                if (view != null) {
                    performItemClick(view, motionPosition, adapter.getItemId(motionPosition));
                }
            }
        }
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        boolean handled = false;
        boolean dispatchItemClick = true;

        if (mChoiceMode != CHOICE_MODE_NONE) {
            handled = true;
            boolean checkedStateChanged = false;

            if (mChoiceMode == CHOICE_MODE_MULTIPLE ||
                    (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode != null)) {
                boolean checked = !mCheckStates.get(position, false);
                mCheckStates.put(position, checked);
                if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                    if (checked) {
                        mCheckedIdStates.put(mAdapter.getItemId(position), position);
                    } else {
                        mCheckedIdStates.delete(mAdapter.getItemId(position));
                    }
                }
                if (checked) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
                if (mChoiceActionMode != null) {
                    mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode,
                            position, id, checked);
                    dispatchItemClick = false;
                }
                checkedStateChanged = true;
            } else if (mChoiceMode == CHOICE_MODE_SINGLE) {
                boolean checked = !mCheckStates.get(position, false);
                if (checked) {
                    mCheckStates.clear();
                    mCheckStates.put(position, true);
                    if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                        mCheckedIdStates.clear();
                        mCheckedIdStates.put(mAdapter.getItemId(position), position);
                    }
                    mCheckedItemCount = 1;
                } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                    mCheckedItemCount = 0;
                }
                checkedStateChanged = true;
            }

            if (checkedStateChanged) {
                updateOnScreenCheckedViews();
            }
        }

        if (dispatchItemClick) {
            handled |= super.performItemClick(view, position, id);
        }

        return handled;
    }

    /**
     * Perform a quick, in-place update of the checked or activated state
     * on all visible item views. This should only be called when a valid
     * choice mode is active.
     */
    private void updateOnScreenCheckedViews() {
        final int firstPos = mFirstPosition;
        final int count = getChildCount();
        final boolean useActivated = getContext().getApplicationInfo().targetSdkVersion
                >= android.os.Build.VERSION_CODES.HONEYCOMB;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int position = firstPos + i;

            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(mCheckStates.get(position));
            } else if (useActivated) {
                child.setActivated(mCheckStates.get(position));
            }
        }
    }
	
    private static final int STATE_ALPHA_NONE = 0;
    private static final int STATE_ALPHA_IN = 1;
    private static final int STATE_ALPHA_EXIT = 2;       
	
    private class CheckForLongPress extends WindowRunnnable implements Runnable {   
        private static final long ALPHA_TIME = 10;
       
        
        private ItemViewAlphaRunnable mItemAlphaRunnable;   
    	private float mOriginalAlpha = 0;
    	private Handler mHandler = new Handler();
    	private View alphaView;
        private int mState;
        
        boolean handled = false;
    	
        public void run() {

            final int motionPosition = mMotionPosition;   	
            final int longPressPosition = mMotionPosition;
            final long longPressId = mAdapter.getItemId(mMotionPosition);
            
        	alphaView = getChildAt(motionPosition-mFirstPosition);
        	
            if (alphaView != null) {
            	performAlpha();
                
                if (sameWindow() && !mDataChanged) {
                    handled = performLongPress(alphaView, longPressPosition, longPressId);
                }                
                
                if (handled) {
                    mTouchMode = TOUCH_MODE_REST;
                    setPressed(false);
                    alphaView.setPressed(false);
                } else {
                    mTouchMode = TOUCH_MODE_DONE_WAITING;
                }
            }
        }
        	
    	private void performAlpha(){
    		if(mItemAlphaRunnable!=null){
				setAlphaRunnableState(STATE_ALPHA_NONE);
	   			mHandler.removeCallbacks(mItemAlphaRunnable);
	   		}else {
	   			mItemAlphaRunnable = new ItemViewAlphaRunnable();
			}
    		
	   		mOriginalAlpha = alphaView.getAlpha();
            mItemAlphaRunnable.initAlpha();
            mHandler.postDelayed(mItemAlphaRunnable, ALPHA_TIME);
               
	   		setAlphaRunnableState(STATE_ALPHA_IN);
    	}
    	
    	private void setAlphaRunnableState(int state) {
        	
        	if(itemNeedAlpha)
        	{
	        	switch (state) {
	                case STATE_ALPHA_NONE:
	                	mHandler.removeCallbacks(mItemAlphaRunnable);
	                	alphaView.invalidate();
	                    break;
	               
	                case STATE_ALPHA_IN:
	                	float x = (float)mItemAlphaRunnable.getAlpha()/255F;
	        	        System.out.println("alphaView <<< " + alphaView.getAlpha());
	                	alphaView.setAlpha(x);
	                	System.out.println("alphaView >>> " + alphaView.getAlpha());
	                	
	                	alphaView.invalidate();
	                	//requestLayout();
	                    break;
	                case STATE_ALPHA_EXIT:
	                	mHandler.removeCallbacks(mItemAlphaRunnable);
	                	//BUG
	                	alphaView.setAlpha(mOriginalAlpha);
	                	alphaView.invalidate();
	                	
	                	break;
	            }
            mState = state;
            }
        }
        
        public int getAlphaRunnableState() {
            return mState;
        }
        
        final class ItemViewAlphaRunnable implements Runnable {

    		long mStartTime;
    	    long mItemAlphaDuration;
    	    static final int ALPHA_MAX = 200;
    	    static final long ALPHA_DURATION = 300;
    	    
    	    public void run() { 
    	    	while (!mDataChanged&&mState!=STATE_ALPHA_EXIT&&getAlpha()>0){
    	    		if(!isInTouchMode()){
    	    			System.out.println(isInTouchMode());
        	    		setAlphaRunnableState(STATE_ALPHA_EXIT);
        	    		break;
        	    	}
    	    		
                    if(mStartTime==0)
                    initAlpha();
        	        //alphaView.invalidate();
        	        setAlphaRunnableState(STATE_ALPHA_IN);
        	       
        	    }
    	    }
    	    
    	    void initAlpha() {
    	    	mItemAlphaDuration = ALPHA_DURATION;
    	        mStartTime = SystemClock.uptimeMillis(); 
    	    }
    	        
    	    int getAlpha() {
    	    	if (getAlphaRunnableState() != STATE_ALPHA_IN) {
    	            return ALPHA_MAX;
    	        }
    	        
    	    	int alpha;
    	        long now = SystemClock.uptimeMillis();
    	        if (now > mStartTime + mItemAlphaDuration) {
    	        	System.out.println("now -- " +now);
    	        	alpha = 0;
    	        } else {
    	        	alpha = (int) (ALPHA_MAX - ((now - mStartTime) * ALPHA_MAX) / mItemAlphaDuration); 
    	        }
    	        
    	        return alpha;
    	    }
    	        
    	}
    }

    boolean performLongPress(final View child,
            final int longPressPosition, final long longPressId) {
        // CHOICE_MODE_MULTIPLE_MODAL takes over long press.
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
            if (mChoiceActionMode == null &&
                    (mChoiceActionMode = startActionMode(mMultiChoiceModeCallback)) != null) {
                setItemChecked(longPressPosition, true);
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            return true;
        }

        boolean handled = false;
        if (mOnItemLongClickListener != null) {
            handled = mOnItemLongClickListener.onItemLongClick(AbsFoolView.this, child,
                    longPressPosition, longPressId);
        }
        if (!handled) {
            mContextMenuInfo = createContextMenuInfo(child, longPressPosition, longPressId);
            handled = super.showContextMenuForChild(AbsFoolView.this);
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }
    
    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    /**
     * Creates the ContextMenuInfo returned from {@link #getContextMenuInfo()}. This
     * methods knows the view, position and ID of the item that received the
     * long press.
     *
     * @param view The view that received the long press.
     * @param position The position of the item that received the long press.
     * @param id The ID of the item that received the long press.
     * @return The extra information that should be returned by
     *         {@link #getContextMenuInfo()}.
     */
    ContextMenuInfo createContextMenuInfo(View view, int position, long id) {
        return new AdapterContextMenuInfo(view, position, id);
    }
    
    /**
     * Sets the checked state of the specified position. The is only valid if
     * the choice mode has been set to {@link #CHOICE_MODE_SINGLE} or
     * {@link #CHOICE_MODE_MULTIPLE}.
     *
     * @param position The item whose checked state is to be checked
     * @param value The new checked state for the item
     */
    public void setItemChecked(int position, boolean value) {
        if (mChoiceMode == CHOICE_MODE_NONE) {
            return;
        }

        // Start selection mode if needed. We don't need to if we're unchecking something.
        if (value && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode == null) {
            if (mMultiChoiceModeCallback == null ||
                    !mMultiChoiceModeCallback.hasWrappedCallback()) {
                throw new IllegalStateException("AbsFoolView: attempted to start selection mode " +
                        "for CHOICE_MODE_MULTIPLE_MODAL but no choice mode callback was " +
                        "supplied. Call setMultiChoiceModeListener to set a callback.");
            }
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        if (mChoiceMode == CHOICE_MODE_MULTIPLE || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
            boolean oldValue = mCheckStates.get(position);
            mCheckStates.put(position, value);
            if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                if (value) {
                    mCheckedIdStates.put(mAdapter.getItemId(position), position);
                } else {
                    mCheckedIdStates.delete(mAdapter.getItemId(position));
                }
            }
            if (oldValue != value) {
                if (value) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
            }
            if (mChoiceActionMode != null) {
                final long id = mAdapter.getItemId(position);
                mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode,
                        position, id, value);
            }
        } else {
            boolean updateIds = mCheckedIdStates != null && mAdapter.hasStableIds();
            // Clear all values if we're checking something, or unchecking the currently
            // selected item
            if (value || isItemChecked(position)) {
                mCheckStates.clear();
                if (updateIds) {
                    mCheckedIdStates.clear();
                }
            }
            // this may end up selecting the value we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact getCheckedItemPosition relies on
            if (value) {
                mCheckStates.put(position, true);
                if (updateIds) {
                    mCheckedIdStates.put(mAdapter.getItemId(position), position);
                }
                mCheckedItemCount = 1;
            } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                mCheckedItemCount = 0;
            }
        }

        // Do not generate a data change while we are in the layout phase
        if (!mInLayout && !mBlockLayoutRequests) {
            mDataChanged = true;
            rememberSyncState();
            requestLayout();
        }
    }

    /**
     * Returns the checked state of the specified position. The result is only
     * valid if the choice mode has been set to {@link #CHOICE_MODE_SINGLE}
     * or {@link #CHOICE_MODE_MULTIPLE}.
     *
     * @param position The item whose checked state to return
     * @return The item's checked state or <code>false</code> if choice mode
     *         is invalid
     */
    public boolean isItemChecked(int position) {
    	//if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
    	if (mCheckStates != null) {
    	    return mCheckStates.get(position);
        }

        return false;
    }
    
    /**
     * Makes the item at the supplied position selected.
     *
     * @param position the position of the new selection
     */
    abstract void setSelectionInt(int position);
    
    private class PerformSpot implements Runnable{
    	int spotPosistion;
    	
		@Override
		public void run() {
			
			spotPosistion = mSpotPosition;
			
			if(spotPosistion>=0&isSpot){
				mItemSpotStates.put(mMotionPosition, true);
				
				if(mLayoutMode!=LAYOUT_SPOT){
					mLayoutMode = LAYOUT_PREVIEW;
				}
				
				boolean handle = spotingNext();
				if(handle){
					mTouchMode = TOUCH_MODE_REST;
					requestLayout();
					removeCallbacks(this);
				}
				else{
					mTouchMode = TOUCH_MODE_DONE_WAITING;
				}
			}
		}
    }
	
    /**
     * @return
     */
    protected boolean spotingNext() {
    	return false;
    }
    
    private class SpotSlide implements Runnable{
    	int mSpotSlidePosistion;
    	boolean handle = false;
    	
		@Override
		public void run() {
			if(!mDataChanged) {
				final ListAdapter adapter = mAdapter;
	            final int motionPosition = mSpotSlidePosistion;
	            int count = adapter.getCount();
	            
		    	if (adapter != null && mItemCount > 0 &&motionPosition != INVALID_POSITION && motionPosition < count) {
		            final View v = getChildAt(motionPosition-mFirstPosition);
		            handle =  performSpotSlide(v, motionPosition,adapter.getItemId(motionPosition));
		    	}
				
		    	mTouchMode = TOUCH_MODE_REST;
				if(handle)removeCallbacks(this);
			}	
		}
    }

    /**
     * Call the mSlideListener, if it is defined.
     *
     * @param view The view within the FoolAdapterView that was clicked.
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was slided.
     * @return True if there was an assigned mSlideListener that was
     *         called, false otherwise is returned.
     */
    protected boolean performSpotSlide(View view, int position, long id) {
    	if (mSlideListener != null) {
            playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT);
            mSlideListener.onSlide(this, view, position, id);
            return true;
        }
        return false;
    }

    
    
    /**
     * Call the mPullListener, if it is defined.
     *
     * @param view The view within the FoolAdapterView that was clicked.
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was pulled.
     * @return True if there was an assigned mPullListener that was
     *         called, false otherwise is returned.
     */
    protected boolean performSpotPull(View view, int position, long id) {
    	   
    	if (mPullListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            mPullListener.onPull(this, view, position, id);
            return true;
        }
        return false;
    }
    
    /**
	 * @return
	 */
    public int getItemPulledItemCount(){
    	return mItemPulledItemCount;
    }
    
    //==============================================================================================
    
    private void initSpot(){
    		
    	if(mItemSpotStates ==null){
    		mItemSpotStates = new SparseBooleanArray();	
    	}
    		
    	//if(mItemSpotedIdStates == null&&mAdapter!=null&&mAdapter.hasStableIds()){
    	if(mItemSpotedIdStates == null&&mAdapter!=null){
    	    	mItemSpotedIdStates = new LongSparseArray<Integer>();
    	}

    } 

    protected boolean isCheckSpottedPosition(int pos){
    	boolean hasACheck = false;
    	
    	for(int i = 0;i<mItemSpotStates.size();i++){
    		if(mItemSpotStates.get(pos))
    			hasACheck = true; 
    	}   	
    	return hasACheck;
    }
    
    /**
     * @return
     */
    private int getMaxItemPulledFromIdStates(){
    	int y = 0;
    	for(int i = 0;i<mItemSpotedIdStates.size();i++){
    		y =Math.max(mItemSpotedIdStates.valueAt(i), y);
    	}   	
    	return y;
    }  
    
    /**
     * @return
     */
    private int getMinItemPulledPosition(){
    	int y = 0;
    	for(int i = 0;i<mItemSpotedIdStates.size();i++){
    		y =Math.min(mItemSpotedIdStates.valueAt(i), y);
    	}   	
    	return y;
    }
    
    /**
     * @param itemNeedAlpha
     */
    public void setIsPullItemViewAlpha(boolean itemNeedAlpha){
    	this.itemNeedAlpha = itemNeedAlpha;
    }
	
	private interface ItemSlide{

		static final int SLIDE_ENTER = 0;
		static final int SLIDE_IN = 1;
		static final int SLIDE_EXIT = 2;
	}
	
    /**
     * @author davidlau
     * 2014 2 27 03:08
     */
	class PerformItemSlide  extends WindowRunnnable implements Runnable {
      
        private int mSlideState = ItemSlide.SLIDE_ENTER;
        private int MaxDeltaX = 0;
        View mSlideItemView;
        int deltaX;
        
        public PerformItemSlide() {
		 	MaxDeltaX = (int) (getWidth()/FRICTION);
		}

		@Override
		public void run() {
			if(!mDataChanged&&mIsAttached&&isSliding){
            	switch (mSlideState) {
                    case ItemSlide.SLIDE_IN:
                        slide();
                        break;
                    case ItemSlide.SLIDE_EXIT:
                    	endSlide();
                        isSliding = false;
                        break;
                }
            }
		}

	    private void slide(){
	    	mSlidedPosition = mMotionPosition;
	    	mLayoutMode = LAYOUT_SLIDE;
	    	
	    	if(Math.abs(deltaX)>MaxDeltaX){
	    		deltaX = deltaX>0?MaxDeltaX:-MaxDeltaX;
	    	}
	    	
	    	setIncrementalDeltaX(deltaX);
	    	requestLayout();
            mTouchMode = TOUCH_MODE_OVERSLIDE; 
        }

		private void endSlide() {
			mSlidedPosition = INVALID_POSITION;
            mLayoutMode = LAYOUT_PREVIEW;
            layoutChildren();
            
            if(mSpotSlide== null){
                mSpotSlide = new SpotSlide();
            }
            //postDelayed(mSpotSlide,SLIDING_TIME);//SLIDEOVERTIME
        }
		
		void setSlideState(int state){
			this.mSlideState = state;
		}
	}
	
    /**
     * The RecycleBin facilitates reuse of views across layouts. The RecycleBin has two levels of
     * storage: ActiveViews and measuredAndUnusedViews. ActiveViews are those views which were onscreen at the
     * start of a layout. By construction, they are displaying current information. At the end of
     * layout, all views in ActiveViews are demoted to measuredAndUnusedViews. measuredAndUnusedViews are old views that
     * could potentially be used by the adapter to avoid allocating views unnecessarily.
     *
     * @see zy.fool.widget.AbsFoolView#setRecyclerListener(zy.fool.widget.AbsFoolView.RecyclerListener)
     * @see zy.fool.widget.AbsFoolView.RecyclerListener
     */
    class RecycleBin {
        private RecyclerListener mRecyclerListener;

        /**
         * The position of the first view stored in mActiveViews.
         */
        private int mFirstActivePosition;

        /**
         * Views that were on screen at the start of layout. This array is populated at the start of
         * layout, and at the end of layout all view in mActiveViews are moved to mMeasuredAndUnusedViews.
         * Views in mActiveViews represent a contiguous range of Views, with position of the first
         * view store in mFirstActivePosition.
         */
        private View[] mActiveViews = new View[0];

        /**
         * Unsorted views that can be used by the adapter as a convert view.
         */
        private ArrayList<View>[] mMeasuredAndUnusedViews;

        private int mViewTypeCount;

        private ArrayList<View> mCurrentMeasuredAndUnused;

        private ArrayList<View> mSkippedMeasuredAndUnused;

        private SparseArray<View> mTransientStateViews;

        public void setViewTypeCount(int viewTypeCount) {

            if (viewTypeCount < 1) {
                throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
            }

            //noinspection unchecked
            ArrayList<View>[] measuredAndUnusedViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++) {
            	measuredAndUnusedViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mCurrentMeasuredAndUnused = measuredAndUnusedViews[0];
            mMeasuredAndUnusedViews = measuredAndUnusedViews;

        }

        public void markChildrenDirty() {

            if (mViewTypeCount == 1) {
                final ArrayList<View> measuredAndUnused = mCurrentMeasuredAndUnused;
                final int measuredAndUnusedCount = measuredAndUnused.size();
                for (int i = 0; i < measuredAndUnusedCount; i++) {
                    measuredAndUnused.get(i).forceLayout();
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> measuredAndUnused = mMeasuredAndUnusedViews[i];
                    final int measuredAndUnusedCount = measuredAndUnused.size();

                    for (int j = 0; j < measuredAndUnusedCount; j++) {
                        measuredAndUnused.get(j).forceLayout();
                        //Forces this view to be laid out during the next layout pass.
                        //This method does not call requestLayout() or forceLayout() on the parent.

                    }
                }
            }
            if (mTransientStateViews != null) {
                final int count = mTransientStateViews.size();
                for (int i = 0; i < count; i++) {
                    mTransientStateViews.valueAt(i).forceLayout();
                }
            }
        }

        public boolean shouldRecycleViewType(int viewType) {
        	return viewType >= 0;
        }

        /**
         * Clears the measuredAndUnused heap.
         */
        void clear() {

            if (mViewTypeCount == 1) {
                final ArrayList<View> measuredAndUnused = mCurrentMeasuredAndUnused;
                final int measuredAndUnusedCount = measuredAndUnused.size();
                for (int i = 0; i < measuredAndUnusedCount; i++) {
                    removeDetachedView(measuredAndUnused.remove(measuredAndUnusedCount - 1 - i), false);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> measuredAndUnused = mMeasuredAndUnusedViews[i];
                    final int measuredAndUnusedCount = measuredAndUnused.size();
                    for (int j = 0; j < measuredAndUnusedCount; j++) {
                        removeDetachedView(measuredAndUnused.remove(measuredAndUnusedCount - 1 - j), false);
                    }
                }
            }
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        /**
         * Fill ActiveViews with all of the children of the AbsFoolView.
         *
         * @param childCount The minimum number of views mActiveViews should hold
         * @param firstActivePosition The position of the first view that will be stored in
         *        mActiveViews
         */
        void fillActiveViews(int childCount, int firstActivePosition) {

            if (mActiveViews.length < childCount) {
                mActiveViews = new View[childCount];
            }
            mFirstActivePosition = firstActivePosition;

            final View[] activeViews = mActiveViews;
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                // Don't put header or footer views into the measuredAndUnused heap
                if (lp != null && lp.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                    // Note:  We do place AdapterView.ITEM_VIEW_TYPE_IGNORE in active views.
                    //        However, we will NOT place them into measuredAndUnused views.
                    activeViews[i] = child;
                 }
            }
        }

        /**
         * Get the view corresponding to the specified position. The view will be removed from
         * mActiveViews if it is found.
         *
         * @param position The position to look up in mActiveViews
         * @return The view if it is found, null otherwise
         */
        View getActiveView(int position) {

            int index = position - mFirstActivePosition;
            final View[] activeViews = mActiveViews;
            if (index >=0 && index < activeViews.length) {
                final View match = activeViews[index];
                activeViews[index] = null;
                return match;
            }
            return null;
        }

        View getTransientStateView(int position) {

            if (mTransientStateViews == null) {
                return null;
            }
            final int index = mTransientStateViews.indexOfKey(position);
            if (index < 0) {
                return null;
            }
            final View result = mTransientStateViews.valueAt(index);
            mTransientStateViews.removeAt(index);
            return result;
        }

        /**
         * Dump any currently saved views with transient state.
         */
        void clearTransientStateViews() {
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        /**
         * @return A view from the measuredAndUnusedViews collection. These are unordered.
         */
        View getMeasuredAndUnusedView(int position) {
        	if (mViewTypeCount == 1) {
                return retrieveFromMeasuredAndUnused(mCurrentMeasuredAndUnused, position);
            } else {
                int whichmeasuredAndUnused = mAdapter.getItemViewType(position);
                if (whichmeasuredAndUnused >= 0 && whichmeasuredAndUnused < mMeasuredAndUnusedViews.length) {
                    return retrieveFromMeasuredAndUnused(mMeasuredAndUnusedViews[whichmeasuredAndUnused], position);
                }
            }
            return null;
        }

        /**
         * Put a view into the measuredAndUnusedViews list. These views are unordered.
         *
         * @param measuredAndUnused The view to add
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		void addMeasuredAndUnusedView(View measuredAndUnused, int position) {
        	
        	LayoutParams lp = (LayoutParams) measuredAndUnused.getLayoutParams();
            if (lp == null) {
                return;
            }

            lp.measuredAndUnusedFromPosition = position;


            // Don't put header or footer views or views that should be ignored
            // into the measuredAndUnused heap
            int viewType = lp.viewType;
            final boolean measuredAndUnusedHasTransientState = measuredAndUnused.hasTransientState();
            if (!shouldRecycleViewType(viewType) || measuredAndUnusedHasTransientState) {
                if (viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER || measuredAndUnusedHasTransientState) {
                    if (mSkippedMeasuredAndUnused == null) {
                        mSkippedMeasuredAndUnused = new ArrayList<View>();
                    }
                    mSkippedMeasuredAndUnused.add(measuredAndUnused);
                }
                if (measuredAndUnusedHasTransientState) {
                    if (mTransientStateViews == null) {
                        mTransientStateViews = new SparseArray<View>();
                    }
                    measuredAndUnused.onStartTemporaryDetach();
                    mTransientStateViews.put(position, measuredAndUnused);
                }
                return;
            }

            measuredAndUnused.onStartTemporaryDetach();

            if (mViewTypeCount == 1) {
                mCurrentMeasuredAndUnused.add(measuredAndUnused);
            } else {
                mMeasuredAndUnusedViews[viewType].add(measuredAndUnused);
            }

            //measuredAndUnused.setAccessibilityDelegate(null);

            if (mRecyclerListener != null) {
                mRecyclerListener.onMovedTomeasuredAndUnusedHeap(measuredAndUnused);
            }
        }

        /**
         * Finish the removal of any views that skipped the measuredAndUnused heap.
         */
        void removeSkippedMeasuredAndUnused() {

        	if (mSkippedMeasuredAndUnused == null) {
                return;
            }
            final int count = mSkippedMeasuredAndUnused.size();
            for (int i = 0; i < count; i++) {
                removeDetachedView(mSkippedMeasuredAndUnused.get(i), false);
            }
            mSkippedMeasuredAndUnused.clear();
        }

        /**
         * Move all views remaining in mActiveViews to mmeasuredAndUnused Views.
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		void moveMeasuredAndUnusedToActiveViews() {

            final View[] activeViews = mActiveViews;
            final boolean hasListener = mRecyclerListener != null;
            final boolean multiplemeasuredAndUnuseds = mViewTypeCount > 1;

            ArrayList<View> measuredAndUnusedViews = mCurrentMeasuredAndUnused;
            final int count = activeViews.length;
            for (int i = count - 1; i >= 0; i--) {
                final View victim = activeViews[i];
                if (victim != null) {
                    final LayoutParams lp
                            = (LayoutParams) victim.getLayoutParams();
                    int whichMeasuredAndUnused = lp.viewType;

                    activeViews[i] = null;

                    final boolean measuredAndUnusedHasTransientState = victim.hasTransientState();
                    if (!shouldRecycleViewType(whichMeasuredAndUnused) || measuredAndUnusedHasTransientState) {
                        // Do not move views that should be ignored
                        if (whichMeasuredAndUnused != ITEM_VIEW_TYPE_HEADER_OR_FOOTER ||
                                measuredAndUnusedHasTransientState) {
                            removeDetachedView(victim, false);
                        }
                        if (measuredAndUnusedHasTransientState) {
                            if (mTransientStateViews == null) {
                                mTransientStateViews = new SparseArray<View>();
                            }
                            mTransientStateViews.put(mFirstActivePosition + i, victim);
                        }
                        continue;
                    }

                    if (multiplemeasuredAndUnuseds) {
                        measuredAndUnusedViews = mMeasuredAndUnusedViews[whichMeasuredAndUnused];
                    }
                    victim.onStartTemporaryDetach();
                    lp.measuredAndUnusedFromPosition = mFirstActivePosition + i;
                    measuredAndUnusedViews.add(victim);

                    //victim.setAccessibilityDelegate(null);

                    if (hasListener) {
                        mRecyclerListener.onMovedTomeasuredAndUnusedHeap(victim);
                    }
                }
            }

            pruneMeasuredAndUnusedViews();
        }


        /**
         * Makes sure that the size of mmeasuredAndUnusedViews does not exceed the size of mActiveViews.
         * (This can happen if an adapter does not recycle its views).
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		private void pruneMeasuredAndUnusedViews() {

            final int maxViews = mActiveViews.length;
            final int viewTypeCount = mViewTypeCount;
            final ArrayList<View>[] measuredAndUnusedViews = mMeasuredAndUnusedViews;
            for (int i = 0; i < viewTypeCount; ++i) {
                final ArrayList<View> measuredAndUnusedPile = measuredAndUnusedViews[i];
                int size = measuredAndUnusedPile.size();
                final int extras = size - maxViews;
                size--;
                for (int j = 0; j < extras; j++) {
                	removeDetachedView(measuredAndUnusedPile.remove(size--), false);

                }
            }

            if (mTransientStateViews != null) {
                for (int i = 0; i < mTransientStateViews.size(); i++) {
                    final View v = mTransientStateViews.valueAt(i);
                    if (!v.hasTransientState()) {
                        mTransientStateViews.removeAt(i);
                        i--;
                    }
                }
            }
        }

        /**
         * Puts all views in the measuredAndUnused heap into the supplied list.
         */
        void reclaimMeasuredAndUnusedViews(List<View> views) {

            if (mViewTypeCount == 1) {
                views.addAll(mCurrentMeasuredAndUnused);
            } else {
                final int viewTypeCount = mViewTypeCount;
                final ArrayList<View>[] measuredAndUnusedViews = mMeasuredAndUnusedViews;
                for (int i = 0; i < viewTypeCount; ++i) {
                    final ArrayList<View> measuredAndUnusedPile = measuredAndUnusedViews[i];
                    views.addAll(measuredAndUnusedPile);
                }
            }
        }

        /**
         * Updates the cache color hint of all known views.
         *
         * @param color The new cache color hint.
         */
        void setCacheColorHint(int color) {

            if (mViewTypeCount == 1) {
                final ArrayList<View> measuredAndUnused = mCurrentMeasuredAndUnused;
                final int measuredAndUnusedCount = measuredAndUnused.size();
                for (int i = 0; i < measuredAndUnusedCount; i++) {
                    measuredAndUnused.get(i).setDrawingCacheBackgroundColor(color);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> measuredAndUnused = mMeasuredAndUnusedViews[i];
                    final int measuredAndUnusedCount = measuredAndUnused.size();
                    for (int j = 0; j < measuredAndUnusedCount; j++) {
                        measuredAndUnused.get(j).setDrawingCacheBackgroundColor(color);
                    }
                }
            }

            // Just in case this is called during a layout pass
            final View[] activeViews = mActiveViews;
            final int count = activeViews.length;
            for (int i = 0; i < count; ++i) {
                final View victim = activeViews[i];
                if (victim != null) {
                    victim.setDrawingCacheBackgroundColor(color);
                }
            }
        }
    }
    
    /**
     * A RecyclerListener is used to receive a notification whenever a View is placed
     * inside the RecycleBin's measuredAndUnused heap. This listener is used to free resources
     * associated to Views placed in the RecycleBin.
     *
     * @see zy.fool.widget.AbsFoolView.RecycleBin
     * @see zy.fool.widget.AbsFoolView#setRecyclerListener(zy.fool.widget.AbsFoolView.RecyclerListener)
     */
    public static interface RecyclerListener {
        /**
         * Indicates that the specified View was moved into the recycler's measuredAndUnused heap.
         * The view is not displayed on screen any more and any expensive resource
         * associated with the view should be discarded.
         *
         * @param view
         */
        void onMovedTomeasuredAndUnusedHeap(View view);
    }

    /**
 	* AbsFoolView extends LayoutParams to provide a place to hold the view type.
 	*/
 	public static class LayoutParams extends ViewGroup.LayoutParams {
 		/**
 	     * View type for this view, as returned by
 	     * {@link android.widget.Adapter#getItemViewType(int) }
 	     */
 	    int viewType;

 	    /**
 	    * When this boolean is set, the view has been added to the AbsFoolView
 	    * at least once. It is used to know whether headers/footers have already
 	    * been added to the list view and whether they should be treated as
 	    * recycled views or not.
 	    */
 	    boolean recycledHeaderFooter;
 	    
 	   /**
  	    * When this boolean is set, the view has been added to the AbsFoolView
  	    * at least once. It is used to know whether pull-topers/bottomers have already
  	    * been added to the list view and whether they should be treated as
  	    * recycled views or not.
  	    */
  	    boolean recycledPulledToperOrBottomer;

 	    /**
 	     * When an AbsFoolView is measured with an AT_MOST measure spec, it needs
 	     * to obtain children views to measure itself. When doing so, the children
 	     * are not attached to the window, but put in the recycler which assumes
 	     * they've been attached before. Setting this flag will force the reused
 	     * view to be attached to the window rather than just attached to the
 	     * parent.
 	     */
 	    boolean forceAdd;

 	    /**
 	     * The position the view was removed from when pulled out of the
 	     * measuredAndUnused heap.
 	     * @hide
 	     */
 	    int measuredAndUnusedFromPosition;

 	    /**
 	     * The ID the view represents
 	     */
 	    long itemId = -1;

 	    public LayoutParams(Context c, AttributeSet attrs) {
 	       super(c, attrs);
 	    }

 	    public LayoutParams(int w, int h) {
 	       super(w, h);
         }

 	    public LayoutParams(int w, int h, int viewType) {
 	       super(w, h);
 	       this.viewType = viewType;
 	    }

 	    public LayoutParams(ViewGroup.LayoutParams source) {
 	       super(source);
 	    }
 	}
 	
 	static View retrieveFromMeasuredAndUnused(ArrayList<View> measuredAndUnusedViews, int position) {
        int size = measuredAndUnusedViews.size();
        if (size > 0) {
            // See if we still have a view for this position.
            for (int i=0; i<size; i++) {
                View view = measuredAndUnusedViews.get(i);
                if (((LayoutParams)view.getLayoutParams()).measuredAndUnusedFromPosition == position) {
                    measuredAndUnusedViews.remove(i);
                    return view;
                }
            }
            return measuredAndUnusedViews.remove(size - 1);
        } else {
            return null;
        }
    }
 	
    /**
     * Returns the number of header views in the list. Header views are special views
     * at the top of the list that should not be recycled during a layout.
     *
     * @return The number of header views, 0 in the default implementation.
     */
    int getHeaderViewCount() {
        return 0;
    }

    /**
     * Returns the number of footer views in the list. Footer views are special views
     * at the bottom of the list that should not be recycled during a layout.
     *
     * @return The number of footer views, 0 in the default implementation.
     */
    int getFooterViewsCount() {
        return 0;
    }

    void hideSelector() {
        if (mSelectedPosition != INVALID_POSITION) {
            if (mLayoutMode != LAYOUT_SPECIFIC) {
                mResurrectToPosition = mSelectedPosition;
            }
            if (mNextSelectedPosition >= 0 && mNextSelectedPosition != mSelectedPosition) {
                mResurrectToPosition = mNextSelectedPosition;
            }
            setSelectedPositionInt(INVALID_POSITION);
            setNextSelectedPositionInt(INVALID_POSITION);
            mSelectedTop = 0;
        }
    }

    public boolean isLayoutRtl() {
        return this.getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }
    
   /**
    * When set to a non-zero value, the cache color hint indicates that this list is always drawn
    * on top of a solid, single-color, opaque background.
    *
    * Zero means that what's behind this object is translucent (non solid) or is not made of a
    * single color. This hint will not affect any existing background drawable set on this view (
    * typically set via {@link #setBackgroundDrawable(android.graphics.drawable.Drawable)}).
    *
    * @param color The background color
    */
   public void setCacheColorHint(int color) {
       if (color != mCacheColorHint) {
           mCacheColorHint = color;
           int count = getChildCount();
           for (int i = 0; i < count; i++) {
               getChildAt(i).setDrawingCacheBackgroundColor(color);
           }
           mRecycler.setCacheColorHint(color);
       }
   }

   /**
    * When set to a non-zero value, the cache color hint indicates that this list is always drawn
    * on top of a solid, single-color, opaque background
    *
    * @return The cache color hint
    */
    public int getCacheColorHint() {
       return mCacheColorHint;
    }
   
    /**
     * @return A position to select. First we try mSelectedPosition. If that has been clobbered by
     * entering touch mode, we then try mResurrectToPosition. Values are pinned to the range
     * of items available in the adapter
     */
    int reconcileSelectedPosition() {
       int position = mSelectedPosition;
       if (position < 0) {
           position = mResurrectToPosition;
       }
       position = Math.max(0, position);
       position = Math.min(position, mItemCount - 1);
       return position;
    }
	
    OnPullListener mPullListener = null;

    OnSlideListener mSlideListener = null;
   
    public void setOnPullListener(OnPullListener onPullListener){
		mPullListener = onPullListener;
    }

    public void setOnSlideListener(OnSlideListener mListener){
	    mSlideListener = mListener;
    }	
	
	
	protected int getPullY() {
		return mPullY;
	}
	
	protected void setPullY(int pull_y) {
		this.mPullY = pull_y;
	}
	
	protected int getPullToRefreshScrollDuration() {
		return SMOOTH_PULL_DURATION_MS;
	}
	
	protected void prepareInnerWindow(){
		
		if(mInnerPolicy.getInnerWindow()==null){
			mInnerPolicy.makeNewInnerWindow();
		}
		mInnerWindow = mInnerPolicy.getInnerWindow();
	}

	protected void closeInnerWindow() {
		
		mPulledPosistion = INVALID_POSITION;
		mCurrentPullMode = PULL_REST;
		mLastPullMode = PULL_IN;
		mTouchMode = TOUCH_MODE_REST;
		
		if (tempPulledRectF != null)
			tempPulledRectF = null;
		
		if(mInnerWindow!=null)
			mInnerWindow = null;
		
		Runnable closeRunnable = new Runnable() {
			
			@Override
			public void run() {
				System.out.println("closeInnerWindow Runnable");
				
				if(mAboverPulledViewInfos.size()>0||mBelowerPulledViewInfos.size()>0){
					mAboverPulledViewInfos.clear();
					System.out.println("mAboverPulledViewInfos isemty" + mAboverPulledViewInfos.isEmpty());
					mBelowerPulledViewInfos.clear();
				}
				
				mLayoutMode = LAYOUT_PREVIEW;
				//mFirstPosition = 0;
				mSelectedPosition = mFirstPosition;
				detachAllViewsFromParent();
				layoutChildren();
			}
		};
		
		postDelayed(closeRunnable, PULLING_TIME);
	}
}
