package com.hongyue.app.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 *  Description:  预览控制
 *  Author: Charlie
 *  Data: 2019/3/20  12:44
 *  Declare: None
 */

public class AutoFitFrameLayout extends FrameLayout {

    private double targetAspectRatio = -1.0;        // initially use default window size

    private Context mContext;

    public AutoFitFrameLayout(Context context) {
        super(context);
        this.mContext = context;
    }

    public AutoFitFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public void setAspectRatio(double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }

        if (targetAspectRatio != aspectRatio) {
            targetAspectRatio = aspectRatio;
            requestLayout();
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (targetAspectRatio > 0) {

            //用于全面屏适配

            int widthPadding = 0;
            int heightPadding = 0;

            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            initialWidth += widthPadding;
            initialHeight += heightPadding;

            // padding
            int horizontalPadding = getPaddingLeft() + getPaddingRight();
            int verticalPadding = getPaddingTop() + getPaddingBottom();

            initialWidth -= horizontalPadding;
            initialHeight -= verticalPadding;

            double viewAspectRatio = (double) initialWidth / initialHeight; //Surface区域的宽长比
            double aspectDifference = targetAspectRatio / viewAspectRatio - 1; //宽长比差值

            if (Math.abs(aspectDifference) < 0.01) { //可忽略不计 +- 0.01
                //no changes
            } else {

                if (aspectDifference > 0) { // 预览尺寸较小，会出现上下剪切

                    initialHeight = (int) (initialWidth / viewAspectRatio); //矫正高度适配全面屏幕，全屏预览

                } else {

                    initialWidth = (int) (initialHeight * targetAspectRatio); //矫正宽度

                }
                initialWidth += horizontalPadding;
                initialHeight += verticalPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }

        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}