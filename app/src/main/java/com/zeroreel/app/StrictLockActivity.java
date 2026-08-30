package com.zeroreel.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StrictLockActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean finishing;
    private final Runnable watch = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || finishing) return;
            refresh();
            if (ProtectLock.isDeviceOwner(StrictLockActivity.this)) {
                ProtectLock.apply(StrictLockActivity.this);
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strict_lock);

        ((TextView) findViewById(R.id.text_required_commands)).setText(ProtectLock.requiredCommands(this));

        findViewById(R.id.btn_open_accounts).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_SYNC_SETTINGS),
                        "Settings → Accounts → remove Google. Add it back after the lock is on."));

        findViewById(R.id.btn_open_developer).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
                        "Enable USB debugging, or Wireless debugging if you are using aShell."));

        findViewById(R.id.btn_copy_required).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Zero Reel device owner", ProtectLock.requiredCommands(this)));
            Toast.makeText(this, "Copied. Paste it once. This screen will notice when it works.", Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.btn_add_account).setOnClickListener(v -> {
            Intent add = new Intent(Settings.ACTION_ADD_ACCOUNT);
            add.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
            startSafe(add, "Open Settings → Accounts → Add account → Google.");
        });

        findViewById(R.id.btn_apply_now).setOnClickListener(v -> {
            if (ProtectLock.ready(this)) {
                completeAndOpenMain();
            } else {
                Toast.makeText(this, "Still waiting for Device Owner.", Toast.LENGTH_SHORT).show();
            }
        });
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(watch);
        handler.post(watch);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(watch);
        super.onPause();
    }

    private void completeAndOpenMain() {
        if (finishing) return;
        finishing = true;
        ProtectLock.apply(this);
        Prefs.get(this).edit()
                .putBoolean(Prefs.SETUP_COMPLETE, true)
                .putBoolean(Prefs.LOCK_ARMED, true)
                .putBoolean(Prefs.MASTER_ENABLED, true)
                .putBoolean(Prefs.APP_YOUTUBE, true)
                .putBoolean(Prefs.APP_INSTAGRAM, true)
                .putBoolean(Prefs.APP_TIKTOK, true)
                .putBoolean(Prefs.APP_FACEBOOK, true)
                .putBoolean(Prefs.APP_SNAPCHAT, true)
                .apply();
        BlockGuardService.start(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void refresh() {
        boolean owner = ProtectLock.isDeviceOwner(this);
        if (owner) {
            ProtectLock.apply(this);
        }
        boolean ready = ProtectLock.ready(this);

        TextView status = findViewById(R.id.text_lock_status);
        TextView accounts = findViewById(R.id.text_step_accounts);
        TextView debug = findViewById(R.id.text_step_debug);
        TextView command = findViewById(R.id.text_step_command);

        if (ready) {
            status.setText("Device Owner is on. Sign back into Google, then open Zero Reel.");
            status.setTextColor(0xFF2E7D32);
            accounts.setText("1. Done. You can sign back into Google.");
            accounts.setTextColor(0xFF2E7D32);
            debug.setText("2. Done.");
            debug.setTextColor(0xFF2E7D32);
            command.setText("3. Done. Device Owner accepted.");
            command.setTextColor(0xFF2E7D32);
            findViewById(R.id.btn_add_account).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_apply_now).setVisibility(View.VISIBLE);
        } else {
            status.setText("Android only accepts Device Owner after Google accounts are removed from this phone. This screen updates itself after the command succeeds.");
            status.setTextColor(0xFFC62828);
            accounts.setText("1. Remove Google accounts on this phone (temporary).");
            accounts.setTextColor(0xFFC62828);
            debug.setText("2. Turn on USB debugging or Wireless debugging.");
            command.setText("3. Waiting for the command…");
            findViewById(R.id.btn_add_account).setVisibility(View.GONE);
            findViewById(R.id.btn_apply_now).setVisibility(View.GONE);
        }
    }

    private void startSafe(Intent intent, String fallback) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, fallback, Toast.LENGTH_LONG).show();
        }
    }
}
