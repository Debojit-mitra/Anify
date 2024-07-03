package com.bunny.entertainment.factoid.adapters;

import static com.bunny.entertainment.factoid.utils.Constants.API_ERROR_MESSAGE;
import static com.bunny.entertainment.factoid.utils.Constants.DEFAULT_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREFS_NAME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_FACT_LAST_UPDATE_TIME;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_ANIME_FACT_UPDATE_INTERVAL;
import static com.bunny.entertainment.factoid.utils.Constants.PREF_LAST_ANIME_FACT;
import static com.bunny.entertainment.factoid.utils.Constants.WAIFU_IT_API_KEY;
import static com.bunny.entertainment.factoid.utils.Constants.WAIFU_IT_API_KEY_ERROR;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.models.AnimeFactResponse;
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

public class AnimeFactsWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "AnimeFactsWidgetRemoteViewsFactory";
    private final Context context;
    private final List<String> animeFacts;
    private static boolean shouldRefresh = false;

    public AnimeFactsWidgetRemoteViewsFactory(Context context) {
        this.context = context;
        animeFacts = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        String lastAnimeFact = getLastShownAnimeFact();
        if (!lastAnimeFact.isEmpty()) {
            animeFacts.add(lastAnimeFact);
            Log.d(TAG, "Last shown Anime fact added: " + lastAnimeFact);
        } else {
            Log.d(TAG, "No last shown Anime fact found");
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
            String lastFact = getLastShownAnimeFact();
            if (!lastFact.isEmpty()) {
                animeFacts.clear();
                animeFacts.add(lastFact);
                Log.d(TAG, "Added last shown anime fact in Off state: " + lastFact);
            } else if (animeFacts.isEmpty()) {
                Log.d(TAG, "No last anime fact and list is empty. Fetching new fact.");
                fetchFacts();
                setLastUpdateTime(currentTime);
            }
        } else {
            long lastUpdateTime = getLastUpdateTime();
            long elapsedTime = currentTime - lastUpdateTime;
            Log.d(TAG, "Last update time: " + lastUpdateTime + ", Current time: " + currentTime + ", Elapsed time: " + elapsedTime);

            if (animeFacts.isEmpty() || shouldRefresh || elapsedTime >= updateInterval) {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    Log.d(TAG, "Fetching new anime facts");
                    fetchFacts();
                    setLastUpdateTime(currentTime);
                } else {
                    NetworkMonitor networkMonitor = NetworkMonitor.getInstance(context);
                    networkMonitor.startMonitoring(() -> {
                        Log.d(TAG, "Fetching new anime facts");
                        fetchFacts();
                        setLastUpdateTime(currentTime);
                        networkMonitor.stopMonitoring();
                    });
                }
                shouldRefresh = false;
            } else {
                Log.d(TAG, "No need to update anime facts yet");
            }
        }
    }

    public void onDestroy() {
        animeFacts.clear();
    }

    @Override
    public int getCount() {
        return animeFacts.size() + 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == animeFacts.size()) {
            // This is the "Next update" item
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.list_item_next_update);
            if (getWaifuItAPIKey() != null) {
                long updateInterval = getUpdateIntervalMillis();
                long nextUpdateTime = System.currentTimeMillis() + updateInterval;
                String formattedTime = formatTime(nextUpdateTime);
                views.setTextViewText(R.id.next_update_time, formattedTime);
            } else {
                // Hide or remove the next update time view
                views.setViewVisibility(R.id.next_update_time, View.GONE);
            }
            return views;
        } else {
            // This is a fact item
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.list_item_fact);
            views.setTextViewText(R.id.fact_text, animeFacts.get(position));
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
        if (getWaifuItAPIKey() != null){
            ApiService apiService = RetrofitClient.getApiServiceAnimeFacts(getWaifuItAPIKey());
            try {
                animeFacts.clear();
                Log.d(TAG, "Cleared existing anime facts");
                Response<AnimeFactResponse> response = apiService.getAnimeFact().execute();
                if (response.isSuccessful() && response.body() != null) {
                    String fact = response.body().getFact();
                    animeFacts.add(fact);
                    setLastShownAnimeFact(fact);
                    getViewAt(animeFacts.size());
                    Log.d(TAG, "Fetched and saved new anime fact: " + fact);
                } else {
                    Log.w(TAG, "Unsuccessful API response or null body");
                    handleError();
                }
                Log.d(TAG, "Total anime facts after fetch: " + animeFacts.size());
            } catch (Exception e) {
                Log.e(TAG, "Error fetching facts", e);
                String lastFact = getLastShownAnimeFact();
                if (!lastFact.isEmpty()) {
                    animeFacts.add(lastFact);
                    Log.d(TAG, "Added last known anime fact due to error: " + lastFact);
                }
            }
        } else {
            animeFacts.add(WAIFU_IT_API_KEY_ERROR);
        }

    }

    private void handleError() {
        String lastFact = getLastShownAnimeFact();
        if (!lastFact.isEmpty()) {
            animeFacts.add(lastFact);
            Log.d(TAG, "Added last known fact due to error: " + lastFact);
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "API Error! Showing last fact.", Toast.LENGTH_SHORT).show());
        } else {
            animeFacts.add(API_ERROR_MESSAGE);
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
        long interval = prefs.getLong(PREF_ANIME_FACT_UPDATE_INTERVAL, DEFAULT_INTERVAL);
        Log.d(TAG, "Retrieved update interval: " + interval + " ms");
        return interval;
    }

    private void setLastUpdateTime(long time) {
        Log.d(TAG, "Saving last update time: " + time);
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(PREF_ANIME_FACT_LAST_UPDATE_TIME, time);
        editor.apply();
    }

    private long getLastUpdateTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(PREF_ANIME_FACT_LAST_UPDATE_TIME, 0);
        Log.d(TAG, "Retrieved last update time: " + lastUpdateTime);
        return lastUpdateTime;
    }

    private void setLastShownAnimeFact(String fact) {
        Log.d(TAG, "Saving last shown fact: " + fact);
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_LAST_ANIME_FACT, fact);
        editor.apply();
    }

    private String getLastShownAnimeFact() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastFact = prefs.getString(PREF_LAST_ANIME_FACT, "");
        Log.d(TAG, "Retrieved last shown fact: " + (lastFact.isEmpty() ? "None" : lastFact));
        return lastFact;
    }
    private String getWaifuItAPIKey() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(WAIFU_IT_API_KEY, null); // Default to 1hr
    }
}
