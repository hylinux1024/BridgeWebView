package net.angrycode.web;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.BuildConfig;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import net.angrycode.library.R;

import java.lang.reflect.Field;

/**
 * 加强版WebView
 * Created by wecodexyz on 2016/12/23.
 */
public class BridgeWebView extends WebView {

    public static final String CUSTOM_ERROR_PAGE = "file:///android_asset/error.html";

    private static final String TAG = BridgeWebView.class.getSimpleName();

    private String mReloadUrl;

    public BridgeWebView(Context context) {
        super(context);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public BridgeWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init() {
        settings();
        addJavascriptInterface(this, "app");

    }

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

    public synchronized void find(String text) {
        if (Build.VERSION.SDK_INT >= 16) {
            findAllAsync(text);
        } else {
            findAll(text);
        }
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return super.startActionMode(new CustomCallback(getContext(), callback));
    }

    public synchronized void freeWebViewMemory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //noinspection deprecation
            freeMemory();
        }

    }

    /**
     * 显示定制的出错页面
     */
    public void onErrorView(String url) {
        if (!url.equalsIgnoreCase(CUSTOM_ERROR_PAGE)) {
            mReloadUrl = url;
        }
        setVisibility(View.INVISIBLE);
        loadUrl("about:blank");
        try {
            stopLoading();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        if (canGoBack()) {
                goBack();
        }

        loadUrl(CUSTOM_ERROR_PAGE);

    }

    public void resume() {
        onResume();
        resumeTimers();
    }

    public void pause() {
        onPause();
        pauseTimers();
    }

    @Override
    public void destroy() {
        if (isDestroy) {
            return;
        }
        isDestroy = true;
        destroyView();//这些方法必须先于super.destroy()调用，否则coolpad手机退出会闪退
        releaseAllWebViewCallback();
        super.destroy();
    }

    private boolean isDestroy = false;

    private void destroyView() {
        stopLoading();
        ((ViewGroup) getParent()).removeView(this);
        removeAllViews();
        setWebChromeClient(null);
        setWebViewClient(null);
        setOnCreateContextMenuListener(null);
    }

    public void releaseAllWebViewCallback() {
        if (android.os.Build.VERSION.SDK_INT < 16) {
            try {
                Field field = WebView.class.getDeclaredField("mWebViewCore");
                field = field.getType().getDeclaredField("mBrowserFrame");
                field = field.getType().getDeclaredField("sConfigCallback");
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Class clazz = Class.forName("android.webkit.BrowserFrame");
                if (clazz != null) {
                    Field sConfigCallback = clazz.getDeclaredField("sConfigCallback");
                    if (sConfigCallback != null) {
                        sConfigCallback.setAccessible(true);
                        sConfigCallback.set(null, null);
                    }
                }
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    @JavascriptInterface
    public void onErrorReload() {
        if (TextUtils.isEmpty(mReloadUrl)) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                loadUrl(mReloadUrl);
            }
        });
    }

    public static int getPrimaryColor(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static class CustomCallback implements ActionMode.Callback {
        private ActionMode.Callback callback;
        private Context context;

        public CustomCallback(Context context, ActionMode.Callback callback) {
            this.callback = callback;
            this.context = context;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return callback.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int size = menu.size();
            for (int i = 0; i < size; i++) {
                MenuItem menuItem = menu.getItem(i);
                final Drawable moreMenuDrawable = menuItem.getIcon();

                if (moreMenuDrawable != null) {
                    moreMenuDrawable.setColorFilter(getPrimaryColor(context), PorterDuff.Mode.SRC_ATOP);
                    menuItem.setIcon(moreMenuDrawable);

                }
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return callback.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            callback.onDestroyActionMode(mode);
            context = null;
        }
    }
}
