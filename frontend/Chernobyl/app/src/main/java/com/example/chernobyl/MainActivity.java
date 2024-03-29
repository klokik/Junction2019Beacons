package com.example.chernobyl;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.icu.util.Output;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;

import android.media.AudioManager;

//import Radioactivity;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    static int counter = 0;

    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;

    MediaPlayer mediaPlayer;

    int dev_id = 0;
    String player_name = "John";
    Boolean is_already_dead = false;

    public static final String server_uri = "http://10.100.52.41:8000";




    // индикатор дозы, от 0 до 1 (зашкал)
    void setDoseIndicatorValue(final float value) {
        runOnUiThread(new Runnable() {
            public void run() {
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageResource(R.drawable.ic_scale);


                Bitmap workingBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                Bitmap bitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
                //Canvas canvas = new Canvas(mutableBitmap);

                //Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
                //Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(15);
                paint.setStyle(Paint.Style.STROKE);

                float initAngle = (float) 3.14 * 1.3f;
                float endAngle = initAngle + 3.14f / 2.3f;

                float actualAngle = initAngle + value * (endAngle - initAngle);


                float angle = actualAngle;
                if (angle >= endAngle) {
                    angle = endAngle;
                }
                float newX = (float) cos(angle) * 10;
                float newY = (float) sin(angle) * 10;


                float pointerLength = bitmap.getWidth() / 2.0f;
                float x0 = workingBitmap.getWidth() / 2;
                float y0 = workingBitmap.getHeight() + 300;
                float x1 = x0 + (float) cos(angle) * pointerLength;
                float y1 = y0 + (float) sin(angle) * pointerLength;


                canvas.drawLine(x0, y0, x1, y1, paint);


                imageView.setImageBitmap(bitmap);
            }});
    }

    void setFirstPlayerStatus(final boolean isShown, final boolean isAlive, final String playerName) {
        runOnUiThread(new Runnable() {
            public void run() {
                ImageView firstImage = (ImageView) findViewById(R.id.firstPlayerImageView);
                TextView firstText = (TextView) findViewById(R.id.firstPlayerTextView);

                if (isShown) {
                    firstImage.setVisibility(View.VISIBLE);
                    firstText.setVisibility(View.VISIBLE);

                } else {
                    firstImage.setVisibility(View.INVISIBLE);
                    firstText.setVisibility(View.INVISIBLE);
                }

                if (isAlive) {
                    firstImage.setImageResource(R.drawable.ic_button2);
                } else {
                    firstImage.setImageResource(R.drawable.ic_button1);
                }

                firstText.setText(playerName);
            }});
    }

    void setSecondPlayerStatus(final boolean isShown, final boolean isAlive, final String playerName) {
        runOnUiThread(new Runnable() {
            public void run() {
                ImageView firstImage = (ImageView) findViewById(R.id.secondPlayerImageView);
                TextView firstText = (TextView) findViewById(R.id.secondPlayerTextView);

                if (isShown) {
                    firstImage.setVisibility(View.VISIBLE);
                    firstText.setVisibility(View.VISIBLE);

                } else {

                    firstImage.setVisibility(View.INVISIBLE);
                    firstText.setVisibility(View.INVISIBLE);

                }

                if (isAlive) {
                    firstImage.setImageResource(R.drawable.ic_button2);
                } else {
                    firstImage.setImageResource(R.drawable.ic_button1);
                }

                firstText.setText(playerName);
            }});
    }

    float delta_dose = 0.1f;

    // Это callback, вызывается само
    void beaconPulseReceived(int rssi, float distanceInMeters) {
        TextView tv1 = (TextView) findViewById(R.id.infoTextView);
        Log.i("", "Our beacon found: " + String.valueOf(rssi));

//        delta_dose =
//        delta_dose += ()
        if (rssi > -36) {
            setTreskIntensity(4);
            delta_dose = 2*10;
        } else if (rssi > -50) {
            setTreskIntensity(3);
            delta_dose = 2*4;
        } else if (rssi > -65) {
            setTreskIntensity(2);
            delta_dose = 2*2;
        } else {
            setTreskIntensity(1);
            delta_dose = 0.5f;
        }

//        requestData(this.server_uri, String.format("{\"what\": \"add_dose\",\"name\":\"%s\",\"delta_dose\":%f}", this.player_name, delta_dose));

        tv1.setText("Warning, radiation level " + String.format("%.2f", 1/(distanceInMeters)-5)+" Rg/h");
    }

    void makeGameOver() {
        Intent myIntent = new Intent(this, GameOverActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        //myIntent.putExtra("key", value); //Optional parameters
        this.startActivity(myIntent);

    }

    void restartNewGame() {

    }

    int tresk_itencity = -1;

    // val в диапазоне 0..4, где 0 - полная тишина, 4 макс треск
    void setTreskIntensity(int val) {
        if (val == tresk_itencity)
            return;

        if (val == 0) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
        } else if (val == 1) {
            startPlayingSample(R.raw.ic_geiger0);
        } else if (val == 2) {
            startPlayingSample(R.raw.ic_geiger1);
        } else if (val == 3) {
            startPlayingSample(R.raw.ic_geiger2);
        } else if (val == 4) {
            startPlayingSample(R.raw.ic_geiger3);
        }
    }


    private void startPlayingSample(int resid) {
        AssetFileDescriptor afd = this.getResources().openRawResourceFd(resid);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setLooping(true);

            PlaybackParams params = new PlaybackParams();
            //params.setSpeed(10);
            //params.setPitch(0.25f);

            mediaPlayer.setPlaybackParams(params);
            afd.close();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to play audio queue do to exception: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Unable to play audio queue do to exception: " + e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to play audio queue do to exception: " + e.getMessage(), e);
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    private void requestData(String url, String jsonRequestString) {
        //Everything below is part of the Android Asynchronous HTTP Client

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(5000);
        client.addHeader("content-type", "application/json");
        RequestParams params = new RequestParams();
        params.add("JSON", jsonRequestString);


        client.post(url, params, new ResponseHandlerInterface() {
            @Override
            public void sendResponseMessage(final HttpResponse response) throws IOException {
                Log.i("sendResponseMessage", response.toString());



//                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
//
//                Runnable myRunnable = new Runnable() {
//                    @Override
//                    public void run() {






                try {
                    String s = convertStreamToString(response.getEntity().getContent());
                    Log.i("", s);


                    s = s.replace("\\", "");
                    s = s.substring(1, s.length()-1);
                    Log.i("", s);
//                    JSONObject jo = new JSONObject(s);
                    JSONObject jo = new JSONObject(s);

                    JSONArray players = jo.getJSONArray("players");
                    int offset = 0;
                    for (int i = 0; i < players.length(); ++i) {
                        JSONObject player = players.getJSONObject(i);
                        final String name = player.getString("name");
                        final Boolean alive = !player.getBoolean("is_dead");
                        Log.i("kek", name);


//                        setFirstPlayerStatus(false, alive, name);
//                        setSecondPlayerStatus(false, alive, name);

                        if (name.startsWith(player_name)) {
                            Log.i("UserName", "It's a me, Mario");
                            float dose = (float) (player.getDouble("dose")) / 100.f;
                            setDoseIndicatorValue(dose);

                            if (!alive && !is_already_dead) {
                                makeGameOver();
                                is_already_dead = true;
                            }
                        } else {
                            Log.i("UserName", "It's not a me, Louigy");

                            if (offset == 0)
                                setFirstPlayerStatus(true, alive, name);
                            else
                                setSecondPlayerStatus(true, alive, name);

                            offset += 1;
                        }
                    }

                    if (offset == 0) {
                        setFirstPlayerStatus(false, false, "Player1");
                        setSecondPlayerStatus(false, false, "Player2");
                    }
                    else if (offset == 1) {
                        setSecondPlayerStatus(false, false, "Player1");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


//                } // This is your code
//                };
//                mainHandler.post(myRunnable);

            }

            @Override
            public void sendStartMessage() {

            }

            @Override
            public void sendFinishMessage() {

            }

            @Override
            public void sendProgressMessage(long bytesWritten, long bytesTotal) {

            }

            @Override
            public void sendCancelMessage() {

            }

            @Override
            public void sendSuccessMessage(int statusCode, Header[] headers, byte[] responseBody) {
                String response = responseBody.toString();
                Log.i("HTTP REQUEST RESPONSE", response);
            }

            @Override
            public void sendFailureMessage(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.i("sendFailureMessage", "" + String.valueOf(statusCode) + " "+error.toString());
            }

            @Override
            public void sendRetryMessage(int retryNo) {

            }

            @Override
            public URI getRequestURI() {
                return null;
            }

            @Override
            public void setRequestURI(URI requestURI) {

            }

            @Override
            public Header[] getRequestHeaders() {
                return new Header[0];
            }

            @Override
            public void setRequestHeaders(Header[] requestHeaders) {

            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void setUseSynchronousMode(boolean useSynchronousMode) {

            }

            @Override
            public boolean getUsePoolThread() {
                return false;
            }

            @Override
            public void setUsePoolThread(boolean usePoolThread) {

            }

            @Override
            public void onPreProcessResponse(ResponseHandlerInterface instance, HttpResponse response) {

            }

            @Override
            public void onPostProcessResponse(ResponseHandlerInterface instance, HttpResponse response) {

            }

            @Override
            public Object getTag() {
                return null;
            }

            @Override
            public void setTag(Object TAG) {

            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestData("http://10.100.52.41:8000", "{\"what\": \"reset\"}");

        //playSample(R.raw.ic_geiger);


//        TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//            Log.i("NOT GRANTED", "ERROR");
//            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("This app needs telephony access");
//            builder.setMessage("Please grant telep access so this app can detect beacons.");
//            builder.setPositiveButton(android.R.string.ok, null);
//            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                @Override
//                public void onDismiss(DialogInterface dialog) {
//                    requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
//                }
//            });
//            builder.show();
//            return;
//        }
//        String uuid = tManager.getDeviceId();
//        Log.i("deviceUUID", uuid);


        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.i("NOT GRANTED", "ERROR");
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                    }
                });
                builder.show();
            }
        }

        setContentView(R.layout.activity_main);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
         beaconManager.getBeaconParsers().add(new BeaconParser().
                //setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
                        setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));

         beaconManager.setForegroundScanPeriod(100);

        beaconManager.bind(this);

        ImageView imageView=(ImageView) findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.ic_scale);




        setDoseIndicatorValue(0);
        setTreskIntensity(1);
        setFirstPlayerStatus(false, true, "Dima");
        setSecondPlayerStatus(false, true, "Sasha");

        String uuid = Build.MODEL;
        if (uuid.startsWith("LG")) {
            this.dev_id = 0;
            this.player_name = "Nick";
        } else if (uuid.startsWith("MI")) {
            this.dev_id = 1;
            this.player_name = "Yuri";
        } else {
            this.dev_id = 2;
            this.player_name = "Joel";
        }

        requestData(this.server_uri, String.format("{\"what\": \"join\",\"name\":\"%s\",\"team_id\":1}", this.player_name));

//        short buffer[] = new short[48000];
//        for (int i = 0; i < buffer.length; ++i) {
//            float phi = 3.1415926f * 2.f * (float)i / 48000.f;
//            buffer[i] = (short)(sin(phi * 1000.f) * 8191.f);
//        }
//        playSound(buffer);
        new CountDownTimer(5000000, 1000) {
            public void onTick(long millisUntilFinished) {

                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {

                        requestData(server_uri, String.format(Locale.US, "{\"what\": \"add_dose\",\"name\":\"%s\",\"delta_dose\":%f}", player_name, delta_dose / 2.f));

                    }
                };
                mainHandler.post(myRunnable);

//                requestData(server_uri, "{\"what\":\"update\"}");
//                String uuid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String uuid = Build.MODEL;
                Log.i("deviceUUID", uuid);
            }

            public void onFinish() {
                setTreskIntensity(3);
                setDoseIndicatorValue(1.0f);
//                setFirstPlayerStatus(true, true, "Petya");
//                setSecondPlayerStatus(true, false, "Katya");

                makeGameOver();

            }
        }.start();

//        ImageView firstImage=(ImageView) findViewById(R.id.firstPlayerImageView);
//        firstImage.setImageResource(R.drawable.ic_button1);
//
//        ImageView secondImage=(ImageView) findViewById(R.id.secondPlayerImageView);
//        secondImage.setImageResource(R.drawable.ic_button2);
//
//        TextView firstText=(TextView) findViewById(R.id.firstPlayerTextView);
//        firstText.setText("Mykola");
//
//        TextView secondText=(TextView) findViewById(R.id.secondPlayerTextView);
//        secondText.setText("Yuri");



    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
        requestData(this.server_uri, String.format("{\"what\": \"unjoin\",\"name\":\"%s\",\"team_id\":1}", this.player_name));
    }
    @Override
    public void onBeaconServiceConnect() {
        Log.i("onBeaconServiceConnect", "onBeaconServiceConnect");
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Beacon closestBeacon = null;
                    //Log.i("", "Found "+ beacons.size() + " beacons. Here they are:");
                    Iterator<Beacon> it = beacons.iterator();
                    for(int i=0; i<beacons.size(); i++) {
                        Beacon beacon = it.next();

//                        Log.i("BeaconFound", "Beacon detected: "+beacon.getDistance()+" with address: "+beacon.getBluetoothAddress());


                        if(beacon.getBluetoothAddress().toString().equals("D2:D7:7E:14:19:00")) {

                            beaconPulseReceived(beacon.getRssi(), (float)beacon.getDistance());

                        }



                    }
                    //Log.i("","=========SCAN========");
                    //Log.i(TAG, "Beacon ranged with distance: "+beacons.iterator().next().getDistance()+", and id "+beacons.iterator());
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
}
