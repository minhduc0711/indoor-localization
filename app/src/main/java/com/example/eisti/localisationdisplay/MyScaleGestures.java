package com.example.eisti.localisationdisplay;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class MyScaleGestures implements View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {
    private View view;
    private ScaleGestureDetector gestureScale;
    private float scaleFactor = 1;
    private boolean inScale;
    float dX, dY, diffX, diffY;
    MapLoc mMap;
    Context mContext;
    private boolean firstTime;
    int navBarSize,statusBarSize;

    public MyScaleGestures(Context c, MapLoc map)
    {
        gestureScale = new ScaleGestureDetector(c, this);
        mMap=map;
        mContext=c;
        firstTime=true;

        navBarSize=0;
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

    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        this.view = view;
        gestureScale.onTouchEvent(event);

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:


                float newX= event.getRawX() + dX;
                float newY= event.getRawY() + dY;

                view.animate()
                        .x(newX)
                        .y(newY)
                        .setListener(null)
                        .setStartDelay(0)
                        .setDuration(0)
                        .start();

                testPos(view,newX,newY,false);

                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        scaleFactor *= detector.getScaleFactor();
        scaleFactor = (scaleFactor < 1 ? 1 : scaleFactor); // prevent our view from becoming too small //
        scaleFactor = (scaleFactor >4 ? 4 : scaleFactor); // prevent our view from becoming too large //
        scaleFactor = ((float)((int)(scaleFactor * 100))) / 100; // Change precision to help with jitter when user just rests their fingers //
        view.setScaleX(scaleFactor);
        view.setScaleY(scaleFactor);

        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        mMap.setxForNumbers(((-pos[0])/scaleFactor)+20);
        mMap.setyForNumbers(((-pos[1])/scaleFactor)+20);
        testPos(view,0,0,true);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        inScale = true;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) { inScale = false; }

    private void testPos(View view, float newX, float newY, boolean fromZoom)
    {

        int[] pos = new int[2];
        view.getLocationInWindow(pos);

        if (fromZoom)
        {
            newX=pos[0];
            newY=pos[1];
        }



        pos[0]=pos[0]-2;
        pos[1]=pos[1]-71;
        if (pos[0]>0)
        {
            view.animate()
                    .x(newX-(pos[0]))
                    .setListener(null)
                    .setStartDelay(0)
                    .setDuration(0)
                    .start();
        }

        if (pos[1]>0-statusBarSize)
        {
            view.animate()
                    .y(newY-(pos[1])-statusBarSize)
                    .setListener(null)
                    .setStartDelay(0)
                    .setDuration(0)
                    .start();
        }

        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels+navBarSize-statusBarSize;

        if ((pos[0]+view.getWidth()*scaleFactor)<width)
        {

            float diff1 =width- (pos[0]+view.getWidth()*scaleFactor);
            view.animate()
                    .x(newX+diff1)
                    .setListener(null)
                    .setStartDelay(0)
                    .setDuration(0)
                    .start();
        }


        if ((pos[1]+view.getHeight()*scaleFactor)<height)
        {
            float diff2 = height - (pos[1]+view.getHeight()*scaleFactor);
            view.animate()
                    .y(newY+diff2)
                    .setDuration(0)
                    .start();

        }

        pos = new int[2];
        view.getLocationInWindow(pos);
        mMap.setxForNumbers(((-pos[0])/scaleFactor)+20);
        mMap.setyForNumbers(((-pos[1])/scaleFactor)+20);


    }





}
