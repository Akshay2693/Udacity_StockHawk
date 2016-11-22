package com.sam_chordas.android.stockhawk.rest;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.service.StockhawkWidgetIntentService;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by Jayson Dela Cruz on 11/21/2016.
 */

public class StockhawkWidgetProvider extends AppWidgetProvider{
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for(int appWidgetId:appWidgetIds){
            // Set up each widget ID
            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.list_item_widget_quote);

            // Setup click intent
            Intent intent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent
                    .getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget_quote, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())){
            context.startService(new Intent(context, StockhawkWidgetIntentService.class));
        }

    }
}
