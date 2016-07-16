package com.mp.citysense.citysense;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SensorIntentService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.mp.citysense.citysense.action.FOO";
    private static final String ACTION_BAZ = "com.mp.citysense.citysense.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.mp.citysense.citysense.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.mp.citysense.citysense.extra.PARAM2";

    private int result = Activity.RESULT_CANCELED;

    private LocationManager locationManager;
    private String provider;
    private Intent intent;
    private SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
    private double prvLat;
    private double prvLng;
    private double latService;
    private double lngService;

    private class LocListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            latService = location.getLatitude();
            lngService = location.getLongitude();

            /*
            if (prvLat == latService && prvLng == lngService) {
                Log.d("Location Service", "Location unchaged.");
                return;
            }
            */

            System.out.println(lngService + ", " + latService);

            Uri data = intent.getData();
            String serverUrl = intent.getStringExtra("serverurl");
            String deviceId = intent.getStringExtra("deviceid");
            String devicename = intent.getStringExtra("devicename");
            String description = intent.getStringExtra("description");
            String status = intent.getStringExtra("status");
            String interval = intent.getStringExtra("interval");

            /*
            String fileName = data.getLastPathSegment();
            File output = new File(Environment.getExternalStorageDirectory(),
                    fileName + ".txt");
            if (output.exists()) {
                // Don't delete allow appending. Only delete if backlog data has been sent
                //output.delete();
            }

            FileOutputStream fos = null;
            */

            //TimeZone tz = TimeZone.getTimeZone("UTC");
            //df.setTimeZone(tz);
            String nowAsISO = dateformat.format(new Date());

            try {
                String jsonStr =
                        "{" +
                                "\"RTDevice\": {" +
                                "\"deviceid\": \"" + deviceId + "\"," +
                                "\"name\": \"" + devicename + "\"," +
                                "\"datetimestamp\": " + "\"" + nowAsISO + "\"," +
                                "\"description\": \"" + description + "\"," +
                                "\"longitude\": " + lngService + "," +
                                "\"latitude\": " + latService + "," +
                                "\"status\": \"" + status + "\"" +
                                "}" +
                                "}";
                //fos = new FileOutputStream(output.getPath(), true);
                // Should only write if failed to send
                //fos.write(jsonStr.getBytes());

                new MyAsyncTask().execute(serverUrl, jsonStr);
                result = Activity.RESULT_OK;
                prvLat = latService;
                prvLng = lngService;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                /*
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                */
            }
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

    } // private class end

    private class MyAsyncTask extends AsyncTask<String, Integer, Double> {
        @Override
        protected Double doInBackground(String... params) {
            postData(params[0], params[1]);
            return null;
        }

        protected void onPostExecute(Double result) {
            System.out.print("command sent");
        }

        protected void onProgressUpdate(Integer... progress) {

        }
    }

    public SensorIntentService() {
        super("SensorIntentService");
    }

    public void LocationUpdates(){
        LocationListener locList = new LocListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        locationManager.requestLocationUpdates(provider, 0, 0.0f, locList);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final Handler h = new Handler();
        h.post(new Runnable() {
            @Override
            public void run() {
                String interval = intent.getStringExtra("interval");
                Integer intervalMS = Integer.parseInt(interval) * 1000;

                System.out.println("Running Location Update Service");
                LocationUpdates();
                h.postDelayed(this, intervalMS);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Location Service", "Service destroyed");
    }


    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SensorIntentService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SensorIntentService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    // Will be called asynchronously by Android
    @Override
    protected void onHandleIntent(Intent intent) {
        this.intent = intent;

        Bundle extras = intent.getExtras();
        if (extras != null) {
            // TODO get your messager via the extras
            Message msg = Message.obtain();
            msg.arg1 = result;
            Bundle bundle = new Bundle();
            bundle.putString("location", lngService + "," + latService);
            msg.setData(bundle);
            // TODO Send message ensure to put it into a try catch

        }

        /*
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
        */
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void postData(String urlPath, String jsonData) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(urlPath);
        try {
            StringEntity se = new StringEntity(jsonData);

            //sets the post request as the resulting string
            httppost.setEntity(se);
            //sets a request header so the page receving the request
            //will know what to do with it
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json; charset=utf-8");
            HttpResponse response = httpclient.execute(httppost);
            StatusLine statusline = response.getStatusLine();
            if (statusline != null) {
                int statuscode = statusline.getStatusCode();
                System.out.println(statusline.toString() + " statusCode " + statuscode);
            }
        } catch(ClientProtocolException e) {

        } catch(IOException e) {

        }
    }
}
