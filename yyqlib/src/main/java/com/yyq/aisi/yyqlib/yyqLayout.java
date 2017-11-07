package com.yyq.aisi.yyqlib;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import java.nio.file.Path;

public class yyqLayout extends RelativeLayout {
    //顶部左右、底部左右距离
    private  float[] radii =new float[8];
    //剪裁图片路径
    private Path mClipPath;


    public yyqLayout(Context context) {
        super(context);
    }

    public yyqLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public yyqLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
