package zy.fool.internal;


import zy.fool.R;
import zy.fool.internal.InnerBaseWindow.Mode;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class LoadingLayout extends FrameLayout implements ILoadingLayout {

	static final String LOG_TAG = "SlideToRefresh-LoadingLayout";

	static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

	private FrameLayout mInnerLayout;

	protected final ImageView mLefterImage;
	protected final ProgressBar mLefterProgress;

	private boolean mUseIntrinsicAnimation;

	private final TextView mLefterText;
	private final TextView mSubLefterText;

	protected final Mode mMode;
	
	private CharSequence mSlideLabel;
	private CharSequence mRefreshingLabel;
	private CharSequence mReleaseLabel;

	public LoadingLayout(Context context, final Mode mode, TypedArray attrs) {
		super(context);
		mMode = mode;
		
		LayoutInflater.from(context).inflate(R.layout.slide_to_refresh_lefter_horizontal, this);

		mInnerLayout = (FrameLayout) findViewById(R.id.fl_inner);
		mLefterText = (TextView) mInnerLayout.findViewById(R.id.slide_to_refresh_text);
		mLefterProgress = (ProgressBar) mInnerLayout.findViewById(R.id.slide_to_refresh_progress);
		mSubLefterText = (TextView) mInnerLayout.findViewById(R.id.slide_to_refresh_sub_text);
		mLefterImage = (ImageView) mInnerLayout.findViewById(R.id.slide_to_refresh_image);

		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInnerLayout.getLayoutParams();

		switch (mode) {
			case SLIDE_FROM_LEFT:
				lp.gravity =  Gravity.LEFT;

				// Load in labels
				mSlideLabel = context.getString(R.string.slide_to_refresh_from_bottom_slide_label);
				mRefreshingLabel = context.getString(R.string.slide_to_refresh_from_bottom_refreshing_label);
				mReleaseLabel = context.getString(R.string.slide_to_refresh_from_bottom_release_label);
				break;

			case SLIDE_FROM_RIGHT:
			default:
				lp.gravity =  Gravity.RIGHT;

				// Load in labels
				mSlideLabel = context.getString(R.string.slide_to_refresh_slide_label);
				mRefreshingLabel = context.getString(R.string.slide_to_refresh_refreshing_label);
				mReleaseLabel = context.getString(R.string.slide_to_refresh_release_label);
				break;
		}

		if (attrs.hasValue(R.styleable.SlideToRefresh_strLefterBackground)) {
			Drawable background = attrs.getDrawable(R.styleable.SlideToRefresh_strLefterBackground);
			if (null != background) {
				ViewCompat.setBackground(this, background);
			}
		}

		if (attrs.hasValue(R.styleable.SlideToRefresh_strLefterTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.SlideToRefresh_strLefterTextAppearance, styleID);
			setTextAppearance(styleID.data);
		}
		if (attrs.hasValue(R.styleable.SlideToRefresh_strSubLefterTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.SlideToRefresh_strSubLefterTextAppearance, styleID);
			setSubTextAppearance(styleID.data);
		}

		// Text Color attrs need to be set after TextAppearance attrs
		if (attrs.hasValue(R.styleable.SlideToRefresh_strLefterTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.SlideToRefresh_strLefterTextColor);
			if (null != colors) {
				setTextColor(colors);
			}
		}
		if (attrs.hasValue(R.styleable.SlideToRefresh_strLefterSubTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.SlideToRefresh_strLefterSubTextColor);
			if (null != colors) {
				setSubTextColor(colors);
			}
		}

		// Try and get defined drawable from Attrs
		Drawable imageDrawable = null;
		if (attrs.hasValue(R.styleable.SlideToRefresh_strDrawable)) {
			imageDrawable = attrs.getDrawable(R.styleable.SlideToRefresh_strDrawable);
		}

		// Check Specific Drawable from Attrs, these overrite the generic
		// drawable attr above
		switch (mode) {
			case SLIDE_FROM_LEFT:
			default:
				if (attrs.hasValue(R.styleable.SlideToRefresh_strDrawableStart)) {
					imageDrawable = attrs.getDrawable(R.styleable.SlideToRefresh_strDrawableStart);
				} else if (attrs.hasValue(R.styleable.SlideToRefresh_strDrawableTop)) {
					//Utils.warnDeprecation("ptrDrawableTop", "ptrDrawableStart");
					imageDrawable = attrs.getDrawable(R.styleable.SlideToRefresh_strDrawableTop);
				}
				break;

			case SLIDE_FROM_RIGHT:
				if (attrs.hasValue(R.styleable.SlideToRefresh_strDrawableEnd)) {
					imageDrawable = attrs.getDrawable(R.styleable.SlideToRefresh_strDrawableEnd);
				} else if (attrs.hasValue(R.styleable.SlideToRefresh_strDrawableBottom)) {
					//Utils.warnDeprecation("ptrDrawableBottom", "ptrDrawableEnd");
					imageDrawable = attrs.getDrawable(R.styleable.SlideToRefresh_strDrawableBottom);
				}
				break;
		}

		// If we don't have a user defined drawable, load the default
		if (null == imageDrawable) {
			imageDrawable = context.getResources().getDrawable(getDefaultDrawableResId());
		}

		// Set Drawable, and save width/height
		setLoadingDrawable(imageDrawable);

		reset();
	}

	public final void setHeight(int height) {
		ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) getLayoutParams();
		lp.height = height;
		requestLayout();
	}

	public final void setWidth(int width) {
		ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) getLayoutParams();
		lp.width = width;
		requestLayout();
	}

	public final int getContentSize() {
		return mInnerLayout.getWidth();	
	}

	public final void hideAllViews() {
		if (View.VISIBLE == mLefterText.getVisibility()) {
			mLefterText.setVisibility(View.INVISIBLE);
		}
		if (View.VISIBLE == mLefterProgress.getVisibility()) {
			mLefterProgress.setVisibility(View.INVISIBLE);
		}
		if (View.VISIBLE == mLefterImage.getVisibility()) {
			mLefterImage.setVisibility(View.INVISIBLE);
		}
		if (View.VISIBLE == mSubLefterText.getVisibility()) {
			mSubLefterText.setVisibility(View.INVISIBLE);
		}
	}

	public final void onSlide(float scaleOfLayout) {
		if (!mUseIntrinsicAnimation) {
			onSlideImpl(scaleOfLayout);
		}
	}

	public final void slideToRefresh() {
		if (null != mLefterText) {
			mLefterText.setText(mSlideLabel);
		}

		// Now call the callback
		slideToRefreshImpl();
	}

	public final void refreshing() {
		if (null != mLefterText) {
			mLefterText.setText(mRefreshingLabel);
		}

		if (mUseIntrinsicAnimation) {
			((AnimationDrawable) mLefterImage.getDrawable()).start();
		} else {
			// Now call the callback
			refreshingImpl();
		}

		if (null != mSubLefterText) {
			mSubLefterText.setVisibility(View.GONE);
		}
	}

	public final void releaseToRefresh() {
		if (null != mLefterText) {
			mLefterText.setText(mReleaseLabel);
		}

		// Now call the callback
		releaseToRefreshImpl();
	}

	public final void reset() {
		if (null != mLefterText) {
			mLefterText.setText(mSlideLabel);
		}
		mLefterImage.setVisibility(View.VISIBLE);

		if (mUseIntrinsicAnimation) {
			((AnimationDrawable) mLefterImage.getDrawable()).stop();
		} else {
			// Now call the callback
			resetImpl();
		}

		if (null != mSubLefterText) {
			if (TextUtils.isEmpty(mSubLefterText.getText())) {
				mSubLefterText.setVisibility(View.GONE);
			} else {
				mSubLefterText.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void setLastUpdatedLabel(CharSequence label) {
		setSubHeaderText(label);
	}

	public final void setLoadingDrawable(Drawable imageDrawable) {
		// Set Drawable
		mLefterImage.setImageDrawable(imageDrawable);
		mUseIntrinsicAnimation = (imageDrawable instanceof AnimationDrawable);

		// Now call the callback
		onLoadingDrawableSet(imageDrawable);
	}

	public void setPullLabel(CharSequence pullLabel) {
		mSlideLabel = pullLabel;
	}

	public void setRefreshingLabel(CharSequence refreshingLabel) {
		mRefreshingLabel = refreshingLabel;
	}

	public void setReleaseLabel(CharSequence releaseLabel) {
		mReleaseLabel = releaseLabel;
	}

	@Override
	public void setTextTypeface(Typeface tf) {
		mLefterText.setTypeface(tf);
	}

	public final void showInvisibleViews() {
		if (View.INVISIBLE == mLefterText.getVisibility()) {
			mLefterText.setVisibility(View.VISIBLE);
		}
		if (View.INVISIBLE == mLefterProgress.getVisibility()) {
			mLefterProgress.setVisibility(View.VISIBLE);
		}
		if (View.INVISIBLE == mLefterImage.getVisibility()) {
			mLefterImage.setVisibility(View.VISIBLE);
		}
		if (View.INVISIBLE == mSubLefterText.getVisibility()) {
			mSubLefterText.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Callbacks for derivative Layouts
	 */

	protected abstract int getDefaultDrawableResId();

	protected abstract void onLoadingDrawableSet(Drawable imageDrawable);

	protected abstract void onSlideImpl(float scaleOfLayout);

	protected abstract void slideToRefreshImpl();

	protected abstract void refreshingImpl();

	protected abstract void releaseToRefreshImpl();

	protected abstract void resetImpl();

	private void setSubHeaderText(CharSequence label) {
		if (null != mSubLefterText) {
			if (TextUtils.isEmpty(label)) {
				mSubLefterText.setVisibility(View.GONE);
			} else {
				mSubLefterText.setText(label);

				// Only set it to Visible if we're GONE, otherwise VISIBLE will
				// be set soon
				if (View.GONE == mSubLefterText.getVisibility()) {
					mSubLefterText.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private void setSubTextAppearance(int value) {
		if (null != mSubLefterText) {
			mSubLefterText.setTextAppearance(getContext(), value);
		}
	}

	private void setSubTextColor(ColorStateList color) {
		if (null != mSubLefterText) {
			mSubLefterText.setTextColor(color);
		}
	}

	private void setTextAppearance(int value) {
		if (null != mLefterText) {
			mLefterText.setTextAppearance(getContext(), value);
		}
		if (null != mSubLefterText) {
			mSubLefterText.setTextAppearance(getContext(), value);
		}
	}

	private void setTextColor(ColorStateList color) {
		if (null != mLefterText) {
			mLefterText.setTextColor(color);
		}
		if (null != mSubLefterText) {
			mSubLefterText.setTextColor(color);
		}
	}

}
