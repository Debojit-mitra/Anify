package com.bunny.entertainment.factoid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

    private static final long[] INTERVALS = {
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
    private static final int SEEKBAR_STEPS = INTERVALS.length;
    private ActivityResultLauncher<Intent> alarmPermissionLauncher;
    private AppUpdater appUpdater;
    public AppUpdater.UpdateReceiver updateReceiver;
    private Spinner categorySpinner, apiSourceSpinner;
    private static final String[] API_SOURCES = {"Waifu.pics", "NekoBot"};


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
        setupCategorySpinner();

        setupSeekBar();

        if (isFirstTime()) {
            showNotificationPermissionDialog();
            showPermissionRequiredDialog();
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
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = INTERVALS[seekBar.getProgress()];
                RandomFactsWidget.setUpdateInterval(MainActivity.this, intervalMillis);
                scheduleNextUpdate();
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
                scheduleNextAnimeUpdate();
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
        if (intervalMillis < 60 * 60 * 1000) {
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
        if (intervalMillis < 60 * 60 * 1000) {
            int minutes = (int) (intervalMillis / (60 * 1000));
            String minutesTxt = "Anime facts update interval: " + minutes + " minutes";
            animeIntervalTextView.setText(minutesTxt);
        } else {
            int hours = (int) (intervalMillis / (60 * 60 * 1000));
            String hoursTxt = "Anime facts update interval: " + hours + " hours";
            animeIntervalTextView.setText(hoursTxt);
        }
    }

    private long getUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomFactsWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomFactsWidget.PREF_UPDATE_INTERVAL, INTERVALS[4]); // Default to 1 hour
    }

    private long getAnimeUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeFactsWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomAnimeFactsWidget.PREF_UPDATE_INTERVAL, INTERVALS[4]); // Default to 1 hour
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
        if (currentIntervalMillis == 0) {
            currentIntervalMillis = 3600000; // Default to 1 hour if no interval is set
            RandomFactsWidget.setUpdateInterval(this, currentIntervalMillis);
        }
        int progress = millisToProgress(currentIntervalMillis);
        intervalSeekBar.setMax(SEEKBAR_STEPS - 1);
        intervalSeekBar.setProgress(progress);
        intervalSeekBar.setEnabled(true);
        updateIntervalText(currentIntervalMillis);
    }

    private void setupAnimeSeekBar() {
        long currentIntervalMillis = getAnimeUpdateIntervalMillis();
        if (currentIntervalMillis == 0) {
            currentIntervalMillis = 3600000; // Default to 1 hour if no interval is set
            RandomAnimeFactsWidget.setUpdateInterval(this, currentIntervalMillis);
        }
        int progress = millisToProgress(currentIntervalMillis);
        animeIntervalSeekBar.setMax(SEEKBAR_STEPS - 1);
        animeIntervalSeekBar.setProgress(progress);
        animeIntervalSeekBar.setEnabled(true);
        updateAnimeIntervalText(currentIntervalMillis);
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

        String currentApiSource = getApiSource();
        int index = currentApiSource.equals(RandomAnimeImageWidget.API_NEKOBOT) ? 1 : 0;
        apiSourceSpinner.setSelection(index);

        apiSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newApiSource = position == 1 ? RandomAnimeImageWidget.API_NEKOBOT : RandomAnimeImageWidget.API_WAIFU_PICS;
                RandomAnimeImageWidget.setApiSource(MainActivity.this, newApiSource);
                setupCategorySpinner(); // Update category options
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupCategorySpinner() {
        String[] categories;
        String currentApiSource = getApiSource();
        if (RandomAnimeImageWidget.API_NEKOBOT.equals(currentApiSource)) {
            categories = new String[]{"neko", "kemonomimi", "kanna", "coffee", "food"};
        } else {
            categories = new String[]{"waifu", "neko", "shinobu", "megumin", "awoo", "nom", "happy", "wink", "cringe"};
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                RandomAnimeImageWidget.setImageCategory(MainActivity.this, selectedCategory);
                scheduleNextImageUpdate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set initial selection
        String currentCategory = getImageCategory();
        int index = Arrays.asList(categories).indexOf(currentCategory);
        if (index != -1) {
            categorySpinner.setSelection(index);
        } else {
            // If the current category is not in the new list, select the first category
            categorySpinner.setSelection(0);
            RandomAnimeImageWidget.setImageCategory(this, categories[0]);
        }
    }

    private String getApiSource() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(RandomAnimeImageWidget.PREF_API_SOURCE, RandomAnimeImageWidget.API_WAIFU_PICS);
    }

    private void setupImageIntervalSeekBar() {
        animeImageIntervalSeekBar.setMax(SEEKBAR_STEPS - 1);
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
                scheduleNextImageUpdate();
            }
        });

        // Set initial progress
        long currentIntervalMillis = getImageUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        animeImageIntervalSeekBar.setProgress(progress);
        updateImageIntervalText(currentIntervalMillis);
    }

    private void updateImageIntervalText(long intervalMillis) {
        if (intervalMillis < 60 * 60 * 1000) {
            int minutes = (int) (intervalMillis / (60 * 1000));
            String minutesTxt = "Anime image update interval: " + minutes + " minutes";
            animeImageIntervalTextView.setText(minutesTxt);
        } else {
            int hours = (int) (intervalMillis / (60 * 60 * 1000));
            String hoursTxt = "Anime image update interval: " + hours + " hours";
            animeImageIntervalTextView.setText(hoursTxt);
        }
    }

    private long getImageUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomAnimeImageWidget.PREF_UPDATE_INTERVAL, INTERVALS[4]); // Default to 1 hour
    }

    private String getImageCategory() {
        SharedPreferences prefs = getSharedPreferences(RandomAnimeImageWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(RandomAnimeImageWidget.PREF_IMAGE_CATEGORY, "waifu");
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

    @Override
    protected void onResume() {
        super.onResume();
        checkAlarmPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }
}