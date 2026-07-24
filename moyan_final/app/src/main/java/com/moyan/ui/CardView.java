package com.moyan.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.moyan.model.Card;

public class CardView extends FrameLayout {

    private TextView textView;
    private Card card;
    private boolean isSelected;
    private boolean isJoker;

    public CardView(Context context, Card card) {
        super(context);
        this.card = card;
        this.isJoker = (card.getValue() == 16 || card.getValue() == 17);
        init(context);
    }

    private void init(Context ctx) {
        // 卡片背景
        setBackgroundResource(com.moyan.R.drawable.card_back);

        textView = new TextView(ctx);
        textView.setText(card.getDisplayName());
        textView.setTextSize(isJoker ? 18 : 16);
        textView.setGravity(Gravity.CENTER);

        // 大王金色，小王银色
        if (card.getValue() == 17) {
            textView.setTextColor(Color.parseColor("#FFD700"));
            setBackgroundResource(com.moyan.R.drawable.card_back);
        } else if (card.getValue() == 16) {
            textView.setTextColor(Color.parseColor("#C0C0C0"));
        } else if (isRed(card)) {
            textView.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            textView.setTextColor(Color.parseColor("#212121"));
        }

        addView(textView);

        // 大王小王尺寸略大
        int w = isJoker ? 64 : 54;
        int h = isJoker ? 88 : 72;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        setLayoutParams(lp);

        // 默认未选中
        setSelected(false);
    }

    private boolean isRed(Card c) {
        String suit = c.getSuit();
        return suit.contains("♥") || suit.contains("♦");
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        this.isSelected = selected;
        if (selected) {
            setTranslationY(-16f);
            setAlpha(1f);
        } else {
            setTranslationY(0f);
            setAlpha(0.9f);
        }
    }

    public Card getCard() { return card; }
}
