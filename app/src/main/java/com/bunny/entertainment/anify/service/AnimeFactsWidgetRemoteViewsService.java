package com.bunny.entertainment.anify.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.anify.adapters.AnimeFactsWidgetRemoteViewsFactory;

public class AnimeFactsWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new AnimeFactsWidgetRemoteViewsFactory(this.getApplicationContext());
    }
}
