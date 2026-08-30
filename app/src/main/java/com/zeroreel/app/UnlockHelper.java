package com.zeroreel.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

final class UnlockHelper {

    interface OnUnlocked {
        void run();
    }

    private UnlockHelper() {}

    static void confirm(Activity activity, String title, String message, OnUnlocked onUnlocked) {
        if (Prefs.totpLockedOut(activity)) {
            long seconds = Math.max(1, Prefs.totpLockRemainingMs(activity) / 1000L);
            Toast.makeText(activity, "Too many attempts. Try again in " + seconds + "s.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 20);
        layout.setPadding(pad, dp(activity, 8), pad, 0);

        TextView hint = new TextView(activity);
        hint.setText(message);
        hint.setTextSize(14);
        layout.addView(hint);

        EditText input = new EditText(activity);
        input.setHint("6-digit authenticator code");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        layout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (Prefs.totpLockedOut(activity)) {
                Toast.makeText(activity, "Too many attempts. Wait and try again.", Toast.LENGTH_LONG).show();
                return;
            }
            String code = input.getText() != null ? input.getText().toString() : "";
            if (Totp.verify(Prefs.totpSecret(activity), code)) {
                Prefs.clearTotpFailures(activity);
                dialog.dismiss();
                onUnlocked.run();
            } else {
                boolean locked = Prefs.recordTotpFailure(activity);
                Toast.makeText(activity,
                        locked ? "Too many wrong codes. Locked for 30 seconds." : "Wrong authenticator code.",
                        Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
        input.requestFocus();
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
