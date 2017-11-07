package com.jni.example.yyqpullrefresh;

import android.widget.Scroller;

/**
 * 创建时间 : 2017/10/11
 * 创建人：yangyingqi
 * 公司：嘉善和盛网络有限公司
 * 备注：刷新模块数据类
 */
public class PullLayoutOption {
    //默认滑动事件 ms
    public static  final int DEFAULT_SMOOTH_TIME=300;
    //自动刷新和自动加载布局弹出时间 ms
    public static final int AUTO_RefRRESH_SMOOTH_TIME=200;
    //触发顶部刷新的偏移量
    private int mRefreshOffset;
    //布局向下滑动的最大偏移量，要大于等于顶部偏移量才能生效
    private int mMaxDownOffset;
    //滑动的系数，主要是在手指滑动的距离的基础上乘以系数，从而产生阻尼或放大的效果
    private float mMoveRation;
    //触发底部加载更多的偏移量(温馨提醒:这个是负数)
    private int mLoadMoreOffset;
    //布局向上滑动的最大偏移量，要小于等于底部偏移量才能生效
    private int mMaxUpOffset;
    //内容视图位置是否固定（不会随着手指滑动而移动位置）
    private boolean isContentFixed;
    //检验监听
    private OnCheckHandler mOnCheckHandler;
    //调用刷新完成之后实际开始操作的延时
    private int mRefreshComPleteDelayed;
    //调用加载完成之后实际开始操作的延时
    private int mLoadCompleteDelayed;
    //自动刷新的时候顶部/底部弹出的时间
    private int mAutoRefreshPopTime;
    //顶部/底部回弹时间
    private int mKickBackTime;
    //缓慢移动Scroller
    private Scroller mScroller;

    public interface OnCheckHandler{
        //下拉
        boolean canUpTpDown();
        //上拉
        boolean canDownToUP();
    }
    public PullLayoutOption(){}


    public int getmRefreshOffset() {
        return mRefreshOffset;
    }

    public void setmRefreshOffset(int mRefreshOffset) {
        this.mRefreshOffset = mRefreshOffset;
    }

    public int getmMaxDownOffset() {
        return mMaxDownOffset;
    }

    public void setmMaxDownOffset(int mMaxDownOffset) {
        this.mMaxDownOffset = mMaxDownOffset;
    }

    public float getmMoveRation() {
        return mMoveRation;
    }

    public void setmMoveRation(float mMoveRation) {
        this.mMoveRation = mMoveRation;
    }

    public int getmLoadMoreOffset() {
        return mLoadMoreOffset;
    }

    public void setmLoadMoreOffset(int mLoadMoreOffset) {
        this.mLoadMoreOffset = (-mLoadMoreOffset);//要转为负数
    }

    public int getmMaxUpOffset() {

        return mMaxUpOffset;
    }

    public void setmMaxUpOffset(int mMaxUpOffset) {
        this.mMaxUpOffset = (-mMaxUpOffset);//要转为负数
    }

    public boolean isContentFixed() {
        return isContentFixed;
    }

    public void setContentFixed(boolean contentFixed) {
        isContentFixed = contentFixed;
    }

    public OnCheckHandler getmOnCheckHandler() {
        return mOnCheckHandler;
    }

    public void setmOnCheckHandler(OnCheckHandler mOnCheckHandler) {
        this.mOnCheckHandler = mOnCheckHandler;
    }

    public int getmRefreshComPleteDelayed() {
        return mRefreshComPleteDelayed;
    }

    public void setmRefreshComPleteDelayed(int mRefreshComPleteDelayed) {
        this.mRefreshComPleteDelayed = mRefreshComPleteDelayed;
    }

    public int getmLoadCompleteDelayed() {
        return mLoadCompleteDelayed;
    }

    public void setmLoadCompleteDelayed(int mLoadCompleteDelayed) {
        this.mLoadCompleteDelayed = mLoadCompleteDelayed;
    }

    public int getmAutoRefreshPopTime() {
        return mAutoRefreshPopTime;
    }

    public void setmAutoRefreshPopTime(int mAutoRefreshPopTime) {
        this.mAutoRefreshPopTime = mAutoRefreshPopTime;
    }

    public int getmKickBackTime() {
        return mKickBackTime;
    }

    public void setmKickBackTime(int mKickBackTime) {
        this.mKickBackTime = mKickBackTime;
    }

    public Scroller getmScroller() {
        return mScroller;
    }

    public void setmScroller(Scroller mScroller) {
        this.mScroller = mScroller;
    }

    //是否可以因为底部加载从而使得手指可以从下到上滑动
    public boolean canUpToDown(){
        return null==mOnCheckHandler||mOnCheckHandler.canUpTpDown();
    }

    //是否可以因为底部加载从而使的手指可以从下到上的滑动
    public boolean canDownToUp(){
        return null==mOnCheckHandler||mOnCheckHandler.canDownToUP();
    }



}
