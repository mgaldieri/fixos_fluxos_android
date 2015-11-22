package br.com.mgaldieri.fixosfluxos;

/**
 * fixos_fluxos android. Aplicativo de suporte à instalação interativa fixos_fluxos em exibição na mostra Arquinterface na Galeria Digital do SESI-SP
 * @author Mauricio de Camargo Galdieri
 * @custom.ra 20458437
 */

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public String uuid;

    private Thread wsT;
    private final int sRate = 22000;
    private boolean isPlaying;
    private PdUiDispatcher dispatcher;

    private PdService pdService = null;

    private final int MIN_RED = 1;  // #01
    private final int MAX_RED = 44; // #2C

    private final int  MIN_GREEN = 1;	// #01
    private final int MAX_GREEN = 32;	// #20

    private final int MIN_BLUE = 0;	    // #00
    private final int MAX_BLUE = 47;	// #2F

    private final float MIN_TRAFFIC = 0.0f;
    private final float MAX_TRAFFIC = 100.0f;

    private final float MIN_POLLUTION = 0.0f;
    private final float MAX_POLLUTION = 200.0f;

    private final float MIN_TEMPERATURE = 3.0f;
    private final float MAX_TEMPERATURE = 45.0f;

    private final float MIN_HUMIDITY = 5.0f;
    private final float MAX_HUMIDITY = 95.0f;

    private final float AMBIENT_MIN_DIFF = 10;
    private final float AMBIENT_MAX_DIFF = 20;

    private final int MIN_BUS_FREQ = 100;
    private final int MAX_BUS_FREQ = 200;
    private final float MIN_BUS_AMP = 0.0f;
    private final float MAX_BUS_AMP = 0.01f;

    private final int MIN_USER_TREMOLO = 3;
    private final int MAX_USER_TREMOLO = 10;
    private final int MIN_USER_FREQ = 800;
    private final int MAX_USER_FREQ = 1600;
    private final float MIN_USER_AMP = 0.0f;
    private final float MAX_USER_AMP = 0.01f;
    private final float MAX_USER_RATIO = 5;

    private final int MAX_BUSES = 5;
    private final int MAX_USERS = 5;
    private final int MAX_PLANES = 1;

    private HashMap<String, HashMap<String, Number>> busPool = new HashMap<>(MAX_BUSES);
    private ArrayList<Number> pdBusAvailable = new ArrayList<>(MAX_BUSES);
    private HashMap<String, HashMap<String, Number>> userPool = new HashMap<>(MAX_USERS);
    private ArrayList<Number> pdUserAvailable = new ArrayList<>(MAX_USERS);
    private HashMap<String, HashMap<String, Number>> planePool = new HashMap<>(MAX_PLANES);
    private ArrayList<Number> pdPlaneAvailable = new ArrayList<>(MAX_PLANES);

    private int ambientId;
    private float ambientValue;

    private final String DTAG = "CBLOG";
    protected static Manager manager;
    private Database database;

    private ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            try {
                initPd();
                loadPatches();
            } catch (IOException e) {
                Log.println(Log.ERROR, "PdError", "Could not open pd files");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        initDB();

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

        findViewById(R.id.mute_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.mute_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSound();
    }
});

        // Init LibPd
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);

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

//    @Override
//    protected void onStop() {
//        super.onStop();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(pdConnection);
    }

    private void initDB() {
        try {
            manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase("fixosfluxosdb");
        } catch (IOException|CouchbaseLiteException e) {
            Log.e(DTAG, "Erro abrindo banco de dados", e);
        }
    }

    private void initSystemServices() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (pdService == null) return;
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    start();
                } else {
                    pdService.stopAudio();
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void start() {
        if (!pdService.isRunning()) {
            Intent intent = new Intent(this, FullscreenActivity.class);
            pdService.startAudio(intent, R.drawable.icon, "fixos_fluxos", "Retornar ao aplicativo.");
        }
    }

    private void initPd() throws IOException {
        // Init audio glue
        int sampleRate = AudioParameters.suggestSampleRate();
        pdService.initAudio(sampleRate, 0, 2, 10.0f);
        initSystemServices();
        start();

        // Install the dispatcher
        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
    }

    private void loadPatches() throws IOException {
        File dir = getFilesDir();

        // Load ambient patch
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.ff_ambient), dir, true);
        File ambientFile = new File(dir, "ff_ambient.pd");
        ambientId = PdBase.openPatch(ambientFile.getAbsolutePath());
        PdBase.sendFloat(ambientId+"-amp", 0.0f);

        // Load user patches
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.ff_user), dir, true);
        File userFile  = new File(dir, "ff_user.pd");
        for (int i=0; i<MAX_USERS; i++) {
            int pdUserId = PdBase.openPatch(userFile);
            pdUserAvailable.add(pdUserId);
            PdBase.sendFloat(pdUserId + "-amp", 0.0f);
        }

        // Load bus patches
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.ff_bus), dir, true);
        File busFile  = new File(dir, "ff_bus.pd");
        for (int i=0; i<MAX_BUSES; i++) {
            int pdBusId = PdBase.openPatch(busFile);
            pdBusAvailable.add(pdBusId);
            PdBase.sendFloat(pdBusId + "-amp", 0.0f);
        }
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

    private void toggleSound() {
        if (pdService.isRunning()) {
            pdService.stopAudio();
        } else {
            pdService.startAudio();
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
        Document doc = database.createDocument();
        Map<String, Object> map = new HashMap<>();

        /*****
         * Handle weather data
         */
        int red = ((JSONObject)data.get("weather")).getInt("red");
        int green = ((JSONObject)data.get("weather")).getInt("green");
        int blue = ((JSONObject)data.get("weather")).getInt("blue");

        float percRed = Utils.mapFloat((float) red, MIN_RED, MAX_RED, 0, 1);
        float percGreen = Utils.mapFloat((float)green, MIN_GREEN, MAX_GREEN, 0, 1);
        float percBlue = Utils.mapFloat((float)blue, MIN_BLUE, MAX_BLUE, 0, 1);

        float weatherAvg = (percRed+percGreen+percBlue)/3;

        ambientValue = Utils.mapFloat(weatherAvg, 0, 1, AMBIENT_MIN_DIFF, AMBIENT_MAX_DIFF);
        PdBase.sendFloat(ambientId+"-diff", ambientValue);
        map.put("ambiente", ambientValue);

        /*****
         * Handle bus data
         */
        JSONObject buses = (JSONObject)data.get("busesFar");

        Iterator<String> nearIter = ((JSONObject)data.get("busesNear")).keys();
        Set<String> nearKeys = new HashSet<>();
        while (nearIter.hasNext()) {
            nearKeys.add(nearIter.next());
        }

        // Remove buses that had disappeared
        ArrayList<String> busToRemove = new ArrayList<>();
        Set<String> currentBusKeys = busPool.keySet();
        for (String key : currentBusKeys) {
            if (!buses.has(key)) {
                Number pdBusId = busPool.get(key).get("pdBusId");
                pdBusAvailable.add(pdBusId);
                busToRemove.add(key);
            }
        }
        for (String key : busToRemove) {
            buses.remove(key);
        }

        // Populate pool if we have available bus slots
        while (pdBusAvailable.size() > 0) {
            Iterator<String> busIter = buses.keys();
            while (busIter.hasNext()) {
                if (pdBusAvailable.size() == 0) break;
                String key = busIter.next();
                // If bus is not already in pool
                if (!busPool.containsKey(key)) {
                    // Add it
                    Number pdBusId = pdBusAvailable.remove(0);
                    int freq = Utils.randInt(MIN_BUS_FREQ, MAX_BUS_FREQ);
                    HashMap<String, Number> busData = new HashMap<>();
                    busData.put("pdBusId", pdBusId);
                    busData.put("freq", freq);
                    // Pool data
                    busPool.put(key, busData);
                    // Send freq to patch
                    if (nearKeys.contains(key)) freq *= 4;
                    PdBase.sendFloat((int) pdBusId + "-freq", (float) freq);
                }
            }
        }

        // Set pool amplitudes
        ArrayList<Float> busAmps = new ArrayList<>();
        for (String poolkey : busPool.keySet()) {
            float pos = (float)((JSONObject)buses.get(poolkey)).getDouble("position");
            float dist = Math.abs(0.5f-pos);
            float amp = Utils.mapFloat(dist, 0.0f, 0.5f, MIN_BUS_AMP, MAX_BUS_AMP);
            int pdBusId = (int) busPool.get(poolkey).get("pdBusId");
            PdBase.sendFloat(pdBusId+"-amp", amp);
            busAmps.add(amp);
        }

        // Silence unused slots
        for (Number pdBusId : pdBusAvailable) {
            PdBase.sendFloat(pdBusId+"-amp", 0.0f);
        }
        map.put("onibus", busAmps);

        /*****
         * Handle user data
         */
        JSONObject users = (JSONObject)data.get("appData");

        // Remove users that had disappeared
        ArrayList<String> userToRemove = new ArrayList<>();
        Set<String> currentUserKeys = userPool.keySet();
        for (String key : currentUserKeys) {
            if (!users.has(key)) {
                Number pdUserId = userPool.get(key).get("pdUserId");
                pdUserAvailable.add(pdUserId);
                userToRemove.add(key);
            }
        }
        for (String key : userToRemove) {
            users.remove(key);
        }


        // Populate pool if we have available user slots
        while (pdUserAvailable.size() > 0) {
            Iterator<String> userIter = users.keys();
            while (userIter.hasNext()) {
                String key = userIter.next();
                // If bus is not already in pool
                if (!userPool.containsKey(key)) {
                    // Add it
                    Number pdUserId = pdUserAvailable.remove(0);
                    int freq = Utils.randInt(MIN_USER_FREQ, MAX_USER_FREQ);
                    int tremolo = Utils.randInt(MIN_USER_TREMOLO, MAX_USER_TREMOLO);
                    HashMap<String, Number> userData = new HashMap<>();
                    userData.put("pdUserId", pdUserId);
                    userData.put("freq", freq);
                    userData.put("tremolo", tremolo);
                    // Pool data
                    userPool.put(key, userData);
                    // Send freq/tremolo to patch
                    PdBase.sendFloat(pdUserId + "-freq", (float) freq);
                    PdBase.sendFloat(pdUserId + "-tremolo", (float) tremolo);
                }
                if (pdUserAvailable.size() == 0) break;
            }
        }

        // Set pool amplitudes
        ArrayList<Float> userAmps = new ArrayList<>();
        for (String poolkey : userPool.keySet()) {
            float ratio = (float)((JSONObject)users.get(poolkey)).getDouble("ratio");
            float amp = Utils.mapFloat(ratio, 1.0f/MAX_USER_RATIO, MAX_USER_RATIO, MIN_USER_AMP, MAX_USER_AMP);
            int pdUserId = (int) userPool.get(poolkey).get("pdUserId");
            PdBase.sendFloat(pdUserId + "-amp", amp);
            userAmps.add(amp);
        }

        // Silence unused slots
        for (Number pdUserId : pdUserAvailable) {
            PdBase.sendFloat(pdUserId+"-amp", 0.0f);
        }
        map.put("users", userAmps);

        try {
            doc.putProperties(map);
        } catch (CouchbaseLiteException e) {
            Log.e(DTAG, "Erro gravando no banco de dados", e);
        }

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
