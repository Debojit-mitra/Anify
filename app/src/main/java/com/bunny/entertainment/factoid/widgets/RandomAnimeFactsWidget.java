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
    import android.os.Handler;
    import android.os.Looper;
    import android.util.Log;
    import android.view.View;
    import android.widget.RemoteViews;
    import android.widget.Toast;

    import com.bunny.entertainment.factoid.R;
    import com.bunny.entertainment.factoid.adapter.AnimeFactsWidgetRemoteViewsFactory;
    import com.bunny.entertainment.factoid.networks.NetworkMonitor;
    import com.bunny.entertainment.factoid.networks.NetworkUtils;
    import com.bunny.entertainment.factoid.service.AnimeFactsWidgetRemoteViewsService;

    public class RandomAnimeFactsWidget extends AppWidgetProvider {
        public static final String ACTION_REFRESH = "com.bunny.entertainment.factoid.widgets.ANIME_ACTION_REFRESH";
        public static final String ACTION_AUTO_UPDATE = "com.bunny.entertainment.factoid.widgets.ANIME_ACTION_AUTO_UPDATE";
        public static final String ACTION_UPDATE_FINISHED = "com.bunny.entertainment.factoid.widgets.ANIME_ACTION_UPDATE_FINISHED";
        public static final String PREFS_NAME = "com.bunny.entertainment.factoid.AnimeWidgetPrefs";
        public static final String PREF_UPDATE_INTERVAL = "anime_update_interval";
        private NetworkMonitor networkMonitor;
        public long lastUpdateTime = 0;

        @Override
        public void onEnabled(Context context) {
            super.onEnabled(context);
            scheduleAutoUpdate(context);
        }
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
            if (networkMonitor == null) {
                networkMonitor = NetworkMonitor.getInstance(context);
            }

            String action = intent.getAction();
            if (ACTION_REFRESH.equals(action) || ACTION_AUTO_UPDATE.equals(action)) {
                Log.d("RandomAnimeFactsWidget", "Refresh action received");
                long currentTime = System.currentTimeMillis();
                long updateInterval = getUpdateIntervalMillis(context);
                if (currentTime - lastUpdateTime >= updateInterval) {
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        showProgressBar(context);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> performUpdate(context), 250); //setting delay of 250ms for showing progress bar
                    } else {
                        Toast.makeText(context, "Ahh! No Internet connection.", Toast.LENGTH_SHORT).show();
                        Log.d("RandomAnimeFactsWidget", "No internet connection. Waiting for network.");
                        networkMonitor.startMonitoring(() -> {
                            showProgressBar(context);
                            performUpdate(context);
                        });
                    }
                } else {
                    Log.d("RandomAnimeFactsWidget", "Update skipped: too soon after last update");
                }
            } else if (ACTION_UPDATE_FINISHED.equals(action)) {
                hideProgressBar(context);
                lastUpdateTime = System.currentTimeMillis();
                networkMonitor.stopMonitoring(); // Ensure we stop monitoring after update
            }
        }

        private void performUpdate(Context context) {
            AnimeFactsWidgetRemoteViewsFactory.setRefreshFlag();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RandomAnimeFactsWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);

            scheduleAutoUpdate(context);

            // Notify that update is finished
            Intent finishedIntent = new Intent(context, RandomAnimeFactsWidget.class);
            finishedIntent.setAction(ACTION_UPDATE_FINISHED);
            context.sendBroadcast(finishedIntent);
        }

        private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_facts_widget);

            Intent serviceIntent = new Intent(context, AnimeFactsWidgetRemoteViewsService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);

            Intent refreshIntent = new Intent(context, RandomAnimeFactsWidget.class);
            refreshIntent.setAction(ACTION_REFRESH);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        private void scheduleAutoUpdate(Context context) {
            long intervalMillis = getUpdateIntervalMillis(context);
            if (intervalMillis == 0) {
                // Auto-update is disabled, so don't schedule anything
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, RandomAnimeFactsWidget.class);
            intent.setAction(ACTION_AUTO_UPDATE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent);
                } else {
                    // Fall back to inexact alarm
                    alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent);
                    Log.w("RandomAnimeFactsWidget", "Exact alarms not allowed, using inexact alarm");
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

        private long getUpdateIntervalMillis(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getLong(PREF_UPDATE_INTERVAL, 3600000); // Default to 1 hour
        }

        public static void setUpdateInterval(Context context, long intervalMillis) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putLong(PREF_UPDATE_INTERVAL, intervalMillis).apply();
        }

        private void showProgressBar(Context context) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_facts_widget);
                views.setViewVisibility(R.id.widget_refresh_button, View.GONE);
                views.setViewVisibility(R.id.widget_refresh_progress, View.VISIBLE);
                updateWidgetViews(context, views);
            }, 200);
        }

        private void hideProgressBar(Context context) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_facts_widget);
            views.setViewVisibility(R.id.widget_refresh_button, View.VISIBLE);
            views.setViewVisibility(R.id.widget_refresh_progress, View.GONE);
            updateWidgetViews(context, views);
        }

        private void updateWidgetViews(Context context, RemoteViews views) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RandomAnimeFactsWidget.class);
            appWidgetManager.updateAppWidget(thisWidget, views);
        }
    }