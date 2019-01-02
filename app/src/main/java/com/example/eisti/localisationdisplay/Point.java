package com.example.eisti.localisationdisplay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


/**
 * Created by eisti on 19/05/17.
 */

public class Point {
    private int x;
    private int y;
    private float dir;

    public Point() {
        x=0;
        y=0;
    }

    public void setDir(int dir)
    {
        this.dir=dir;
    }

    public void draw(Canvas canvas, int maxX, int maxY, int w, int h)
    {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        float cx = convertYPos(w, maxY);
        float cy = convertXPos(h, maxX);
        canvas.drawCircle(cx, cy,15, paint);
    }

    public void resize(int wScreen, int hScreen) {

    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public float getDir() {
        return dir;
    }

    public void setDir(float dir) {
        this.dir = dir;
    }

    public float convertXPos(float height, float maxGrid) {
//        float newX = (this.x * maxGrid);
//        newX = newX/width;
//        return newX;
        return (this.x / maxGrid) * height;
    }

    public float convertYPos(float width, float maxGrid)
    {
        return (this.y / maxGrid) * width;
    }
}