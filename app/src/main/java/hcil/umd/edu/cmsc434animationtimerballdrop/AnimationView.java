package hcil.umd.edu.cmsc434animationtimerballdrop;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by jonf on 12/6/2016.
 */

public class AnimationView extends View {
    public static float MAX_VELOCITY = 100;
    public static float DEFAULT_BALL_RADIUS = 15;
    private static final Random _random = new Random();

    private Paint _paintText = new Paint();
    private Paint _paintTrail = new Paint();

    private int _desiredFramesPerSecond = 40;

    public ArrayList<Ball> balls = new ArrayList<Ball>();

    // This is for measuring frame rate, you can ignore
    private float _actualFramesPerSecond = -1;
    private long _startTime = -1;
    private int _frameCnt = 0;

    //https://developer.android.com/reference/java/util/Timer.html
    private Timer _timer = new Timer("AnimationView");

    public AnimationView(Context context) {
        super(context);
        init(null, null, 0);
    }

    public AnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public AnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr) {
        _paintTrail.setStyle(Paint.Style.FILL);
        _paintTrail.setAntiAlias(true);

        _paintText.setColor(Color.BLACK);
        _paintText.setTextSize(40f);


        // https://developer.android.com/referecance/java/util/Timer.html#scheduleAtFixedRate(java.util.TimerTask, long, long)
        // 60 fps will have period of 16.67
        // 40 fps will have period of 25
        long periodInMillis = 1000 / _desiredFramesPerSecond;
        _timer.schedule(new AnimationTimerTask(this), 0, periodInMillis);
    }

    @Override
    public void onDraw(Canvas canvas) {
        // start time helps measure fps calculations
        if(_startTime == -1) {
            _startTime = SystemClock.elapsedRealtime();
        }
        _frameCnt++;

        super.onDraw(canvas);

        synchronized (this.balls) {
            for(Ball ball : this.balls){
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
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        long startTime = SystemClock.elapsedRealtime();

        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();

        switch(motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                float randomXVelocity = _random.nextFloat() * MAX_VELOCITY;
                float randomYVelocity = randomXVelocity;

                //setup random direction
                randomXVelocity = _random.nextFloat() < 0.5f ? randomXVelocity : -1 * randomXVelocity;
                randomYVelocity = _random.nextFloat() < 0.5f ? randomYVelocity : -1 * randomYVelocity;

                Ball ball = new Ball(DEFAULT_BALL_RADIUS, curTouchX, curTouchY, randomXVelocity, randomYVelocity, getRandomOpaqueColor());
                synchronized (this.balls) {
                    this.balls.add(ball);
                }

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

        public Ball(float radius, float xLoc, float yLoc, float xVel, float yVel, int color){
            this.radius = radius;
            this.xLocation = xLoc;
            this.yLocation = yLoc;
            this.xVelocity = xVel;
            this.yVelocity = yVel;
            this.paint.setColor(color);
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
    }

    //TimerTask: https://developer.android.com/reference/java/util/TimerTask.html
    class AnimationTimerTask extends TimerTask {

        private AnimationView _animationView;
        private long _lastTimeInMs = -1;

        public AnimationTimerTask(AnimationView animationView){
            _animationView = animationView;
        }

        @Override
        public void run() {
            if(_lastTimeInMs == -1){
                _lastTimeInMs = SystemClock.elapsedRealtime();
            }
            long curTimeInMs = SystemClock.elapsedRealtime();

            synchronized (_animationView.balls){
                for(Ball ball : _animationView.balls){
                    ball.xLocation += ball.xVelocity * (curTimeInMs - _lastTimeInMs)/1000f;
                    ball.yLocation += ball.yVelocity * (curTimeInMs - _lastTimeInMs)/1000f;

                    // check x ball boundary
                    if(ball.getRight() >= getWidth() || ball.getLeft() <= 0){
                        // switch directions
                        ball.xVelocity = -1 * ball.xVelocity;
                    }

                    // check y ball boundary
                    if(ball.getBottom() >= getHeight() || ball.getTop() <= 0){
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


