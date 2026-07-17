package com.example.appeyesense;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class UserProfile {
    private static final String PREF_NAME = "userProfile";
    private final SharedPreferences prefs;

    public UserProfile(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getUserId() {
        String userId = prefs.getString("userId", null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            prefs.edit().putString("userId", userId).apply();
        }
        return userId;
    }

    public String getUserName() {
        return prefs.getString("userName", "Utilisateur");
    }

    public void setUserName(String name) {
        prefs.edit().putString("userName", name).apply();
    }
}