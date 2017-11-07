package com.jni.example.yyqpullrefresh;

/**
 * 创建时间 : 2017/10/11
 * 创建人：yangyingqi
 * 公司：嘉善和盛网络有限公司
 * 备注：上拉加载监听回调,处理各种状态
 */
public interface ILoadMoreListener {
    void onBeforeLoad();//没有达到加载条件底部开始回弹的回调
    void onUIPositionChanged(int oldOffset,int newOffset,int loadMoreOffset);
    void onLoadMoreBegin();
    void onLoadMoreComplete();
    void onCanLoadMore();
}
