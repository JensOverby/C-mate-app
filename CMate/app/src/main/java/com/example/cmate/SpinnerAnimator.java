package com.example.cmate;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import org.w3c.dom.Text;

import java.util.Date;

public class SpinnerAnimator {
    volatile float v_omega = 0;
    volatile float v_omega_target = 0;
    float s_angle = 0;
    volatile float a_alpha = 0;
    float t_update = 2;
    long lastTime = 0;

    SpringAnimation mAnimation;
    long t_last = 0;
    volatile boolean docking = false;
    volatile boolean spindown = false;
    float dockingValue = 0;
    TextView rpmView;
    private RpmRunnable rpmRunnable;

    private class RpmRunnable implements Runnable {
        Activity mActivity;
        volatile boolean isRunning = false;
        int rpm_gui = 0;
        private final int sleeptime = 101; // prime number
        private final int countDownFrom = 7000/sleeptime;
        volatile int countDown = 0;

        RpmRunnable (Activity activity) {
            mActivity = activity;
        }

        void keepAlive() {
            countDown = countDownFrom;
            //System.out.println("keepalive");
        }

        @Override
        public void run() {
            isRunning = true;
            countDown = countDownFrom*3;
            while (true) {
                int rpm = (int) (v_omega * 60 / 360.f);
                if (rpm != rpm_gui) {
                    if (rpm > 0)
                        rpm_gui = rpm;
                    else if (docking){
                        rpm_gui = 0;
                        docking = false;
                        break;
                    }

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            rpmView.setText(Integer.toString(rpm_gui));
                            rpmView.invalidate();
                        }
                    });

                }

                if (countDown < 0) {
                    if (rpm <= 0)
                        break;
                    else if (countDown < -50) {
                        v_omega_target = 0;
                        a_alpha = -v_omega / 3;
                        System.out.print("shutdown spinner ALPHA: ");
                        System.out.println(a_alpha);
                        /*try {
                            ((MainActivity) mActivity).serialService.setSpinDown();
                        } catch (NullPointerException e) {
                        }*/
                        break;
                    }
                }

                countDown--;
                //System.out.print("countdown=");
                //System.out.println(countDown);
                if (countDown == 0) {
                    v_omega_target = 0;
                    a_alpha = -v_omega / 2;
                    System.out.print("ALPHA: ");
                    System.out.println(a_alpha);
                    /*try {
                        ((MainActivity) mActivity).serialService.setSpinDown();
                    } catch (NullPointerException e) {
                    }*/
                    if (rpm <= 0) {
                        System.out.println("shutdown spinner");
                        break;
                    }
                }

                try {
                    Thread.sleep(sleeptime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rpmView.setText("");
                    //rpmView.setVisibility(View.INVISIBLE);
                    rpmView.invalidate();
                }
            });
            isRunning = false;
        }
    }

    public SpinnerAnimator(View wings, TextView rpm, Activity activity) {
        rpmView = rpm;
        rpmView.setVisibility(View.INVISIBLE);

        rpmRunnable = new RpmRunnable(activity);

        mAnimation = new SpringAnimation(wings, DynamicAnimation.ROTATION);
        mAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                long t = (new Date()).getTime();

                float dt = (t-t_last)/1000.f;
                t_last = t;
                if (a_alpha != 0) {
                    v_omega += a_alpha * dt;
                    if (a_alpha > 0) {
                        if (v_omega > v_omega_target) {
                            a_alpha = 0;
                            v_omega = v_omega_target;
                        }
                    } else {
                        if (v_omega < v_omega_target) {
                            a_alpha = 0;
                            v_omega = v_omega_target;
                        }
                    }
                    if (v_omega_target == 0 && v_omega < 30) {
                        docking = true;
                        a_alpha = 0;
                        long half_rots = (long)(s_angle/180);
                        dockingValue = (half_rots+1) * 180;
                        v_omega = 30;
                    }
                }

                s_angle += v_omega * dt;

                if (docking && value > dockingValue) {
                    mAnimation.animateToFinalPosition(dockingValue);
                    mAnimation.skipToEnd();
                    //docking = false;
                    spindown = false;
                    v_omega = -1;
                    return;
                }

                mAnimation.animateToFinalPosition(s_angle);
            }
        });
    }

    /*void assureInvisible() {
        if (rpmView.getVisibility() == View.VISIBLE) {
            rpmView.setVisibility(View.INVISIBLE);
            //rpmView.invalidate();
        }
    }*/

    void setRpm(float rpm, boolean startUp) {
        float t;
        if (startUp) {
            spindown = false;
            docking = false;
            t = 12;
            rpmRunnable.keepAlive();
        }
        else {
            long nowTime = (new Date()).getTime();
            long dUpdate = nowTime - lastTime;
            lastTime = nowTime;
            if (dUpdate > 500 && dUpdate < 3000)
                t_update = dUpdate / 1000.f;
            t = t_update;
        }

        //if (docking) {
        //    rpmView.setVisibility(View.INVISIBLE);
        //    rpmView.invalidate();
        //}

        rpmRunnable.keepAlive();

        if (docking || spindown)
            return;

        if (rpm == 0) {
            if (v_omega_target != 0)
                spindown = true;
            t = 12;
        }

        v_omega_target = rpm * 360 / 60.f;

        if (rpm > 0 && !mAnimation.isRunning() && !rpmRunnable.isRunning) {
            rpmView.setVisibility(View.VISIBLE);
            rpmView.invalidate();
            docking = false;
            spindown = false;
            a_alpha = (v_omega_target - v_omega) / t;
            s_angle = 0;
            v_omega = 0;
            mAnimation.setStartValue(0);
            mAnimation.setStartVelocity(0);
            mAnimation.setMinimumVisibleChange(0.0001f);
            SpringForce springForce = new SpringForce();
            springForce.setFinalPosition(0.001f);
            springForce.setStiffness(50.0f);
            mAnimation.setSpring(springForce);
            t_last = (new Date()).getTime();
            mAnimation.start();

            System.out.println("starting spinner");
            new Thread(rpmRunnable).start();
        }
        else
            a_alpha = (v_omega_target - v_omega) / t;
    }
}

