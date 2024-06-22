package com.bunny.entertainment.factoid.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.factoid.adapter.AnimeFactsWidgetRemoteViewsFactory;

public class AnimeFactsWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new AnimeFactsWidgetRemoteViewsFactory(this.getApplicationContext());
    }
}