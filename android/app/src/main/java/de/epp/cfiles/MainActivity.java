package de.epp.cfiles;

import android.content.Intent;
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
        setupMediaSession();
        setupMediaBridge();
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
