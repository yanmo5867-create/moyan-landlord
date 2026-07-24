package com.moyan.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import com.moyan.model.Card;

/**
 * 自定义卡牌视图
 * 大王金色高光、小王银色高光、选中上浮+白边
 */
public class CardView extends View {

    private Card card;
    private boolean selected = false;
    private boolean enabled = true;
    private Paint bgPaint, borderPaint, textPaint, suitPaint, glowPaint;
    private RectF cardRect;
    private OnCardClickListener clickListener;

    // 尺寸
    private float cardWidth = 80f;
    private float cardHeight = 120f;
    private float cornerRadius = 8f;

    public interface OnCardClickListener {
        void onCardClick(CardView cardView);
    }

    public CardView(Context context, Card card) {
        super(context);
        this.card = card;
        initPaints();
    }

    private void initPaints() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(28f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        suitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        suitPaint.setTextSize(22f);
        suitPaint.setTextAlign(Paint.Align.CENTER);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4f);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = (int) (cardWidth * getResources().getDisplayMetrics().density);
        int h = (int) (cardHeight * getResources().getDisplayMetrics().density);
        // 王牌更大
        if (card != null && (card.getValue() == Card.JOKER_BIG || card.getValue() == Card.JOKER_SMALL)) {
            w = (int) (cardWidth * 1.15f * getResources().getDisplayMetrics().density);
            h = (int) (cardHeight * 1.15f * getResources().getDisplayMetrics().density);
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (card == null) return;

        float w = getWidth();
        float h = getHeight();
        cardRect = new RectF(2, 2, w - 2, h - 2);

        // 背景
        if (selected) {
            bgPaint.setColor(Color.parseColor("#FFF8E1")); // 暖白
        } else {
            bgPaint.setColor(Color.WHITE);
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint);

        // 边框
        if (selected) {
            borderPaint.setColor(Color.WHITE);
            borderPaint.setStrokeWidth(4f);
        } else {
            borderPaint.setColor(Color.parseColor("#BDBDBD"));
            borderPaint.setStrokeWidth(2f);
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint);

        // 大王/小王特殊效果
        if (card.getValue() == Card.JOKER_BIG) {
            // 金色高光
            Paint goldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            goldPaint.setColor(Color.parseColor("#FFD700"));
            goldPaint.setAlpha(60);
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, goldPaint);
            // 金色边框
            Paint goldBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            goldBorder.setColor(Color.parseColor("#FFD700"));
            goldBorder.setStyle(Paint.Style.STROKE);
            goldBorder.setStrokeWidth(3f);
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, goldBorder);
        } else if (card.getValue() == Card.JOKER_SMALL) {
            // 银色高光
            Paint silverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            silverPaint.setColor(Color.parseColor("#C0C0C0"));
            silverPaint.setAlpha(60);
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, silverPaint);
            Paint silverBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            silverBorder.setColor(Color.parseColor("#C0C0C0"));
            silverBorder.setStyle(Paint.Style.STROKE);
            silverBorder.setStrokeWidth(3f);
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, silverBorder);
        }

        // 文字颜色
        if (card.isRed()) {
            textPaint.setColor(Color.parseColor("#D32F2F"));
            suitPaint.setColor(Color.parseColor("#D32F2F"));
        } else {
            textPaint.setColor(Color.parseColor("#212121"));
            suitPaint.setColor(Color.parseColor("#212121"));
        }

        // 绘制牌面
        float cx = w / 2;
        if (card.getValue() == Card.JOKER_BIG) {
            textPaint.setTextSize(w * 0.22f);
            canvas.drawText("大王", cx, h * 0.45f, textPaint);
            suitPaint.setTextSize(w * 0.18f);
            suitPaint.setColor(Color.parseColor("#FFD700"));
            canvas.drawText("JOKER", cx, h * 0.7f, suitPaint);
        } else if (card.getValue() == Card.JOKER_SMALL) {
            textPaint.setTextSize(w * 0.22f);
            canvas.drawText("小王", cx, h * 0.45f, textPaint);
            suitPaint.setTextSize(w * 0.18f);
            suitPaint.setColor(Color.parseColor("#9E9E9E"));
            canvas.drawText("JOKER", cx, h * 0.7f, suitPaint);
        } else {
            // 左上角点数
            textPaint.setTextSize(w * 0.25f);
            canvas.drawText(card.getLabel(), w * 0.25f, h * 0.28f, textPaint);
            // 左上角花色
            suitPaint.setTextSize(w * 0.18f);
            canvas.drawText(card.getSuitSymbol(), w * 0.25f, h * 0.48f, suitPaint);
            // 中间大花色
            suitPaint.setTextSize(w * 0.4f);
            canvas.drawText(card.getSuitSymbol(), cx, h * 0.65f, suitPaint);
            // 右下角倒置点数
            textPaint.setTextSize(w * 0.25f);
            canvas.save();
            canvas.rotate(180, w * 0.75f, h * 0.72f);
            canvas.drawText(card.getLabel(), w * 0.75f, h * 0.78f, textPaint);
            canvas.restore();
        }

        // 选中上浮效果（绘制阴影）
        if (selected) {
            Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shadowPaint.setColor(Color.parseColor("#40000000"));
            shadowPaint.setStyle(Paint.Style.FILL);
            RectF shadowRect = new RectF(cardRect);
            shadowRect.offset(3, 5);
            canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enabled) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setSelected(!selected);
                if (clickListener != null) clickListener.onCardClick(this);
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setOnCardClickListener(OnCardClickListener l) { this.clickListener = l; }
    public Card getCard() { return card; }
    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        // 上浮动效
        if (selected) {
            setTranslationY(-20f);
        } else {
            setTranslationY(0f);
        }
        invalidate();
    }
    public boolean isCardSelected() { return selected; }
    public void setCardEnabled(boolean e) { this.enabled = e; invalidate(); }
}
