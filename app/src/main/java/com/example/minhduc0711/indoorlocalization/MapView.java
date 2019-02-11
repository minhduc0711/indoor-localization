package com.example.minhduc0711.indoorlocalization;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import static com.example.minhduc0711.indoorlocalization.Utils.drawableToBitmap;

public class MapView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TRAIN_IDX_DICT_PATH = "train_idx_dict.json";
    private static final String MODEL_NAME = "poly";

    private final static float X_MAX = 20;
    private final static float Y_MAX = 40;

    private WifiManager mWifiManager;
    private PositionIndicator mPositionIndicator;
    private Bitmap arrowIcon;

    private SurfaceHolder mSurfaceHolder;
    private Canvas mCanvas;
    private Thread mDrawThread;
    private boolean threadRunning;

    private PredictiveModel mPredictiveModel;
    private JSONObject trainIndexDict;

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);

        // Get SurfaceHolder object.
        mSurfaceHolder = getHolder();
        // Add current object as the callback listener.
        mSurfaceHolder.addCallback(this);

        arrowIcon = drawableToBitmap(getResources().getDrawable(R.drawable.ic_navigation_arrow));
        mPositionIndicator = new PositionIndicator();
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mPredictiveModel = new PredictiveModel(context, PredictiveModel.PMML_FILE_EXT, MODEL_NAME);
        trainIndexDict = Utils.loadJSONFromAsset(TRAIN_IDX_DICT_PATH, context);
    }

//    public MapView(Context context) {
//        super(context);
//        setFocusable(true);
//
//        // Get SurfaceHolder object.
//        mSurfaceHolder = getHolder();
//        // Add current object as the callback listener.
//        mSurfaceHolder.addCallback(this);
//
//        arrowIcon = drawableToBitmap(getResources().getDrawable(R.drawable.ic_navigation_arrow));
//        mPositionIndicator = new PositionIndicator();
//        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//
//        mPredictiveModel = new PredictiveModel(context, MODEL_NAME);
//        trainIndexDict = Utils.loadJSONFromAsset(TRAIN_IDX_DICT_PATH, context);
//    }

    /**
     * Converts the wifi status to a feature vector
     */
    private float[] toFeatureVector(List<ScanResult> wifiResults) {
        float[] vec = new float[76];

        for (ScanResult scanResult : wifiResults) {
            String ssid = String.valueOf(scanResult.SSID);
            if (trainIndexDict.has(ssid)) {
                Integer rss = WifiManager.calculateSignalLevel(scanResult.level, 100);
                try {
                    vec[trainIndexDict.getInt(ssid)] = rss;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        vec[vec.length - 1] = mPositionIndicator.getAngle();
        vec = Utils.scaleFeatures(vec);
        return vec;
    }

    /**
     * Predicts the position then updates the indicator
     */
    private void updatePositionIndicator() {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            List<ScanResult> wifiResults = mWifiManager.getScanResults();
            float[] input = toFeatureVector(wifiResults);
            float[] output = mPredictiveModel.predict(input);
            Log.d("output", Arrays.toString(output));

            // INVERTED X AND Y !!!!
            mPositionIndicator.update(Math.round(output[1]), Math.round(output[0]));
        }
    }

    /**
     * Updates the z-axis orientation angle when the sensor detects a change
     */
    public void updateOrientation(int angle) {
        mPositionIndicator.setAngle(angle);
    }

    /**
     * Performs the actual drawing on the SurfaceView
     */
    private void doDraw() {
        updatePositionIndicator();

        mCanvas = mSurfaceHolder.lockCanvas();

        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        mPositionIndicator.draw(mCanvas, arrowIcon, getWidth(), getHeight());

        mSurfaceHolder.unlockCanvasAndPost(mCanvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mDrawThread = new Thread(this);
        threadRunning = true;
        mDrawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        threadRunning = false;
    }

    @Override
    public void run() {
        while (threadRunning) {
            long startTime = System.currentTimeMillis();

            doDraw();

            long endTime = System.currentTimeMillis();
            long deltaTime = endTime - startTime;

            if(deltaTime < 200)
            {
                try {
                    Thread.sleep(200 - deltaTime);
                }
                catch (InterruptedException ex) {
                    Log.e("THREAD", ex.getMessage());
                }
            }
        }
    }

    private class PositionIndicator {
        private int xPos;
        private int yPos;
        private int angle;

        public void update(int xPos, int yPos) {
            this.xPos = xPos;
            this.yPos = yPos;
        }

        public int getAngle() {
            return angle;
        }

        public void setAngle(int angle) {
            this.angle = angle;
        }

        public void draw(Canvas canvas, Bitmap icon, float viewWidth, float viewHeight) {
            float[] newCoordinates = scaleCoordinates(viewWidth, viewHeight);
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.postTranslate(-icon.getWidth() / 2, -icon.getHeight() / 2); // Centers image
            matrix.postRotate(angle);
            matrix.postTranslate(newCoordinates[0], newCoordinates[1]);
            canvas.drawBitmap(icon, matrix, null);
        }

        private float[] scaleCoordinates(float width, float height) {
            float[] res = new float[2];
            res[0] = (xPos / X_MAX) * width;
            res[1] = (yPos / Y_MAX) * height;
            return res;
        }
    }
}
