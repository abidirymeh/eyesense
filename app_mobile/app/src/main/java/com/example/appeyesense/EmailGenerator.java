package com.example.appeyesense;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class EmailGenerator {

    private static final String TAG = "EmailGenerator";
    private final Context context;

    public EmailGenerator(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, " EmailGenerator initialisé");
    }


    public String getAppEmail() {
        return "rimeh.abidi@enis.tn";
    }


    public String getDeviceInfo() {
        return "Modèle: " + Build.MODEL + "\n" +
                "Marque: " + Build.BRAND + "\n" +
                "Android: " + Build.VERSION.RELEASE;
    }


    private String getUniqueDeviceId() {
        try {
            if (context != null) {
                String androidId = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );
                return androidId != null ? androidId : "ID_NON_DISPONIBLE";
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur récupération ID", e);
        }
        return "ID_NON_DISPONIBLE";
    }
}