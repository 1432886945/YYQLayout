package com.jni.example.yyqpullrefresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.Size;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * 创建时间 : 2017/10/11
 * 创建人：yangyingqi
 * 公司：嘉善和盛网络有限公司
 * 备注：自定义下拉刷新+上拉加载View
 */
public class PullRefreshView extends ViewGroup implements NestedScrollingParent,NestedScrollingChild{
    //content
    private View mContentView;
    //refreshing
    private View mHeaderView;
    //loading
    private View mFooterView;
    //ismode
    private boolean isOnTouch;
    //滑动参数模块
    private PullLayoutOption mOption;
    //头部高度
    private int mHeaderHeight;
    //底部高度
    private int mFooterHeight;
    //坐标类
    private Point mLastPoint;
    //当前滑动距离
    private int mCurrentOffset;
    //持续滑动距离
    private int mPrevOffset;
    private int mTouchSlop;
    //刷新加载事件监听
    private ArrayList<IRefreshListener> mRefreshListeners;
    private ArrayList<ILoadMoreListener> mLoadMoreListeners;
    //是否刷新标识
    private boolean isRefreshing;
    //是否加载标识
    private boolean isLoading;
    //use to smooth scroll
    private ScrollerWorker mScroller;
    //触摸/点击判断标识
    private boolean canUp;
    private boolean canDown;
    //滑动判断标识
    private boolean isNestedScrolling;
    //结束滑动标识
    private boolean disabledNestedScrolling;

    private NestedScrollingParentHelper mParentHelper;
    private NestedScrollingChildHelper mChildHelper;

    public PullRefreshView(Context context) {
        this(context, null);
    }

    public PullRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //强行绘制
        setWillNotDraw(false);
        //可以忽略
        //setAlwaysDrawnWithCacheEnabled(false);
        initData();
        //自定义属性
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PullLayout);
        //刷新偏移量
        int refreshOffset = array.getDimensionPixelOffset(R.styleable.PullLayout_refreshOffset,0);
        if(0 != refreshOffset){
            mOption.setmRefreshOffset(refreshOffset);
        }
        //加载偏移量
        int loadMoreOffset = array.getDimensionPixelOffset(R.styleable.PullLayout_loadMoreOffset,0);
        if(0 != loadMoreOffset){
            mOption.setmLoadMoreOffset(loadMoreOffset);
        }
        //刷新最大偏移量
        int maxUpOffset = array.getDimensionPixelOffset(R.styleable.PullLayout_maxUpOffset,0);
        if(0 != maxUpOffset){
            mOption.setmMaxUpOffset(maxUpOffset);
        }
        //加载最大偏移量
        int maxDownOffset = array.getDimensionPixelOffset(R.styleable.PullLayout_maxDownOffset,0);
        if(0 != maxDownOffset){
            mOption.setmMaxDownOffset(maxDownOffset);
        }
        //view比例
        float movieRatio = array.getFloat(R.styleable.PullLayout_movieRatio,1);
        mOption.setmMoveRation(movieRatio);
        //是否固定view
        boolean contentFixed = array.getBoolean(R.styleable.PullLayout_contentFixed,false);
        mOption.setContentFixed(contentFixed);
        //是否禁止嵌套其他布局
        disabledNestedScrolling = array.getBoolean(R.styleable.PullLayout_disableNestedScrolling,false);
        //刷新结束高度
        int refreshCompleteDelayedTime = array.getInt(R.styleable.PullLayout_refreshComleteDelayedTime,0);
        mOption.setmRefreshComPleteDelayed(refreshCompleteDelayedTime);
        array.recycle();
    }

    private void initData() {
        if(null == mOption) {
            mOption = new PullLayoutOption();
        }
        mLastPoint = new Point();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mRefreshListeners = new ArrayList<>();
        mLoadMoreListeners = new ArrayList<>();
        mScroller = new ScrollerWorker(getContext(),mOption.getmScroller());
        mParentHelper = new NestedScrollingParentHelper(this);
        mChildHelper = new NestedScrollingChildHelper(this);
    }

    //自定义参数(默认的够用了)
    public void setOption(PullLayoutOption mOption) {
        this.mOption = mOption;
    }

    /**
     * 当父类的子view全部加载完调用，可以用初始化子view的引用(这里无法获取子view的宽高 )
     *
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        switch (childCount) {
            case 1:
                mContentView = getChildAt(0);
                break;
            case 2:
                mContentView = getChildAt(0);
                mHeaderView = getChildAt(1);
                break;
            case 3:
                mContentView = getChildAt(0);
                mHeaderView = getChildAt(1);
                mFooterView = getChildAt(2);
                break;
            default:
                throw new IllegalArgumentException("must cover 1-3 child view");
        }
        checkHeaderAndFooterAndAddListener();
    }


    private void checkHeaderAndFooterAndAddListener() {
        if (mHeaderView instanceof IRefreshListener) {
            mRefreshListeners.add((IRefreshListener) mHeaderView);
        }
        if (mFooterView instanceof ILoadMoreListener) {
            mLoadMoreListeners.add((ILoadMoreListener) mFooterView);
        }
    }

    //绘制回调
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildWithMargins(mContentView, widthMeasureSpec, 0, heightMeasureSpec, 0);
        MarginLayoutParams lp = null;
        if (null != mHeaderView) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            mHeaderHeight = mHeaderView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
        }
        if (null != mFooterView) {
            measureChildWithMargins(mFooterView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            lp = (MarginLayoutParams) mFooterView.getLayoutParams();
            mFooterHeight = mFooterView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left, top;
        MarginLayoutParams lp;
        lp = (MarginLayoutParams) mContentView.getLayoutParams();
        left = (getPaddingLeft() + lp.leftMargin);
        if (mOption.isContentFixed()) {
            top = (getPaddingTop() + lp.topMargin);
        }else{
            top = (getPaddingTop() + lp.topMargin) + mCurrentOffset;
        }
        mContentView.layout(left, top, left + mContentView.getMeasuredWidth(), top + mContentView.getMeasuredHeight());
        if (null != mHeaderView) {
            lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            left = (getPaddingLeft() + lp.leftMargin);
            top = (getPaddingTop() + lp.topMargin) - mHeaderHeight + mCurrentOffset;
            mHeaderView.layout(left, top, left + mHeaderView.getMeasuredWidth(), top + mHeaderView.getMeasuredHeight());
        }
        if (null != mFooterView) {
            lp = (MarginLayoutParams) mFooterView.getLayoutParams();
            left = (getPaddingLeft() + lp.leftMargin);
            top = (b - t - getPaddingBottom() + lp.topMargin) + mCurrentOffset;
            mFooterView.layout(left, top, left + mFooterView.getMeasuredWidth(), top + mFooterView.getMeasuredHeight());
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        @SuppressWarnings({"unused"})
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    //因为ViewGroup可以嵌套子View,所以这里多重写这个这个方法（用于触摸事件的拦截）
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.v("xxx","onInterceptTouchEvent");
        if (!isEnabled() || !hasHeaderOrFooter() || isRefreshing || isLoading || isNestedScrolling) {
            return false;
        }
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_MOVE:
                int x = (int) event.getX();
                int y = (int) event.getY();
                int deltaY = (y - mLastPoint.y);
                int dy = Math.abs(deltaY);
                int dx = Math.abs(x - mLastPoint.x);
                if (dy > mTouchSlop && dy >= dx) {
                    canUp = mOption.canUpToDown();
                    canDown = mOption.canDownToUp();
                    return (deltaY > 0 && canUp) || (deltaY < 0 && canDown);
                }
                return false;
        }
        mLastPoint.set((int) event.getX(), (int) event.getY());
        return false;
    }

    //这个大家都懂（用于事件的处理）
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || !hasHeaderOrFooter() || isRefreshing || isLoading || isNestedScrolling) {
            return false;
        }
        switch (MotionEventCompat.getActionMasked(event)) {
            //按下
            case MotionEvent.ACTION_MOVE:
                isOnTouch = true;
                updatePos((int) (mOption.getmMoveRation() * (event.getY() - mLastPoint.y)));
                break;
            //end(这里不做处理，可以根据自己的需要做处理)
            case MotionEvent.ACTION_CANCEL:
            //滑动
            case MotionEvent.ACTION_UP:
                isOnTouch = false;
                if (mCurrentOffset > 0) {
                    tryPerformRefresh();
                } else if(mCurrentOffset < 0){
                    tryPerformLoading();
                }
                break;
        }
        mLastPoint.set((int) event.getX(), (int) event.getY());
        return true;
    }

    //更新滑动数据
    private void updatePos(int deltaY) {
        if (!hasHeaderOrFooter() || deltaY == 0) {
            return;
        }
        if (isOnTouch) {
            if (!canUp && (mCurrentOffset + deltaY > 0)) {
                deltaY = (0 - mCurrentOffset);
            } else if (!canDown && (mCurrentOffset + deltaY < 0)) {
                deltaY = (0 - mCurrentOffset);
            }
        }
        mPrevOffset = mCurrentOffset;
        mCurrentOffset += deltaY;
        mCurrentOffset = Math.max(Math.min(mCurrentOffset, mOption.getmMaxDownOffset()), mOption.getmMaxUpOffset());
        deltaY = mCurrentOffset - mPrevOffset;
        if (deltaY == 0) {
            return;
        }
        callUIPositionChangedListener(mPrevOffset, mCurrentOffset);
        if (mCurrentOffset >= mOption.getmRefreshOffset()) {
            callCanRefreshListener();
        } else if (mCurrentOffset <= mOption.getmLoadMoreOffset()) {
            callCanLoadMoreListener();
        }
        if (!mOption.isContentFixed()) {
            mContentView.offsetTopAndBottom(deltaY);
        }
        if (null != mHeaderView) {
            mHeaderView.offsetTopAndBottom(deltaY);
        }
        if (null != mFooterView) {
            mFooterView.offsetTopAndBottom(deltaY);
        }
        invalidate();
    }

    //判断是否添加头部和尾部
    private boolean hasHeaderOrFooter() {
        return null != mHeaderView || null != mFooterView;
    }

    //处理加载出现的未知异常
    private void tryPerformLoading() {
        if (isOnTouch || isLoading || isNestedScrolling) {
            return;
        }
        if (mCurrentOffset <= mOption.getmLoadMoreOffset()) {
            startLoading();
        } else {
            mScroller.trySmoothScrollToOffset(0);
            if(mCurrentOffset < 0) {
                callBeforeLoadMoreListener();
            }
        }
    }
    //处理刷新的未知异常
    private void tryPerformRefresh() {
        if (isOnTouch || isRefreshing || isNestedScrolling) {
            return;
        }
        if (mCurrentOffset >= mOption.getmRefreshOffset()) {
            startRefreshing();
        } else {
            mScroller.trySmoothScrollToOffset(0);
            if(mCurrentOffset > 0) {
                callBeforeRefreshListener();
            }
        }
    }

    private void startRefreshing() {
        isRefreshing = true;
        callRefreshBeginListener();
        mScroller.trySmoothScrollToOffset(mOption.getmRefreshOffset());
    }

    private void startLoading() {
        isLoading = true;
        callLoadMoreBeginListener();
        mScroller.trySmoothScrollToOffset(mOption.getmLoadMoreOffset());
    }

    public void refreshComplete() {
        if (!isRefreshing) {
            return;
        }
        callRefreshCompleteListener();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if(null != getContext()) {
                    isRefreshing = false;
                    mScroller.trySmoothScrollToOffset(0);
                }
            }
        },mOption.getmRefreshComPleteDelayed());
    }


    public void loadingComplete() {
        if (!isLoading) {
            return;
        }
        callLoadMoreCompleteListener();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if(null != getContext()) {
                    isLoading = false;
                    mScroller.trySmoothScrollToOffset(0);
                }
            }
        },mOption.getmLoadCompleteDelayed());
    }

    private void callBeforeRefreshListener(){
        for (IRefreshListener listener : mRefreshListeners) {
            listener.onBeforeRefresh();
        }
    }
    private void callRefreshBeginListener() {
        for (IRefreshListener listener : mRefreshListeners) {
            listener.onRefreshBegin();
        }
    }

    private void callRefreshCompleteListener() {
        for (IRefreshListener listener : mRefreshListeners) {
            listener.onRefreshComplete();
        }
    }

    private void callCanRefreshListener() {
        for (IRefreshListener listener : mRefreshListeners) {
            listener.onCanRefresh();
        }
    }

    private void callUIPositionChangedListener(int oldOffset, int newOffset) {
        for (IRefreshListener listener : mRefreshListeners) {
            listener.onUIPositionChanged(oldOffset, newOffset, mOption.getmRefreshOffset());
        }
        for (ILoadMoreListener loadMoreListener : mLoadMoreListeners) {
            loadMoreListener.onUIPositionChanged(oldOffset, newOffset, mOption.getmLoadMoreOffset());
        }
    }

    private  void callBeforeLoadMoreListener(){
        for (ILoadMoreListener listener : mLoadMoreListeners) {
            listener.onBeforeLoad();
        }
    }
    private void callLoadMoreBeginListener() {
        for (ILoadMoreListener listener : mLoadMoreListeners) {
            listener.onLoadMoreBegin();
        }
    }

    private void callLoadMoreCompleteListener() {
        for (ILoadMoreListener listener : mLoadMoreListeners) {
            listener.onLoadMoreComplete();
        }
    }

    private void callCanLoadMoreListener() {
        for (ILoadMoreListener listener : mLoadMoreListeners) {
            listener.onCanLoadMore();
        }
    }

    //注册
    public void addRefreshListener(IRefreshListener listener) {
        mRefreshListeners.add(listener);
    }
    //移除
    public void removeRefreshListener(IRefreshListener listener) {
        mRefreshListeners.remove(listener);
    }

    public void addLoadMoreListener(ILoadMoreListener listener) {
        mLoadMoreListeners.add(listener);
    }

    public void removeLoadMoreListener(ILoadMoreListener listener) {
        mLoadMoreListeners.remove(listener);
    }
    //设置头部
    public void setOnCheckHandler(PullLayoutOption.OnCheckHandler handler) {
        mOption.setmOnCheckHandler(handler);
    }

    //头部高度
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    //底部
    public int getFooterHeight() {
        return mFooterHeight;
    }


    //设置自动刷新
    public void autoRefresh() {
        boolean hasView = (mHeaderView != null && isEnabled());
        boolean isWorking = (isRefreshing || isLoading || mScroller.isRunning);
        boolean isTouch = (isOnTouch || isNestedScrolling);
        if (!hasView || isWorking || isTouch) {
            return;
        }
        mScroller.mSmoothScrollTime = mOption.getmAutoRefreshPopTime();
        startRefreshing();
        mScroller.mSmoothScrollTime = mOption.getmKickBackTime();
    }

    //设置自动加载
    public void autoLoading() {
        boolean hasView = (mFooterView != null && isEnabled());
        boolean isWorking = (isLoading || isRefreshing || mScroller.isRunning);
        boolean isTouch = (isOnTouch || isNestedScrolling);
        if (!hasView || isWorking || isTouch) {
            return;
        }
        mScroller.mSmoothScrollTime = mOption.getmAutoRefreshPopTime();
        startLoading();
        mScroller.mSmoothScrollTime = mOption.getmKickBackTime();
    }

    //子View开始滑动
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        boolean isVerticalScroll = (nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL);
        boolean canTouchMove = isEnabled() && hasHeaderOrFooter();
        return !disabledNestedScrolling && isVerticalScroll && canTouchMove;
    }

    //子View停止滑动
    @Override
    public void onStopNestedScroll(View child) {
        if(disabledNestedScrolling){
            return;
        }
        mParentHelper.onStopNestedScroll(child);
        if (isNestedScrolling) {
            isNestedScrolling = false;
            isOnTouch = false;
            if (mCurrentOffset >= mOption.getmRefreshOffset()) {
                startRefreshing();
            } else if(mCurrentOffset <= mOption.getmLoadMoreOffset()){
                startLoading();
            } else {
                mScroller.trySmoothScrollToOffset(0);
                if(mCurrentOffset < 0){
                    callBeforeLoadMoreListener();
                }else if(mCurrentOffset > 0){
                    callBeforeRefreshListener();
                }
            }
        }
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mParentHelper.onNestedScrollAccepted(child, target, axes);
    }

    //子View准备滑动事件
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if(disabledNestedScrolling){
            return;
        }
        if (isNestedScrolling) {
            canUp = mOption.canUpToDown();
            canDown = mOption.canDownToUp();
            int minOffset = canDown?mOption.getmMaxUpOffset():0;
            int maxOffset = canUp?mOption.getmMaxDownOffset():0;
            int nextOffset = (mCurrentOffset - dy);
            int sureOffset = Math.min(Math.max(minOffset,nextOffset),maxOffset);
            int deltaY = sureOffset - mCurrentOffset;
            consumed[1] = (-deltaY);
            updatePos((int) (mOption.getmMoveRation() * deltaY));
        }
        dispatchNestedPreScroll(dx, dy, consumed, null);
    }

    //正在滑动事件
    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if(disabledNestedScrolling){
            return;
        }
        boolean canTouch = !isLoading && !isRefreshing && !isOnTouch;
        if (dyUnconsumed != 0 && canTouch) {
            canUp = mOption.canUpToDown();
            canDown = mOption.canDownToUp();
            boolean canUpToDown = (canUp && dyUnconsumed < 0);
            boolean canDownToUp = (canDown && dyUnconsumed > 0);
            if(canUpToDown || canDownToUp){
                isOnTouch = true;
                isNestedScrolling = true;
                updatePos((int) (mOption.getmMoveRation() * -dyUnconsumed));
                dyConsumed = dyUnconsumed;
                dyUnconsumed = 0;
            }
        }
        dispatchNestedScroll(dxConsumed,dxUnconsumed,dyConsumed,dyUnconsumed,null);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }


    /**
     * 异步线程刷新
     */
    private class ScrollerWorker implements Runnable {
        private int mSmoothScrollTime;
        private int mLastY;  //最后计算pointY,用来计算deltaY
        private Scroller mScroller;
        private Context mContext;
        //刷新变量标识
        private boolean isRunning;

        public ScrollerWorker(Context mContext,Scroller scroller) {
            this.mContext = mContext;
            mScroller = null != scroller?scroller:new Scroller(mContext,new DecelerateInterpolator());
            mSmoothScrollTime = mOption.getmKickBackTime();
        }

        @Override
        public void run() {
            boolean isFinished = (!mScroller.computeScrollOffset() || mScroller.isFinished());
            if (isFinished) {
                //处理外部其他原因产生的滚动停止
                if(mScroller.getCurrY() != mLastY){
                    checkScrollerAndRun();
                }
                end();
            } else {
                checkScrollerAndRun();
            }
        }

        //检测滑动是否已停止
        private void checkScrollerAndRun(){
            int y = mScroller.getCurrY();
            int deltaY = (y - mLastY);
            boolean isDown = ((mPrevOffset == mOption.getmRefreshOffset() && deltaY > 0));
            boolean isUp = ((mPrevOffset == mOption.getmLoadMoreOffset()) && deltaY < 0);
            if (isDown || isUp) {//don't should scroll
                end();
                return;
            }
            updatePos(deltaY);
            mLastY = y;
            post(this);
        }

        //处理平移事件异常
        public void trySmoothScrollToOffset(int targetOffset) {
            if (!hasHeaderOrFooter()) {
                return;
            }
            endScroller();
            removeCallbacks(this);
            mLastY = 0;
            int deltaY = (targetOffset - mCurrentOffset);
            mScroller.startScroll(0, 0, 0, deltaY, mSmoothScrollTime);
            isRunning = true;
            post(this);
        }
        //结束滑动
        private void endScroller() {
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            mScroller.abortAnimation();
        }
        //end
        public void end() {
            removeCallbacks(this);
            endScroller();
            isRunning = false;
            mLastY = 0;
        }
    }
}
