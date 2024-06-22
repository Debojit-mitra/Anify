package com.bunny.entertainment.factoid.networks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public class NetworkMonitor {
    private static NetworkMonitor instance;
    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler;
    private ConnectivityManager.NetworkCallback callback;


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
        stopMonitoring(); // Remove any existing callback

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                mainHandler.post(() -> {
                    onNetworkAvailable.run();
                    stopMonitoring(); // Remove callback after network becomes available
                });
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, callback);
    }

    public void stopMonitoring() {
        if (callback != null) {
            connectivityManager.unregisterNetworkCallback(callback);
            callback = null;
        }
    }
}