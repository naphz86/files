package de.epp.cfiles;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private MediaSession mediaSession;
    private String currentTitle = "";

    private static final long ACTIONS =
        PlaybackState.ACTION_PLAY |
        PlaybackState.ACTION_PAUSE |
        PlaybackState.ACTION_SKIP_TO_NEXT |
        PlaybackState.ACTION_SKIP_TO_PREVIOUS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerPlugin(ZipPlugin.class);
        super.onCreate(savedInstanceState);
        requestManageFilesPermission();
        requestNotificationPermission();
        setupMediaSession();
        setupMediaBridge();
        MediaService.activity = this;
    }

    /** Android 13+: ohne diese Berechtigung wird keine Medien-Benachrichtigung angezeigt. */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    // Aufrufe aus der Benachrichtigung (MediaService) -> Player in der WebView
    public void toggleFromNotification() {
        js("if(typeof playerTogglePause==='function')playerTogglePause();");
    }
    public void nextFromNotification() {
        js("if(typeof playerNext==='function')playerNext();");
    }
    public void prevFromNotification() {
        js("if(typeof playerPrev==='function')playerPrev();");
    }

    private boolean serviceRunning = false;

    private void updateService(String title, boolean playing) {
        Intent i = new Intent(this, MediaService.class)
            .setAction(MediaService.ACTION_UPDATE)
            .putExtra(MediaService.EXTRA_PLAYING, playing);
        if (title != null) i.putExtra(MediaService.EXTRA_TITLE, title);
        try {
            if (serviceRunning || Build.VERSION.SDK_INT < 26) {
                startService(i);
            } else {
                startForegroundService(i);
            }
            serviceRunning = true;
        } catch (Exception e) {
            // z. B. Start aus dem Hintergrund nicht erlaubt – Session funktioniert trotzdem weiter
        }
    }

    private void setupMediaSession() {
        // WICHTIG: Hier KEIN requestAudioFocus() und KEIN setActive(true) beim Start.
        // Sonst pausiert fremde Musik, sobald die App geöffnet wird.
        // Den Audio-Fokus fordert das <audio>-Element der WebView selbst an,
        // sobald wirklich ein Lied abgespielt wird.

        mediaSession = new MediaSession(this, "cfiles");
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        PlaybackState state = new PlaybackState.Builder()
            .setActions(ACTIONS)
            .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
            .build();
        mediaSession.setPlaybackState(state);

        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                js("if(typeof playerTogglePause==='function'&&document.getElementById('audioEl').paused)playerTogglePause();");
            }
            @Override
            public void onPause() {
                js("if(typeof playerTogglePause==='function'&&!document.getElementById('audioEl').paused)playerTogglePause();");
            }
            @Override
            public void onSkipToNext() {
                js("if(typeof playerNext==='function')playerNext();");
            }
            @Override
            public void onSkipToPrevious() {
                js("if(typeof playerPrev==='function')playerPrev();");
            }
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                KeyEvent ev = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (ev != null && ev.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (ev.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            onSkipToNext(); return true;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            onSkipToPrevious(); return true;
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            js("if(typeof playerTogglePause==='function')playerTogglePause();"); return true;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            onPlay(); return true;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            onPause(); return true;
                    }
                }
                return super.onMediaButtonEvent(intent);
            }
        });

        MediaService.sessionToken = mediaSession.getSessionToken();

        // Session bleibt inaktiv, bis in der App wirklich ein Lied gestartet wird.
        mediaSession.setActive(false);
    }

    /**
     * Brücke für die WebView: index.html ruft AndroidMedia.setPlaying(true/false) auf.
     * Erst dann wird die MediaSession aktiv (Kopfhörer-/Bluetooth-Tasten) und der
     * Wiedergabestatus gesetzt.
     */
    private void setupMediaBridge() {
        getBridge().getWebView().addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void setTitle(final String title) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaSession == null) return;
                        currentTitle = title;
                        mediaSession.setMetadata(new MediaMetadata.Builder()
                            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Meine Dateien")
                            .build());
                    }
                });
            }

            @JavascriptInterface
            public void setPlaying(final boolean playing) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaSession == null) return;
                        mediaSession.setActive(true);
                        mediaSession.setPlaybackState(new PlaybackState.Builder()
                            .setActions(ACTIONS)
                            .setState(
                                playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                                1f)
                            .build());
                        updateService(currentTitle, playing);
                    }
                });
            }
        }, "AndroidMedia");
    }

    private void js(final String script) {
        getBridge().getWebView().post(new Runnable() {
            @Override public void run() {
                getBridge().getWebView().evaluateJavascript(script, null);
            }
        });
    }

    @Override
    public void onDestroy() {
        if (MediaService.activity == this) MediaService.activity = null;
        stopService(new Intent(this, MediaService.class));
        serviceRunning = false;
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }

    private void requestManageFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            }
        }
    }
}
