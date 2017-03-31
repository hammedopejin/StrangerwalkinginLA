package com.example.hammedopejin.strangerwalkinginla;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

public class MainActivity extends Activity {

        // Our object to handle the View
        private ParallaxView parallaxView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Get a Display object to access screen details
            Display display = getWindowManager().getDefaultDisplay();

            // Load the resolution into a Point object
            Point resolution = new Point();
            display.getSize(resolution);

            // And finally set the view for our game
            parallaxView = new ParallaxView(this, resolution.x, resolution.y);

            // Make our parallaxView the view for the Activity
            setContentView(parallaxView);

        }

        public class ParallaxView extends SurfaceView implements Runnable {

            ArrayList<Background> backgrounds;
            private volatile boolean running;
            private Thread gameThread = null;

            // For drawing
            private Paint paint;
            private Canvas canvas;
            private SurfaceHolder ourHolder;

            // Holds a reference to the Activity
            Context context;

            // Control the fps
            long fps =60;

            // Screen resolution
            int screenWidth;
            int screenHeight;

            ////////////////////////////////////
            // Declare an object of type Bitmap
            Bitmap bitmapBob;

            // Bob starts off not moving
            boolean isMoving = false;

            // He can walk at 150 pixels per second
            float walkSpeedPerSecond = 250;

            // He starts 10 pixels from the left
            float bobXPosition = 10;

            // These next two values can be anything you like
            // As long as the ratio doesn't distort the sprite too much
            private int frameWidth = 100;
            private int frameHeight = 50;

            // How many frames are there on the sprite sheet?
            private int frameCount = 5;

            // Start at the first frame - where else?
            private int currentFrame = 0;

            // What time was it when we last changed frames
            private long lastFrameChangeTime = 0;

            // How long should each frame last
            private int frameLengthInMilliseconds = 100;

            // A rectangle to define an area of the
            // sprite sheet that represents 1 frame
            private Rect frameToDraw = new Rect(
                    0,
                    0,
                    frameWidth,
                    frameHeight);

            // A rect that defines an area of the screen
            // on which to draw
            RectF whereToDraw = new RectF(
                    bobXPosition, 0,
                    bobXPosition + frameWidth,
                    frameHeight);

            ParallaxView(Context context, int screenWidth, int screenHeight) {
                super(context);

                this.context = context;

                this.screenWidth = screenWidth;
                this.screenHeight = screenHeight;

                // Initialize our drawing objects
                ourHolder = getHolder();
                paint = new Paint();

                // Initialize our array list
                backgrounds = new ArrayList<>();

                //load the background data into the Background objects and
                // place them in our GameObject arraylist

                backgrounds.add(new Background(
                        this.context,
                        screenWidth,
                        screenHeight,
                        "skyline",  0, 80, 50));

                backgrounds.add(new Background(
                        this.context,
                        screenWidth,
                        screenHeight,
                        "grass",  70, 110, 200));

                // Load Bob from his .png file
                bitmapBob = BitmapFactory.decodeResource(this.getResources(), R.drawable.bob);

                // Scale the bitmap to the correct size
                // We need to do this because Android automatically
                // scales bitmaps based on screen density
                bitmapBob = Bitmap.createScaledBitmap(bitmapBob,
                        frameWidth * frameCount,
                        frameHeight,
                        false);

            }

            private void drawBackground(int position) {

                // Make a copy of the relevant background
                Background bg = backgrounds.get(position);

                // define what portion of images to capture and
                // what coordinates of screen to draw them at

                // For the regular bitmap
                Rect fromRect1 = new Rect(0, 0, bg.width - bg.xClip, bg.height);
                Rect toRect1 = new Rect(bg.xClip, bg.startY, bg.width, bg.endY);

                // For the reversed background
                Rect fromRect2 = new Rect(bg.width - bg.xClip, 0, bg.width, bg.height);
                Rect toRect2 = new Rect(0, bg.startY, bg.xClip, bg.endY);

                //draw the two background bitmaps
                if (!bg.reversedFirst) {
                    canvas.drawBitmap(bg.bitmap, fromRect1, toRect1, paint);
                    canvas.drawBitmap(bg.bitmapReversed, fromRect2, toRect2, paint);
                } else {
                    canvas.drawBitmap(bg.bitmap, fromRect2, toRect2, paint);
                    canvas.drawBitmap(bg.bitmapReversed, fromRect1, toRect1, paint);
                }

            }


            @Override
            public void run() {

                while (running) {
                    long startFrameTime = System.currentTimeMillis();

                    update();

                    draw();

                    // Calculate the fps this frame
                    long timeThisFrame = System.currentTimeMillis() - startFrameTime;
                    if (timeThisFrame >= 1) {
                        fps = 1000 / timeThisFrame;
                    }
                }
            }

            private void update() {
                // Update all the background positions

                // If bob is moving (the player is touching the screen)
                // then move him to the right based on his target speed and the current fps.
                if(isMoving){

                    for (Background bg : backgrounds) {
                        bg.update(fps);
                    }


                    bobXPosition = bobXPosition + (walkSpeedPerSecond / fps);
                }


            }

            private void draw() {

                if (ourHolder.getSurface().isValid()) {
                    //First we lock the area of memory we will be drawing to
                    canvas = ourHolder.lockCanvas();

                    //draw a background color
                    canvas.drawColor(Color.argb(255, 0, 3, 70));

                    // Draw the background parallax
                    drawBackground(0);

                    // Draw bob
                    whereToDraw.set(80,
                            510,
                            frameWidth+80,
                            frameHeight+510);

                    getCurrentFrame();

                    canvas.drawBitmap(bitmapBob,
                            frameToDraw,
                            whereToDraw, paint);

                    // Draw the foreground parallax
                    drawBackground(1);

                    // Unlock and draw the scene
                    ourHolder.unlockCanvasAndPost(canvas);
                }
            }

            // Clean up our thread if the game is stopped
            public void pause() {
                running = false;
                try {
                    gameThread.join();
                } catch (InterruptedException e) {
                    // Error
                }
            }

            // Make a new thread and start it
            // Execution moves to our run method
            public void resume() {
                running = true;
                gameThread = new Thread(this);
                gameThread.start();
            }

            public void getCurrentFrame(){

                long time  = System.currentTimeMillis();
                if(isMoving) {// Only animate if bob is moving
                    if ( time > lastFrameChangeTime + frameLengthInMilliseconds) {
                        lastFrameChangeTime = time;
                        currentFrame ++;
                        if (currentFrame >= frameCount) {

                            currentFrame = 0;
                        }
                    }
                }
                //update the left and right values of the source of
                //the next frame on the spritesheet
                frameToDraw.left = currentFrame * frameWidth;
                frameToDraw.right = frameToDraw.left + frameWidth;

            }

            // The SurfaceView class implements onTouchListener
            // So we can override this method and detect screen touches.
            @Override
            public boolean onTouchEvent(MotionEvent motionEvent) {

                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                    // Player has touched the screen
                    case MotionEvent.ACTION_DOWN:

                        // Set isMoving so Bob is moved in the update method
                        isMoving = true;

                        break;

                    // Player has removed finger from screen
                    case MotionEvent.ACTION_UP:

                        // Set isMoving so Bob does not move
                        isMoving = false;
                        //paddle.setMovementState(paddle.STOPPED);
                        break;
                }

                return true;
            }


        }// End of ParallaxView

        // If the Activity is paused make sure to pause our thread
        @Override
        protected void onPause() {
            super.onPause();
            parallaxView.pause();
        }

        // If the Activity is resumed make sure to resume our thread
        @Override
        protected void onResume() {
            super.onResume();
            parallaxView.resume();
        }
    }

