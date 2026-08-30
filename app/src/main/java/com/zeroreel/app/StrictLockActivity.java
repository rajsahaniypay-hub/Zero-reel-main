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

        ((TextView) findViewById(R.id.text_adb_command)).setText(ProtectLock.adbCommand(this));

        findViewById(R.id.btn_open_accounts).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_SYNC_SETTINGS), "Open Settings → Accounts and remove Google accounts."));

        findViewById(R.id.btn_open_developer).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
                        "Turn on USB debugging, or Wireless debugging if you will paste the command on the phone."));

        findViewById(R.id.btn_copy_command).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Zero Reel device owner", ProtectLock.adbCommand(this)));
            Toast.makeText(this, "Command copied. Paste it in a PC terminal or an ADB app on this phone.", Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.btn_apply_now).setOnClickListener(v -> {
            if (ProtectLock.apply(this)) {
                Toast.makeText(this, "Strict lock is on. You can add your Google accounts back now.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Not Device Owner yet. Remove accounts, run the command, then tap this again.", Toast.LENGTH_LONG).show();
            }
            refresh();
        });

        findViewById(R.id.btn_done).setOnClickListener(v -> finish());
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        TextView status = findViewById(R.id.text_lock_status);
        if (DeviceStatus.strictUninstallLock(this)) {
            status.setText("Strict lock is on. Uninstall is blocked until you disarm with an authenticator code.");
            status.setTextColor(0xFF2E7D32);
        } else if (ProtectLock.isDeviceOwner(this)) {
            status.setText("This phone is Device Owner. Tap Apply lock to disable Uninstall.");
            status.setTextColor(0xFFFFA000);
        } else {
            status.setText("Not Device Owner yet. Android blocks this while a Google account is on the phone.");
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
