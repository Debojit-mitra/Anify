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

import com.bunny.entertainment.factoid.updater.AppUpdater;
import com.bunny.entertainment.factoid.widgets.RandomAnimeFactsWidget;
import com.bunny.entertainment.factoid.widgets.RandomAnimeImageWidget;
import com.bunny.entertainment.factoid.widgets.RandomFactsWidget;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public SeekBar intervalSeekBar, animeIntervalSeekBar, animeImageIntervalSeekBar;
    private TextView intervalTextView, animeIntervalTextView, animeImageIntervalTextView;
    public TextView allowTextView;
    private static final int REQUEST_SCHEDULE_EXACT_ALARM = 1001;
    public static final int REQUEST_INSTALL_PACKAGES = 1002;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1003;
    public static final String PREFS_NAME = "com.bunny.entertainment.factoid.AnimeImageWidgetPrefs";
    private static final String PREF_VERSION_CODE_KEY = "version_code";
    private static final String DOESNT_EXIST = null;
    private Spinner apiSourceSpinner, categorySpinner;
    private String currentApiSource, currentCategory, lastWaifuPicsCategory, lastNekoBotCategory, lastWaifuImCategory;


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
    private ActivityResultLauncher<Intent> alarmPermissionLauncher;
    private AppUpdater appUpdater;
    public AppUpdater.UpdateReceiver updateReceiver;
    private static final String[] API_SOURCES = {"Waifu.pics", "NekoBot", "waifu.im"};


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

        setupApiSourceSpinner();


        setupSeekBar();

        if (isFirstTime() || getAppLastVersion(MainActivity.this) != null && !getCurrentVersionName(MainActivity.this).equals(getAppLastVersion(MainActivity.this))) {
            showNotificationPermissionDialog();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                showPermissionRequiredDialog();
            }
        } else {
            checkAlarmPermission();
        }

       /* //// this is temporary
        if () {
            showNotificationPermissionDialog();
            RandomAnimeImageWidget.setAppLastVersion(MainActivity.this);
        }*/

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
        return prefs.getLong(RandomFactsWidget.PREF_UPDATE_INTERVAL, INTERVALS[3]); // Default to 1hr
    }

    private long getAnimeUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeFactsWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomAnimeFactsWidget.PREF_UPDATE_INTERVAL, INTERVALS[3]); // Default to 1hr
    }

    private int millisToProgress(long millis) {
        for (int i = 0; i < INTERVALS.length; i++) {
            if (millis <= INTERVALS[i]) {
                return i;
            }
        }
        return INTERVALS.length - 1;
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
                setupSeekBar();
                setupAnimeSeekBar();
                setupImageIntervalSeekBar();
            }
        } else {
            hideAllowTextView();
            setupSeekBar();
            setupAnimeSeekBar();
            setupImageIntervalSeekBar();
        }
    }

    private void setupSeekBar() {
        long currentIntervalMillis = getUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        intervalSeekBar.setMax(INTERVALS.length - 1);
        intervalSeekBar.setProgress(progress);
        intervalSeekBar.setEnabled(true);
        updateIntervalText(currentIntervalMillis);
    }

    private void setupAnimeSeekBar() {
        long currentIntervalMillis = getAnimeUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        animeIntervalSeekBar.setMax(INTERVALS.length - 1);
        animeIntervalSeekBar.setProgress(progress);
        animeIntervalSeekBar.setEnabled(true);
        updateAnimeIntervalText(currentIntervalMillis);
    }

    private void setupImageIntervalSeekBar() {
        long currentIntervalMillis = getImageUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        animeImageIntervalSeekBar.setMax(INTERVALS.length - 1);
        animeImageIntervalSeekBar.setProgress(progress);
        animeImageIntervalSeekBar.setEnabled(true);
        updateImageIntervalText(currentIntervalMillis);
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
                    disableAutoUpdate();
                    showAllowTextView();
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
                break;
            case RandomAnimeImageWidget.API_WAIFU_IM:
                index = 2;
                break;
            case RandomAnimeImageWidget.API_WAIFU_PICS:
            default:
                index = 0;
                break;
        }
        apiSourceSpinner.setSelection(index);

        // Initialize last selected categories
        lastWaifuPicsCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_PICS);
        lastNekoBotCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_NEKOBOT);
        lastWaifuImCategory = getLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_IM);

        apiSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newApiSource = position == 1 ? RandomAnimeImageWidget.API_NEKOBOT : RandomAnimeImageWidget.API_WAIFU_PICS;
                if (position == 0) {
                    newApiSource = RandomAnimeImageWidget.API_WAIFU_PICS;
                } else if (position == 1) {
                    newApiSource = RandomAnimeImageWidget.API_NEKOBOT;
                } else if (position == 2) {
                    newApiSource = RandomAnimeImageWidget.API_WAIFU_IM;
                }
                if (!newApiSource.equals(currentApiSource)) {
                    // Save the current category for the previous API source
                    switch (currentApiSource) {
                        case RandomAnimeImageWidget.API_NEKOBOT:
                            lastNekoBotCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_IM:
                            lastWaifuImCategory = currentCategory;
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

    private void setupCategorySpinner() {
        String[] categories;
        if (RandomAnimeImageWidget.API_NEKOBOT.equals(currentApiSource)) {
            categories = new String[]{"neko", "kemonomimi", "kanna", "coffee", "food"};
            currentCategory = lastNekoBotCategory;
        } else if (RandomAnimeImageWidget.API_WAIFU_IM.equals(currentApiSource)) {
            categories = new String[]{"waifu", "maid", "marin-kitagawa", "mori-calliope", "raiden-shogun", "oppai", "selfies", "uniform", "kamisato-ayaka"};
            currentCategory = lastWaifuImCategory;
        } else {
            categories = new String[]{"waifu", "neko", "shinobu", "megumin", "awoo", "nom", "happy", "wink", "cringe"};
            currentCategory = lastWaifuPicsCategory;
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
                            lastNekoBotCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_IM:
                            lastWaifuImCategory = currentCategory;
                            break;
                        case RandomAnimeImageWidget.API_WAIFU_PICS:
                        default:
                            lastWaifuPicsCategory = currentCategory;
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
        return prefs.getLong(RandomAnimeImageWidget.PREF_UPDATE_INTERVAL, INTERVALS[3]); // Default to 1 hour
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
        return prefs.getString(PREF_VERSION_CODE_KEY, null); // Default to "waifu"
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
            case RandomAnimeImageWidget.API_WAIFU_PICS:
                defaultCategory = "waifu";
                break;
            case RandomAnimeImageWidget.API_NEKOBOT:
                defaultCategory = "neko";
                break;
            case RandomAnimeImageWidget.API_WAIFU_IM:
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

    @Override
    protected void onResume() {
        super.onResume();
        checkAlarmPermission();
    }

    @Override
    protected void onDestroy() {
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_PICS, lastWaifuPicsCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_NEKOBOT, lastNekoBotCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_IM, lastWaifuImCategory);
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }

    @Override
    protected void onStop() {
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_PICS, lastWaifuPicsCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_NEKOBOT, lastNekoBotCategory);
        saveLastSelectedCategory(RandomAnimeImageWidget.API_WAIFU_IM, lastWaifuImCategory);
        super.onStop();
    }
}