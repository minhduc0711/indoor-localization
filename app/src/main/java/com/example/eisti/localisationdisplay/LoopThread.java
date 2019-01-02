package com.example.eisti.localisationdisplay;

import android.graphics.Canvas;

public class LoopThread extends Thread
{
    // on définit arbitrairement le nombre d'images par secondes à 30
    private final static int FRAMES_PER_SECOND = 30;

    // si on veut X images en 1 seconde, soit en 1000 ms,
    // on doit en afficher une toutes les (1000 / X) ms.
    private final static int SKIP_TICKS = 1000 / FRAMES_PER_SECOND;

    private final MapLoc view; // l'objet SurfaceView que nous verrons plus bas
    private boolean running = false; // état du thread, en cours ou non

    // constructeur de l'objet, on lui associe l'objet view passé en paramètre
    public LoopThread(MapLoc view) {
        this.view = view;
    }

    // défini l'état du thread : true ou false
    public void setRunning(boolean run) {
        running = run;
    }

    // démarrage du thread
    @Override
    public void run()
    {
        // déclaration des temps de départ et de pause
        long startTime;
        long sleepTime;

        // boucle tant que running est vrai
        // il devient faux par setRunning(false), notamment lors de l'arrêt de l'application
        // cf : surfaceDestroyed() dans GameView.java
        while (running)
        {

            // horodatage actuel
            startTime = System.currentTimeMillis();

            synchronized (view.getHolder()) {view.updatePosition();}

            // Rendu de l'image, tout en vérrouillant l'accès car nous
            // y accédons à partir d'un processus distinct
            Canvas c = null;
            try {
                c = view.getHolder().lockCanvas();
                synchronized (view.getHolder()) {view.doDraw(c);}
            }
            finally
            {
                if (c != null) {view.getHolder().unlockCanvasAndPost(c);}
            }

            // Calcul du temps de pause, et pause si nécessaire
            // afin de ne réaliser le travail ci-dessus que X fois par secondes
            sleepTime = SKIP_TICKS-(System.currentTimeMillis() - startTime);
            try {
                if (sleepTime >= 0) {sleep(sleepTime);}
            }
            catch (Exception e) {}
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } // boucle while (running)
    } // public void run()

} // class GameLoopThread