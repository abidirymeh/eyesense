package com.example.appeyesense;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationService extends Service implements LocationListener {

    private static final String TAG = "LocationService";
    private LocationManager locationManager;
    private Location currentLocation;
    private Context context;
    private static final long MIN_TIME = 5000; // 5 secondes
    private static final float MIN_DISTANCE = 10; // 10 mètres

    public LocationService() {
    }

    public LocationService(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void startUpdates(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (locationManager == null) {
            Log.e(TAG, "❌ LocationManager est null");
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ Permissions de localisation non accordées");
            return;
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME,
                        MIN_DISTANCE,
                        this
                );
                Log.d(TAG, "📍 GPS activé");
            } else {
                Log.w(TAG, "⚠️ GPS désactivé");
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME,
                        MIN_DISTANCE,
                        this
                );
                Log.d(TAG, "📍 Réseau activé");
            } else {
                Log.w(TAG, "⚠️ Réseau désactivé");
            }

            Location lastLocation = null;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } else if (locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

            if (lastLocation != null) {
                currentLocation = lastLocation;
                Log.d(TAG, "📍 Dernière position connue: " + getLatitude() + ", " + getLongitude());
            } else {
                Log.w(TAG, "⚠️ Aucune dernière position connue");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "❌ Erreur de sécurité", e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            currentLocation = location;
            Log.d(TAG, "📍 Position mise à jour: " + getLatitude() + ", " + getLongitude());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "📡 Statut changed: " + provider + " status=" + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "📡 Provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "📡 Provider disabled: " + provider);
    }

    public String getLatitude() {
        if (currentLocation != null) {
            return String.valueOf(currentLocation.getLatitude());
        }
        Log.w(TAG, "⚠️ Latitude: position null");
        return "0.0";
    }

    public String getLongitude() {
        if (currentLocation != null) {
            return String.valueOf(currentLocation.getLongitude());
        }
        Log.w(TAG, "⚠️ Longitude: position null");
        return "0.0";
    }

    public String getLocationLink() {
        if (currentLocation != null) {
            return "https://www.google.com/maps?q=" +
                    currentLocation.getLatitude() + "," +
                    currentLocation.getLongitude();
        }
        return "Localisation non disponible";
    }

    public String getLocationText() {
        if (currentLocation != null) {
            return "Lat: " + String.format("%.6f", currentLocation.getLatitude()) +
                    ", Lon: " + String.format("%.6f", currentLocation.getLongitude());
        }
        return "Localisation non disponible";
    }

    public void stopUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
                Log.d(TAG, "📍 Mises à jour arrêtées");
            } catch (SecurityException e) {
                Log.e(TAG, "❌ Erreur arrêt", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUpdates();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}