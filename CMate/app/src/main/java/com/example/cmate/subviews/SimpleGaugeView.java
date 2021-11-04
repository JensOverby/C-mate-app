// SimpleGaugeView.java
package com.example.cmate.subviews;

        import android.animation.Animator;
        import android.animation.AnimatorListenerAdapter;
        import android.animation.ValueAnimator;
        import android.animation.ValueAnimator.AnimatorUpdateListener;
        import android.content.Context;
        import android.content.res.TypedArray;
        import android.graphics.Canvas;
        import android.graphics.Color;
        import android.graphics.LinearGradient;
        import android.graphics.Paint;
        import android.graphics.Rect;
        import android.graphics.RectF;
        import android.graphics.Shader;
        import android.graphics.Paint.Cap;
        import android.graphics.Paint.Style;
        import android.graphics.Shader.TileMode;
        import android.os.Parcel;
        import android.os.Parcelable;
        import android.util.AttributeSet;
        import android.view.View;

        import androidx.annotation.ColorInt;

        import com.example.cmate.R;
        import com.example.cmate.R.styleable;

        import kotlin.TypeCastException;

public final class SimpleGaugeView extends View {
    private float value;
    @ColorInt
    private int barColor;
    @ColorInt
    private int fillColor;
    @ColorInt
    private int textColor;
    @ColorInt
    private int labelColor;
    private String labelText;
    private float minValue;
    private float maxValue;
    private boolean showValue;
    private float startAngle;
    private float sweepAngle;
    private float barWidth;
    private float fillBarWidth;
    private float textSize;
    private float labelSize;
    private int textOffset;
    private Cap barStrokeCap;
    @ColorInt
    private int fillColorStart;
    @ColorInt
    private int fillColorEnd;
    private Paint painter = null;
    private RectF drawingRect = null;
    private Rect textBounds = null;
    private boolean isInitialising;
    private boolean isAnimating;
    private Shader gradientShader;
    private SimpleGaugeView.OnValueChangeListener onValueChangeListener;
    private static final String TAG = SimpleGaugeView.class.getSimpleName();
    private static final float DEFAULT_VALUE_TEXT_SCALE = 4.0F;
    private static final float DEFAULT_LABEL_TEXT_SCALE = 3.0F;
    private static final float DEFAULT_START_ANGLE = 135.0F;
    private static final float DEFAULT_SWEEP_ANGLE = 270.0F;
    private static final int DEFAULT_FILL_COLOR = -11702621;
    private static final int DEFAULT_BAR_COLOR = -4013374;
    private static final float DEFAULT_BAR_WIDTH = 12.0F;
    private static final int DEFAULT_MAX_VALUE = 100;
    //public static final SimpleGaugeView.Companion Companion = new SimpleGaugeView.Companion((DefaultConstructorMarker)null);

    public final float getValue() {
        return this.value;
    }

    public final void setValue(float value) {
        if (this.minValue <= value) {
            if (this.maxValue >= value) {
                this.value = value;
                this.invalidate();
                if (this.onValueChangeListener != null) {
                    this.onValueChangeListener.onValueChanged(this, (int) this.value);
                }
            }
        }

    }

    public final int getBarColor() {
        return this.barColor;
    }

    public final void setBarColor(int value) {
        this.barColor = value;
        this.invalidate();
    }

    public final int getFillColor() {
        return this.fillColor;
    }

    public final void setFillColor(int value) {
        this.fillColor = value;
        this.invalidate();
    }

    public final int getTextColor() {
        return this.textColor;
    }

    public final void setTextColor(int value) {
        this.textColor = value;
        this.invalidate();
    }

    public final int getLabelColor() {
        return this.labelColor;
    }

    public final void setLabelColor(int value) {
        this.labelColor = value;
        this.invalidate();
    }

    public final String getLabelText() {
        return this.labelText;
    }

    public final void setLabelText(String value) {
        //Intrinsics.checkParameterIsNotNull(value, "value");
        this.labelText = value;
        this.invalidate();
    }

    public final float getMinValue() {
        return this.minValue;
    }

    public final float getMaxValue() {
        return this.maxValue;
    }

    protected void onDraw(Canvas canvas) {
        //Intrinsics.checkParameterIsNotNull(canvas, "canvas");
        this.drawBackground(canvas, this.drawingRect);
        this.drawForeground(canvas, this.drawingRect);
        this.drawTextLabels(canvas, this.drawingRect);
    }

    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        this.drawingRect.set((float)this.getPaddingLeft() + this.barWidth, (float)this.getPaddingTop() + this.barWidth, (float)(w - this.getPaddingRight()) - this.barWidth, (float)(h - this.getPaddingBottom()) - this.barWidth);
        if (this.fillColorStart != 0 && this.fillColorEnd != 0) {
            this.gradientShader = (Shader)(new LinearGradient(0.0F, 0.0F, (float)w, (float)h, this.fillColorStart, this.fillColorEnd, TileMode.CLAMP));
        }

    }

    protected Parcelable onSaveInstanceState() {
        SimpleGaugeView.SimpleGaugeViewState it = new SimpleGaugeView.SimpleGaugeViewState(super.onSaveInstanceState());
        //boolean var2 = false;
        //boolean var3 = false;
        //int var5 = false;
        it.setValue(this.value);
        it.setBarColor(this.barColor);
        it.setFillColor(this.fillColor);
        it.setTextColor(this.textColor);
        it.setLabelColor(this.labelColor);
        it.setLabelText(this.labelText);
        return (Parcelable)it;
    }

    protected void onRestoreInstanceState(Parcelable state) {
        this.isInitialising = true;
        if (state instanceof SimpleGaugeView.SimpleGaugeViewState) {
            super.onRestoreInstanceState(((SimpleGaugeView.SimpleGaugeViewState)state).getSuperState());
            this.maxValue = ((SimpleGaugeView.SimpleGaugeViewState)state).getMaxValue();
            this.setBarColor(((SimpleGaugeView.SimpleGaugeViewState)state).getBarColor());
            this.setFillColor(((SimpleGaugeView.SimpleGaugeViewState)state).getFillColor());
            this.setTextColor(((SimpleGaugeView.SimpleGaugeViewState)state).getTextColor());
            this.setLabelColor(((SimpleGaugeView.SimpleGaugeViewState)state).getLabelColor());
            this.setLabelText(((SimpleGaugeView.SimpleGaugeViewState)state).getLabelText());
            this.setValue(((SimpleGaugeView.SimpleGaugeViewState)state).getValue());
        } else {
            super.onRestoreInstanceState(state);
        }

        this.isInitialising = false;
        this.invalidate();
    }

    public void invalidate() {
        if (!this.isInitialising) {
            super.invalidate();
        }

    }

    /*public final void animateTo(int toValue, long animateTime) {
        if (this.value != toValue) {
            if (animateTime == 0L) {
                this.setValue(toValue);
            } else if (!this.isAnimating) {
                ValueAnimator var4 = ValueAnimator.ofInt(new int[]{this.value, toValue});
                //boolean var5 = false;
                //boolean var6 = false;
                //int var8 = false;
                var4.addUpdateListener((AnimatorUpdateListener)(new SimpleGaugeView$animateTo$$inlined$apply$lambda$1(this, animateTime, toValue)));
                var4.addListener((AnimatorListener)(new SimpleGaugeView$animateTo$$inlined$apply$lambda$2(this, animateTime, toValue)));
                //float var10001 = (float)animateTime;
                //int var9 = toValue - this.value;
                //float var10 = (float)animateTime;;
                //boolean var12 = false;
                int var13 = Math.abs(toValue - this.value);
                var4.setDuration((long)((float)animateTime * ((float)var13 / (float)(this.maxValue - this.minValue))));
                var4.setInterpolator((TimeInterpolator)(new LinearInterpolator()));
                var4.start();
            }
        }

    }*/

    // $FF: synthetic method
    /*public static void animateTo$default(SimpleGaugeView var0, int var1, long var2, int var4, Object var5) {
        if ((var4 & 2) != 0) {
            var2 = 0L;
        }

        var0.animateTo(var1, var2);
    }*/

    public final void setOnValueChangeListener(SimpleGaugeView.OnValueChangeListener onValueChangeListener) {
        //Intrinsics.checkParameterIsNotNull(onValueChangeListener, "onValueChangeListener");
        this.onValueChangeListener = onValueChangeListener;
    }

    private final void drawForeground(Canvas canvas, RectF rect) {
        float sweepAmount = (float)(this.value - this.minValue) / (float)(this.maxValue - this.minValue) * this.sweepAngle;
        this.painter.setStrokeCap(this.barStrokeCap);
        this.painter.setStrokeWidth(this.fillBarWidth);
        if (this.gradientShader != null) {
            this.painter.setShader(this.gradientShader);
        } else {
            this.painter.setColor(this.fillColor);
        }

        canvas.drawArc(rect, this.startAngle, sweepAmount, false, this.painter);
        this.painter.setShader((Shader)null);
    }

    private final void drawTextLabels(Canvas canvas, RectF rect) {
        this.painter.setStyle(Style.FILL);
        float labelPosY;
        if (this.showValue) {
            //StringCompanionObject var4 = StringCompanionObject.INSTANCE;
            //String var5 = "%d";
            String valueText = String.format("%.1f", value);
            //Object[] var6 = new Object[]{this.value};
            //boolean var7 = false;
            //String var10000 = String.format(valueText, Arrays.copyOf(var6, var6.length));
            //Intrinsics.checkNotNullExpressionValue(var10000, "java.lang.String.format(format, *args)");
            //String valueText = var10000;
            this.painter.setColor(this.textColor);
            this.painter.setTextSize(this.textSize);
            this.painter.getTextBounds(valueText, 0, valueText.length(), this.textBounds);
            labelPosY = rect.centerX() - (float)this.textBounds.width() * 0.5F;
            float textPosY = (float)this.textOffset + rect.centerY();

            if (labelText.isEmpty()) {
                textPosY += (float)this.textBounds.height() * 0.5F;
            }

            canvas.drawText(valueText, labelPosY, textPosY, this.painter);
        }

        if (!labelText.isEmpty()) {
            this.painter.setColor(this.labelColor);
            this.painter.setTextSize(this.labelSize);
            this.painter.getTextBounds(this.labelText, 0, this.labelText.length(), this.textBounds);
            float labelPosX = rect.centerX() - (float)this.textBounds.width() * 0.5F;
            labelPosY = (float)this.textOffset + rect.centerY() + (float)this.textBounds.height() + (float)this.getPaddingTop();
            canvas.drawText(this.labelText, labelPosX, labelPosY, this.painter);
        }

    }

    private final void drawBackground(Canvas canvas, RectF rect) {
        if (this.getBackground() != null) {
            this.getBackground().draw(canvas);
        }

        this.painter.setColor(this.barColor);
        this.painter.setStyle(Style.STROKE);
        this.painter.setStrokeCap(this.barStrokeCap);
        this.painter.setStrokeWidth(this.barWidth);
        canvas.drawArc(rect, this.startAngle, this.sweepAngle, false, this.painter);
    }

    private final float getTextSize(TypedArray $this$getTextSize, int index, float defaultSize) {
        return (float)$this$getTextSize.getDimensionPixelSize(index, (int)defaultSize);
    }

    /*public SimpleGaugeView(Context context, Paint painter) {
        this.painter = painter;
        this(context, (AttributeSet)null);
    }*/

    public SimpleGaugeView(Context context, AttributeSet attrs) {
        //this(context, attrs, 0);
        this(context, attrs, R.attr.seekBarStyle);// 0, 4, (DefaultConstructorMarker)null);

    }

    public SimpleGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.labelText = new String();
        this.painter = new Paint(1);
        this.drawingRect = new RectF();
        this.textBounds = new Rect();
        TypedArray var4 = context.getTheme().obtainStyledAttributes(attrs, styleable.SimpleGaugeView, defStyleAttr, 0);
        //boolean var5 = false;
        //boolean var6 = false;
        TypedArray $this$apply = var4;
        boolean var8 = false;

        try {
            this.isInitialising = true;
            this.barWidth = $this$apply.getDimension(R.styleable.SimpleGaugeView_gaugeView_barWidth, DEFAULT_BAR_WIDTH);
            this.fillBarWidth = $this$apply.getDimension(R.styleable.SimpleGaugeView_gaugeView_fillBarWidth, this.barWidth * (float)2);
            this.setBarColor($this$apply.getColor(R.styleable.SimpleGaugeView_gaugeView_barColor, DEFAULT_BAR_COLOR));
            this.setFillColor($this$apply.getColor(R.styleable.SimpleGaugeView_gaugeView_fillColor, DEFAULT_FILL_COLOR));
            this.fillColorStart = $this$apply.getColor(R.styleable.SimpleGaugeView_gaugeView_fillColorStart, Color.TRANSPARENT);
            this.fillColorEnd = $this$apply.getColor(R.styleable.SimpleGaugeView_gaugeView_fillColorEnd, Color.TRANSPARENT);
            int var9 = $this$apply.getInt(R.styleable.SimpleGaugeView_gaugeView_strokeCap, 0);
            //boolean var10 = false;
            //boolean var11 = false;
            //int var13 = false;
            Cap var10001;
            switch(var9) {
                case 1:
                    var10001 = Cap.ROUND;
                    break;
                case 2:
                    var10001 = Cap.SQUARE;
                    break;
                default:
                    var10001 = Cap.BUTT;
            }

            this.barStrokeCap = var10001;

            minValue = $this$apply.getFloat(R.styleable.SimpleGaugeView_gaugeView_minValue, 0);
            maxValue = $this$apply.getFloat(R.styleable.SimpleGaugeView_gaugeView_maxValue, DEFAULT_MAX_VALUE);
            showValue = $this$apply.getBoolean(R.styleable.SimpleGaugeView_gaugeView_showValue, true);
            value = $this$apply.getFloat(R.styleable.SimpleGaugeView_gaugeView_value, minValue);

            startAngle = $this$apply.getFloat(R.styleable.SimpleGaugeView_gaugeView_startAngle, DEFAULT_START_ANGLE);
            sweepAngle = $this$apply.getFloat(R.styleable.SimpleGaugeView_gaugeView_sweepAngle, DEFAULT_SWEEP_ANGLE);

            textSize = getTextSize($this$apply, R.styleable.SimpleGaugeView_gaugeView_textSize, this.barWidth * DEFAULT_VALUE_TEXT_SCALE);
            textColor = $this$apply.getColor(R.styleable.SimpleGaugeView_gaugeView_textColor, fillColor);
            textOffset = $this$apply.getDimensionPixelOffset(R.styleable.SimpleGaugeView_gaugeView_textOffset, 0);

            labelSize = getTextSize($this$apply, R.styleable.SimpleGaugeView_gaugeView_labelSize, this.barWidth * DEFAULT_LABEL_TEXT_SCALE);
            labelColor = $this$apply.getColor(R.styleable.SimpleGaugeView_gaugeView_labelColor, barColor);

            String var16 = $this$apply.getString(R.styleable.SimpleGaugeView_gaugeView_labelText);
            if (var16 == null) {
                var16 = new String();
            }

            this.setLabelText(var16);
            this.isInitialising = false;
            this.invalidate();
        } finally {
            var4.recycle();
        }

        this.setSaveEnabled(true);
    }

    // $FF: synthetic method
    public static final boolean access$isAnimating$p(SimpleGaugeView $this) {
        return $this.isAnimating;
    }

    // $FF: synthetic method
    public static final void access$setAnimating$p(SimpleGaugeView $this, boolean var1) {
        $this.isAnimating = var1;
    }

    public interface OnValueChangeListener {
        void onValueChanged(SimpleGaugeView var1, float var2);
    }

    public static final class SimpleGaugeViewState extends BaseSavedState {
        private float value;
        private int maxValue;
        private int barColor;
        private int fillColor;
        private int textColor;
        private int labelColor;
        private String labelText;
        public final Creator CREATOR;

        public final float getValue() {
            return this.value;
        }

        public final void setValue(float var) {
            this.value = var;
        }

        public final int getMaxValue() {
            return this.maxValue;
        }

        public final void setMaxValue(int var) {
            this.maxValue = var;
        }

        public final int getBarColor() {
            return this.barColor;
        }

        public final void setBarColor(int var) {
            this.barColor = var;
        }

        public final int getFillColor() {
            return this.fillColor;
        }

        public final void setFillColor(int var) {
            this.fillColor = var;
        }

        public final int getTextColor() {
            return this.textColor;
        }

        public final void setTextColor(int var) {
            this.textColor = var;
        }

        public final int getLabelColor() {
            return this.labelColor;
        }

        public final void setLabelColor(int var) {
            this.labelColor = var;
        }

        public final String getLabelText() {
            return this.labelText;
        }

        public final void setLabelText(String var) {
            this.labelText = var;
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            if (out != null) {
                //boolean var4 = false;
                //boolean var5 = false;
                //int var7 = false;
                out.writeFloat(this.value);
                out.writeFloat(this.maxValue);
                out.writeInt(this.barColor);
                out.writeInt(this.fillColor);
                out.writeInt(this.textColor);
                out.writeInt(this.labelColor);
                out.writeString(this.labelText);
            }

        }

        public SimpleGaugeViewState(Parcelable parcelable) {
            super(parcelable);
            this.labelText = new String();
            this.CREATOR = (Creator)(new Creator() {

                /*public SimpleGaugeView.SimpleGaugeViewState createFromParcel(Parcel source) {
                    return new SimpleGaugeView.SimpleGaugeViewState(source);
                }*/

                // $FF: synthetic method
                // $FF: bridge method
                public Object createFromParcel(Parcel var1) {
                    return this.createFromParcel(var1);
                }

                //public SimpleGaugeView.SimpleGaugeViewState[] newArray(int size) {
                //    return new SimpleGaugeView.SimpleGaugeViewState[size];
                //}

                // $FF: synthetic method
                // $FF: bridge method
                public Object[] newArray(int var1) {
                    return this.newArray(var1);
                }
            });
        }

        public SimpleGaugeViewState(Parcel parcel) {
            super(parcel);
            this.labelText = new String();
            this.CREATOR = (Creator)(new Creator() {
                //public SimpleGaugeView.SimpleGaugeViewState createFromParcel(Parcel source) {
                //    return new SimpleGaugeView.SimpleGaugeViewState(source);
                //}

                // $FF: synthetic method
                // $FF: bridge method
                public Object createFromParcel(Parcel var1) {
                    return this.createFromParcel(var1);
                }

                public SimpleGaugeView.SimpleGaugeViewState[] newArray_(int size) {
                    return new SimpleGaugeView.SimpleGaugeViewState[size];
                }

                // $FF: synthetic method
                // $FF: bridge method
                public Object[] newArray(int var1) {
                    return this.newArray(var1);
                }
            });
            this.value = parcel.readFloat();
            this.maxValue = parcel.readInt();
            this.barColor = parcel.readInt();
            this.fillColor = parcel.readInt();
            this.textColor = parcel.readInt();
            this.labelColor = parcel.readInt();
            String var10001 = parcel.readString();
            if (var10001 == null) {
                var10001 = new String();
            }

            this.labelText = var10001;
        }
    }

    public static final class Companion {
        public final String getTAG() {
            return SimpleGaugeView.TAG;
        }

        private Companion() {
        }

    }
}

/*final class SimpleGaugeView$animateTo$$inlined$apply$lambda$1 implements AnimatorUpdateListener {
    // $FF: synthetic field
    final SimpleGaugeView this$0;
    // $FF: synthetic field
    final long $animateTime$inlined;
    // $FF: synthetic field
    final int $toValue$inlined;

    SimpleGaugeView$animateTo$$inlined$apply$lambda$1(SimpleGaugeView var1, long var2, int var4) {
        this.this$0 = var1;
        this.$animateTime$inlined = var2;
        this.$toValue$inlined = var4;
    }

    public final void onAnimationUpdate(ValueAnimator it) {
        SimpleGaugeView var10000 = this.this$0;
        Object var10001 = it.getAnimatedValue();
        if (var10001 == null) {
            throw new TypeCastException("null cannot be cast to non-null type kotlin.Int");
        } else {
            var10000.setValue((Integer)var10001);
        }
    }
}

final class SimpleGaugeView$animateTo$$inlined$apply$lambda$2 extends AnimatorListenerAdapter {
    // $FF: synthetic field
    final SimpleGaugeView this$0;
    // $FF: synthetic field
    final long $animateTime$inlined;
    // $FF: synthetic field
    final int $toValue$inlined;

    SimpleGaugeView$animateTo$$inlined$apply$lambda$2(SimpleGaugeView var1, long var2, int var4) {
        this.this$0 = var1;
        this.$animateTime$inlined = var2;
        this.$toValue$inlined = var4;
    }

    public void onAnimationStart(Animator animation) {
        SimpleGaugeView.access$setAnimating$p(this.this$0, true);
    }

    public void onAnimationEnd(Animator animation) {
        SimpleGaugeView.access$setAnimating$p(this.this$0, false);
    }
}*/
