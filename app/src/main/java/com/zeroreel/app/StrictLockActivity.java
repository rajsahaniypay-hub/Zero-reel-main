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

        ((TextView) findViewById(R.id.text_stay_commands)).setText(StaySignedIn.bothCommands(this));
        ((TextView) findViewById(R.id.text_adb_command)).setText(ProtectLock.adbCommand(this));

        findViewById(R.id.btn_open_developer).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
                        "Turn on USB debugging or Wireless debugging."));

        findViewById(R.id.btn_copy_stay).setOnClickListener(v -> copy(
                "Zero Reel stay signed in",
                StaySignedIn.bothCommands(this),
                "Commands copied. Run both. You do not need to remove Google accounts."));

        findViewById(R.id.btn_apply_now).setOnClickListener(v -> {
            AccessibilityKeeper.restoreIfAllowed(this);
            boolean deviceOwner = ProtectLock.apply(this);
            if (deviceOwner) {
                Toast.makeText(this, "Device Owner lock is on.", Toast.LENGTH_LONG).show();
            } else if (StaySignedIn.ready(this)) {
                Toast.makeText(this, "Stay-signed-in lock is on. Accessibility will turn back on. Uninstall is still possible.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Run the two stay-signed-in commands first. Do not remove your Google account for this path.", Toast.LENGTH_LONG).show();
            }
            refresh();
        });

        findViewById(R.id.btn_open_accounts).setOnClickListener(v ->
                startSafe(new Intent(Settings.ACTION_SYNC_SETTINGS),
                        "Only do this if you accept signing out on this phone."));

        findViewById(R.id.btn_copy_command).setOnClickListener(v -> copy(
                "Zero Reel device owner",
                ProtectLock.adbCommand(this),
                "Copied. This command fails while a Google account is still on the phone."));

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
        if (DeviceStatus.strictUninstallLock(this) && AccessibilityKeeper.canRestore(this)) {
            status.setText("Full lock on. Uninstall, battery, and Accessibility are protected.");
            status.setTextColor(0xFF2E7D32);
        } else if (StaySignedIn.ready(this) && DeviceStatus.strictUninstallLock(this)) {
            status.setText("Device Owner is on. Run the stay-signed-in grant if Accessibility can still be turned off.");
            status.setTextColor(0xFF2E7D32);
        } else if (StaySignedIn.ready(this)) {
            status.setText("Stay-signed-in lock is on. You are still logged into Google. Uninstall is still possible from Settings.");
            status.setTextColor(0xFF2E7D32);
        } else if (ProtectLock.isDeviceOwner(this)) {
            status.setText("Device Owner is set. Tap check lock to apply uninstall blocking.");
            status.setTextColor(0xFFFFA000);
        } else {
            status.setText("Use the stay-signed-in commands. You do not need to log out of Google for that.");
            status.setTextColor(0xFFC62828);
        }
    }

    private void copy(String label, String text, String toast) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
    }

    private void startSafe(Intent intent, String fallback) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, fallback, Toast.LENGTH_LONG).show();
        }
    }
}
