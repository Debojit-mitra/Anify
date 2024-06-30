package com.bunny.entertainment.anify.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.anify.adapters.FactsWidgetRemoteViewsFactory;

public class FactsWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new FactsWidgetRemoteViewsFactory(this.getApplicationContext());
    }
}
