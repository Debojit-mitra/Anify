package com.bunny.entertainment.factoid.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.bunny.entertainment.factoid.adapters.FactsWidgetRemoteViewsFactory;

public class FactsWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new FactsWidgetRemoteViewsFactory(this.getApplicationContext());
    }
}
