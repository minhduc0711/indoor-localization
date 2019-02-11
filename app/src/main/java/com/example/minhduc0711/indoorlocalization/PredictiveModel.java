package com.example.minhduc0711.indoorlocalization;

import android.content.Context;

import org.dmg.pmml.FieldName;
import org.jpmml.android.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PredictiveModel {
    public static final int PB_FILE_EXT = 0;
    public static final int PMML_FILE_EXT = 1;

    private int modelFileExt;
    private TensorFlowInferenceInterface mInferenceInterface;
    private ModelEvaluator<?> mEvaluatorX;
    private ModelEvaluator<?> mEvaluatorY;

    public PredictiveModel(Context context, int modelFileExt, String modelName) {
        this.modelFileExt = modelFileExt;
        switch (modelFileExt) {
            case PB_FILE_EXT:
                mInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), modelName + ".pb");
                break;
            case PMML_FILE_EXT:
                try {
                    InputStream is = context.getAssets().open(modelName + "_x.pmml.ser");
                    mEvaluatorX = (ModelEvaluator<?>) EvaluatorUtil.createEvaluator(is);
                    is = context.getAssets().open(modelName + "_y.pmml.ser");
                    mEvaluatorY = (ModelEvaluator<?>) EvaluatorUtil.createEvaluator(is);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public float[] predict(float[] input) {
        float[] output = new float[2];
        switch (this.modelFileExt) {
            case PB_FILE_EXT:
                String[] outputNames = {"output_node0"};
                mInferenceInterface.feed("dense_1_input", input, 1, input.length);
                mInferenceInterface.run(outputNames);
                mInferenceInterface.fetch(outputNames[0], output);
                break;
            case PMML_FILE_EXT:
                int i = 0;
                Map<FieldName, FieldValue> inputMap = new HashMap<>();
                for (InputField inputField : mEvaluatorX.getInputFields()) {
                    FieldName fieldName = inputField.getName();
                    Object rawValue = input[i++];
                    FieldValue fieldValue = inputField.prepare(rawValue);
                    inputMap.put(fieldName, fieldValue);
                }
                Map<String, ?> resultsX = org.jpmml.evaluator.EvaluatorUtil.decode(mEvaluatorX.evaluate(inputMap));
                Map<String, ?> resultsY = org.jpmml.evaluator.EvaluatorUtil.decode(mEvaluatorY.evaluate(inputMap));
                output[0] = ((Double) resultsX.get("y")).floatValue();
                output[1] = ((Double) resultsY.get("y")).floatValue();
                break;
            default:
                throw new IllegalArgumentException();
        }
        return output;
    }
}
