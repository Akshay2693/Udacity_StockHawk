package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

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
            mServiceIntent.putExtra("symbol", "TEST");
            startService(mServiceIntent);
        } else {
            networkToast();
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister since we're about to close the activity
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("result");
            TextView textView = (TextView) findViewById(R.id.detail_text);
            Log.d(LOG_TAG, result);
            textView.setText(result);
        }
    };

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.toast_network_disconnected), Toast.LENGTH_SHORT).show();
    }
}
