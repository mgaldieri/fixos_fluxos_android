package br.com.dobralab.arquinterfaceandroid;

import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by mgaldieri on 11/11/15.
 */
public class FFLocationListener implements LocationListener {
    private String id;

    public FFLocationListener(String uuid) {
        id = uuid;
    }
    @Override
    public void onLocationChanged(Location location) {
        JSONObject userData = new JSONObject();
        JSONObject geoData = new JSONObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        try {
            geoData.put("uuid", id);
            geoData.put("lat", location.getLatitude());
            geoData.put("lon", location.getLongitude());
            geoData.put("dir", location.getBearing());
            geoData.put("speed", location.getSpeed());
            geoData.put("timestamp", timestamp);
            userData.put("userdata", geoData);
            Log.println(Log.DEBUG, "GeoData", userData.toString(4));

            LocationSender locSender = new LocationSender();
            locSender.execute(userData);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    protected class LocationSender extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected Void doInBackground(JSONObject... params) {
            JSONObject data = params[0];
            // TODO: put appkey in strings resource
            String jwt = generateJWT();
            String appKey = "b8e9e36879fadab0858fa9df8d5c75e9ab60d34cab4cbfa5ba983ee331107431";
            try {
                data.put("jwt", jwt);
                data.put("appkey", appKey);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Send POST data
            OkHttpClient client = new OkHttpClient();
            MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
            Request request = new Request.Builder()
                    .url("http://192.168.0.7:8989/api/v1/userdata")
//                    .header("jwt", jwt)
//                    .header("appkey", appKey)
                    .post(RequestBody.create(MEDIA_TYPE_JSON, data.toString()))
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    Log.println(Log.DEBUG, "POSTError", "Erro no request. Código "+response.code());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String generateJWT() {
            String returnString = "";
            Calendar calendar = Calendar.getInstance();
            // TODO: put appsecret in strings resource
            try {
                // create header
                JSONObject header = new JSONObject();
                header.put("typ", "JWT");
                header.put("alg", "HS256");

                // create claims
                JSONObject claims = new JSONObject();
                claims.put("iss", "fixosfluxos.org");
                claims.put("sub", "user_payload");
                claims.put("iat", (int)(calendar.getTimeInMillis() / 1000));
                calendar.add(Calendar.MINUTE, 5);
                claims.put("exp", (int)(calendar.getTimeInMillis()/1000));

                String headerString = Base64.encodeToString(header.toString().getBytes(), Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP);
                String claimsString = Base64.encodeToString(claims.toString().getBytes(), Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP);
                String jwtString = headerString+"."+claimsString;

                // create signature
                String appSecret = "eff832a28d914502d1d16c78f3006c18241d7b2c457dd237e23cc75249fd6b5bed834d02fd4468f7a73d0619d1a2bf83569cb1b10f44be7d5693deb2de6962ec";
                SecretKeySpec keySpec = new SecretKeySpec(appSecret.getBytes(), "HmacSHA256");
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(keySpec);
                byte[] result = mac.doFinal(jwtString.getBytes());

                String sigString = Base64.encodeToString(result, Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP);

                returnString += headerString+"."+claimsString+"."+sigString;

            } catch (JSONException|NoSuchAlgorithmException|InvalidKeyException e) {
                e.printStackTrace();
            }
            return returnString;
        }
    }
}
