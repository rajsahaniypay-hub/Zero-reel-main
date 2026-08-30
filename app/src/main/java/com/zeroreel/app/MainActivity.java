package com.zeroreel.app;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Prefs.setupComplete(this)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        if (Prefs.masterEnabled(this)) {
            BlockGuardService.start(this);
        }

        findViewById(R.id.btn_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.btn_admin).setOnClickListener(v -> {
            if (DeviceStatus.adminActive(this)) {
                Toast.makeText(this, "Uninstall protection is already on. Use Disarm to turn it off.", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DeviceStatus.adminComponent(this));
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explanation));
            startActivity(intent);
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

        bindSwitch(R.id.switch_youtube, Prefs.APP_YOUTUBE, true);
        bindSwitch(R.id.switch_instagram, Prefs.APP_INSTAGRAM, true);
        bindSwitch(R.id.switch_tiktok, Prefs.APP_TIKTOK, true);
        bindSwitch(R.id.switch_facebook, Prefs.APP_FACEBOOK, true);
        bindSwitch(R.id.switch_snapchat, Prefs.APP_SNAPCHAT, true);
        bindSwitch(R.id.switch_debug, Prefs.DEBUG_LOG, false);

        MaterialButtonToggleGroup limits = findViewById(R.id.group_daily_limit);
        selectLimit(limits, Prefs.dailyLimitMinutes(this));
        limits.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            int minutes = minutesFor(checkedId);
            if (minutes == Prefs.dailyLimitMinutes(this)) return;
            if (Prefs.lockArmed(this)) {
                selectLimit(group, Prefs.dailyLimitMinutes(this));
                UnlockHelper.confirm(this, "Change daily limit",
                        "Enter your authenticator code to change how long Reels are allowed.",
                        () -> {
                            Prefs.get(this).edit().putInt(Prefs.DAILY_LIMIT_MINUTES, minutes).apply();
                            selectLimit(group, minutes);
                        });
                return;
            }
            Prefs.get(this).edit().putInt(Prefs.DAILY_LIMIT_MINUTES, minutes).apply();
        });

        findViewById(R.id.btn_pause).setOnClickListener(v ->
                UnlockHelper.confirm(this, "Pause for 5 minutes",
                        "Enter the current authenticator code to pause blocking.",
                        () -> {
                            Prefs.get(this).edit()
                                    .putLong(Prefs.PAUSE_UNTIL_MS, System.currentTimeMillis() + 5 * 60_000L)
                                    .apply();
                            BlockGuardService.start(this);
                            refresh();
                            Toast.makeText(this, "Paused for 5 minutes.", Toast.LENGTH_SHORT).show();
                        }));

        findViewById(R.id.btn_disarm).setOnClickListener(v ->
                UnlockHelper.confirm(this, "Disarm Zero Reel",
                        "This turns off blocking and uninstall protection. Enter your authenticator code.",
                        this::disarm));

        findViewById(R.id.btn_show_secret).setOnClickListener(v ->
                UnlockHelper.confirm(this, "Show authenticator secret",
                        "Enter a valid code to reveal the backup secret.",
                        () -> new AlertDialog.Builder(this)
                                .setTitle("Authenticator secret")
                                .setMessage(Prefs.totpSecret(this) + "\n\nKeep this private. Anyone with it can disarm Zero Reel.")
                                .setPositiveButton("OK", null)
                                .show()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void bindSwitch(int id, String key, boolean fallback) {
        SwitchMaterial sw = findViewById(id);
        sw.setChecked(Prefs.platformEnabled(this, key, fallback));
        sw.setOnCheckedChangeListener((button, checked) -> {
            boolean current = Prefs.platformEnabled(this, key, fallback);
            if (checked == current) return;
            if (Prefs.lockArmed(this)) {
                button.setChecked(current);
                UnlockHelper.confirm(this, "Change block list",
                        "Enter your authenticator code to change what Zero Reel blocks.",
                        () -> {
                            Prefs.get(this).edit().putBoolean(key, checked).apply();
                            button.setChecked(checked);
                        });
                return;
            }
            Prefs.get(this).edit().putBoolean(key, checked).apply();
        });
    }

    private void refresh() {
        boolean access = DeviceStatus.accessibilityEnabled(this);
        boolean admin = DeviceStatus.adminActive(this);
        boolean paused = Prefs.isPaused(this);
        boolean armed = Prefs.lockArmed(this) && Prefs.masterEnabled(this);

        TextView status = findViewById(R.id.text_service_status);
        TextView detail = findViewById(R.id.text_status_detail);
        if (!access) {
            status.setText("SERVICE INTERRUPTED");
            status.setTextColor(0xFFFF5722);
            detail.setText("Turn accessibility back on. Zero Reel cannot block Reels without it.");
        } else if (paused) {
            status.setText("PAUSED");
            status.setTextColor(0xFFFFA000);
            detail.setText("Blocking resumes in " + Math.max(1, Prefs.pauseRemainingMs(this) / 60000L) + " min.");
        } else if (armed) {
            status.setText("ARMED");
            status.setTextColor(0xFF2E7D32);
            detail.setText("Blocking stays on after restart. Authenticator code required to stop or uninstall.");
        } else {
            status.setText("DISARMED");
            status.setTextColor(0xFFFF5722);
            detail.setText("Protection is off. Arm it again from setup if you still want blocking.");
        }

        ((TextView) findViewById(R.id.text_blocks_today)).setText(String.valueOf(UsageStore.blocksToday(this)));
        long allowedMin = UsageStore.allowedMsToday(this) / 60000L;
        int limit = Prefs.dailyLimitMinutes(this);
        ((TextView) findViewById(R.id.text_allowed_today)).setText(
                limit <= 0 ? "Always block" : allowedMin + " / " + limit + " min"
        );

        setChip(R.id.text_access_chip, access, "Accessibility");
        setChip(R.id.text_admin_chip, admin, "Uninstall lock");
        setChip(R.id.text_battery_chip, DeviceStatus.batteryUnrestricted(this), "Battery");

        ((SwitchMaterial) findViewById(R.id.switch_youtube)).setChecked(Prefs.platformEnabled(this, Prefs.APP_YOUTUBE, true));
        ((SwitchMaterial) findViewById(R.id.switch_instagram)).setChecked(Prefs.platformEnabled(this, Prefs.APP_INSTAGRAM, true));
        ((SwitchMaterial) findViewById(R.id.switch_tiktok)).setChecked(Prefs.platformEnabled(this, Prefs.APP_TIKTOK, true));
        ((SwitchMaterial) findViewById(R.id.switch_facebook)).setChecked(Prefs.platformEnabled(this, Prefs.APP_FACEBOOK, true));
        ((SwitchMaterial) findViewById(R.id.switch_snapchat)).setChecked(Prefs.platformEnabled(this, Prefs.APP_SNAPCHAT, true));
        ((SwitchMaterial) findViewById(R.id.switch_debug)).setChecked(Prefs.debugLog(this));
    }

    private void setChip(int id, boolean ok, String label) {
        TextView view = findViewById(id);
        view.setText(label + (ok ? " on" : " off"));
        view.setTextColor(ok ? 0xFF2E7D32 : 0xFFC62828);
    }

    private void selectLimit(MaterialButtonToggleGroup group, int minutes) {
        int id = R.id.limit_off;
        if (minutes == 5) id = R.id.limit_5;
        else if (minutes == 15) id = R.id.limit_15;
        else if (minutes == 30) id = R.id.limit_30;
        group.check(id);
    }

    private int minutesFor(int checkedId) {
        if (checkedId == R.id.limit_5) return 5;
        if (checkedId == R.id.limit_15) return 15;
        if (checkedId == R.id.limit_30) return 30;
        return 0;
    }

    private void disarm() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm != null && DeviceStatus.adminActive(this)) {
            dpm.removeActiveAdmin(DeviceStatus.adminComponent(this));
        }
        SharedPreferences.Editor editor = Prefs.get(this).edit();
        editor.putBoolean(Prefs.LOCK_ARMED, false);
        editor.putBoolean(Prefs.MASTER_ENABLED, false);
        editor.putLong(Prefs.PAUSE_UNTIL_MS, 0L);
        editor.apply();
        BlockGuardService.stop(this);
        refresh();
        Toast.makeText(this, "Disarmed. You can now uninstall Zero Reel from Settings.", Toast.LENGTH_LONG).show();
    }
}
