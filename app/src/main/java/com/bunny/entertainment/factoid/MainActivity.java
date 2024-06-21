package com.bunny.entertainment.factoid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bunny.entertainment.factoid.widgets.RandomFactsWidget;

public class MainActivity extends AppCompatActivity {

    private SeekBar intervalSeekBar;
    private TextView intervalTextView;
    private static final int REQUEST_SCHEDULE_EXACT_ALARM = 1001;
    private static final long MIN_INTERVAL = 5 * 60 * 1000L; // 5 minutes
    private static final long MAX_INTERVAL = 24 * 60 * 60 * 1000L; // 1 day
    private static final int SEEKBAR_STEPS = 24; // Number of steps in the SeekBar


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        intervalSeekBar = findViewById(R.id.intervalSeekBar);
        intervalTextView = findViewById(R.id.intervalTextView);

        // Set up SeekBar
        intervalSeekBar.setMax(SEEKBAR_STEPS - 1);
        long currentIntervalMillis = getUpdateIntervalMillis();
        int progress = millisToProgress(currentIntervalMillis);
        intervalSeekBar.setProgress(progress);

        updateIntervalText(currentIntervalMillis);

        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long intervalMillis = progressToMillis(progress);
                updateIntervalText(intervalMillis);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long intervalMillis = progressToMillis(seekBar.getProgress());
                RandomFactsWidget.setUpdateInterval(MainActivity.this, intervalMillis);
                scheduleNextUpdate();
            }
        });

        checkAlarmPermission();
    }

    private void updateIntervalText(long intervalMillis) {
        if (intervalMillis < 60 * 60 * 1000) {
            int minutes = (int) (intervalMillis / (60 * 1000));
            String minute = "Update interval: " + minutes + " minutes";
            intervalTextView.setText(minute);
        } else if (intervalMillis < 24 * 60 * 60 * 1000) {
            int hours = (int) (intervalMillis / (60 * 60 * 1000));
            int minutes = (int) ((intervalMillis % (60 * 60 * 1000)) / (60 * 1000)); // Calculate remaining minutes
            String updateIntervalText = "Update interval: " + hours + " hours " + minutes + " minutes";
            intervalTextView.setText(updateIntervalText);
        } else {
            intervalTextView.setText(getString(R.string.update_interval_24_hour));
        }
    }

    private long getUpdateIntervalMillis() {
        SharedPreferences prefs = getSharedPreferences(RandomFactsWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomFactsWidget.PREF_UPDATE_INTERVAL, 3600000); // Default to 1 hour
    }

    private int millisToProgress(long millis) {
        double normalized = (Math.log(millis) - Math.log(MIN_INTERVAL)) / (Math.log(MAX_INTERVAL) - Math.log(MIN_INTERVAL));
        return (int) Math.round(normalized * (SEEKBAR_STEPS - 1));
    }

    private long progressToMillis(int progress) {
        double normalized = (double) progress / (SEEKBAR_STEPS - 1);
        return Math.round(Math.exp(normalized * (Math.log(MAX_INTERVAL) - Math.log(MIN_INTERVAL)) + Math.log(MIN_INTERVAL)));
    }

    private void scheduleNextUpdate() {
        Intent intent = new Intent(this, RandomFactsWidget.class);
        intent.setAction(RandomFactsWidget.ACTION_AUTO_UPDATE);
        sendBroadcast(intent);
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!RandomFactsWidget.canScheduleExactAlarms(this)) {
                Toast.makeText(this, "Please allow exact alarms for better functionality", Toast.LENGTH_LONG).show();
                Intent intent = RandomFactsWidget.getAlarmPermissionSettingsIntent(this);
                if (intent != null) {
                    startActivityForResult(intent, REQUEST_SCHEDULE_EXACT_ALARM);
                }
            }
        }
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
        }
    }

}