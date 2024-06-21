package com.bunny.entertainment.factoid.networks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

public class NetworkMonitor {
    private static NetworkMonitor instance;
    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler;
    private NetworkCallback callback;
    private boolean isMonitoring = false;


    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    public void startMonitoring(Runnable onNetworkAvailable) {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        callback = new NetworkCallback(onNetworkAvailable);

        connectivityManager.registerNetworkCallback(networkRequest, callback);
        isMonitoring = true;
    }

    public void stopMonitoring() {
        if (callback != null) {
            connectivityManager.unregisterNetworkCallback(callback);
            callback = null;
        }
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private final Runnable onNetworkAvailable;

        NetworkCallback(Runnable onNetworkAvailable) {
            this.onNetworkAvailable = onNetworkAvailable;
        }

        @Override
        public void onAvailable(Network network) {
            mainHandler.post(onNetworkAvailable);
        }
    }
}