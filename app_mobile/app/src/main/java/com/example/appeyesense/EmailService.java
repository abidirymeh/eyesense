package com.example.appeyesense;

import android.content.Context;
import android.util.Log;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {
    private static final String TAG = "EmailService";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String EMAIL_ADDRESS = "appeyesense@gmail.com";
    private static final String APP_PASSWORD = "gvxa kzqt usrc cvbb";

    private final EmailGenerator emailGenerator;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public EmailService(Context context) {
        this.emailGenerator = new EmailGenerator(context);
    }


    public void sendDrowsinessAlert(String recipientEmail, String location, String timestamp,
                                    String latitude, String longitude, Callback callback) {
        executorService.execute(() -> {
            try {
                String subject = "🚨 ALERTE SOMNOLENCE DÉTECTÉE - EyeSense";
                String body = buildAlertEmailBody(location, timestamp, latitude, longitude);
                boolean success = sendEmail(recipientEmail, subject, body);
                if (callback != null) {
                    callback.onEmailSent(success, success ? "Email envoyé" : "Erreur");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur envoi", e);
                if (callback != null) {
                    callback.onEmailSent(false, e.getMessage());
                }
            }
        });
    }


    public void sendTestEmail(String recipientEmail, String location, String timestamp,
                              String latitude, String longitude, Callback callback) {
        executorService.execute(() -> {
            try {
                String subject = "🧪 TEST - EyeSense Email Service avec Localisation";
                String body = buildTestEmailBody(location, timestamp, latitude, longitude);
                boolean success = sendEmail(recipientEmail, subject, body);
                if (callback != null) {
                    callback.onEmailSent(success, success ? "Email de test envoyé" : "Erreur");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur test email", e);
                if (callback != null) {
                    callback.onEmailSent(false, e.getMessage());
                }
            }
        });
    }


    private String buildAlertEmailBody(String location, String timestamp, String latitude, String longitude) {
        String mapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;

        return "⚠️ ALERTE DE SOMNOLENCE DÉTECTÉE\n" +
                "================================\n\n" +
                "Le conducteur a été détecté somnolent et risque de s'endormir.\n\n" +
                "📍 LOCALISATION:\n" +
                location + "\n" +
                "   Latitude: " + latitude + "\n" +
                "   Longitude: " + longitude + "\n" +
                "   Lien Google Maps: " + mapsLink + "\n\n" +
                "🕐 HEURE:\n" +
                timestamp + "\n\n" +
                "📱 DISPOSITIF:\n" +
                emailGenerator.getDeviceInfo() + "\n\n" +
                "⚡ ACTION REQUISE:\n" +
                "Vérifiez immédiatement l'état du conducteur!\n\n" +
                "---\n" +
                "Message généré automatiquement par EyeSense";
    }


    private String buildTestEmailBody(String location, String timestamp, String latitude, String longitude) {
        String mapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;

        return "🧪 TEST - EyeSense Email Service\n" +
                "================================\n\n" +
                "Ceci est un email de test.\n" +
                "✅ Le service d'email fonctionne correctement!\n\n" +
                "📍 LOCALISATION DE TEST:\n" +
                location + "\n" +
                "   Latitude: " + latitude + "\n" +
                "   Longitude: " + longitude + "\n" +
                "   Lien Google Maps: " + mapsLink + "\n\n" +
                "🕐 HEURE:\n" +
                timestamp + "\n\n" +
                "📱 DISPOSITIF:\n" +
                emailGenerator.getDeviceInfo() + "\n\n" +
                "---\n" +
                "Message généré automatiquement par EyeSense";
    }


    private boolean sendEmail(String recipientEmail, String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", 5000);
            props.put("mail.smtp.timeout", 5000);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_ADDRESS, APP_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_ADDRESS));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            Log.d(TAG, " Email envoyé avec succès à " + recipientEmail);
            return true;

        } catch (MessagingException e) {
            Log.e(TAG, "❌ Erreur MessagingException", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur générale", e);
            return false;
        }
    }

    public interface Callback {
        void onEmailSent(boolean success, String message);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}