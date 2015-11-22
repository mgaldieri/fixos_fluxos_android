package br.com.mgaldieri.fixosfluxos;

import java.util.Random;

/**
 * Created by mgaldieri on 12/11/15.
 */
public class Utils {
    public static float mapFloat(float value, float fromLow, float fromHigh, float toLow, float toHigh) {
        return (value-fromLow) * (toHigh-toLow) / (fromHigh-fromLow) + toLow;
    }

    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }
}
