### Android WebView

使用WebView开发的坑很多，这是众所周知的。本文分别对WebView的三个基本控件（俗称三剑客`WebViewClient`，`WebChromeClient`,`WebView`）做了一些封装，方便使用，避免掉坑里。

主要有以下功能：

1. **自定义出错页面，并实现重新加载事件**
2. **全屏播放视频**
3. **封装更加简单易用生命周期api，使用这些生命周期的方法可以避免很多与H5交互的坑**

#### CustomWebViewClient

在`WebViewClient`中主要是对`onReceivedError`方法进行重写。这里面的逻辑这样的：

1. 出错的url如果跟打开的url是一样的，那么这个时候显示自定义的出错页面。这个自定义页面是一个本地静态html。放在assets目录下。
2. 如果这个出错的url就是本地的静态文件，那么也显示自定义访问出错页面。

```java
public class CustomWebViewClient extends WebViewClient {
  
    ...
      
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (view == null) {
            return; // 极端情况下可能出现，加个保险
        }
        boolean isMainFrame = view.getUrl() != null && view.getUrl().equals(failingUrl);
        String url = view.getUrl();
        //这判断非常重要，避免打开一个页面时，显示出来了内容，然后有被重定向到一个无效地址
        if (isMainFrame || url.equalsIgnoreCase(CustomWebView.CUSTOM_ERROR_PAGE)) {//或者加载本地时也发生错误
            if (view instanceof CustomWebView) {
                ((CustomWebView) view).onErrorView(url);
            }
        } else {
            Log.d(TAG, "did not show error view! reload url:" + view.getUrl());
        }

        Log.d(TAG, "onReceivedError-> errorCode=" + errorCode + ",desc=" + description + ",failingUrl=" + failingUrl);
    }

    @TargetApi(23)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (view == null) {
            return; // 极端情况下可能出现，加个保险
        }
        super.onReceivedError(view, request, error);//这里会回调上面那个onReceivedError方法
        log(request);
    }
    ...
}

```

#### CustomWebChromeClient

在这里主要是实现视频全屏播放的逻辑，重写`onShowCustomView()`和`onHideCustomView()`方法

```java
/**
 * 加强版WebChromeClient
 * Created by wecodexyz on 2016/12/23.
 */
public class CustomWebChromeClient extends WebChromeClient {

    ...

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (view == null) {
            return;
        }
        mActivity = AppUtils.getCurrActivity(view.getContext());
        Activity activity = mActivity;
        if (activity == null || activity.isFinishing()) {
            LogUtils.e("must use activity context to show video view!");
            return;
        }
        if (mCustomView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }
        try {
            view.setKeepScreenOn(true);
        } catch (SecurityException e) {
            LogUtils.d("WebView is not allowed to keep the screen on");
        }

        mOriginalOrientation = activity.getRequestedOrientation();
        FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
        mFullscreenContainer = new FrameLayout(activity.getApplicationContext());
//        mFullscreenContainer.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), android.R.color.black));
        mCustomView = view;
        mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setFullscreen(true, true);
//        mCurrentView.setVisibility(View.GONE);
        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                mVideoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                mVideoView.setOnErrorListener(new VideoCompletionListener());
                mVideoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
        mCustomViewCallback = callback;


    }

    @Override
    public void onHideCustomView() {
        if (mCustomView == null || mCustomViewCallback == null) {
            return;
        }
        LogUtils.d("onHideCustomView");
//        mCurrentView.setVisibility(View.VISIBLE);
        try {
            mCustomView.setKeepScreenOn(false);
        } catch (SecurityException e) {
            LogUtils.d("WebView is not allowed to keep the screen on");
        }
        setFullscreen(false, false);
        Activity activity = mActivity;
        if (activity == null || activity.isFinishing()) {
            return;
        }
        FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
        if (decor != null) {
            decor.removeView(mFullscreenContainer);
        }

        if (API < Build.VERSION_CODES.KITKAT) {
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Throwable ignored) {

            }
        }
        mFullscreenContainer = null;
        mCustomView = null;
        if (mVideoView != null) {
            mVideoView.setOnErrorListener(null);
            mVideoView.setOnCompletionListener(null);
            mVideoView = null;
        }
        activity.setRequestedOrientation(mOriginalOrientation);
        mActivity = null;
    }
    ...
}
```

#### CustomWebView

在CustomWebView中封装了生命周期方法，`resume()`,`pause()`,`destroy()` 这几个方法对应于Activity或者Fragment中的生命周期方法。同时还自定义访问出错页面。

有了以上三个基本控件的封装，那么使用起来就非常简单了。

```java
public class WebViewActivity extends AppCompatActivity {

    ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        ...
        mWebView.loadUrl(url);
        mWebView.setWebViewClient(new CustomWebViewClient());
        mWebView.setWebChromeClient(new CustomWebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                mProgressBar.setProgress(newProgress);
                mProgressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
    }

    ...

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.resume();
    }
}
```

#### 其他注意

由于`WebViewActivity`中有实现视频全屏播放的功能，那么在`CustomWebView`中的初始化中需要对`WebView`作以下配置

```java
void settings() {
        WebSettings setting = getSettings();
        setting.setJavaScriptEnabled(true);
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        setting.setAllowFileAccess(false);
//        setting.setAllowFileAccessFromFileURLs(true);
        setting.setBuiltInZoomControls(false);
        setting.setSupportZoom(false);
        setting.setDisplayZoomControls(false);
        setting.setSaveFormData(false);
        setting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        setting.setCacheMode(WebSettings.LOAD_NO_CACHE);// 不使用缓存
        setting.setDefaultTextEncodingName("UTF-8");
        setting.setRenderPriority(WebSettings.RenderPriority.HIGH);
        // 自适应屏幕
        setting.setUseWideViewPort(true);
        setting.setLoadWithOverviewMode(true);
        setting.setDomStorageEnabled(true);//应该设置为true，否则视频的缩略图会被放大
    }
```

以上配置都在实际开发中得到验证的，一般来说，这样配置是可以满足很多需求的。

另外如果需要显示全屏，那么需要在`WebViewActivity`的`manifiest`中的`configChanges`属性配置如下:

```java
<activity
            android:name=".WebViewActivity"
            android:label="Web"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:screenOrientation="portrait"/>
```





