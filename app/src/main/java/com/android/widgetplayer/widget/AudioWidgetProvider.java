package com.android.widgetplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.android.widgetplayer.R;
import com.android.widgetplayer.model.Audio;
import com.android.widgetplayer.service.AudioPlayer;


public class AudioWidgetProvider extends AppWidgetProvider{
    private static final String TAG = AudioWidgetProvider.class.getSimpleName();
    private static String ONCLICK = "OnClick";
    private final String COMMAND = "command";
    private final String PLAY = "play";
    private final String STOP = "stop";
    private final static String SETBKGRES = "setBackgroundResource";

    private static boolean isPlaying = false;
    private RemoteViews remoteViews;
    private ComponentName audioPlayerWidget;
    private static CountDownTimer downTimer;
    private static  long elapsedTimeStart;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        audioPlayerWidget = new ComponentName(context, AudioWidgetProvider.class);

        Intent intentClick = new Intent(context, AudioWidgetProvider.class);
        intentClick.setAction(ONCLICK);
        intentClick.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, "" + appWidgetIds[0]);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetIds[0], intentClick, 0);
        remoteViews.setOnClickPendingIntent(R.id.btnPlay, pendingIntent);
        remoteViews.setInt(R.id.btnPlay, SETBKGRES, R.drawable.play);
        appWidgetManager.updateAppWidget(audioPlayerWidget, remoteViews);
    }

    //Starting service based on the mode of PLAY or STOP
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Bundle extras = intent.getExtras();
        if (extras != null && intent.getAction().equals(ONCLICK)) {
            if (isPlaying) {
                startService(STOP, context);
            } else {
                startService(PLAY, context);
            }
            audioPlayerWidget = new ComponentName(context, AudioWidgetProvider.class);
            (AppWidgetManager.getInstance(context)).updateAppWidget(audioPlayerWidget, remoteViews);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        Intent i = new Intent(context, AudioPlayer.class);
        i.putExtra(COMMAND, STOP);
        context.stopService(i);
    }

    private void startService(String action, Context context) {
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        if(action == STOP)
            isPlaying = false;
        else
            isPlaying = true;

        Intent i = new Intent(context, AudioPlayer.class);
        i.putExtra(COMMAND, action);
        try {
            context.startService(i);
        }
        catch(Exception e ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, i);
            } else {
                context.startService(i);

            }
        }
    }

    //Updating the audio data to the widget from the service
    public static void updateWidget(Audio song, boolean bisPlaying, Context context, AppWidgetManager appWidgetManager,
                                    int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            if (bisPlaying) {
                remoteViews.setTextViewText(R.id.Name, song.getName());
                remoteViews.setTextViewText(R.id.Artist, String.format("%s - %s", song.getArtist(), song.getAlbum()));
                remoteViews.setImageViewResource(R.id.imgAudio, R.drawable.ic_audiotrack);
                remoteViews.setInt(R.id.btnPlay, SETBKGRES, R.drawable.stop);

            } else {
                isPlaying = false;
                remoteViews.setTextViewText(R.id.Artist, context.getString(R.string.artist_name));
                remoteViews.setTextViewText(R.id.Elapsedtime, context.getString(R.string.elapsed_time));
                remoteViews.setTextViewText(R.id.Name, context.getString(R.string.app_name));
                remoteViews.setInt(R.id.btnPlay, SETBKGRES, R.drawable.play);
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    // Starting a counter along with calculating an elapsed timer
    public static void getElapsedTime(long duration, final Context context) {
        elapsedTimeStart = System.currentTimeMillis();
        if(duration == 0 && downTimer != null) {
            downTimer.cancel();
        }
        else {
            downTimer = new CountDownTimer(duration, 1000) {

                public void onTick(long millisUntilFinished) {
                    updateElapsedTime(millisUntilFinished, context);
                }

                public void onFinish() {
                    updateElapsedTime(0, context);
                }
            };
            downTimer.start();
        }

    }

    //Updating Elapsed time in the widget continuously based on the count down timer
    private static void updateElapsedTime(long duration, Context context) {
        long elapsedTimeEnd = System.currentTimeMillis();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        remoteViews.setTextViewText(R.id.Elapsedtime, evaluateElapsedTime(elapsedTimeEnd - elapsedTimeStart));
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName theWidget = new ComponentName(context, AudioWidgetProvider.class);
        manager.updateAppWidget(theWidget, remoteViews);
    }

    //Evaluating the time from milliseconds received to string output
    private static String evaluateElapsedTime(long milliseconds) {
        String res = "";
        String minutes = "";
        String seconds = "";

        int hr = (int) (milliseconds / (1000 * 60 * 60));
        int min = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int sec = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (hr > 0) {
            res = hr + ":";
        }

        if (sec < 10) {
            seconds = "0" + sec;
        }   else {
            seconds = "" + sec;
        }

        if(min < 10) {
            minutes = "0" + min;
        }
        else {
            minutes = "" + min;
        }

        res = res + minutes + ":" + seconds;
        return res;
    }
}





