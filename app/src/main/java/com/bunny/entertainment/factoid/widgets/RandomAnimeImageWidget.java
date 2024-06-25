package com.bunny.entertainment.factoid.widgets;

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
import com.bunny.entertainment.factoid.MainActivity;
import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.models.AnimeImageResponse;
import com.bunny.entertainment.factoid.models.NekoBotImageResponse;
import com.bunny.entertainment.factoid.models.WaifuImResponse;
import com.bunny.entertainment.factoid.networks.ApiService;
import com.bunny.entertainment.factoid.networks.NetworkMonitor;
import com.bunny.entertainment.factoid.networks.NetworkUtils;
import com.bunny.entertainment.factoid.networks.RetrofitClient;
import com.bunny.entertainment.factoid.utils.DownloadService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RandomAnimeImageWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.bunny.entertainment.factoid.widgets.ANIME_IMAGE_ACTION_REFRESH";
    public static final String ACTION_AUTO_UPDATE = "com.bunny.entertainment.factoid.widgets.ANIME_IMAGE_ACTION_AUTO_UPDATE";
    public static final String ACTION_UPDATE_FINISHED = "com.bunny.entertainment.factoid.widgets.ANIME_IMAGE_ACTION_UPDATE_FINISHED";
    public static final String PREFS_NAME = "com.bunny.entertainment.factoid.AnimeImageWidgetPrefs";
    public static final String PREF_UPDATE_INTERVAL = "anime_image_update_interval";
    public static final String PREF_IMAGE_CATEGORY = "anime_image_category";
    public static final String PREF_API_SOURCE = "anime_image_api_source";
    public static final String API_WAIFU_PICS = "waifu_pics";
    public static final String API_WAIFU_PICS_NSFW = "waifu_pics_nsfw";
    public static final String API_NEKOBOT_NSFW = "nekobot_nsfw";
    public static final String API_WAIFU_IM_NSFW = "waifu_im_nsfw";
    public static final String API_NEKOBOT = "nekobot";
    public static final String API_WAIFU_IM = "waifu_im";
    public static final String ACTION_DOWNLOAD = "com.bunny.entertainment.factoid.widgets.ANIME_IMAGE_ACTION_DOWNLOAD";
    public static final String ACTION_DOWNLOAD_COMPLETE = "com.bunny.entertainment.factoid.widgets.ACTION_DOWNLOAD_COMPLETE";
    public static final String ACTION_DOWNLOAD_FAILED = "com.bunny.entertainment.factoid.widgets.ACTION_DOWNLOAD_FAILED";
    public static final String PREF_LAST_IMAGE_URL = "last_image_url";
    private NetworkMonitor networkMonitor;
    private long lastUpdateTime = 0;
    String byteSizeConstraintForWaifuIm = "<=3145728";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.e("onEnabled", "onEnabled");
        updateWidgetAfterSleep(context);
        scheduleAutoUpdate(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleAutoUpdate(context);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (networkMonitor == null) {
            networkMonitor = NetworkMonitor.getInstance(context);
        }

        String action = intent.getAction();

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.e("ACTION_MY_PACKAGE_REPLACED", "ACTION_MY_PACKAGE_REPLACED");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }


        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction()) || Intent.ACTION_SCREEN_ON.equals(action)) {
            updateWidgetAfterSleep(context);
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            for (int appWidgetId : appWidgetIds) {
                long updateInterval = getUpdateIntervalMillis(context);
                if (updateInterval == 0) {
                    String lastImageUrl = getLastImageUrl(context);
                    if (lastImageUrl != null) {
                        updateWidgetWithImage(context, appWidgetManager, appWidgetId, lastImageUrl);
                    }
                }
            }
        }


        if (ACTION_REFRESH.equals(action) || ACTION_AUTO_UPDATE.equals(action)) {
            Log.e("ACTION_REFRESH", "ACTION_REFRESH");
            // if (!onEnabledCalled) {
            long currentTime = System.currentTimeMillis();
            long updateInterval = getUpdateIntervalMillis(context);
            if (currentTime - lastUpdateTime >= updateInterval) {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    showProgressBar(context);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> performUpdate(context), 250);
                } else {
                    Toast.makeText(context, "Ahh! No Internet connection.", Toast.LENGTH_SHORT).show();
                    networkMonitor.startMonitoring(() -> {
                        showProgressBar(context);
                        performUpdate(context);
                    });
                }
            }
            //   }
        } else if (ACTION_UPDATE_FINISHED.equals(action)) {
            hideProgressBar(context);
            lastUpdateTime = System.currentTimeMillis();
            networkMonitor.stopMonitoring();
        }

        if (ACTION_DOWNLOAD.equals(intent.getAction())) {
            String imageUrl = intent.getStringExtra("image_url");
            Log.d("RandomAnimeImageWidget", "Download action received for URL: " + imageUrl);
            if (imageUrl != null) {
                downloadImage(context, imageUrl);
            } else {
                Log.e("RandomAnimeImageWidget", "Image URL is null");
            }
        }

        if (ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            Log.d("RandomAnimeImageWidget", "Download complete, hiding progress bar");
            hideDownloadProgressBar(context);
            Toast.makeText(context, "Download complete", Toast.LENGTH_SHORT).show();
        } else if (ACTION_DOWNLOAD_FAILED.equals(action)) {
            Log.d("RandomAnimeImageWidget", "Download failed, hiding progress bar");
            hideDownloadProgressBar(context);
            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show();
        }

        // Register for download complete and failed broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(ACTION_DOWNLOAD_FAILED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(this, filter);
        }
    }

    private void updateWidgetAfterSleep(Context context) {
        Log.d("RandomAnimeImageWidget", "Updating widget after sleep");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    private void performUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        String category = getImageCategory(context);
        fetchAndUpdateImage(context, appWidgetManager, appWidgetIds, category);

        scheduleAutoUpdate(context);
    }

    private void fetchAndUpdateImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        String apiSource = getApiSource(context);
        if (API_WAIFU_PICS.equals(apiSource) || API_WAIFU_PICS_NSFW.equals(apiSource)) {
            fetchWaifuPicsImage(context, appWidgetManager, appWidgetIds, category);
        } else if (API_NEKOBOT.equals(apiSource)) {
            fetchNekoBotImage(context, appWidgetManager, appWidgetIds, category);
        } else if (API_WAIFU_IM.equals(apiSource)) {
            fetchWaifuImImage(context, appWidgetManager, appWidgetIds, category);
        }
    }

    private void fetchWaifuPicsImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        ApiService apiService = RetrofitClient.getApiServiceAnimeImages();
        boolean nsfw = getNSFWSwitchState(context);
        if (nsfw) {
            apiService.getNsfwImageWaifuPics(category).enqueue(new Callback<AnimeImageResponse>() {
                @Override
                public void onResponse(@NonNull Call<AnimeImageResponse> call, @NonNull Response<AnimeImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().getUrl();
                        for (int appWidgetId : appWidgetIds) {
                            updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                        }
                    }
                    sendUpdateFinishedBroadcast(context);
                }

                @Override
                public void onFailure(@NonNull Call<AnimeImageResponse> call, @NonNull Throwable t) {
                    handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: " + t.getMessage());

                }
            });
        } else {
            apiService.getSfwImageWaifuPics(category).enqueue(new Callback<AnimeImageResponse>() {
                @Override
                public void onResponse(@NonNull Call<AnimeImageResponse> call, @NonNull Response<AnimeImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().getUrl();
                        for (int appWidgetId : appWidgetIds) {
                            updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                        }
                    }
                    sendUpdateFinishedBroadcast(context);
                }

                @Override
                public void onFailure(@NonNull Call<AnimeImageResponse> call, @NonNull Throwable t) {
                    handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: " + t.getMessage());

                }
            });
        }
    }

    private void fetchWaifuImImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        ApiService apiService = RetrofitClient.getApiServiceWaifuIm();
        apiService.getWaifuImImage(category, byteSizeConstraintForWaifuIm).enqueue(new Callback<WaifuImResponse>() {
            @Override
            public void onResponse(@NonNull Call<WaifuImResponse> call, @NonNull Response<WaifuImResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().getImageUrl();
                    for (int appWidgetId : appWidgetIds) {
                        updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                    }
                }
                sendUpdateFinishedBroadcast(context);
            }

            @Override
            public void onFailure(@NonNull Call<WaifuImResponse> call, @NonNull Throwable t) {
                handleApiFailure(context, appWidgetManager, appWidgetIds, "Failed to fetch image: " + t.getMessage());
            }
        });
    }

    private void fetchNekoBotImage(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String category) {
        ApiService apiService = RetrofitClient.getApiServiceNekoBot();
        apiService.getNekoBotImage(category).enqueue(new Callback<NekoBotImageResponse>() {
            @Override
            public void onResponse(@NonNull Call<NekoBotImageResponse> call, @NonNull Response<NekoBotImageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String imageUrl = response.body().getMessage();
                    for (int appWidgetId : appWidgetIds) {
                        updateWidgetWithImage(context, appWidgetManager, appWidgetId, imageUrl);
                    }
                }
                sendUpdateFinishedBroadcast(context);
            }

            @Override
            public void onFailure(@NonNull Call<NekoBotImageResponse> call, @NonNull Throwable t) {
                Log.e("RandomAnimeImageWidget", "Failed to fetch image", t);
                sendUpdateFinishedBroadcast(context);
            }
        });
    }

    public static void setApiSource(Context context, String apiSource) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_API_SOURCE, apiSource).apply();
    }

    private String getApiSource(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_API_SOURCE, API_WAIFU_PICS); // Default to WaifuPics
    }

    private void sendUpdateFinishedBroadcast(Context context) {
        Intent finishedIntent = new Intent(context, RandomAnimeImageWidget.class);
        finishedIntent.setAction(ACTION_UPDATE_FINISHED);
        context.sendBroadcast(finishedIntent);
    }

    private void showProgressBar(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_image_widget);
        views.setViewVisibility(R.id.widget_refresh_button, View.GONE);
        views.setViewVisibility(R.id.widget_refresh_progress, View.VISIBLE);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    private void hideProgressBar(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_image_widget);
        views.setViewVisibility(R.id.widget_refresh_button, View.VISIBLE);
        views.setViewVisibility(R.id.widget_refresh_progress, View.GONE);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    public void showDownloadProgressBar(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_image_widget);
        views.setViewVisibility(R.id.widget_download_button, View.GONE);
        views.setViewVisibility(R.id.widget_download_progress, View.VISIBLE);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    public void hideDownloadProgressBar(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RandomAnimeImageWidget.class);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_image_widget);
        views.setViewVisibility(R.id.widget_download_button, View.VISIBLE);
        views.setViewVisibility(R.id.widget_download_progress, View.GONE);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String lastImageUrl = getLastImageUrl(context);
        if (lastImageUrl != null) {
            updateWidgetWithImage(context, appWidgetManager, appWidgetId, lastImageUrl);
        } else {
            showPlaceholderImage(context, appWidgetManager, appWidgetId);
            fetchNewImageAndUpdate(context, appWidgetManager, appWidgetId);
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

    private void showPlaceholderImage(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_image_widget);
        views.setImageViewResource(R.id.widget_image_view, R.drawable.ic_loading);
        setupWidgetButtons(context, views, null);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    private void loadImageWithRetry(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String imageUrl, int retryCount) {
        if (retryCount == -1 || retryCount == 1) {
            showProgressBar(context);
        }
        if (retryCount > 3) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                fetchNewImageAndRetry(context, appWidgetManager, appWidgetId, retryCount + 1);
            }
            return;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_image_widget);

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
                                setupWidgetButtons(context, views, imageUrl);
                                appWidgetManager.updateAppWidget(appWidgetId, views);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                // Handle this case if needed
                            }
                        });

            } catch (Exception e) {
                handleApiFailure(context, appWidgetManager, appWidgetId, "Failed to fetch image: " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                loadImageWithRetry(context, appWidgetManager, appWidgetId, imageUrl, retryCount + 1);
            }
        }).start();
    }

    private void setupWidgetButtons(Context context, RemoteViews views, String imageUrl) {
        // Set up refresh button
        Intent refreshIntent = new Intent(context, RandomAnimeImageWidget.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);

        // Set up download button
        Intent downloadIntent = new Intent(context, RandomAnimeImageWidget.class);
        downloadIntent.setAction(ACTION_DOWNLOAD);
        downloadIntent.putExtra("image_url", imageUrl);
        PendingIntent downloadPendingIntent = PendingIntent.getBroadcast(context, 1, downloadIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_download_button, downloadPendingIntent);
    }

    private void fetchNewImageAndRetry(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int retryCount) {
        String category = getImageCategory(context);
        String apiSource = getApiSource(context);

        ApiService apiService;
        if (API_WAIFU_PICS.equals(apiSource)) {
            apiService = RetrofitClient.getApiServiceAnimeImages();
            apiService.getSfwImageWaifuPics(category).enqueue(new Callback<AnimeImageResponse>() {
                @Override
                public void onResponse(@NonNull Call<AnimeImageResponse> call, @NonNull Response<AnimeImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String newImageUrl = response.body().getUrl();
                        loadImageWithRetry(context, appWidgetManager, appWidgetId, newImageUrl, retryCount);
                    } else {
                        Log.e("RandomAnimeImageWidget", "Failed to fetch new image URL");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AnimeImageResponse> call, @NonNull Throwable t) {
                    Log.e("RandomAnimeImageWidget", "Error fetching new image URL", t);
                    handleApiFailure(context, appWidgetManager, appWidgetId, "Failed to fetch image: " + t.getMessage());
                }
            });
        } else if (API_NEKOBOT.equals(apiSource)) {
            apiService = RetrofitClient.getApiServiceNekoBot();
            apiService.getNekoBotImage(category).enqueue(new Callback<NekoBotImageResponse>() {
                @Override
                public void onResponse(@NonNull Call<NekoBotImageResponse> call, @NonNull Response<NekoBotImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String newImageUrl = response.body().getMessage();
                        loadImageWithRetry(context, appWidgetManager, appWidgetId, newImageUrl, retryCount);
                    } else {
                        Log.e("RandomAnimeImageWidget", "Failed to fetch new image URL");
                    }
                    sendUpdateFinishedBroadcast(context);
                }

                @Override
                public void onFailure(@NonNull Call<NekoBotImageResponse> call, @NonNull Throwable t) {
                    handleApiFailure(context, appWidgetManager, appWidgetId, "Failed to fetch image: " + t.getMessage());
                }
            });
        } else if (API_WAIFU_IM.equals(apiSource)) {
            apiService = RetrofitClient.getApiServiceWaifuIm();
            apiService.getWaifuImImage(category, byteSizeConstraintForWaifuIm).enqueue(new Callback<WaifuImResponse>() {
                @Override
                public void onResponse(@NonNull Call<WaifuImResponse> call, @NonNull Response<WaifuImResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String newImageUrl = response.body().getImageUrl();
                        loadImageWithRetry(context, appWidgetManager, appWidgetId, newImageUrl, retryCount);
                    } else {
                        Log.e("RandomAnimeImageWidget", "Failed to fetch new image URL");
                    }
                    sendUpdateFinishedBroadcast(context);
                }

                @Override
                public void onFailure(@NonNull Call<WaifuImResponse> call, @NonNull Throwable t) {
                    handleApiFailure(context, appWidgetManager, appWidgetId, "Failed to fetch image: " + t.getMessage());
                }
            });
        }
    }

    private void updateWidgetWithImage(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String imageUrl) {
        if (imageUrl != null) {
            loadImageWithRetry(context, appWidgetManager, appWidgetId, imageUrl, 0);
            saveLastImageUrl(context, imageUrl);
        } else {
            showPlaceholderImage(context, appWidgetManager, appWidgetId);
        }
    }

    private void scheduleAutoUpdate(Context context) {
        long intervalMillis = getUpdateIntervalMillis(context);
        if (intervalMillis == 0) {
            // Auto-update is disabled, so don't schedule anything
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RandomAnimeImageWidget.class);
        intent.setAction(ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent);
            } else {
                // Fall back to inexact alarm
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent);
                Log.w("RandomAnimeImageWidget", "Exact alarms not allowed, using inexact alarm");
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent);
        }
    }

    public static void setUpdateInterval(Context context, long intervalMillis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_UPDATE_INTERVAL, intervalMillis).apply();
    }

    private long getUpdateIntervalMillis(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(PREF_UPDATE_INTERVAL, 3600000); // Default to 1 hour
    }

    public static void setImageCategory(Context context, String category) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_IMAGE_CATEGORY, category).apply();
    }

    private String getImageCategory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_IMAGE_CATEGORY, "waifu"); // Default to "waifu"
    }

    private void downloadImage(Context context, String imageUrl) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            Intent intent = new Intent(context, DownloadService.class);
            intent.putExtra("image_url", imageUrl);

            context.startForegroundService(intent);
            showDownloadProgressBar(context);
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Ahh! No Internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleApiFailure(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String errorMessage) {
        Log.e("RandomAnimeImageWidget", errorMessage);
        for (int appWidgetId : appWidgetIds) {
            showErrorDrawable(context, appWidgetManager, appWidgetId);
        }
        sendUpdateFinishedBroadcast(context);
    }

    private void handleApiFailure(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String errorMessage) {
        handleApiFailure(context, appWidgetManager, new int[]{appWidgetId}, errorMessage);
    }

    private void showErrorDrawable(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_anime_image_widget);
        views.setImageViewResource(R.id.widget_image_view, R.drawable.ic_error);
        setupWidgetButtons(context, views, null);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void saveLastImageUrl(Context context, String imageUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_LAST_IMAGE_URL, imageUrl).apply();
    }

    private static String getLastImageUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_LAST_IMAGE_URL, null);
    }

    private boolean getNSFWSwitchState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NSFW_SWITCH, Context.MODE_PRIVATE);
        String mode = prefs.getString("mode", "OFF");
        return mode.equals("ON");
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        try {
            context.unregisterReceiver(this);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered, ignore
        }
    }
}