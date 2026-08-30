package com.zeroreel.app;

import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
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
        ProtectLock.apply(this);

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

        findViewById(R.id.btn_copy_adb).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Zero Reel device owner", ProtectLock.requiredCommands(this)));
            Toast.makeText(this, "Copied the Device Owner commands.", Toast.LENGTH_LONG).show();
        });
        findViewById(R.id.btn_apply_strict).setOnClickListener(v -> {
            if (ProtectLock.apply(this) && ProtectLock.ready(this)) {
                Toast.makeText(this, "Device Owner policies are on.", Toast.LENGTH_LONG).show();
            } else {
                startActivity(new Intent(this, StrictLockActivity.class));
            }
            refresh();
        });
        findViewById(R.id.btn_strict_help).setOnClickListener(v ->
                startActivity(new Intent(this, StrictLockActivity.class)));

        findViewById(R.id.btn_disarm).setOnClickListener(v ->
                UnlockHelper.confirm(this, "Disarm Zero Reel",
                        "This turns off blocking and allows uninstall. Enter your authenticator code.",
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
        ProtectLock.apply(this);
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
        } else if (armed && ProtectLock.ready(this)) {
            status.setText("ARMED · MAX LOCK");
            status.setTextColor(0xFF2E7D32);
            detail.setText("Blocking is on. Uninstall, force-stop, battery, Safe Mode, extra users, and USB debugging are locked. Authenticator required to disarm.");
        } else if (armed) {
            status.setText("ARMED");
            status.setTextColor(0xFF2E7D32);
            detail.setText("Blocking is on. Device Owner is optional and adds the strongest uninstall and battery lock.");
        } else {
            status.setText("DISARMED");
            status.setTextColor(0xFFFF5722);
            detail.setText("Protection is off. Arm it again from setup if you still want blocking.");
        }

        ((TextView) findViewById(R.id.text_blocks_today)).setText(String.valueOf(UsageStore.blocksToday(this)));
        refreshUrgeCounts();
        long allowedMin = UsageStore.allowedMsToday(this) / 60000L;
        int limit = Prefs.dailyLimitMinutes(this);
        ((TextView) findViewById(R.id.text_allowed_today)).setText(
                limit <= 0 ? "Always block" : allowedMin + " / " + limit + " min"
        );

        setChip(R.id.text_access_chip, access,
                AccessibilityKeeper.canRestore(this) ? "Accessibility locked" : "Accessibility");
        setChip(R.id.text_admin_chip, ProtectLock.ready(this) || admin,
                ProtectLock.ready(this) ? "Max lock" : "Uninstall lock");
        setChip(R.id.text_battery_chip, ProtectLock.isDeviceOwner(this) || DeviceStatus.batteryUnrestricted(this),
                ProtectLock.isDeviceOwner(this) ? "Battery locked" : "Battery");

        TextView strict = findViewById(R.id.text_strict_status);
        if (ProtectLock.ready(this)) {
            strict.setText("Max lock is on. Uninstall, Accessibility, Safe Mode, extra users, and battery limits stay locked. Disarm with an authenticator code.");
            strict.setTextColor(0xFF2E7D32);
        } else if (ProtectLock.isDeviceOwner(this)) {
            strict.setText("Device Owner is set. Run the WRITE_SECURE_SETTINGS grant so Accessibility stays locked on.");
            strict.setTextColor(0xFFFFA000);
        } else if (admin) {
            strict.setText("Blocking already works. Device Owner is optional and is the strongest self-control lock: Uninstall disabled, Accessibility locked, Safe Mode and extra users blocked.");
            strict.setTextColor(0xFF1565C0);
        } else {
            strict.setText("Blocking already works. Turn on uninstall protection, then optionally add Device Owner for the strongest lock.");
            strict.setTextColor(0xFF1565C0);
        }

        ((SwitchMaterial) findViewById(R.id.switch_youtube)).setChecked(Prefs.platformEnabled(this, Prefs.APP_YOUTUBE, true));
        ((SwitchMaterial) findViewById(R.id.switch_instagram)).setChecked(Prefs.platformEnabled(this, Prefs.APP_INSTAGRAM, true));
        ((SwitchMaterial) findViewById(R.id.switch_tiktok)).setChecked(Prefs.platformEnabled(this, Prefs.APP_TIKTOK, true));
        ((SwitchMaterial) findViewById(R.id.switch_facebook)).setChecked(Prefs.platformEnabled(this, Prefs.APP_FACEBOOK, true));
        ((SwitchMaterial) findViewById(R.id.switch_snapchat)).setChecked(Prefs.platformEnabled(this, Prefs.APP_SNAPCHAT, true));
        ((SwitchMaterial) findViewById(R.id.switch_debug)).setChecked(Prefs.debugLog(this));
    }

    private void refreshUrgeCounts() {
        setUrgeRow(R.id.text_urge_youtube, BlockRules.Platform.YOUTUBE);
        setUrgeRow(R.id.text_urge_instagram, BlockRules.Platform.INSTAGRAM);
        setUrgeRow(R.id.text_urge_tiktok, BlockRules.Platform.TIKTOK);
        setUrgeRow(R.id.text_urge_facebook, BlockRules.Platform.FACEBOOK);
        setUrgeRow(R.id.text_urge_snapchat, BlockRules.Platform.SNAPCHAT);
        ((TextView) findViewById(R.id.text_urge_total)).setText(
                UsageStore.urgesTotalAll(this) + "  ·  " + UsageStore.blocksToday(this) + " today");
    }

    private void setUrgeRow(int id, BlockRules.Platform platform) {
        ((TextView) findViewById(id)).setText(UsageStore.formatAppCount(this, platform));
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
        ProtectLock.release(this);
        SharedPreferences.Editor editor = Prefs.get(this).edit();
        editor.putBoolean(Prefs.LOCK_ARMED, false);
        editor.putBoolean(Prefs.MASTER_ENABLED, false);
        editor.putLong(Prefs.PAUSE_UNTIL_MS, 0L);
        editor.apply();
        BlockGuardService.stop(this);
        refresh();
        Toast.makeText(this, "Disarmed. Uninstall is allowed again.", Toast.LENGTH_LONG).show();
    }
}
