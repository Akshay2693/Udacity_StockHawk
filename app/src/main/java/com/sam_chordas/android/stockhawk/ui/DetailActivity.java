package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.animation.Animation;
import com.db.chart.animation.easing.CubicEase;
import com.db.chart.model.LineSet;
import com.db.chart.renderer.AxisRenderer;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import static android.R.attr.data;

public class DetailActivity extends AppCompatActivity {

    private String LOG_TAG = getClass().getSimpleName();
    private Intent mServiceIntent;
    private Context mContext;
    private boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        mContext = this;

        // Set title
        this.setTitle(getIntent().getStringExtra("symbol").toUpperCase());

        // Register LocalBroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("send-result-event"));

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if(isConnected){
            // Create and start intent to get stock history
            mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra("tag", "detail");
            mServiceIntent.putExtra("symbol", getIntent().getStringExtra("symbol"));
            startService(mServiceIntent);
        } else {
            networkToast();
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister since we're about to close the activity
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("result");

            // Grab UI elements
            //TextView textView = (TextView) findViewById(R.id.detail);
            LineChartView detailChart = (LineChartView) findViewById(R.id.detail_line_chart);
            TextView priceView = (TextView) findViewById(R.id.detail_current_price);
            TextView changeView = (TextView) findViewById(R.id.detail_change);


            // Populate dataset
            LineSet dataSet = Utils.historyJsonToContentVals(result);

            // Customize dataset
            dataSet.setColor(getResources().getColor(R.color.material_green_300, getTheme()));

            // Get min and max
            int min = (int) Math.floor(dataSet.getMin().getValue());
            int max = (int) Math.ceil(dataSet.getMax().getValue());

            // Get price and change data
            Float currentPrice = dataSet.getValue(dataSet.size()-1);
            Float startingPrice = dataSet.getValue(0);
            Float changePrice = currentPrice-startingPrice;
            Float percentChange = changePrice/startingPrice * 100;

            String priceString = Utils.truncateBidPrice(currentPrice.toString());
            String changeString = Utils.truncateChange(changePrice.toString(), false);
            String percentString = Utils.truncateChange(percentChange.toString() + "%", true);
            String detailString = changeString + " (" + percentString + ") " +
                    getString(R.string.detail_time_month);

            priceView.setText(priceString);
            changeView.setText(detailString);

            // Customize Chart
            detailChart.setXLabels(AxisRenderer.LabelPosition.NONE);
            detailChart.setYLabels(AxisRenderer.LabelPosition.NONE);
            detailChart.setXAxis(false);
            detailChart.setYAxis(false);

            // Draw dotted line across chart
            Paint line = new Paint();
            line.setColor(getResources().getColor(R.color.material_green_300, getTheme()));
            line.setStrokeWidth(2);
            detailChart.setValueThreshold(dataSet.getValue(0), dataSet.getValue(0),
                    line);

            // Create animation
            Animation anim = new Animation(500);
            anim.setEasing(new CubicEase());

            // Set chart data
            detailChart.addData(dataSet);
            detailChart.setAxisBorderValues(min, max);
            detailChart.show(anim);

            Log.d(LOG_TAG, result);
            //textView.setText(getIntent().getStringExtra("symbol"));
        }
    };

    private final BroadcastReceiver mProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean displayProgress = intent.getBooleanExtra("display-progress", false);
            Log.d(LOG_TAG, "Received progress update");

            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
            if(displayProgress){
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
    };

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.toast_network_disconnected), Toast.LENGTH_SHORT).show();
    }
}
