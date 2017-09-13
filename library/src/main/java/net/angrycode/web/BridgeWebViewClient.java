package net.angrycode.web;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Map;
import java.util.Set;

/**
 * 加强版WebViewClient可以定制网页访问出错页面
 * Created by wecodexyz on 2016/12/23.
 */
public class BridgeWebViewClient extends WebViewClient {

    private static final String TAG = BridgeWebViewClient.class.getSimpleName();

    public BridgeWebViewClient() {
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (view == null) {
            return; // 极端情况下可能出现，加个保险
        }
        view.setVisibility(View.VISIBLE);

    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (URLUtil.isValidUrl(url)) {
            return false;
        }
        return true;//默认当前页面打开
    }

    @TargetApi(23)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (view == null) {
            return; // 极端情况下可能出现，加个保险
        }
        boolean isMainFrame = view.getUrl() != null && view.getUrl().equals(failingUrl);
        String url = view.getUrl();
        //这判断非常重要，避免打开一个页面时，显示出来了内容，然后有被重定向到一个无效地址
        if (isMainFrame || url.equalsIgnoreCase(BridgeWebView.CUSTOM_ERROR_PAGE)) {//或者加载本地时也发生错误
            if (view instanceof BridgeWebView) {
                ((BridgeWebView) view).onErrorView(url);
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


    @TargetApi(21)
    void log(WebResourceRequest request) {
        Log.d(TAG, "request---->" + request.getMethod() + "<---->" + request.getUrl());
        Map<String, String> headers = request.getRequestHeaders();
        Set<String> keys = headers.keySet();
        for (String key : keys) {
            Log.d(TAG, "Header " + key + ", " + headers.get(key));
        }
    }


}
