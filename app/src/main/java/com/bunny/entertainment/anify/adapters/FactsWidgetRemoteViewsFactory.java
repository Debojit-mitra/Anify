package com.bunny.entertainment.anify.adapters;

import static com.bunny.entertainment.anify.utils.Constants.ACTION_AUTO_UPDATE;
import static com.bunny.entertainment.anify.utils.Constants.ACTION_UPDATE_FINISHED;
import static com.bunny.entertainment.anify.utils.Constants.DEFAULT_INTERVAL;
import static com.bunny.entertainment.anify.utils.Constants.PREFS_NAME;
import static com.bunny.entertainment.anify.utils.Constants.PREF_FACT_LAST_UPDATE_TIME;
import static com.bunny.entertainment.anify.utils.Constants.PREF_FACT_UPDATE_INTERVAL;
import static com.bunny.entertainment.anify.utils.Constants.PREF_LAST_FACT;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.anify.R;
import com.bunny.entertainment.anify.models.FactResponse;
import com.bunny.entertainment.anify.network.ApiService;
import com.bunny.entertainment.anify.network.NetworkMonitor;
import com.bunny.entertainment.anify.network.NetworkUtils;
import com.bunny.entertainment.anify.network.RetrofitClient;
import com.bunny.entertainment.anify.widgets.FactsWidget;

import java.util.ArrayList;
import java.util.List;

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
        return facts.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.list_item_fact);
        views.setTextViewText(R.id.fact_text, facts.get(position));
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
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
                String lastFact = getLastShownFact();
                if (!lastFact.isEmpty()) {
                    facts.add(lastFact);
                    Log.d(TAG, "Added last known fact: " + lastFact);
                }
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

    public static void setRefreshFlag() {
        shouldRefresh = true;
        Log.d(TAG, "Refresh flag set to true");
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
