package com.bunny.entertainment.factoid.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.adapter.WidgetRemoteViewsFactory;
import com.bunny.entertainment.factoid.service.WidgetRemoteViewsService;

public class RandomFactsWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.bunny.entertainment.factoid.widgets.ACTION_REFRESH";
    public static final String ACTION_AUTO_UPDATE = "com.bunny.entertainment.factoid.widgets.ACTION_AUTO_UPDATE";
    public  static final String PREFS_NAME = "com.bunny.entertainment.factoid.WidgetPrefs";
    public  static final String PREF_UPDATE_INTERVAL = "update_interval";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleAutoUpdate(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction()) || ACTION_AUTO_UPDATE.equals(intent.getAction())) {
            Log.d("RandomFactsWidget", "Refresh action received");
            WidgetRemoteViewsFactory.setRefreshFlag();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RandomFactsWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);

            if (ACTION_AUTO_UPDATE.equals(intent.getAction())) {
                scheduleAutoUpdate(context);
            }
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_facts_widget);

        Intent serviceIntent = new Intent(context, WidgetRemoteViewsService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);

        Intent refreshIntent = new Intent(context, RandomFactsWidget.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void scheduleAutoUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RandomFactsWidget.class);
        intent.setAction(ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long intervalMillis = getUpdateIntervalMillis(context);
        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent);
            } else {
                // Fall back to inexact alarm
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent);
                Log.w("RandomFactsWidget", "Exact alarms not allowed, using inexact alarm");
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent);
        }
    }

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public static Intent getAlarmPermissionSettingsIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + context.getPackageName()));
        }
        return null;
    }

    private long getUpdateIntervalMillis(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(PREF_UPDATE_INTERVAL, 3600000); // Default to 1 hour
    }

    public static void setUpdateInterval(Context context, long intervalMillis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_UPDATE_INTERVAL, intervalMillis).apply();
    }
}