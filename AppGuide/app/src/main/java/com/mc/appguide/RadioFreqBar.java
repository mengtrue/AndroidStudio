package com.mc.appguide;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class RadioFreqBar extends View {
    private static final String TAG = "RadioFreqBar";
    /**
     * 2个大刻度之间间距，默认为1
     */
    private int scaleLimit = 1;
    /**
     * 尺子高度
     */
    private int rulerHeight = 50;

    /**
     * 尺子和屏幕顶部以及结果之间的高度
     */
    private int rulerToResultgap = rulerHeight / 4;
    /**
     * 刻度平分多少份
     */
    private int scaleCount = 10;  //刻度评分多少份
    /**
     * 刻度间距
     */
    private int scaleGap = 20;
    /**
     * 刻度最小值
     */
    private int minScale = 80;
    /**
     * 第一次显示的刻度
     */
    private float firstScale = 80.1f;
    /**
     * 刻度最大值
     */
    private int maxScale = 100;

    /**
     * 背景颜色
     */
    private int bgColor = 0xffffffff;
    /**
     * 小刻度的颜色
     */
    private int smallScaleColor = 0xff999999;
    /**
     * 中刻度的颜色
     */
    private int midScaleColor = 0xff666666;
    /**
     * 大刻度的颜色
     */
    private int largeScaleColor = 0xff50b586;
    /**
     * 刻度颜色
     */
    private int scaleNumColor = 0xff333333;
    /**
     * 结果值颜色
     */
    private int resultNumColor = 0xff50b586;
    /**
     * 单位kg
     */
    private String unit = "kg";
    /**
     * kg颜色
     */
    private int unitColor = 0xff50b586;
    /**
     * 小刻度粗细大小
     */
    private int smallScaleStroke = 1;
    /**
     * 中刻度粗细大小
     */
    private int midScaleStroke = 2;
    /**
     * 大刻度粗细大小
     */
    private int largeScaleStroke = 3;
    /**
     * 结果字体大小
     */
    private int resultNumTextSize = 20;
    /**
     * 刻度字体大小
     */
    private int scaleNumTextSize = 16;
    /**
     * 单位字体大小
     */
    private int unitTextSize = 13;
    /**
     * 是否显示刻度结果
     */
    private boolean showScaleResult = true;
    /**
     * 是否显示刻度结果为整数
     */
    private boolean showScaleResultInteger = false;
    /**
     * 是否背景显示圆角
     */
    private boolean isBgRoundRect = true;

    /**
     * 结果回调
     */
    private OnValueChangedListener updateValueListener;
    /**
     * 滑动选择刻度
     */
    private float computeScale = -1;
    /**
     * 当前刻度
     */
    public float currentScale = firstScale;

    private ValueAnimator valueAnimator;
    private final VelocityTracker velocityTracker = VelocityTracker.obtain();
    private String resultText = String.valueOf(firstScale);
    private Paint bgPaint;
    private Paint smallScalePaint;
    private Paint midScalePaint;
    private Paint lagScalePaint;
    private Paint scaleNumPaint;
    private Paint resultNumPaint;
    private Paint kgPaint;
    private Rect scaleNumRect;
    private Rect resultNumRect;
    private Rect kgRect;
    private RectF bgRect;
    private int height, width;
    private int smallScaleHeight;
    private int midScaleHeight;
    private int lagScaleHeight;

    private float downX;
    private float moveX = 0;
    private float lastMoveX = 0;
    private boolean isUp = false;

    public RadioFreqBar(Context context, int decimalPlace, float minScale, float maxScale) {
        super(context, null, 0);
        initView(decimalPlace, minScale, maxScale);
    }

    public void setUpdateValueListener(OnValueChangedListener updateValueListener) {
        this.updateValueListener = updateValueListener;
    }

    private void initView(int decimalPlace, float minScale, float maxScale) {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        midScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lagScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scaleNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resultNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        bgPaint.setColor(bgColor);
        smallScalePaint.setColor(smallScaleColor);
        midScalePaint.setColor(midScaleColor);
        lagScalePaint.setColor(largeScaleColor);
        scaleNumPaint.setColor(scaleNumColor);
        resultNumPaint.setColor(resultNumColor);
        kgPaint.setColor(unitColor);

        resultNumPaint.setStyle(Paint.Style.FILL);
        kgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setStyle(Paint.Style.FILL);
        smallScalePaint.setStyle(Paint.Style.FILL);
        midScalePaint.setStyle(Paint.Style.FILL);
        lagScalePaint.setStyle(Paint.Style.FILL);

        lagScalePaint.setStrokeCap(Paint.Cap.ROUND);
        midScalePaint.setStrokeCap(Paint.Cap.ROUND);
        smallScalePaint.setStrokeCap(Paint.Cap.ROUND);

        smallScalePaint.setStrokeWidth(smallScaleStroke);
        midScalePaint.setStrokeWidth(midScaleStroke);
        lagScalePaint.setStrokeWidth(largeScaleStroke);

        resultNumPaint.setTextSize(resultNumTextSize);
        kgPaint.setTextSize(unitTextSize);
        scaleNumPaint.setTextSize(scaleNumTextSize);

        bgRect = new RectF();
        resultNumRect = new Rect();
        scaleNumRect = new Rect();
        kgRect = new Rect();

        resultNumPaint.getTextBounds(resultText, 0, resultText.length(), resultNumRect);
        kgPaint.getTextBounds(resultText, 0, 1, kgRect);

        smallScaleHeight = rulerHeight / 4;
        midScaleHeight = rulerHeight / 2;
        lagScaleHeight = rulerHeight / 2 + 10;
        valueAnimator = new ValueAnimator();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int heightModule = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        switch (heightModule) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                height = rulerHeight + (showScaleResult ? resultNumRect.height() : 0) + rulerToResultgap * 2 + getPaddingTop() + getPaddingBottom();
                break;
            case MeasureSpec.EXACTLY:
                height = heightSize + getPaddingTop() + getPaddingBottom();
                break;
        }

        width = widthSize + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBg(canvas);
        drawScaleAndNum(canvas);
        drawResultText(canvas, resultText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currentX = event.getX();
        isUp = false;
        velocityTracker.computeCurrentVelocity(500);
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //按下时如果属性动画还没执行完,就终止,记录下当前按下点的位置
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    valueAnimator.end();
                    valueAnimator.cancel();
                }
                downX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                //滑动时候,通过假设的滑动距离,做超出左边界以及右边界的限制。
                moveX = currentX - downX + lastMoveX;
                if (moveX >= width / 2) {
                    moveX = width / 2;
                } else if (moveX <= getWhichScalMovex(maxScale)) {
                    moveX = getWhichScalMovex(maxScale);
                }
                break;
            case MotionEvent.ACTION_UP:
                //手指抬起时候制造惯性滑动
                lastMoveX = moveX;
                int xVelocity = (int) velocityTracker.getXVelocity();
                autoVelocityScroll(xVelocity);
                velocityTracker.clear();
                break;
        }
        invalidate();
        return true;
    }

    private void autoVelocityScroll(int xVelocity) {
        //惯性滑动的代码,速率和滑动距离,以及滑动时间需要控制的很好,应该网上已经有关于这方面的算法了吧。。这里是经过N次测试调节出来的惯性滑动
        if (Math.abs(xVelocity) < 50) {
            isUp = true;
            return;
        }
        if (valueAnimator.isRunning()) {
            return;
        }
        valueAnimator = ValueAnimator.ofInt(0, xVelocity / 20).setDuration(Math.abs(xVelocity / 10));
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            moveX += (int) animation.getAnimatedValue();
            if (moveX >= width / 2) {
                moveX = width / 2;
            } else if (moveX <= getWhichScalMovex(maxScale)) {
                moveX = getWhichScalMovex(maxScale);
            }
            lastMoveX = moveX;
            invalidate();
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isUp = true;
                invalidate();
            }
        });

        valueAnimator.start();
    }

    private float getWhichScalMovex(float scale) {
        return width / 2 - scaleGap * scaleCount * (scale - minScale);
    }

    @SuppressLint("DefaultLocale")
    private void drawScaleAndNum(Canvas canvas) {
        canvas.translate(0, (showScaleResult ? resultNumRect.height() : 0) + rulerToResultgap);//移动画布到结果值的下面

        int num1;//确定刻度位置
        float num2;

        if (firstScale != -1) {   //第一次进来的时候计算出默认刻度对应的假设滑动的距离moveX
            moveX = getWhichScalMovex(firstScale);          //如果设置了默认滑动位置，计算出需要滑动的距离
            lastMoveX = moveX;
            firstScale = -1;                                //将结果置为-1，下次不再计算初始位置
        }

        if (computeScale != -1) {//弹性滑动到目标刻度
            lastMoveX = moveX;
            if (valueAnimator != null && !valueAnimator.isRunning()) {
                valueAnimator = ValueAnimator.ofFloat(getWhichScalMovex(currentScale), getWhichScalMovex(computeScale));
                valueAnimator.addUpdateListener(animation -> {
                    moveX = (float) animation.getAnimatedValue();
                    lastMoveX = moveX;
                    invalidate();
                });
                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        //这里是滑动结束时候回调给使用者的结果值
                        computeScale = -1;
                    }
                });
                valueAnimator.setDuration(Math.abs((long) ((getWhichScalMovex(computeScale) - getWhichScalMovex(currentScale)) / 100)));
                valueAnimator.start();
            }
        }

        num1 = -(int) (moveX / scaleGap);                   //滑动刻度的整数部分
        num2 = (moveX % scaleGap);                         //滑动刻度的小数部分

        canvas.save();                                      //保存当前画布

        int rulerRight = 0;                                    //准备开始绘制当前屏幕,从最左面开始

        if (isUp) {   //这部分代码主要是计算手指抬起时，惯性滑动结束时，刻度需要停留的位置
            num2 = ((moveX - width / 2 % scaleGap) % scaleGap);     //计算滑动位置距离整点刻度的小数部分距离
            if (num2 <= 0) {
                num2 = scaleGap - Math.abs(num2);
            }
            int leftScroll = (int) Math.abs(num2);                        //当前滑动位置距离左边整点刻度的距离
            int rightScroll = (int) (scaleGap - Math.abs(num2));         //当前滑动位置距离右边整点刻度的距离

            float moveX2 = num2 <= scaleGap / 2 ? moveX - leftScroll : moveX + rightScroll; //最终计算出当前位置到整点刻度位置需要滑动的距离

            if (valueAnimator != null && !valueAnimator.isRunning()) {      //手指抬起，并且当前没有惯性滑动在进行，创建一个惯性滑动
                valueAnimator = ValueAnimator.ofFloat(moveX, moveX2);
                valueAnimator.addUpdateListener(animation -> {
                    moveX = (float) animation.getAnimatedValue();            //不断滑动去更新新的位置
                    lastMoveX = moveX;
                    invalidate();
                });
                valueAnimator.addListener(new AnimatorListenerAdapter() {       //增加一个监听，用来返回给使用者滑动结束后的最终结果刻度值
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        //这里是滑动结束时候回调给使用者的结果值
                        if (updateValueListener != null) {
                            updateValueListener.onChanged((int) currentScale * 1000);
                        }
                    }
                });
                valueAnimator.setDuration(300);
                valueAnimator.start();
                isUp = false;
            }

            num1 = (int) -(moveX / scaleGap);                //重新计算当前滑动位置的整数以及小数位置
            num2 = (moveX % scaleGap);
        }


        canvas.translate(num2, 0);    //不加该偏移的话，滑动时刻度不会落在0~1之间只会落在整数上面,其实这个都能设置一种模式了，毕竟初衷就是指针不会落在小数上面

        //这里是滑动时候不断回调给使用者的结果值
        currentScale = new WeakReference<>(
                new BigDecimal(((width / 2 - moveX) / (scaleGap * scaleCount) + minScale) * scaleLimit)).get().setScale(2, RoundingMode.HALF_UP).floatValue();
        resultText = String.format("%.2f", currentScale);

        //绘制当前屏幕可见刻度,不需要裁剪屏幕,while循环只会执行·屏幕宽度/刻度宽度·次,大部分的绘制都是if(curDis<width)这样子内存暂用相对来说会比较高。。
        while (rulerRight < width) {
            if (num1 % scaleCount == 0) {    //绘制整点刻度以及文字
                if ((moveX >= 0 && rulerRight < moveX - scaleGap) || width / 2 - rulerRight <= getWhichScalMovex(maxScale + 1) - moveX) {
                    //当滑动出范围的话，不绘制，去除左右边界
                } else {
                    //绘制刻度，绘制刻度数字
                    canvas.drawLine(0, 0, 0, midScaleHeight, midScalePaint);
                    String scaleS = (num1 / scaleCount + minScale) * scaleLimit + "";
                    scaleNumPaint.getTextBounds(scaleS, 0, scaleS.length(), scaleNumRect);

                    canvas.drawText(scaleS, -scaleNumRect.width() / 2, (float) (lagScaleHeight +
                            (rulerHeight - lagScaleHeight) / 2 + scaleNumRect.height()), scaleNumPaint);
                }
            } else {   //绘制小数刻度
                if ((moveX >= 0 && rulerRight < moveX) || width / 2 - rulerRight < getWhichScalMovex(maxScale) - moveX) {
                    //当滑动出范围的话，不绘制，去除左右边界
                } else {
                    //绘制小数刻度
                    canvas.drawLine(0, 0, 0, smallScaleHeight, smallScalePaint);
                }
            }
            ++num1;  //刻度加1
            rulerRight += scaleGap;  //绘制屏幕的距离在原有基础上+1个刻度间距
            canvas.translate(scaleGap, 0); //移动画布到下一个刻度
        }

        canvas.restore();
        //绘制屏幕中间用来选中刻度的最大刻度
        canvas.drawLine(width / 2, 0, width / 2, lagScaleHeight, lagScalePaint);
    }

    //绘制上面的结果 结果值+单位
    private void drawResultText(Canvas canvas, String resultText) {
        if (!showScaleResult) {//判断用户是否设置需要显示当前刻度值，如果否则取消绘制
            return;
        }
        if (showScaleResultInteger) {//判断用户是否设置需要显示当前刻度值为整数
            resultText = String.valueOf(Float.valueOf(resultText).intValue());
        }
        canvas.translate(0, -resultNumRect.height() - rulerToResultgap / 2);  //移动画布到正确的位置来绘制结果值
        resultNumPaint.getTextBounds(resultText, 0, resultText.length(), resultNumRect);
        kgPaint.getTextBounds(unit, 0, unit.length(), kgRect);

        float x = width / 2 - (resultNumRect.width() / 2 + kgRect.width() / 2);
        canvas.drawText(resultText, x, resultNumRect.height(), //绘制当前刻度结果值
                resultNumPaint);
        int resultNumRight = (int) (x + resultNumRect.width() + 10);
        canvas.drawText(unit, resultNumRight, resultNumRect.height(), kgPaint);//绘制当前刻度结果值单位
    }

    private void drawBg(Canvas canvas) {
        bgRect.set(0, 0, width, height);
        if (isBgRoundRect) {
            canvas.drawRoundRect(bgRect, 20, 20, bgPaint); //20->椭圆的用于圆形角x-radius
        } else {
            canvas.drawRect(bgRect, bgPaint);
        }
    }

    public void computeScrollTo(float scale) {
        scale = scale / scaleLimit;
        if (scale < minScale || scale > maxScale) {
            return;
        }

        computeScale = scale;
        invalidate();
    }

    public interface OnValueChangedListener {
        void onChanged(int newValue);
    }

    public void setRulerHeight(int rulerHeight) {
        this.rulerHeight = rulerHeight;
        invalidate();
    }

    public void setRulerToResultgap(int rulerToResultgap) {
        this.rulerToResultgap = rulerToResultgap;
        invalidate();
    }

    public void setScaleCount(int scaleCount) {
        this.scaleCount = scaleCount;
        invalidate();
    }

    public void setScaleGap(int scaleGap) {
        this.scaleGap = scaleGap;
        invalidate();
    }

    public void setMinScale(int minScale) {
        this.minScale = minScale;
        invalidate();
    }

    public void setFirstScale(float firstScale) {
        this.firstScale = firstScale;
        invalidate();
    }

    public float getFirstScale() {
        if (this.firstScale == -1) this.firstScale = currentScale;
        return this.firstScale;
    }

    public void setMaxScale(int maxScale) {
        this.maxScale = maxScale;
        invalidate();
    }

    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
        invalidate();
    }

    public void setSmallScaleColor(int smallScaleColor) {
        this.smallScaleColor = smallScaleColor;
        invalidate();
    }

    public void setMidScaleColor(int midScaleColor) {
        this.midScaleColor = midScaleColor;
        invalidate();
    }

    public void setLargeScaleColor(int largeScaleColor) {
        this.largeScaleColor = largeScaleColor;
    }

    public void setScaleNumColor(int scaleNumColor) {
        this.scaleNumColor = scaleNumColor;
        invalidate();
    }

    public void setResultNumColor(int resultNumColor) {
        this.resultNumColor = resultNumColor;
        invalidate();
    }

    public void setUnit(String unit) {
        this.unit = unit;
        invalidate();
    }

    public void setUnitColor(int unitColor) {
        this.unitColor = unitColor;
        invalidate();
    }

    public void setSmallScaleStroke(int smallScaleStroke) {
        this.smallScaleStroke = smallScaleStroke;
        invalidate();
    }

    public void setMidScaleStroke(int midScaleStroke) {
        this.midScaleStroke = midScaleStroke;
        invalidate();
    }

    public void setLargeScaleStroke(int largeScaleStroke) {
        this.largeScaleStroke = largeScaleStroke;
        invalidate();
    }

    public void setResultNumTextSize(int resultNumTextSize) {
        this.resultNumTextSize = resultNumTextSize;
        invalidate();
    }

    public void setScaleNumTextSize(int scaleNumTextSize) {
        this.scaleNumTextSize = scaleNumTextSize;
        invalidate();
    }

    public void setUnitTextSize(int unitTextSize) {
        this.unitTextSize = unitTextSize;
        invalidate();
    }

    public void setShowScaleResult(boolean showScaleResult) {
        this.showScaleResult = showScaleResult;
        invalidate();
    }

    public void setShowScaleResultInteger(boolean showScaleResultInteger) {
        this.showScaleResultInteger = showScaleResultInteger;
        invalidate();
    }

    public void setIsBgRoundRect(boolean bgRoundRect) {
        isBgRoundRect = bgRoundRect;
        invalidate();
    }

    public void setScaleLimit(int scaleLimit) {
        this.scaleLimit = scaleLimit;
        invalidate();
    }
}
