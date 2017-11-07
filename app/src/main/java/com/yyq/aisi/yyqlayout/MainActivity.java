package com.yyq.aisi.yyqlayout;

import android.os.Handler;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.Toast;

import com.jni.example.yyqpullrefresh.ILoadMoreListener;
import com.jni.example.yyqpullrefresh.IRefreshListener;
import com.jni.example.yyqpullrefresh.PullLayoutOption;
import com.jni.example.yyqpullrefresh.PullRefreshView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final PullRefreshView pullRefreshView= (PullRefreshView) findViewById(R.id.pull);
        final ScrollView scrollView= (ScrollView) findViewById(R.id.content_scrollview);
        /**
         * 刷新滑动监听
         */
        pullRefreshView.addRefreshListener(new IRefreshListener() {
            @Override
            public void onBeforeRefresh() {
                Toast.makeText(getApplicationContext(),"亲，还没到刷新条件哦~",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRefreshBegin() {
                Toast.makeText(getApplicationContext(),"亲，开始刷新了哦~",Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullRefreshView.refreshComplete();
                    }
                },3000);
            }
            //滑动进度改变事件回调
            @Override
            public void onUIPositionChanged(int oldOffset, int newOffset, int refreshOffset) {}

            @Override
            public void onRefreshComplete() {
                Toast.makeText(getApplicationContext(),"亲，刷新已完成~",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCanRefresh() {}
        });

        /**
         * 加载滑动监听
         */
        pullRefreshView.addLoadMoreListener(new ILoadMoreListener() {
            @Override
            public void onBeforeLoad() {
                Toast.makeText(getApplicationContext(),"亲，还没到加载条件哦~",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUIPositionChanged(int oldOffset, int newOffset, int loadMoreOffset) {}

            @Override
            public void onLoadMoreBegin() {
                Toast.makeText(getApplicationContext(),"亲，加载开始了哦~",Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullRefreshView.loadingComplete();
                    }
                },3000);
            }

            @Override
            public void onLoadMoreComplete() {
                Toast.makeText(getApplicationContext(),"亲，加载完成了哦~",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCanLoadMore() {}
        });

        /**
         * 设置可以下拉刷新和上拉加载更多的条件
         */
        pullRefreshView.setOnCheckHandler(new PullLayoutOption.OnCheckHandler() {
            @Override
            public boolean canUpTpDown() {
                //recycleView不能向上滑动，即滑到底了，这时候，可以加载更多
                return !scrollView.canScrollVertically(-1);
            }

            @Override
            public boolean canDownToUP() {
                //recycleview不能向下滑动，即在首部了，这时候，可以下拉刷新
                return !scrollView.canScrollVertically(1);
            }
        });
    }
}
