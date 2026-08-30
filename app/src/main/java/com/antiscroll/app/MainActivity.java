package com.antiscroll.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "AntiScrollPrefs";
    public static final String PREF_MASTER_TOGGLE = "master_toggle";
    public static final String PREF_DARK_MODE = "dark_mode_toggle";
    public static final String PREF_DEBUG_LOG = "debug_log_toggle";
    public static final String PREF_APP_YOUTUBE = "app_youtube";
    public static final String PREF_APP_INSTAGRAM = "app_instagram";

    private SharedPreferences prefs;
    private LinearLayout layoutAppToggles;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String[] ANIME_QUOTES = {
        "\"Power comes in response to a need, not a desire.\" - Goku",
        "\"A lesson without pain is meaningless.\" - Edward Elric",
        "\"If you don't take risks, you can't create a future.\" - Monkey D. Luffy",
        "\"The only ones who should kill are those prepared to be killed.\" - Lelouch",
        "\"Fear is not evil. It tells you what your weakness is.\" - Gildarts Clive",
        "\"Hard work betrays none, but dreams betray many.\" - Hachiman Hikigaya",
        "\"Whatever you do, enjoy it to the fullest.\" - Naruto Uzumaki",
        "\"There's no such thing as a painless lesson.\" - Edward Elric",
        "\"To know sorrow is not terrifying. What is terrifying is to know you can't go back to happiness.\" - Matsumoto Rangiku",
        "\"The world isn't perfect. But it's there for us, doing the best it can.\" - Roy Mustang",
        "\"People's lives don't end when they die. It ends when they lose faith.\" - Itachi Uchiha",
        "\"Even if I die, you keep living okay? Live to see the end of this world.\" - Portgas D. Ace",
        "\"Being alone is more painful than getting hurt.\" - Monkey D. Luffy",
        "\"In this world, wherever there is light, there are also shadows.\" - Madara Uchiha",
        "\"You should enjoy the little detours to the fullest.\" - Ging Freecss",
        "\"The strong don't win. The ones who win are strong.\" - Sora",
        "\"A person grows up when he's able to overcome hardships.\" - Jiraiya",
        "\"If you don't share someone's pain, you can never understand them.\" - Nagato",
        "\"Reject common sense to make the impossible possible.\" - Simon",
        "\"Life is not a game of luck. If you wanna win, work hard.\" - Sora"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(PREF_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutAppToggles = findViewById(R.id.layout_app_toggles);

        Button btnSettings = findViewById(R.id.btn_accessibility_settings);
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        SwitchMaterial masterSwitch = findViewById(R.id.switch_master_toggle);
        masterSwitch.setChecked(prefs.getBoolean(PREF_MASTER_TOGGLE, false));
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isAccessibilityServiceEnabled(this, AntiScrollAccessibilityService.class)) {
                buttonView.setChecked(false);
                android.widget.Toast.makeText(this, "Please enable Accessibility Permission first.", android.widget.Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
            prefs.edit().putBoolean(PREF_MASTER_TOGGLE, isChecked).apply();
            layoutAppToggles.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        layoutAppToggles.setVisibility(prefs.getBoolean(PREF_MASTER_TOGGLE, false) ? View.VISIBLE : View.GONE);

        setupSwitch(R.id.switch_theme_toggle, PREF_DARK_MODE, true, isChecked -> {
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            overridePendingTransition(R.anim.theme_fade_in, R.anim.theme_fade_out);
        });

        setupSwitch(R.id.switch_debug_log, PREF_DEBUG_LOG, false, null);
        setupSwitch(R.id.switch_app_youtube, PREF_APP_YOUTUBE, false, null);
        setupSwitch(R.id.switch_app_instagram, PREF_APP_INSTAGRAM, false, null);

        TextView tvQuote = findViewById(R.id.text_anime_quote);
        if (tvQuote != null) {
            tvQuote.setText(ANIME_QUOTES[(int)(Math.random() * ANIME_QUOTES.length)]);
            fetchAnimeQuote(tvQuote);
        }

        View githubLink = findViewById(R.id.layout_github_link);
        if (githubLink != null) {
            githubLink.setOnClickListener(v -> showDeveloperInfoDialog());
        }
    }

    private void fetchAnimeQuote(TextView tvQuote) {
        executor.execute(() -> {
            try {
                URL url = new URL("https://animechan.io/api/v1/quotes/random");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject data = new JSONObject(sb.toString()).optJSONObject("data");
                    if (data != null) {
                        String quote = data.optString("content", "");
                        JSONObject character = data.optJSONObject("character");
                        String charName = character != null ? character.optString("name", "Unknown") : "Unknown";
                        JSONObject anime = data.optJSONObject("anime");
                        String animeName = anime != null ? anime.optString("name", "") : "";

                        if (!quote.isEmpty()) {
                            String formatted = "\"" + quote + "\"";
                            if (!charName.equals("Unknown")) formatted += " - " + charName;
                            if (!animeName.isEmpty()) formatted += " (" + animeName + ")";

                            String finalQuote = formatted;
                            mainHandler.post(() -> tvQuote.setText(finalQuote));
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {
            }
        });
    }

    private void showDeveloperInfoDialog() {
        new AlertDialog.Builder(this)
            .setTitle("About AntiScroll")
            .setMessage("AntiScroll is an open-source application designed to help users reduce screen time by blocking short-form video feeds across popular platforms.\n\nVersion: 1.0\nDeveloper: Nikhil Yadav\nGitHub: @yadavnikhil03\n\nBuilt with Android Accessibility APIs.\nNo data is collected or transmitted.")
            .setPositiveButton("View on GitHub", (dialog, which) -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yadavnikhil03")));
            })
            .setNegativeButton("Dismiss", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();

        SwitchMaterial masterSwitch = findViewById(R.id.switch_master_toggle);
        if (masterSwitch.isChecked() && !isAccessibilityServiceEnabled(this, AntiScrollAccessibilityService.class)) {
            masterSwitch.setChecked(false);
            prefs.edit().putBoolean(PREF_MASTER_TOGGLE, false).apply();
            if (layoutAppToggles != null) layoutAppToggles.setVisibility(View.GONE);
        }
    }

    private void updateServiceStatus() {
        TextView statusText = findViewById(R.id.text_service_status);
        View statusDot = findViewById(R.id.view_status_dot);
        View statusContainer = findViewById(R.id.layout_service_status);
        Button btnSettings = findViewById(R.id.btn_accessibility_settings);
        if (statusText == null) return;

        boolean active = isAccessibilityServiceEnabled(this, AntiScrollAccessibilityService.class);
        if (active) {
            statusText.setText("SERVICE : ACTIVE");
            statusText.setTextColor(0xFF4CAF50);
            if (statusDot != null) statusDot.setBackgroundResource(R.drawable.status_dot_active);
            if (statusContainer != null) statusContainer.setBackgroundResource(R.drawable.status_bg_active);
            if (btnSettings != null) btnSettings.setText("Accessibility Settings");
        } else {
            statusText.setText("SERVICE : INACTIVE");
            statusText.setTextColor(0xFFFF5722);
            if (statusDot != null) statusDot.setBackgroundResource(R.drawable.status_dot_inactive);
            if (statusContainer != null) statusContainer.setBackgroundResource(R.drawable.status_bg_inactive);
            if (btnSettings != null) btnSettings.setText("Enable Accessibility Service");
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (prefString != null) {
            return prefString.contains(context.getPackageName() + "/" + service.getName());
        }
        return false;
    }

    private interface OnSwitchAction { void onToggle(boolean isChecked); }

    private void setupSwitch(int switchId, String prefKey, boolean defaultValue, OnSwitchAction action) {
        SwitchMaterial sw = findViewById(switchId);
        sw.setChecked(prefs.getBoolean(prefKey, defaultValue));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(prefKey, isChecked).apply();
            if (action != null) action.onToggle(isChecked);
        });
    }
}
