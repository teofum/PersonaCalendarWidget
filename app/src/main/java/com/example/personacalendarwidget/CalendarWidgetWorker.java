package com.example.personacalendarwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class CalendarWidgetWorker extends Worker {
    public static String WORK_NAME = "update-calendar-widget";

    private Context context;

    public CalendarWidgetWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);

        this.context = context;
    }

    @Override
    public Result doWork() {
        context.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE)
                .edit()
                .putLong("LAST_UPDATED", System.currentTimeMillis())
                .apply();

        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponentName = new ComponentName(context.getPackageName(), CalendarWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName);
        for (int appWidgetId : appWidgetIds) {
            CalendarWidget.updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        return Result.success();
    }
}
