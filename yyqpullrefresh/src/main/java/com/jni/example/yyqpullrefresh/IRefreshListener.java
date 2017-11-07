package com.jni.example.yyqpullrefresh;

/**
 * 创建时间 : 2017/10/11
 * 创建人：yangyingqi
 * 公司：嘉善和盛网络有限公司
 * 备注：下拉刷新回调，处理各种状态
 */
public interface IRefreshListener {
    void onBeforeRefresh();//当前偏移量没有达到刷新的标准是松手，然后头部开始回弹的回调
    void onRefreshBegin();//开始刷新的回调
    void onUIPositionChanged(int oldOffset,int newOffset,int refreshOffset);//视图滑动过程中的回调
    void onRefreshComplete();//刷新完成的回调
    void onCanRefresh();//当前偏移量已超过刷新的标准的时候，还在滑动的话会触发的回调
}
