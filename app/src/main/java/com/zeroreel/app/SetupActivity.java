package com.zeroreel.app;

import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;

public class SetupActivity extends AppCompatActivity {
    private static final int REQ_ADMIN = 41;
    private static final int REQ_NOTIFY = 42;

    private String secret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        secret = Prefs.totpSecret(this);
        if (secret == null || secret.isEmpty()) {
            secret = Totp.generateSecret();
            Prefs.get(this).edit().putString(Prefs.TOTP_SECRET, secret).apply();
        }

        TextView secretView = findViewById(R.id.text_totp_secret);
        secretView.setText(secret);

        findViewById(R.id.btn_copy_secret).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Zero Reel secret", secret));
            Toast.makeText(this, "Secret copied. Add it in Google Authenticator or Authy.", Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.btn_open_authenticator).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Totp.otpAuthUri(secret)));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No authenticator app found. Add the secret manually.", Toast.LENGTH_LONG).show();
            }
        });

        EditText codeInput = findViewById(R.id.input_totp_confirm);
        Button confirm = findViewById(R.id.btn_confirm_code);
        confirm.setOnClickListener(v -> {
            String code = codeInput.getText() != null ? codeInput.getText().toString() : "";
            if (!Totp.verify(secret, code)) {
                Toast.makeText(this, "That code is wrong. Check the authenticator app.", Toast.LENGTH_LONG).show();
                return;
            }
            Prefs.clearTotpFailures(this);
            Prefs.get(this).edit().putBoolean(Prefs.TOTP_LINKED, true).apply();
            Toast.makeText(this, "Authenticator linked.", Toast.LENGTH_SHORT).show();
            requestNotificationPermission();
        });

        findViewById(R.id.btn_enable_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.btn_enable_admin).setOnClickListener(v -> {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DeviceStatus.adminComponent(this));
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explanation));
            startActivityForResult(intent, REQ_ADMIN);
        });

        findViewById(R.id.btn_battery).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                }
            }
        });

        findViewById(R.id.btn_finish_setup).setOnClickListener(v -> finishSetup());
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        setStatus(R.id.text_access_status, DeviceStatus.accessibilityEnabled(this), "Accessibility on", "Accessibility off");
        if (DeviceStatus.strictUninstallLock(this)) {
            setStatus(R.id.text_admin_status, true, "Strict uninstall lock on", "Uninstall protection off");
        } else {
            setStatus(R.id.text_admin_status, DeviceStatus.adminActive(this),
                    "Weak lock on (Settings can still uninstall)", "Uninstall protection off");
        }
        setStatus(R.id.text_battery_status, DeviceStatus.batteryUnrestricted(this), "Battery unrestricted", "Battery may kill the service");
    }

    private void setStatus(int id, boolean ok, String good, String bad) {
        TextView view = findViewById(id);
        view.setText(ok ? good : bad);
        view.setTextColor(ok ? 0xFF2E7D32 : 0xFFC62828);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
        }
    }

    private void finishSetup() {
        if (!Prefs.get(this).getBoolean(Prefs.TOTP_LINKED, false)) {
            Toast.makeText(this, "Link the authenticator first with a valid code.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!DeviceStatus.accessibilityEnabled(this)) {
            Toast.makeText(this, "Turn on the Zero Reel accessibility service first.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        if (!DeviceStatus.adminActive(this)) {
            Toast.makeText(this, "Enable uninstall protection first.", Toast.LENGTH_LONG).show();
            return;
        }

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
}
