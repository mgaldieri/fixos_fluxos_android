package br.com.dobralab.arquinterfaceandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private View mContentView;
    private View mControlsView;
    private boolean mVisible;

    private Thread geoThread;
    private LocationManager locManager;

    private Thread wsT;
    private final int sRate = 22000;
    private boolean isPlaying;

    public String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.about_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.about_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FullscreenActivity.this, AboutActivity.class));
            }
        });

        // Set Timer to periodically send geo position;
        String uuid = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new FFLocationListener(uuid));

        // Start ws thread
        wsT = new Thread(new Runnable() {
            @Override
            public void run() {
                // Start websocket connection
                final WSHandler wsHandler = new WSHandler(Looper.getMainLooper());
                String swHost = "ws://"+getString(R.string.socket_host)+"/"+getString(R.string.socket_endpoint);
                try {
                    WebSocket ws = new WebSocketFactory().createSocket(swHost);
                    ws.addListener(new WebSocketAdapter(){
                        Bundle msgBundle = new Bundle();
                        @Override
                        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                            Message msg = new Message();
                            msg.what = Constants.MSG_CONNECTED;
                            wsHandler.sendMessage(msg);
                        }

                        @Override
                        public void onConnectError(WebSocket websocket, WebSocketException exception) {
                            Message msg = new Message();
                            msg.what = Constants.MSG_CONNECTION_ERROR;
                            msgBundle.putString("error", exception.toString());
                            msg.setData(msgBundle);
                            wsHandler.sendMessage(msg);
                        }

                        @Override
                        public void onTextMessage(WebSocket websocket, String text) {
                            Message msg = new Message();
                            msg.what = Constants.MSG_TEXT_MESSAGE;
                            msgBundle.putString("json", text);
                            msg.setData(msgBundle);
                            wsHandler.sendMessage(msg);
                        }
                    });
                    ws.connectAsynchronously();
                } catch (IOException e) {
                    Message msg = new Message();
                    msg.what = Constants.MSG_IO_ERROR;
                    wsHandler.sendMessage(msg);
                }
            }
        });
        wsT.start();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void handleJSON(JSONObject data) throws JSONException {
//        Log.println(Log.DEBUG, "WebSocketMessage", data.toString(4));
    }

    protected class WSHandler extends Handler {
        public WSHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case Constants.MSG_IO_ERROR:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Erro de conexão", Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                case Constants.MSG_CONNECTED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.println(Log.DEBUG, "WebSocket", "CONNECTED");
                        }
                    });
                    break;
                case Constants.MSG_CONNECTION_ERROR:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.println(Log.DEBUG, "WebSocket", "CONNECTION ERROR!!!");
                            Log.println(Log.DEBUG, "WebSocketError", ""+msg.getData().getString("error"));
                        }
                    });
                    break;
                case Constants.MSG_TEXT_MESSAGE:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.println(Log.DEBUG, "WebSocket", "MESSAGE RECEIVED");
                            try {
                                JSONObject data = new JSONObject(msg.getData().getString("json"));
                                handleJSON(data);
                            } catch (JSONException e) {
                                Log.println(Log.DEBUG, "WebSocketJSON", e.toString());
                            }
                        }
                    });
                    break;
            }
        }
    }
}
