package hcil.umd.edu.cmsc434animationtimerballdrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by jonf on 12/6/2016
 * Has paint trails
 */

public class AnimationView2 extends View {
    public static float MAX_VELOCITY = 1000;
    public static float MIN_VELOCITY = 10;
    public static float DEFAULT_BALL_RADIUS = 15;
    public static float MIN_BALL_RADIUS = 8;
    public static float MAX_BALL_RADIUS = 35;
    public static int DEFAULT_PAINT_TRAIL_ALPHA = 128;
    public static int DEFAULT_ERASING_OVERLAY_ALPHA = 20;
    public static long DEFAULT_DEATH_THRESHOLD_MILLISEC = 5 * 1000; //5 seconds
    public static int DEFAULT_BACKGROUND_COLOR = Color.WHITE;

    private static final Random _random = new Random();

    private Paint _paintText = new Paint();

    // for drawing an erasing overlay
    private Paint _paintErasingOverlay = new Paint();
    private boolean _enableErasingOverlay = true;

    // for eliminating old balls
    private long _deathThresholdMs = DEFAULT_DEATH_THRESHOLD_MILLISEC;
    private boolean _enableDeath = true;

    // for drawing paint trails
    private Paint _paintTrail = new Paint();
    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;

    private int _desiredFramesPerSecond = 40;

    // switched to linkedlist so penalty not so high for removing aged balls
    public LinkedList<Ball> balls = new LinkedList<Ball>();

    // This is for measuring frame rate, you can ignore
    private float _actualFramesPerSecond = -1;
    private long _startTime = -1;
    private int _frameCnt = 0;

    //https://developer.android.com/reference/java/util/Timer.html
    private Timer _timer = new Timer("AnimationView2");

    public AnimationView2(Context context) {
        super(context);
        init(context, null, 0);
    }

    public AnimationView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public AnimationView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr) {
        this.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        // If you don't do this, then the off screen canvas drawing will not work
        this.setDrawingCacheEnabled(true);
        _paintTrail.setStyle(Paint.Style.FILL);
        _paintTrail.setAntiAlias(true);

        _paintErasingOverlay.setStyle(Paint.Style.FILL);
        _paintErasingOverlay.setColor(ColorUtils.setAlphaComponent(this.getDrawingCacheBackgroundColor(), DEFAULT_ERASING_OVERLAY_ALPHA));

        int inverseBackgroundColor = ~this.getDrawingCacheBackgroundColor() | 0xFF000000;
        _paintText.setColor(inverseBackgroundColor);
        _paintText.setTextSize(40f);


        // https://developer.android.com/referecance/java/util/Timer.html#scheduleAtFixedRate(java.util.TimerTask, long, long)
        // 60 fps will have period of 16.67
        // 40 fps will have period of 25
        long periodInMillis = 1000 / _desiredFramesPerSecond;
        _timer.schedule(new AnimationTimerTask(this), 0, periodInMillis);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
            clearDrawing();
        }
    }

    public void clearDrawing() {

        synchronized (balls){
            balls.clear();
        }

        if (_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(this.getDrawingCacheBackgroundColor());
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
        }

        invalidate();
    }


    @Override
    public void onDraw(Canvas canvas) {
        // start time helps measure fps calculations
        if(_startTime == -1) {
            _startTime = SystemClock.elapsedRealtime();
        }
        _frameCnt++;

        super.onDraw(canvas);

        if(_offScreenBitmap  != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, null);

            if(_enableErasingOverlay){
                // makes the trails slowly disappear over time
                _offScreenCanvas.drawRect(0, 0, getWidth(), getHeight(), _paintErasingOverlay);
            }
        }

        int numBalls = -1;
        synchronized (this.balls) {
            numBalls = this.balls.size();
            for(Ball ball : this.balls){

                if(_offScreenBitmap  != null) {
                    // draw trail
                    _paintTrail.setColor(ColorUtils.setAlphaComponent(ball.paint.getColor(), DEFAULT_PAINT_TRAIL_ALPHA));
                    _offScreenCanvas.drawCircle(ball.xLocation, ball.yLocation, ball.radius, _paintTrail);
                }

                // draw current
                canvas.drawCircle(ball.xLocation, ball.yLocation, ball.radius, ball.paint);
            }
        }

        // The code below is about measuring and printing out fps calculations. You can ignore
        long endTime = SystemClock.elapsedRealtime();
        long elapsedTimeInMs = endTime - _startTime;
        if(elapsedTimeInMs > 1000){
            _actualFramesPerSecond = _frameCnt / (elapsedTimeInMs/1000f);
            _frameCnt = 0;
            _startTime = -1;
        }
        //MessageFormat: https://developer.android.com/reference/java/text/MessageFormat.html
        canvas.drawText(MessageFormat.format("fps: {0,number,#.#}", _actualFramesPerSecond), 5, 40, _paintText);
        canvas.drawText(MessageFormat.format("balls: {0}", numBalls), 5, 80, _paintText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // See: https://developer.android.com/training/gestures/multi.html
        int action = MotionEventCompat.getActionMasked(motionEvent);

        switch(action){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float curTouchX = motionEvent.getX(motionEvent.getPointerCount() - 1);
                float curTouchY = motionEvent.getY(motionEvent.getPointerCount() - 1);

                //setup random velocities. if x and y velocities are equal, trajectories will always be 45 degrees
                float randomXVelocity = Math.max(_random.nextFloat() * MAX_VELOCITY, MIN_VELOCITY);
                float randomYVelocity = Math.max(_random.nextFloat() * MAX_VELOCITY, MIN_VELOCITY);

                //setup random direction
                randomXVelocity = _random.nextFloat() < 0.5f ? randomXVelocity : -1 * randomXVelocity;
                randomYVelocity = _random.nextFloat() < 0.5f ? randomYVelocity : -1 * randomYVelocity;

                //setup random radius
                float radius = Math.max(_random.nextFloat() * MAX_BALL_RADIUS, MIN_BALL_RADIUS);

                Ball ball = new Ball(radius, curTouchX, curTouchY, randomXVelocity, randomYVelocity, getRandomOpaqueColor());
                synchronized (this.balls) {
                    this.balls.add(ball);
                }

                invalidate();
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    protected int getRandomOpaqueColor(){
        int r = _random.nextInt(255);
        int g = _random.nextInt(255);
        int b = _random.nextInt(255);
        //int b = 50 + (int)(_random.nextFloat() * (255-50));
        return Color.argb(255, r, g, b);
    }

    class Ball{

        public float radius = DEFAULT_BALL_RADIUS;
        public float xLocation = 0; // center x
        public float yLocation = 0; // center y
        public float xVelocity = 10;
        public float yVelocity = 10;
        public Paint paint = new Paint();
        private long _creationTimestampMs = -1;

        public Ball(float radius, float xLoc, float yLoc, float xVel, float yVel, int color){
            this.radius = radius;
            this.xLocation = xLoc;
            this.yLocation = yLoc;
            this.xVelocity = xVel;
            this.yVelocity = yVel;
            this.paint.setColor(color);
            _creationTimestampMs = SystemClock.elapsedRealtime();
        }

        public float getRight() {
            return this.xLocation + this.radius;
        }

        public float getLeft(){
            return this.xLocation - this.radius;
        }

        public float getTop(){
            return this.yLocation - this.radius;
        }

        public float getBottom(){
            return this.yLocation + this.radius;
        }

        public long getAge(){ return SystemClock.elapsedRealtime() - _creationTimestampMs; }
    }

    //TimerTask: https://developer.android.com/reference/java/util/TimerTask.html
    class AnimationTimerTask extends TimerTask {

        private AnimationView2 _animationView;
        private long _lastTimeInMs = -1;

        public AnimationTimerTask(AnimationView2 animationView){
            _animationView = animationView;
        }

        @Override
        public void run() {
            if(_lastTimeInMs == -1){
                _lastTimeInMs = SystemClock.elapsedRealtime();
            }
            long curTimeInMs = SystemClock.elapsedRealtime();

            synchronized (_animationView.balls){

                if(_animationView._enableDeath){
                    ListIterator<Ball> iter = _animationView.balls.listIterator();
                    while(iter.hasNext()){
                        Ball curBall = iter.next();
                        if(curBall.getAge() > _deathThresholdMs){
                            iter.remove();
                        }
                    }
                }

                for(Ball ball : _animationView.balls){
                    ball.xLocation += ball.xVelocity * (curTimeInMs - _lastTimeInMs)/1000f;
                    ball.yLocation += ball.yVelocity * (curTimeInMs - _lastTimeInMs)/1000f;

                    // check x ball boundary
                    if(ball.getRight() > getWidth() || ball.getLeft() < 0){
                        // switch directions
                        ball.xVelocity = -1 * ball.xVelocity;
                    }

                    // check y ball boundary
                    if(ball.getBottom() > getHeight() || ball.getTop() < 0){
                        // switch directions
                        ball.yVelocity = -1 * ball.yVelocity;
                    }

                }
            }

            _animationView.postInvalidate();
            _lastTimeInMs = curTimeInMs;
        }
    }
}
