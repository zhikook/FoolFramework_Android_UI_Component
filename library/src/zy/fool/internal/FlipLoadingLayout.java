package zy.fool.internal;

/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
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

import zy.fool.R;
import zy.fool.internal.InnerBaseWindow.Mode;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView.ScaleType;


@SuppressLint("ViewConstructor")
public class FlipLoadingLayout extends LoadingLayout {

	static final int FLIP_ANIMATION_DURATION = 150;

	private final Animation mRotateAnimation, mResetRotateAnimation;

	public FlipLoadingLayout(Context context, final Mode mode, TypedArray attrs) {
		super(context, mode,attrs);

		final int rotateAngle = mode == Mode.SLIDE_FROM_LEFT ? -180 : 180;

		mRotateAnimation = new RotateAnimation(0, rotateAngle, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
		mRotateAnimation.setFillAfter(true);

		mResetRotateAnimation = new RotateAnimation(rotateAngle, 0, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		mResetRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mResetRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
		mResetRotateAnimation.setFillAfter(true);
	}

	@Override
	protected void onLoadingDrawableSet(Drawable imageDrawable) {
		if (null != imageDrawable) {
			final int dHeight = imageDrawable.getIntrinsicHeight();
			final int dWidth = imageDrawable.getIntrinsicWidth();

			/**
			 * We need to set the width/height of the ImageView so that it is
			 * square with each side the size of the largest drawable dimension.
			 * This is so that it doesn't clip when rotated.
			 */
			ViewGroup.LayoutParams lp = mLefterImage.getLayoutParams();
			lp.width = lp.height = Math.max(dHeight, dWidth);
			mLefterImage.requestLayout();

			/**
			 * We now rotate the Drawable so that is at the correct rotation,
			 * and is centered.
			 */
			mLefterImage.setScaleType(ScaleType.MATRIX);
			Matrix matrix = new Matrix();
			matrix.postTranslate((lp.width - dWidth) / 2f, (lp.height - dHeight) / 2f);
			matrix.postRotate(getDrawableRotationAngle(), lp.width / 2f, lp.height / 2f);
			mLefterImage.setImageMatrix(matrix);
		}
	}

	@Override
	protected void onSlideImpl(float scaleOfLayout) {
		// NO-OP
	}

	@Override
	protected void slideToRefreshImpl() {
		// Only start reset Animation, we've previously show the rotate anim
		if (mRotateAnimation == mLefterImage.getAnimation()) {
			mLefterImage.startAnimation(mResetRotateAnimation);
		}
	}

	@Override
	protected void refreshingImpl() {
		mLefterImage.clearAnimation();
		mLefterImage.setVisibility(View.INVISIBLE);
		mLefterProgress.setVisibility(View.VISIBLE);
	}

	@Override
	protected void releaseToRefreshImpl() {
		mLefterImage.startAnimation(mRotateAnimation);
	}

	@Override
	protected void resetImpl() {
		mLefterImage.clearAnimation();
		mLefterProgress.setVisibility(View.GONE);
		mLefterImage.setVisibility(View.VISIBLE);
	}

	@Override
	protected int getDefaultDrawableResId() {
		return R.drawable.default_str_flip;
	}

	private float getDrawableRotationAngle() {
		float angle = 0f;
		switch (mMode) {
			case SLIDE_FROM_RIGHT:
				angle = 90f;
				break;
			case SLIDE_FROM_LEFT:
				angle = 270f;
				break;
			default:
				break;
		}

		return angle;
	}

}
