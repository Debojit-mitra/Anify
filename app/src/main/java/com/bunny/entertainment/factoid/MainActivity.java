package com.bunny.entertainment.factoid;

import static com.bunny.entertainment.factoid.utils.Constants.ACTION_HOMESCREEN_FROM_APP;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_RESET_ALARM;
import static com.bunny.entertainment.factoid.utils.Constants.ACTION_STOP_AUTO_UPDATE;
import static com.bunny.entertainment.factoid.utils.Constants.API_NEKOBOT;
import static com.bunny.entertainment.factoid.utils.Constants.API_NEKOBOT_NSFW;
import static com.bunny.entertainment.factoid.utils.Constants.API_WAIFU_IM;
import static com.bunny.entertainment.factoid.utils.Constants.API_WAIFU_IM_NSFW;
import static com.bunny.entertainment.factoid.utils.Constants.API_WAIFU_PICS;
import static com.bunny.entertainment.factoid.utils.Constants.API_WAIFU_PICS_NSFW;
import static com.bunny.entertainment.factoid.utils.Constants.CACHE_INTERVALS;
import static com.bunny.entertainment.factoid.utils.Constants.DEFAULT_CACHE_REMOVAL_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.DEFAULT_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.INTERVALS;
import static com.bunny.entertainment.factoid.utils.Constants.PREFS_NAME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_FACT_UPDATE_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_IMAGE_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_IMAGE_UPDATE_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_API_SOURCE;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_CACHE_REMOVAL_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_FACT_UPDATE_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_NEKOBOT_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_NSFW_NEKOBOT_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_NSFW_WAIFU_IM_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_NSFW_WAIFU_PICS_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_WAIFU_IM_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_WAIFU_PICS_CATEGORY;
import static com.bunny.entertainment.factoid.utils.Constants.REQUEST_INSTALL_PACKAGES;
import static com.bunny.entertainment.factoid.utils.Constants.REQUEST_NOTIFICATION_PERMISSION;
import static com.bunny.entertainment.factoid.utils.Constants.WAIFU_IT_API_KEY;
import static com.bunny.entertainment.factoid.utils.Constants.categoryNsfwNekobot;
import static com.bunny.entertainment.factoid.utils.Constants.categoryNsfwWaifuIm;
import static com.bunny.entertainment.factoid.utils.Constants.categoryNsfwWaifuPics;
import static com.bunny.entertainment.factoid.utils.Constants.categorySfwNekobot;
import static com.bunny.entertainment.factoid.utils.Constants.categorySfwWaifuIm;
import static com.bunny.entertainment.factoid.utils.Constants.categorySfwWaifuPics;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bunny.entertainment.factoid.updater.AppUpdater;
import com.bunny.entertainment.factoid.utils.CacheRemovalWorker;
import com.bunny.entertainment.factoid.utils.Constants;
import com.bunny.entertainment.factoid.widgets.AnimeFactsWidget;
import com.bunny.entertainment.factoid.widgets.AnimeImageWidget;
import com.bunny.entertainment.factoid.widgets.FactsWidget;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public SeekBar intervalSeekBar, animeIntervalSeekBar, animeImageIntervalSeekBar, cacheRemovalIntervalSeekBar;
    private TextView intervalTextView, animeIntervalTextView, animeImageIntervalTextView, cacheRemovalIntervalTextView, get_api_btn;
    private Spinner apiSourceSpinner, categorySpinner;
    private MaterialSwitch switch_nsfw;
    private String currentApiSource, currentCategory, lastWaifuPicsCategory,
            lastWaifuPicsNSFCategory, lastNekoBotCategory, lastNekoBotNSFWCategory,
            lastWaifuImCategory, lastWaifuImNSFWCategory;
    private TextInputEditText edittext_anime_facts_api_key;
    private MaterialButton btn_save_api_key;
    private AppUpdater appUpdater;
    public AppUpdater.UpdateReceiver updateReceiver;
    private ActivityResultLauncher<Intent> alarmPermissionLauncher;
    private Vibrator mVibrator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initialize();


    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void initialize() {
        appUpdater = new AppUpdater(MainActivity.this);

        intervalSeekBar = findViewById(R.id.intervalSeekBar);
        intervalTextView = findViewById(R.id.intervalTextView);
        animeIntervalSeekBar = findViewById(R.id.animeIntervalSeekBar);
        animeIntervalTextView = findViewById(R.id.animeIntervalTextView);
        animeImageIntervalSeekBar = findViewById(R.id.animeImageIntervalSeekBar);
        animeImageIntervalTextView = findViewById(R.id.animeImageIntervalTextView);
        categorySpinner = findViewById(R.id.category_spinner);
        apiSourceSpinner = findViewById(R.id.api_source_spinner);
        switch_nsfw = findViewById(R.id.switch_nsfw);
        cacheRemovalIntervalSeekBar = findViewById(R.id.cacheRemovalIntervalSeekBar);
        cacheRemovalIntervalTextView = findViewById(R.id.cacheRemovalIntervalTextView);
        edittext_anime_facts_api_key = findViewById(R.id.edittext_anime_facts_api_key);
        btn_save_api_key = findViewById(R.id.btn_save_api_key);
        get_api_btn = findViewById(R.id.get_api_btn);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setUpApiSource();

        initializeBarBtnSwh();

        alarmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (getCanScheduleExactAlarms(this)) {
                        Toast.makeText(this, "Exact alarms allowed", Toast.LENGTH_SHORT).show();

                        // Set default intervals for all widgets
                        setUpdateInterval(PREF_FACT_UPDATE_INTERVAL, DEFAULT_INTERVAL);
                        setUpdateInterval(PREF_ANIME_FACT_UPDATE_INTERVAL, DEFAULT_INTERVAL);
                        setUpdateInterval(PREF_ANIME_IMAGE_UPDATE_INTERVAL, DEFAULT_INTERVAL);
                        scheduleNextFactUpdate();
                        scheduleNextAnimeFactUpdate();
                        scheduleNextImageUpdate();

                        checkAlarmPermission();
                    } else {
                        Toast.makeText(this, "Exact alarms not allowed, widget may update less precisely", Toast.LENGTH_LONG).show();
                    }
                }
        );

        updateReceiver = new AppUpdater.UpdateReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, new IntentFilter(AppUpdater.INSTALL_ACTION), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, new IntentFilter(AppUpdater.INSTALL_ACTION));
        }

        appUpdater.checkForUpdates(false);


    }

    private void initializeBarBtnSwh() {
        if (getNSFWSwitchState()) {
            switch_nsfw.setChecked(true);
            switch_nsfw.setVisibility(View.VISIBLE);
        }

        switch_nsfw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NSFW_SWITCH, Context.MODE_PRIVATE);
            if (isChecked) {
                switch_nsfw.setVisibility(View.VISIBLE);
                prefs.edit().putString("mode", "ON").apply();
                vibrate(40);
                Log.d(TAG, "NSFW switch turned ON");
            } else {
                switch_nsfw.setVisibility(View.GONE);
                prefs.edit().putString("mode", "OFF").apply();
                vibrate(40);
                Log.d(TAG, "NSFW switch turned OFF");
            }
            setupCategorySpinner();
            scheduleNextImageUpdate();
        });

        if (isFirstTime()) {
            showNotificationPermissionDialog();
            //exact alarms permissions above A13
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                showExactAlarmPermissionRequiredDialog();
            }
        } else {
            checkAlarmPermission();
        }

        setupCacheRemovalSeekBar();

        apiSourceSpinner.setOnLongClickListener(v -> {
            switch_nsfw.setVisibility(View.VISIBLE);
            return true;
        });

        if (getWaifuItAPIKey() != null) {
            edittext_anime_facts_api_key.setText(getWaifuItAPIKey());
        }

        btn_save_api_key.setOnClickListener(view -> {
            String apiKey = Objects.requireNonNull(edittext_anime_facts_api_key.getText()).toString();
            if (TextUtils.isEmpty(apiKey)) {
                Toast.makeText(MainActivity.this, "Paste an api key first!", Toast.LENGTH_SHORT).show();
            } else if (getWaifuItAPIKey() == null || !apiKey.equals(getWaifuItAPIKey())) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(WAIFU_IT_API_KEY, apiKey).apply();
                if (edittext_anime_facts_api_key.hasFocus()) {
                    edittext_anime_facts_api_key.clearFocus();
                    try {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(edittext_anime_facts_api_key.getWindowToken(), 0);
                    } catch (Exception e) {
                        Log.e("btn_save_api_key", e.toString());
                    }
                }
                showApiAddedDialog();
            }
        });

        get_api_btn.setOnClickListener(view -> new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("To get the API key follow the steps:")
                .setMessage("1. Press Open\n2. waifu.it page will open, click dashboard\n3. Login with discord and allow the permissions\n4. Copy the generated key and paste here")
                .setPositiveButton("Open", (dialogInterface, i) -> {
                    String url = "https://waifu.it/";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                })
                .setNegativeButton("No Need", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show());

    }

    private void showApiAddedDialog() {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("API added successfully!")
                .setMessage("If you have added the widget already, remove and add it again.")
                .setNegativeButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupCacheRemovalSeekBar() {
        long currentIntervalMillis = getCacheRemovalInterval();
        int progress = getMillisToCacheProgress(currentIntervalMillis);
        cacheRemovalIntervalSeekBar.setMax(CACHE_INTERVALS.length - 1);
        cacheRemovalIntervalSeekBar.setProgress(progress);
        updateCacheRemovalIntervalText(currentIntervalMillis);

        cacheRemovalIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = CACHE_INTERVALS[progress];
                updateCacheRemovalIntervalText(intervalMillis);
                vibrate(20);
                Log.d(TAG, "onProgressChanged: progress=" + progress + ", intervalMillis=" + intervalMillis);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = CACHE_INTERVALS[seekBar.getProgress()];
                setCacheRemovalInterval(intervalMillis);
                scheduleCacheRemoval();
                Log.d(TAG, "onStopTrackingTouch: intervalMillis=" + intervalMillis);
            }
        });
        // Store the original drawables
        Drawable originalProgressDrawable = cacheRemovalIntervalSeekBar.getProgressDrawable();
        cacheRemovalIntervalSeekBar.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // User started interacting with the SeekBar
                    cacheRemovalIntervalSeekBar.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.custom_seekbar_progress));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // User stopped interacting with the SeekBar
                    cacheRemovalIntervalSeekBar.setProgressDrawable(originalProgressDrawable); // This will reset to the default style
                    break;
            }

            return false;
        });
    }

    private void scheduleCacheRemoval() {
        long intervalMillis = getCacheRemovalInterval();
        WorkManager.getInstance(this).cancelAllWorkByTag("cache_removal");
        if (intervalMillis > 0) {
            PeriodicWorkRequest cacheRemovalWork = new PeriodicWorkRequest.Builder(
                    CacheRemovalWorker.class,
                    intervalMillis,
                    TimeUnit.MILLISECONDS
            )
                    .addTag("cache_removal")
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "cache_removal",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    cacheRemovalWork
            );
            Log.d(TAG, "scheduleCacheRemoval: Work scheduled with interval " + intervalMillis + " ms");
        } else {
            Log.d(TAG, "scheduleCacheRemoval: Cache removal disabled");
        }
    }

    private void updateCacheRemovalIntervalText(long intervalMillis) {
        Log.d(TAG, "updateCacheRemovalIntervalText: intervalMillis=" + intervalMillis);
        if (intervalMillis == 0) {
            cacheRemovalIntervalTextView.setText(getString(R.string.cache_removal_off));
        } else {
            int days = (int) (intervalMillis / (24 * 60 * 60 * 1000));
            if (days == 1) {
                cacheRemovalIntervalTextView.setText(R.string.cache_removal_interval_1_day);
            } else {
                String daysTxt = "Cache removal interval: " + days + " days";
                cacheRemovalIntervalTextView.setText(daysTxt);
            }
        }
    }

    private void showExactAlarmPermissionRequiredDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permission Required")
                .setMessage("Exact alarm permission is required for automatic updates. Without it, you'll need to manually update the widget.")
                .setPositiveButton("Allow", (dialog, which) -> {
                    Intent intent = getAlarmPermissionSettingsIntent(this);
                    if (intent != null) {
                        alarmPermissionLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Deny", (dialog, which) -> {
                    Toast.makeText(this, "Auto-update disabled due to missing permission.", Toast.LENGTH_SHORT).show();
                    checkAlarmPermission();
                })
                .setCancelable(false)
                .show();
    }

    private void showNotificationPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Notification Permission Required")
                .setMessage("Notification permission is required to show download notifications. Please allow notification permission.")
                .setPositiveButton("Allow", (dialog, which) -> requestNotificationPermission())
                .setNegativeButton("Deny", (dialog, which) -> Toast.makeText(this, "Notification permission denied. Download notifications will not be shown.", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            } else {
                Toast.makeText(this, "Notification permission denied! Image downloads will not be notified!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TextView allowTextView = findViewById(R.id.allowTextView);
            if (!getCanScheduleExactAlarms(this)) {
                //disable update seekbars
                //fact seekbar
                setUpdateInterval(PREF_FACT_UPDATE_INTERVAL, 0); // 0 means no auto-update
                intervalSeekBar.setEnabled(false);
                String updateInterval = "Facts update interval: Off (No permission)";
                intervalTextView.setText(updateInterval);
                //anime fact seekbar
                setUpdateInterval(PREF_ANIME_FACT_UPDATE_INTERVAL, 0); // 0 means no auto-update
                animeIntervalSeekBar.setEnabled(false);
                String updateInterval1 = "Anime facts update interval: Off (No permission)";
                animeIntervalTextView.setText(updateInterval1);
                //anime image seekbar
                setUpdateInterval(PREF_ANIME_IMAGE_UPDATE_INTERVAL, 0); // 0 means no auto-update
                animeImageIntervalSeekBar.setEnabled(false);
                String updateInterval2 = "Anime Images update interval: Off (No permission)";
                animeImageIntervalTextView.setText(updateInterval2);
                //showing allow permission textview
                allowTextView.setVisibility(View.VISIBLE);
                allowTextView.setOnClickListener(v -> showExactAlarmPermissionRequiredDialog());
            } else {
                allowTextView.setVisibility(View.GONE);
                setupFactSeekBar();
                setupAnimeFactSeekBar();
                setupAnimeImageIntervalSeekBar();
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupAnimeImageIntervalSeekBar() {
        long currentIntervalMillis = getUpdateIntervalMillis(PREF_ANIME_IMAGE_UPDATE_INTERVAL);
        int progress = getMillisToProgress(currentIntervalMillis);
        animeImageIntervalSeekBar.setMax(INTERVALS.length - 1);
        animeImageIntervalSeekBar.setProgress(progress);
        animeImageIntervalSeekBar.setEnabled(true);
        updateAnimeImageIntervalText(currentIntervalMillis);

        animeImageIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = INTERVALS[progress];
                updateAnimeImageIntervalText(intervalMillis);
                vibrate(20);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = INTERVALS[seekBar.getProgress()];
                setUpdateInterval(PREF_ANIME_IMAGE_UPDATE_INTERVAL, intervalMillis);
                if (intervalMillis > 0) {
                    scheduleNextImageUpdate();
                } else {
                    stopNextImageUpdate();
                }
            }
        });
        // Store the original drawables
        Drawable originalProgressDrawable = animeImageIntervalSeekBar.getProgressDrawable();
        animeImageIntervalSeekBar.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // User started interacting with the SeekBar
                    animeImageIntervalSeekBar.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.custom_seekbar_progress));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // User stopped interacting with the SeekBar
                    animeImageIntervalSeekBar.setProgressDrawable(originalProgressDrawable); // This will reset to the default style
                    break;
            }

            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupAnimeFactSeekBar() {
        long currentIntervalMillis = getUpdateIntervalMillis(PREF_ANIME_FACT_UPDATE_INTERVAL);
        int progress = getMillisToProgress(currentIntervalMillis);
        animeIntervalSeekBar.setMax(INTERVALS.length - 1);
        animeIntervalSeekBar.setProgress(progress);
        animeIntervalSeekBar.setEnabled(true);
        updateAnimeFactIntervalText(currentIntervalMillis);

        animeIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = INTERVALS[progress];
                updateAnimeFactIntervalText(intervalMillis);
                vibrate(20);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = INTERVALS[seekBar.getProgress()];
                setUpdateInterval(PREF_ANIME_FACT_UPDATE_INTERVAL, intervalMillis);
                if (intervalMillis > 0) {
                    scheduleNextAnimeFactUpdate();
                    updateAnimeFactsWidgets();
                }
            }
        });
        // Store the original drawables
        Drawable originalProgressDrawable = animeIntervalSeekBar.getProgressDrawable();
        animeIntervalSeekBar.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // User started interacting with the SeekBar
                    animeIntervalSeekBar.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.custom_seekbar_progress));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // User stopped interacting with the SeekBar
                    animeIntervalSeekBar.setProgressDrawable(originalProgressDrawable); // This will reset to the default style
                    break;
            }

            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupFactSeekBar() {
        long currentIntervalMillis = getUpdateIntervalMillis(PREF_FACT_UPDATE_INTERVAL);
        Log.e("currentIntervalMillis", String.valueOf(currentIntervalMillis));
        int progress = getMillisToProgress(currentIntervalMillis);
        intervalSeekBar.setMax(INTERVALS.length - 1);
        intervalSeekBar.setProgress(progress);
        intervalSeekBar.setEnabled(true);
        updateFactIntervalText(currentIntervalMillis);

        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = INTERVALS[progress];
                updateFactIntervalText(intervalMillis);
                vibrate(20);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = INTERVALS[seekBar.getProgress()];
                Log.e("setupFactSeekBar: intervalMillis", String.valueOf(intervalMillis));
                setUpdateInterval(PREF_FACT_UPDATE_INTERVAL, intervalMillis);
                if (intervalMillis > 0) {
                    scheduleNextFactUpdate();
                    updateFactsWidgets();
                }
            }
        });
        // Store the original drawables
        Drawable originalProgressDrawable = intervalSeekBar.getProgressDrawable();
        intervalSeekBar.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // User started interacting with the SeekBar
                    intervalSeekBar.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.custom_seekbar_progress));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // User stopped interacting with the SeekBar
                    intervalSeekBar.setProgressDrawable(originalProgressDrawable); // This will reset to the default style
                    break;
            }

            return false;
        });
    }

    private void updateFactsWidgets() {
        Intent updateIntent = new Intent(this, FactsWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), FactsWidget.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(updateIntent);
    }

    private void stopNextImageUpdate() {
        Intent intent = new Intent(this, AnimeImageWidget.class);
        intent.setAction(ACTION_STOP_AUTO_UPDATE);
        sendBroadcast(intent);
    }

    private void scheduleNextFactUpdate() {
        Intent intent = new Intent(this, FactsWidget.class);
        intent.setAction(ACTION_RESET_ALARM);
        sendBroadcast(intent);
    }

    private void scheduleNextAnimeFactUpdate() {
        Intent intent = new Intent(this, AnimeFactsWidget.class);
        intent.setAction(ACTION_RESET_ALARM);
        sendBroadcast(intent);
    }

    private void scheduleNextImageUpdate() {
        Log.d(TAG, "scheduleNextImageUpdate: Scheduling next update");
        Intent intent = new Intent(this, AnimeImageWidget.class);
        intent.setAction(ACTION_RESET_ALARM);
        sendBroadcast(intent);
    }

    private void updateAnimeFactsWidgets() {
        Intent updateIntent = new Intent(this, AnimeFactsWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), AnimeFactsWidget.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(updateIntent);
    }

    private void setUpApiSource() {
        Log.d(TAG, "setupApiSourceSpinner: Setting up API source spinner");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Constants.API_SOURCES);
        apiSourceSpinner.setAdapter(adapter);
        int index;
        currentApiSource = getApiSource();
        switch (currentApiSource) {
            case API_NEKOBOT:
                index = 1;
                break;
            case API_WAIFU_IM:
                index = 2;
                break;
            case API_WAIFU_PICS:
            default:
                index = 0;
                break;
        }
        apiSourceSpinner.setSelection(index);

        // Initialize last selected category for the source
        lastWaifuPicsCategory = getLastSelectedCategory(API_WAIFU_PICS);
        lastWaifuPicsNSFCategory = getLastSelectedCategory(API_WAIFU_PICS_NSFW);
        lastNekoBotCategory = getLastSelectedCategory(API_NEKOBOT);
        lastNekoBotNSFWCategory = getLastSelectedCategory(API_NEKOBOT_NSFW);
        lastWaifuImCategory = getLastSelectedCategory(API_WAIFU_IM);
        lastWaifuImNSFWCategory = getLastSelectedCategory(API_WAIFU_IM_NSFW);

        setupCategorySpinner();

        apiSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newApiSource = position == 1 ? API_NEKOBOT : API_WAIFU_PICS;
                if (position == 0) {
                    newApiSource = API_WAIFU_PICS;
                } else if (position == 1) {
                    newApiSource = API_NEKOBOT;
                } else if (position == 2) {
                    newApiSource = API_WAIFU_IM;
                }
                Log.d(TAG, "onItemSelected: New API source selected: " + newApiSource);

                if (!newApiSource.equals(currentApiSource)) {
                    // Save the current category for the previous API source
                    switch (currentApiSource) {
                        case API_NEKOBOT:
                            lastNekoBotCategory = currentCategory;
                            break;
                        case API_NEKOBOT_NSFW:
                            lastNekoBotNSFWCategory = currentCategory;
                            break;
                        case API_WAIFU_IM:
                            lastWaifuImCategory = currentCategory;
                            break;
                        case API_WAIFU_IM_NSFW:
                            lastWaifuImNSFWCategory = currentCategory;
                            break;
                        case API_WAIFU_PICS_NSFW:
                            lastWaifuPicsNSFCategory = currentCategory;
                            break;
                        case API_WAIFU_PICS:
                        default:
                            lastWaifuPicsCategory = currentCategory;
                            break;
                    }
                    currentApiSource = newApiSource;
                    setApiSource(MainActivity.this, newApiSource);
                    setupCategorySpinner();
                    scheduleNextImageUpdate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e(TAG, "onNothingSelected: No API source selected");
            }
        });
    }

    private void setupCategorySpinner() {
        Log.d(TAG, "setupCategorySpinner: Setting up category spinner for " + currentApiSource);
        String[] categories;
        if (API_NEKOBOT.equals(currentApiSource)) {
            if (getNSFWSwitchState()) {
                categories = categoryNsfwNekobot;
                currentCategory = lastNekoBotNSFWCategory;
            } else {
                categories = categorySfwNekobot;
                currentCategory = lastNekoBotCategory;
            }
        } else if (API_WAIFU_IM.equals(currentApiSource)) {
            if (getNSFWSwitchState()) {
                categories = categoryNsfwWaifuIm;
                currentCategory = lastWaifuImNSFWCategory;
            } else {
                categories = categorySfwWaifuIm;
                currentCategory = lastWaifuImCategory;
            }
        } else {
            if (getNSFWSwitchState()) {
                categories = categoryNsfwWaifuPics;
                currentCategory = lastWaifuPicsNSFCategory;
            } else {
                categories = categorySfwWaifuPics;
                currentCategory = lastWaifuPicsCategory;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        int index = Arrays.asList(categories).indexOf(currentCategory);
        if (index == -1) {
            index = 0;
            currentCategory = categories[0];
        }
        categorySpinner.setSelection(index);

        setImageCategory(MainActivity.this, currentCategory);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                Log.d(TAG, "onItemSelected: Category selected: " + selectedCategory);
                if (!selectedCategory.equals(currentCategory)) {
                    currentCategory = selectedCategory;
                    switch (currentApiSource) {
                        case API_NEKOBOT:
                            if (getNSFWSwitchState()) {
                                lastNekoBotNSFWCategory = currentCategory;
                                setLastImageCategory(PREF_LAST_NSFW_NEKOBOT_CATEGORY, currentCategory);
                            } else {
                                lastNekoBotCategory = currentCategory;
                                setLastImageCategory(PREF_LAST_NEKOBOT_CATEGORY, currentCategory);
                            }
                            break;
                        case API_WAIFU_IM:
                            if (getNSFWSwitchState()) {
                                lastWaifuImNSFWCategory = currentCategory;
                                setLastImageCategory(PREF_LAST_NSFW_WAIFU_IM_CATEGORY, currentCategory);
                            } else {
                                lastWaifuImCategory = currentCategory;
                                setLastImageCategory(PREF_LAST_WAIFU_IM_CATEGORY, currentCategory);
                            }
                            break;
                        case API_WAIFU_PICS:
                        default:
                            if (getNSFWSwitchState()) {
                                lastWaifuPicsNSFCategory = currentCategory;
                                setLastImageCategory(PREF_LAST_NSFW_WAIFU_PICS_CATEGORY, currentCategory);
                            } else {
                                lastWaifuPicsCategory = currentCategory;
                                setLastImageCategory(PREF_LAST_WAIFU_PICS_CATEGORY, currentCategory);
                            }
                            break;
                    }
                    Log.d(TAG, "onItemSelected: Category changed, updating settings");
                    setImageCategory(MainActivity.this, currentCategory);
                    scheduleNextImageUpdate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e(TAG, "onNothingSelected: No category selected");
            }
        });
    }


    private void updateFactIntervalText(long intervalMillis) {
        if (intervalMillis == 0) {
            String factsTxt = "Facts update interval: Off";
            intervalTextView.setText(factsTxt);
        } else if (intervalMillis < 60 * 60 * 1000) {
            int minutes = (int) (intervalMillis / (60 * 1000));
            String minutesTxt = "Facts update interval: " + minutes + " minutes";
            intervalTextView.setText(minutesTxt);
        } else {
            int hours = (int) (intervalMillis / (60 * 60 * 1000));
            String hoursTxt = "Facts update interval: " + hours + " hours";
            intervalTextView.setText(hoursTxt);
        }
    }

    private void updateAnimeFactIntervalText(long intervalMillis) {
        if (intervalMillis == 0) {
            String animeFacts = "Anime facts update interval: Off";
            animeIntervalTextView.setText(animeFacts);
        } else if (intervalMillis < 60 * 60 * 1000) {
            int minutes = (int) (intervalMillis / (60 * 1000));
            String minutesTxt = "Anime facts update interval: " + minutes + " minutes";
            animeIntervalTextView.setText(minutesTxt);
        } else {
            int hours = (int) (intervalMillis / (60 * 60 * 1000));
            String hoursTxt = "Anime facts update interval: " + hours + " hours";
            animeIntervalTextView.setText(hoursTxt);
        }
    }

    private void updateAnimeImageIntervalText(long intervalMillis) {
        if (intervalMillis == 0) {
            String animeImageTxt = "Anime image update interval: Off";
            animeImageIntervalTextView.setText(animeImageTxt);
        } else if (intervalMillis < 60 * 60 * 1000) {
            int minutes = (int) (intervalMillis / (60 * 1000));
            String minutesTxt = "Anime image update interval: " + minutes + " minutes";
            animeImageIntervalTextView.setText(minutesTxt);
        } else {
            int hours = (int) (intervalMillis / (60 * 60 * 1000));
            String hoursTxt = "Anime image update interval: " + hours + " hours";
            animeImageIntervalTextView.setText(hoursTxt);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (hasInstallPermission()) {
                Toast.makeText(this, "Permission to install packages granted", Toast.LENGTH_SHORT).show();
                appUpdater.checkAndDownloadPendingUpdate();
            } else {
                Toast.makeText(this, "Permission to install packages is required for updates", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasInstallPermission() {
        return getPackageManager().canRequestPackageInstalls();
    }

    //setters
    private void setCacheRemovalInterval(long intervalMillis) {
        Log.d(TAG, "setCacheRemovalInterval: " + intervalMillis);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_CACHE_REMOVAL_INTERVAL, intervalMillis).apply();
    }

    public void setUpdateInterval(String prefsName, long intervalMillis) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "Setting update interval to " + intervalMillis + " ms");
        prefs.edit().putLong(prefsName, intervalMillis).apply();
    }

    private void setLastImageCategory(String prefsName, String currentCategory) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "setLastImageCategory: Setting image category to " + currentCategory);
        prefs.edit().putString(prefsName, currentCategory).apply();
    }

    public static void setApiSource(Context context, String apiSource) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_API_SOURCE, apiSource).apply();
        Log.d(TAG, "setApiSource: " + apiSource);
    }

    //getters
    private int getMillisToCacheProgress(long millis) {
        for (int i = 0; i < CACHE_INTERVALS.length; i++) {
            if (millis <= CACHE_INTERVALS[i]) {
                Log.d(TAG, "millisToCacheProgress: millis=" + millis + ", progress=" + i);
                return i;
            }
        }
        Log.d(TAG, "millisToCacheProgress: millis=" + millis + ", progress=" + (CACHE_INTERVALS.length - 1));
        return CACHE_INTERVALS.length - 1;
    }

    private long getCacheRemovalInterval() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long interval = prefs.getLong(PREF_CACHE_REMOVAL_INTERVAL, DEFAULT_CACHE_REMOVAL_INTERVAL);
        Log.d(TAG, "getCacheRemovalInterval: " + interval);
        return interval;
    }

    private int getMillisToProgress(long millis) {
        for (int i = 0; i < INTERVALS.length; i++) {
            if (millis <= INTERVALS[i]) {
                return i;
            }
        }
        return 5;
    }

    private long getUpdateIntervalMillis(String prefName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(prefName, DEFAULT_INTERVAL); // Default to 1hr
    }

    public static boolean getCanScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            boolean canSchedule = alarmManager.canScheduleExactAlarms();
            Log.d(TAG, "Can schedule exact alarms: " + canSchedule);
            return canSchedule;
        }
        return true;
    }

    public static Intent getAlarmPermissionSettingsIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Getting alarm permission settings intent");
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + context.getPackageName()));
        }
        return null;
    }

    private String getLastSelectedCategory(String apiSource) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = apiSource + "_last_category";
        String defaultCategory;

        switch (apiSource) {
            case API_NEKOBOT:
                defaultCategory = "neko";
                break;
            case API_NEKOBOT_NSFW:
                defaultCategory = "ass";
                break;
            case API_WAIFU_IM_NSFW:
                defaultCategory = "ero";
                break;
            default:
                defaultCategory = "waifu";
        }

        String category = prefs.getString(key, defaultCategory);
        Log.d(TAG, "getLastSelectedCategory: apiSource=" + apiSource + ", category=" + category);
        return category;
    }

    private void saveLastSelectedCategory(String apiSource, String category) {
        Log.d(TAG, "saveLastSelectedCategory: apiSource=" + apiSource + ", category=" + category);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = apiSource + "_last_category";
        prefs.edit().putString(key, category).apply();
    }

    private void saveLastCategories() {
        Log.d(TAG, "saveLastCategories: Saving all last selected categories");
        saveLastSelectedCategory(API_WAIFU_PICS_NSFW, lastWaifuPicsNSFCategory);
        saveLastSelectedCategory(API_WAIFU_PICS, lastWaifuPicsCategory);
        saveLastSelectedCategory(API_NEKOBOT_NSFW, lastNekoBotNSFWCategory);
        saveLastSelectedCategory(API_NEKOBOT, lastNekoBotCategory);
        saveLastSelectedCategory(API_WAIFU_IM, lastWaifuImCategory);
        saveLastSelectedCategory(API_WAIFU_IM_NSFW, lastWaifuImNSFWCategory);
    }

    public static void setImageCategory(Context context, String category) {
        Log.d(TAG, "setImageCategory: Setting image category to " + category);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_ANIME_IMAGE_CATEGORY, category).apply();
    }

    private boolean getNSFWSwitchState() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NSFW_SWITCH, Context.MODE_PRIVATE);
        String mode = prefs.getString("mode", "OFF");
        Log.d(TAG, "getNSFWSwitchState: " + mode);
        return mode.equals("ON");
    }

    private String getApiSource() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String apiSource = prefs.getString(PREF_API_SOURCE, API_WAIFU_PICS);
        Log.d(TAG, "getApiSource: " + apiSource);
        return apiSource;
    }

    private String getWaifuItAPIKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(WAIFU_IT_API_KEY, null); // Default to 1hr
    }

    //methods
    private void vibrate(int vibrateTime) {
        Log.d(TAG, "vibrate: vibrateTime=" + vibrateTime);
        if (mVibrator != null) {
            mVibrator.vibrate(VibrationEffect.createOneShot(vibrateTime, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private boolean isFirstTime() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);
        if (isFirstTime) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isFirstTime", false);
            editor.apply();
        }
        return isFirstTime;
    }

    private void updateHomescreenImage() {
        Intent intent = new Intent(this, AnimeImageWidget.class);
        intent.setAction(ACTION_HOMESCREEN_FROM_APP);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAlarmPermission();
    }

    @Override
    protected void onDestroy() {
        //saveLastCategories();
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }

    @Override
    protected void onStop() {
        //saveLastCategories();
        super.onStop();
    }

    @Override
    protected void onPause() {
        updateHomescreenImage();
        saveLastCategories();
        super.onPause();
    }
}