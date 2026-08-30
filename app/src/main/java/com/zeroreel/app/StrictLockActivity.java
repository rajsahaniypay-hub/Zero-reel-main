package com.zeroreel.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StrictLockActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strict_lock);

        ((TextView) findViewById(R.id.text_required_commands)).setText(ProtectLock.requiredCommands(this));

        findViewById(R.id.btn_open_accounts).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_SYNC_SETTINGS),
                        "Remove Google accounts on this phone, then run the commands. Add them back after."));

        findViewById(R.id.btn_open_developer).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
                        "Turn on USB debugging or Wireless debugging."));

        findViewById(R.id.btn_copy_required).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Zero Reel device owner", ProtectLock.requiredCommands(this)));
            Toast.makeText(this, "Copied all three commands. Run them in order.", Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.btn_apply_now).setOnClickListener(v -> {
            if (!ProtectLock.isDeviceOwner(this)) {
                Toast.makeText(this, "Device Owner is required. Remove Google accounts and run the first command.", Toast.LENGTH_LONG).show();
                refresh();
                return;
            }
            if (!AccessibilityKeeper.canRestore(this)) {
                Toast.makeText(this, "Device Owner is set. Run the WRITE_SECURE_SETTINGS grant next.", Toast.LENGTH_LONG).show();
                ProtectLock.apply(this);
                refresh();
                return;
            }
            if (ProtectLock.apply(this)) {
                completeAndOpenMain();
            } else {
                Toast.makeText(this, "Could not apply Device Owner policies. Run the commands again.", Toast.LENGTH_LONG).show();
            }
            refresh();
        });

        findViewById(R.id.btn_done).setOnClickListener(v -> {
            if (ProtectLock.ready(this)) {
                completeAndOpenMain();
            } else {
                Toast.makeText(this, "Zero Reel needs Device Owner before it will arm.", Toast.LENGTH_LONG).show();
            }
        });
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        if (ProtectLock.ready(this) && Prefs.setupComplete(this)) {
            ProtectLock.apply(this);
        }
    }

    private void completeAndOpenMain() {
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
        TextView status = findViewById(R.id.text_lock_status);
        if (ProtectLock.ready(this)) {
            status.setText("Device Owner and best settings are on. You can add Google accounts back now.");
            status.setTextColor(0xFF2E7D32);
        } else if (ProtectLock.isDeviceOwner(this) && AccessibilityKeeper.canRestore(this)) {
            status.setText("Almost ready. Tap apply lock.");
            status.setTextColor(0xFFFFA000);
        } else if (ProtectLock.isDeviceOwner(this)) {
            status.setText("Device Owner is set. Run the grant command so Accessibility stays on.");
            status.setTextColor(0xFFFFA000);
        } else {
            status.setText("Device Owner is required. Android will reject the first command until Google accounts are removed from this phone.");
            status.setTextColor(0xFFC62828);
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
