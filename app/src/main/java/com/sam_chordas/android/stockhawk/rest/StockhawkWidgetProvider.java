package com.sam_chordas.android.stockhawk.rest;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.service.StockhawkWidgetIntentService;

/**
 * Created by Jayson Dela Cruz on 11/21/2016.
 */

public class StockhawkWidgetProvider extends AppWidgetProvider{
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // Call start widget intent service
        Intent intent = new Intent(context, StockhawkWidgetIntentService.class);
        context.startService(intent);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())){
            context.startService(new Intent(context, StockhawkWidgetIntentService.class));
        }

    }
}
