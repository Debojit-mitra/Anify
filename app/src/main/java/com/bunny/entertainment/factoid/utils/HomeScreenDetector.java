package com.bunny.entertainment.factoid.utils;

import static com.bunny.entertainment.factoid.utils.Constants.ACTION_HOME_SCREEN_BECAME_VISIBLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bunny.entertainment.factoid.widgets.AnimeImageWidget;

public class HomeScreenDetector extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
            String reason = intent.getStringExtra("reason");
            if ("homekey".equals(reason) || "recentapps".equals(reason)) {
                sendHomeScreenVisibleIntent(context);
            }
        }
    }

    private void sendHomeScreenVisibleIntent(Context context) {
        Intent widgetIntent = new Intent(context, AnimeImageWidget.class);
        widgetIntent.setAction(ACTION_HOME_SCREEN_BECAME_VISIBLE);
        context.sendBroadcast(widgetIntent);
    }
}
