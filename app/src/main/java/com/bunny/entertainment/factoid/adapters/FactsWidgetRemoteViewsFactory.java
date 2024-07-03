package com.bunny.entertainment.factoid.adapters;

import static com.bunny.entertainment.factoid.utils.Constants.API_ERROR_MESSAGE;
import static com.bunny.entertainment.factoid.utils.Constants.DEFAULT_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREFS_NAME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_FACT_LAST_UPDATE_TIME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_FACT_UPDATE_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_FACT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.models.FactResponse;
import com.bunny.entertainment.factoid.network.ApiService;
import com.bunny.entertainment.factoid.network.NetworkMonitor;
import com.bunny.entertainment.factoid.network.NetworkUtils;
import com.bunny.entertainment.factoid.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

public class FactsWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "FactsWidgetRemoteViewsFactory";
    private final Context context;
    private final List<String> facts;
    private static boolean shouldRefresh = false;

    public FactsWidgetRemoteViewsFactory(Context context) {
        this.context = context;
        this.facts = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        String lastFact = getLastShownFact();
        if (!lastFact.isEmpty()) {
            facts.add(lastFact);
            Log.d(TAG, "Last shown fact added: " + lastFact);
        } else {
            Log.d(TAG, "No last shown fact found");
        }
    }

    @Override
    public void onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged called");
        long updateInterval = getUpdateIntervalMillis();
        Log.d(TAG, "Update interval: " + updateInterval + " ms");
        long currentTime = System.currentTimeMillis();

        if (updateInterval == 0) {
            Log.d(TAG, "Update interval is 0 (Off state)");
            String lastFact = getLastShownFact();
            if (!lastFact.isEmpty()) {
                facts.clear();
                facts.add(lastFact);
                Log.d(TAG, "Added last shown fact in Off state: " + lastFact);
            } else if (facts.isEmpty()) {
                Log.d(TAG, "No last fact and list is empty. Fetching new fact.");
                fetchFacts();
                setLastUpdateTime(currentTime);
            }
        } else {
            long lastUpdateTime = getLastUpdateTime();
            long elapsedTime = currentTime - lastUpdateTime;
            Log.d(TAG, "Last update time: " + lastUpdateTime + ", Current time: " + currentTime + ", Elapsed time: " + elapsedTime);

            if (facts.isEmpty() || shouldRefresh || elapsedTime >= updateInterval) {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    Log.d(TAG, "Fetching new facts");
                    fetchFacts();
                    setLastUpdateTime(currentTime);
                } else {
                    NetworkMonitor networkMonitor = NetworkMonitor.getInstance(context);
                    networkMonitor.startMonitoring(() -> {
                        Log.d(TAG, "Fetching new facts");
                        fetchFacts();
                        setLastUpdateTime(currentTime);
                        networkMonitor.stopMonitoring();
                    });
                }
                shouldRefresh = false;
            } else {
                Log.d(TAG, "No need to update facts yet");
            }
        }
    }

    @Override
    public void onDestroy() {
        facts.clear();
    }

    @Override
    public int getCount() {
        return facts.size() + 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == facts.size()) {
            // This is the "Next update" item
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.list_item_next_update);
            long updateInterval = getUpdateIntervalMillis();
            long nextUpdateTime = System.currentTimeMillis() + updateInterval;
            String formattedTime = formatTime(nextUpdateTime);
            views.setTextViewText(R.id.next_update_time, formattedTime);
            return views;
        }  else {
            // This is a fact item
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.list_item_fact);
            views.setTextViewText(R.id.fact_text, facts.get(position));
            return views;
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void fetchFacts() {
        ApiService apiService = RetrofitClient.getApiServiceFacts();
        try {
            facts.clear();
            Log.d(TAG, "Cleared existing facts");
            Response<FactResponse> response = apiService.getRandomFact().execute();
            if (response.isSuccessful() && response.body() != null) {
                String fact = response.body().getText();
                facts.add(fact);
                setLastShownFact(fact);
                Log.d(TAG, "Fetched and saved new fact: " + fact);
            } else {
                Log.w(TAG, "Unsuccessful API response or null body");
                handleError();
            }
            Log.d(TAG, "Total facts after fetch: " + facts.size());
        } catch (Exception e) {
            Log.e(TAG, "Error fetching facts", e);
            String lastFact = getLastShownFact();
            if (!lastFact.isEmpty()) {
                facts.add(lastFact);
                Log.d(TAG, "Added last known fact due to error: " + lastFact);
            }
        }
    }

    private void handleError() {
        String lastFact = getLastShownFact();
        if (!lastFact.isEmpty()) {
            facts.add(lastFact);
            Log.d(TAG, "Added last known fact due to error: " + lastFact);
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error! Showing last fact.", Toast.LENGTH_SHORT).show());
        } else {
            facts.add(API_ERROR_MESSAGE);
            Log.d(TAG, "Added error message to facts list");
        }
        //setLastShownFact(facts.get(0)); // Save the displayed message (last fact or error)
    }

    public static void setRefreshFlag() {
        shouldRefresh = true;
        Log.d(TAG, "Refresh flag set to true");
    }

    private String formatTime(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timeInMillis));
    }

    private long getUpdateIntervalMillis() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long interval = prefs.getLong(PREF_FACT_UPDATE_INTERVAL, DEFAULT_INTERVAL);
        Log.d(TAG, "Retrieved update interval: " + interval + " ms");
        return interval;
    }

    private void setLastUpdateTime(long time) {
        Log.d(TAG, "Saving last update time: " + time);
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(PREF_FACT_LAST_UPDATE_TIME, time);
        editor.apply();
    }

    private long getLastUpdateTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(PREF_FACT_LAST_UPDATE_TIME, 0);
        Log.d(TAG, "Retrieved last update time: " + lastUpdateTime);
        return lastUpdateTime;
    }

    private void setLastShownFact(String fact) {
        Log.d(TAG, "Saving last shown fact: " + fact);
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_LAST_FACT, fact);
        editor.apply();
    }

    private String getLastShownFact() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastFact = prefs.getString(PREF_LAST_FACT, "");
        Log.d(TAG, "Retrieved last shown fact: " + (lastFact.isEmpty() ? "None" : lastFact));
        return lastFact;
    }
}
