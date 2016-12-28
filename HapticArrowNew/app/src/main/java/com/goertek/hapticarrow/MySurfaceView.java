package com.goertek.hapticarrow;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Shader;
import android.os.Haptics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.goertek.hapticarrow.utils.FileUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.HAPTICS_SERVICE;

/**
 * Created by fili.zhang on 2016/9/23.
 * now, we play effect according to the rules
 * 1. the distance the pointY to startY
 * 2. up touch up,the mArrow will send up
 * 3. restore the low string restore
 */

public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static String TAG = "MySurfaceView";

    private final int CONSTANT_FPS = 20;
    private int CONST_BG_LINE_INTERVAL = 100;
    private int ARROW_FLY_LENGHT_MAX = 0;
    private int TARGET_MAX_DISTANCE = 0;

    //用于控制SurfaceView
    private SurfaceHolder mSurfaceHolder;
    //声明一个画笔
    private Paint mPaint;

    private Context mContext;
    //声明一条线程
    private Thread mThread = null;
    //线程消亡的标识位
    private boolean mRunningFlag;
    //声明一个画布
    private Canvas mCanvas;

    private Resources mResource = this.getResources();

    //声明屏幕的宽高
    private int mScreenWidth, mScreenHeight;
    private int range;

//    /**
//     * 记录两张背景图片时时更新的Y坐标
//     **/
//    private int mBitPosY0 = 0;

    private Bow mBow;
    private Arrow mArrow;
    private Target mTarget;
    private Background mBackground;

    private final int BUZZ_100 = 47;
    private final int BUZZ_80 = 48;
    private final int BUZZ_60 = 49;
    private final int BUZZ_40 = 50;
    private final int BUZZ_20 = 51;

    private Configuration mConfiguration;
    private List<Integer> mHapticIdArrowOutHeavy = new ArrayList<>();
    private List<Integer> mHapticIdArrowOutLight = new ArrayList<>();
    private List<Integer> mHapticIdArrowFlyHeavy = new ArrayList<>();
    private List<Integer> mHapticIdArrowFlyLight = new ArrayList<>();
    private List<Integer> mHapticIdArrowEnd = new ArrayList<>();
    private List<Integer> mHapticIdArrowDrag = new ArrayList<>();

    //定义游戏状态常量
    public static final int GAME_MENU = 0;//游戏菜单
    public static final int GAMEING = 1;//游戏中
    public static final int GAME_WIN = 2;//游戏胜利[NO]
    public static final int GAME_LOST = 3;//游戏失败
    public static final int GAME_PAUSE = -1;//游戏菜单
    public static final int GAME_FLY = 4;//游戏菜单
    public static final int GAME_MOVE = 5;
    public static final int GAME_OVER = 6;

    //当前游戏状态(默认初始在游戏菜单界面)
    public static int gameState = GAME_MENU;

    // Haptic
    private Haptics mHaptic;

    private void initGame() {
        // read the configuration file
        mConfiguration = FileUtil.ReadConfiguration(mContext);
        if (mConfiguration != null) {
            // override the default value;
            mHapticIdArrowFlyHeavy = mConfiguration.arrowFlyHeavyId;
            mHapticIdArrowFlyLight = mConfiguration.arrowFlyLightId;
            mHapticIdArrowEnd = mConfiguration.arrowEndId;
            mHapticIdArrowOutHeavy = mConfiguration.arrowOutHeavyId;
            mHapticIdArrowOutLight = mConfiguration.arrowOutLightId;
            mHapticIdArrowDrag = mConfiguration.arrowPrepareId;
        } else {
            mHapticIdArrowFlyHeavy.add(BUZZ_100);
            mHapticIdArrowFlyLight.add(BUZZ_60);
            mHapticIdArrowEnd.add(BUZZ_20);
            mHapticIdArrowOutHeavy.add(BUZZ_100);
            mHapticIdArrowOutLight.add(BUZZ_60);

            //{33, 30, 29, 32, 28, 31, 27, 14}
            mHapticIdArrowDrag.add(33);
            mHapticIdArrowDrag.add(30);
            mHapticIdArrowDrag.add(29);
            mHapticIdArrowDrag.add(32);
            mHapticIdArrowDrag.add(28);
            mHapticIdArrowDrag.add(31);
            mHapticIdArrowDrag.add(27);
            mHapticIdArrowDrag.add(14);
        }

        if (gameState == GAME_MENU) {

            mBow = new Bow(mScreenWidth / 2, mScreenHeight - 5 * range);
            mArrow = new Arrow(mScreenWidth / 2, mBow.getBowBottom());

            TARGET_MAX_DISTANCE = 2 * mScreenHeight; // can adjust the distance;

            mTarget = new Target(mScreenWidth / 2, 0 - TARGET_MAX_DISTANCE);
            mBackground = new Background();
        }
    }

    public Bitmap ReadBitMap(Context context, int resId) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        // 获取资源图片
        InputStream is = context.getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is, null, opt);
    }

    /**
     * SurfaceView初始化函数
     */
    public MySurfaceView(Context context, int screenHeight, int screenWidth) {
        super(context);
        mContext = context;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        range = mScreenHeight / 10;
        Log.d(TAG, "surfaceCreated: mScreenWidth = " + mScreenWidth + "--mScreenHeight = " + mScreenHeight + "--range = " + range);

        //实例SurfaceHolder
        mSurfaceHolder = this.getHolder();
        //为SurfaceView添加状态监听
        mSurfaceHolder.addCallback(this);
        //实例一个画笔
        mPaint = new Paint();
        //设置画笔颜色为白色
        mPaint.setColor(Color.WHITE);
        //设置焦点
        setFocusable(true);
        initHaptic(context);

    }

    /**
     * init the haptic service
     */
    void initHaptic(Context context) {
        mHaptic = (Haptics) context.getSystemService(HAPTICS_SERVICE);
    }

    /**
     * SurfaceView视图创建，响应此函数
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initGame();
        mRunningFlag = true;
        mThread = new Thread(this);
        mThread.start();
    }

    /**
     * 判断碰撞
     */
    boolean isHit() {
        boolean result = false;
        double dx = (double) (mArrow.arrowInitX - mTarget.targetInitX);
        double dy = (double) (mArrow.bmpY - mTarget.bmpY - mTarget.getTargetHeight() / 2);
//        double distance = Math.sqrt(dx * dx + dy * dy);
//        result = Double.compare(distance, mArrow.getSpeed()) < 0;
        result = dy < mArrow.getSpeed();
        Log.d("DISTANCE", "distance = " + dy + ", speed =" + mArrow.getSpeed() + ",  result = " + result);
        return result;
    }

    boolean isHitCenter() {
        boolean result = Double.compare(mTarget.getTargetcCenter(), mArrow.bmpY) == 0;
        Log.d("DISTANCE", "distance = " + (mTarget.getTargetcCenter() - mArrow.bmpY) + ", speed =" + mArrow.getSpeed() + ",  result = " + result);
        return result;
    }

    /**
     * 游戏绘图
     */
    public void myDraw() {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();
            if (mCanvas != null) {
                mCanvas.drawColor(Color.WHITE);
                switch (gameState) {
                    case GAME_MENU:
                    case GAME_MOVE:
                    case GAME_FLY:
                        mBackground.draw(mCanvas, mPaint);
                        mTarget.draw(mCanvas, mPaint);
                        mBow.draw(mCanvas, mPaint);
                        mArrow.draw(mCanvas, mPaint);
                        break;

                    default:
                        break;

                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (mCanvas != null)
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    /**
     * 触屏事件监听
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (gameState) {
            case GAME_MENU:
                mArrow.onTouchEvent(event);
                break;
            case GAME_MOVE://游戏进行中
                mArrow.onTouchEvent(event);
                break;
            case GAME_FLY:
                if (isHit()) {
                    mArrow.onTouchEvent(event);
                }
                break;
            case GAME_PAUSE://游戏最后
                break;
            case GAME_WIN://胜利
                break;
            case GAME_LOST://输掉
                break;
        }
        return true;
    }//触屏监听函数

    /**
     * 按键事件监听
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //处理back返回按键,重置游戏
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //游戏胜利、失败、进行时都默认返回菜单
            if (gameState == GAME_FLY || gameState == GAME_MOVE) {
                gameState = GAME_MENU;
                initGame();//重置游戏

            } else if (gameState == GAME_MENU) {//当前游戏状态在菜单界面，默认返回按键退出游戏
                //Haptic.terminate();
                MainActivity.instance.finish();
                System.exit(0);
            }
            //表示此按键已处理，不再交给系统处理，
            //从而避免游戏被切入后台
            return true;
        }
        //按键监听事件函数根据游戏状态不同进行不同监听
        switch (gameState) {
            case GAME_MENU:
                break;
            case GAMEING:
                break;
            case GAME_PAUSE:
                break;
            case GAME_WIN:
                break;
            case GAME_LOST:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }//按键按下监听


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //处理back返回按键
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //游戏胜利、失败、进行时都默认返回菜单
            if (gameState == GAME_FLY || gameState == GAME_MOVE) {
                gameState = GAME_MENU;
            }
            //表示此按键已处理，不再交给系统处理，
            //从而避免游戏被切入后台
            return true;
        }
        //按键监听事件函数根据游戏状态不同进行不同监听
        switch (gameState) {
            case GAME_MENU:
                break;
            case GAMEING:
                //按键抬起事件

                break;
            case GAME_PAUSE:
                break;
            case GAME_WIN:
                break;
            case GAME_LOST:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }//按键抬起监听

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        switch (gameState) {
            case GAME_MOVE:
                Log.d(TAG, "setOnLongClickListener: MOVE");
                break;
            default:
                break;
        }
        Log.d(TAG, "setOnLongClickListener: gameState :" + gameState);
        super.setOnLongClickListener(l);
    }

    private boolean hasPlayHit = false;

    /**
     * 游戏逻辑
     */
    private void logic() {
        boolean flag = false;
//        Log.d(TAG, "logic: gameState"+gameState);
        switch (gameState) {//逻辑处理根据游戏状态不同进行不同处理
            case GAME_MENU:
                hasPlayHit = false;
                if (mBow != null) {
                    mBow.logic();
                }
                if (mTarget != null) {
                    mTarget.logic();
                }
                break;
            case GAME_MOVE:
                if (mArrow != null) {
                    mArrow.logic();
                } else {
                    flag = true;
                }
                break;
            case GAME_FLY:
                if (!isHit()) {
                    if (mBow != null) {
                        mBow.logic();
                    } else {
                        flag = true;
                    }
                    if (mArrow != null) {
                        mArrow.logic();
                    } else {
                        flag = true;
                    }
                    if (mTarget != null) {
                        mTarget.logic();
                    }
                    if (mBackground != null) {
                        mBackground.logic();
                    }
                } else if (!isHitCenter()) {
                    mArrow.logic();
                } else {
                    // hit play the hit effect
                    if (!hasPlayHit) {
                        playEffectList(mHapticIdArrowEnd);
                        hasPlayHit = true;
                    }
                }

                break;

            default:
                break;
        }
        if (flag) {
            Log.d(TAG, "logic: exit gameState" + gameState);
            MainActivity.instance.finish();
            System.exit(0);
        }
    }

    @Override
    public void run() {
        while (mRunningFlag) {
            long start, end;
            synchronized (mSurfaceHolder) {
                start = System.currentTimeMillis();
                myDraw();
                logic();
                end = System.currentTimeMillis();
            }
            try {
                long interval = 1000 / CONSTANT_FPS;
                if (end - start < interval) {
                    Thread.sleep(interval - (end - start));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * SurfaceView视图状态发生改变，响应此函数
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
     * SurfaceView视图消亡时，响应此函数
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mRunningFlag = false;
    }


    public int getScreenBottom() {
        int bottomY;
        if ((mScreenHeight > (mBow.getBowBottom() + 4 * mArrow.getArrowHeight() / 9))) {
            bottomY = mBow.getBowBottom() + 4 * mArrow.getArrowHeight() / 9;
        } else {
            bottomY = mScreenHeight - 1;
        }
        return bottomY;
    }

    private class Background {
        private Bitmap mGrass = null;
        private int mY0;
        private int grassWidth, grassHeight;

        public Background() {
            initResource();
            init();
        }

        private void initResource() {
            mGrass = ReadBitMap(mContext, R.mipmap.grass);
            grassHeight = mGrass.getHeight();
            grassWidth = mGrass.getWidth();
        }

        private void init() {
            CONST_BG_LINE_INTERVAL = mScreenHeight / 2;
            mY0 = mScreenHeight;
        }

        public void logic() {
            switch (gameState) {//逻辑处理根据游戏状态不同进行不同处理
                case GAME_MENU:
                    init();
                    break;
                case GAME_FLY:
                    if (!mTarget.isShow()) {
                        mY0 += mArrow.getSpeed();
                        if (mY0 > mScreenHeight * 2) {
                            mY0 -= mScreenHeight;
                        }
                    }
                    break;
            }
        }

        public void draw(Canvas canvas, Paint paint) {
//            for (int y = mBitPosY0; y <= mScreenHeight; y += CONST_BG_LINE_INTERVAL) {
//                int x0 = (((y - mBitPosY0) / CONST_BG_LINE_INTERVAL) % 2 == 0) ? 0 : mScreenWidth - mGrass.getWidth();
//                mCanvas.drawBitmap(mGrass, x0, y, mPaint);
//
//            }

            for (int y = mY0; y >= 0 - mScreenHeight; y -= CONST_BG_LINE_INTERVAL) {
                int x0 = (((y - mY0) / CONST_BG_LINE_INTERVAL) % 2 == 0) ? 0 : mScreenWidth - grassWidth;
                canvas.drawBitmap(mGrass, x0, y - grassHeight, mPaint);
            }
        }
    }

    /*
     * draw target and target logic
     * param
     *
     */
    private class Target {
        private final String TAG = Target.class.getSimpleName();
        private Bitmap mTarget;
        private int bmpX, bmpY;
        private int targetInitX, targetInitY;
        private int targetWidth;
        private int targetHeight;
        private final int OffsetToScreenTop = 10;

        public Target(int x, int y) {
            targetInitX = x;
            targetInitY = y;
            initResource();
            init();
        }

        private void initResource() {
            mTarget = BitmapFactory.decodeResource(mResource, R.mipmap.target);
            targetWidth = mTarget.getWidth();
            targetHeight = mTarget.getHeight();
        }

        private void init() {
            bmpX = targetInitX - targetWidth / 2;
            bmpY = targetInitY;
        }

        public int getTargetHeight() {
            return targetHeight;
        }

        public int getTargetcCenter() {
            return bmpY + targetHeight / 2;
        }

        public void draw(Canvas canvas, Paint paint) {
            canvas.drawBitmap(mTarget, bmpX, bmpY, paint);
        }

        public void logic() {
            switch (gameState) {
                case GAME_MENU:
                    init();
                    break;
                case GAME_FLY:
                    if (!isShow()) {
                        bmpY += mArrow.getSpeed();
                    }
                    break;
                default:
                    break;
            }
        }

        public boolean isShow() {
            return bmpY > OffsetToScreenTop;
        }
    }

    private class Bow {
        private Bitmap mBow;
        private Bitmap mSpring;

        private int bowInitX, bowInitY;
        private int bmpX, bmpY;
        private int bowStringX0, bowStringY0, bowStringX1, bowStringY1;
        private int bowWidth, bowHeight;

        // adjust the spring and bow's offset
        private int springOffsetX = 30;
        private int springOffsetY = 6;

        private void dumpBow() {
//            Log.d(TAG, "dumpBow: mBowX0 :" + mBowX0 + "--mBowY0" + mBowY0 + "--aSpeed" + bSpeed);
//            Log.d(TAG, "dumpBow: mBowX :" + bmpX + "--mBowY" + bmpX);
//            Log.d(TAG, "dumpBow: gameState :" + gameState);

        }

        public Bow(int x, int y) {
            bowInitX = x;
            bowInitY = y;
            initResource();
            init();
        }


        public int getBowInitY() {
            return bowInitY;
        }

        public int getBowHeight() {
            return bowHeight;
        }

        public int getBowBottom() {
            return bmpY + bowHeight;
        }

        private int getBowSpringBottom() {
            return bmpY + bowHeight - springOffsetY;
        }

        public void draw(Canvas canvas, Paint paint) {

            canvas.drawBitmap(mBow, bmpX, bmpY, paint);
//            initPoint(paint);

            Paint pt = new Paint();
            BitmapShader bitmapShader = new BitmapShader(mSpring, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            pt.setShader(bitmapShader);
            pt.setStrokeWidth(mSpring.getHeight());
            PaintFlagsDrawFilter pfd = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.setDrawFilter(pfd);

            // TODO: 2016/11/16 get Arrow bottom x,y
            if (mArrow.getArrowBottom() < getBowSpringBottom()) {
                canvas.drawLine(bowStringX0, getBowSpringBottom(), bowStringX1, getBowSpringBottom(), pt);
            } else {
                canvas.drawLine(bowStringX0, getBowSpringBottom(), bowInitX, mArrow.getArrowBottom(), pt);
                canvas.drawLine(bowStringX1, getBowSpringBottom(), bowInitX, mArrow.getArrowBottom(), pt);
            }

        }

        private void initResource() {
            mBow = BitmapFactory.decodeResource(mResource, R.mipmap.bow);
            bowHeight = mBow.getHeight();
            bowWidth = mBow.getWidth();
            mSpring = BitmapFactory.decodeResource(mResource, R.mipmap.spring);
        }


        public void init() {
            bmpX = bowInitX - bowWidth / 2;
            bmpY = bowInitY;
            bowStringX0 = bowInitX - bowWidth / 2 + springOffsetX;
            bowStringX1 = bowInitX + bowWidth / 2 - springOffsetX;
            bowStringY0 = bowStringY1 = bowInitY - springOffsetY;

        }

        public void logic() {
            switch (gameState) {
                case GAME_MOVE:
//                    if (bowStringY >= getScreenBottom()) {//mScreenHeight){
//                        bowStringY = getScreenBottom();//mScreenHeight - 1;
//                    }
                    break;
                case GAME_FLY:
                    // 弓箭要随箭发射后，后移速度 = 箭飞行速度
//                    bowStringY += mArrow.getSpeed();
                    bmpY += mArrow.getSpeed();

                    // 箭弦要恢复，恢复完成，位置固定了。
                    if (mArrow.getArrowBottom() <= getBowBottom()) {
//                        bowStringY = getBowInitBottom();
//                        mHaptic.playTimedEffect(50);// bowstring  restore ,playeffect
                        playEffectLong(50);
//                        init();
                    } else {
//                        bowStringY -= bSpeed;
                    }
                    dumpBow();
                    break;
                case GAME_MENU:
                    // TODO: 2016/10/28  init() will be called mutitimes
                    init();
                    break;
                default:
                    break;

            }
        }


    }
    
    /*
     * draw mArrow and mArrow logic
     * param    Bitmap  mArrow   
     * 			bmpX    -------
     * 			bmpY  
     * 			aSpeed   the speed of mArrow
     * 			indexEffect   the haptic index
     * 			prebmpY   long touch but do not up 
     */

    private class Arrow {
        private Bitmap mArrow;
        private int bmpX, bmpY;
        private int arrowInitX, arrowInitY;
        private int arrowWidth;


        private int arrowHeight;
        private int aSpeed = range / 4;
        private int indexEffect;
        private int prebmpY;
        private int stopTimes;
        private int outTimes;
        private int pointX = 0;
        private int pointY = 0;
        private Integer[] effectArrow = {33, 30, 29, 32, 28, 31, 27, 14};

        private void dumpArrow() {
            Log.d(TAG, "dumpArrow: bmpX :" + bmpX + "--bmpY :" + bmpY + "--aSpeed :" + aSpeed);
            Log.d(TAG, "dumpArrow: gameState :" + gameState);

        }

        public Arrow(int x, int y) {
            arrowInitX = x;
            arrowInitY = y;
            initResource();
            init();
        }

        private void initResource() {
            mArrow = BitmapFactory.decodeResource(mResource, R.mipmap.arrow);
            arrowWidth = mArrow.getWidth();
            arrowHeight = mArrow.getHeight();
        }

        private void init() {
            bmpX = arrowInitX - arrowWidth / 2;
            bmpY = arrowInitY - arrowHeight;
            prebmpY = bmpY;
            indexEffect = 0;
            stopTimes = 0;
            outTimes = 0;
            pointX = 0;
            pointY = 0;

        }

        public int getArrowBottom() {
            return bmpY + arrowHeight;
        }


        public int getArrowHeight() {
            return arrowHeight;
        }

        public void draw(Canvas canvas, Paint paint) {
            canvas.drawBitmap(mArrow, bmpX, bmpY, paint);

        }

        public boolean onTouchEvent(MotionEvent event) {
            pointX = (int) event.getX();
            pointY = (int) event.getY();

            if ((pointY <= getScreenBottom()) && (pointY >= mBow.getBowBottom())) {
                prebmpY = bmpY;  // record the pre position
                bmpY = arrowInitY - arrowHeight + (pointY - (mBow.getBowBottom())); //
                gameState = GAME_MOVE;//GAME_MOVE;

            } else {
                Log.d(TAG, "onTouchEvent_arrow pointX:" + pointX + "pointY:" + pointY + "--gameState:" + gameState);
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "onTouchEvent_arrow down: gameState" + gameState);
                if (isHit() && gameState == GAME_FLY) {
                    gameState = GAME_MENU;
                    init();
                }

            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (gameState == GAME_MOVE) {
                    Log.d(TAG, "onTouchEvent_arrow up: gameState  MOVE -> FLY");
                    gameState = GAME_FLY;
                    calcSpeed();
                    playEffectLong(200);

                } else {
                    Log.d(TAG, "onTouchEvent_arrow up: gameState" + gameState);

                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (gameState == GAME_MOVE) {
                    Log.d(TAG, "onTouchEvent_arrow MOVE : gameState:" + gameState);
                } else {
                    Log.d(TAG, "onTouchEvent MOVE: gameState" + gameState);
                }
            } else {

            }

            return true;
        }


        public void logic() {
            switch (gameState) {
                case GAME_MOVE: {

                    int distance = pointY - mBow.getBowBottom(); //
                    int max = 4 * arrowHeight / 9;  //pull the length of bowstring

                    for (indexEffect = effectArrow.length; indexEffect > 0; indexEffect--) {
                        if (distance >= indexEffect * max / effectArrow.length) {
                            playEffect(effectArrow[indexEffect - 1]);
                            Log.d(TAG, "logic MOVE -playeffect index:" + indexEffect + "--distance :" + distance + "--max:" + max);
                            break;
                        }

                    }

                    break;
                }
                case GAME_FLY: {

                    if (mTarget.isShow() && !isHit()) {
                        bmpY -= aSpeed;
                    } else if (isHit() && !isHitCenter()) {
                        bmpY = mTarget.getTargetcCenter();
                    } else {
                        Log.d("Arrow", "during fly");
                    }
                    if (aSpeed >= range) {
                        playEffectList(mHapticIdArrowFlyHeavy);
                    } else {
                        playEffectList(mHapticIdArrowFlyLight);
                    }

                    break;
                }
                default:
                    break;

            }
        }

        public int getSpeed() {
            return aSpeed;
        }

        private void calcSpeed() {

            //aSpeed = range/3;
            // the mScreenHeight is small ,so speed is fixed .
            // The speed should be changed.

            int max = getScreenBottom() - arrowHeight;
            int min = mBow.getBowBottom() - arrowHeight;
            int diff = max - min;
            Log.d(TAG, "calcSpeed: max:" + max + "--min:" + min + "--diff:" + diff);
            //dumpArrow();
            //dump01();

            if (bmpY >= max) {
                aSpeed = range;
            } else if (bmpY >= max - diff / 4) {
                aSpeed = 3 * range / 4;
            } else if (bmpY >= max - diff / 2) {
                aSpeed = range / 2;
            } else {
                aSpeed = range / 4;
            }
            Log.d(TAG, "calcSpeed: " + aSpeed);
            //dumpArrow();

        }

    }


    void playEffectLong(int milsecond) {
        mHaptic.stopPlayingEffect();
        mHaptic.playTimedEffect(milsecond);
    }

    void playEffect(int id) {
        mHaptic.stopPlayingEffect();
        mHaptic.playeffect(id);
    }

    void playEffectList(List<Integer> ids) {
        List<Integer> effectIds = ids;
        StringBuilder builder = new StringBuilder();
        List<Integer> list = new ArrayList<Integer>();
        for (Integer i : effectIds
                ) {
            list.add(i);
            builder.append(" ");
            builder.append(String.valueOf(i));
        }
        // Toast.makeText(SubCategoryActivity.this, "play haptic effect " + builder.toString(), Toast.LENGTH_SHORT).show();

          mHaptic.stopPlayingEffect();
        mHaptic.playEffectSeqBuff(changeByte2byte(list), list.size());
    }

    private byte[] changeByte2byte(List<Integer> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = list.get(i).byteValue();
        }
        return bytes;
    }

}
