/* Copyright (c) 2012 vortexwolf

        Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
// Tweaked by me

package com.weebly.opus1269.clipman.ui.views;

import java.lang.reflect.Field;

import com.weebly.opus1269.clipman.app.Log;

import android.content.Context;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * A TextView that correctly handles clickable spans.
 */
public class ClickableLinksTextView extends
    android.support.v7.widget.AppCompatTextView {
    private static final String TAG = "ClickableLinksTextView";

    private boolean mBaseEditorCopied = false;
    private Object mBaseEditor = null;
    private Field mDiscardNextActionUpField = null;
    private Field mIgnoreActionUpEventField = null;

    public ClickableLinksTextView(Context context) {
        super(context);
    }

    public ClickableLinksTextView(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);
    }

    public ClickableLinksTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();

        // listview scrolling behaves incorrectly after you select
        // and copy some text, so I've added this code
        if (this.isFocused()) {
            this.clearFocus();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // the base TextView class checks if getAutoLinkMask != 0,
        // so I added a similar code for == 0
        if(this.isTextSelectable()
                && this.getText() instanceof Spannable
                && this.getAutoLinkMask() == 0
                && this.getLinksClickable()
                && this.isEnabled()
                && this.getLayout() != null) {
            return this.checkLinksOnTouch(event);
        }

        return super.onTouchEvent(event);
    }

    private boolean checkLinksOnTouch(MotionEvent event) {
        this.copyBaseEditorIfNecessary();

        int action = event.getAction() & 0xff; // getActionMasked()
        boolean discardNextActionUp = this.getDiscardNextActionUp();

        // call the base method anyway
        final boolean superResult = super.onTouchEvent(event);

        // the same check as in the super.onTouchEvent(event)
        if(discardNextActionUp && action == MotionEvent.ACTION_UP) {
            return superResult;
        }

        final boolean touchIsFinished = (action == MotionEvent.ACTION_UP) &&
            !this.getIgnoreActionUpEvent() && isFocused();

        // Copied from the LinkMovementMethod class
        if (touchIsFinished) {
            Spannable spannable = (Spannable)this.getText();
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= this.getTotalPaddingLeft();
            y -= this.getTotalPaddingTop();

            x += this.getScrollX();
            y += this.getScrollY();

            Layout layout = this.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link =
                spannable.getSpans(off, off, ClickableSpan.class);

            if (link.length != 0) {
                link[0].onClick(this);
                return true;
            }
        }

        return superResult;
    }

    private void copyBaseEditorIfNecessary(){
        if(this.mBaseEditorCopied) {
            return;
        }

        try {
            Field field = TextView.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            this.mBaseEditor = field.get(this);

            if (this.mBaseEditor != null) {
                Class editorClass = this.mBaseEditor.getClass();
                this.mDiscardNextActionUpField =
                    editorClass.getDeclaredField("mDiscardNextActionUp");
                this.mDiscardNextActionUpField.setAccessible(true);

                this.mIgnoreActionUpEventField =
                    editorClass.getDeclaredField("mIgnoreActionUpEvent");
                this.mIgnoreActionUpEventField.setAccessible(true);
            }

        } catch (Exception ex) {
            Log.logEx(TAG, "", ex);
        } finally {
            this.mBaseEditorCopied = true;
        }
    }

    private boolean getDiscardNextActionUp() {
        if(this.mBaseEditor == null) {
            return false;
        }

        try {
            return this.mDiscardNextActionUpField.getBoolean(this.mBaseEditor);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean getIgnoreActionUpEvent() {
        if(this.mBaseEditor == null) {
            return false;
        }

        try {
            return this.mIgnoreActionUpEventField.getBoolean(this.mBaseEditor);
        } catch (Exception e) {
            return false;
        }
    }
}
