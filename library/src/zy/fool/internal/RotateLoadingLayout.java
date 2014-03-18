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
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView.ScaleType;

public class RotateLoadingLayout extends LoadingLayout {

	static final int ROTATION_ANIMATION_DURATION = 1200;

	private final Animation mRotateAnimation;
	private final Matrix mLefterImageMatrix;

	private float mRotationPivotX, mRotationPivotY;

	private final boolean mRotateDrawableWhileSliding;

	public RotateLoadingLayout(Context context, Mode mode, TypedArray attrs) {
		super(context, mode, attrs);

		mRotateDrawableWhileSliding = attrs.getBoolean(R.styleable.SlideToRefresh_strRotateDrawableWhileSliding, true);

		mLefterImage.setScaleType(ScaleType.MATRIX);
		mLefterImageMatrix = new Matrix();
		mLefterImage.setImageMatrix(mLefterImageMatrix);

		mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mRotateAnimation.setDuration(ROTATION_ANIMATION_DURATION);
		mRotateAnimation.setRepeatCount(Animation.INFINITE);
		mRotateAnimation.setRepeatMode(Animation.RESTART);
	}

	public void onLoadingDrawableSet(Drawable imageDrawable) {
		if (null != imageDrawable) {
			mRotationPivotX = Math.round(imageDrawable.getIntrinsicWidth() / 2f);
			mRotationPivotY = Math.round(imageDrawable.getIntrinsicHeight() / 2f);
		}
	}

	@Override
	protected void onSlideImpl(float scaleOfLayout) {
		float angle;
		if (mRotateDrawableWhileSliding) {
			angle = scaleOfLayout * 90f;
		} else {
			angle = Math.max(0f, Math.min(180f, scaleOfLayout * 360f - 180f));
		}

		mLefterImageMatrix.setRotate(angle, mRotationPivotX, mRotationPivotY);
		mLefterImage.setImageMatrix(mLefterImageMatrix);
	}

	@Override
	protected void refreshingImpl() {
		mLefterImage.startAnimation(mRotateAnimation);
	}

	@Override
	protected void resetImpl() {
		mLefterImage.clearAnimation();
		resetImageRotation();
	}

	private void resetImageRotation() {
		if (null != mLefterImageMatrix) {
			mLefterImageMatrix.reset();
			mLefterImage.setImageMatrix(mLefterImageMatrix);
		}
	}

	@Override
	protected void slideToRefreshImpl() {
		// NO-OP
	}

	@Override
	protected void releaseToRefreshImpl() {
		// NO-OP
	}

	@Override
	protected int getDefaultDrawableResId() {
		return R.drawable.default_str_rotate;
	}

}
