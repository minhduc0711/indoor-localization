package com.example.minhduc0711.indoorlocalization;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class PredictiveModel {
    private TensorFlowInferenceInterface mInferenceInterface;

    public PredictiveModel(Context context, String modelPath) {
        mInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), modelPath);
    }

    public float[] predict(float[] input) {
        String[] outputNames = {"output_node0"};
        float[] output = new float[2];
        mInferenceInterface.feed("dense_1_input", input, 1, input.length);
        mInferenceInterface.run(outputNames);
        mInferenceInterface.fetch(outputNames[0], output);
        return output;
    }
}
