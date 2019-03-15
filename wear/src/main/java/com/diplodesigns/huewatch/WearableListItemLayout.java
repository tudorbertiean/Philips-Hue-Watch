package com.diplodesigns.huewatch;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Tudor on 3/31/2016.
 */
public class WearableListItemLayout extends LinearLayout
        implements WearableListView.OnCenterProximityListener {

    private ImageView mCircle;
    private TextView mName;

    private final float mFadedTextAlpha;

    public WearableListItemLayout(Context context) {
        this(context, null);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);

        mFadedTextAlpha = getResources()
                .getInteger(R.integer.action_text_faded_alpha) / 100f;
    }

    // Get references to the icon and text in the item layout definition
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // These are defined in the layout file for list items
        // (see next section)
        mCircle = (ImageView) findViewById(R.id.lightOn);
        mName = (TextView) findViewById(R.id.lightName);
    }

    @Override
    public void onCenterPosition(boolean animate) {
        mName.setAlpha(1f);
        LinearLayout.LayoutParams circleSize = new LinearLayout.LayoutParams(25, 25);
        mCircle.setLayoutParams(circleSize);
        mName.setTextSize(13);
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        mName.setAlpha(mFadedTextAlpha);
        LinearLayout.LayoutParams circleSize = new LinearLayout.LayoutParams(20, 20);
        mCircle.setLayoutParams(circleSize);
        mName.setTextSize(7);
    }
}
