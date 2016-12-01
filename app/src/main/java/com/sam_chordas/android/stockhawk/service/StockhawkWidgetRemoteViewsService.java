package com.sam_chordas.android.stockhawk.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by Jayson Dela Cruz on 11/22/2016.
 *
 * This class launches a thread to update the Stockhawk widget with data from our content
 * provider.
 */

public class StockhawkWidgetRemoteViewsService extends RemoteViewsService {

    private String LOG_TAG = getClass().getSimpleName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory(){

            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                // Grab the updated dataset
                if(data != null){
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                // TODO: Get updated quotes data from the ContentProvider
//                String location = Utility.getPreferredLocation(DetailWidgetRemoteViewsService.this);
//                Uri weatherForLocationUri = WeatherContract.WeatherEntry
//                        .buildWeatherLocationWithStartDate(location, System.currentTimeMillis());
//                data = getContentResolver().query(weatherForLocationUri,
//                        FORECAST_COLUMNS,
//                        null,
//                        null,
//                        WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
                Binder.restoreCallingIdentity(identityToken);


            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                //views.setContentDescription(R.id.widget_icon, description);
            }

            @Override
            public void onDestroy() {
                if(data != null){
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                // Return null if data is empty or position is invalid
                if(position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)){
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_quotes);

                // TODO: populate views

                final Intent fillInIntent = new Intent();
                String symbol = "AAPL";
                fillInIntent.putExtra("symbol", symbol);
                views.setOnClickFillInIntent(R.id.widget_quote, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_quotes);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int i) {
                // TODO: change this once we get ahold of the quotes data
                //if(data.moveToPosition(i))
                    //return data.getLong(INDEX_WEATHER_ID);

                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }


        };
    }
}
