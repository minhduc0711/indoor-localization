package com.example.minhduc0711.indoorlocalization;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.example.minhduc0711.indoorlocalization.models.PredictiveModel;
import com.example.minhduc0711.indoorlocalization.models.ScikitModel;
import com.example.minhduc0711.indoorlocalization.models.TensorflowModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MapView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TRAIN_IDX_DICT_PATH = "cleaned_idx_dict.json";

    private final static float X_MAX = 20;
    private final static float Y_MAX = 40;

    private WifiManager mWifiManager;
    private PositionIndicator mPositionIndicator;
    private float mDeviceOrientation;

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

        mPositionIndicator = new PositionIndicator();

        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            Toast.makeText(getContext(), "Enabling Wifi...", Toast.LENGTH_LONG).show();
            mWifiManager.setWifiEnabled(true);
        }

        initializeModel(new TensorflowModel("neural_net.pb", context));
//        initializeModel(new ScikitModel("decision_tree_x.pmml.ser", "decision_tree_y.pmml.ser", context));
        trainIndexDict = Utils.loadJSONFromAsset(TRAIN_IDX_DICT_PATH, context);

        mDrawThread = new DrawThread();
    }

    public void initializeModel(PredictiveModel newModel) {
        mPredictiveModel = newModel;
    }

    /**
     * Converts the wifi status to a feature vector
     */
    private float[] toFeatureVector(List<ScanResult> wifiResults) {
        float[] vec = new float[trainIndexDict.length() + 1];

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
        vec[vec.length - 1] = mDeviceOrientation;
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

            mPositionIndicator.update(Math.round(output[0]), Math.round(output[1]));
        }
    }

    /**
     * Updates the z-axis orientation angle when the sensor detects a change
     */
    public void updateOrientation(int angle) {
        mDeviceOrientation = angle;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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

    private class DrawThread extends Thread {
        @Override
        public void run() {
            while (threadRunning) {
                long startTime = System.currentTimeMillis();

                updatePositionIndicator();

                mCanvas = mSurfaceHolder.lockCanvas();

                mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                mPositionIndicator.draw(mCanvas, getWidth(), getHeight());

                Paint textPaint = new Paint(Color.BLACK);
                textPaint.setTextSize(30);
                textPaint.setFakeBoldText(true);
                mCanvas.drawText("x pos: " + mPositionIndicator.xPos, 20, 50, textPaint);
                mCanvas.drawText("y pos: " + mPositionIndicator.yPos, 20, 90, textPaint);
                mCanvas.drawText("Orientation: " + mDeviceOrientation, 20, 140, textPaint);

                mSurfaceHolder.unlockCanvasAndPost(mCanvas);

                long endTime = System.currentTimeMillis();
                long deltaTime = endTime - startTime;

                if (deltaTime < 200)
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
    }

    private class PositionIndicator {
        private Paint paint;
        private int xPos;
        private int yPos;

        PositionIndicator() {
            paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
        }

        public void update(int xPos, int yPos) {
            this.xPos = xPos;
            this.yPos = yPos;
        }

        public void draw(Canvas canvas, float viewWidth, float viewHeight) {
            float[] newCoordinates = scaleCoordinates(viewWidth, viewHeight);
            canvas.drawCircle(newCoordinates[0], newCoordinates[1], 15, new Paint());
        }

        private float[] scaleCoordinates(float width, float height) {
            float[] res = new float[2];
            res[0] = (xPos / X_MAX) * width;
            res[1] = (yPos / Y_MAX) * height;
            return res;
        }
    }
}
