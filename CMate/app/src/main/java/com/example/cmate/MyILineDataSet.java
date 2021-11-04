package com.example.cmate;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.List;

public class MyILineDataSet extends LineDataSet {

    public MyILineDataSet(List<Entry> yVals, String label) {
        super(yVals, label);
    }

    public void setEntry(float y, int maxSize) {
        Entry entry;
        int x = getEntryCount();
        boolean remove = false;
        if (x > maxSize) {
            entry = mValues.get(0); //.remove(0); //.r.remove(e);
            mXMin = (int)entry.getX() + 1;
            mXMax = mXMin + maxSize;
            entry.setX(mXMax);
            entry.setY(y);
            remove = true;
        }
        else
            entry = new Entry(x,y); //(x_index, s[i]);
        if (remove)
            mValues.remove(0);
        addEntry(entry);
    }
}