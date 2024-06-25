package com.bunny.entertainment.factoid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.bunny.entertainment.factoid.widgets.RandomAnimeFactsWidget;
import com.bunny.entertainment.factoid.widgets.RandomAnimeImageWidget;
import com.bunny.entertainment.factoid.widgets.RandomFactsWidget;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public SeekBar intervalSeekBar, animeIntervalSeekBar, animeImageIntervalSeekBar, cacheRemovalIntervalSeekBar;
    private TextView intervalTextView, animeIntervalTextView, animeImageIntervalTextView, cacheRemovalIntervalTextView;
    public TextView allowTextView;
    private static final int REQUEST_SCHEDULE_EXACT_ALARM = 1001;
    public static final int REQUEST_INSTALL_PACKAGES = 1002;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1003;
    public static final String PREFS_NAME = "com.bunny.entertainment.factoid.AnimeImageWidgetPrefs";
    public static final String PREFS_NSFW_SWITCH = "nsfw_switch";
    private static final String PREF_VERSION_CODE_KEY = "version_code";
    private static final String DOESNT_EXIST = null;
    private Spinner apiSourceSpinner, categorySpinner;
    private String currentApiSource, currentCategory, lastWaifuPicsCategory, lastWaifuPicsNSFCategory, lastNekoBotCategory, lastNekoBotNSFWCategory, lastWaifuImCategory, lastWaifuImNSFWCategory;
    private MaterialSwitch switch_nsfw;
    private static final long[] INTERVALS = {
            0L,                 // Off
            5 * 60 * 1000L,    // 5 minutes
            10 * 60 * 1000L,   // 10 minutes
            20 * 60 * 1000L,   // 20 minutes
            30 * 60 * 1000L,   // 30 minutes
            60 * 60 * 1000L,   // 1 hour
            2 * 60 * 60 * 1000L,   // 2 hours
            3 * 60 * 60 * 1000L,   // 3 hours
            4 * 60 * 60 * 1000L,   // 4 hours
            5 * 60 * 60 * 1000L,   // 5 hours
            6 * 60 * 60 * 1000L,   // 6 hours
            8 * 60 * 60 * 1000L,   // 8 hours
            10 * 60 * 60 * 1000L,  // 10 hours
            12 * 60 * 60 * 1000L   // 12 hours
    };
    private static final String PREF_CACHE_REMOVAL_INTERVAL = "cache_removal_interval";
    private static final long DEFAULT_CACHE_REMOVAL_INTERVAL = 7 * 24 * 60 * 60 * 1000; // 7 days
    private static final long[] CACHE_INTERVALS = {
            0L,                  // Off
            24 * 60 * 60 * 1000L,    // 1 day
            2 * 24 * 60 * 60 * 1000L,   // 2 days
            3 * 24 * 60 * 60 * 1000L,   // 3 days
            5 * 24 * 60 * 60 * 1000L,   // 5 days
            7 * 24 * 60 * 60 * 1000L,   // 7 days
            14 * 24 * 60 * 60 * 1000L,  // 14 days
            30 * 24 * 60 * 60 * 1000L   // 30 days
    };
    private ActivityResultLauncher<Intent> alarmPermissionLauncher;
    private AppUpdater appUpdater;
    public AppUpdater.UpdateReceiver updateReceiver;
    private static final String[] API_SOURCES = {"Waifu.pics", "NekoBot", "waifu.im"};
    private Vibrator mVibrator;


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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

        appUpdater = new AppUpdater(this);

        intervalSeekBar = findViewById(R.id.intervalSeekBar);
        intervalTextView = findViewById(R.id.intervalTextView);
        allowTextView = findViewById(R.id.allowTextView);
        animeIntervalSeekBar = findViewById(R.id.animeIntervalSeekBar);
        animeIntervalTextView = findViewById(R.id.animeIntervalTextView);
        animeImageIntervalSeekBar = findViewById(R.id.animeImageIntervalSeekBar);
        animeImageIntervalTextView = findViewById(R.id.animeImageIntervalTextView);
        categorySpinner = findViewById(R.id.category_spinner);
        apiSourceSpinner = findViewById(R.id.api_source_spinner);
        switch_nsfw = findViewById(R.id.switch_nsfw);
        cacheRemovalIntervalSeekBar = findViewById(R.id.cacheRemovalIntervalSeekBar);
        cacheRemovalIntervalTextView = findViewById(R.id.cacheRemovalIntervalTextView);


        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setupApiSourceSpinner();
        if (getNSFWSwitchState()) {
            switch_nsfw.setChecked(true);
        }
        setupSeekBar(true);
        setupCacheRemovalSeekBar();

        if (isFirstTime() || getAppLastVersion(MainActivity.this) != null && !getCurrentVersionName(MainActivity.this).equals(getAppLastVersion(MainActivity.this))) {
            showNotificationPermissionDialog();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                showPermissionRequiredDialog();
            }
        } else {
            checkAlarmPermission();
        }

        allowTextView.setOnClickListener(v -> showPermissionRequiredDialog());

        alarmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkAlarmPermission()
        );

        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = INTERVALS[progress];
                updateIntervalText(intervalMillis);
                vibrate(20);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                long intervalMillis = INTERVALS[seekBar.getProgress()];
                RandomFactsWidget.setUpdateInterval(MainActivity.this, intervalMillis);
                if (intervalMillis > 0) {
                    scheduleNextUpdate();
                }
            }
        });

        animeIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = INTERVALS[progress];
                updateAnimeIntervalText(intervalMillis);
                vibrate(20);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = INTERVALS[seekBar.getProgress()];
                RandomAnimeFactsWidget.setUpdateInterval(MainActivity.this, intervalMillis);
                if (intervalMillis > 0) {
                    scheduleNextAnimeUpdate();
                }
            }
        });

        animeImageIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = INTERVALS[progress];
                updateImageIntervalText(intervalMillis);
                vibrate(20);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = INTERVALS[seekBar.getProgress()];
                RandomAnimeImageWidget.setUpdateInterval(MainActivity.this, intervalMillis);
                if (intervalMillis > 0) {
                    scheduleNextImageUpdate();
                }
            }
        });

        updateReceiver = new AppUpdater.UpdateReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, new IntentFilter(AppUpdater.INSTALL_ACTION), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, new IntentFilter(AppUpdater.INSTALL_ACTION));
        }

        apiSourceSpinner.setOnLongClickListener(v -> {
            switch_nsfw.setVisibility(View.VISIBLE);
            return true;
        });

        appUpdater.checkForUpdates(false);
    }

    private boolean hasInstallPermission() {
        return getPackageManager().canRequestPackageInstalls();
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

    private void updateIntervalText(long intervalMillis) {
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

    private void updateAnimeIntervalText(long intervalMillis) {
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

    private void updateImageIntervalText(long intervalMillis) {
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

    private long getUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomFactsWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomFactsWidget.PREF_UPDATE_INTERVAL, INTERVALS[5]); // Default to 1hr
    }

    private long getAnimeUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeFactsWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomAnimeFactsWidget.PREF_UPDATE_INTERVAL, INTERVALS[5]); // Default to 1hr
    }

    private int millisToProgress(long millis) {
        for (int i = 0; i < INTERVALS.length; i++) {
            if (millis <= INTERVALS[i]) {
                return i;
            }
        }
        return 5;
    }

    private void scheduleNextUpdate() {
        Intent intent = new Intent(this, RandomFactsWidget.class);
        intent.setAction(RandomFactsWidget.ACTION_AUTO_UPDATE);
        sendBroadcast(intent);
    }

    private void scheduleNextAnimeUpdate() {
        Intent intent = new Intent(this, RandomAnimeFactsWidget.class);
        intent.setAction(RandomAnimeFactsWidget.ACTION_AUTO_UPDATE);
        sendBroadcast(intent);
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!RandomAnimeFactsWidget.canScheduleExactAlarms(this)) {
                disableAutoUpdate();
                disableAnimeAutoUpdate();
                disableImageAutoUpdate();
                showAllowTextView();
            } else {
                hideAllowTextView();
                enableAutoUpdates(true); // Pass true to indicate permission was just granted
            }
        } else {
            hideAllowTextView();
            enableAutoUpdates(false); // Pass false for older Android versions
        }
    }
    private void enableAutoUpdates(boolean justGranted) {
        setupSeekBar(justGranted);
        setupAnimeSeekBar(justGranted);
        setupImageIntervalSeekBar(justGranted);
    }

    private void setupSeekBar(boolean justGranted) {
        long currentIntervalMillis = justGranted ? INTERVALS[5] : getUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        intervalSeekBar.setMax(INTERVALS.length - 1);
        intervalSeekBar.setProgress(progress);
        intervalSeekBar.setEnabled(true);
        updateIntervalText(currentIntervalMillis);
        if (justGranted) {
            RandomFactsWidget.setUpdateInterval(this, currentIntervalMillis);
        }
    }

    private void setupAnimeSeekBar(boolean justGranted) {
        long currentIntervalMillis = justGranted ? INTERVALS[5] : getAnimeUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        animeIntervalSeekBar.setMax(INTERVALS.length - 1);
        animeIntervalSeekBar.setProgress(progress);
        animeIntervalSeekBar.setEnabled(true);
        updateAnimeIntervalText(currentIntervalMillis);
        if (justGranted) {
            RandomAnimeFactsWidget.setUpdateInterval(this, currentIntervalMillis);
        }
    }

    private void setupImageIntervalSeekBar(boolean justGranted) {
        long currentIntervalMillis = justGranted ? INTERVALS[5] : getImageUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        animeImageIntervalSeekBar.setMax(INTERVALS.length - 1);
        animeImageIntervalSeekBar.setProgress(progress);
        animeImageIntervalSeekBar.setEnabled(true);
        updateImageIntervalText(currentIntervalMillis);
        if (justGranted) {
            RandomAnimeImageWidget.setUpdateInterval(this, currentIntervalMillis);
        }
    }

    private void disableAutoUpdate() {
        RandomFactsWidget.setUpdateInterval(this, 0); // 0 means no auto-update
        intervalSeekBar.setEnabled(false);
        String updateInterval = "Facts update interval: Off (No permission)";
        intervalTextView.setText(updateInterval);
    }

    private void disableAnimeAutoUpdate() {
        RandomAnimeFactsWidget.setUpdateInterval(this, 0); // 0 means no auto-update
        animeIntervalSeekBar.setEnabled(false);
        String updateInterval = "Anime facts update interval: Off (No permission)";
        animeIntervalTextView.setText(updateInterval);
    }

    private void disableImageAutoUpdate() {
        RandomAnimeImageWidget.setUpdateInterval(this, 0); // 0 means no auto-update
        animeImageIntervalSeekBar.setEnabled(false);
        String updateInterval = "Anime Images update interval: Off (No permission)";
        animeImageIntervalTextView.setText(updateInterval);
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
                Toast.makeText(this, "Notification permission denied! Image downloads will not be notified1", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showPermissionRequiredDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permission Required")
                .setMessage("Exact alarm permission is required for automatic updates. Without it, you'll need to manually update the widget.")
                .setPositiveButton("Allow", (dialog, which) -> {
                    Intent intent = RandomFactsWidget.getAlarmPermissionSettingsIntent(this);
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

    private void showAllowTextView() {
        TextView allowTextView = findViewById(R.id.allowTextView);
        allowTextView.setVisibility(View.VISIBLE);
    }

    private void hideAllowTextView() {
        TextView allowTextView = findViewById(R.id.allowTextView);
        allowTextView.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCHEDULE_EXACT_ALARM) {
            if (RandomFactsWidget.canScheduleExactAlarms(this)) {
                Toast.makeText(this, "Exact alarms allowed", Toast.LENGTH_SHORT).show();
                checkAlarmPermission(); // This will set up the seekbars with default values
            } else {
                Toast.makeText(this, "Exact alarms not allowed, widget may update less precisely", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (hasInstallPermission()) {
                Toast.makeText(this, "Permission to install packages granted", Toast.LENGTH_SHORT).show();
                appUpdater.checkAndDownloadPendingUpdate();
            } else {
                Toast.makeText(this, "Permission to install packages is required for updates", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupApiSourceSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, API_SOURCES);
        apiSourceSpinner.setAdapter(adapter);
        int index;
        currentApiSource = getApiSource();
        switch (currentApiSource) {
            case RandomAnimeImageWidget.API_NEKOBOT:
                index = 1;
                showNSFWSwitch();
                break;
            case RandomAnimeImageWidget.API_WAIFU_IM:
                index = 2;
                showNSFWSwitch();
                break;
            case RandomAnimeImageWidget.API_WAIFU_PICS:
            default:
                index = 0;
                showNSFWSwitch();
                break;
        }
        apiSourceSpinner.setSelection(index);

        // Initialize last selected categories
        lastWaifuPicsCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_PICS);
        lastWaifuPicsNSFCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_PICS_NSFW);
        lastNekoBotCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_NEKOBOT);
        lastNekoBotNSFWCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_NEKOBOT_NSFW);
        lastWaifuImCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_IM);
        lastWaifuImNSFWCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_IM_NSFW);


        apiSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newApiSource = position == 1 ? RandomAnimeImageWidget.API_NEKOBOT : RandomAnimeImageWidget.API_WAIFU_PICS;
                if (position == 0) {
                    newApiSource = RandomAnimeImageWidget.API_WAIFU_PICS;
                    showNSFWSwitch();
                } else if (position == 1) {
                    newApiSource = RandomAnimeImageWidget.API_NEKOBOT;
                    showNSFWSwitch();
                } else if (position == 2) {
                    newApiSource = RandomAnimeImageWidget.API_WAIFU_IM;
                    showNSFWSwitch();
                }
                if (!newApiSource.equals(currentApiSource)) {
                    // Save the current category for the previous API source
                    switch (currentApiSource) {
                        case RandomAnimeImageWidget.API_NEKOBOT:
                            lastNekoBotCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_NEKOBOT_NSFW:
                            lastNekoBotNSFWCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_IM:
                            lastWaifuImCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_IM_NSFW:
                            lastWaifuImNSFWCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_PICS_NSFW:
                            lastWaifuPicsNSFCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_PICS:
                        default:
                            lastWaifuPicsCategory = currentCategory;
                            break;
                    }

                    currentApiSource = newApiSource;
                    RandomAnimeImageWidget.setApiSource(MainActivity.this, newApiSource);
                    setupCategorySpinner();
                    scheduleNextImageUpdate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        setupCategorySpinner();
    }

    private void showNSFWSwitch() {
        switch_nsfw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NSFW_SWITCH, Context.MODE_PRIVATE);
            if (isChecked) {
                switch_nsfw.setVisibility(View.VISIBLE);
                prefs.edit().putString("mode", "ON").apply();
                vibrate(100);
            } else {
                switch_nsfw.setVisibility(View.GONE);
                prefs.edit().putString("mode", "OFF").apply();
                vibrate(100);
            }
            setupCategorySpinner();
            scheduleNextImageUpdate();
        });
    }

    private boolean getNSFWSwitchState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NSFW_SWITCH, Context.MODE_PRIVATE);
        String mode = prefs.getString("mode", "OFF");
        return mode.equals("ON");
    }

    private void setupCategorySpinner() {
        String[] categories;
        if (RandomAnimeImageWidget.API_NEKOBOT.equals(currentApiSource)) {
            if (getNSFWSwitchState()) {
                categories = new String[]{"hmidriff", "hentai", "holo", "hneko", "hkitsune", "thigh", "hthigh", "paizuri", "tentacle", "hboobs"};
                currentCategory = lastNekoBotNSFWCategory;
            } else {
                categories = new String[]{"neko", "kemonomimi", "kanna", "coffee", "food"};
                currentCategory = lastNekoBotCategory;
            }
        } else if (RandomAnimeImageWidget.API_WAIFU_IM.equals(currentApiSource)) {
            if (getNSFWSwitchState()) {
                categories = new String[]{"ero", "ass", "hentai", "milf", "oral", "paizuri", "ecchi"};
                currentCategory = lastWaifuImNSFWCategory;
            } else {
                categories = new String[]{"waifu", "maid", "marin-kitagawa", "mori-calliope", "raiden-shogun", "oppai", "selfies", "uniform", "kamisato-ayaka"};
                currentCategory = lastWaifuImCategory;
            }
        } else {
            if (getNSFWSwitchState()) {
                categories = new String[]{"waifu", "neko", "trap", "blowjob"};
                currentCategory = lastWaifuPicsNSFCategory;
            } else {
                categories = new String[]{"waifu", "neko", "shinobu", "megumin", "awoo", "nom", "happy", "wink", "cringe"};
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

        RandomAnimeImageWidget.setImageCategory(this, currentCategory);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                if (!selectedCategory.equals(currentCategory)) {
                    currentCategory = selectedCategory;
                    switch (currentApiSource) {
                        case RandomAnimeImageWidget.API_NEKOBOT:
                            if (getNSFWSwitchState()) {
                                lastNekoBotNSFWCategory = currentCategory;
                            } else {
                                lastNekoBotCategory = currentCategory;
                            }
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_IM:
                            if (getNSFWSwitchState()) {
                                lastWaifuImNSFWCategory = currentCategory;
                            } else {
                                lastWaifuImCategory = currentCategory;
                            }
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_PICS:
                        default:
                            if (getNSFWSwitchState()) {
                                lastWaifuPicsNSFCategory = currentCategory;
                            } else {
                                lastWaifuPicsCategory = currentCategory;
                            }
                            break;
                    }
                    RandomAnimeImageWidget.setImageCategory(MainActivity.this, selectedCategory);
                    scheduleNextImageUpdate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    private String getApiSource() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(RandomAnimeImageWidget.PREF_API_SOURCE, RandomAnimeImageWidget.API_WAIFU_PICS);
    }

    private long getImageUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomAnimeImageWidget.PREF_UPDATE_INTERVAL, INTERVALS[5]); // Default to 1 hour
    }

    private void scheduleNextImageUpdate() {
        Intent intent = new Intent(this, RandomAnimeImageWidget.class);
        intent.setAction(RandomAnimeImageWidget.ACTION_AUTO_UPDATE);
        sendBroadcast(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. Download notifications will not be shown.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getAppLastVersion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_VERSION_CODE_KEY, null);
    }

    private static String getCurrentVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return DOESNT_EXIST; // In case of an error, return a non-existent version code
        }
    }

    private String getLastSelectedCategory(String apiSource) {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        String key = apiSource + "_last_category";
        String defaultCategory;

        switch (apiSource) {
            case RandomAnimeImageWidget.API_NEKOBOT:
                defaultCategory = "neko";
                break;
            case RandomAnimeImageWidget.API_NEKOBOT_NSFW:
                defaultCategory = "ass";
                break;
            case RandomAnimeImageWidget.API_WAIFU_IM_NSFW:
                defaultCategory = "ero";
                break;
            default:
                defaultCategory = "waifu";
        }

        return prefs.getString(key, defaultCategory);
    }

    private void saveLastSelectedCategory(String apiSource, String category) {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        String key = apiSource + "_last_category";
        prefs.edit().putString(key, category).apply();
    }

    private void saveLastCategories() {
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_PICS_NSFW, lastWaifuPicsNSFCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_PICS, lastWaifuPicsCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_NEKOBOT_NSFW, lastNekoBotNSFWCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_NEKOBOT, lastNekoBotCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_IM, lastWaifuImCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_IM_NSFW, lastWaifuImNSFWCategory);
    }

    private void vibrate(int vibrateTime) {
        if (mVibrator != null) {
            mVibrator.vibrate(VibrationEffect.createOneShot(vibrateTime, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void setupCacheRemovalSeekBar() {
        long currentIntervalMillis = getCacheRemovalInterval();
        int progress = millisToCacheProgress(currentIntervalMillis);
        cacheRemovalIntervalSeekBar.setMax(CACHE_INTERVALS.length - 1);
        cacheRemovalIntervalSeekBar.setProgress(progress);
        updateCacheRemovalIntervalText(currentIntervalMillis);

        cacheRemovalIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = CACHE_INTERVALS[progress];
                updateCacheRemovalIntervalText(intervalMillis);
                vibrate(20);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = CACHE_INTERVALS[seekBar.getProgress()];
                setCacheRemovalInterval(intervalMillis);
                scheduleCacheRemoval();
            }
        });
    }

    private int millisToCacheProgress(long millis) {
        for (int i = 0; i < CACHE_INTERVALS.length; i++) {
            if (millis <= CACHE_INTERVALS[i]) {
                return i;
            }
        }
        return CACHE_INTERVALS.length - 1;
    }

    private void updateCacheRemovalIntervalText(long intervalMillis) {
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

    private long getCacheRemovalInterval() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(PREF_CACHE_REMOVAL_INTERVAL, DEFAULT_CACHE_REMOVAL_INTERVAL);
    }

    private void setCacheRemovalInterval(long intervalMillis) {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_CACHE_REMOVAL_INTERVAL, intervalMillis).apply();
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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAlarmPermission();
    }

    @Override
    protected void onDestroy() {
        saveLastCategories();
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }

    @Override
    protected void onStop() {
        saveLastCategories();
        super.onStop();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

}