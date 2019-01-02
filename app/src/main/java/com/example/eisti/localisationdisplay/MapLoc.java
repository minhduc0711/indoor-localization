package com.example.eisti.localisationdisplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

// SurfaceView est une surface de dessin.
// référence : http://developer.android.com/reference/android/view/SurfaceView.html
public class MapLoc extends SurfaceView implements SurfaceHolder.Callback {
    private static final String MODEL_PATH = "model.pb";
    private static final String TRAIN_IDX_DICT_PATH = "train_idx_dict.json";

    public Point point;
    public Canvas mCanvas;
    public MyScaleGestures myScale;
    Context context;
    boolean alreadyShow = false;
    int rowsCount = 39;
    int columnsCount = 18;
    int navBarSize, statusBarSize;
    // déclaration de l'objet définissant la boucle principale de déplacement et de rendu
    private LoopThread loopThread;
    private float xForNumbers = 50;
    private float yForNumbers = 50;
    private int firstTime = 0;
    private float scaleFactor = 1;

    private JSONObject trainIdxDict;
    private float[] scaleValues;
    private TensorFlowInferenceInterface mInferenceInterface;

    // création de la surface de dessin
    public MapLoc(Context mContext) {
        super(mContext);
        getHolder().addCallback(this);
        navBarSize = 0;
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            navBarSize = resources.getDimensionPixelSize(resourceId);
        }

        statusBarSize = 0;
        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarSize = resources.getDimensionPixelSize(resourceId);
        }

        loopThread = new LoopThread(this);

        // création d'un objet "balle", dont on définira la largeur/hauteur
        // selon la largeur ou la hauteur de l'écran
        point = new Point();


        context = mContext;

        myScale = new MyScaleGestures(context, this);
        this.setOnTouchListener(myScale);

        mInferenceInterface = new TensorFlowInferenceInterface(mContext.getAssets(), MODEL_PATH);
        trainIdxDict = Utils.loadJSONFromAsset(TRAIN_IDX_DICT_PATH, mContext);
        JSONObject scaleJson = Utils.loadJSONFromAsset("scale.json", context);
        try {
            scaleValues = Utils.parseArray(scaleJson.getJSONArray("scale_array"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public void setxForNumbers(float xForNumbers) {
        this.xForNumbers = xForNumbers + 20;
    }

    public void setyForNumbers(float yForNumbers) {
        this.yForNumbers = yForNumbers + 20;
    }

    // Fonction qui "dessine" un écran de jeu
    public void doDraw(Canvas canvas) {
        if (canvas == null) {
            return;
        }

        canvas.save();
        // on efface l'écran, en blanc
        canvas.drawColor(Color.WHITE);


        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(35);

        int height = getHeight();
        int width = getWidth();
        for (int i = 0; i < rowsCount; ++i) {
            canvas.drawLine(0, height / rowsCount * (i + 1), width, height / rowsCount * (i + 1), paint);
            canvas.drawText(Integer.toString(i + 1), xForNumbers, height / rowsCount * (i + 1), paint);
        }
        for (int i = 0; i < columnsCount; ++i) {
            canvas.drawLine(width / columnsCount * (i + 1), 0, width / columnsCount * (i + 1), height, paint);
            canvas.drawText(Integer.toString(i + 1), width / columnsCount * (i + 1), yForNumbers, paint);
        }

        // on dessine la balle
        point.draw(canvas, rowsCount, columnsCount, width, height);

        paint.setTextSize(25);
//        canvas.drawText("x = " + Float.toString(point.convertXPos(width, columnsCount)), 15, 50, paint);
//        canvas.drawText("y = " + Float.toString(point.convertYPos(height, rowsCount)), 15, 80, paint);
        canvas.drawText("x = " + Float.toString(point.getX()), 15, 50, paint);
        canvas.drawText("y = " + Float.toString(point.getY()), 15, 80, paint);
        canvas.drawText("Dir = " + Float.toString(point.getDir()), 18, 110, paint);

        canvas.restore();
        mCanvas = canvas;
    }


    // Fonction obligatoire de l'objet SurfaceView
    // Fonction appelée immédiatement après la création de l'objet SurfaceView
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // création du processus GameLoopThread si cela n'est pas fait
        if (loopThread.getState() == Thread.State.TERMINATED) {
            loopThread = new LoopThread(this);
        }
        loopThread.setRunning(true);
        loopThread.start();
    }

    // Fonction obligatoire de l'objet SurfaceView
    // Fonction appelée juste avant que l'objet ne soit détruit.
    // on tente ici de stopper le processus de gameLoopThread
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        boolean retry = true;
        loopThread.setRunning(false);
        while (retry) {
            try {
                loopThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }


    // Update position
    public void updatePosition() {
        ArrayList<Integer> listRss = new ArrayList<>();
        ArrayList<String> listWifi = new ArrayList<>();

        final WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int state = wifi.getWifiState();
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            List<ScanResult> results = wifi.getScanResults();

            Date dt = new Date();
            int hours = dt.getHours();
            int minutes = dt.getMinutes();
            int seconds = dt.getSeconds();
            Date todayDate = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String curDate = formatter.format(todayDate);
            String curTime = hours + ":" + minutes + ":" + seconds;

            predict(toFeatureVector(results));
            alreadyShow = false;
        } else {
            if (!alreadyShow) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(context.getApplicationContext(), "Unable to find position, connect your WIFI", Toast.LENGTH_LONG);
                        toast.show();
                    }
                });
                point.setX(-10);
                point.setY(-10);
                alreadyShow = true;
            }
        }
    }

    public void predict(float[] input) {
//        DisplayMetrics metrics = new DisplayMetrics();
//        ((Activity) getContext()).getWindowManager()
//                .getDefaultDisplay()
//                .getMetrics(metrics);
//        int height = metrics.heightPixels;
//        int width = metrics.widthPixels;
//        Random r = new Random();
//        int x = r.nextInt(height);
//        Random r1 = new Random();
//        int y = r1.nextInt(width);
//        point.setX(y);
//        point.setY(x);

        String[] outputNames = {"output_node0"};
        float[] output = new float[2];
        mInferenceInterface.feed("dense_1_input", input, 1, input.length);
        mInferenceInterface.run(outputNames);
        mInferenceInterface.fetch(outputNames[0], output);
        point.setX((int) Math.round(output[0]));
        point.setY((int) Math.round(output[1]));
    }

    float[] toFeatureVector(List<ScanResult> results) {
        float[] vec = new float[76];

        for (ScanResult scanResult : results) {
            String ssid = String.valueOf(scanResult.SSID);
            if (trainIdxDict.has(ssid)) {
                Integer rss = WifiManager.calculateSignalLevel(scanResult.level, 100);
                try {
                    vec[trainIdxDict.getInt(ssid)] = rss;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        vec[vec.length - 1] = point.getDir();
//        Arrays.fill(vec, 50);
        vec = Utils.scaleFeatures(vec, scaleValues);
        Log.d("vec", Arrays.toString(vec));
        return vec;
    }

    // Fonction obligatoire de l'objet SurfaceView
    // Fonction appelée à la CREATION et MODIFICATION et ONRESUME de l'écran
    // nous obtenons ici la largeur/hauteur de l' écran en pixels
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h) {
        point.resize(w, h); // on définit la taille de la balle selon la taille de l'écran
    }
} // class GameView