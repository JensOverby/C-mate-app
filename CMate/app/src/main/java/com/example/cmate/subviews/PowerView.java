package com.example.cmate.subviews;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.example.cmate.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Semaphore;

import static androidx.core.view.ViewCompat.generateViewId;

public class PowerView {

    private Activity mActivity;
    private ArrayList<ProgressBar> barList = new ArrayList<ProgressBar>();
    private ConstraintLayout parentLayout;
    private TextView textView;
    //private boolean isActive = false;
    //float mValue;
    private int currentBarId = 0;

    private volatile float v=0;
    private volatile float s=0;
    private volatile float s_target=0;
    private long lastTime = 0;
    private volatile float interval = 1;

    private PowerRunnable powerRunnable;

    private class PowerRunnable implements Runnable {
        Activity mActivity;
        volatile boolean isRunning = false;
        private final int sleeptime = 199;
        private final int countDownFrom = 10000/sleeptime;
        volatile int countDown = countDownFrom;
        private long tLast = 0;
        //final Semaphore mutex = new Semaphore(0);

        PowerRunnable (Activity activity) {
            mActivity = activity;
            //try { mutex.acquire(); } catch (InterruptedException e) { e.printStackTrace(); }
        }

        void keepAlive() {
            countDown = countDownFrom;
        }

        @Override
        public void run() {
            tLast = (new Date()).getTime() - 100;
            countDown = countDownFrom;
            isRunning = true;
            //float dt = sleeptime/1000.f;
            int s_old = 0;
            boolean firstTime = true;
            while (true) {

                long nowTime = (new Date()).getTime();
                float dt = ((nowTime-tLast)/1000f)/interval;
                tLast = nowTime;

                if (v != 0) {
                    s += v * dt;
                    if (s > s_target) {
                        if (v > 0) {
                            s = s_target;
                            v = 0;
                        }
                    }
                    else {
                        if (s < 0) {
                            s = 0;
                            v = 0;
                        }
                        else if (v < 0) {
                            s = s_target;
                            v = 0;
                        }
                    }
                }

                if ((int)s != s_old) {
                    if (firstTime) {
                        firstTime = false;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLevel(s, false);
                                textView.setVisibility(View.VISIBLE);
                                textView.invalidate();
                            }
                        });
                    }
                    else {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLevel(s, false);
                            }
                        });
                    }
                    s_old = (int)s;
                }
                countDown--;
                if (countDown == 0) {
                    s=0;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLevel(0, true);
                            //mutex.release();
                        }
                    });
                    //try { mutex.acquire(); } catch (InterruptedException e) { e.printStackTrace(); }
                    System.out.println("shutdown powerview");
                    isRunning = false;
                    return;
                }
                try {
                    Thread.sleep(sleeptime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //float dt = sleeptime/1000.f;

    public void setValue(float value) {
        if (value > 499)
            value = 499;
        if (value < 0)
            value = 0;

        long nowTime = (new Date()).getTime();
        interval = (nowTime-lastTime) / 1000f;
        if (interval > 3 || interval < 0.5)
            interval = 2;
        lastTime = nowTime;
        //dt = (powerRunnable.sleeptime/1000f) / interval;

        s_target = value;

        if (s_target > 0) {
            float dS = s_target - s;
            v = dS / interval;
            if (!powerRunnable.isRunning) {
                System.out.println("starting powerview");
                new Thread(powerRunnable).start();
            }
            else
                powerRunnable.keepAlive();
        }
    }

    public PowerView(Activity activity, int resID) {
        mActivity = activity;
        // R.id.power_frame
        parentLayout = (ConstraintLayout) mActivity.findViewById(resID);
        textView = new TextView(mActivity);
        textView.setId(generateViewId());
        textView.setText("watts");
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.bottomToBottom = R.id.power_frame;
        textView.setLayoutParams(params);
        textView.setVisibility(View.INVISIBLE);
        parentLayout.addView(textView);

        powerRunnable = new PowerRunnable(mActivity);

        ProgressBar currentBar = null;
        while(barList.size() < 10) {
            ProgressBar bar = new ProgressBar(mActivity, null, android.R.attr.progressBarStyleHorizontal);
            bar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
            params = new ConstraintLayout.LayoutParams(150, 15);
            if (currentBar == null)
                params.bottomToTop = textView.getId();
            else {
                params.bottomToTop = currentBar.getId();
                currentBar.setProgress(100);
            }
            bar.setId(generateViewId());
            bar.setLayoutParams(params);
            bar.setVisibility(View.INVISIBLE);
            if (barList.size() % 2 == 1)
                bar.setRotation(180);
            parentLayout.addView(bar);
            barList.add(bar);
            bar.setProgress(0);
            currentBar = bar;
            //power.bringToFront();
        }
    }

    void setLevel(float value, boolean deactivate) {
        int barId = (int) (value / 50);
        while (barId > currentBarId) {
            ProgressBar bar = barList.get(currentBarId);
            bar.setProgress(100);
            bar.setVisibility(View.VISIBLE);
            bar.invalidate();
            currentBarId++;
        }
        while (barId < currentBarId) {
            ProgressBar bar = barList.get(currentBarId);
            bar.setVisibility(View.INVISIBLE);
            bar.invalidate();
            currentBarId--;
        }

        ProgressBar bar = barList.get(currentBarId);
        if (deactivate) {
            bar.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.INVISIBLE);
        }
        else {
            bar.setVisibility(View.VISIBLE);
            bar.setProgress((int) (2*(value - currentBarId * 50))); //, true); animate requires API level 24. Current is 23
            textView.setText(Integer.toString((int) value) + " watt");
        }
        bar.invalidate();
        textView.invalidate();
    }
}
