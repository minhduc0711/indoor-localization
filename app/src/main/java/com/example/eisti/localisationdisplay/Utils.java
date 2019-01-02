package com.example.eisti.localisationdisplay;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static JSONObject loadJSONFromAsset(String fname, Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fname);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
            return new JSONObject(json);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static float[] parseArray(JSONArray jsonArray) {
        float[] res = new float[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                res[i] = (float) jsonArray.getDouble(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public static float[] scaleFeatures(float[] input, float[] scaleValues) {
        for (int i = 0; i < input.length; i++) {
            input[i] /= scaleValues[i];
        }
        return input;
    }
}
