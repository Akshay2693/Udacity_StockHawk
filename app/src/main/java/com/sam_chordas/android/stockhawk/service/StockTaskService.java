package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;
  private String mSymbol;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }

  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onStartCommand(Intent intent, int i, int i1) {
    mSymbol = intent.getStringExtra("symbol");

    return super.onStartCommand(intent, i, i1);
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }

    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      if(params.getTag().equals("detail")){
        // Build a detail query
        urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata "
                + "where symbol = ", "UTF-8"));
      } else {
        // Build a quotes query
        urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                + "in (", "UTF-8"));
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    // Create a recurring query url
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
          null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
              initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }

      // Create a new stock query URL
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }

      // Create a stock detail query URL
    } else if (params.getTag().equals("detail")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");

      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\"", "UTF-8"));
        urlStringBuilder.append(URLEncoder.encode(" and startDate = \"2016-10-01\"" +
                " and endDate = \"2016-11-07\"", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }

    }


    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
      Log.d(LOG_TAG, "URL is: " + urlString);
      try{
        // Network communication starts, show progress bar until result is received
        showProgress();
        getResponse = fetchData(urlString);
        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                null, null);
          }

          if(params.getTag().equals("detail")){
            // Process the result for details
            sendResult(getResponse);
          } else {
            // Process the result for a quote
            if(isValidQuote(getResponse)){
              mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                      Utils.quoteJsonToContentVals(getResponse));
            } else {
              // Create a runnable to show a Toast message on the main UI thread.
              showToast(mContext.getString(R.string.toast_stock_invalid));
            }
          }
          hideProgress();

        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }
      } catch (IOException e){
        e.printStackTrace();
        Log.d(LOG_TAG, e.getMessage());
        // Display the error to the user
        showToast("Server error: " + e.getMessage());
        hideProgress();
      }
    }

    return result;
  }

  /**
   * Checks that the resulting JSON contains valid quote data. Searches for invalid ticker symbols
   * returns a valid JSON string, but the quote data is all "null" so we have to manually check.
   * @param JSON string returned from the yahoo stocks API
   * @return True if quote data is valid, false if not.
     */
  private boolean isValidQuote(String JSON){

    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    boolean isValid = true;

    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
                  .getJSONObject("quote");

          // If this field == 'null' then the quote is invalid
          if(jsonObject.getString("Ask").equals("null")){
            isValid = false;
          }

        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);

              // If this field == 'null' then the quote is invalid
              if(jsonObject.getString("Ask").equals("null")){
                isValid = false;
              }

            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }

    if(isValid){
      Log.d(LOG_TAG, "Quote is valid.");
    } else {
      Log.d(LOG_TAG, "Quote is invalid.");
    }
    return isValid;
  }

  /**
   * This method is used to display toast messages to the user
   * @param message to be shown to the user
     */
  private void showToast(final String message){
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(new Runnable() {

      @Override
      public void run() {
        Toast.makeText(mContext,
                message,
                Toast.LENGTH_SHORT).show();
      }
    });
  }

  /**
   * Send an intent to the stocks activity to display the progress bar
   */
  private void showProgress(){
    Intent intent = new Intent("update-progress-event");
    // Add whether to display the progress bar
    intent.putExtra("display-progress", true);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  /**
   * Send an intent to the stocks activity to hide the progress bar
   */
  private void hideProgress(){
    Intent intent = new Intent("update-progress-event");
    // Add whether to display the progress bar
    intent.putExtra("display-progress", false);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  /**
   * Send the resulting detail JSON back to the detail activity for processing
   */
  private void sendResult(String result){
    Intent intent = new Intent("send-result-event");
    intent.putExtra("result", result);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    stopSelf();
  }

}


