package com.example.appeyesense;
import android.util.Log;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private SwitchCompat swNightMode, swAutoEmail;
    private Button btnReset;
    private TextView btnBack;

    private android.content.SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("EyeSenseSettings", MODE_PRIVATE);

        btnBack = findViewById(R.id.btnBack);
        swNightMode = findViewById(R.id.swNightMode);
        swAutoEmail = findViewById(R.id.swAutoEmail);
        btnReset = findViewById(R.id.btnReset);

        loadSettings();


        btnBack.setOnClickListener(v -> finish());

        swNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setNightMode(isChecked);
            prefs.edit().putBoolean("night_mode", isChecked).apply();
        });

        swAutoEmail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_email", isChecked).apply();
            Toast.makeText(this,
                    isChecked ? " Auto Email activé" : "❌ Auto Email désactivé",
                    Toast.LENGTH_SHORT).show();
        });

        btnReset.setOnClickListener(v -> resetSettings());
    }

    private void loadSettings() {
        swNightMode.setChecked(prefs.getBoolean("night_mode", false));
        swAutoEmail.setChecked(prefs.getBoolean("auto_email", true));

        setNightMode(swNightMode.isChecked());
    }

    private void setNightMode(boolean enabled) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (enabled) {
            layoutParams.screenBrightness = 0.8f;
            getWindow().setAttributes(layoutParams);
            Toast.makeText(this, "🌙 Night Mode ON - Brightness increased", Toast.LENGTH_SHORT).show();
        } else {
            layoutParams.screenBrightness = -1f;
            getWindow().setAttributes(layoutParams);
            Toast.makeText(this, "🌙 Night Mode OFF", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "Night Mode: " + (enabled ? "ON" : "OFF"));
    }

    private void resetSettings() {
        prefs.edit()
                .putBoolean("night_mode", false)
                .putBoolean("auto_email", true)
                .apply();

        loadSettings();
        Toast.makeText(this, " Settings reset to default", Toast.LENGTH_SHORT).show();
    }

    public static boolean isAutoEmailEnabled(android.content.Context context) {
        return context.getSharedPreferences("EyeSenseSettings", MODE_PRIVATE)
                .getBoolean("auto_email", true);
    }
}