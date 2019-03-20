package com.example.minhduc0711.indoorlocalization.models;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class TensorflowModel implements PredictiveModel {
    private TensorFlowInferenceInterface mInferenceInterface;

    public TensorflowModel(String fileName, Context context) {
        mInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), fileName);
    }

    @Override
    public float[] predict(float[] input) {
        float[] output = new float[2];

        String[] outputNames = {"output_node0"};
        mInferenceInterface.feed("dense_55_input", input, 1, input.length);
        mInferenceInterface.run(outputNames);
        mInferenceInterface.fetch(outputNames[0], output);

        return output;
    }
}
