package com.bunny.entertainment.factoid.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.models.AnimeFactResponse;
import com.bunny.entertainment.factoid.networks.ApiService;
import com.bunny.entertainment.factoid.networks.NetworkUtils;
import com.bunny.entertainment.factoid.networks.RetrofitClient;
import com.bunny.entertainment.factoid.widgets.RandomAnimeFactsWidget;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class AnimeFactsWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private final List<String> facts;
    private static boolean shouldRefresh = false;
    private static final String PREFS_NAME = "AnimeFactsWidgetPrefs";
    private static final String LAST_FACT_KEY = "last_anime_fact";

    public AnimeFactsWidgetRemoteViewsFactory(Context context) {
        this.context = context;
        this.facts = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        String lastFact = getLastShownFact();
        if (!lastFact.isEmpty()) {
            facts.add(lastFact);
        }
    }

    @Override
    public void onDataSetChanged() {
        long updateInterval = getUpdateIntervalMillis();
        if (updateInterval == 0) { // "Off" state
            String lastFact = getLastShownFact();
            if (!lastFact.isEmpty()) {
                facts.clear();
                facts.add(lastFact);
            } else if (facts.isEmpty()) {
                // If there's no last fact and the list is empty, fetch a new fact
                fetchFacts();
            }
        } else if (facts.isEmpty() || shouldRefresh) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                fetchFacts();
            } else {
                Log.d("WidgetFactory", "No internet connection. Skipping update.");
            }
            shouldRefresh = false;
        }
    }

    private void fetchFacts() {
        ApiService apiService = RetrofitClient.getApiServiceAnimeFacts();
        try {
            facts.clear(); // Clear existing facts
            Response<AnimeFactResponse> response = apiService.getAnimeFact().execute();
            if (response.isSuccessful() && response.body() != null) {
                String fact = response.body().getFact();
                facts.add(fact);
                saveLastShownFact(fact); // Save the new fact
                Log.d("WidgetFactory", "Fetched fact: " + fact);
            }
            Log.d("WidgetFactory", "Total facts fetched: " + facts.size());
        } catch (Exception e) {
            Log.e("WidgetFactory", "Error fetching facts", e);
        }
    }


    public static void setRefreshFlag() {
        shouldRefresh = true;
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

    private void saveLastShownFact(String fact) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(LAST_FACT_KEY, fact);
        editor.apply();
    }

    private String getLastShownFact() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_FACT_KEY, "");
    }

    private long getUpdateIntervalMillis() {
        SharedPreferences prefs = context.getSharedPreferences(RandomAnimeFactsWidget.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(RandomAnimeFactsWidget.PREF_UPDATE_INTERVAL, 3600000); // Default to 1 hour
    }
}