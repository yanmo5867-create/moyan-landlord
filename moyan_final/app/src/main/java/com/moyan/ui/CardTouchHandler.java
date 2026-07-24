package com.moyan.ui;

import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.moyan.model.Card;

import java.util.ArrayList;
import java.util.List;

public class CardTouchHandler implements View.OnTouchListener {

    private LinearLayout handCardArea;
    private HorizontalScrollView scrollView;
    private List<Card> handCards;
    private List<View> cardViews;
    private float startX;
    private float startY;
    private boolean isSelecting;
    private int touchSlop = 20;
    private OnCardsSelectedListener listener;

    public interface OnCardsSelectedListener {
        void onCardsSelected(List<Card> selectedCards);
    }

    public CardTouchHandler(LinearLayout area, HorizontalScrollView scroll, List<Card> cards, List<View> views) {
        this.handCardArea = area;
        this.scrollView = scroll;
        this.handCards = cards;
        this.cardViews = views;
    }

    public void setOnCardsSelectedListener(OnCardsSelectedListener l) {
        this.listener = l;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                isSelecting = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - startX;
                float dy = event.getY() - startY;

                // 判断是滑动选牌还是滚动手牌区
                if (!isSelecting && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    isSelecting = true;
                }

                if (isSelecting) {
                    // 根据滑动方向批量选/取消选牌
                    int centerIdx = findCardIndexAt(event.getX() + scrollView.getScrollX());
                    if (centerIdx >= 0 && centerIdx < cardViews.size()) {
                        if (dx > 0) {
                            // 右滑：选中滑过的牌
                            selectCardsUpTo(centerIdx, true);
                        } else {
                            // 左滑：取消选中
                            selectCardsUpTo(centerIdx, false);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!isSelecting) {
                    // 单击：选中/取消单张牌
                    int idx = findCardIndexAt(event.getX() + scrollView.getScrollX());
                    if (idx >= 0 && idx < handCards.size()) {
                        Card c = handCards.get(idx);
                        c.setSelected(!c.isSelected());
                        updateCardView(cardViews.get(idx), c);
                        notifySelection();
                    }
                } else {
                    notifySelection();
                }
                isSelecting = false;
                break;
        }
        return true;
    }

    private int findCardIndexAt(float x) {
        // 简化：根据x坐标估算牌索引
        float cardWidth = 54f; // 卡片宽度+间距
        int idx = (int)(x / cardWidth);
        return Math.max(0, Math.min(idx, handCards.size() - 1));
    }

    private void selectCardsUpTo(int idx, boolean selected) {
        // 简化实现：切换目标牌状态
        if (idx >= 0 && idx < handCards.size()) {
            Card c = handCards.get(idx);
            if (c.isSelected() != selected) {
                c.setSelected(selected);
                updateCardView(cardViews.get(idx), c);
            }
        }
    }

    private void updateCardView(View view, Card card) {
        if (card.isSelected()) {
            view.setTranslationY(-20f);
            view.setAlpha(1f);
        } else {
            view.setTranslationY(0f);
            view.setAlpha(0.85f);
        }
    }

    private void notifySelection() {
        if (listener == null) return;
        List<Card> selected = new ArrayList<>();
        for (Card c : handCards) {
            if (c.isSelected()) selected.add(c);
        }
        listener.onCardsSelected(selected);
    }

    /**
     * 清空所有选中
     */
    public void clearSelection() {
        for (int i = 0; i < handCards.size(); i++) {
            handCards.get(i).setSelected(false);
            updateCardView(cardViews.get(i), handCards.get(i));
        }
        notifySelection();
    }
}
