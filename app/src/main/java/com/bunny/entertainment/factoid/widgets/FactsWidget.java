package com.bunny.entertainment.factoid.widgets;

import static com.bunny.entertainment.factoid.utils.Constants.ACTION_AUTO_UPDATE;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_REFRESH;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_RESET_ALARM;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_UPDATE_FINISHED;
import static com.bunny.entertainment.factoid.utils.Constants.DEFAULT_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREFS_NAME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_FACT_LAST_UPDATE_TIME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_FACT_UPDATE_INTERVAL;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.adapters.FactsWidgetRemoteViewsFactory;
import com.bunny.entertainment.factoid.network.NetworkMonitor;
import com.bunny.entertainment.factoid.network.NetworkUtils;
import com.bunny.entertainment.factoid.service.FactsWidgetRemoteViewsService;

public class FactsWidget extends AppWidgetProvider {

    private static final String TAG = "FactsWidget";
    private NetworkMonitor networkMonitor;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facts_widget);

        Intent serviceIntent = new Intent(context, FactsWidgetRemoteViewsService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);

        Intent refreshIntent = new Intent(context, FactsWidget.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_container, refreshPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.e(TAG, "onReceive");
        String action = intent.getAction();

        if (networkMonitor == null) {
            networkMonitor = NetworkMonitor.getInstance(context);
        }

        if (ACTION_REFRESH.equals(action) || ACTION_AUTO_UPDATE.equals(action)) {
            Log.e("PREF_FACT_UPDATE_INTERVAL", PREF_FACT_UPDATE_INTERVAL);
            if (NetworkUtils.isNetworkAvailable(context)) {
                showProgressBar(context);
                new Handler(Looper.getMainLooper()).postDelayed(() -> performUpdate(context), 250);

            } else {
                if (ACTION_AUTO_UPDATE.equals(action)) {
                    Log.d(TAG, "No internet connection. Waiting for network.");
                    networkMonitor.startMonitoring(() -> {
                        Log.d(TAG, "Network became available, starting update");
                        showProgressBar(context);
                        performUpdate(context);
                    });
                }
            }
            //   }
        } else if (ACTION_UPDATE_FINISHED.equals(action)) {
            Log.d(TAG, "Update finished");
            hideProgressBar(context);
            ///  lastUpdateTime = System.currentTimeMillis();
            networkMonitor.stopMonitoring();
        } else if (ACTION_RESET_ALARM.equals(action)) {
            scheduleAutoUpdate(context); //scheduling new alarm
        }
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) { //ensures that if app is updated it reloads views
            Log.d(TAG, "App updated, refreshing widgets");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, FactsWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, FactsWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            // Force data refresh in RemoteViewsFactory
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);

            // Update widget layout
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }

    }

    private void performUpdate(Context context) {
        Log.d(TAG, "Performing update");
        FactsWidgetRemoteViewsFactory.setRefreshFlag();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, FactsWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long updateTime = System.currentTimeMillis();
        prefs.edit().putLong(PREF_FACT_LAST_UPDATE_TIME, updateTime).apply();
        //  Log.d(TAG, "Last update time saved: " + updateTime);
        // Update the widget layout
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facts_widget);
        appWidgetManager.updateAppWidget(thisWidget, views);

        scheduleAutoUpdate(context);

        Intent finishedIntent = new Intent(context, FactsWidget.class);
        finishedIntent.setAction(ACTION_UPDATE_FINISHED);
        context.sendBroadcast(finishedIntent);

    }

    private void scheduleAutoUpdate(Context context) {
        Log.d(TAG, "scheduleAutoUpdate");
        long intervalMillis = getUpdateIntervalMillis(context);
        Log.d(TAG, "Scheduling auto update with interval: " + intervalMillis);
        if (intervalMillis == 0) {
            Log.d(TAG, "Auto-update is disabled");
            return;
        }

        //SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        //long lastUpdateTime = prefs.getLong(PREF_FACT_LAST_UPDATE_TIME, 0);
        // long currentTime = System.currentTimeMillis();
        // long elapsedTime = currentTime - lastUpdateTime;

        // long nextUpdateDelay = (elapsedTime >= intervalMillis) ? 0 : intervalMillis - elapsedTime;
        //Log.d(TAG, "Next update scheduled in " + nextUpdateDelay + " ms");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FactsWidget.class);
        intent.setAction(ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "Setting exact alarm");
                alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent);
            } else {
                Log.w(TAG, "Exact alarms not allowed, using inexact alarm");
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent);
            }
        } else {
            Log.d(TAG, "Setting exact alarm (pre-Android 12)");
            alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent);
        }

    }

    private void cancelAlarmAndClear(Context context) {
        // Cancel any pending alarms
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FactsWidget.class);
        intent.setAction(ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled pending alarms");
        }

        // Stop network monitoring if it's running
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
            networkMonitor = null;
            Log.d(TAG, "Stopped network monitoring");
        }

    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled called");
        // Perform an initial update to load data immediately
        scheduleAutoUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled called - last widget instance removed");
        cancelAlarmAndClear(context);
    }

    private void showProgressBar(Context context) {
        Log.d(TAG, "Showing progress bar");
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facts_widget);
        views.setViewVisibility(R.id.widget_refresh_button, View.GONE);
        views.setViewVisibility(R.id.widget_refresh_progress, View.VISIBLE);
        updateWidgetViews(context, views);
    }

    private void hideProgressBar(Context context) {
        Log.d(TAG, "Hiding progress bar");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.facts_widget);
            views.setViewVisibility(R.id.widget_refresh_button, View.VISIBLE);
            views.setViewVisibility(R.id.widget_refresh_progress, View.GONE);
            updateWidgetViews(context, views);
        }, 200);
    }

    private void updateWidgetViews(Context context, RemoteViews views) {
        Log.d(TAG, "Updating widget views");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, FactsWidget.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    //getter
    private long getUpdateIntervalMillis(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(com.bunny.entertainment.factoid.utils.Constants.PREF_FACT_UPDATE_INTERVAL, DEFAULT_INTERVAL); // Default to 1hr
    }

    private long getLastUpdateTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(PREF_FACT_LAST_UPDATE_TIME, 0);
        Log.d(TAG, "Retrieved last update time: " + lastUpdateTime);
        return lastUpdateTime;
    }

}