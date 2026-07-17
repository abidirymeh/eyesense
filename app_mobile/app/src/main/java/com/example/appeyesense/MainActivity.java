package com.example.appeyesense;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EyeSense";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int INPUT_SIZE = 640;
    private static final float CONF_THRESHOLD = 0.05f;
    private static final String MODEL_FILE = "yolo26n_float32.tflite";
    private static final long FRAME_INTERVAL_MS = 500;

    private static final float BRIGHTNESS_HIGH = 0.8f;
    private static final float BRIGHTNESS_AUTO = -1f;

    private PreviewView cameraPreview;
    private TextView statusText, confidenceText, frameText, statusLabel;
    private Button detectButton;
    private ImageButton btnContacts;
    private ImageButton btnSettings;
    private ImageView statusAnimation;
    private android.view.View statusIndicator;

    private Interpreter tflite;
    private boolean isRunning = false;
    private int frameCount = 0;
    private ProcessCameraProvider cameraProvider;
    private long lastFrameTime = 0;

    private EmailService emailService;
    private EmailGenerator emailGenerator;

    private ToneGenerator toneGenerator;
    private Vibrator vibrator;
    private boolean alarmActive = false;
    private final Handler alarmHandler = new Handler(Looper.getMainLooper());
    private Runnable alarmRunnable;

    private static final int ALERT_THRESHOLD = 3;
    private static final long ALERT_RESET_TIME = 30_000;

    private LocationService locationService;
    private final List<EmergencyContact> emergencyContacts = new ArrayList<>();

    private int alertCount = 0;
    private final Handler emergencyHandler = new Handler(Looper.getMainLooper());
    private Runnable alertResetRunnable;

    private static final int NOTIFICATION_PERMISSION_REQUEST = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);
        statusText = findViewById(R.id.statusText);
        confidenceText = findViewById(R.id.confidenceText);
        frameText = findViewById(R.id.frameText);
        statusLabel = findViewById(R.id.statusLabel);
        detectButton = findViewById(R.id.detectButton);
        btnContacts = findViewById(R.id.btnContacts);
        btnSettings = findViewById(R.id.btnSettings);
        statusAnimation = findViewById(R.id.statusAnimation);
        statusIndicator = findViewById(R.id.statusIndicator);

        setAnimation("en_attente.gif");
        updateStatusLabel("Ready", "#00E676");

        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tflite = new Interpreter(loadModelFile(), options);
            statusText.setText(" Model loaded — ready");
            Log.d(TAG, " Modèle TFLite chargé : " + MODEL_FILE);
        } catch (IOException e) {
            statusText.setText("❌ Error loading model");
            updateStatusLabel("Error", "#FF1744");
            Log.e(TAG, "❌ Erreur chargement modèle", e);
        }
        if (checkPermissions()) startCamera();


        checkNotificationPermission();

        emailGenerator = new EmailGenerator(this);
        emailService = new EmailService(this);

        Log.d(TAG, "📧 Email app généré: " + emailGenerator.getAppEmail());
        Log.d(TAG, "📱 " + emailGenerator.getDeviceInfo());

        if (checkPermissions()) startCamera();

        setupEmergencyContacts();

        detectButton.setOnClickListener(v -> toggleDetection());

        detectButton.setOnLongClickListener(v -> {
            testSendEmailWithLocation();
            return true;
        });

        btnContacts.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);


        startLocationService();
    }


    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
                return false;
            }
        }
        return true;
    }



    private void triggerAlarm() {
        if (!alarmActive) {
            alarmActive = true;


            alarmRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!alarmActive) return;

                    try {
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);
                        Log.d(TAG, "🔊 Son activé");
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Erreur son", e);
                    }

                    alarmHandler.postDelayed(this, 2000);
                }
            };
            alarmHandler.post(alarmRunnable);
        }

        alertCount++;
        Log.d(TAG, "⚠️ Drowsiness detection #" + alertCount + "/" + ALERT_THRESHOLD);

        emergencyHandler.removeCallbacks(alertResetRunnable);
        alertResetRunnable = () -> {
            Log.d(TAG, "🔄 Alert counter reset (inactivity)");
            alertCount = 0;
        };
        emergencyHandler.postDelayed(alertResetRunnable, ALERT_RESET_TIME);

        if (alertCount >= ALERT_THRESHOLD) {
            sendEmergencyAlert();
            alertCount = 0;
            emergencyHandler.removeCallbacks(alertResetRunnable);
        }
    }



    private void stopAlarm() {
        alarmActive = false;
        alertCount = 0;
        alarmHandler.removeCallbacks(alarmRunnable);
        emergencyHandler.removeCallbacks(alertResetRunnable);

    }

    private void applyDetectionBrightness(boolean enabled) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (enabled) {
            layoutParams.screenBrightness = BRIGHTNESS_HIGH;
            Log.d(TAG, "💡 Detection brightness ON (80%)");
        } else {
            layoutParams.screenBrightness = BRIGHTNESS_AUTO;
            Log.d(TAG, "💡 Detection brightness OFF (auto)");
        }
        getWindow().setAttributes(layoutParams);
    }


    private void applyNightMode() {
        android.content.SharedPreferences prefs = getSharedPreferences("EyeSenseSettings", MODE_PRIVATE);
        boolean nightMode = prefs.getBoolean("night_mode", false);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (nightMode) {
            layoutParams.screenBrightness = 0.8f;
            getWindow().setAttributes(layoutParams);
            Log.d(TAG, "🌙 Night Mode ON");
        } else {
            if (!isRunning) {
                layoutParams.screenBrightness = BRIGHTNESS_AUTO;
                getWindow().setAttributes(layoutParams);
            }
            Log.d(TAG, "🌙 Night Mode OFF");
        }
    }


    private void toggleDetection() {
        isRunning = !isRunning;
        frameCount = 0;

        if (isRunning) {
            detectButton.setText("⏹ Stop");
            statusText.setText("🔍 Detection running...");
            updateStatusLabel("Analyzing", "#FFD600");
            setAnimation("eye_detection.gif");
            applyDetectionBrightness(true);
            Log.d(TAG, "▶ Detection started");
        } else {
            detectButton.setText("▶ Start Detection");
            statusText.setText("▶ Press Start to begin");
            updateStatusLabel("Ready", "#00E676");
            setAnimation("en_attente.gif");
            stopAlarm();
            applyDetectionBrightness(false);
            Log.d(TAG, "⏹ Detection stopped");
        }
    }




    private void setupEmergencyContacts() {
        ContactManager contactManager = new ContactManager(this);
        emergencyContacts.clear();
        emergencyContacts.addAll(contactManager.getContacts());

        if (emergencyContacts.isEmpty()) {
            Log.w(TAG, "⚠️ Aucun contact d'urgence configuré");
            emergencyContacts.add(new EmergencyContact("Default", "abidirymeh01@gmail.com"));
        }

        Log.d(TAG,  emergencyContacts.size() + " contacts chargés");
        for (EmergencyContact c : emergencyContacts) {
            Log.d(TAG, "   📧 " + c.getName() + " (" + c.getEmail() + ")");
        }
    }

    private void startLocationService() {
        locationService = new LocationService(this);
        locationService.startUpdates(this);
    }

    private void testSendEmailWithLocation() {
        if (emergencyContacts.isEmpty()) {
            runOnUiThread(() -> {
                Toast.makeText(this, "❌ No contacts configured", Toast.LENGTH_LONG).show();
            });
            return;
        }

        Log.d(TAG, "🧪 TEST EMAIL with location (long press)");
        Toast.makeText(this, "🧪 Sending test email to " + emergencyContacts.size() + " contact(s)...", Toast.LENGTH_SHORT).show();

        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String location = locationService != null ? locationService.getLocationText() : "Location not available";
        String latitude = locationService != null ? locationService.getLatitude() : "0.0";
        String longitude = locationService != null ? locationService.getLongitude() : "0.0";

        for (EmergencyContact contact : emergencyContacts) {
            emailService.sendTestEmail(
                    contact.getEmail(),
                    location,
                    timestamp,
                    latitude,
                    longitude,
                    new EmailService.Callback() {
                        @Override
                        public void onEmailSent(boolean success, String message) {
                            String msg = success ? " Test email sent to " + contact.getName() : "❌ Failed to send to " + contact.getName();
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
                            Log.d(TAG, msg);
                        }
                    }
            );
        }
    }

    private void sendEmergencyAlert() {
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "❌ No emergency contacts", Toast.LENGTH_LONG).show();
            return;
        }

        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String location = locationService != null ? locationService.getLocationText() : "Location not available";
        String latitude = locationService != null ? locationService.getLatitude() : "0.0";
        String longitude = locationService != null ? locationService.getLongitude() : "0.0";

        for (EmergencyContact contact : emergencyContacts) {
            emailService.sendDrowsinessAlert(
                    contact.getEmail(),
                    location,
                    timestamp,
                    latitude,
                    longitude,
                    new EmailService.Callback() {
                        @Override
                        public void onEmailSent(boolean success, String message) {
                            if (success) {
                                Log.d(TAG, " Email sent to " + contact.getName() + " (" + contact.getEmail() + ")");
                            } else {
                                Log.e(TAG, "❌ Failed to send to " + contact.getName() + ": " + message);
                            }
                        }
                    }
            );
        }

        runOnUiThread(() -> {
            Toast.makeText(this, "⚠️ Alerts sent to " + emergencyContacts.size() + " contact(s)", Toast.LENGTH_LONG).show();
            statusText.setText("⚠️ ALERT SENT!");
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
        });
    }

    private void analyzeResults(float[][][] output, long inferenceTime) {
        float maxAwakeConf = 0;
        float maxDrowsyConf = 0;

        for (int i = 0; i < 300; i++) {
            float conf = output[0][i][4];
            int classId = Math.round(output[0][i][5]);

            if (conf > CONF_THRESHOLD) {
                if (classId == 0) maxAwakeConf = Math.max(maxAwakeConf, conf);
                if (classId == 1) maxDrowsyConf = Math.max(maxDrowsyConf, conf);
            }
        }

        final float finalAwake = maxAwakeConf;
        final float finalDrowsy = maxDrowsyConf;

        runOnUiThread(() -> {
            frameCount++;
            frameText.setText("Frames: " + frameCount + " | " + inferenceTime + "ms");

            if (finalDrowsy > CONF_THRESHOLD && finalDrowsy >= finalAwake) {
                statusText.setText("⚠️ DROWSINESS DETECTED");
                statusText.setTextColor(getColor(android.R.color.holo_red_dark));
                updateStatusLabel("⚠️ Alert", "#FF1744");
                setAnimation("alert.gif");
                triggerAlarm();
                confidenceText.setText("Confidence: " + (int)(finalDrowsy * 100) + "% | Alerts: " + alertCount + "/" + ALERT_THRESHOLD);

            } else if (finalAwake > CONF_THRESHOLD) {
                statusText.setText(" DRIVER AWAKE");
                statusText.setTextColor(getColor(android.R.color.holo_green_light));
                confidenceText.setText("Confidence: " + (int)(finalAwake * 100) + "%");
                updateStatusLabel(" Awake", "#00E676");
                setAnimation("search.gif");
                stopAlarm();

            } else {
                statusText.setText("🔍 Waiting for detection...");
                statusText.setTextColor(getColor(android.R.color.white));
                confidenceText.setText("");
                updateStatusLabel("Analyzing", "#FFD600");
                setAnimation("eye_detection.gif");
                stopAlarm();
            }
        });
    }

    private void setAnimation(String gifName) {
        try {
            int resId = getResources().getIdentifier(
                    gifName.replace(".gif", ""),
                    "drawable",
                    getPackageName()
            );
            if (resId != 0) {
                Glide.with(this)
                        .load(resId)
                        .into(statusAnimation);
            } else {
                Log.e(TAG, "GIF not found: " + gifName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading GIF: " + gifName, e);
        }
    }

    private void updateStatusLabel(String label, String colorHex) {
        statusLabel.setText(label);
        try {
            int color = android.graphics.Color.parseColor(colorHex);
            statusIndicator.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color)
            );
        } catch (Exception e) {
            Log.e(TAG, "Color error", e);
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        android.content.res.AssetFileDescriptor fd = getAssets().openFd(MODEL_FILE);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(), fd.getDeclaredLength());
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Permission notification accordée");
            } else {
                Log.w(TAG, "⚠️ Permission notification refusée");
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        imageProxy -> {
                            long now = System.currentTimeMillis();
                            if (isRunning && (now - lastFrameTime) >= FRAME_INTERVAL_MS) {
                                lastFrameTime = now;
                                processImageProxy(imageProxy);
                            } else {
                                imageProxy.close();
                            }
                        });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis);

                Log.d(TAG, " Camera started");

            } catch (Exception e) {
                Log.e(TAG, "❌ Camera error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImageProxy(ImageProxy imageProxy) {
        try {
            long startTime = System.currentTimeMillis();

            Bitmap bitmap = imageProxy.toBitmap();
            imageProxy.close();

            if (bitmap == null) {
                Log.e(TAG, "❌ Bitmap null");
                return;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            Bitmap resized = Bitmap.createScaledBitmap(rotated, INPUT_SIZE, INPUT_SIZE, true);
            rotated.recycle();

            ByteBuffer inputBuffer = bitmapToByteBuffer(resized);
            resized.recycle();

            float[][][] output = new float[1][300][6];
            tflite.run(inputBuffer, output);

            long inferenceTime = System.currentTimeMillis() - startTime;

            analyzeResults(output, inferenceTime);

        } catch (Exception e) {
            Log.e(TAG, "❌ Frame processing error", e);
        }
    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            buffer.putFloat((pixel & 0xFF) / 255.0f);
        }
        return buffer;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupEmergencyContacts();
        applyNightMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) tflite.close();
        if (cameraProvider != null) cameraProvider.unbindAll();
        stopAlarm();
        if (toneGenerator != null) toneGenerator.release();
        if (locationService != null) locationService.stopUpdates();
        if (emailService != null) emailService.shutdown();
        emergencyHandler.removeCallbacksAndMessages(null);
    }
}