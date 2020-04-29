package com.android.widgetplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import com.android.widgetplayer.model.Audio;
import com.android.widgetplayer.widget.AudioWidgetProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AudioPlayer extends Service {
    private final String TAG = AudioPlayer.class.getSimpleName();
    private final String MP3 = "mp3";
    private final String COMMAND = "command";
    private final String PLAY = "play";

    private MediaPlayer player = new MediaPlayer();
    private List<Audio> audioList = new ArrayList<>();

    public static final String NOTIFICATION_CHANNEL_ID_SERVICE = "com.android.widgetplayer.service";

    @Override
    public void onCreate() {
        super.onCreate();
        scanAudio();
        requestForegroundStart();
    }

    private void scanAudio() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Cursor cursor = null;
        try {
            Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.ArtistColumns.ARTIST,};

            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            Cursor c = getApplicationContext().getContentResolver().query(uri, projection, selection, null, sortOrder);

            cursor = getApplicationContext().getContentResolver().query(uri, projection, selection, null, sortOrder);

            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {

                    String path = cursor.getString(0);
                    String album = cursor.getString(1);
                    String artist = cursor.getString(2);
                    String name = path.substring(path.lastIndexOf("/") + 1);
                    Audio audio = new Audio();
                    audio.setPath( path);
                    audio.setName(name);
                    audio.setAlbum(album);
                    audio.setArtist(artist);

                    if(path.substring(path.lastIndexOf(".")+1).equalsIgnoreCase(MP3))
                        audioList.add(audio);

                    cursor.moveToNext();
                }
            }

        } catch (Exception e) {
            Log.d(TAG, e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // To ensure the service is started within few seconds of requesting the service and notification
    private void requestForegroundStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID_SERVICE, "AudioPlayer", NotificationManager.IMPORTANCE_DEFAULT));

        } else {
            Notification notification = new Notification();
            startForeground(1, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Handles choosing random song from the list and issuing it to media player/ stop the current audio
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        requestForegroundStart();

        if (intent != null) {
            String command = intent.getStringExtra(COMMAND);
            if (command.equals(PLAY)) {
                startPlayer(getRandomSong());

            } else {
                stopPlayer();
            }

        }
        return super.onStartCommand(intent, flags, START_STICKY);
    }

    private void stopPlayer() {
        updateWidget(null, false, 0);
        if (player != null) {
            player.reset();
        }
    }

    // Updating the widgets based on the update from service
    private void updateWidget(Audio audio, boolean isPlaying, long duration) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, AudioWidgetProvider.class));
        AudioWidgetProvider.updateWidget(audio, isPlaying, getApplicationContext(),appWidgetManager,appWidgetIds);
        AudioWidgetProvider.getElapsedTime(duration, getApplicationContext());
    }

    private void startPlayer(Audio audio) {

        try {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(audio.getPath());
            player.prepare();
            updateWidget(audio, true, player.getDuration());
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                stopPlayer();
                stopSelf();
                return false;
            }
        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlayer();
            }
        });
    }

    private Audio getRandomSong() {
        Random r = new Random();
        int min = 0;
        int max = audioList.size();
        int random = r.nextInt(max - min) + min;
        return audioList.get(random);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayer();
    }
}


