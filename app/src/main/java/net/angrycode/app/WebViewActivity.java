package net.angrycode.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import net.angrycode.web.BridgeWebChromeClient;
import net.angrycode.web.BridgeWebView;
import net.angrycode.web.BridgeWebViewClient;

public class WebViewActivity extends AppCompatActivity {

    BridgeWebView mWebView;
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        String url = getIntent().getStringExtra("url");
        initToolbar();
        mWebView = (BridgeWebView) findViewById(R.id.webview);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mWebView.loadUrl(url);
        mWebView.setWebViewClient(new BridgeWebViewClient());
        mWebView.setWebChromeClient(new BridgeWebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                mProgressBar.setProgress(newProgress);
                mProgressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
    }

    void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }

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
