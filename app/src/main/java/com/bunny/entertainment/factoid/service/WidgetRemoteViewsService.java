package com.bunny.entertainment.factoid.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.factoid.adapter.WidgetRemoteViewsFactory;

public class WidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}