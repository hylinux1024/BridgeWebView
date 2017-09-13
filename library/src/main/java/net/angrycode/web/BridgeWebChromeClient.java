package net.angrycode.web;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.VideoView;

import net.angrycode.library.R;


/**
 * 加强版WebChromeClient
 * Created by wecodexyz on 2016/12/23.
 */
public class BridgeWebChromeClient extends WebChromeClient {

    private static final int API = android.os.Build.VERSION.SDK_INT;
    private static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    private CustomViewCallback mCustomViewCallback;
    private Activity mActivity;
    private View mCustomView;
    private VideoView mVideoView;
    private FrameLayout mFullscreenContainer;

    private int mOriginalOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    public BridgeWebChromeClient() {
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        LogUtils.d(consoleMessage.message() + " -- From line "
                + consoleMessage.lineNumber() + " of "
                + consoleMessage.sourceId());
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        showConfirmMessage(view.getContext(), message, result);
        return true;
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        showAlertMessage(view.getContext(), message, result);
        return true;
    }


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

    protected void showConfirmMessage(Context context, String message, final JsResult result) {
        if (context == null || TextUtils.isEmpty(message)) {
            return;
        }
        try {
            new AlertDialog.Builder(context).setMessage(message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.cancel();
                        }
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void showAlertMessage(Context context, String message, final JsResult jsResult) {
        if (context == null) {
            return;
        }
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    jsResult.confirm();
                }
            }).setCancelable(false).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method sets whether or not the activity will display
     * in full-screen mode (i.e. the ActionBar will be hidden) and
     * whether or not immersive mode should be set. This is used to
     * set both parameters correctly as during a full-screen video,
     * both need to be set, but other-wise we leave it up to user
     * preference.
     *
     * @param enabled   true to enable full-screen, false otherwise
     * @param immersive true to enable immersive mode, false otherwise
     */
    private void setFullscreen(boolean enabled, boolean immersive) {
//        mIsFullScreen = enabled;
//        mIsImmersive = immersive;
        Activity activity = mActivity;
        if (activity == null || activity.isFinishing()) {
            return;
        }
        Window window = activity.getWindow();
        View decor = window.getDecorView();
        if (enabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (immersive) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }

    }
}
