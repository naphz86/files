package de.epp.cfiles;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;

/**
 * Zeigt die laufende Wiedergabe als System-Player (Statusleiste / Sperrbildschirm /
 * Schnelleinstellungen) mit Play/Pause, Vor und Zurück.
 * Läuft als Vordergrunddienst (Typ mediaPlayback), solange ein Lied spielt.
 */
public class MediaService extends Service {

    public static final String ACTION_UPDATE = "de.epp.cfiles.media.UPDATE";
    public static final String ACTION_STOP = "de.epp.cfiles.media.STOP";
    public static final String ACTION_PLAY_PAUSE = "de.epp.cfiles.media.PLAY_PAUSE";
    public static final String ACTION_NEXT = "de.epp.cfiles.media.NEXT";
    public static final String ACTION_PREV = "de.epp.cfiles.media.PREV";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PLAYING = "playing";

    private static final String CHANNEL_ID = "cfiles_playback";
    private static final int NOTIFICATION_ID = 4711;

    /** Wird von MainActivity gesetzt; der Dienst braucht das Token der Session. */
    public static volatile MediaSession.Token sessionToken;
    /** Weiterleitung der Benachrichtigungs-Tasten an die Activity. */
    public static volatile MainActivity activity;

    private String title = "";
    private boolean playing = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_PLAY_PAUSE.equals(action)) {
            if (activity != null) activity.toggleFromNotification();
            return START_NOT_STICKY;
        }
        if (ACTION_NEXT.equals(action)) {
            if (activity != null) activity.nextFromNotification();
            return START_NOT_STICKY;
        }
        if (ACTION_PREV.equals(action)) {
            if (activity != null) activity.prevFromNotification();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            if (intent.hasExtra(EXTRA_TITLE)) title = intent.getStringExtra(EXTRA_TITLE);
            playing = intent.getBooleanExtra(EXTRA_PLAYING, playing);
        }

        Notification n = buildNotification();
        // Immer startForeground aufrufen (der Dienst wurde per startForegroundService gestartet,
        // sonst droht auf Android 8+ eine ForegroundServiceDidNotStartInTimeException).
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
        if (!playing) {
            // Pausiert: Benachrichtigung bleibt sichtbar, ist aber wegwischbar.
            stopForeground(false);
        }
        return START_NOT_STICKY;
    }

    private PendingIntent action(String act, int req) {
        Intent i = new Intent(this, MediaService.class).setAction(act);
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) f |= PendingIntent.FLAG_IMMUTABLE;
        // Bewusst getService: Benachrichtigungs-Aktionen dürfen den laufenden Dienst auch aus dem
        // Hintergrund ansprechen; getForegroundService würde ein erneutes startForeground erzwingen.
        return PendingIntent.getService(this, req, i, f);
    }

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Wiedergabe", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(this, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) f |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content = PendingIntent.getActivity(this, 0, open, f);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);

        b.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title == null || title.isEmpty() ? "Meine Dateien" : title)
            .setContentText("Meine Dateien")
            .setContentIntent(content)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setDeleteIntent(action(ACTION_STOP, 9))
            .addAction(android.R.drawable.ic_media_previous, "Zurück", action(ACTION_PREV, 1))
            .addAction(
                playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                playing ? "Pause" : "Play",
                action(ACTION_PLAY_PAUSE, 2))
            .addAction(android.R.drawable.ic_media_next, "Weiter", action(ACTION_NEXT, 3));

        Notification.MediaStyle style = new Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2);
        if (sessionToken != null) style.setMediaSession(sessionToken);
        b.setStyle(style);

        return b.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
