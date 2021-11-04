package com.example.cmate.subviews;

//import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.cmate.R;
import com.example.cmate.R.styleable;

/*import kotlin.Metadata;
import kotlin.jvm.JvmOverloads;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;*/


public final class BatteryView extends View {
    private float radius;
    private boolean isCharging;
    private PaintDrawable topPaint;
    private Rect topRect;
    private int topPaintWidthPercent;
    private int topPaintHeightPercent;
    private Paint borderPaint;
    private RectF borderRect;
    private int borderStrokeWidthPercent;
    private float borderStroke;
    private Paint percentPaint;
    private RectF percentRect;
    private float percentRectTopMin;
    private int percent;
    private RectF chargingRect;
    private Bitmap chargingBitmap;
    private Paint painter = null;
    private TypedArray tArray;
    private float textSize;
    private static final float DEFAULT_VALUE_TEXT_SCALE = 0.3F;

    private void init(AttributeSet attrs) {

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BatteryView,
                0, 0);

        try {
            this.percent = a.getInt(styleable.BatteryView_bv_percent, 0);
            this.isCharging = a.getBoolean(styleable.BatteryView_bv_charging, false);
        } finally {
            a.recycle();
        }
    }

    //@SuppressLint({"DrawAllocation"})
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = View.getDefaultSize(this.getSuggestedMinimumWidth(), widthMeasureSpec);
        int measureHeight = (int)((float)measureWidth * 1.8F);
        this.setMeasuredDimension(measureWidth, measureHeight);
        this.radius = this.borderStroke / (float)2;
        this.borderStroke = (float)(this.borderStrokeWidthPercent * measureWidth) / (float)100;
        int topLeft = measureWidth * ((100 - this.topPaintWidthPercent) / 2) / 100;
        int topRight = measureWidth - topLeft;
        int topBottom = this.topPaintHeightPercent * measureHeight / 100;
        this.topRect = new Rect(topLeft, 0, topRight, topBottom);
        float borderLeft = this.borderStroke / (float)2;
        float borderTop = (float)topBottom + this.borderStroke / (float)2;
        float borderRight = (float)measureWidth - this.borderStroke / (float)2;
        float borderBottom = (float)measureHeight - this.borderStroke / (float)2;
        this.borderRect = new RectF(borderLeft, borderTop, borderRight, borderBottom);
        float progressLeft = this.borderStroke;
        this.percentRectTopMin = (float)topBottom + this.borderStroke;
        float progressRight = (float)measureWidth - this.borderStroke;
        float progressBottom = (float)measureHeight - this.borderStroke;
        this.percentRect = new RectF(progressLeft, this.percentRectTopMin, progressRight, progressBottom);
        float chargingLeft = this.borderStroke;
        float chargingTop = (float)topBottom + this.borderStroke;
        float chargingRight = (float)measureWidth - this.borderStroke;
        float chargingBottom = (float)measureHeight - this.borderStroke;
        float diff = chargingBottom - chargingTop - (chargingRight - chargingLeft);
        chargingTop += diff / (float)2;
        chargingBottom -= diff / (float)2;
        this.chargingRect = new RectF(chargingLeft, chargingTop, chargingRight, chargingBottom);
    }

    protected void onDraw(Canvas canvas) {
        //Intrinsics.checkParameterIsNotNull(canvas, "canvas");
        this.drawTop(canvas);
        this.drawBody(canvas);

        this.drawProgress(canvas, this.percent);
        if (isCharging)
            this.drawCharging(canvas);

        drawTextLabels(canvas);

        /*if (!this.isCharging) {
            this.drawProgress(canvas, this.percent);
        } else {
            this.drawCharging(canvas);
        }*/

    }

    private void drawTop(Canvas canvas) {
        this.topPaint.setBounds(this.topRect);
        this.topPaint.setCornerRadii(new float[]{this.radius, this.radius, this.radius, this.radius, 0.0F, 0.0F, 0.0F, 0.0F});
        this.topPaint.draw(canvas);
    }

    private void drawBody(Canvas canvas) {
        this.borderPaint.setStrokeWidth(this.borderStroke);
        canvas.drawRoundRect(this.borderRect, this.radius, this.radius, this.borderPaint);
    }

    private void drawProgress(Canvas canvas, int percent) {
        this.percentPaint.setColor(this.getPercentColor(percent));
        this.percentRect.top = this.percentRectTopMin + (this.percentRect.bottom - this.percentRectTopMin) * (float)(100 - percent) / (float)100;
        canvas.drawRect(this.percentRect, this.percentPaint);
    }

    private final void drawTextLabels(Canvas canvas) {
        textSize = getTextSize(tArray, R.styleable.SimpleGaugeView_gaugeView_textSize, borderRect.width() * DEFAULT_VALUE_TEXT_SCALE);
        this.painter.setStyle(Style.FILL);
        float labelPosX;

        String valueText = String.format("%d", percent) + "%";
        this.painter.setColor(Color.GRAY);
        this.painter.setTextSize(this.textSize);
        Rect textBounds = new Rect();
        this.painter.getTextBounds(valueText, 0, valueText.length(), textBounds);
        labelPosX = this.topRect.centerX() - (float)textBounds.width() * 0.5F;
        int textOffset = 60;
        float textPosY = (float)textOffset + this.topRect.centerY();

        textPosY += (float)textBounds.height() * 0.5F;

        canvas.drawText(valueText, labelPosX, textPosY, this.painter);
    }









    private int getPercentColor(int percent) {
        if (percent < 50)
            return Color.RED;
        if (percent < 70)
            return Color.YELLOW;
        return Color.GREEN;
    }

    private void drawCharging(Canvas canvas) {
        if (this.chargingBitmap != null) {
            canvas.drawBitmap(this.chargingBitmap, (Rect)null, this.chargingRect, (Paint)null);
        }

    }

    private Bitmap getBitmap(int drawableId, Integer desireWidth, Integer desireHeight) {
        Drawable drawable = AppCompatResources.getDrawable(this.getContext(), drawableId);
        if (drawable != null) {
            //Intrinsics.checkExpressionValueIsNotNull(var10000, "AppCompatResources.getDr…rawableId) ?: return null");
            //Drawable drawable = var10000;
            Bitmap bitmap = Bitmap.createBitmap(desireWidth != null ? desireWidth : drawable.getIntrinsicWidth(), desireHeight != null ? desireHeight : drawable.getIntrinsicHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
        return null;
    }

    // $FF: synthetic method
    /*static Bitmap getBitmap$default(BatteryView var0, int var1, Integer var2, Integer var3, int var4, Object var5) {
        if ((var4 & 2) != 0) {
            var2 = (Integer)null;
        }

        if ((var4 & 4) != 0) {
            var3 = (Integer)null;
        }

        return var0.getBitmap(var1, var2, var3);
    }*/

    public final void setIsCharging(boolean value) {
        if (value != this.isCharging) {
            this.isCharging = value;
            this.invalidate();
        }
    }

    /*public final void charge() {
        this.isCharging = true;
        this.invalidate();
    }

    public final void unCharge() {
        this.isCharging = false;
        this.invalidate();
    }*/

    public final boolean getCharging() {
        return isCharging;
    }

    public final void setPercent(int percent) {
        if (percent <= 100 && percent >= 0) {
            this.percent = percent;
            this.invalidate();
        }
    }

    public final int getPercent() {
        return this.percent;
    }

    private final float getTextSize(TypedArray $this$getTextSize, int index, float defaultSize) {
        return (float)$this$getTextSize.getDimensionPixelSize(index, (int)defaultSize);
    }

    //@JvmOverloads
    public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        //Intrinsics.checkParameterIsNotNull(context, "context");
        super(context, attrs, defStyleAttr);
        this.topPaint = new PaintDrawable(Color.WHITE);
        this.topRect = new Rect();
        this.topPaintWidthPercent = 50;
        this.topPaintHeightPercent = 8;

        this.painter = new Paint(1);

        /*Paint var4 = new Paint();
        boolean var5 = false;
        boolean var6 = false;
        boolean var8 = false;
        var4.setColor(-16776961);
        var4.setStyle(Style.STROKE);*/
        this.borderPaint = new Paint();
        this.borderPaint.setColor(Color.BLUE);
        this.borderPaint.setStyle(Style.STROKE);
        this.borderRect = new RectF();
        this.borderStrokeWidthPercent = 8;
        this.percentPaint = new Paint();
        this.percentRect = new RectF();
        this.chargingRect = new RectF();

        this.init(attrs);
        this.chargingBitmap = getBitmap(R.drawable.ic_charging, (Integer)null, (Integer)null);
        tArray = context.getTheme().obtainStyledAttributes(attrs, styleable.SimpleGaugeView, defStyleAttr, 0);
        //this.chargingBitmap = getBitmap$default(this, R.drawable.ic_charging, (Integer)null, (Integer)null, 6, (Object)null);
    }

    // $FF: synthetic method
    /*public BatteryView(Context var1, AttributeSet var2, int var3, int var4, DefaultConstructorMarker var5) {
        if ((var4 & 2) != 0) {
            var2 = (AttributeSet)null;
        }

        if ((var4 & 4) != 0) {
            var3 = 0;
        }

        this(var1, var2, var3);
    }*/

    //@JvmOverloads
    public BatteryView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarStyle);// 0, 4, (DefaultConstructorMarker)null);
    }

    //@JvmOverloads
    /*public BatteryView(Context context) {
        this(context, (AttributeSet)null, 0, 6, (DefaultConstructorMarker)null);
    }*/
}