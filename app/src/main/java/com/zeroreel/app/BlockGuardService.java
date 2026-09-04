package com.zeroreel.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

public class BlockGuardService extends Service {
    public static final String CHANNEL_ID = "zero_reel_guard";
    public static final String ALERT_CHANNEL_ID = "zero_reel_alert";
    private static final int NOTIFICATION_ID = 7101;
    private static final long TICK_MS = 3000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!Prefs.setupComplete(BlockGuardService.this) || !Prefs.masterEnabled(BlockGuardService.this)) {
                return;
            }
            ProtectLock.apply(BlockGuardService.this);
            AccessibilityKeeper.restoreIfAllowed(BlockGuardService.this);
            publishNotification();
            handler.postDelayed(this, TICK_MS);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, BlockGuardService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, BlockGuardService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        ProtectLock.apply(this);
        AccessibilityKeeper.restoreIfAllowed(this);
        handler.post(tick);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        publishNotification();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (Prefs.setupComplete(this) && Prefs.masterEnabled(this)) {
            start(getApplicationContext());
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    private void publishNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel guard = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.guard_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        guard.setDescription(getString(R.string.guard_channel_desc));
        guard.setShowBadge(false);
        manager.createNotificationChannel(guard);

        NotificationChannel alert = new NotificationChannel(
                ALERT_CHANNEL_ID,
                "Protection alert",
                NotificationManager.IMPORTANCE_HIGH
        );
        alert.setDescription("Shown when Accessibility was turned off");
        manager.createNotificationChannel(alert);
    }

    private Notification buildNotification() {
        boolean accessOn = DeviceStatus.accessibilityEnabled(this);
        Intent launch = accessOn
                ? new Intent(this, MainActivity.class)
                : new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        PendingIntent pending = PendingIntent.getActivity(
                this, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String text;
        String channel = CHANNEL_ID;
        if (Prefs.isPaused(this)) {
            text = getString(R.string.guard_paused);
        } else if (!accessOn && AccessibilityKeeper.canRestore(this)) {
            text = "Accessibility was turned off. Zero Reel is turning it back on.";
            channel = ALERT_CHANNEL_ID;
        } else if (!accessOn) {
            text = "Accessibility is off. Blocking is stopped. Tap to turn it back on.";
            channel = ALERT_CHANNEL_ID;
        } else if (ProtectLock.ready(this)) {
            text = "Max lock on. Uninstall, battery, Safe Mode, and extra users are locked.";
        } else {
            text = getString(R.string.guard_active);
        }

        return new NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.drawable.ic_splash_logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pending)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
