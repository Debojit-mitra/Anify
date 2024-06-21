package com.bunny.entertainment.factoid.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.factoid.R;
import com.bunny.entertainment.factoid.models.FactResponse;
import com.bunny.entertainment.factoid.networks.ApiService;
import com.bunny.entertainment.factoid.networks.NetworkMonitor;
import com.bunny.entertainment.factoid.networks.NetworkUtils;
import com.bunny.entertainment.factoid.networks.RetrofitClient;
import com.bunny.entertainment.factoid.widgets.RandomFactsWidget;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<String> facts;
    private static boolean shouldRefresh = false;
    private NetworkMonitor networkMonitor;


    public WidgetRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.facts = new ArrayList<>();
        this.networkMonitor = NetworkMonitor.getInstance(context);
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        if (facts.isEmpty() || shouldRefresh) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                fetchFacts();
            } else {
                Log.d("WidgetFactory", "No internet connection. Skipping update.");
            }
            shouldRefresh = false;
        }
    }

    private void fetchFacts() {
        ApiService apiService = RetrofitClient.getApiService();
        try {
            facts.clear(); // Clear existing facts
            Response<FactResponse> response = apiService.getRandomFact().execute();
            if (response.isSuccessful() && response.body() != null) {
                String fact = response.body().getText();
                facts.add(fact);
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
}
