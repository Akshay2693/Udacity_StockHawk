package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by Jayson Dela Cruz on 11/22/2016.
 *
 * This class launches a thread to update the Stockhawk widget with data from our content
 * provider.
 */

public class StockhawkWidgetIntentService extends IntentService {

    public StockhawkWidgetIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
