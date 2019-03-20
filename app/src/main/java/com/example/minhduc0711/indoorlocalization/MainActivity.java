package com.example.minhduc0711.indoorlocalization;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.example.minhduc0711.indoorlocalization.models.PredictiveModel;
import com.example.minhduc0711.indoorlocalization.models.ScikitModel;
import com.example.minhduc0711.indoorlocalization.models.TensorflowModel;

public class MainActivity extends AppCompatActivity implements SensorEventListener, AdapterView.OnItemSelectedListener {
    private Spinner mModelSpinner;
    private MapView mMapView;

    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION},
                2);

        mMapView = findViewById(R.id.map_view);
        mMapView.setZOrderOnTop(true);
        mMapView.getHolder().setFormat(PixelFormat.TRANSPARENT);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

//        mModelSpinner = findViewById(R.id.model_spinner);
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
//                R.array.models_array, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        mModelSpinner.setAdapter(adapter);
//        mModelSpinner.setOnItemSelectedListener(this);
//        mModelSpinner.setSelection(0);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selected = (String) parent.getItemAtPosition(position);
        PredictiveModel newModel;
        switch (selected) {
            case "Neural network":
                newModel = new TensorflowModel("neural_net.pb", this);
                break;
            case "Decision tree":
                newModel = new ScikitModel("decision_tree_x.pmml.ser", "decision_tree_y.pmml.ser", this);
                break;
            default:
                throw new RuntimeException();
        }
        mMapView.initializeModel(newModel);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mMapView.updateOrientation(Math.round(event.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
