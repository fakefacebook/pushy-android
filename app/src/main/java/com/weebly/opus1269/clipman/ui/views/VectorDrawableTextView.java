
package com.weebly.opus1269.clipman.ui.views;

import android.support.v7.widget.AppCompatTextView;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;

import com.weebly.opus1269.clipman.R;

/**
 * Custom TextView that handles VectorDrawables correctly
 * on drawableXXXX resources back to Jelly Bean
 * http://stackoverflow.com/a/40250753/4468645
 */

public class VectorDrawableTextView extends AppCompatTextView {
    public VectorDrawableTextView(Context context) {
        super(context);
    }

    public VectorDrawableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public VectorDrawableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray attributeArray =
                context.obtainStyledAttributes(attrs, R.styleable.VectorDrawableTextView);

        Drawable drawableLeft = null;
        Drawable drawableRight = null;
        Drawable drawableBottom = null;
        Drawable drawableTop = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawableLeft = attributeArray.getDrawable(R.styleable.VectorDrawableTextView_drawableLeftCompat);
            drawableRight = attributeArray.getDrawable(R.styleable.VectorDrawableTextView_drawableRightCompat);
            drawableBottom = attributeArray.getDrawable(R.styleable.VectorDrawableTextView_drawableBottomCompat);
            drawableTop = attributeArray.getDrawable(R.styleable.VectorDrawableTextView_drawableTopCompat);
        } else {
            final int drawableLeftId = attributeArray.getResourceId(R.styleable.VectorDrawableTextView_drawableLeftCompat, -1);
            final int drawableRightId = attributeArray.getResourceId(R.styleable.VectorDrawableTextView_drawableRightCompat, -1);
            final int drawableBottomId = attributeArray.getResourceId(R.styleable.VectorDrawableTextView_drawableBottomCompat, -1);
            final int drawableTopId = attributeArray.getResourceId(R.styleable.VectorDrawableTextView_drawableTopCompat, -1);

            if (drawableLeftId != -1)
                drawableLeft = AppCompatResources.getDrawable(context, drawableLeftId);
            if (drawableRightId != -1)
                drawableRight = AppCompatResources.getDrawable(context, drawableRightId);
            if (drawableBottomId != -1)
                drawableBottom = AppCompatResources.getDrawable(context, drawableBottomId);
            if (drawableTopId != -1)
                drawableTop = AppCompatResources.getDrawable(context, drawableTopId);
        }

        setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
        attributeArray.recycle();
    }
}
