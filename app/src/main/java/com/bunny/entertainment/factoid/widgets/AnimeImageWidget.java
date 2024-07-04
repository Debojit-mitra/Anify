package com.bunny.entertainment.factoid.widgets;

import static com.bunny.entertainment.factoid.utils.Constants.ACTION_AUTO_UPDATE;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_DOWNLOAD;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_DOWNLOAD_COMPLETE;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_DOWNLOAD_FAILED;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_HOMESCREEN_FROM_APP;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_HOME_SCREEN_BECAME_VISIBLE;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_REFRESH;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_RESET_ALARM;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_STOP_AUTO_UPDATE;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_UPDATE_FINISHED;
import static com.bunny.entertainment.factoid.utils.Constants.API_NEKOBOT;
import static com.bunny.entertainment.factoid.utils.Constants.API_WAIFU_IM;
import static com.bunny.entertainment.factoid.utils.Constants.API_WAIFU_PICS;
import static com.bunny.entertainment.factoid.utils.Constants.API_WAIFU_PICS_NSFW;
import static com.bunny.entertainment.factoid.utils.Constants.PREFS_NAME;
import static com.bunny.entertainment.factoid.utils.Constants.PREFS_NSFW_SWITCH;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_IMAGE_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_IMAGE_LAST_UPDATE_TIME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_IMAGE_UPDATE_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_IMAGE_WIDGET_INITIALIZED;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_API_SOURCE;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_ANIME_IMAGE_URL;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.models.NekoBotImageResponse;
import com.bunny.entertainment.factoid.models.WaifuImResponse;
import com.bunny.entertainment.factoid.models.WaifuPicsImageResponse;
import com.bunny.entertainment.factoid.network.ApiService;
import com.bunny.entertainment.factoid.network.NetworkMonitor;
import com.bunny.entertainment.factoid.network.NetworkUtils;
import com.bunny.entertainment.factoid.network.RetrofitClient;
import com.bunny.entertainment.factoid.utils.DownloadService;
import com.bunny.entertainment.factoid.utils.HomeScreenDetector;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnimeImageWidget extends AppWidgetProvider {

    private static final String TAG = "AnimeImageWidget";
    private NetworkMonitor networkMonitor;
    private boolean isHomeScreenDetectorRegistered = false;
    private boolean isAnimeImageWidgetRegistered = false;
    String byteSizeConstraintForWaifuIm = "<=3145728";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.e(TAG, "onUpdate AnimeImageWidget");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(PREF_ANIME_IMAGE_WIDGET_INITIALIZED, false);

        if (initialized) {//or else it wont refresh widget when phone comes out of sleep
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
            refreshWidgetViews(context);
            if (isUpdateNeeded(context)){
                showDownloadProgressBar(context);
                performUpdate(context);
            } else {
                scheduleAutoUpdate(context);
            }
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(PREF_ANIME_IMAGE_WIDGET_INITIALIZED, false);
        if (initialized) {
            refreshWidgetViews(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.e(TAG, "onEnabled AnimeImageWidget");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(PREF_ANIME_IMAGE_WIDGET_INITIALIZED, false);

        updateWidget(context);
        if (!initialized) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> prefs.edit().putBoolean(PREF_ANIME_IMAGE_WIDGET_INITIALIZED, true).apply(), Log.e(TAG, "set initialized"), 1000);

        }
        registerReceivers(context);
        scheduleAutoUpdate(context);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerReceivers(Context context) {
        if (!isHomeScreenDetectorRegistered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.getApplicationContext().registerReceiver(new HomeScreenDetector(), filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    context.getApplicationContext().registerReceiver(new HomeScreenDetector(), filter);
                }
                isHomeScreenDetectorRegistered = true;
            } catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }

        if (!isAnimeImageWidgetRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            filter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
            filter.addAction(Intent.ACTION_BOOT_COMPLETED);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.getApplicationContext().registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    context.getApplicationContext().registerReceiver(this, filter);
                }
                isAnimeImageWidgetRegistered = true;
            } catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }
    }

    private void refreshWidgetViews(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            String lastImageUrl = getLastImageUrl(context);
            if (lastImageUrl != null) {
                updateWidgetWithImage(context, appWidgetManager, appWidgetId, lastImageUrl);
            }
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.e(TAG, "onDisabled AnimeImageWidget");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(PREF_ANIME_IMAGE_WIDGET_INITIALIZED, false);
        if (initialized) {
            prefs.edit().putBoolean(PREF_ANIME_IMAGE_WIDGET_INITIALIZED, false).apply();
        }
        cancelAlarmAndClear(context);

        try {
            context.getApplicationContext().unregisterReceiver(this);
            context.getApplicationContext().unregisterReceiver(new HomeScreenDetector());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void cancelAlarmAndClear(Context context) {
        // Cancel any pending alarms
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AnimeImageWidget.class);
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

    private boolean isUpdateNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(PREF_ANIME_IMAGE_LAST_UPDATE_TIME, 0);
        long updateInterval = getUpdateInterval(context);
        return System.currentTimeMillis() - lastUpdateTime >= updateInterval;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(PREF_ANIME_IMAGE_WIDGET_INITIALIZED, false);
        String action = intent.getAction();
        Log.e(TAG, "ACTION: " + action);
        if (initialized) {

            Log.e(TAG, "onReceive AnimeImageWidget");

            if (networkMonitor == null) {
                networkMonitor = NetworkMonitor.getInstance(context);
            }

            if (ACTION_REFRESH.equals(action) || ACTION_AUTO_UPDATE.equals(action)) {
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
            } else if (ACTION_UPDATE_FINISHED.equals(action)) {
                Log.d(TAG, "ACTION_UPDATE_FINISHED");
                hideProgressBar(context);
                networkMonitor.stopMonitoring();
            } else if (ACTION_RESET_ALARM.equals(action)) {
                scheduleAutoUpdate(context); //scheduling new alarm
            } else if (ACTION_STOP_AUTO_UPDATE.equals(action)) {
                cancelAlarmAndClear(context);
            } else if (ACTION_DOWNLOAD.equals(intent.getAction())) {
                String imageUrl = intent.getStringExtra("image_url");
                Log.d("RandomAnimeImageWidget", "Download action received for URL: " + imageUrl);
                if (imageUrl != null) {
                    downloadImage(context, imageUrl);
                } else {
                    Log.e("RandomAnimeImageWidget", "Image URL is null");
                }
            } else if (ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Log.d("RandomAnimeImageWidget", "Download complete, hiding progress bar");
                hideDownloadProgressBar(context);
                Toast.makeText(context, "Download complete", Toast.LENGTH_SHORT).show();
            } else if (ACTION_DOWNLOAD_FAILED.equals(action)) {
                Log.d("RandomAnimeImageWidget", "Download failed, hiding progress bar");
                hideDownloadProgressBar(context);
                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show();
            }

            if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
                Log.d(TAG, "Received ACTION_APPWIDGET_UPDATE");
                registerReceivers(context);
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
                }
            }

            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                Log.d(TAG, "ACTION_BOOT_COMPLETED");
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

                for (int appWidgetId : appWidgetIds) {
                    long updateInterval = getUpdateInterval(context);
                    if (updateInterval == 0) {
                        String lastImageUrl = getLastImageUrl(context);
                        if (lastImageUrl != null) {
                            updateWidgetWithImage(context, appWidgetManager, appWidgetId, lastImageUrl);
                        }
                    }
                }
            }

            if (ACTION_HOMESCREEN_FROM_APP.equals(action) || Intent.ACTION_CONFIGURATION_CHANGED.equals(action) || Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.e(TAG, "ACTION_HOMESCREEN_FROM_APP || ACTION_CONFIGURATION_CHANGED || ACTION_SCREEN_ON");
                refreshWidgetViews(context);
            }

            if (ACTION_HOME_SCREEN_BECAME_VISIBLE.equals(action)) {
                Log.d(TAG, "Home screen became visible, refreshing widget");
                refreshWidgetViews(context);
            }

            // Register for download complete and failed broadcasts
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_DOWNLOAD_COMPLETE);
            filter.addAction(ACTION_DOWNLOAD_FAILED);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.getApplicationContext().registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    context.getApplicationContext().registerReceiver(this, filter);
                }
            } catch (Exception e){
                Log.e(TAG, e.toString());
            }
        } else {
            if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) { //ensures that if app is updated it reloads views
                Log.d(TAG, "App updated, refreshing widgets");
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                onUpdate(context, appWidgetManager, appWidgetIds);
                registerReceivers(context);
            }
        }
    }


    private void downloadImage(Context context, String imageUrl) {
        Log.d(TAG, "downloadImage: Attempting to download image from URL: " + imageUrl);
        if (NetworkUtils.isNetworkAvailable(context)) {
            Intent intent = new Intent(context, DownloadService.class);
            intent.putExtra("image_url", imageUrl);

            context.startForegroundService(intent);
            showDownloadProgressBar(context);
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "downloadImage: Download service started");
        } else {
            Log.d(TAG, "downloadImage: No internet connection available");
            Toast.makeText(context, "Ahh! No Internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    private void performUpdate(Context context) {
        Log.d(TAG, "Performing update");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        String category = getImageCategory(context);
        Log.d(TAG, "Fetching and updating image for category: " + category);
        fetchAndUpdateImage(context, appWidgetManager, appWidgetIds, category);

        scheduleAutoUpdate(context);
    }

    private void updateWidget(Context context) {
        Log.d("RandomAnimeImageWidget", "Updating widget after sleep");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void scheduleAutoUpdate(Context context) {
        long intervalMillis = getUpdateInterval(context);
        Log.d(TAG, "scheduleAutoUpdate: Scheduling auto-update with interval: " + intervalMillis + " ms");
        if (intervalMillis == 0) {
            // Auto-update is disabled, so don't schedule anything
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_ANIME_IMAGE_LAST_UPDATE_TIME, System.currentTimeMillis()).apply();
        Log.d(TAG, "Last update time saved: " + System.currentTimeMillis());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AnimeImageWidget.class);
        intent.setAction(ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "Setting exact alarm");
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pendingIntent);
            } else {
                Log.w(TAG, "Exact alarms not allowed, using inexact alarm");
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent);
            }
        } else {
            Log.d(TAG, "Setting exact alarm (pre-Android 12)");
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pendingIntent);
        }

    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "Updating widget with ID: " + appWidgetId);
        String lastImageUrl = getLastImageUrl(context);
        if (lastImageUrl != null) {
            Log.d(TAG, "Using last image URL: " + lastImageUrl);
            updateWidgetWithImage(context, appWidgetManager, appWidgetId, lastImageUrl);
        } else {
            Log.d(TAG, "No last image URL, showing placeholder and fetching new image");
            showPlaceholderImage(context, appWidgetManager, appWidgetId);
            fetchNewImageAndUpdate(context, appWidgetManager, appWidgetId);
        }
    }

    private void fetchAndUpdateImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        String apiSource = getApiSource(context);
        Log.d(TAG, "Fetching image from API source: " + apiSource + " for category: " + category);
        if (API_WAIFU_PICS.equals(apiSource) || API_WAIFU_PICS_NSFW.equals(apiSource)) {
            fetchWaifuPicsImage(context, appWidgetManager, appWidgetIds, category);
        } else if (API_NEKOBOT.equals(apiSource)) {
            fetchNekoBotImage(context, appWidgetManager, appWidgetIds, category);
        } else if (API_WAIFU_IM.equals(apiSource)) {
            fetchWaifuImImage(context, appWidgetManager, appWidgetIds, category);
        }
    }

    private void fetchNewImageAndUpdate(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String category = getImageCategory(context);
        String apiSource = getApiSource(context);

        if (API_WAIFU_PICS.equals(apiSource) || API_WAIFU_PICS_NSFW.equals(apiSource)) {
            fetchWaifuPicsImage(context, appWidgetManager, new int[]{appWidgetId}, category);
        } else if (API_NEKOBOT.equals(apiSource)) {
            fetchNekoBotImage(context, appWidgetManager, new int[]{appWidgetId}, category);
        } else if (API_WAIFU_IM.equals(apiSource)) {
            fetchWaifuImImage(context, appWidgetManager, new int[]{appWidgetId}, category);
        }
    }

    private void fetchWaifuImImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        Log.d(TAG, "Fetching WaifuIM image for category: " + category);
        ApiService apiService = RetrofitClient.getApiServiceWaifuIm();
        apiService.getWaifuImImage(category, byteSizeConstraintForWaifuIm).enqueue(new Callback<WaifuImResponse>() {
            @Override
            public void onResponse(@NonNull Call<WaifuImResponse> call, @NonNull Response<WaifuImResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().getImageUrl();
                    for (int appWidgetId : appWidgetIds) {
                        updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                    }
                } else {
                    String imageUrl = getLastImageUrl(context);
                    if (imageUrl != null) {
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error! Showing last image.", Toast.LENGTH_SHORT).show());
                        for (int appWidgetId : appWidgetIds) {
                            updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error!", Toast.LENGTH_SHORT).show());
                        handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image");
                    }
                }
                sendUpdateFinishedBroadcast(context);
            }

            @Override
            public void onFailure(@NonNull Call<WaifuImResponse> call, @NonNull Throwable throwable) {
                handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image:" + throwable);
            }
        });
    }

    private void fetchNekoBotImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        Log.d(TAG, "Fetching NekoBot image for category: " + category);
        ApiService apiService = RetrofitClient.getApiServiceNekoBot();
        apiService.getNekoBotImage(category).enqueue(new Callback<NekoBotImageResponse>() {
            @Override
            public void onResponse(@NonNull Call<NekoBotImageResponse> call, @NonNull Response<NekoBotImageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().getMessage();
                    for (int appWidgetId : appWidgetIds) {
                        updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                    }
                } else {
                    String imageUrl = getLastImageUrl(context);
                    if (imageUrl != null) {
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error! Showing last image.", Toast.LENGTH_SHORT).show());
                        for (int appWidgetId : appWidgetIds) {
                            updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error!", Toast.LENGTH_SHORT).show());
                        handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: ");
                    }
                }
                sendUpdateFinishedBroadcast(context);
            }

            @Override
            public void onFailure(@NonNull Call<NekoBotImageResponse> call, @NonNull Throwable throwable) {
                handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: " + throwable);
            }
        });
    }

    private void fetchWaifuPicsImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        Log.d(TAG, "Fetching WaifuPics image for category: " + category);
        ApiService apiService = RetrofitClient.getApiServiceAnimeImages();
        boolean nsfw = getNSFWSwitchState(context);

        Log.d(TAG, "NSFW state: " + nsfw);
        if (nsfw) {

            apiService.getNsfwImageWaifuPics(category).enqueue(new Callback<WaifuPicsImageResponse>() {
                @Override
                public void onResponse(@NonNull Call<WaifuPicsImageResponse> call, @NonNull Response<WaifuPicsImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().getUrl();
                        for (int appWidgetId : appWidgetIds) {
                            updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                        }
                    } else {
                        String imageUrl = getLastImageUrl(context);
                        if (imageUrl != null) {
                            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error! Showing last image.", Toast.LENGTH_SHORT).show());
                            for (int appWidgetId : appWidgetIds) {
                                updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                            }
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error!", Toast.LENGTH_SHORT).show());
                            handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: ");
                        }
                    }
                    sendUpdateFinishedBroadcast(context);
                }

                @Override
                public void onFailure(@NonNull Call<WaifuPicsImageResponse> call, @NonNull Throwable throwable) {
                    handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: " + throwable);
                }
            });

        } else {
            apiService.getSfwImageWaifuPics(category).enqueue(new Callback<WaifuPicsImageResponse>() {
                @Override
                public void onResponse(@NonNull Call<WaifuPicsImageResponse> call, @NonNull Response<WaifuPicsImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().getUrl();
                        for (int appWidgetId : appWidgetIds) {
                            updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                        }
                    } else {
                        String imageUrl = getLastImageUrl(context);
                        if (imageUrl != null) {
                            for (int appWidgetId : appWidgetIds) {
                                updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                            }
                        } else {
                            handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: ");
                        }
                    }
                    sendUpdateFinishedBroadcast(context);
                }

                @Override
                public void onFailure(@NonNull Call<WaifuPicsImageResponse> call, @NonNull Throwable throwable) {
                    handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: " + throwable);
                }
            });
        }

    }

    private void updateWidgetWithImage(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String imageUrl) {
        if (imageUrl != null) {
            loadImage(context, appWidgetManager, appWidgetId, imageUrl);
            saveLastImageUrl(context, imageUrl);
        } else {
            showPlaceholderImage(context, appWidgetManager, appWidgetId);
        }
    }

    private void loadImage(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String imageUrl) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_image_widget);

        new Thread(() -> {
            try {
                Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(500, 1000)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                views.setImageViewBitmap(R.id.widget_image_view, resource);
                                hideProgressBar(context);
                                setupUpdateTime(context, views);
                                setupWidgetButtons(context, views, imageUrl);
                                appWidgetManager.updateAppWidget(appWidgetId, views);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });


            } catch (Exception e) {
                Log.e(TAG, "loadImage: " + e);
                int[] appWidgetIds = new int[]{appWidgetId};
                handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: " + e.getMessage());
            }
        }).start();

    }

    private void setupUpdateTime(Context context, RemoteViews views) {
        long updateInterval = getUpdateInterval(context);
        String formattedTime;
        if (updateInterval > 0) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long nextUpdateTime = prefs.getLong(PREF_ANIME_IMAGE_LAST_UPDATE_TIME, System.currentTimeMillis()) + updateInterval;
            formattedTime = formatTime(nextUpdateTime);
        } else {
            formattedTime = "off";
        }
        views.setTextViewText(R.id.next_update_time, formattedTime);
    }

    private void handleApiFailure(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String errorMessage) {
        for (int appWidgetId_ : appWidgetIds) {
            showErrorDrawable(context, appWidgetManager, appWidgetId_, errorMessage);
        }
        sendUpdateFinishedBroadcast(context);
    }

    private void showErrorDrawable(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String errorMessage) {
        Log.e(TAG, "showErrorDrawable: " + errorMessage);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_image_widget);
        views.setImageViewResource(R.id.widget_image_view, R.drawable.ic_error);
        setupWidgetButtons(context, views, null);
        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.d(TAG, "showErrorDrawable: Error drawable set and widget updated");
    }

    private void sendUpdateFinishedBroadcast(Context context) {
        Intent finishedIntent = new Intent(context, AnimeImageWidget.class);
        finishedIntent.setAction(ACTION_UPDATE_FINISHED);
        context.sendBroadcast(finishedIntent);
    }

    private void setupWidgetButtons(Context context, RemoteViews views, String imageUrl) {
        Log.d(TAG, "Setting up widget buttons. Image URL: " + (imageUrl != null ? imageUrl : "null"));
        // Set up refresh button
        Intent refreshIntent = new Intent(context, AnimeImageWidget.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_container, refreshPendingIntent);

        // Set up download button
        Intent downloadIntent = new Intent(context, AnimeImageWidget.class);
        downloadIntent.setAction(ACTION_DOWNLOAD);
        downloadIntent.putExtra("image_url", imageUrl);
        PendingIntent downloadPendingIntent = PendingIntent.getBroadcast(context, 1, downloadIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_download_container, downloadPendingIntent);
    }

    private void showPlaceholderImage(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_image_widget);
        views.setImageViewResource(R.id.widget_image_view, R.drawable.ic_loading);
        setupWidgetButtons(context, views, null);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private boolean getNSFWSwitchState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NSFW_SWITCH, Context.MODE_PRIVATE);
        String mode = prefs.getString("mode", "OFF");
        boolean nsfwEnabled = mode.equals("ON");
        Log.d(TAG, "getNSFWSwitchState: NSFW switch state: " + nsfwEnabled);
        return nsfwEnabled;
    }

    private String getApiSource(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "getApiSource: " + prefs.getString(PREF_API_SOURCE, API_WAIFU_PICS));
        return prefs.getString(PREF_API_SOURCE, API_WAIFU_PICS); // Default to WaifuPics
    }

    private static void saveLastImageUrl(Context context, String imageUrl) {
        Log.d(TAG, "saveLastImageUrl: Saving last image URL: " + imageUrl);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_LAST_ANIME_IMAGE_URL, imageUrl).apply();
    }

    private static String getLastImageUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastImageUrl = prefs.getString(PREF_LAST_ANIME_IMAGE_URL, null);
        Log.d(TAG, "getLastImageUrl: Retrieved last image URL: " + lastImageUrl);
        return lastImageUrl;
    }

    private String getImageCategory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String category = prefs.getString(PREF_ANIME_IMAGE_CATEGORY, "waifu"); // Default to "waifu"
        Log.d(TAG, "getImageCategory: Retrieved image category: " + category);
        return category;
    }

    private long getUpdateInterval(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long interval = prefs.getLong(PREF_ANIME_IMAGE_UPDATE_INTERVAL, 3600000); // Default to 1 hour
        Log.d(TAG, "getUpdateIntervalMillis: Retrieved update interval: " + interval + " ms");
        return interval;
    }

    private String formatTime(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timeInMillis));
    }

    private void showProgressBar(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_image_widget);
        views.setViewVisibility(R.id.widget_refresh_button, View.GONE);
        views.setViewVisibility(R.id.widget_refresh_progress, View.VISIBLE);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    private void hideProgressBar(Context context) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_image_widget);
            views.setViewVisibility(R.id.widget_refresh_button, View.VISIBLE);
            views.setViewVisibility(R.id.widget_refresh_progress, View.GONE);
            appWidgetManager.updateAppWidget(thisWidget, views);
        }, 500);
    }

    public void showDownloadProgressBar(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_image_widget);
        views.setViewVisibility(R.id.widget_download_button, View.GONE);
        views.setViewVisibility(R.id.widget_download_progress, View.VISIBLE);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    public void hideDownloadProgressBar(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, AnimeImageWidget.class);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_image_widget);
        views.setViewVisibility(R.id.widget_download_button, View.VISIBLE);
        views.setViewVisibility(R.id.widget_download_progress, View.GONE);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }
}