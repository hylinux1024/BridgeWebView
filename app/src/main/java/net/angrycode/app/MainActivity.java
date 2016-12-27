package net.angrycode.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();

        findViewById(R.id.btn_error_url).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWebErrorView();
            }
        });

        findViewById(R.id.btn_video_enable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVideoFullScreen();
            }
        });
    }

    void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    private void showWebErrorView() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", "http://errorwebsite.com/");
        startActivity(intent);
    }

    private void showVideoFullScreen() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", "http://youku.com/");
        startActivity(intent);
    }

}
