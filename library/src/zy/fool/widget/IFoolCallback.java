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

import android.view.View;

public interface IFoolCallback {
	
	 interface OnPullListener{
		boolean onPull(FoolAdapterView<?> parent, View view, int position, long id);
//     	public boolean onPullOut();
//		public boolean onPullIn();
	}

	static interface OnSlideListener{
		boolean onSlide(FoolAdapterView<?> parent, View view, int position, long id);
//		public boolean onSlideLeft();
//		public boolean onSlideRight();
	}
	
	static interface OnSmoothPullFinishedListener {
		void onSmoothPullFinished();
	}

}
