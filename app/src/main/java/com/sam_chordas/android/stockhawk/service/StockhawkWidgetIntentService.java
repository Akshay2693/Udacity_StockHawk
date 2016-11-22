package com.sam_chordas.android.stockhawk.service;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.StockhawkWidgetProvider;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by Jayson Dela Cruz on 11/22/2016.
 *
 * This class launches a thread to update the Stockhawk widget with data from our content
 * provider.
 */

public class StockhawkWidgetIntentService extends IntentService {

    private String LOG_TAG = getClass().getSimpleName();

    public StockhawkWidgetIntentService() {
        super("StockhawkWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                StockhawkWidgetProvider.class));


        // Perform this loop procedure for each Stockhawk widget
        for (int appWidgetId : appWidgetIds) {
            int layoutId = R.layout.list_item_widget_quote;
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Add the data to the RemoteViews
            Log.d(LOG_TAG, "Updating widget with " + Long.toString(SystemClock.uptimeMillis()));
            views.setTextViewText(R.id.widget_symbol, Long.toString(SystemClock.uptimeMillis()));

            // Content Descriptions for RemoteViews were only added in ICS MR1
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//                setRemoteContentDescription(views, description);
//            }

            // Setup click intent
            Intent myintent = new Intent(getApplicationContext(), MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent
                    .getActivity(getApplicationContext(), 0, myintent, 0);
            views.setOnClickPendingIntent(R.id.widget_quote, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);



        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        //views.setContentDescription(R.id.widget_icon, description);
    }
}
