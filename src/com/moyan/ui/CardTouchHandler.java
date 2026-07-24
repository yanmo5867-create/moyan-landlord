package com.moyan.ui;

import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.moyan.model.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡牌触摸处理器
 * 支持：单点选/取消、左右滑动连选/连取消、点击空白取消全部
 */
public class CardTouchHandler implements View.OnTouchListener {

    private LinearLayout handContainer;
    private List<CardView> cardViews;
    private float touchStartX = -1;
    private float touchStartY = -1;
    private boolean isDragging = false;
    private float lastTouchX = -1;
    private static final float DRAG_THRESHOLD = 20f; // 拖动阈值dp

    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(List<Card> selectedCards);
    }

    public CardTouchHandler(LinearLayout container) {
        this.handContainer = container;
        this.cardViews = new ArrayList<>();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener l) {
        this.listener = l;
    }

    /** 设置手牌视图列表 */
    public void setCardViews(List<CardView> views) {
        this.cardViews = views;
        for (CardView cv : views) {
            cv.setOnTouchListener(this);
        }
    }

    /** 清空所有选中 */
    public void clearSelection() {
        for (CardView cv : cardViews) {
            cv.setSelected(false);
        }
        notifyChanged();
    }

    /** 获取当前选中的卡牌 */
    public List<Card> getSelectedCards() {
        List<Card> selected = new ArrayList<>();
        for (CardView cv : cardViews) {
            if (cv.isCardSelected() && cv.getCard() != null) {
                selected.add(cv.getCard());
            }
        }
        return selected;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!(v instanceof CardView)) return false;
        CardView touchedCard = (CardView) v;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                lastTouchX = event.getRawX();
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - lastTouchX;
                if (!isDragging && Math.abs(dx) > DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging) {
                    handleDrag(touchedCard, dx > 0);
                    lastTouchX = event.getRawX();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging) {
                    // 单击：切换选中
                    touchedCard.setSelected(!touchedCard.isCardSelected());
                    notifyChanged();
                }
                isDragging = false;
                touchStartX = -1;
                return true;
        }
        return false;
    }

    /**
     * 处理拖动选牌
     * 向右滑 → 选中，向左滑 → 取消选中
     */
    private void handleDrag(CardView fromCard, boolean rightDrag) {
        int fromIndex = cardViews.indexOf(fromCard);
        if (fromIndex < 0) return;

        // 找到当前选中的最左/最右边界
        int leftMost = -1, rightMost = -1;
        for (int i = 0; i < cardViews.size(); i++) {
            if (cardViews.get(i).isCardSelected()) {
                if (leftMost == -1) leftMost = i;
                rightMost = i;
            }
        }

        if (rightDrag) {
            // 向右滑：选中 fromCard 及其右侧到 rightMost 的牌
            if (rightMost == -1) rightMost = fromIndex;
            for (int i = fromIndex; i <= rightMost; i++) {
                cardViews.get(i).setSelected(true);
            }
        } else {
            // 向左滑：取消 fromCard 及其左侧到 leftMost 的牌
            if (leftMost == -1) leftMost = fromIndex;
            for (int i = leftMost; i <= fromIndex; i++) {
                cardViews.get(i).setSelected(false);
            }
        }
        notifyChanged();
    }

    /** 检测是否点击了空白区域（用于取消全部选中） */
    public void checkBlankTouch(float x, float y) {
        // 检查是否点击在卡牌区域外
        boolean hitCard = false;
        for (CardView cv : cardViews) {
            int[] loc = new int[2];
            cv.getLocationOnScreen(loc);
            if (x >= loc[0] && x <= loc[0] + cv.getWidth()
                    && y >= loc[1] && y <= loc[1] + cv.getHeight()) {
                hitCard = true;
                break;
            }
        }
        if (!hitCard && !cardViews.isEmpty()) {
            clearSelection();
        }
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onSelectionChanged(getSelectedCards());
        }
    }
}
