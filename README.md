#FoolFramework  Android UI Component *NFINSIHED*

Version 0.3655

## Framework Priview

### Welcome guys join us!!!create special UI for android!

### This project aims to to provide more apis of gesture control than the listview widget for android.

the framework is based on AbsListView class，in actual condition , It needs some new gestures,such as pull out /in (Pointers-Spread ,Pointers-pinch), not from top or bottom,but for special position. the framework can recognize these gesture,and provides interfaces such as onPull Out/in Listener、onSlide Left/Right Listener. besides scrolling, the widget can layout as spot-display.the usage introduce more details .

***********************************************************************************************
![FoolFramework](https://raw.github.com/zhikook/FoolFramework_Android_UI_Component/master/pullinout.png)

![FoolFramework](https://raw.github.com/zhikook/FoolFramework_Android_UI_Component/master/slide.png)
***********************************************************************************************
## Usage

Need to create a subclass that extends FooView and layout in the xml file, with import FoollistView.jar 
which contains baseclasses and others.

### activity_main.xml
``` xml
<!-- the FoolListView replaces a standard ListView widget.-->
    ...
<zy.fool.widget.FoolListView
    android:id="@+id/foollist"
    android:layout_height="fill_parent"
    android:layout_width="fill_parent"
    ....
    />
    ...
```

### MainActivity.java
``` java
public class MainActivity extends Activity implements OnPullOutListener{
	
	MyAdapter mAdapter ;
	FoolListView mView ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
    	mView = (FoolListView)findViewById(R.id.foollist);
		mAdapter = new MyAdapter(this);
		mView.setOnPullListener(this);
		mFoolView.setAdapter(mAdapter);
	}
	
	...
    
	@Override
	public boolean onPullOut() {
		// TODO Auto-generated method stub
		
		...Callback Method...
		
		return true;
	}
}

```

## Add Jar Libs



## License

    Copyright 2014 Zhiyong Liu/David Lau
    zhikook # gmail.com # -> @

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


***********************************************************************************************

### Author
* Zhiyong Liu ,At Shanghai,China 
* Blog website:[http://zhiyong.sinaapp.com](http://zhiyong.sinaapp.com)
* weixin:@daliu_v
* weibo:@gswift
* email:zhikook # gmail.com # -> @

***********************************************************************************************

### The Architecture Design of Android (UML)

update 2013.10.16

* [Android System Architecture Design](https://github.com/zhikook/AndroidUML/raw/master/01-Android%20System%20Architecture%20Design%20Introduction%20ch-ok.pdf)
* [Android GUI Architecture Summary](https://github.com/zhikook/AndroidUML/raw/master/02-Android%20GUI%20Architecture%20Summary%20-01-ok.pdf)
* [Application Framework Activity and Window](https://github.com/zhikook/AndroidUML/raw/master/03-Activity%20and%20it's%20window%20-01-ok.pdf)
* [Application Framework WindowManagerService](https://github.com/zhikook/AndroidUML/raw/master/04-Windowmangerservice%20-01-ok.pdf)
* [Application Framework View and ViewRootImpl](https://github.com/zhikook/AndroidUML/raw/master/05-View%20and%20viewrootimpl%20performtraversals-ok.pdf)
* [Android GUI Renderer](https://github.com/zhikook/AndroidUML/raw/master/06-Android%20Renderer-ok.pdf)
* [Android’s SurfaceFlinger](https://github.com/zhikook/AndroidUML/raw/master/07-Android%20SurfaceFlinger-ok.pdf)
* [Android’s Handler](https://github.com/zhikook/AndroidUML/raw/master/08-Android%20Thread%20Looper%20Message%20Handler%20Java%20and%20native%20ok.pdf)
* [Android Input Framework](https://github.com/zhikook/AndroidUML/raw/master/09-Android%20input%20framework-ok.pdf)

