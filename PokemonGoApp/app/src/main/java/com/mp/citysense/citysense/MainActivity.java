package com.mp.citysense.citysense;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;
import android.os.Message;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.util.Random;

public class MainActivity extends ActionBarActivity {

    private Handler handler = new Handler() {
        public void handleMessage(Message message) {
            Bundle data = message.getData();
            if (message.arg1 == RESULT_OK && data != null) {

                String location = data.getString("location");
                TextView textViewTemperature = (TextView)findViewById(R.id.textViewTemperature);
                textViewTemperature.setText("35 °F");

                TextView textViewHumidity = (TextView)findViewById(R.id.textViewHumidity);
                textViewHumidity.setText("Humidity High");

                Toast.makeText(MainActivity.this, "Location " + location,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Location failed.",
                        Toast.LENGTH_LONG).show();
            }

        };
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService();

        final Handler h = new Handler();
        h.post(new Runnable() {
            @Override
            public void run() {
                Integer intervalMS = 3000;
                final Random rand = new Random(System.currentTimeMillis());
                Integer temperature = 32 + rand.nextInt(10);
                TextView textViewTemperature = (TextView)findViewById(R.id.textViewTemperature);
                textViewTemperature.setText(temperature.toString() + "°F");

                Integer humidity = 20 + rand.nextInt(40);
                TextView textViewHumidity = (TextView)findViewById(R.id.textViewHumidity);
                textViewHumidity.setText(humidity.toString() + "%");
                h.postDelayed(this, intervalMS);
            }
        });

        String wsUrl = "wss://runm-east.att.io/042c6ef9aa2c0/af54689f1f07/cd37b285777ff9b/in/flow/ws/pokemon";
        AsyncHttpClient.getDefaultInstance().websocket(wsUrl, "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                //webSocket.send("a string");
                //webSocket.send(new byte[10]);
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    Integer count = 0;
                    MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.wildcut);

                    public void onStringAvailable(String s) {
                        count += 1;
                        final String input = s;
                        System.out.println("I got a string: " + s);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView textViewPokemon = (TextView) findViewById(R.id.pokemon_count);
                               // textViewPokemon.setText(count.toString());

                                if (input.equals("haspokemon")) {
                                   // mp = MediaPlayer.create(getApplicationContext(), R.raw.wildcut);
                                    TextView textViewPokemonText = (TextView) findViewById(R.id.pokemon);
                                    textViewPokemonText.setTextColor(Color.RED);
                                    textViewPokemonText.setText("Pokemon Found!");
                                    Animation anim = new AlphaAnimation(0.0f, 1.0f);
                                    anim.setDuration(50); //You can manage the time of the blink with this parameter
                                    anim.setStartOffset(20);
                                    anim.setRepeatMode(Animation.REVERSE);
                                    anim.setRepeatCount(Animation.INFINITE);
                                    textViewPokemonText.startAnimation(anim);
                                    //mp.start();
                                } else if (input.equals("nopokemon")) {
                                    TextView textViewPokemonText = (TextView) findViewById(R.id.pokemon);
                                    textViewPokemonText.setText("Pokemon Not Found");
                                    textViewPokemonText.setTextColor(Color.BLACK);
                                    textViewPokemonText.clearAnimation();
                                    //mp.stop();
                                }
                            }
                        });
                    }
                });
                webSocket.setDataCallback(new DataCallback() {
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                        System.out.println("I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });
            }
        });
    }

    public void startService() {
        Intent intent = new Intent(this, SensorIntentService.class);
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(handler);
        // TODO put messages into the intent
        intent.putExtra("MESSENGER", messenger);

        //EditText editTextServerURL = (EditText)findViewById(R.id.editTextServerURL);
        //String serverUrl = editTextServerURL.getText().toString();
        //TODO: change the URL
        String serverUrl = "https://geoeventsample4.esri.com:6143/geoevent/rest/receiver/rtdevicejson";
        intent.putExtra("serverurl", serverUrl);
        intent.setData(Uri.parse(serverUrl));

        //EditText editTextDeviceId = (EditText)findViewById(R.id.editTextDeviceId);
        //String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        String deviceId = "CitySense1";
        //TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        //String deviceId = telephonyManager.getDeviceId();
        //editTextDeviceId.setText(deviceId);
        intent.putExtra("deviceid", deviceId);
        //EditText editTextUsername = (EditText)findViewById(R.id.editTextDevicename);
        //BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
       // String devicename = myDevice.getName();
        String devicename = "CitySense1";
        //editTextUsername.setText(devicename);
        intent.putExtra("username", devicename);
        //EditText editTextDescription = (EditText)findViewById(R.id.editTextDescription);
        //String description = editTextDescription.getText().toString();
        String description = "temperature";
        intent.putExtra("description", description);
        //EditText editTextStatus = (EditText)findViewById(R.id.editTextStatus);
        //String status = editTextStatus.getText().toString();
        String status = "35";
        intent.putExtra("status", status);
        //EditText editTextInterval = (EditText)findViewById(R.id.editTextInterval);
        //String interval = editTextInterval.getText().toString();
        String interval = "15";
        intent.putExtra("interval", interval);

        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    public void onClickStartService(View view) {
        startService();
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
    }

    public void onClickStopService(View view) {
        Intent intent = new Intent(this, LocationUpdateService.class);
        stopService(intent);
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }
    */
}
