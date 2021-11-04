package com.example.cmate;

import android.view.View;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.Date;

import static java.lang.Math.abs;

public class TailAnimator {

    float gustLevel = 0;
    int direction = 1;

    SpringAnimation mAnimation;

    public TailAnimator(View v) {
        //view = v;
        mAnimation = new SpringAnimation(v, DynamicAnimation.TRANSLATION_Y);
        mAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                float pos = 10.f * gustLevel * direction;
                float error = abs(value - pos);
                if (gustLevel > 0) {
                    if (error < 1) {
                        direction *= -1;
                    }
                }
                else {
                    if (error <= 1) {
                        mAnimation.skipToEnd();
                        return;
                    }
                }
                mAnimation.animateToFinalPosition(pos);
            }
        });
    }

    void setGustLevel(float level) {
        gustLevel = level;

        if (gustLevel > 0 && !mAnimation.isRunning()) {
            mAnimation.setStartValue(0);
            mAnimation.setStartVelocity(0);
            mAnimation.setMinimumVisibleChange(0.0001f);
            SpringForce springForce = new SpringForce();
            springForce.setFinalPosition(0.001f);
            springForce.setStiffness(50.0f);
            mAnimation.setSpring(springForce);
            mAnimation.start();
        }
    }
}
