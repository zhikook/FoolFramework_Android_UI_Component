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

package zy.fool.extras;

import java.util.HashMap;

import zy.fool.internal.InnerBaseWindow.Mode;
import zy.fool.widget.IPullEventListener;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.View;

public class SoundEventListener<V extends View> implements IPullEventListener {

	private final Context mContext;
	private final HashMap<Mode, Integer> mSoundMap; //状态和资源的绑定
	private MediaPlayer mCurrentMediaPlayer;

	/**
	 * Constructor
	 * 
	 * @param context - Context
	 */
	public SoundEventListener(Context context) {
		mContext = context;
		mSoundMap = new HashMap<Mode, Integer>();
	}

	public final void onTouchEvent(Mode event) {
		Integer soundResIdObj = mSoundMap.get(event);
		if (null != soundResIdObj) {
			playSound(soundResIdObj.intValue());
		}
	}

	/**
	 * Set the Sounds to be played when a Pull Event happens. You specify which
	 * sound plays for which events by calling this method multiple times for
	 * each event.
	 * <p/>
	 * If you've already set a sound for a certain event, and add another sound
	 * for that event, only the new sound will be played.
	 * 
	 * @param event - The event for which the sound will be played.
	 * @param resId - Resource Id of the sound file to be played (e.g.
	 *            <var>R.raw.pull_sound</var>)
	 */
	public void addSoundEvent(Mode event, int resId) {
		mSoundMap.put(event, resId);
	}

	/**
	 * Clears all of the previously set sounds and events.
	 */
	public void clearSounds() {
		mSoundMap.clear();
	}

	/**
	 * Gets the current (or last) MediaPlayer instance.
	 */
	public MediaPlayer getCurrentMediaPlayer() {
		return mCurrentMediaPlayer;
	}

	private void playSound(int resId) {
		// Stop current player, if there's one playing
		if (null != mCurrentMediaPlayer) {
			mCurrentMediaPlayer.stop();
			mCurrentMediaPlayer.release();
		}

		mCurrentMediaPlayer = MediaPlayer.create(mContext, resId);
		if (null != mCurrentMediaPlayer) {
			mCurrentMediaPlayer.start();
		}
	}

}
