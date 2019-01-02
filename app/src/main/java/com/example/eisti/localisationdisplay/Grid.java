package com.example.eisti.localisationdisplay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class Grid extends View {

    private int rowsCount = 8;
    private int columnsCount = 8;

    public Grid(Context context) {
        super(context);
    }

    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        int height = getHeight()-400;
        int width = getWidth()-400;
        for (int i = 0; i < rowsCount; ++i) {
            canvas.drawLine(200, height / rowsCount * (i + 1), width, height / rowsCount * (i + 1), paint);
        }
        for (int i = 0; i < columnsCount; ++i) {
            canvas.drawLine(width / columnsCount * (i + 1), 200, width / columnsCount * (i + 1), height, paint);
        }
    }
}