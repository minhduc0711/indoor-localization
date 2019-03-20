package com.example.minhduc0711.indoorlocalization.models;

import android.content.Context;

import org.dmg.pmml.FieldName;
import org.jpmml.android.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ScikitModel implements PredictiveModel {
    private ModelEvaluator<?> mEvaluatorX;
    private ModelEvaluator<?> mEvaluatorY;

    public ScikitModel(String fileNameX, String fileNameY, Context context) {
        try {
            InputStream is = context.getAssets().open(fileNameX);
            mEvaluatorX = (ModelEvaluator<?>) EvaluatorUtil.createEvaluator(is);
            is = context.getAssets().open(fileNameY);
            mEvaluatorY = (ModelEvaluator<?>) EvaluatorUtil.createEvaluator(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public float[] predict(float[] input) {
        float[] output = new float[2];

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

        return output;
    }
}
