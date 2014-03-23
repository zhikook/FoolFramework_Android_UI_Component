package zy.fool.internal;

import zy.fool.R;
import zy.fool.policy.IInnerWin;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public  abstract class InnerBaseWindow<T extends View> extends LinearLayout implements IInnerWin{
	public static final int SMOOTH_SCROLL_DURATION_MS = 200;
	public static final int SMOOTH_SCROLL_LONG_DURATION_MS = 325;
	static final int DEMO_SCROLL_INTERVAL = 225;
	static final float FRICTION = 2.0f;
	
	static final String STATE_STATE = "str_state";
	static final String STATE_MODE = "str_mode";
	static final String STATE_CURRENT_MODE = "str_current_mode";
	static final String STATE_SCROLLING_REFRESHING_ENABLED = "str_disable_scrolling";
	static final String STATE_SHOW_REFRESHING_VIEW = "str_show_refreshing_view";
	static final String STATE_SUPER = "str_super";
	
	// ===========================================================
	// Fields
	// ===========================================================
	
	static final boolean USE_HW_LAYERS = false;

	private int mTouchSlop;
	private float mLastMotionX, mLastMotionY;
	private float mInitialMotionX, mInitialMotionY;
	
	VelocityTracker mVelocityTracker;
	private int mLastScrollX;

	private boolean mIsBeingDragged = false;
	private State mState = State.RESET;
	private Mode mMode = Mode.getDefault();
	
	private Mode mCurrentMode;
	T mRefreshableView;
	private FrameLayout mRefreshableViewWrapper;
	
	private boolean mShowViewWhileRefreshing = true;
	private boolean mScrollingWhileRefreshingEnabled = false;
	private boolean mFilterTouchEvents = true;
	private boolean mOverScrollEnabled = true;
	private boolean mLayoutVisibilityChangesEnabled = true;

	private Interpolator mScrollAnimationInterpolator;
	private AnimationStyle mLoadingAnimationStyle = AnimationStyle.getDefault();
	private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
	
	private LoadingLayout mLefterLayout;
	private LoadingLayout mRighterLayout;

	private OnSlideToRefreshListener<T> mOnSlideToRefreshListener;
	private OnSlideToRefreshListener2<T> mOnSlideToRefreshListener2;
	private OnSlideEventListener<T> mOnSlideEventListener;
	
	public InnerBaseWindow(Context context) {
		super(context);
		init(context,null);
	}
	
	public InnerBaseWindow(Context context, AttributeSet attrs) {
		super(context,attrs);
		init(context, attrs);
	}
	
	public InnerBaseWindow(Context context,Mode mode) {
		super(context);
		mMode = mode;
		init(context, null);
	}
	
	public InnerBaseWindow(Context context, Mode mode, AnimationStyle animStyle) {
		super(context);
		mMode = mode;
		mLoadingAnimationStyle = animStyle;
		init(context, null);
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		final T refreshableView = getRefreshableView();

		if (refreshableView instanceof ViewGroup) {
			((ViewGroup) refreshableView).addView(child, index, params);
		} else {
			throw new UnsupportedOperationException("Refreshable View is not a ViewGroup so can't addView");
		}
	}

	private void init(Context context, AttributeSet attrs) {
		setOrientation(LinearLayout.HORIZONTAL);	
		setGravity(Gravity.CENTER);

		ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();

		// Styleables from XML
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideToRefresh);
	
		if (a.hasValue(R.styleable.SlideToRefresh_strMode)) {
			mMode = Mode.mapIntToValue(a.getInteger(R.styleable.SlideToRefresh_strMode, 0));
		}
	
		if (a.hasValue(R.styleable.SlideToRefresh_strAnimationStyle)) {
			mLoadingAnimationStyle = AnimationStyle.mapIntToValue(a.getInteger(
					R.styleable.SlideToRefresh_strAnimationStyle, 0));
		}
	
		// Refreshable View
		// By passing the attrs, we can add ListView/GridView params via XML
		mRefreshableView = createRefreshableView(context, attrs);
		addRefreshableView(context, mRefreshableView);
	
		// We need to create now layouts now
		mLefterLayout = createLoadingLayout(context, Mode.SLIDE_FROM_LEFT, a);
		mRighterLayout = createLoadingLayout(context, Mode.SLIDE_FROM_RIGHT, a);
	
		/**
		 * Styleables from XML
		 */
		if (a.hasValue(R.styleable.SlideToRefresh_strRefreshableViewBackground)) {
			Drawable background = a.getDrawable(R.styleable.SlideToRefresh_strRefreshableViewBackground);
			if (null != background) {
				mRefreshableView.setBackgroundDrawable(background);
			}
		} else if (a.hasValue(R.styleable.SlideToRefresh_strAdapterViewBackground)) {
			//Utils.warnDeprecation("ptrAdapterViewBackground", "ptrRefreshableViewBackground");
			Drawable background = a.getDrawable(R.styleable.SlideToRefresh_strAdapterViewBackground);
			if (null != background) {
				mRefreshableView.setBackgroundDrawable(background);
			}
		}
	
		if (a.hasValue(R.styleable.SlideToRefresh_strOverScroll)) {
			mOverScrollEnabled = a.getBoolean(R.styleable.SlideToRefresh_strOverScroll, true);
		}
	
		if (a.hasValue(R.styleable.SlideToRefresh_strScrollingWhileRefreshingEnabled)) {
			mScrollingWhileRefreshingEnabled = a.getBoolean(
					R.styleable.SlideToRefresh_strScrollingWhileRefreshingEnabled, false);
		}
	
		// Let the derivative classes have a go at handling attributes, then
		// recycle them...
		handleStyledAttributes(a);
		a.recycle();
	
		// Finally update the UI for the modes
		updateUIForMode();
	}
	
	@Override
	protected final void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;

			setMode(Mode.mapIntToValue(bundle.getInt(STATE_MODE, 0)));
			mCurrentMode = Mode.mapIntToValue(bundle.getInt(STATE_CURRENT_MODE, 0));

			mScrollingWhileRefreshingEnabled = bundle.getBoolean(STATE_SCROLLING_REFRESHING_ENABLED, false);
			mShowViewWhileRefreshing = bundle.getBoolean(STATE_SHOW_REFRESHING_VIEW, true);

			// Let super Restore Itself
			super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));

			State viewState = State.mapIntToValue(bundle.getInt(STATE_STATE, 0));
			if (viewState == State.REFRESHING || viewState == State.MANUAL_REFRESHING) {
				setState(viewState, true);
			}

			// Now let derivative classes restore their state
			onStrRestoreInstanceState(bundle);
			return;
		}

		super.onRestoreInstanceState(state);
	}

	@Override
	protected final Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();

		// Let derivative classes get a chance to save state first, that way we
		// can make sure they don't overrite any of our values
		onStrSaveInstanceState(bundle);

		bundle.putInt(STATE_STATE, mState.getIntValue());
		bundle.putInt(STATE_MODE, mMode.getIntValue());
		bundle.putInt(STATE_CURRENT_MODE, mCurrentMode.getIntValue());
		bundle.putBoolean(STATE_SCROLLING_REFRESHING_ENABLED, mScrollingWhileRefreshingEnabled);
		bundle.putBoolean(STATE_SHOW_REFRESHING_VIEW, mShowViewWhileRefreshing);
		bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());

		return bundle;
	}

	@Override
	protected final void onSizeChanged(int w, int h, int oldw, int oldh) {

		super.onSizeChanged(w, h, oldw, oldh);

		// We need to update the header/footer when our size changes
		refreshLoadingViewsSize();

		// Update the Refreshable View layout
		refreshRefreshableViewSize(w, h);

		/**
		 * As we're currently in a Layout Pass, we need to schedule another one
		 * to layout any changes we've made here
		 */
		post(new Runnable() {
			@Override
			public void run() {
				requestLayout();
			}
		});
	}
	
	protected final void refreshRefreshableViewSize(int width, int height) {
		// We need to set the Height of the Refreshable View to the same as
		// this layout
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mRefreshableViewWrapper.getLayoutParams();
		if (lp.width != width) {
			lp.width = width;
			mRefreshableViewWrapper.requestLayout();
		}	
	}

	/**
	 * Called by {@link #onRestoreInstanceState(Parcelable)} so that derivative
	 * classes can handle their saved instance state.
	 * 
	 * @param savedInstanceState - Bundle which contains saved instance state.
	 */
	protected void onStrRestoreInstanceState(Bundle savedInstanceState) {
	}

	/**
	 * Called by {@link #onSaveInstanceState()} so that derivative classes can
	 * save their instance state.
	 * 
	 * @param saveState - Bundle to be updated with saved state.
	 */
	protected void onStrSaveInstanceState(Bundle saveState) {
	}
	
	public final ILoadingLayout getLoadingLayoutProxy() {
		return getLoadingLayoutProxy(true, true);
	}
	
	public final ILoadingLayout getLoadingLayoutProxy(boolean includeStart, boolean includeEnd) {
		return createLoadingLayoutProxy(includeStart, includeEnd);
	}
	
	private void addRefreshableView(Context context, T refreshableView) {
		mRefreshableViewWrapper = new FrameLayout(context);
		mRefreshableViewWrapper.addView(refreshableView, ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);

		addViewInternal(mRefreshableViewWrapper, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
	}

	/**
	 * This is implemented by derived classes to return the created View. If you
	 * need to use a custom View (such as a custom ListView), override this
	 * method and return an instance of your custom class.
	 * <p/>
	 * Be sure to set the ID of the view in this method, especially if you're
	 * using a ListActivity or ListFragment.
	 * 
	 * @param context Context to create view with
	 * @param attrs AttributeSet from wrapped class. Means that anything you
	 *            include in the XML layout declaration will be routed to the
	 *            created View
	 * @return New instance of the Refreshable View
	 */
	protected abstract T createRefreshableView(Context context, AttributeSet attrs);
	
	/**
	 * Allows Derivative classes to handle the XML Attrs without creating a
	 * TypedArray themsevles
	 * 
	 * @param a - TypedArray of PullToRefresh Attributes
	 */
	protected void handleStyledAttributes(TypedArray a) {
	}
	
	public final Mode getMode() {
		return mMode;
	}
	
	public final State getState() {
		return mState;
	}
	
	public final T getRefreshableView() {
		return mRefreshableView;
	}

	public final boolean getShowViewWhileRefreshing() {
		return mShowViewWhileRefreshing;
	}

	//@Override
	public final boolean isSlideToRefreshEnabled() {
		return mMode.permitsSlideToRefresh();
	}

	//@Override
	public final boolean isSlideToRefreshOverScrollEnabled() {
		return VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD && mOverScrollEnabled
				&& OverscrollHelper.isAndroidOverScrollEnabled(mRefreshableView);
	}

	//@Override
	public final boolean isRefreshing() {
		return mState == State.REFRESHING || mState == State.MANUAL_REFRESHING;
	}

	//@Override
	public final boolean isScrollingWhileRefreshingEnabled() {
		return mScrollingWhileRefreshingEnabled;
	}
	
	/**
	 * Helper method which just calls scrollTo() in the correct scrolling
	 * direction.
	 * 
	 * @param value - New Scroll value
	 */
	protected final void setLefterScroll(int value) {

		// Clamp value to with pull scroll range
		final int maximumPullScroll = getMaximumSlideScroll();
		value = Math.min(maximumPullScroll, Math.max(-maximumPullScroll, value));

		if (mLayoutVisibilityChangesEnabled) {
			if (value < 0) {
				mLefterLayout.setVisibility(View.VISIBLE);
			} else if (value > 0) {
				mRighterLayout.setVisibility(View.VISIBLE);
			} else {
				mLefterLayout.setVisibility(View.INVISIBLE);
				mRighterLayout.setVisibility(View.INVISIBLE);
			}
		}

		if (USE_HW_LAYERS) {
			/**
			 * Use a Hardware Layer on the Refreshable View if we've scrolled at
			 * all. We don't use them on the Header/Footer Views as they change
			 * often, which would negate any HW layer performance boost.
			 */
			ViewCompat.setLayerType(mRefreshableViewWrapper, value != 0 ? View.LAYER_TYPE_HARDWARE
					: View.LAYER_TYPE_NONE);
		}
		scrollTo(value, 0);	
	}
	
	protected final void smoothScrollTo(int scrollValue) {
		smoothScrollTo(scrollValue, getSlideToRefreshScrollDuration());
	}

	protected final void smoothScrollTo(int scrollValue, OnSmoothScrollFinishedListener listener) {
		smoothScrollTo(scrollValue, getSlideToRefreshScrollDuration(), 0, listener);
	}
	
	private final void smoothScrollTo(int scrollValue, long duration) {
		smoothScrollTo(scrollValue, duration, 0, null);
	}

	private final void smoothScrollTo(int newScrollValue, long duration, long delayMillis,
			OnSmoothScrollFinishedListener listener) {
		if (null != mCurrentSmoothScrollRunnable) {
			mCurrentSmoothScrollRunnable.stop();
		}

		final int oldScrollValue;
		
		oldScrollValue = getScrollX();
				

		if (oldScrollValue != newScrollValue) {
			if (null == mScrollAnimationInterpolator) {
				// Default interpolator is a Decelerate Interpolator
				mScrollAnimationInterpolator = new DecelerateInterpolator();
			}
			mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration, listener);

			if (delayMillis > 0) {
				postDelayed(mCurrentSmoothScrollRunnable, delayMillis);
			} else {
				post(mCurrentSmoothScrollRunnable);
			}
		}
	}

	private final void smoothScrollToAndBack(int y) {
		smoothScrollTo(y, SMOOTH_SCROLL_DURATION_MS, 0, new OnSmoothScrollFinishedListener() {

			@Override
			public void onSmoothScrollFinished() {
				smoothScrollTo(0, SMOOTH_SCROLL_DURATION_MS, DEMO_SCROLL_INTERVAL, null);
			}
		});
	}

	static interface OnSmoothScrollFinishedListener {
		void onSmoothScrollFinished();
	}

	/**
	 * Helper method which just calls scrollTo() in the correct scrolling
	 * direction.
	 * 
	 * @param value - New Scroll value
	 */
	protected final  void setScrollIfNeed(int value) {
		synchronized (this) {
			final int maximumPullScroll = getMaximumSlideScroll();
			value = Math.min(maximumPullScroll, Math.max(-maximumPullScroll, value));

			//if (mLayoutVisibilityChangesEnabled) {
			if (true) {
				if (value < 0) {
					this.setVisibility(View.VISIBLE);
				}
			}		
			scrollTo(value, 0);
		}
	}

	private int getMaximumSlideScroll() {
		return Math.round(getWidth() / FRICTION);			
	}
	
	protected int getSlideToRefreshScrollDuration() {
		return SMOOTH_SCROLL_DURATION_MS;
	}


	/**
	 * Simple Listener to listen for any callbacks to Add.
	 */
	public static interface OnSlideToRefreshListener<T extends View> {

		/**
		 * onAdd will be called for preparePull/others
		 */
		public void onSlideToRefresh(final InnerBaseWindow<T> innerWin);

	}
	
	/**
	 * Listener that allows you to be notified when the user has started or
	 * finished a touch event. Useful when you want to append extra UI events
	 * (such as sounds). 
	 */
	public static interface OnSlideEventListener<T extends View> {

		public void onSlideEvent(final InnerBaseWindow<T> innerWin, State state, Mode mode);

	}
	
	public static interface OnSlideToRefreshListener2<T extends View>{
		
		public void onSlideLeftToRefresh(final InnerBaseWindow<T> innerWin);

		public void onSlideRightToRefresh(final InnerBaseWindow<T> innerWin);

	}
	
	public static enum State {

		/**
		 * When the Inner-UI is in a state which means that user is not interacting
		 * with the LongPress-to-Refresh function.
		 */
		RESET(0x0),

		/**
		 * When the Inner-UI is being long pressed by the user, but has not been pressed long
		 * enough so that it refreshes when released.
		 */
		SLIDE_TO_REFRESH(0x1),

		/**
		 * When the Inner-UI is being long pressed by the user, and <strong>has</strong>
		 * been pressed long enough so that it will refresh when released.
		 */
		RELEASE_TO_REFRESH(0x2),

		/**
		 * When the Inner-UI is currently refreshing, caused by a long press gesture.
		 */
		REFRESHING(0x8),

		/**
		 * When the Inner-UI is currently refreshing, caused by a call to
		 * {@link InnerBase#setRefreshing() setRefreshing()}.
		 */
		MANUAL_REFRESHING(0x9),
		
		/**
		 * When the UI is currently overscrolling, caused by a fling on the
		 * Refreshable View.
		 */
		OVERSCROLLING(0x10);
		
		/**
		 * Maps an int to a specific state. This is needed when saving state.
		 * 
		 * @param stateInt - int to map a State to
		 * @return State that stateInt maps to
		 */
		
		
		static State mapIntToValue(final int stateInt) {
			for (State value : State.values()) {
				if (stateInt == value.getIntValue()) {
					return value;
				}
			}

			// If not, return default
			return RESET;
		}

		private int mIntValue;

		State(int intValue) {
			mIntValue = intValue;
		}

		int getIntValue() {
			return mIntValue;
		}
	}
	
	public static enum AnimationStyle {
		/**
		 * This is the default for Android-PullToRefresh. Allows you to use any
		 * drawable, which is automatically rotated and used as a Progress Bar.
		 */
		ROTATE,

		/**
		 * This is the old default, and what is commonly used on iOS. Uses an
		 * arrow image which flips depending on where the user has scrolled.
		 */
		FLIP;

		static AnimationStyle getDefault() {
			return ROTATE;
		}

		/**
		 * Maps an int to a specific mode. This is needed when saving state, or
		 * inflating the view from XML where the mode is given through a attr
		 * int.
		 * 
		 * @param modeInt - int to map a Mode to
		 * @return Mode that modeInt maps to, or ROTATE by default.
		 */
		static AnimationStyle mapIntToValue(int modeInt) {
			switch (modeInt) {
				case 0x0:
				default:
					return ROTATE;
				case 0x1:
					return FLIP;
			}
		}

		LoadingLayout createLoadingLayout(Context context, Mode mode, TypedArray attrs) {
			switch (this) {
				case ROTATE:
				default:
					return new RotateLoadingLayout(context, mode, attrs);
				case FLIP:
					return new FlipLoadingLayout(context, mode, attrs);
			}
		}
	}
	
	/**
	 * Used internally for adding view. Need because we override addView to
	 * pass-through to the Refreshable View
	 */
	protected final void addViewInternal(View child, int index, ViewGroup.LayoutParams params) {
		super.addView(child, index, params);
	}

	/**
	 * Used internally for adding view. Need because we override addView to
	 * pass-through to the Refreshable View
	 */
	protected final void addViewInternal(View child, ViewGroup.LayoutParams params) {
		super.addView(child, -1, params);
	}

	protected LoadingLayout createLoadingLayout(Context context, Mode mode, TypedArray attrs) {
		LoadingLayout layout = mLoadingAnimationStyle.createLoadingLayout(context, mode, attrs);
		layout.setVisibility(View.INVISIBLE);
		return layout;
	}

	/**
	 * Used internally for {@link #getLoadingLayoutProxy(boolean, boolean)}.
	 * Allows derivative classes to include any extra LoadingLayouts.
	 */
	protected LoadingLayoutProxy createLoadingLayoutProxy(final boolean includeStart, final boolean includeEnd) {
		LoadingLayoutProxy proxy = new LoadingLayoutProxy();

		if (includeStart && mMode.showLefterLoadingLayout()) {
			proxy.addLayout(mLefterLayout);
		}
		if (includeEnd && mMode.showRighterLoadingLayout()) {
			proxy.addLayout(mRighterLayout);
		}

		return proxy;
	}
	
	public static enum Mode {

		/**
		 * Disable all Slide-to-Refresh gesture and Refreshing handling
		 */
		DISABLED(0x0),

		/**
		 * Only allow the user to Pull from the start of the Refreshable View to
		 * refresh. The start is either the Top or Left, depending on the
		 * scrolling direction.
		 */
		SLIDE_FROM_LEFT(0x1),

		/**
		 * Only allow the user to Pull from the end of the Refreshable View to
		 * refresh. The start is either the Bottom or Right, depending on the
		 * scrolling direction.
		 */
		SLIDE_FROM_RIGHT(0x2),

		/**
		 * Allow the user to both Pull from the start, from the end to refresh.
		 */
		BOTH(0x3),

		/**
		 * Disables Pull-to-Refresh gesture handling, but allows manually
		 * setting the Refresh state via
		 * {@link PullToRefreshBase#setRefreshing() setRefreshing()}.
		 */
		MANUAL_REFRESH_ONLY(0x4);

		/**
		 * Maps an int to a specific mode. This is needed when saving state, or
		 * inflating the view from XML where the mode is given through a attr
		 * int.
		 * 
		 * @param modeInt - int to map a Mode to
		 * @return Mode that modeInt maps to, or PULL_FROM_START by default.
		 */
		static Mode mapIntToValue(final int modeInt) {
			for (Mode value : Mode.values()) {
				if (modeInt == value.getIntValue()) {
					return value;
				}
			}

			// If not, return default
			return getDefault();
		}

		static Mode getDefault() {
			return SLIDE_FROM_LEFT;
		}

		private int mIntValue;

		// The modeInt values need to match those from attrs.xml
		Mode(int modeInt) {
			mIntValue = modeInt;
		}

		/**
		 * @return true if the mode permits Pull-to-Refresh
		 */
		boolean permitsSlideToRefresh() {
			return !(this == DISABLED || this == MANUAL_REFRESH_ONLY);
		}

		/**
		 * @return true if this mode wants the Loading Layout Header to be shown
		 */
		public boolean showLefterLoadingLayout() {
			return this == SLIDE_FROM_LEFT || this == BOTH;
		}

		/**
		 * @return true if this mode wants the Loading Layout Footer to be shown
		 */
		public boolean showRighterLoadingLayout() {
			return this == SLIDE_FROM_RIGHT || this == BOTH || this == MANUAL_REFRESH_ONLY;
		}

		int getIntValue() {
			return mIntValue;
		}
	}
	
	
	public void setOnSlideEventListener(OnSlideEventListener<T> listener) {
		mOnSlideEventListener = listener;
	}

	public final void setOnSlideToRefreshListener(OnSlideToRefreshListener<T> listener) {
		mOnSlideToRefreshListener = listener;
		mOnSlideToRefreshListener2 = null;
	}

	public final void setOnSlideToRefreshListener(OnSlideToRefreshListener2<T> listener) {
		mOnSlideToRefreshListener2 = listener;
		mOnSlideToRefreshListener = null;
	}
	
	public void setLongClickable(boolean longClickable) {
		getRefreshableView().setLongClickable(longClickable);
	}

	public final void setMode(Mode mode) {
		if (mode != mMode) {
			mMode = mode;
			updateUIForMode();
		}
	}
	
	final void setState(State state, final boolean... params) {
		mState = state;

		switch (mState) {
			case RESET:
				onReset();
				break;
			case SLIDE_TO_REFRESH:
				onSlideToRefresh();
				break;
			case RELEASE_TO_REFRESH:
				onReleaseToRefresh();
				break;
			case REFRESHING:
			case MANUAL_REFRESHING:
				onRefreshing(params[0]);
				break;
			case OVERSCROLLING:
				// NO-OP
				break;
		}

		// Call OnPullEventListener
		if (null != mOnSlideEventListener) {
			mOnSlideEventListener.onSlideEvent(this, mState, mCurrentMode);
		}
	}
	
	/**
	 * Updates the View State when the mode has been set. This does not do any
	 * checking that the mode is different to current state so always updates.
	 */
	protected void updateUIForMode() {
		// We need to use the correct LayoutParam values, based on scroll
		// direction
		final LinearLayout.LayoutParams lp = getLoadingLayoutLayoutParams();

		// Remove Header, and then add Header Loading View again if needed
		if (this == mLefterLayout.getParent()) {
			removeView(mLefterLayout);
		}
		if (mMode.showLefterLoadingLayout()) {
			addViewInternal(mLefterLayout, 0, lp);
		}

		// Remove Footer, and then add Footer Loading View again if needed
		if (this == mRighterLayout.getParent()) {
			removeView(mRighterLayout);
		}
		if (mMode.showRighterLoadingLayout()) {
			addViewInternal(mRighterLayout, lp);
		}

		// Hide Loading Views
		refreshLoadingViewsSize();

		// If we're not using Mode.BOTH, set mCurrentMode to mMode, otherwise
		// set it to slide right
		mCurrentMode = (mMode != Mode.BOTH) ? mMode : Mode.SLIDE_FROM_LEFT;
	}
	
	/**
	 * Re-measure the Loading Views height, and adjust internal padding as
	 * necessary
	 */
	protected final void refreshLoadingViewsSize() {
		final int maximumSlideScroll = (int) (getMaximumSlideScroll() * 1.2f);

		int pLeft = getPaddingLeft();
		int pTop = getPaddingTop();
		int pRight = getPaddingRight();
		int pBottom = getPaddingBottom();
		
		if (mMode.showLefterLoadingLayout()) {
			mLefterLayout.setWidth(maximumSlideScroll);
			pLeft = -maximumSlideScroll;
		} else {
			pLeft = 0;
		}
		
		if (mMode.showRighterLoadingLayout()) {
			mRighterLayout.setWidth(maximumSlideScroll);
			pRight = -maximumSlideScroll;
		} else {
			pRight = 0;
		}
		
		setPadding(pLeft, pTop, pRight, pBottom);
	}
	
	private LinearLayout.LayoutParams getLoadingLayoutLayoutParams() {
		return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
						LinearLayout.LayoutParams.MATCH_PARENT);	
	}

	/**
	 * Called when the UI has been to be updated to be in the
	 * {@link State#PULL_TO_REFRESH} state.
	 */
	protected void onSlideToRefresh() {
		switch (mCurrentMode) {
			case SLIDE_FROM_RIGHT:
				mRighterLayout.slideToRefresh();
				break;
			case SLIDE_FROM_LEFT:
				mLefterLayout.slideToRefresh();
				break;
			default:
				// NO-OP
				break;
		}
	}

	/**
	 * Called when the UI has been to be updated to be in the
	 * {@link State#REFRESHING} or {@link State#MANUAL_REFRESHING} state.
	 * 
	 * @param doScroll - Whether the UI should scroll for this event.
	 */
	protected void onRefreshing(final boolean doScroll) {
		if (mMode.showLefterLoadingLayout()) {
			mLefterLayout.refreshing();
		}
		if (mMode.showRighterLoadingLayout()) {
			mRighterLayout.refreshing();
		}

		if (doScroll) {
			if (mShowViewWhileRefreshing) {

				// Call Refresh Listener when the Scroll has finished
				OnSmoothScrollFinishedListener listener = new OnSmoothScrollFinishedListener() {
					@Override
					public void onSmoothScrollFinished() {
						callRefreshListener();
					}
				};

				switch (mCurrentMode) {
					case MANUAL_REFRESH_ONLY:
					case SLIDE_FROM_RIGHT:
						smoothScrollTo(getLefterSize(), listener);
						break;
					default:
					case SLIDE_FROM_LEFT:
						smoothScrollTo(-getLefterSize(), listener);
						break;
				}
			} else {
				smoothScrollTo(0);
			}
		} else {
			// We're not scrolling, so just call Refresh Listener now
			callRefreshListener();
		}
	}
	
	
	@Override
	public final boolean onTouchEvent(MotionEvent event) {

		if (!isSlideToRefreshEnabled()) {
			return false;
		}

		// If we're refreshing, and the flag is set. Eat the event
		if (!mScrollingWhileRefreshingEnabled && isRefreshing()) {
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE: {
				if (mIsBeingDragged) {
					mLastMotionY = event.getY();
					mLastMotionX = event.getX();
					slideEvent();
					return true;
				}
				break;
			}

			case MotionEvent.ACTION_DOWN: {
				if (isReadyForSlide()) {
					mLastMotionY = mInitialMotionY = event.getY();
					mLastMotionX = mInitialMotionX = event.getX();
					return true;
				}
				break;
			}

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				if (mIsBeingDragged) {
					mIsBeingDragged = false;

					if (mState == State.RELEASE_TO_REFRESH
							&& (null != mOnSlideToRefreshListener || null != mOnSlideToRefreshListener2)) {
						setState(State.REFRESHING, true);
						return true;
					}

					// If we're already refreshing, just scroll back to the top
					if (isRefreshing()) {
						smoothScrollTo(0);
						return true;
					}

					// If we haven't returned by here, then we're not in a state
					// to pull, so just reset
					setState(State.RESET);

					return true;
				}
				break;
			}
		}

		return false;
	}
	
	@Override
	public final boolean onInterceptTouchEvent(MotionEvent event) {

		if (!isSlideToRefreshEnabled()) {
			return false;
		}

		final int action = event.getAction();

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			mIsBeingDragged = false;
			return false;
		}

		if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
			return true;
		}

		switch (action) {
			case MotionEvent.ACTION_MOVE: {
				// If we're refreshing, and the flag is set. Eat all MOVE events
				if (!mScrollingWhileRefreshingEnabled && isRefreshing()) {
					return true;
				}

				if (isReadyForSlide()) {
					final float y = event.getY(), x = event.getX();
					final float diff, oppositeDiff, absDiff;

					// We need to use the correct values, based on scroll
					// direction
					diff = x - mLastMotionX;
					oppositeDiff = y - mLastMotionY;
							
					absDiff = Math.abs(diff);

					if (absDiff > mTouchSlop && (!mFilterTouchEvents || absDiff > Math.abs(oppositeDiff))) {
						if (mMode.showLefterLoadingLayout() && diff >= 1f && isReadyForSlideLeft()) {
							System.out.println(" isReadyForSlideLeft()");
							mLastMotionY = y;
							mLastMotionX = x;
							mIsBeingDragged = true;
							if (mMode == Mode.BOTH) {
								mCurrentMode = Mode.SLIDE_FROM_LEFT;
							}
						} else if (mMode.showRighterLoadingLayout() && diff <= -1f && isReadyForSlideRight()) {
							mLastMotionY = y;
							mLastMotionX = x;
							mIsBeingDragged = true;
							if (mMode == Mode.BOTH) {
								mCurrentMode = Mode.SLIDE_FROM_RIGHT;
							}
						}
					}
				}
				break;
			}
			case MotionEvent.ACTION_DOWN: {
				if (isReadyForSlide()) {
					mLastMotionY = mInitialMotionY = event.getY();
					mLastMotionX = mInitialMotionX = event.getX();
					mIsBeingDragged = false;
				}
				break;
			}
		}

		return mIsBeingDragged;
	}
	
	public final void onRefreshComplete() {
		if (isRefreshing()) {
			setState(State.RESET);
		}
	}
	
	/**
	 * Actions a slide Event
	 * 
	 * @return true if the Event has been handled, false if there has been no
	 *         change
	 */
	private void slideEvent() {
		System.out.println("slideEvent");
		final int newScrollValue;
		final int itemDimension;
		final float initialMotionValue, lastMotionValue;

		initialMotionValue = mInitialMotionX;
		lastMotionValue = mLastMotionX;
			
		switch (mCurrentMode) {
			case SLIDE_FROM_RIGHT:
				newScrollValue = Math.round(Math.max(initialMotionValue - lastMotionValue, 0) / FRICTION);
				itemDimension = getRighterSize();
				break;
			case SLIDE_FROM_LEFT:
			default:
				newScrollValue = Math.round(Math.min(initialMotionValue - lastMotionValue, 0) / FRICTION);
				itemDimension = getLefterSize();
				break;
		}

		setLefterScroll(newScrollValue);

		if (newScrollValue != 0 && !isRefreshing()) {
			float scale = Math.abs(newScrollValue) / (float) itemDimension;
			switch (mCurrentMode) {
				case SLIDE_FROM_RIGHT:
					mRighterLayout.onSlide(scale);
					break;
				case SLIDE_FROM_LEFT:
				default:
					mLefterLayout.onSlide(scale);
					break;
			}

			if (mState != State.SLIDE_TO_REFRESH && itemDimension >= Math.abs(newScrollValue)) {
				setState(State.SLIDE_TO_REFRESH);
			} else if (mState == State.SLIDE_TO_REFRESH && itemDimension < Math.abs(newScrollValue)) {
				setState(State.RELEASE_TO_REFRESH);
			}
		}
	}

	private boolean isReadyForSlide() {
		switch (mMode) {
			case SLIDE_FROM_LEFT:
				return isReadyForSlideLeft();
			case SLIDE_FROM_RIGHT:
				return isReadyForSlideRight();
			case BOTH:
				return isReadyForSlideRight() || isReadyForSlideLeft();
			default:
				return false;
		}
	}
	
	/**
	 * Implemented by derived class to return whether the View is in a state
	 * where the user can Pull to Refresh by scrolling from the end.
	 * 
	 * @return true if the View is currently in the correct state (for example,
	 *         bottom of a ListView)
	 */
	protected abstract boolean isReadyForSlideRight();

	/**
	 * Implemented by derived class to return whether the View is in a state
	 * where the user can Pull to Refresh by scrolling from the start.
	 * 
	 * @return true if the View is currently the correct state (for example, top
	 *         of a ListView)
	 */
	protected abstract boolean isReadyForSlideLeft();

	protected final LoadingLayout getRighterLayout() {
		return mRighterLayout;
	}

	protected final int getRighterSize() {
		return mRighterLayout.getContentSize();
	}

	protected final LoadingLayout getLefterLayout() {
		return mLefterLayout;
	}

	protected final int getLefterSize() {
		return mLefterLayout.getContentSize();
	}
	
	private void callRefreshListener() {
		if (null != mOnSlideToRefreshListener) {
			mOnSlideToRefreshListener.onSlideToRefresh(this);
		} else if (null != mOnSlideToRefreshListener2) {
			if (mCurrentMode == Mode.SLIDE_FROM_LEFT) {
				mOnSlideToRefreshListener2.onSlideRightToRefresh(this);
			} else if (mCurrentMode == Mode.SLIDE_FROM_RIGHT) {
				mOnSlideToRefreshListener2.onSlideLeftToRefresh(this);
			}
		}
	}
	/**
	 * Called when the UI has been to be updated to be in the
	 * {@link State#RESET} state.
	 */
	protected void onReset() {
		mIsBeingDragged = false;
		mLayoutVisibilityChangesEnabled = true;

		// Always reset both layouts, just in case...
		mLefterLayout.reset();
		mRighterLayout.reset();

		smoothScrollTo(0);
	}

	
	/**
	 * Called when the UI has been to be updated to be in the
	 * {@link State#RELEASE_TO_REFRESH} state.
	 */
	protected void onReleaseToRefresh() {
		switch (mCurrentMode) {
			case SLIDE_FROM_RIGHT:
				mRighterLayout.releaseToRefresh();
				break;
			case SLIDE_FROM_LEFT:
				mLefterLayout.releaseToRefresh();
				break;
			default:
				// NO-OP
				break;
		}
	}

	final class SmoothScrollRunnable implements Runnable {
		private final Interpolator mInterpolator;
		private final int mScrollToY;
		private final int mScrollFromY;
		private final long mDuration;
		private OnSmoothScrollFinishedListener mListener;

		private boolean mContinueRunning = true;
		private long mStartTime = -1;
		private int mCurrentY = -1;

		public SmoothScrollRunnable(int fromY, int toY, long duration, OnSmoothScrollFinishedListener listener) {
			mScrollFromY = fromY;
			mScrollToY = toY;
			mInterpolator = mScrollAnimationInterpolator;
			mDuration = duration;
			mListener = listener;
		}

		@Override
		public void run() {

			/**
			 * Only set mStartTime if this is the first time we're starting,
			 * else actually calculate the Y delta
			 */
			if (mStartTime == -1) {
				mStartTime = System.currentTimeMillis();
			} else {

				/**
				 * We do do all calculations in long to reduce software float
				 * calculations. We use 1000 as it gives us good accuracy and
				 * small rounding errors
				 */
				long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

				final int deltaY = Math.round((mScrollFromY - mScrollToY)
						* mInterpolator.getInterpolation(normalizedTime / 1000f));
				mCurrentY = mScrollFromY - deltaY;
				setLefterScroll(mCurrentY);
			}

			// If we're not at the target Y, keep going...
			if (mContinueRunning && mScrollToY != mCurrentY) {
				ViewCompat.postOnAnimation(InnerBaseWindow.this, this);
			} else {
				if (null != mListener) {
					mListener.onSmoothScrollFinished();
				}
			}
		}

		public void stop() {
			mContinueRunning = false;
			removeCallbacks(this);
		}
	}

}
