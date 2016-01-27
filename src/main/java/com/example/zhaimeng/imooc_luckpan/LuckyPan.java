package com.example.zhaimeng.imooc_luckpan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by zhaimeng on 2016/1/10.
 */
public class LuckyPan extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder mHolder;
    private Canvas mCanvas;

    /**
     * 用于绘制的子线程
     */
    private Thread t;
    /**
     * 线程的控制开关
     */
    private boolean isRunning;
    /**
     * 盘块奖项的文字
     */
    public static String[] mStrs = new String[]{"单反相机", "IPAD", "恭喜发财", "IPHONE", "服装一套", "恭喜发财"};
    /**
     * 盘块奖项的图片
     */
    private int[] mImgs = new int[]{R.drawable.danfan, R.drawable.ipad, R.drawable.f040, R.drawable.iphone, R.drawable.meizi, R.drawable.f015};
    /**
     * 与图片对应的bitmap,绘制需要使用bitmap
     */
    private Bitmap[] mImgsBitmap;
    /**
     * 转盘字体大小
     */
    private float mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics());
    /**
     * 盘块的颜色
     */
    private int[] mColors = new int[]{0xffffc300, 0xfff17e01, 0xffffc300, 0xfff17e01, 0xffffc300, 0xfff17e01,};
    /**
     * 盘块的数量
     */
    private int mItemCount = 6;
    /**
     * 盘块的背景图
     */
    private Bitmap mBgBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg2);
    /**
     * 整个盘块的范围
     */
    private RectF mRange = new RectF();
    /**
     * 整个盘块的直径
     */
    private int mDiameter;
    /**
     * 绘制盘块的画笔，弧形
     */
    private Paint mArcPaint;
    /**
     * 绘制文本的画笔
     */
    private Paint mTextPaint;

    /**
     * 盘块滚动的速度
     */
    private double mSpeed;
    /**
     * 盘块的开始角度
     */
    private volatile float mStartAngle = 0;
    /**
     * 判断是否点击了停止按钮
     */
    private boolean isShouldEnd;

    /**
     * 转盘的中心位置
     */
    private int mCenter;
    /**
     * 这里padding直接以paddingLeft为准
     */
    private int mPadding;


    public LuckyPan(Context context) {
        this(context, null);
    }

    public LuckyPan(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //将转盘强制为正方形
        int width = Math.min(getMeasuredWidth(), getMeasuredHeight());
        mPadding = getPaddingLeft();
        mDiameter = width - mPadding * 2;
        mCenter = mPadding + mDiameter / 2;
        setMeasuredDimension(width, width);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        //初始化绘制盘块的画笔
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setDither(true);

        //初始化绘制文字的画笔
        mTextPaint = new Paint();
        mTextPaint.setColor(0xffffffff);
        mTextPaint.setTextSize(mTextSize);

        //初始化盘块绘制的范围
        mRange = new RectF(mPadding, mPadding, mPadding + mDiameter, mPadding + mDiameter);
        //初始化图片
        mImgsBitmap = new Bitmap[mItemCount];
        for (int i = 0; i < mItemCount; i++) {
            mImgsBitmap[i] = BitmapFactory.decodeResource(getResources(), mImgs[i]);
        }

        isRunning = true;
        t = new Thread(this);
        t.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;

    }

    @Override
    public void run() {
        while (isRunning) {
            long start = System.currentTimeMillis();
            draw();
            long end = System.currentTimeMillis();
            if (end - start < 50) {
                try {
                    Thread.sleep(50 - (end - start));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void draw() {
        try {
            mCanvas = mHolder.lockCanvas();
            if (mCanvas != null) {
                //绘制背景
                drawBg();
                //绘制盘块
                float tmpAngle = mStartAngle;
                float sweepAngle = 360 / mItemCount;
                for (int i = 0; i < mItemCount; i++) {
                    mArcPaint.setColor(mColors[i]);
                    mCanvas.drawArc(mRange, tmpAngle, sweepAngle, true, mArcPaint);
                    //绘制文本
                    drawText(tmpAngle, sweepAngle, mStrs[i]);
                    //绘制Icon
                    drawIcon(tmpAngle, mImgsBitmap[i]);
                    tmpAngle += sweepAngle;
                }
                //使转盘旋转，mStartAngle为盘块的初始角度
                mStartAngle += mSpeed;
                //如果点击了停止按钮
                if (isShouldEnd) {
                    mSpeed -= 1;//每次draw减1
                }
                if (mSpeed < 0) {
                    mSpeed = 0;
                    isShouldEnd = false;//减到0不再减
                    if (mOnFinishListener != null) {
                        mOnFinishListener.onFinish();
                    }
                }

            }
        } catch (Exception e) {
        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }

    }

    /**
     * 判断转盘是否在旋转
     */
    public boolean isStart() {
        return mSpeed != 0;
    }

    /**
     * 点击启动旋转
     * index 控制奖品为哪个
     */
    public void luckyStart(int index) {
        float angle = 360 / mItemCount; //每一项的角度
        //计算每一项中奖角度范围(当前index)
        //转盘转from ~ end之间的角度即可中index的奖品
        float from = 270 - (index + 1) * angle;
        float end = from + angle;
        //设置停下来所需要旋转的角度量，4为可设置值，不影响结果
        float targetFrom = 4 * 360 + from;
        float targetEnd = 4 * 360 + end;
        //mSpeed就是每次执行draw时转盘所转的角度量
        float v1 = (float) ((-1 + Math.sqrt(1 + 8 * targetFrom)) / 2);//计算起始边的起始速度
        float v2 = (float) ((-1 + Math.sqrt(1 + 8 * targetEnd)) / 2);//计算起始边的起始速度
        mSpeed = v1 + Math.random() * (v2 - v1);
        isShouldEnd = false;
    }

    /**
     * 点击停止旋转，点击后开始减速，到0后停止
     */
    public void luckyEnd() {
        isShouldEnd = true;
        mStartAngle = 0;
    }

    /**
     * 转盘是否在旋转
     */
    public boolean isRotating() {
        return mSpeed == 0;
    }

    /**
     * 是否点击了停止按钮，点击了为true，转盘还在转为false
     */
    public boolean isShouldEnd() {
        return isShouldEnd;
    }

    /**
     * 绘制奖品图片
     *
     * @param tmpAngle 起始角度
     * @param bitmap   bitmap资源
     */
    private void drawIcon(float tmpAngle, Bitmap bitmap) {
        //设置图片的宽度为直径的1/8
        int imgWidth = mDiameter / 8;
        float angle = (float) ((tmpAngle + 360 / mItemCount / 2) * (Math.PI / 180));
        //图片中心点坐标
        int x = (int) (mCenter + mDiameter / 2 / 2 * Math.cos(angle));
        int y = (int) (mCenter + mDiameter / 2 / 2 * Math.sin(angle));
        //确定图片的矩形框
        Rect rect = new Rect(x - imgWidth / 2, y - imgWidth / 2, x + imgWidth / 2, y + imgWidth / 2);
        mCanvas.drawBitmap(bitmap, null, rect, null);
    }

    /**
     * 绘制每个盘块的文本
     *
     * @param tmpAngle   起始角度
     * @param sweepAngle 经历角度
     * @param string     字串
     */
    private void drawText(float tmpAngle, float sweepAngle, String string) {
        //弧形路径
        Path path = new Path();
        path.addArc(mRange, tmpAngle, sweepAngle);
        //水平偏移量，使文字居中
        float textWidth = mTextPaint.measureText(string);
        float hOffset = (float) (mDiameter * Math.PI / mItemCount / 2 - textWidth / 2);
        //垂直偏移量，使文字居中
        float vOffset = mDiameter / 2 / 6;
        mCanvas.drawTextOnPath(string, path, hOffset, vOffset, mTextPaint);
    }

    /**
     * 绘制背景
     */
    private void drawBg() {
        mCanvas.drawColor(0xffffffff);
        mCanvas.drawBitmap(mBgBitmap, null, new Rect(mPadding / 2, mPadding / 2, getMeasuredWidth() - mPadding / 2, getMeasuredHeight() - mPadding / 2), null);

    }

    public interface OnFinishLuckyPan {
        void onFinish();
    }

    private OnFinishLuckyPan mOnFinishListener;

    public void setOnFinishListener(OnFinishLuckyPan onFinishLuckyPan) {
        this.mOnFinishListener = onFinishLuckyPan;
    }

}
