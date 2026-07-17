package com.example.appeyesense;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ContactManager {
    private static final String TAG = "ContactManager";
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public ContactManager(Context context) {
        UserProfile profile = new UserProfile(context);
        String userId = profile.getUserId();
        this.sharedPreferences = context.getSharedPreferences(
                "contacts_" + userId,
                Context.MODE_PRIVATE
        );
        this.gson = new Gson();
        Log.d(TAG, "📁 Fichier contacts pour: " + userId);
    }

    public List<EmergencyContact> getContacts() {
        String json = sharedPreferences.getString("contacts", "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<EmergencyContact>>() {}.getType();
            List<EmergencyContact> contacts = gson.fromJson(json, type);
            return contacts != null ? contacts : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur lecture", e);
            return new ArrayList<>();
        }
    }

    public boolean addContact(EmergencyContact contact) {
        if (contact == null || contact.getEmail() == null || contact.getEmail().isEmpty()) {
            return false;
        }

        List<EmergencyContact> contacts = getContacts();
        for (EmergencyContact c : contacts) {
            if (c.getEmail().equalsIgnoreCase(contact.getEmail())) {
                return false;
            }
        }

        contacts.add(contact);
        saveContacts(contacts);
        Log.d(TAG, " Contact ajouté: " + contact.getName());
        return true;
    }

    public boolean removeContact(String email) {
        List<EmergencyContact> contacts = getContacts();
        boolean removed = contacts.removeIf(c -> c.getEmail().equalsIgnoreCase(email));
        if (removed) {
            saveContacts(contacts);
            Log.d(TAG, "Contact supprimé: " + email);
        }
        return removed;
    }

    public boolean updateContact(String oldEmail, EmergencyContact newContact) {
        List<EmergencyContact> contacts = getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getEmail().equalsIgnoreCase(oldEmail)) {
                contacts.set(i, newContact);
                saveContacts(contacts);
                Log.d(TAG, " Contact modifié: " + oldEmail + " → " + newContact.getEmail());
                return true;
            }
        }
        return false;
    }

    public int getContactCount() {
        return getContacts().size();
    }

    private void saveContacts(List<EmergencyContact> contacts) {
        String json = gson.toJson(contacts);
        sharedPreferences.edit().putString("contacts", json).apply();
    }
}