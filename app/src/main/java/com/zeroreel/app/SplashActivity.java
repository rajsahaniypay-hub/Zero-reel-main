package com.zeroreel.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> next;
            if (!Prefs.setupComplete(this)) {
                next = SetupActivity.class;
            } else if (!ProtectLock.ready(this)) {
                next = StrictLockActivity.class;
            } else {
                next = MainActivity.class;
            }
            startActivity(new Intent(this, next));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 900);
    }
}
