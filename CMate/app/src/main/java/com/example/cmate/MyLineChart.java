package com.example.cmate;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;

public class MyLineChart extends LineChart {

    public MyLineChart(Context context) {super(context);}
    public MyLineChart(Context context, AttributeSet attrs) {super(context, attrs);}
    public MyLineChart(Context context, AttributeSet attrs, int defStyle) {super(context, attrs, defStyle);}

    private float myMinScale = 1;
    public void setMyMinScale(float val) {
        myMinScale = val;
    }

    //@Override
    protected void autoScale() {
        final float fromX = getLowestVisibleX();
        final float toX = getHighestVisibleX();

        mData.calcMinMaxY(fromX, toX);

        mXAxis.calculate(mData.getXMin(), mData.getXMax());

        // calculate axis range (min / max) according to provided data

        float yminleft = mData.getYMin(YAxis.AxisDependency.LEFT);
        float ymaxleft = mData.getYMax(YAxis.AxisDependency.LEFT);
        float dyleft = (myMinScale - (ymaxleft-yminleft))/2;
        if (dyleft > 0) {
            yminleft -= dyleft;
            ymaxleft += dyleft;
        }

        float yminright = mData.getYMin(YAxis.AxisDependency.RIGHT);
        float ymaxright = mData.getYMax(YAxis.AxisDependency.RIGHT);
        float dyright = (myMinScale - (ymaxright-yminright))/2;
        if (dyright > 0) {
            yminright -= dyright;
            ymaxright += dyright;
        }

        if (mAxisLeft.isEnabled())
            mAxisLeft.calculate(yminleft, ymaxleft);

        if (mAxisRight.isEnabled())
            mAxisRight.calculate(yminright,ymaxright);

        calculateOffsets();
    }
}
