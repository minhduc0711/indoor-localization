package com.example.minhduc0711.indoorlocalization;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static JSONObject loadJSONFromAsset(String fname, Context context) {
        String json;
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

    public static float[] scaleFeatures(float[] input) {
        for (int i = 0; i < input.length - 1; i++) {
            input[i] /= 99;
        }
        input[input.length - 1] /= 360;
        return input;
    }
}
