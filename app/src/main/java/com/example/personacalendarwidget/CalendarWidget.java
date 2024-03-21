package com.example.personacalendarwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of App Widget functionality.
 */
public class CalendarWidget extends AppWidgetProvider {
    private static final String SYNC_CLICKED = "automaticWidgetSyncButtonClick";

    // Returns a string representing the rough time of day based on the hour
    static String getTimeOfDayString(int hour) {
        if (hour < 4) return "latenight";
        if (hour < 8) return "earlymorning";
        if (hour < 12) return "morning";
        if (hour < 15) return "daytime";
        if (hour < 18) return "afternoon";
        if (hour < 22) return "evening";
        return "latenight";
    }

    static PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, CalendarWidget.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d("Widget", "Update widget");

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.calendar_widget);

        // Get the current date and time and extract month, day, weekday, time of day
        LocalDateTime now = LocalDateTime.now();
        String month = now.format(DateTimeFormatter.ofPattern("M"));
        String day = now.format(DateTimeFormatter.ofPattern("d"));
        String weekday = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)).toLowerCase();
        int hour = now.getHour();
        String time = getTimeOfDayString(hour);

        // Get the image resources and set ImageView
        // Month
        int month0 = context.getResources().getIdentifier(String.format("month_%s_0", month), "drawable", context.getPackageName());
        int month1 = context.getResources().getIdentifier(String.format("month_%s_1", month), "drawable", context.getPackageName());
        int month2 = context.getResources().getIdentifier(String.format("month_%s_2", month), "drawable", context.getPackageName());
        views.setImageViewResource(R.id.month_0, month0);
        views.setImageViewResource(R.id.month_1, month1);
        views.setImageViewResource(R.id.month_2, month2);

        // Day
        int day0 = context.getResources().getIdentifier(String.format("day_%s_0", day), "drawable", context.getPackageName());
        int day1 = context.getResources().getIdentifier(String.format("day_%s_1", day), "drawable", context.getPackageName());
        int day2 = context.getResources().getIdentifier(String.format("day_%s_2", day), "drawable", context.getPackageName());
        views.setImageViewResource(R.id.day_0, day0);
        views.setImageViewResource(R.id.day_1, day1);
        views.setImageViewResource(R.id.day_2, day2);

        // Weekday
        int weekday0 = context.getResources().getIdentifier(String.format("weekday_%s_0", weekday), "drawable", context.getPackageName());
        int weekday1 = context.getResources().getIdentifier(String.format("weekday_%s_1", weekday), "drawable", context.getPackageName());
        int weekday2 = context.getResources().getIdentifier(String.format("weekday_%s_2", weekday), "drawable", context.getPackageName());
        views.setImageViewResource(R.id.weekday_0, weekday0);
        views.setImageViewResource(R.id.weekday_1, weekday1);
        views.setImageViewResource(R.id.weekday_2, weekday2);

        // Time
        int timeResource = context.getResources().getIdentifier(String.format("time_%s", time), "drawable", context.getPackageName());
        views.setImageViewResource(R.id.time, timeResource);

        // Debug time string
//        String debug = now.format(DateTimeFormatter.ofPattern("HH:mm:ss M/d EEEE"));
//        views.setTextViewText(R.id.debugText, debug);

        // Set a pending intent to detect touch event
        ComponentName watchWidget = new ComponentName(context, CalendarWidget.class.getName());
        views.setOnClickPendingIntent(R.id.time, getPendingSelfIntent(context, SYNC_CLICKED));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("Widget", "onReceive");

        if (intent.getAction().equals(SYNC_CLICKED)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponentName = new ComponentName(context.getPackageName(), CalendarWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName);
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        enqueueUpdateAppWidgetRequest(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        cancelUpdateAppWidgetRequest(context);
    }

    private void enqueueUpdateAppWidgetRequest(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CalendarWidgetWorker.class,
                15,
                TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                CalendarWidgetWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    private void cancelUpdateAppWidgetRequest(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(CalendarWidgetWorker.WORK_NAME);
    }
}